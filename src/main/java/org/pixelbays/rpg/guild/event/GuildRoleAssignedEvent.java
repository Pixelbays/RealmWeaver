package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildRoleAssignedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull UUID targetId,
        @Nonnull String roleId) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, UUID targetId, String roleId) {
        IEventDispatcher<GuildRoleAssignedEvent, GuildRoleAssignedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildRoleAssignedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildRoleAssignedEvent(guildId, actorId, targetId, roleId));
        }
    }
}
