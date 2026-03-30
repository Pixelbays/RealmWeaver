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

public class GuildJoinCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> guildArg;
    private final GuildManager guildManager;

    public GuildJoinCommand() {
        super("join", "Join a guild");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.guildArg = this.withRequiredArg("guild", "Guild name or tag", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String guildName = guildArg.get(ctx);
        if (guildName == null || guildName.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.usage.join"));
            return;
        }

        GuildActionResult result = guildManager.joinGuild(playerRef.getUuid(), guildName);
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
    }
}
