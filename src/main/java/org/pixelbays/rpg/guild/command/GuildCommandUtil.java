package org.pixelbays.rpg.guild.command;

import java.util.UUID;

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

    public static Message managerResultMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Message.translation("server.rpg.common.unknownError");
        }

        return switch (message) {
            case "Guilds are disabled." -> Message.translation("server.rpg.guild.error.disabled");
            case "You are already in a guild." -> Message.translation("server.rpg.guild.error.alreadyInGuild");
            case "Invalid guild name." -> Message.translation("server.rpg.guild.error.invalidName");
            case "Invalid guild tag." -> Message.translation("server.rpg.guild.error.invalidTag");
            case "That guild name is already taken." -> Message.translation("server.rpg.guild.error.nameTaken");
            case "That guild tag is already taken." -> Message.translation("server.rpg.guild.error.tagTaken");
            case "You are not in a guild. Use /guild create first." -> Message.translation("server.rpg.guild.error.notInGuildCreateFirst");
            case "You do not have permission to invite players." -> Message.translation("server.rpg.guild.error.invitePermission");
            case "That player is already in a guild." -> Message.translation("server.rpg.guild.error.targetAlreadyInGuild");
            case "The guild is full." -> Message.translation("server.rpg.guild.error.guildFull");
            case "Invite sent." -> Message.translation("server.rpg.guild.success.inviteSent");
            case "That guild was not found." -> Message.translation("server.rpg.guild.error.notFound");
            case "That guild is full." -> Message.translation("server.rpg.guild.error.guildFullTarget");
            case "That guild requires applications. Use /guild apply." -> Message.translation("server.rpg.guild.error.requiresApplications");
            case "You do not have an invite to that guild." -> Message.translation("server.rpg.guild.error.noInvite");
            case "Your guild invite has expired." -> Message.translation("server.rpg.guild.error.inviteExpired");
            case "That guild is not accepting applications." -> Message.translation("server.rpg.guild.error.notAcceptingApplications");
            case "You already applied to that guild." -> Message.translation("server.rpg.guild.error.alreadyApplied");
            case "Application submitted." -> Message.translation("server.rpg.guild.success.applicationSubmitted");
            case "You are not in a guild." -> Message.translation("server.rpg.guild.error.notInGuild");
            case "You do not have permission to accept applications." -> Message.translation("server.rpg.guild.error.acceptApplicationPermission");
            case "No application found for that player." -> Message.translation("server.rpg.guild.error.applicationNotFound");
            case "Application accepted." -> Message.translation("server.rpg.guild.success.applicationAccepted");
            case "You do not have permission to deny applications." -> Message.translation("server.rpg.guild.error.denyApplicationPermission");
            case "Application denied." -> Message.translation("server.rpg.guild.success.applicationDenied");
            case "You left the guild." -> Message.translation("server.rpg.guild.success.left");
            case "Only the leader can disband the guild." -> Message.translation("server.rpg.guild.error.disbandPermission");
            case "Guild disbanded." -> Message.translation("server.rpg.guild.success.disbanded");
            case "That player is not in your guild." -> Message.translation("server.rpg.guild.error.targetNotInYourGuild");
            case "You do not have permission to kick members." -> Message.translation("server.rpg.guild.error.kickPermission");
            case "You cannot kick the leader." -> Message.translation("server.rpg.guild.error.cannotKickLeader");
            case "Member removed." -> Message.translation("server.rpg.guild.success.memberRemoved");
            case "Only the leader can transfer leadership." -> Message.translation("server.rpg.guild.error.transferPermission");
            case "That player is already the leader." -> Message.translation("server.rpg.guild.error.targetAlreadyLeader");
            case "Leadership transferred." -> Message.translation("server.rpg.guild.success.leadershipTransferred");
            case "You do not have permission to change join policy." -> Message.translation("server.rpg.guild.error.joinPolicyPermission");
            case "Join policy updated." -> Message.translation("server.rpg.guild.success.joinPolicyUpdated");
            case "You do not have permission to manage roles." -> Message.translation("server.rpg.guild.error.manageRolesPermission");
            case "Invalid role name." -> Message.translation("server.rpg.guild.error.invalidRoleName");
            case "That role name is reserved." -> Message.translation("server.rpg.guild.error.roleNameReserved");
            case "That role already exists." -> Message.translation("server.rpg.guild.error.roleExists");
            case "Role created." -> Message.translation("server.rpg.guild.success.roleCreated");
            case "Leader permissions cannot be changed." -> Message.translation("server.rpg.guild.error.leaderPermsImmutable");
            case "That role does not exist." -> Message.translation("server.rpg.guild.error.roleNotFound");
            case "Role permission updated." -> Message.translation("server.rpg.guild.success.rolePermissionUpdated");
            case "You do not have permission to assign roles." -> Message.translation("server.rpg.guild.error.assignRolePermission");
            case "Only the leader can assign the leader role." -> Message.translation("server.rpg.guild.error.assignLeaderPermission");
            case "You cannot change the leader's role." -> Message.translation("server.rpg.guild.error.cannotChangeLeaderRole");
            case "Role assigned." -> Message.translation("server.rpg.guild.success.roleAssigned");
            default -> {
                if (message.startsWith("Created guild ") && message.endsWith(".")) {
                    String guildName = message.substring("Created guild ".length(), message.length() - 1);
                    yield Message.translation("server.rpg.guild.success.created").param("name", guildName);
                }
                yield Message.translation("server.rpg.common.unmappedMessage").param("text", message);
            }
        };
    }
}
