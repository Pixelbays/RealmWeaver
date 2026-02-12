package org.pixelbays.rpg.guild.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GuildRoleCreateCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> nameArg;
    private final GuildManager guildManager;

    public GuildRoleCreateCommand() {
        super("create", "Create a guild role");
        this.nameArg = this.withOptionalArg("name", "Role name", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!nameArg.provided(ctx)) {
            player.sendMessage(Message.raw("Usage: /guild role create <name>"));
            return;
        }

        String roleName = nameArg.get(ctx);
        if (roleName == null || roleName.isEmpty()) {
            player.sendMessage(Message.raw("Usage: /guild role create <name>"));
            return;
        }

        GuildActionResult result = guildManager.createRole(playerRef.getUuid(), roleName);
        player.sendMessage(Message.raw(result.getMessage()));
    }
}
