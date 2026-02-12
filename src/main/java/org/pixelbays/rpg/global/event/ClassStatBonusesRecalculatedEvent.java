package org.pixelbays.rpg.global.event;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ClassStatBonusesRecalculatedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Set<String> classIds) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, Set<String> classIds) {
        IEventDispatcher<ClassStatBonusesRecalculatedEvent, ClassStatBonusesRecalculatedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ClassStatBonusesRecalculatedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ClassStatBonusesRecalculatedEvent(entityRef, new HashSet<>(classIds)));
        }
    }
}
