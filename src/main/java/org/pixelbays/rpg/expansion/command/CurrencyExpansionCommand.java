package org.pixelbays.rpg.expansion.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CurrencyExpansionCommand extends AbstractCommandCollection {

    public CurrencyExpansionCommand() {
        super("expansion", "Browse and unlock expansions");
        this.addSubCommand(new CurrencyExpansionListCommand());
        this.addSubCommand(new CurrencyExpansionUnlockCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
