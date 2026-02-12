package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.guild.GuildJoinPolicy;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildCreatedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID leaderId,
        @Nonnull String name,
        @Nonnull String tag,
        @Nonnull GuildJoinPolicy joinPolicy) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID leaderId, String name, String tag, GuildJoinPolicy joinPolicy) {
        IEventDispatcher<GuildCreatedEvent, GuildCreatedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildCreatedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildCreatedEvent(guildId, leaderId, name, tag, joinPolicy));
        }
    }
}
