package org.pixelbays.rpg.guild.event;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.guild.GuildPermission;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record GuildRolePermissionChangedEvent(
        @Nonnull UUID guildId,
        @Nonnull UUID actorId,
        @Nonnull String roleId,
        @Nonnull GuildPermission permission,
        boolean enabled) implements IEvent<Void> {

    public static void dispatch(UUID guildId, UUID actorId, String roleId, GuildPermission permission, boolean enabled) {
        IEventDispatcher<GuildRolePermissionChangedEvent, GuildRolePermissionChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(GuildRolePermissionChangedEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new GuildRolePermissionChangedEvent(guildId, actorId, roleId, permission, enabled));
        }
    }
}
