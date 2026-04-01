package org.pixelbays.rpg.chat.config.settings;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ChatModSettings {

    private static final FunctionCodec<ChatChannelDefinition[], List<ChatChannelDefinition>> CHANNEL_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ChatChannelDefinition.CODEC, ChatChannelDefinition[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(List.of(arr)),
            list -> list == null ? null : list.toArray(ChatChannelDefinition[]::new));

    public static final BuilderCodec<ChatModSettings> CODEC = BuilderCodec
            .builder(ChatModSettings.class, ChatModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("DefaultFilterEnabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.defaultFilterEnabled = s, i -> i.defaultFilterEnabled)
            .add()
            .append(new KeyedCodec<>("CustomFilterEnabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.customFilterEnabled = s, i -> i.customFilterEnabled)
            .add()
            .append(new KeyedCodec<>("ChatLoggingEnabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.chatLoggingEnabled = s, i -> i.chatLoggingEnabled)
            .add()
            .append(new KeyedCodec<>("ChatLogMaxEntriesPerPlayer", Codec.INTEGER, false, true),
                (i, s) -> i.chatLogMaxEntriesPerPlayer = s, i -> i.chatLogMaxEntriesPerPlayer)
            .add()
            .append(new KeyedCodec<>("BaseChatProximityEnabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.baseChatProximityEnabled = s, i -> i.baseChatProximityEnabled)
            .add()
            .append(new KeyedCodec<>("BaseChatRangeBlocks", Codec.INTEGER, false, true),
                (i, s) -> i.baseChatRangeBlocks = s, i -> i.baseChatRangeBlocks)
            .add()
            .append(new KeyedCodec<>("Channels", CHANNEL_LIST_CODEC, false, true),
                    (i, s) -> i.channels = s, i -> i.channels)
            .add()
            .build();

    private boolean enabled;
    private boolean defaultFilterEnabled;
    private boolean customFilterEnabled;
    private boolean chatLoggingEnabled;
    private int chatLogMaxEntriesPerPlayer;
    private boolean baseChatProximityEnabled;
    private int baseChatRangeBlocks;
    private List<ChatChannelDefinition> channels;

    public ChatModSettings() {
        this.enabled = true;
        this.defaultFilterEnabled = false;
        this.customFilterEnabled = false;
        this.chatLoggingEnabled = false;
        this.chatLogMaxEntriesPerPlayer = 250;
        this.baseChatProximityEnabled = false;
        this.baseChatRangeBlocks = 24;
        this.channels = new ArrayList<>();
        this.channels.add(ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Party,
                "party",
                List.of("p"),
            ChatChannelDefinition.DEFAULT_PARTY_FORMAT_TRANSLATION_KEY,
            "#79C8FF",
            ChatChannelDefinition.NameDisplayType.AccountName));
        this.channels.add(ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Guild,
                "guild",
                List.of("g"),
            ChatChannelDefinition.DEFAULT_GUILD_FORMAT_TRANSLATION_KEY,
            "#66D48F",
            ChatChannelDefinition.NameDisplayType.AccountName));
        this.channels.add(ChatChannelDefinition.builtIn(
            ChatChannelDefinition.ChannelType.Global,
            "global",
            List.of("all"),
            ChatChannelDefinition.DEFAULT_GLOBAL_FORMAT_TRANSLATION_KEY,
            "#E0C061",
            ChatChannelDefinition.NameDisplayType.AccountName));
        this.channels.add(ChatChannelDefinition.builtIn(
            ChatChannelDefinition.ChannelType.Zone,
            "zone",
            List.of("z"),
            ChatChannelDefinition.DEFAULT_ZONE_FORMAT_TRANSLATION_KEY,
            "#B78CFF",
            ChatChannelDefinition.NameDisplayType.AccountName));
        this.channels.add(ChatChannelDefinition.builtIn(
            ChatChannelDefinition.ChannelType.Proximity,
            "local",
            List.of("l", "say"),
            ChatChannelDefinition.DEFAULT_PROXIMITY_FORMAT_TRANSLATION_KEY,
            "#F1A763",
            ChatChannelDefinition.NameDisplayType.AccountName,
            18));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultFilterEnabled() {
        return defaultFilterEnabled;
    }

    public boolean isCustomFilterEnabled() {
        return customFilterEnabled;
    }

    public boolean isChatLoggingEnabled() {
        return chatLoggingEnabled;
    }

    public int getChatLogMaxEntriesPerPlayer() {
        return Math.max(chatLogMaxEntriesPerPlayer, 1);
    }

    public boolean isBaseChatProximityEnabled() {
        return baseChatProximityEnabled;
    }

    public int getBaseChatRangeBlocks() {
        return Math.max(baseChatRangeBlocks, 0);
    }

    public List<ChatChannelDefinition> getChannels() {
        return channels != null ? channels : List.of();
    }

    public boolean hasEnabledChannels() {
        for (ChatChannelDefinition channel : getChannels()) {
            if (channel != null && channel.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOperationalFeatures() {
        return hasEnabledChannels()
                || isBaseChatProximityEnabled()
                || isDefaultFilterEnabled()
                || isCustomFilterEnabled()
                || isChatLoggingEnabled();
    }
}