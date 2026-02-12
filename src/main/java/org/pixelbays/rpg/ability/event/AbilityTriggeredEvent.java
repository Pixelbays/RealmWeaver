package org.pixelbays.rpg.ability.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record AbilityTriggeredEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String abilityId,
        @Nonnull InteractionType interactionType,
        @Nonnull String interactionChainId) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> entityRef, String abilityId,
            InteractionType interactionType, String interactionChainId) {
        IEventDispatcher<AbilityTriggeredEvent, AbilityTriggeredEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(AbilityTriggeredEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new AbilityTriggeredEvent(entityRef, abilityId, interactionType, interactionChainId));
        }
    }
}
