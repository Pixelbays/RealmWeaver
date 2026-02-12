package org.pixelbays.rpg.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.party.config.PartyData;
import org.pixelbays.rpg.party.event.PartyAssistantPromotedEvent;
import org.pixelbays.rpg.party.event.PartyCreatedEvent;
import org.pixelbays.rpg.party.event.PartyDisbandedEvent;
import org.pixelbays.rpg.party.event.PartyInviteSentEvent;
import org.pixelbays.rpg.party.event.PartyJoinedEvent;
import org.pixelbays.rpg.party.event.PartyLeaderChangedEvent;
import org.pixelbays.rpg.party.event.PartyLeftEvent;
import org.pixelbays.rpg.party.event.PartyMemberKickedEvent;

public class PartyManager {

    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberToParty = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
    private final PartyPersistence persistence = new PartyPersistence();

    public PartyActionResult createParty(UUID leaderId, PartyType type) {
        RpgModConfig config = resolveConfig();
        if (!isTypeEnabled(type, config)) {
            return PartyActionResult.failure(type == PartyType.RAID ? "Raids are disabled." : "Parties are disabled.");
        }

        if (memberToParty.containsKey(leaderId)) {
            return PartyActionResult.failure("You are already in a party.");
        }

        PartySettings settings = buildSettings(type, config);
        Party party = new Party(UUID.randomUUID(), type, leaderId, settings);
        PartyMember leader = new PartyMember(leaderId, PartyMemberType.PLAYER, PartyRole.LEADER, System.currentTimeMillis());
        party.addMember(leader);

        partiesById.put(party.getId(), party);
        memberToParty.put(leaderId, party.getId());
        savePartyIfEnabled(party, config);
        PartyCreatedEvent.dispatch(party.getId(), leaderId, type);

        return PartyActionResult.success("Created " + type.name().toLowerCase() + ".", party);
    }

    public PartyActionResult invitePlayer(UUID inviterId, UUID targetId) {
        Party party = getPartyForMember(inviterId);
        if (party == null) {
            return PartyActionResult.failure("You are not in a party. Use /party create first.");
        }

        if (!isLeaderOrAssistant(party, inviterId)) {
            return PartyActionResult.failure("Only leaders or assistants can invite players.");
        }

        if (memberToParty.containsKey(targetId)) {
            return PartyActionResult.failure("That player is already in a party.");
        }

        if (party.size() >= getMaxSize(party)) {
            return PartyActionResult.failure("The party is full.");
        }

        pendingInvites.put(targetId, party.getId());
        PartyInviteSentEvent.dispatch(party.getId(), inviterId, targetId);
        return PartyActionResult.success("Invite sent.", party);
    }

    public PartyActionResult joinParty(UUID joinerId, UUID inviterId) {
        if (memberToParty.containsKey(joinerId)) {
            return PartyActionResult.failure("You are already in a party.");
        }

        Party party = getPartyForMember(inviterId);
        if (party == null) {
            return PartyActionResult.failure("That player is not in a party.");
        }

        UUID invitePartyId = pendingInvites.get(joinerId);
        if (invitePartyId == null || !invitePartyId.equals(party.getId())) {
            return PartyActionResult.failure("You do not have an invite to that party.");
        }

        if (party.size() >= getMaxSize(party)) {
            return PartyActionResult.failure("The party is full.");
        }

        PartyMember member = new PartyMember(joinerId, PartyMemberType.PLAYER, PartyRole.MEMBER, System.currentTimeMillis());
        party.addMember(member);
        memberToParty.put(joinerId, party.getId());
        pendingInvites.remove(joinerId);
        savePartyIfEnabled(party, resolveConfig());
        PartyJoinedEvent.dispatch(party.getId(), joinerId, inviterId);

        return PartyActionResult.success("Joined the party.", party);
    }

