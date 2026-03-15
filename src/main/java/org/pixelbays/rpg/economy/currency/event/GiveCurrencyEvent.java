package org.pixelbays.rpg.economy.currency.event;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record GiveCurrencyEvent(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull String currencyId,
        long amount,
        @Nonnull CurrencyScope scope) implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> playerRef,
            @Nonnull String currencyId,
            long amount,
            @Nonnull CurrencyScope scope) {
        IEventDispatcher<GiveCurrencyEvent, GiveCurrencyEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(GiveCurrencyEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GiveCurrencyEvent(playerRef, currencyId, amount, scope));
        }
    }
}
