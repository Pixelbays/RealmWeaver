package org.pixelbays.rpg.ability.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record AbilityTriggerFailedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String abilityId,
        @Nonnull String reason) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String abilityId, String reason) {
        IEventDispatcher<AbilityTriggerFailedEvent, AbilityTriggerFailedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(AbilityTriggerFailedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new AbilityTriggerFailedEvent(entityRef, abilityId, reason));
        }
    }
}
