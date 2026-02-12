package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.guild.GuildJoinPolicy;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildJoinPolicyChangedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull GuildJoinPolicy oldPolicy,
        @Nonnull GuildJoinPolicy newPolicy) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, GuildJoinPolicy oldPolicy, GuildJoinPolicy newPolicy) {
        IEventDispatcher<GuildJoinPolicyChangedEvent, GuildJoinPolicyChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildJoinPolicyChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildJoinPolicyChangedEvent(guildId, actorId, oldPolicy, newPolicy));
        }
    }
}
