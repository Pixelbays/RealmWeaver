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

public class GuildCreateCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> nameArg;
    private final OptionalArg<String> tagArg;
    private final GuildManager guildManager;

    public GuildCreateCommand() {
        super("create", "Create a new guild");
        this.nameArg = this.withOptionalArg("name", "Guild name", ArgTypes.STRING);
        this.tagArg = this.withOptionalArg("tag", "Guild tag", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!nameArg.provided(ctx) || !tagArg.provided(ctx)) {
            player.sendMessage(Message.translation("server.rpg.guild.usage.create"));
            return;
        }

        String name = nameArg.get(ctx);
        String tag = tagArg.get(ctx);
        if (name == null || name.isEmpty() || tag == null || tag.isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.guild.usage.create"));
            return;
        }

        GuildActionResult result = guildManager.createGuild(playerRef.getUuid(), name, tag);
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
    }
}
