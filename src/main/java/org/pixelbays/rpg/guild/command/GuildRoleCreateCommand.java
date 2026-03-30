package org.pixelbays.rpg.guild.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GuildRoleCreateCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg;
    private final GuildManager guildManager;

    public GuildRoleCreateCommand() {
        super("create", "Create a guild role");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.nameArg = this.withRequiredArg("name", "Role name", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String roleName = nameArg.get(ctx);
        if (roleName == null || roleName.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.usage.roleCreate"));
            return;
        }

        GuildActionResult result = guildManager.createRole(playerRef.getUuid(), roleName);
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
    }
}
