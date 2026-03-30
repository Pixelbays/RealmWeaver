package org.pixelbays.rpg.economy.currency.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CurrencyNormalizeCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> scopeArg;
    private final CurrencyManager currencyManager;

    public CurrencyNormalizeCommand() {
        super("normalize", "Normalize convertible currencies for a scope");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.scopeArg = this.withRequiredArg("scope", "character|account|guild", ArgTypes.STRING);
        this.currencyManager = Realmweavers.get().getCurrencyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String rawScope = this.scopeArg.get(ctx);
        if (player == null || rawScope == null) {
            return;
        }

        CurrencyScope scope = CurrencyCommandUtil.parseScope(rawScope);
        if (scope == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.currency.usage.normalize"));
            return;
        }

        String ownerId = CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.currency.error.scopeUnavailable")
                    .param("scope", scope.name().toLowerCase()));
            return;
        }

        boolean changed = currencyManager.normalizeWallet(scope, ownerId);
        player.sendMessage(Message.translation(changed
                ? "pixelbays.rpg.currency.success.normalized"
                : "pixelbays.rpg.currency.success.normalizeNoChange").param("scope", scope.name().toLowerCase()));
    }
}
