package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildMemberKickedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull UUID targetId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, UUID targetId) {
        IEventDispatcher<GuildMemberKickedEvent, GuildMemberKickedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildMemberKickedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildMemberKickedEvent(guildId, actorId, targetId));
        }
    }
}
