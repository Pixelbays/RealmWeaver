package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class ChatManager {

    private volatile Map<String, ChatChannel> channelsById = Map.of();
    private volatile Map<String, String> aliasToId = Map.of();
    @Nullable
    private volatile ChatFilterManager chatFilterManager;
    @Nullable
    private volatile ChatLogManager chatLogManager;
    private volatile boolean baseChatProximityEnabled;
    private volatile int baseChatRangeBlocks;
    private final Map<UUID, String> activeChannelByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> joinedChannelsByPlayer = new ConcurrentHashMap<>();

    public void configureModeration(
            @Nullable ChatFilterManager chatFilterManager,
            @Nullable ChatLogManager chatLogManager) {
        this.chatFilterManager = chatFilterManager;
        this.chatLogManager = chatLogManager;
    }

    public void configureBaseChatProximity(boolean enabled, int rangeBlocks) {
        this.baseChatProximityEnabled = enabled;
        this.baseChatRangeBlocks = Math.max(rangeBlocks, 0);
    }

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
        joinedChannelsByPlayer.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(channelId -> !nextChannelsById.containsKey(channelId));
            return entry.getValue().isEmpty();
        });
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

        if (channel.isJoinable()) {
            joinChannel(playerId, channel.getId());
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

    public boolean joinChannel(@Nonnull UUID playerId, @Nullable String channelIdOrAlias) {
        ChatChannel channel = channelIdOrAlias == null ? null : getChannel(channelIdOrAlias);
        if (channel == null || !channel.isJoinable()) {
            return false;
        }

        joinedChannelsByPlayer
                .computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet())
                .add(normalizeKey(channel.getId()));
        return true;
    }

    public boolean leaveChannel(@Nonnull UUID playerId, @Nullable String channelIdOrAlias) {
        ChatChannel channel = channelIdOrAlias == null ? null : getChannel(channelIdOrAlias);
        if (channel == null || !channel.isJoinable()) {
            return false;
        }

        Set<String> joinedChannels = joinedChannelsByPlayer.get(playerId);
        if (joinedChannels == null) {
            return false;
        }

        String normalizedId = normalizeKey(channel.getId());
        boolean removed = joinedChannels.remove(normalizedId);
        if (joinedChannels.isEmpty()) {
            joinedChannelsByPlayer.remove(playerId, joinedChannels);
        }
        if (removed && normalizedId.equals(activeChannelByPlayer.get(playerId))) {
            clearActiveChannel(playerId);
        }
        return removed;
    }

    public boolean isJoined(@Nonnull UUID playerId, @Nullable String channelIdOrAlias) {
        ChatChannel channel = channelIdOrAlias == null ? null : getChannel(channelIdOrAlias);
        if (channel == null) {
            return false;
        }
        if (!channel.isJoinable()) {
            return true;
        }

        Set<String> joinedChannels = joinedChannelsByPlayer.get(playerId);
        return joinedChannels != null && joinedChannels.contains(normalizeKey(channel.getId()));
    }

    @Nonnull
    public List<PlayerRef> getJoinedPlayers(@Nonnull String channelIdOrAlias) {
        String normalizedId = normalizeKey(channelIdOrAlias);
        if (normalizedId.isEmpty()) {
            return List.of();
        }

        ChatChannel channel = getChannel(normalizedId);
        if (channel != null) {
            normalizedId = normalizeKey(channel.getId());
        }

        List<PlayerRef> targets = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> entry : joinedChannelsByPlayer.entrySet()) {
            if (!entry.getValue().contains(normalizedId)) {
                continue;
            }

            PlayerRef ref = Universe.get().getPlayer(entry.getKey());
            if (ref != null) {
                targets.add(ref);
            }
        }
        return targets;
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
            ChatFilterManager.FilterResult filtered = applyFilter(content);
            if (filtered.changed()) {
                event.setContent(filtered.filteredContent());
            }
            if (baseChatProximityEnabled && baseChatRangeBlocks > 0) {
                event.setTargets(ChatTargetingSupport.resolvePlayersInRange(sender, baseChatRangeBlocks));
            }
                logMessage(
                    sender,
                    "base",
                    content,
                    filtered.filteredContent(),
                    filtered.matchedWords(),
                    event.getTargets() == null ? 0 : event.getTargets().size());
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

        ChatFilterManager.FilterResult filteredRouteMessage = applyFilter(route.message);

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
        event.setContent(filteredRouteMessage.filteredContent());
        event.setFormatter(channel.getFormatter());
        logMessage(
            sender,
            channel.getId(),
            route.message,
            filteredRouteMessage.filteredContent(),
            filteredRouteMessage.matchedWords(),
            targets.size());
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

    @Nonnull
    private ChatFilterManager.FilterResult applyFilter(@Nonnull String content) {
        ChatFilterManager filterManager = this.chatFilterManager;
        return filterManager == null
                ? new ChatFilterManager.FilterResult(content, false)
                : filterManager.filterMessage(content);
    }

    private void logMessage(
            @Nonnull PlayerRef sender,
            @Nonnull String channelId,
            @Nonnull String originalMessage,
            @Nonnull String deliveredMessage,
            @Nonnull List<String> matchedWords,
            int targetCount) {
        ChatLogManager logManager = this.chatLogManager;
        if (logManager == null) {
            return;
        }

        logManager.recordMessage(sender, channelId, originalMessage, deliveredMessage, matchedWords, targetCount);
    }

    private record ChatRoute(@Nonnull String channelKey, @Nonnull String message) {
    }
}
