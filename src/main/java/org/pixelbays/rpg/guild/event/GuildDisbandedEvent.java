package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildDisbandedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID leaderId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID leaderId) {
        IEventDispatcher<GuildDisbandedEvent, GuildDisbandedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildDisbandedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildDisbandedEvent(guildId, leaderId));
        }
    }
}
