package org.pixelbays.rpg.classes.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ActiveClassChangedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String oldClassId,
        @Nonnull String newClassId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String oldClassId, String newClassId) {
        IEventDispatcher<ActiveClassChangedEvent, ActiveClassChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ActiveClassChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ActiveClassChangedEvent(entityRef, oldClassId, newClassId));
        }
    }
}
