package org.pixelbays.rpg.ability.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ClassAbilityUnlockedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String classId,
        @Nonnull String abilityId,
        int rank,
        @Nonnull String systemId,
        boolean exactLevel) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String classId, String abilityId,
            int rank, String systemId, boolean exactLevel) {
        IEventDispatcher<ClassAbilityUnlockedEvent, ClassAbilityUnlockedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ClassAbilityUnlockedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ClassAbilityUnlockedEvent(entityRef, classId, abilityId, rank, systemId, exactLevel));
        }
    }
}
