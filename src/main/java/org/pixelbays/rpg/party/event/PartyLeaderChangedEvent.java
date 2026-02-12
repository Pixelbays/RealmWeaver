package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyLeaderChangedEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID oldLeaderId,
        @Nonnull UUID newLeaderId) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID oldLeaderId, UUID newLeaderId) {
        IEventDispatcher<PartyLeaderChangedEvent, PartyLeaderChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyLeaderChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyLeaderChangedEvent(partyId, oldLeaderId, newLeaderId));
        }
    }
}
