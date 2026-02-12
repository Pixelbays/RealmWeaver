package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.party.PartyType;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyCreatedEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID leaderId,
        @Nonnull PartyType type) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID leaderId, PartyType type) {
        IEventDispatcher<PartyCreatedEvent, PartyCreatedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyCreatedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyCreatedEvent(partyId, leaderId, type));
        }
    }
}