    public PartyActionResult leaveParty(UUID memberId) {
        Party party = getPartyForMember(memberId);
        if (party == null) {
            return PartyActionResult.failure("You are not in a party.");
        }

        boolean wasLeader = party.isLeader(memberId);
        if (wasLeader) {
            UUID newLeaderId = handleLeaderLeave(party, memberId);
            if (newLeaderId != null) {
                PartyLeaderChangedEvent.dispatch(party.getId(), memberId, newLeaderId);
            } else {
                PartyDisbandedEvent.dispatch(party.getId(), memberId);
            }
        } else {
            party.removeMember(memberId);
        }

        memberToParty.remove(memberId);
        if (party.size() == 0) {
            partiesById.remove(party.getId());
            deletePartyIfEnabled(party.getId(), resolveConfig());
        } else {
            savePartyIfEnabled(party, resolveConfig());
        }

        PartyLeftEvent.dispatch(party.getId(), memberId, wasLeader);
        return PartyActionResult.success("You left the party.");
    }

    public PartyActionResult disbandParty(UUID leaderId) {
        Party party = getPartyForMember(leaderId);
        if (party == null) {
            return PartyActionResult.failure("You are not in a party.");
        }

        if (!party.isLeader(leaderId)) {
            return PartyActionResult.failure("Only the leader can disband the party.");
        }

        removeParty(party);
        deletePartyIfEnabled(party.getId(), resolveConfig());
        PartyDisbandedEvent.dispatch(party.getId(), leaderId);
        return PartyActionResult.success("Party disbanded.");
    }

    public PartyActionResult kickMember(UUID actorId, UUID targetId) {
        Party party = getPartyForMember(actorId);
        if (party == null || !party.hasMember(targetId)) {
            return PartyActionResult.failure("That player is not in your party.");
        }

        if (!isLeaderOrAssistant(party, actorId)) {
            return PartyActionResult.failure("Only leaders or assistants can kick members.");
        }

        if (party.isLeader(targetId)) {
            return PartyActionResult.failure("You cannot kick the leader.");
        }

        party.removeMember(targetId);
        memberToParty.remove(targetId);
        savePartyIfEnabled(party, resolveConfig());
        PartyMemberKickedEvent.dispatch(party.getId(), actorId, targetId);

        return PartyActionResult.success("Member removed.");
    }

    public PartyActionResult promoteToAssistant(UUID actorId, UUID targetId) {
        Party party = getPartyForMember(actorId);
        if (party == null || !party.hasMember(targetId)) {
            return PartyActionResult.failure("That player is not in your party.");
        }

        if (!party.isLeader(actorId)) {
            return PartyActionResult.failure("Only the leader can promote assistants.");
        }

        PartyMember member = party.getMember(targetId);
        if (member.getMemberType() != PartyMemberType.PLAYER) {
            return PartyActionResult.failure("NPCs cannot be assistants.");
        }

        member.setRole(PartyRole.ASSISTANT);
        party.addAssistant(targetId);
        savePartyIfEnabled(party, resolveConfig());
        PartyAssistantPromotedEvent.dispatch(party.getId(), actorId, targetId);

        return PartyActionResult.success("Member promoted to assistant.");
    }

    public PartyActionResult promoteToLeader(UUID actorId, UUID targetId) {
        Party party = getPartyForMember(actorId);
        if (party == null || !party.hasMember(targetId)) {
            return PartyActionResult.failure("That player is not in your party.");
        }

        if (!party.isLeader(actorId)) {
            return PartyActionResult.failure("Only the leader can transfer leadership.");
        }

        PartyMember member = party.getMember(targetId);
        if (member.getMemberType() != PartyMemberType.PLAYER) {
            return PartyActionResult.failure("NPCs cannot be leaders.");
        }

        PartyMember oldLeader = party.getMember(actorId);
        oldLeader.setRole(PartyRole.MEMBER);

        member.setRole(PartyRole.LEADER);
        party.setLeaderId(targetId);
        party.removeAssistant(targetId);
        savePartyIfEnabled(party, resolveConfig());
        PartyLeaderChangedEvent.dispatch(party.getId(), actorId, targetId);

        return PartyActionResult.success("Leadership transferred.");
    }

