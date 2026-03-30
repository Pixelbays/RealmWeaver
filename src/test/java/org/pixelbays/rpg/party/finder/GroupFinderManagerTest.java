package org.pixelbays.rpg.party.finder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyPersistence;
import org.pixelbays.rpg.party.PartyType;
import org.pixelbays.rpg.party.config.PartyData;

import com.hypixel.hytale.server.core.Message;

@SuppressWarnings("null")
class GroupFinderManagerTest {

    @Test
    void leaderCanListApplicantsCanApplyAndLeaderCanInvite() {
        UUID leaderId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        AtomicLong now = new AtomicLong(1_000L);

        FakePartyRuntimeAccess partyRuntime = new FakePartyRuntimeAccess(Set.of(leaderId, applicantId));
        partyRuntime.nameById.put(leaderId, "Leader");
        partyRuntime.nameById.put(applicantId, "Applicant");

        PartyManager partyManager = new PartyManager(new FakePartyPersistence(List.of()), now::get, partyRuntime, false);
        PartyActionResult createResult = partyManager.createParty(leaderId, PartyType.PARTY);
        assertTrue(createResult.isSuccess());

        FakeGroupFinderRuntimeAccess finderRuntime = new FakeGroupFinderRuntimeAccess();
        finderRuntime.nameById.put(leaderId, "Leader");
        finderRuntime.nameById.put(applicantId, "Applicant");

        GroupFinderManager manager = new GroupFinderManager(partyManager, now::get, finderRuntime, () -> true);

        GroupFinderActionResult listingResult = manager.upsertListing(
                leaderId,
                "Goblin Caves",
                "Learning run",
            "Need healer and ranged DPS",
            "healer, dps");
        assertTrue(listingResult.isSuccess());

        GroupFinderSnapshot initialSnapshot = manager.getSnapshot(applicantId);
        assertEquals(1, initialSnapshot.getListings().size());
        assertEquals("Goblin Caves", initialSnapshot.getListings().getFirst().getActivity());
        assertEquals(List.of("healer", "dps"), initialSnapshot.getListings().getFirst().getOpenRoles());

        GroupFinderActionResult applicationResult = manager.applyToListing(
                applicantId,
                initialSnapshot.getListings().getFirst().getPartyId(),
                "Healer",
                "Can play support.");
        assertTrue(applicationResult.isSuccess());

        GroupFinderSnapshot leaderSnapshot = manager.getSnapshot(leaderId);
        GroupFinderSnapshot.LeaderListingView ownedListing = leaderSnapshot.getOwnedListing();
        assertNotNull(ownedListing);
        assertEquals(1, ownedListing.getApplications().size());
        assertEquals("Applicant", ownedListing.getApplications().getFirst().getApplicantName());

        GroupFinderActionResult inviteResult = manager.inviteApplicant(leaderId, applicantId);
        assertTrue(inviteResult.isSuccess());
        assertTrue(finderRuntime.messagesByPlayer.containsKey(applicantId));
        assertNull(manager.getSnapshot(applicantId).getCurrentApplication());
    }

    @Test
    void staleListingIsPrunedAfterPartyDisbands() {
        UUID leaderId = UUID.randomUUID();

        FakePartyRuntimeAccess partyRuntime = new FakePartyRuntimeAccess(Set.of(leaderId));
        partyRuntime.nameById.put(leaderId, "Leader");

        PartyManager partyManager = new PartyManager(new FakePartyPersistence(List.of()), System::currentTimeMillis, partyRuntime, false);
        assertTrue(partyManager.createParty(leaderId, PartyType.PARTY).isSuccess());

        FakeGroupFinderRuntimeAccess finderRuntime = new FakeGroupFinderRuntimeAccess();
        finderRuntime.nameById.put(leaderId, "Leader");

        GroupFinderManager manager = new GroupFinderManager(partyManager, System::currentTimeMillis, finderRuntime, () -> true);
        assertTrue(manager.upsertListing(leaderId, "Dungeon", "Fast clear", "Any DPS", "dps").isSuccess());
        assertEquals(1, manager.getSnapshot(leaderId).getListings().size());

        assertTrue(partyManager.disbandParty(leaderId).isSuccess());
        assertTrue(manager.getSnapshot(leaderId).getListings().isEmpty());
    }

