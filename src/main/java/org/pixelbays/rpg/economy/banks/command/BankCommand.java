package org.pixelbays.rpg.economy.banks.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class BankCommand extends AbstractCommandCollection {

    public BankCommand() {
        super("bank", "Manage your banks");
        this.addSubCommand(new BankInfoCommand());
        this.addSubCommand(new BankUiCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
