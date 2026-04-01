package org.pixelbays.rpg.chat.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "null", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ChatLogData implements JsonAssetWithMap<String, DefaultAssetMap<String, ChatLogData>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final FunctionCodec<ChatLogEntryData[], List<ChatLogEntryData>> ENTRY_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ChatLogEntryData.CODEC, ChatLogEntryData[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(ChatLogEntryData[]::new));

    public static final AssetBuilderCodec<String, ChatLogData> CODEC = AssetBuilderCodec.builder(
            ChatLogData.class,
            ChatLogData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("LastKnownUsername", Codec.STRING, false, true),
                    (i, s) -> i.lastKnownUsername = s, i -> i.lastKnownUsername)
            .add()
            .append(new KeyedCodec<>("LastKnownCharacterName", Codec.STRING, false, true),
                    (i, s) -> i.lastKnownCharacterName = s, i -> i.lastKnownCharacterName)
            .add()
            .append(new KeyedCodec<>("Entries", ENTRY_LIST_CODEC, false, true),
                    (i, s) -> i.entries = s, i -> i.entries)
            .add()
            .build();

    private static DefaultAssetMap<String, ChatLogData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private String lastKnownUsername;
    private String lastKnownCharacterName;
    private List<ChatLogEntryData> entries;

    public ChatLogData() {
        this.id = "";
        this.lastKnownUsername = "";
        this.lastKnownCharacterName = "";
        this.entries = new ArrayList<>();
    }

    public static DefaultAssetMap<String, ChatLogData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(ChatLogData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, ChatLogData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public UUID getAccountId() {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    public String getLastKnownUsername() {
        return lastKnownUsername == null ? "" : lastKnownUsername;
    }

    public void setLastKnownUsername(String lastKnownUsername) {
        this.lastKnownUsername = lastKnownUsername == null ? "" : lastKnownUsername;
    }

    public String getLastKnownCharacterName() {
        return lastKnownCharacterName == null ? "" : lastKnownCharacterName;
    }

    public void setLastKnownCharacterName(String lastKnownCharacterName) {
        this.lastKnownCharacterName = lastKnownCharacterName == null ? "" : lastKnownCharacterName;
    }

    public List<ChatLogEntryData> getEntries() {
        return entries == null ? List.of() : new ArrayList<>(entries);
    }

    public void appendEntry(ChatLogEntryData entry, int maxEntries) {
        if (entry == null) {
            return;
        }

        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.add(entry);

        int limit = Math.max(maxEntries, 1);
        while (entries.size() > limit) {
            entries.remove(0);
        }
    }

    public boolean matchesName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return false;
        }

        String normalized = rawName.trim();
        return getLastKnownUsername().equalsIgnoreCase(normalized)
                || getLastKnownCharacterName().equalsIgnoreCase(normalized)
                || id.equalsIgnoreCase(normalized);
    }

    public static ChatLogData create(UUID accountId) {
        ChatLogData data = new ChatLogData();
        data.id = accountId == null ? "" : accountId.toString();
        return data;
    }

    public static class ChatLogEntryData {

        public static final BuilderCodec<ChatLogEntryData> CODEC = BuilderCodec
                .builder(ChatLogEntryData.class, ChatLogEntryData::new)
                .append(new KeyedCodec<>("TimestampMillis", Codec.LONG, false, true),
                        (i, s) -> i.timestampMillis = s, i -> i.timestampMillis)
                .add()
                .append(new KeyedCodec<>("ChannelId", Codec.STRING, false, true),
                        (i, s) -> i.channelId = s, i -> i.channelId)
                .add()
                .append(new KeyedCodec<>("AccountName", Codec.STRING, false, true),
                        (i, s) -> i.accountName = s, i -> i.accountName)
                .add()
                .append(new KeyedCodec<>("CharacterName", Codec.STRING, false, true),
                        (i, s) -> i.characterName = s, i -> i.characterName)
                .add()
                .append(new KeyedCodec<>("OriginalMessage", Codec.STRING, false, true),
                        (i, s) -> i.originalMessage = s, i -> i.originalMessage)
                .add()
                .append(new KeyedCodec<>("DeliveredMessage", Codec.STRING, false, true),
                        (i, s) -> i.deliveredMessage = s, i -> i.deliveredMessage)
                .add()
                .append(new KeyedCodec<>("Filtered", Codec.BOOLEAN, false, true),
                        (i, s) -> i.filtered = s, i -> i.filtered)
                .add()
                .append(new KeyedCodec<>("MatchedWords", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.matchedWords = sanitizeMatchedWords(s), i -> i.matchedWords)
                .add()
                .append(new KeyedCodec<>("TargetCount", Codec.INTEGER, false, true),
                        (i, s) -> i.targetCount = s, i -> i.targetCount)
                .add()
                .build();

        private long timestampMillis;
        private String channelId;
        private String accountName;
        private String characterName;
        private String originalMessage;
        private String deliveredMessage;
        private boolean filtered;
        private List<String> matchedWords;
        private int targetCount;

        public ChatLogEntryData() {
            this.timestampMillis = 0L;
            this.channelId = "";
            this.accountName = "";
            this.characterName = "";
            this.originalMessage = "";
            this.deliveredMessage = "";
            this.filtered = false;
            this.matchedWords = new ArrayList<>();
            this.targetCount = 0;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public String getChannelId() {
            return channelId == null ? "" : channelId;
        }

        public String getAccountName() {
            return accountName == null ? "" : accountName;
        }

        public String getCharacterName() {
            return characterName == null ? "" : characterName;
        }

        public String getOriginalMessage() {
            return originalMessage == null ? "" : originalMessage;
        }

        public String getDeliveredMessage() {
            return deliveredMessage == null ? "" : deliveredMessage;
        }

        public boolean isFiltered() {
            return filtered;
        }

        public List<String> getMatchedWords() {
            return matchedWords == null ? List.of() : new ArrayList<>(matchedWords);
        }

        public int getTargetCount() {
            return Math.max(targetCount, 0);
        }

        public static ChatLogEntryData create(
                long timestampMillis,
                String channelId,
                String accountName,
                String characterName,
                String originalMessage,
                String deliveredMessage,
                List<String> matchedWords,
                int targetCount) {
            ChatLogEntryData entry = new ChatLogEntryData();
            entry.timestampMillis = timestampMillis;
            entry.channelId = channelId == null ? "" : channelId;
            entry.accountName = accountName == null ? "" : accountName;
            entry.characterName = characterName == null ? "" : characterName;
            entry.originalMessage = originalMessage == null ? "" : originalMessage;
            entry.deliveredMessage = deliveredMessage == null ? "" : deliveredMessage;
            entry.filtered = !entry.originalMessage.equals(entry.deliveredMessage);
            entry.matchedWords = sanitizeMatchedWords(matchedWords);
            entry.targetCount = Math.max(targetCount, 0);
            return entry;
        }

        private static List<String> sanitizeMatchedWords(List<String> matchedWords) {
            if (matchedWords == null || matchedWords.isEmpty()) {
                return new ArrayList<>();
            }

            LinkedHashSet<String> uniqueWords = new LinkedHashSet<>();
            for (String matchedWord : matchedWords) {
                if (matchedWord == null) {
                    continue;
                }

                String normalized = matchedWord.trim();
                if (!normalized.isEmpty()) {
                    uniqueWords.add(normalized);
                }
            }
            return new ArrayList<>(uniqueWords);
        }
    }
}