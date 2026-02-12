package org.pixelbays.rpg.race.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record RaceChangedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String oldRaceId,
        @Nonnull String newRaceId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String oldRaceId, String newRaceId) {
        IEventDispatcher<RaceChangedEvent, RaceChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(RaceChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new RaceChangedEvent(entityRef, oldRaceId, newRaceId));
        }
    }
}
