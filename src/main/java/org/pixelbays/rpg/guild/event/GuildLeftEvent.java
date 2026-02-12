package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildLeftEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID memberId,
        boolean wasLeader) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID memberId, boolean wasLeader) {
        IEventDispatcher<GuildLeftEvent, GuildLeftEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildLeftEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildLeftEvent(guildId, memberId, wasLeader));
        }
    }
}
