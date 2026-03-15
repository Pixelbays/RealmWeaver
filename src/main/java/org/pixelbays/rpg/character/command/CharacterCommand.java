package org.pixelbays.rpg.character.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CharacterCommand extends AbstractCommandCollection {
    public CharacterCommand() {
        super("character", "Manage account characters");
        this.addSubCommand(new CharacterUiCommand());
        this.addSubCommand(new CharacterLogoutCommand());
    }
}
