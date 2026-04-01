package org.pixelbays.rpg.guild.command;

import java.util.UUID;

import org.pixelbays.rpg.guild.GuildJoinPolicy;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class GuildCommandUtil {

    private GuildCommandUtil() {
    }

    public static PlayerRef findPlayerByName(String name) {
        return Universe.get().getPlayerByUsername(name, NameMatching.DEFAULT);
    }

    public static String resolveDisplayName(UUID playerId) {
        PlayerRef ref = Universe.get().getPlayer(playerId);
        return ref != null ? ref.getUsername() : playerId.toString();
    }

    public static Message joinPolicyMessage(GuildJoinPolicy joinPolicy) {
        return switch (joinPolicy) {
            case INVITE_ONLY -> Message.translation("pixelbays.rpg.guild.joinPolicy.inviteOnly");
            case OPEN -> Message.translation("pixelbays.rpg.guild.joinPolicy.open");
            case APPLICATION -> Message.translation("pixelbays.rpg.guild.joinPolicy.application");
        };
    }

    public static Message managerResultMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        return switch (message) {
            case "Guilds are disabled." -> Message.translation("pixelbays.rpg.guild.error.disabled");
            case "You are already in a guild." -> Message.translation("pixelbays.rpg.guild.error.alreadyInGuild");
            case "Invalid guild name." -> Message.translation("pixelbays.rpg.guild.error.invalidName");
            case "Invalid guild tag." -> Message.translation("pixelbays.rpg.guild.error.invalidTag");
            case "That guild name is already taken." -> Message.translation("pixelbays.rpg.guild.error.nameTaken");
            case "That guild tag is already taken." -> Message.translation("pixelbays.rpg.guild.error.tagTaken");
            case "You are not in a guild. Use /guild create first." -> Message.translation("pixelbays.rpg.guild.error.notInGuildCreateFirst");
            case "You do not have permission to invite players." -> Message.translation("pixelbays.rpg.guild.error.invitePermission");
            case "That player is already in a guild." -> Message.translation("pixelbays.rpg.guild.error.targetAlreadyInGuild");
            case "The guild is full." -> Message.translation("pixelbays.rpg.guild.error.guildFull");
            case "Invite sent." -> Message.translation("pixelbays.rpg.guild.success.inviteSent");
            case "That guild was not found." -> Message.translation("pixelbays.rpg.guild.error.notFound");
            case "That guild is full." -> Message.translation("pixelbays.rpg.guild.error.guildFullTarget");
            case "That guild requires applications. Use /guild apply." -> Message.translation("pixelbays.rpg.guild.error.requiresApplications");
            case "You do not have an invite to that guild." -> Message.translation("pixelbays.rpg.guild.error.noInvite");
            case "You do not have a pending guild invite." -> Message.translation("pixelbays.rpg.guild.error.noPendingInvite");
            case "Your guild invite has expired." -> Message.translation("pixelbays.rpg.guild.error.inviteExpired");
            case "That guild is not accepting applications." -> Message.translation("pixelbays.rpg.guild.error.notAcceptingApplications");
            case "You already applied to that guild." -> Message.translation("pixelbays.rpg.guild.error.alreadyApplied");
            case "Invalid guild application message." -> Message.translation("pixelbays.rpg.guild.error.invalidApplicationMessage");
            case "Select a guild from the results first." -> Message.translation("pixelbays.rpg.guild.applications.ui.selectionRequired");
            case "Application submitted." -> Message.translation("pixelbays.rpg.guild.success.applicationSubmitted");
            case "No outgoing invite found for that player." -> Message.translation("pixelbays.rpg.guild.error.outgoingInviteNotFound");
            case "That invite does not belong to your guild." -> Message.translation("pixelbays.rpg.guild.error.outgoingInviteWrongGuild");
            case "You are not in a guild." -> Message.translation("pixelbays.rpg.guild.error.notInGuild");
            case "You do not have permission to accept applications." -> Message.translation("pixelbays.rpg.guild.error.acceptApplicationPermission");
            case "No application found for that player." -> Message.translation("pixelbays.rpg.guild.error.applicationNotFound");
            case "Application accepted." -> Message.translation("pixelbays.rpg.guild.success.applicationAccepted");
            case "You do not have permission to deny applications." -> Message.translation("pixelbays.rpg.guild.error.denyApplicationPermission");
            case "Application denied." -> Message.translation("pixelbays.rpg.guild.success.applicationDenied");
            case "Guild invite declined." -> Message.translation("pixelbays.rpg.guild.success.inviteDeclined");
            case "Invite canceled." -> Message.translation("pixelbays.rpg.guild.success.inviteCanceled");
            case "You left the guild." -> Message.translation("pixelbays.rpg.guild.success.left");
            case "Only the leader can disband the guild." -> Message.translation("pixelbays.rpg.guild.error.disbandPermission");
            case "Guild disbanded." -> Message.translation("pixelbays.rpg.guild.success.disbanded");
            case "That player is not in your guild." -> Message.translation("pixelbays.rpg.guild.error.targetNotInYourGuild");
            case "You do not have permission to kick members." -> Message.translation("pixelbays.rpg.guild.error.kickPermission");
            case "You do not have permission to manage guild info." -> Message.translation("pixelbays.rpg.guild.error.manageInfoPermission");
            case "You cannot kick the leader." -> Message.translation("pixelbays.rpg.guild.error.cannotKickLeader");
            case "Member removed." -> Message.translation("pixelbays.rpg.guild.success.memberRemoved");
            case "Only the leader can transfer leadership." -> Message.translation("pixelbays.rpg.guild.error.transferPermission");
            case "That player is already the leader." -> Message.translation("pixelbays.rpg.guild.error.targetAlreadyLeader");
            case "Leadership transferred." -> Message.translation("pixelbays.rpg.guild.success.leadershipTransferred");
            case "You do not have permission to change join policy." -> Message.translation("pixelbays.rpg.guild.error.joinPolicyPermission");
            case "Join policy updated." -> Message.translation("pixelbays.rpg.guild.success.joinPolicyUpdated");
            case "You do not have permission to manage roles." -> Message.translation("pixelbays.rpg.guild.error.manageRolesPermission");
            case "Guild name and tag updates are disabled." -> Message.translation("pixelbays.rpg.guild.error.nameTagUpdatesDisabled");
            case "Invalid guild description." -> Message.translation("pixelbays.rpg.guild.error.invalidDescription");
            case "Invalid guild message of the day." -> Message.translation("pixelbays.rpg.guild.error.invalidMotd");
            case "Invalid role name." -> Message.translation("pixelbays.rpg.guild.error.invalidRoleName");
            case "That role name is reserved." -> Message.translation("pixelbays.rpg.guild.error.roleNameReserved");
            case "That role already exists." -> Message.translation("pixelbays.rpg.guild.error.roleExists");
            case "Role created." -> Message.translation("pixelbays.rpg.guild.success.roleCreated");
            case "Role renamed." -> Message.translation("pixelbays.rpg.guild.success.roleRenamed");
            case "Leader permissions cannot be changed." -> Message.translation("pixelbays.rpg.guild.error.leaderPermsImmutable");
            case "That role does not exist." -> Message.translation("pixelbays.rpg.guild.error.roleNotFound");
            case "Role permission updated." -> Message.translation("pixelbays.rpg.guild.success.rolePermissionUpdated");
            case "Role permissions updated." -> Message.translation("pixelbays.rpg.guild.success.rolePermissionsUpdated");
            case "You do not have permission to assign roles." -> Message.translation("pixelbays.rpg.guild.error.assignRolePermission");
            case "Only the leader can assign the leader role." -> Message.translation("pixelbays.rpg.guild.error.assignLeaderPermission");
            case "You cannot change the leader's role." -> Message.translation("pixelbays.rpg.guild.error.cannotChangeLeaderRole");
            case "Role assigned." -> Message.translation("pixelbays.rpg.guild.success.roleAssigned");
            case "Guild info updated." -> Message.translation("pixelbays.rpg.guild.success.infoUpdated");
            default -> {
                if (message.startsWith("Created guild ") && message.endsWith(".")) {
                    String guildName = message.substring("Created guild ".length(), message.length() - 1);
                    yield Message.translation("pixelbays.rpg.guild.success.created").param("name", guildName);
                }
                yield Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", message);
            }
        };
    }
}
