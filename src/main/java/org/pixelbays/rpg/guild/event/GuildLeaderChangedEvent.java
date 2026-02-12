package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildLeaderChangedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID oldLeaderId,
        @Nonnull UUID newLeaderId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID oldLeaderId, UUID newLeaderId) {
        IEventDispatcher<GuildLeaderChangedEvent, GuildLeaderChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildLeaderChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildLeaderChangedEvent(guildId, oldLeaderId, newLeaderId));
        }
    }
}
