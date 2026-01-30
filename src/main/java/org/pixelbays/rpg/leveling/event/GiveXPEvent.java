package org.pixelbays.rpg.leveling.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record GiveXPEvent(
        @Nonnull Ref<EntityStore> playerRef,
        long amount, String type) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> playerRef, long amount, String type) {
        IEventDispatcher<GiveXPEvent, GiveXPEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(GiveXPEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GiveXPEvent(playerRef, amount, type));
        }
    }
}
