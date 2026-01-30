package org.pixelbays.rpg.leveling.event;


import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record LevelUpEvent(
        @Nonnull Ref<EntityStore> playerRef,
    @Nonnull String systemId,
        int oldLevel,
        int newLevel
) implements IEvent<Void> {

    public int levelsGained() {
        return newLevel - oldLevel;
    }

    public static void dispatch(Ref<EntityStore> playerRef, String systemId, int oldLevel, int newLevel) {
        IEventDispatcher<LevelUpEvent, LevelUpEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(LevelUpEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new LevelUpEvent(playerRef, systemId, oldLevel, newLevel));
        }
    }
}
