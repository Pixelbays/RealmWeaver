package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildApplicationSubmittedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID applicantId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID applicantId) {
        IEventDispatcher<GuildApplicationSubmittedEvent, GuildApplicationSubmittedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildApplicationSubmittedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildApplicationSubmittedEvent(guildId, applicantId));
        }
    }
}
