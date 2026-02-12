package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyJoinedEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID memberId,
        @Nonnull UUID inviterId) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID memberId, UUID inviterId) {
        IEventDispatcher<PartyJoinedEvent, PartyJoinedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyJoinedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyJoinedEvent(partyId, memberId, inviterId));
        }
    }
}
