package org.pixelbays.rpg.party.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record PartyMemberKickedEvent(
        @Nonnull UUID partyId,
        @Nonnull UUID actorId,
        @Nonnull UUID targetId) implements IEvent<Void> {

    public static void dispatch(UUID partyId, UUID actorId, UUID targetId) {
        IEventDispatcher<PartyMemberKickedEvent, PartyMemberKickedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(PartyMemberKickedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PartyMemberKickedEvent(partyId, actorId, targetId));
        }
    }
}
