package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildApplicationDeniedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull UUID applicantId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, UUID applicantId) {
        IEventDispatcher<GuildApplicationDeniedEvent, GuildApplicationDeniedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildApplicationDeniedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildApplicationDeniedEvent(guildId, actorId, applicantId));
        }
    }
}
