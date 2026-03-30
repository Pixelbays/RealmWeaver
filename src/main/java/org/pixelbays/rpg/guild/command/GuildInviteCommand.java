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

public class GuildInviteCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;
    private final GuildManager guildManager;

    public GuildInviteCommand() {
        super("invite", "Invite a player to your guild");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String targetName = playerArg.get(ctx);
        if (targetName == null || targetName.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.usage.invite"));
            return;
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        GuildActionResult result = guildManager.invitePlayer(playerRef.getUuid(), targetRef.getUuid());
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.invitedBy").param("player", playerRef.getUsername()));
        }
    }
}
