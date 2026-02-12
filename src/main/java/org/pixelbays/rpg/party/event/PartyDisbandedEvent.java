package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyDisbandedEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID leaderId) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID leaderId) {
        IEventDispatcher<PartyDisbandedEvent, PartyDisbandedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyDisbandedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyDisbandedEvent(partyId, leaderId));
        }
    }
}
