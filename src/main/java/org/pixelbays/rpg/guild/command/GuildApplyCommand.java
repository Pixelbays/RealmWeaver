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

public class GuildApplyCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> guildArg;
    private final GuildManager guildManager;

    public GuildApplyCommand() {
        super("apply", "Apply to a guild");
        this.guildArg = this.withOptionalArg("guild", "Guild name or tag", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!guildArg.provided(ctx)) {
            player.sendMessage(Message.translation("server.rpg.guild.usage.apply"));
            return;
        }

        String guildName = guildArg.get(ctx);
        if (guildName == null || guildName.isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.guild.usage.apply"));
            return;
        }

        GuildActionResult result = guildManager.applyToGuild(playerRef.getUuid(), guildName);
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
    }
}
