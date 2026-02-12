package org.pixelbays.rpg.classes.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ClassUnlearnedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String classId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String classId) {
        IEventDispatcher<ClassUnlearnedEvent, ClassUnlearnedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ClassUnlearnedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ClassUnlearnedEvent(entityRef, classId));
        }
    }
}
