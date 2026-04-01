package org.pixelbays.rpg.chat.command;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.chat.ChatLogManager;
import org.pixelbays.rpg.chat.ui.ChatLogPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChatLogCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;

    public ChatLogCommand() {
        super("chatlog", "Open a player's chat history");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        ChatLogManager chatLogManager = Realmweavers.get().getChatLogManager();
        UUID targetAccountId = chatLogManager.resolveAccountIdByPlayerName(playerArg.get(ctx));
        if (targetAccountId == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new ChatLogPage(playerRef, targetAccountId));
    }
}