package org.pixelbays.rpg.achievement.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record AchievementUnlockedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String achievementId,
        int pointsAwarded,
        boolean accountWide) implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String achievementId,
            int pointsAwarded,
            boolean accountWide) {
        IEventDispatcher<AchievementUnlockedEvent, AchievementUnlockedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(AchievementUnlockedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new AchievementUnlockedEvent(entityRef, achievementId, pointsAwarded, accountWide));
        }
    }
}
