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

public class GuildTransferCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> playerArg;
    private final GuildManager guildManager;

    public GuildTransferCommand() {
        super("transfer", "Transfer guild leadership");
        this.playerArg = this.withOptionalArg("player", "Player name", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!playerArg.provided(ctx)) {
            player.sendMessage(Message.raw("Usage: /guild transfer <player>"));
            return;
        }

        String targetName = playerArg.get(ctx);
        if (targetName == null || targetName.isEmpty()) {
            player.sendMessage(Message.raw("Usage: /guild transfer <player>"));
            return;
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.raw("Player not found."));
            return;
        }

        GuildActionResult result = guildManager.transferLeadership(playerRef.getUuid(), targetRef.getUuid());
        player.sendMessage(Message.raw(result.getMessage()));
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.raw("You are now the guild leader."));
        }
    }
}
