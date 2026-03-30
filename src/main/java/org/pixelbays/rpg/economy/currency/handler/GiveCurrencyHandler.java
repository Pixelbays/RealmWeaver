package org.pixelbays.rpg.economy.currency.handler;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.event.GiveCurrencyEvent;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class GiveCurrencyHandler implements Consumer<GiveCurrencyEvent> {

    @Override
    public void accept(GiveCurrencyEvent event) {
        if (!event.playerRef().isValid() || event.amount() <= 0L || event.currencyId().isBlank()) {
            return;
        }

        RpgModConfig config = resolveConfig();
        if (config == null || !config.isCurrencyModuleEnabled()) {
            return;
        }

        Store<EntityStore> store = event.playerRef().getStore();
        Player player = store.getComponent(event.playerRef(), Player.getComponentType());
        if (player == null) {
            return;
        }

        String ownerId = resolveOwnerId(store, event);
        if (ownerId == null || ownerId.isBlank()) {
            RpgLogging.debugDeveloper("Currency grant skipped (owner unresolved): currency=%s amount=%s scope=%s player=%s",
                    event.currencyId(), event.amount(), event.scope(), event.playerRef());
            return;
        }

        CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
        CurrencyActionResult result = currencyManager.addBalance(event.scope(), ownerId, event.currencyId(), event.amount());
        if (!result.isSuccess()) {
            RpgLogging.debugDeveloper("Currency grant failed: currency=%s amount=%s scope=%s owner=%s message=%s",
                    event.currencyId(), event.amount(), event.scope(), ownerId, result.getMessage());
        }
    }

    @Nullable
    private String resolveOwnerId(@Nonnull Store<EntityStore> store, @Nonnull GiveCurrencyEvent event) {
        UUIDComponent uuidComponent = store.getComponent(event.playerRef(), UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return null;
        }

        UUID playerId = uuidComponent.getUuid();
        return switch (event.scope()) {
            case Character, Account -> playerId.toString();
            case Guild -> resolveGuildOwnerId(playerId);
            case Global -> "global";
            case Custom -> null;
        };
    }

    @Nullable
    private String resolveGuildOwnerId(@Nonnull UUID playerId) {
        GuildManager guildManager = Realmweavers.get().getGuildManager();
        Guild guild = guildManager.getGuildForMember(playerId);
        return guild == null ? null : guild.getId().toString();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        return RpgModConfig.getAssetMap() == null ? null : RpgModConfig.getAssetMap().getAsset("default");
    }
}
