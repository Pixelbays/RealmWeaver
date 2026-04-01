package org.pixelbays.rpg.chat.command;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
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

@SuppressWarnings("null")
public final class ChatCommand extends AbstractPlayerCommand {

    private enum Mode {
        Status,
        SetActive,
        Membership
    }

    private final Mode mode;
    @Nullable
    private final RequiredArg<String> actionArg;
    @Nullable
    private final RequiredArg<String> channelArg;

    public ChatCommand() {
        super("chat", "Manage your active chat channel");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.mode = Mode.Status;
        this.actionArg = null;
        this.channelArg = null;
        this.addUsageVariant(new ChatCommand(Mode.SetActive, "Set your active chat channel"));
        this.addUsageVariant(new ChatCommand(Mode.Membership, "Join or leave a global chat channel"));
    }

    private ChatCommand(@Nonnull Mode mode, @Nonnull String description) {
        super(description);
        this.mode = mode;
        if (mode == Mode.Membership) {
            this.actionArg = this.withRequiredArg("action", "join or leave", ArgTypes.STRING);
            this.channelArg = this.withRequiredArg("channel", "Joinable channel id or alias", ArgTypes.STRING);
        } else if (mode == Mode.SetActive) {
            this.actionArg = null;
            this.channelArg = this.withRequiredArg("channel", "Channel id or alias, or off", ArgTypes.STRING);
        } else {
            this.actionArg = null;
            this.channelArg = null;
        }
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        ChatManager chatManager = Realmweavers.get().getChatManager();
        if (player == null) {
            return;
        }

        if (mode == Mode.Status) {
            ChatChannel active = chatManager.getActiveChannel(playerRef.getUuid());
            if (active != null) {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.info.active").param("channel", active.getId()));
            } else {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.info.none"));
            }
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        RequiredArg<String> requestedArg = channelArg;
        if (requestedArg == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        String requestedChannel = requestedArg.get(ctx);
        if (requestedChannel == null || requestedChannel.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        if (mode == Mode.Membership) {
            RequiredArg<String> membershipActionArg = actionArg;
            handleMembershipAction(
                    player,
                    playerRef,
                    chatManager,
                    membershipActionArg == null ? null : membershipActionArg.get(ctx),
                    requestedChannel);
            return;
        }

        setActiveChannel(player, playerRef, chatManager, requestedChannel);
    }

    private void setActiveChannel(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull ChatManager chatManager,
            @Nonnull String requested) {

        String normalized = requested.trim().toLowerCase();
        if (normalized.equals("off") || normalized.equals("clear") || normalized.equals("none")) {
            chatManager.clearActiveChannel(playerRef.getUuid());
            player.sendMessage(Message.translation("pixelbays.rpg.chat.success.cleared"));
            return;
        }

        ChatChannel channel = chatManager.getChannel(normalized);
        if (channel == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.unknownChannel").param("channel", requested));
            return;
        }

        boolean wasJoined = channel.isJoinable() && chatManager.isJoined(playerRef.getUuid(), channel.getId());
        if (channel.isJoinable()) {
            chatManager.setActiveChannel(playerRef.getUuid(), channel.getId());
            if (!channel.canSend(playerRef)) {
                if (!wasJoined) {
                    chatManager.leaveChannel(playerRef.getUuid(), channel.getId());
                }
                player.sendMessage(Message.translation("pixelbays.rpg.chat.error.cannotSend").param("channel", channel.getId()));
                return;
            }
            if (!wasJoined) {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.success.joined").param("channel", channel.getId()));
            }
        } else if (!channel.canSend(playerRef)) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.cannotSend").param("channel", channel.getId()));
            return;
        }

        chatManager.setActiveChannel(playerRef.getUuid(), channel.getId());
        player.sendMessage(Message.translation("pixelbays.rpg.chat.success.active").param("channel", channel.getId()));
    }

    private void handleMembershipAction(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull ChatManager chatManager,
            @Nullable String action,
            @Nonnull String requestedChannel) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase();
        if (!normalizedAction.equals("join") && !normalizedAction.equals("leave")) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.usage"));
            return;
        }

        ChatChannel channel = chatManager.getChannel(requestedChannel);
        if (channel == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.unknownChannel").param("channel", requestedChannel));
            return;
        }

        if (!channel.isJoinable()) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.notJoinable").param("channel", channel.getId()));
            return;
        }

        if (normalizedAction.equals("join")) {
            if (chatManager.isJoined(playerRef.getUuid(), channel.getId())) {
                player.sendMessage(Message.translation("pixelbays.rpg.chat.info.alreadyJoined").param("channel", channel.getId()));
                return;
            }

            chatManager.joinChannel(playerRef.getUuid(), channel.getId());
            player.sendMessage(Message.translation("pixelbays.rpg.chat.success.joined").param("channel", channel.getId()));
            return;
        }

        if (!chatManager.isJoined(playerRef.getUuid(), channel.getId())) {
            player.sendMessage(Message.translation("pixelbays.rpg.chat.error.notJoined").param("channel", channel.getId()));
            return;
        }

        chatManager.leaveChannel(playerRef.getUuid(), channel.getId());
        player.sendMessage(Message.translation("pixelbays.rpg.chat.success.left").param("channel", channel.getId()));
    }
}