    public void loadFromAssets() {
        RpgModConfig config = resolveConfig();
        if (config != null && !config.isPartyPersistenceEnabled()) {
            return;
        }

        for (PartyData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }

            Party party = data.toParty();
            if (!hasAnyOnlineMember(party)) {
                continue;
            }

            partiesById.put(party.getId(), party);
            for (PartyMember member : party.getMemberList()) {
                memberToParty.put(member.getEntityId(), party.getId());
            }
        }
    }

    public Party getPartyForMember(UUID memberId) {
        UUID partyId = memberToParty.get(memberId);
        return partyId != null ? partiesById.get(partyId) : null;
    }

    public Party getParty(UUID partyId) {
        return partiesById.get(partyId);
    }

    public List<Party> getParties() {
        return new ArrayList<>(partiesById.values());
    }

    public boolean isPartyEnabled(PartyType type) {
        return isTypeEnabled(type, resolveConfig());
    }

    private boolean isTypeEnabled(PartyType type, RpgModConfig config) {
        if (config == null) {
            return true;
        }
        return type == PartyType.RAID ? config.isRaidEnabled() : config.isPartyEnabled();
    }

    private int getMaxSize(Party party) {
        PartySettings settings = party.getSettings();
        if (settings == null) {
            return party.getType() == PartyType.RAID ? 20 : 5;
        }
        return settings.getMaxSize();
    }

    private UUID handleLeaderLeave(Party party, UUID leaderId) {
        party.removeMember(leaderId);

        UUID replacement = party.getAssistants().stream().findFirst().orElse(null);
        if (replacement == null) {
            replacement = party.getMembers().keySet().stream()
                    .filter(id -> !id.equals(leaderId))
                    .findFirst()
                    .orElse(null);
        }

        if (replacement != null) {
            PartyMember newLeader = party.getMember(replacement);
            newLeader.setRole(PartyRole.LEADER);
            party.setLeaderId(replacement);
            party.removeAssistant(replacement);
            return replacement;
        } else {
            removeParty(party);
            return null;
        }
    }

    private void removeParty(Party party) {
        for (PartyMember member : party.getMemberList()) {
            memberToParty.remove(member.getEntityId());
        }
        partiesById.remove(party.getId());
    }

    private boolean isLeaderOrAssistant(Party party, UUID memberId) {
        return party.isLeader(memberId) || party.isAssistant(memberId);
    }

    private PartySettings buildSettings(PartyType type, RpgModConfig config) {
        boolean xpEnabled = config == null || config.isPartyXpEnabled();
        var xpMode = config == null ? RpgModConfig.PartyXpGrantingMode.SplitEqualInRange : config.getPartyXpGrantingMode();
        int xpRange = config == null ? 48 : config.getPartyXpRangeBlocks();
        int minMembers = config == null ? 1 : config.getPartyXpMinMembersInRange();
        boolean npcAllowed = config == null || config.isPartyNpcAllowed();
        int maxSize = config == null
                ? (type == PartyType.RAID ? 20 : 5)
                : (type == PartyType.RAID ? config.getRaidMaxSize() : config.getPartyMaxSize());

        return new PartySettings(xpEnabled, xpMode, xpRange, minMembers, npcAllowed, maxSize);
    }

    private void savePartyIfEnabled(Party party, RpgModConfig config) {
        if (config != null && !config.isPartyPersistenceEnabled()) {
            return;
        }
        persistence.saveParty(party);
    }

    private void deletePartyIfEnabled(UUID partyId, RpgModConfig config) {
        if (config != null && !config.isPartyPersistenceEnabled()) {
            return;
        }
        persistence.deleteParty(partyId);
    }

    private boolean hasAnyOnlineMember(Party party) {
        for (PartyMember member : party.getMemberList()) {
            if (member.getMemberType() == PartyMemberType.PLAYER) {
                UUID memberId = member.getEntityId();
                if (memberId != null
                        && com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(memberId) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config == null) {
            config = assetMap.getAsset("default");
        }

        if (config == null) {
            RpgLogging.debugDeveloper("[Party] RpgModConfig not found; using defaults.");
        }

        return config;
    }
}
