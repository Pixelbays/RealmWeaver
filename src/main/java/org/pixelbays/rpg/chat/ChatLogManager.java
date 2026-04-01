package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.ChatLogData;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class ChatLogManager {

    private final CharacterManager characterManager;
    private final ChatLogPersistence persistence;
    private final Map<UUID, ChatLogData> logsByPlayer = new ConcurrentHashMap<>();
    private volatile boolean loggingEnabled;
    private volatile int maxEntriesPerPlayer = 250;

    public ChatLogManager(@Nonnull CharacterManager characterManager) {
        this(characterManager, new ChatLogPersistence());
    }

    ChatLogManager(@Nonnull CharacterManager characterManager, @Nonnull ChatLogPersistence persistence) {
        this.characterManager = characterManager;
        this.persistence = persistence;
    }

    public void configure(boolean loggingEnabled, int maxEntriesPerPlayer) {
        this.loggingEnabled = loggingEnabled;
        this.maxEntriesPerPlayer = Math.max(maxEntriesPerPlayer, 1);
        trimAllLogs();
    }

    public void loadFromAssets() {
        logsByPlayer.clear();
        for (ChatLogData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }

            UUID accountId = data.getAccountId();
            if (accountId.getLeastSignificantBits() == 0L && accountId.getMostSignificantBits() == 0L) {
                continue;
            }
            logsByPlayer.put(accountId, data);
        }
        trimAllLogs();
    }

    public void clear() {
        logsByPlayer.clear();
        loggingEnabled = false;
    }

    public void recordMessage(
            @Nonnull PlayerRef sender,
            @Nonnull String channelId,
            @Nonnull String originalMessage,
            @Nonnull String deliveredMessage,
            @Nonnull List<String> matchedWords,
            int targetCount) {
        if (!loggingEnabled) {
            return;
        }

        UUID accountId = sender.getUuid();
        String accountName = resolveAccountName(accountId, sender.getUsername());
        String characterName = resolveCharacterName(accountId, accountName);
        ChatLogData.ChatLogEntryData entry = ChatLogData.ChatLogEntryData.create(
                System.currentTimeMillis(),
                channelId,
                accountName,
                characterName,
                originalMessage,
                deliveredMessage,
                matchedWords,
                targetCount);

        ChatLogData logData = logsByPlayer.computeIfAbsent(accountId, ChatLogData::create);
        logData.setLastKnownUsername(accountName);
        logData.setLastKnownCharacterName(characterName);
        logData.appendEntry(entry, maxEntriesPerPlayer);
        persistence.save(logData);
    }

    @Nullable
    public UUID resolveAccountIdByPlayerName(@Nullable String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }

        String trimmed = rawName.trim();
        PlayerRef playerRef = Universe.get().getPlayerByUsername(trimmed, NameMatching.DEFAULT);
        if (playerRef != null) {
            return playerRef.getUuid();
        }

        for (UUID accountId : characterManager.getKnownAccountIds()) {
            String username = characterManager.resolveAccountUsername(accountId);
            if (username != null && username.equalsIgnoreCase(trimmed)) {
                return accountId;
            }
        }

        for (ChatLogData logData : logsByPlayer.values()) {
            if (logData != null && logData.matchesName(trimmed)) {
                return logData.getAccountId();
            }
        }

        return null;
    }

    @Nullable
    public ChatLogData getLog(@Nonnull UUID accountId) {
        return logsByPlayer.get(accountId);
    }

    @Nonnull
    public String resolveDisplayName(@Nonnull UUID accountId) {
        PlayerRef playerRef = Universe.get().getPlayer(accountId);
        if (playerRef != null && playerRef.getUsername() != null && !playerRef.getUsername().isBlank()) {
            return playerRef.getUsername();
        }

        ChatLogData logData = logsByPlayer.get(accountId);
        if (logData != null && !logData.getLastKnownUsername().isBlank()) {
            return logData.getLastKnownUsername();
        }

        String username = characterManager.resolveAccountUsername(accountId);
        return username == null || username.isBlank() ? accountId.toString() : username;
    }

    @Nonnull
    public String resolveLastKnownCharacterName(@Nonnull UUID accountId) {
        ChatLogData logData = logsByPlayer.get(accountId);
        if (logData != null && !logData.getLastKnownCharacterName().isBlank()) {
            return logData.getLastKnownCharacterName();
        }
        return resolveCharacterName(accountId, resolveDisplayName(accountId));
    }

    private void trimAllLogs() {
        int limit = Math.max(maxEntriesPerPlayer, 1);
        for (ChatLogData data : logsByPlayer.values()) {
            if (data == null) {
                continue;
            }

            List<ChatLogData.ChatLogEntryData> entries = new ArrayList<>(data.getEntries());
            if (entries.size() <= limit) {
                continue;
            }

            ChatLogData replacement = ChatLogData.create(data.getAccountId());
            replacement.setLastKnownUsername(data.getLastKnownUsername());
            replacement.setLastKnownCharacterName(data.getLastKnownCharacterName());
            int startIndex = Math.max(entries.size() - limit, 0);
            for (int index = startIndex; index < entries.size(); index++) {
                replacement.appendEntry(entries.get(index), limit);
            }
            logsByPlayer.put(data.getAccountId(), replacement);
            persistence.save(replacement);
        }
    }

    @Nonnull
    private String resolveAccountName(@Nonnull UUID accountId, @Nullable String username) {
        String resolved = username == null || username.isBlank()
                ? characterManager.resolveAccountUsername(accountId)
                : username;
        return resolved == null || resolved.isBlank() ? accountId.toString() : resolved;
    }

    @Nonnull
    private String resolveCharacterName(@Nonnull UUID accountId, @Nonnull String accountName) {
        String displayName = characterManager.resolveChatDisplayName(accountId, accountName, true);
        if (displayName.equalsIgnoreCase(accountName)) {
            return "";
        }
        return displayName;
    }
}