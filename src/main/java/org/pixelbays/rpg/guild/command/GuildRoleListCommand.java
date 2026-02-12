package org.pixelbays.rpg.guild.command;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.GuildRole;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GuildRoleListCommand extends AbstractPlayerCommand {

    private final GuildManager guildManager;

    public GuildRoleListCommand() {
        super("list", "List guild roles");
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        if (guild == null) {
            player.sendMessage(Message.raw("You are not in a guild."));
            return;
        }

        for (GuildRole role : guild.getRoles().values()) {
            String permissions = role.getPermissions().stream()
                    .map(GuildPermission::name)
                    .collect(Collectors.joining(", "));
            player.sendMessage(Message.raw(role.getName() + " (" + role.getId() + ") - "
                    + (permissions.isEmpty() ? "No permissions" : permissions)));
        }
    }
}
