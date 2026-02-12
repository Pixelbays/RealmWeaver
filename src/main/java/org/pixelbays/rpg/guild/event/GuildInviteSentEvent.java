package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildInviteSentEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID inviterId,
        @Nonnull UUID targetId,
        long expiresAtMillis) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID inviterId, UUID targetId, long expiresAtMillis) {
        IEventDispatcher<GuildInviteSentEvent, GuildInviteSentEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildInviteSentEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildInviteSentEvent(guildId, inviterId, targetId, expiresAtMillis));
        }
    }
}
