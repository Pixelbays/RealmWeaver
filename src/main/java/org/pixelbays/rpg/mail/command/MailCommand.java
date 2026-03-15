package org.pixelbays.rpg.mail.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class MailCommand extends AbstractCommandCollection {

    public MailCommand() {
        super("mail", "Manage your mail");
        this.addSubCommand(new org.pixelbays.rpg.mail.command.MailUiCommand());
    }
}
