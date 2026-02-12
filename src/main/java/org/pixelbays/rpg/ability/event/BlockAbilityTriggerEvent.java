package org.pixelbays.rpg.ability.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record BlockAbilityTriggerEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String abilityId,
        @Nonnull InteractionType interactionType,
        @Nonnull String interactionChainId) implements IEvent<Void> {

        public static void dispatch(@Nonnull Ref<EntityStore> entityRef, @Nonnull String abilityId,
            @Nonnull InteractionType interactionType, @Nonnull String interactionChainId) {
        IEventDispatcher<BlockAbilityTriggerEvent, BlockAbilityTriggerEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(BlockAbilityTriggerEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new BlockAbilityTriggerEvent(entityRef, abilityId, interactionType, interactionChainId));
        }
    }
}
