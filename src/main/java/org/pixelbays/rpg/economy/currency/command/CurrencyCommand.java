package org.pixelbays.rpg.economy.currency.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CurrencyCommand extends AbstractCommandCollection {

    public CurrencyCommand() {
        super("currency", "Manage currencies");
        this.addSubCommand(new CurrencyInfoCommand());
        this.addSubCommand(new CurrencyUiCommand());
        this.addSubCommand(new CurrencyNormalizeCommand());
        this.addSubCommand(new CurrencyAddCommand());
        this.addSubCommand(new CurrencyRemoveCommand());
        this.addSubCommand(new CurrencySetCommand());
    }
}
