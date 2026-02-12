package org.pixelbays.rpg.guild.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main guild command collection
 * Usage:
 *   /guild create <name> <tag>
 *   /guild invite <player>
 *   /guild join <name|tag>
 *   /guild apply <name|tag>
 *   /guild accept <player>
 *   /guild deny <player>
 *   /guild leave
 *   /guild kick <player>
 *   /guild transfer <player>
 *   /guild joinpolicy <invite|open|application>
 *   /guild role ...
 *   /guild disband
 *   /guild info
 */
public class GuildCommand extends AbstractCommandCollection {

    public GuildCommand() {
        super("guild", "Manage your guild");
        this.addSubCommand(new GuildCreateCommand());
        this.addSubCommand(new GuildInviteCommand());
        this.addSubCommand(new GuildJoinCommand());
        this.addSubCommand(new GuildApplyCommand());
        this.addSubCommand(new GuildAcceptCommand());
        this.addSubCommand(new GuildDenyCommand());
        this.addSubCommand(new GuildLeaveCommand());
        this.addSubCommand(new GuildKickCommand());
        this.addSubCommand(new GuildTransferCommand());
        this.addSubCommand(new GuildJoinPolicyCommand());
        this.addSubCommand(new GuildRoleCommand());
        this.addSubCommand(new GuildDisbandCommand());
        this.addSubCommand(new GuildInfoCommand());
    }
}
