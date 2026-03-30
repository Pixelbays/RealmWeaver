package org.pixelbays.rpg.chat.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.chat.ChatChannel;
import org.pixelbays.rpg.chat.ChatManager;

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

public final class ChatCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> channelArg;

    public ChatCommand() {
        super("chat", "Set your active chat channel");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.channelArg = null;
        this.addUsageVariant(new ChatCommand("Set your active chat channel"));
    }

    private ChatCommand(String description) {
        super(description);
        this.channelArg = this.withRequiredArg("channel", "Channel id or alias, or off", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        ChatManager chatManager = ExamplePlugin.get().getChatManager();

        if (channelArg == null) {
            ChatChannel active = chatManager.getActiveChannel(playerRef.getUuid());
            if (active != null) {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.info.active").param("channel", active.getId()));
            } else {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.info.none"));
            }
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        String requested = channelArg.get(ctx);
        if (requested == null || requested.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        String normalized = requested.trim().toLowerCase();
        if (normalized.equals("off") || normalized.equals("clear") || normalized.equals("none") || normalized.equals("global")) {
            chatManager.clearActiveChannel(playerRef.getUuid());
            player.sendMessage(Message.translation("pixelbays.rpg.chat.success.cleared"));
            return;
        }

        ChatChannel channel = chatManager.getChannel(normalized);
        if (channel == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.unknownChannel").param("channel", requested));
            return;
        }

        if (!channel.canSend(playerRef)) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.cannotSend").param("channel", channel.getId()));
            return;
        }

        chatManager.setActiveChannel(playerRef.getUuid(), channel.getId());
        player.sendMessage(Message.translation("pixelbays.rpg.chat.success.active").param("channel", channel.getId()));
    }
}
