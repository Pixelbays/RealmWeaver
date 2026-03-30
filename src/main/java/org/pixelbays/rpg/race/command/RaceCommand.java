package org.pixelbays.rpg.race.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main race command collection
 * Usage:
 *   /race list - List all available races
 *   /race select <raceId> - Select a race (only one allowed)
 */
public class RaceCommand extends AbstractCommandCollection {

    public RaceCommand() {
        super("race", "Manage your race");
        this.addSubCommand(new RaceListCommand());
        this.addSubCommand(new RaceSelectCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
