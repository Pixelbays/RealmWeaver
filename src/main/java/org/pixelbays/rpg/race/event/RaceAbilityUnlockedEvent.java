package org.pixelbays.rpg.race.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record RaceAbilityUnlockedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String raceId,
        @Nonnull String abilityId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String raceId, String abilityId) {
        IEventDispatcher<RaceAbilityUnlockedEvent, RaceAbilityUnlockedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(RaceAbilityUnlockedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new RaceAbilityUnlockedEvent(entityRef, raceId, abilityId));
        }
    }
}
