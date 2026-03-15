package org.pixelbays.rpg.economy.currency.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CurrencyAddCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> scopeArg;
    private final RequiredArg<String> currencyIdArg;
    private final RequiredArg<Integer> amountArg;
    private final CurrencyManager currencyManager;

    public CurrencyAddCommand() {
        super("add", "Add currency to yourself");
        this.scopeArg = this.withRequiredArg("scope", "character|account|guild", ArgTypes.STRING);
        this.currencyIdArg = this.withRequiredArg("currencyId", "Currency id", ArgTypes.STRING);
        this.amountArg = this.withRequiredArg("amount", "Amount", ArgTypes.INTEGER);
        this.currencyManager = ExamplePlugin.get().getCurrencyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String rawScope = this.scopeArg.get(ctx);
        String currencyId = this.currencyIdArg.get(ctx);
        Integer amount = this.amountArg.get(ctx);

        if (player == null || rawScope == null || currencyId == null || amount == null) {
            return;
        }

        CurrencyScope scope = CurrencyCommandUtil.parseScope(rawScope);
        if (scope == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.currency.usage.add"));
            return;
        }

        String ownerId = CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.currency.error.scopeUnavailable")
                    .param("scope", scope.name().toLowerCase()));
            return;
        }

        CurrencyActionResult result = currencyManager.addBalance(scope, ownerId, currencyId, amount.longValue());
        player.sendMessage(CurrencyCommandUtil.managerResultMessage(result));
    }
}
