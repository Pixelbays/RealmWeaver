package org.pixelbays.rpg.global.event;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.leveling.config.StatGrowthConfig;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record StatGrowthAppliedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        int level,
        @Nonnull StatGrowthConfig growth) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, int level, StatGrowthConfig growth) {
        IEventDispatcher<StatGrowthAppliedEvent, StatGrowthAppliedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(StatGrowthAppliedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new StatGrowthAppliedEvent(entityRef, level, growth));
        }
    }
}
