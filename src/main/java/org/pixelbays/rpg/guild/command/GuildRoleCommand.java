package org.pixelbays.rpg.guild.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Guild role command collection
 * Usage:
 *   /guild role create <name>
 *   /guild role setperm <role> <permission> <true|false>
 *   /guild role assign <player> <role>
 *   /guild role list
 */
public class GuildRoleCommand extends AbstractCommandCollection {

    public GuildRoleCommand() {
        super("role", "Manage guild roles");
        this.addSubCommand(new GuildRoleCreateCommand());
        this.addSubCommand(new GuildRoleSetPermCommand());
        this.addSubCommand(new GuildRoleAssignCommand());
        this.addSubCommand(new GuildRoleListCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
