package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildJoinedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID memberId,
        @Nonnull String roleId,
        @Nonnull GuildJoinMethod joinMethod) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID memberId, String roleId, GuildJoinMethod joinMethod) {
        IEventDispatcher<GuildJoinedEvent, GuildJoinedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildJoinedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildJoinedEvent(guildId, memberId, roleId, joinMethod));
        }
    }
}
