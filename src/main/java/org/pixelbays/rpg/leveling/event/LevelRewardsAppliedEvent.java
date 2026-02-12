package org.pixelbays.rpg.leveling.event;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.leveling.config.LevelRewardConfig;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record LevelRewardsAppliedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String systemId,
        int level,
        @Nonnull LevelRewardConfig rewards) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String systemId, int level, LevelRewardConfig rewards) {
        IEventDispatcher<LevelRewardsAppliedEvent, LevelRewardsAppliedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(LevelRewardsAppliedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new LevelRewardsAppliedEvent(entityRef, systemId, level, rewards));
        }
    }
}
