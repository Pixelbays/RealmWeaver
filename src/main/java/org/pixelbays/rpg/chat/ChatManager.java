package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class ChatManager {

    private volatile Map<String, ChatChannel> channelsById = Map.of();
    private volatile Map<String, String> aliasToId = Map.of();
    private final Map<UUID, String> activeChannelByPlayer = new ConcurrentHashMap<>();

    public synchronized void registerChannel(@Nonnull ChatChannel channel) {
        List<ChatChannel> channels = new ArrayList<>(channelsById.values());
        channels.add(channel);
        replaceChannels(channels);
    }

    public synchronized void replaceChannels(@Nonnull List<? extends ChatChannel> channels) {
        LinkedHashMap<String, ChatChannel> nextChannelsById = new LinkedHashMap<>();
        LinkedHashMap<String, String> nextAliasToId = new LinkedHashMap<>();

        for (ChatChannel channel : channels) {
            registerChannel(channel, nextChannelsById, nextAliasToId);
        }

        channelsById = Collections.unmodifiableMap(nextChannelsById);
        aliasToId = Collections.unmodifiableMap(nextAliasToId);
        activeChannelByPlayer.entrySet().removeIf(entry -> !nextChannelsById.containsKey(entry.getValue()));
    }

    private static void registerChannel(
            @Nonnull ChatChannel channel,
            @Nonnull Map<String, ChatChannel> channelsById,
            @Nonnull Map<String, String> aliasToId) {
        String id = normalizeKey(channel.getId());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Channel id cannot be empty");
        }
        if (channelsById.containsKey(id)) {
            throw new IllegalStateException("Channel already registered: " + id);
        }

        channelsById.put(id, channel);
        aliasToId.put(id, id);
        for (String alias : channel.getAliases()) {
            String normalized = normalizeKey(alias);
            if (normalized.isEmpty()) {
                continue;
            }
            aliasToId.putIfAbsent(normalized, id);
        }
    }

    @Nullable
    public ChatChannel getChannel(@Nonnull String idOrAlias) {
        String key = normalizeKey(idOrAlias);
        if (key.isEmpty()) {
            return null;
        }

        String id = aliasToId.get(key);
        if (id == null) {
            return null;
        }

        return channelsById.get(id);
    }

    public void setActiveChannel(@Nonnull UUID playerId, @Nullable String channelIdOrAlias) {
        if (channelIdOrAlias == null || normalizeKey(channelIdOrAlias).isEmpty()) {
            activeChannelByPlayer.remove(playerId);
            return;
        }

        ChatChannel channel = getChannel(channelIdOrAlias);
        if (channel == null) {
            activeChannelByPlayer.remove(playerId);
            return;
        }

        activeChannelByPlayer.put(playerId, normalizeKey(channel.getId()));
    }

    @Nullable
    public ChatChannel getActiveChannel(@Nonnull UUID playerId) {
        String id = activeChannelByPlayer.get(playerId);
        return id != null ? channelsById.get(id) : null;
    }

    public void clearActiveChannel(@Nonnull UUID playerId) {
        activeChannelByPlayer.remove(playerId);
    }

    @Nonnull
    public Function<CompletableFuture<PlayerChatEvent>, CompletableFuture<PlayerChatEvent>> asAsyncHandler() {
        return future -> future.thenApply(this::onPlayerChat);
    }

    @Nonnull
    private PlayerChatEvent onPlayerChat(@Nonnull PlayerChatEvent event) {
        if (event.isCancelled()) {
            return event;
        }

        PlayerRef sender = event.getSender();
        String content = event.getContent();
        if (content == null) {
            return event;
        }

        ChatRoute route = resolveRoute(sender, content);
        if (route == null) {
            return event;
        }

        ChatChannel channel = getChannel(route.channelKey);
        if (channel == null) {
            sender.sendMessage(Message.translation("pixelbays.rpg.chat.error.unknownChannel").param("channel", route.channelKey));
            event.setCancelled(true);
            return event;
        }

        if (route.message.isEmpty()) {
            sender.sendMessage(Message.translation("pixelbays.rpg.chat.error.emptyMessage"));
            event.setCancelled(true);
            return event;
        }

        if (!channel.canSend(sender)) {
            sender.sendMessage(Message.translation("pixelbays.rpg.chat.error.cannotSend").param("channel", channel.getId()));
            event.setCancelled(true);
            return event;
        }

        List<PlayerRef> targets = new ArrayList<>(channel.resolveTargets(sender));
        if (targets.isEmpty()) {
            sender.sendMessage(Message.translation("pixelbays.rpg.chat.error.noTargets").param("channel", channel.getId()));
            event.setCancelled(true);
            return event;
        }

        event.setTargets(targets);
        event.setContent(route.message);
        event.setFormatter(channel.getFormatter());
        return event;
    }

    @Nullable
    private ChatRoute resolveRoute(@Nonnull PlayerRef sender, @Nonnull String content) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // One-off channel message: !party hello
        if (trimmed.startsWith("!")) {
            String rest = trimmed.substring(1);
            int space = rest.indexOf(' ');
            String channelKey;
            String message;
            if (space < 0) {
                channelKey = rest;
                message = "";
            } else {
                channelKey = rest.substring(0, space);
                message = rest.substring(space + 1).trim();
            }

            if (normalizeKey(channelKey).isEmpty()) {
                return null;
            }

            return new ChatRoute(channelKey, message);
        }

        // Active channel (if set)
        ChatChannel active = getActiveChannel(sender.getUuid());
        if (active != null) {
            return new ChatRoute(active.getId(), trimmed);
        }

        return null;
    }

    @Nonnull
    private static String normalizeKey(@Nonnull String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private record ChatRoute(@Nonnull String channelKey, @Nonnull String message) {
    }
}
