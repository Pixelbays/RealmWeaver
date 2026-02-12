package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyInviteSentEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID inviterId,
        @Nonnull UUID targetId) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID inviterId, UUID targetId) {
        IEventDispatcher<PartyInviteSentEvent, PartyInviteSentEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyInviteSentEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyInviteSentEvent(partyId, inviterId, targetId));
        }
    }
}
