package org.pixelbays.rpg.global.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record RaceStatBonusesRecalculatedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String raceId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String raceId) {
        IEventDispatcher<RaceStatBonusesRecalculatedEvent, RaceStatBonusesRecalculatedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(RaceStatBonusesRecalculatedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new RaceStatBonusesRecalculatedEvent(entityRef, raceId));
        }
    }
}
