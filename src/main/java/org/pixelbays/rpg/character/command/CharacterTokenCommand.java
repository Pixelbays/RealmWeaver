package org.pixelbays.rpg.character.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CharacterTokenCommand extends AbstractCommandCollection {

    public CharacterTokenCommand() {
        super("token", "Manage account character tokens");
        this.addSubCommand(new CharacterTokenInfoCommand());
        this.addSubCommand(new CharacterTokenGiveCommand());
        this.addSubCommand(new CharacterTokenRemoveCommand());
        this.addSubCommand(new CharacterTokenSetCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}