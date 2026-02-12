package org.pixelbays.rpg.global.event;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record StatIncreasesAppliedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Map<String, Float> increases) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, Map<String, Float> increases) {
        IEventDispatcher<StatIncreasesAppliedEvent, StatIncreasesAppliedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(StatIncreasesAppliedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new StatIncreasesAppliedEvent(entityRef, new HashMap<>(increases)));
        }
    }
}
