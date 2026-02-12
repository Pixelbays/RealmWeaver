package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildApplicationAcceptedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull UUID applicantId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, UUID applicantId) {
        IEventDispatcher<GuildApplicationAcceptedEvent, GuildApplicationAcceptedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildApplicationAcceptedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildApplicationAcceptedEvent(guildId, actorId, applicantId));
        }
    }
}
