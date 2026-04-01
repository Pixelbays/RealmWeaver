package org.pixelbays.rpg.chat.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class ChatFilterCommand extends AbstractCommandCollection {

    public ChatFilterCommand() {
        super("chatfilter", "Manage custom chat filter words");
        this.addSubCommand(new ChatFilterAddCommand());
        this.addSubCommand(new ChatFilterRemoveCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}