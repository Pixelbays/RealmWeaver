package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildRoleCreatedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull String roleId,
        @Nonnull String roleName) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, String roleId, String roleName) {
        IEventDispatcher<GuildRoleCreatedEvent, GuildRoleCreatedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildRoleCreatedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildRoleCreatedEvent(guildId, actorId, roleId, roleName));
        }
    }
}
