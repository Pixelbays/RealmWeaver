package org.pixelbays.rpg.party;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.pixelbays.rpg.party.config.PartyData;
import org.pixelbays.rpg.party.config.settings.PartyModSettings.PartyXpGrantingMode;

import com.hypixel.hytale.server.core.Message;

class PartyManagerPersistenceTest {

    @Test
    void loadFromAssets_prunesOfflinePlayersAndPromotesOnlineAssistant() {
        UUID leaderId = UUID.randomUUID();
        UUID assistantId = UUID.randomUUID();
        UUID npcId = UUID.randomUUID();

        Party party = new Party(UUID.randomUUID(), PartyType.PARTY, leaderId, defaultSettings());
        party.addMember(new PartyMember(leaderId, PartyMemberType.PLAYER, PartyRole.LEADER, 10L));
        party.addMember(new PartyMember(assistantId, PartyMemberType.PLAYER, PartyRole.ASSISTANT, 20L));
        party.addMember(new PartyMember(npcId, PartyMemberType.NPC, PartyRole.MEMBER, 30L));

        FakePartyPersistence persistence = new FakePartyPersistence(List.of(PartyData.fromParty(party)));
        FakeRuntimeAccess runtime = new FakeRuntimeAccess(Set.of(assistantId));
        runtime.nameById.put(assistantId, "Assistant");
        runtime.nameById.put(npcId, "Follower");

        PartyManager manager = new PartyManager(persistence, () -> 1000L, runtime);
        manager.loadFromAssets();

        Party restored = manager.getParty(party.getId());
        assertNotNull(restored);
        assertEquals(assistantId, restored.getLeaderId());
        assertFalse(restored.hasMember(leaderId));
        assertTrue(restored.hasMember(assistantId));
        assertTrue(restored.hasMember(npcId));
        assertTrue(persistence.savedPartyIds.contains(party.getId()));
        assertFalse(persistence.deletedPartyIds.contains(party.getId()));
    }

    @Test
    void loadFromAssets_deletesPartyWhenNoOnlinePlayersRemain() {
        UUID leaderId = UUID.randomUUID();
        UUID npcId = UUID.randomUUID();

        Party party = new Party(UUID.randomUUID(), PartyType.PARTY, leaderId, defaultSettings());
        party.addMember(new PartyMember(leaderId, PartyMemberType.PLAYER, PartyRole.LEADER, 10L));
        party.addMember(new PartyMember(npcId, PartyMemberType.NPC, PartyRole.MEMBER, 30L));

        FakePartyPersistence persistence = new FakePartyPersistence(List.of(PartyData.fromParty(party)));
        PartyManager manager = new PartyManager(persistence, () -> 1000L, new FakeRuntimeAccess(Set.of()));
        manager.loadFromAssets();

        assertNull(manager.getParty(party.getId()));
        assertTrue(persistence.deletedPartyIds.contains(party.getId()));
        assertTrue(persistence.savedPartyIds.isEmpty());
    }

    @Test
    void partyDataRoundTrip_preservesJoinedAtAndAssistantLimit() {
        UUID leaderId = UUID.randomUUID();
        UUID npcId = UUID.randomUUID();
        PartySettings settings = new PartySettings(true, PartyXpGrantingMode.SplitEqualInRange, 48, 1, true, 20, 4);

        Party party = new Party(UUID.randomUUID(), PartyType.RAID, leaderId, settings);
        party.addMember(new PartyMember(leaderId, PartyMemberType.PLAYER, PartyRole.LEADER, 123L));
        party.addMember(new PartyMember(npcId, PartyMemberType.NPC, PartyRole.MEMBER, 456L));

        Party restored = PartyData.fromParty(party).toParty();

        assertEquals(123L, restored.getMember(leaderId).getJoinedAtMillis());
        assertEquals(456L, restored.getMember(npcId).getJoinedAtMillis());
        assertEquals(4, restored.getSettings().getMaxAssistants());
    }

    @Test
    void joinParty_failsWhenInviteExpires() {
        UUID leaderId = UUID.randomUUID();
        UUID joinerId = UUID.randomUUID();
        AtomicLong now = new AtomicLong(1_000L);
        FakeRuntimeAccess runtime = new FakeRuntimeAccess(Set.of(leaderId));

        PartyManager manager = new PartyManager(new FakePartyPersistence(List.of()), now::get, runtime);
        assertTrue(manager.createParty(leaderId, PartyType.PARTY).isSuccess());
        assertTrue(manager.invitePlayer(leaderId, joinerId).isSuccess());

        now.set(400_000L);
        PartyActionResult result = manager.joinParty(joinerId, leaderId);

        assertFalse(result.isSuccess());
        assertEquals("Your party invite has expired.", result.getMessage());
    }

    private static PartySettings defaultSettings() {
        return new PartySettings(true, PartyXpGrantingMode.SplitEqualInRange, 48, 1, true, 5, 1);
    }

    private static class FakePartyPersistence extends PartyPersistence {
        private final List<PartyData> storedParties;
        private final List<Party> savedParties = new ArrayList<>();
        private final Set<UUID> savedPartyIds = new HashSet<>();
        private final Set<UUID> deletedPartyIds = new HashSet<>();

        private FakePartyPersistence(List<PartyData> storedParties) {
            this.storedParties = new ArrayList<>(storedParties);
        }

        @Override
        public List<PartyData> loadAll() {
            return new ArrayList<>(storedParties);
        }

        @Override
        public void saveParty(Party party) {
            savedParties.add(party);
            savedPartyIds.add(party.getId());
        }

        @Override
        public void deleteParty(UUID partyId) {
            deletedPartyIds.add(partyId);
        }
    }

    private static class FakeRuntimeAccess implements PartyManager.RuntimeAccess {
        private final Set<UUID> onlinePlayers;
        private final Map<UUID, String> nameById = new HashMap<>();
        private final Map<UUID, List<Message>> messagesByPlayer = new HashMap<>();

        private FakeRuntimeAccess(Set<UUID> onlinePlayers) {
            this.onlinePlayers = new HashSet<>(onlinePlayers);
        }

        @Override
        public boolean isPlayerOnline(@Nonnull UUID playerId) {
            return onlinePlayers.contains(playerId);
        }

        @Override
        public void sendPlayerMessage(@Nonnull UUID playerId, @Nonnull Message message) {
            messagesByPlayer.computeIfAbsent(playerId, id -> new ArrayList<>()).add(message);
        }

        @Override
        @Nonnull
        public String resolveDisplayName(@Nonnull UUID entityId) {
            return nameById.getOrDefault(entityId, entityId.toString());
        }
    }
}