    @Test
    void applicantMustChooseOneOfTheListedRoles() {
        UUID leaderId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();

        FakePartyRuntimeAccess partyRuntime = new FakePartyRuntimeAccess(Set.of(leaderId, applicantId));
        PartyManager partyManager = new PartyManager(new FakePartyPersistence(List.of()), System::currentTimeMillis, partyRuntime, false);
        assertTrue(partyManager.createParty(leaderId, PartyType.PARTY).isSuccess());

        GroupFinderManager manager = new GroupFinderManager(partyManager, System::currentTimeMillis,
                new FakeGroupFinderRuntimeAccess(), () -> true);
        assertTrue(manager.upsertListing(leaderId, "Arena", "Push rating", "Need a healer", "tank, healer").isSuccess());

        UUID listingPartyId = manager.getSnapshot(applicantId).getListings().getFirst().getPartyId();

        GroupFinderActionResult missingRoleResult = manager.applyToListing(applicantId, listingPartyId, "", "Ready");
        assertFalse(missingRoleResult.isSuccess());
        assertEquals("Choose a role for this listing.", missingRoleResult.getMessage());

        GroupFinderActionResult wrongRoleResult = manager.applyToListing(applicantId, listingPartyId, "dps", "Ready");
        assertFalse(wrongRoleResult.isSuccess());
        assertEquals("That group is not recruiting for that role.", wrongRoleResult.getMessage());
    }

    @Test
    void listingRejectsUnknownRoleTypes() {
        UUID leaderId = UUID.randomUUID();

        FakePartyRuntimeAccess partyRuntime = new FakePartyRuntimeAccess(Set.of(leaderId));
        PartyManager partyManager = new PartyManager(new FakePartyPersistence(List.of()), System::currentTimeMillis, partyRuntime, false);
        assertTrue(partyManager.createParty(leaderId, PartyType.PARTY).isSuccess());

        GroupFinderManager manager = new GroupFinderManager(partyManager, System::currentTimeMillis,
                new FakeGroupFinderRuntimeAccess(), () -> true);

        GroupFinderActionResult result = manager.upsertListing(leaderId, "Dungeon", "Fast clear", "Any", "bard");
        assertFalse(result.isSuccess());
        assertEquals("Unknown role type: bard", result.getMessage());
    }

    private static class FakePartyPersistence extends PartyPersistence {
        private final List<PartyData> storedParties;

        private FakePartyPersistence(List<PartyData> storedParties) {
            this.storedParties = new ArrayList<>(storedParties);
        }

        @Override
        public List<PartyData> loadAll() {
            return new ArrayList<>(storedParties);
        }

        @Override
        public void saveParty(org.pixelbays.rpg.party.Party party) {
        }

        @Override
        public void deleteParty(UUID partyId) {
        }
    }

    private static class FakePartyRuntimeAccess implements PartyManager.RuntimeAccess {
        private final Set<UUID> onlinePlayers;
        private final Map<UUID, String> nameById = new HashMap<>();
        private final Map<UUID, List<Message>> messagesByPlayer = new HashMap<>();

        private FakePartyRuntimeAccess(Set<UUID> onlinePlayers) {
            this.onlinePlayers = new HashSet<>(onlinePlayers);
        }

        @Override
        public boolean isPlayerOnline(@Nonnull UUID playerId) {
            return onlinePlayers.contains(playerId);
        }

        @Override
        public void sendPlayerMessage(@Nonnull UUID playerId, @Nonnull Message message) {
            messagesByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
        }

        @Override
        @Nonnull
        public String resolveDisplayName(@Nonnull UUID entityId) {
            return Objects.requireNonNull(nameById.getOrDefault(entityId, entityId.toString()));
        }
    }

    private static class FakeGroupFinderRuntimeAccess implements GroupFinderManager.RuntimeAccess {
        private final Map<UUID, String> nameById = new HashMap<>();
        private final Map<UUID, List<Message>> messagesByPlayer = new HashMap<>();

        @Override
        public void sendPlayerMessage(@Nonnull UUID playerId, @Nonnull Message message) {
            messagesByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
        }

        @Override
        @Nonnull
        public String resolveDisplayName(@Nonnull UUID entityId) {
            return Objects.requireNonNull(nameById.getOrDefault(entityId, entityId.toString()));
        }
    }
}
