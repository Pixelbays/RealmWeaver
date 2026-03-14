package org.pixelbays.rpg.party.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main party command collection
 * Usage:
 *   /party create [party|raid]
 *   /party invite <player>
 *   /party join <player>
 *   /party leave
 *   /party kick <player>
 *   /party promote <player> [assistant|leader]
 *   /party disband
 *   /party info
 */
public class PartyCommand extends AbstractCommandCollection {

    public PartyCommand() {
        super("party", "Manage your party");
        this.addSubCommand(new PartyCreateCommand());
        this.addSubCommand(new PartyInviteCommand());
        this.addSubCommand(new PartyJoinCommand());
        this.addSubCommand(new PartyLeaveCommand());
        this.addSubCommand(new PartyKickCommand());
        this.addSubCommand(new PartyPromoteCommand());
        this.addSubCommand(new PartyDisbandCommand());
        this.addSubCommand(new PartyInfoCommand());
        this.addSubCommand(new PartyUiCommand());
    }
}
