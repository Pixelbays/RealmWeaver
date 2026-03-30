package org.pixelbays.rpg.classes.talent.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * /class talent ... — Talent tree subcommands.
 * Usage:
 *   /class talent ui [classId]        — Open the interactive talent tree UI
 *   /class talent info [classId]      — View talent allocation summary (text)
 *   /class talent allocate <class> <tree> <node> — Spend a point in a node
 *   /class talent refund <class> <tree> <node>   — Refund a single rank (Partial/Free mode only)
 *   /class talent reset <class>        — Reset all talents for a class
 */
public class ClassTalentCommand extends AbstractCommandCollection {

    public ClassTalentCommand() {
        super("talent", "Manage talent trees for your classes");
        this.addSubCommand(new ClassTalentUiCommand());
        this.addSubCommand(new ClassTalentInfoCommand());
        this.addSubCommand(new ClassTalentAllocateCommand());
        this.addSubCommand(new ClassTalentGrantCommand());
        this.addSubCommand(new ClassTalentRefundCommand());
        this.addSubCommand(new ClassTalentResetCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
