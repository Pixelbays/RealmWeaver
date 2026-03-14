package org.pixelbays.rpg.party.command;

import java.util.UUID;

import org.pixelbays.rpg.party.PartyType;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class PartyCommandUtil {

    private PartyCommandUtil() {
    }

    public static PlayerRef findPlayerByName(String name) {
        return Universe.get().getPlayerByUsername(name, NameMatching.DEFAULT);
    }

    public static String resolveDisplayName(UUID playerId) {
        PlayerRef ref = Universe.get().getPlayer(playerId);
        return ref != null ? ref.getUsername() : playerId.toString();
    }

    public static Message partyTypeMessage(PartyType type) {
        return switch (type) {
            case RAID -> Message.translation("pixelbays.rpg.party.type.raid");
            case PARTY -> Message.translation("pixelbays.rpg.party.type.party");
        };
    }

    public static Message managerResultMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        return switch (message) {
            case "Raids are disabled." -> Message.translation("pixelbays.rpg.party.error.raidsDisabled");
            case "Parties are disabled." -> Message.translation("pixelbays.rpg.party.error.partiesDisabled");
            case "You are already in a party." -> Message.translation("pixelbays.rpg.party.error.alreadyInParty");
            case "You are not in a party. Use /party create first." -> Message.translation("pixelbays.rpg.party.error.notInPartyCreateFirst");
            case "Only leaders or assistants can invite players." -> Message.translation("pixelbays.rpg.party.error.invitePermission");
            case "That player is already in a party." -> Message.translation("pixelbays.rpg.party.error.targetAlreadyInParty");
            case "The party is full." -> Message.translation("pixelbays.rpg.party.error.partyFull");
            case "Invite sent." -> Message.translation("pixelbays.rpg.party.success.inviteSent");
            case "That player is not in a party." -> Message.translation("pixelbays.rpg.party.error.targetNotInParty");
            case "You do not have an invite to that party." -> Message.translation("pixelbays.rpg.party.error.noInvite");
            case "Joined the party." -> Message.translation("pixelbays.rpg.party.success.joined");
            case "You are not in a party." -> Message.translation("pixelbays.rpg.party.error.notInParty");
            case "You left the party." -> Message.translation("pixelbays.rpg.party.success.left");
            case "Only the leader can disband the party." -> Message.translation("pixelbays.rpg.party.error.disbandPermission");
            case "Party disbanded." -> Message.translation("pixelbays.rpg.party.success.disbanded");
            case "That player is not in your party." -> Message.translation("pixelbays.rpg.party.error.targetNotInYourParty");
            case "Only leaders or assistants can kick members." -> Message.translation("pixelbays.rpg.party.error.kickPermission");
            case "You cannot kick the leader." -> Message.translation("pixelbays.rpg.party.error.cannotKickLeader");
            case "Member removed." -> Message.translation("pixelbays.rpg.party.success.memberRemoved");
            case "Only the leader can promote assistants." -> Message.translation("pixelbays.rpg.party.error.promoteAssistantPermission");
            case "NPCs cannot be assistants." -> Message.translation("pixelbays.rpg.party.error.npcAssistant");
            case "Member promoted to assistant." -> Message.translation("pixelbays.rpg.party.success.promotedAssistant");
            case "Only the leader can transfer leadership." -> Message.translation("pixelbays.rpg.party.error.transferPermission");
            case "NPCs cannot be leaders." -> Message.translation("pixelbays.rpg.party.error.npcLeader");
            case "Leadership transferred." -> Message.translation("pixelbays.rpg.party.success.leadershipTransferred");
            default -> {
                if ("Created party.".equals(message)) {
                    yield Message.translation("pixelbays.rpg.party.success.created").param("type", "party");
                }
                if ("Created raid.".equals(message)) {
                    yield Message.translation("pixelbays.rpg.party.success.created").param("type", "raid");
                }

                yield Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", message);
            }
        };
    }
}
