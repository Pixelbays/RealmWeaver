package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyLeftEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID memberId,
        boolean wasLeader) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID memberId, boolean wasLeader) {
        IEventDispatcher<PartyLeftEvent, PartyLeftEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyLeftEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyLeftEvent(partyId, memberId, wasLeader));
        }
    }
}
