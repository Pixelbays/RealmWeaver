package org.pixelbays.rpg.chat.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ChatChannelDefinition {

    public static final String DEFAULT_PARTY_FORMAT_TRANSLATION_KEY = "pixelbays.rpg.chat.party.message";
    public static final String DEFAULT_GUILD_FORMAT_TRANSLATION_KEY = "pixelbays.rpg.chat.guild.message";
    public static final String DEFAULT_GLOBAL_FORMAT_TRANSLATION_KEY = "pixelbays.rpg.chat.global.message";
    public static final String DEFAULT_ZONE_FORMAT_TRANSLATION_KEY = "pixelbays.rpg.chat.zone.message";
    public static final String DEFAULT_PROXIMITY_FORMAT_TRANSLATION_KEY = "pixelbays.rpg.chat.proximity.message";

    private static final String TRANSLATION_NAMESPACE_PREFIX = "pixelbays.";

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    public static final BuilderCodec<ChatChannelDefinition> CODEC = BuilderCodec
            .builder(ChatChannelDefinition.class, ChatChannelDefinition::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("Type", new EnumCodec<>(ChannelType.class), false, true),
                    (i, s) -> i.type = s, i -> i.type)
            .add()
            .append(new KeyedCodec<>("Id", Codec.STRING, false, true),
                    (i, s) -> i.id = s, i -> i.id)
            .add()
            .append(new KeyedCodec<>("Aliases", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.aliases = s, i -> i.aliases)
            .add()
                .append(new KeyedCodec<>("Color", Codec.STRING, false, true),
                    (i, s) -> i.color = s, i -> i.color)
            .add()
            .append(new KeyedCodec<>("NameType", new EnumCodec<>(NameDisplayType.class), false, true),
                    (i, s) -> i.nameType = s, i -> i.nameType)
            .add()
                .append(new KeyedCodec<>("RangeBlocks", Codec.INTEGER, false, true),
                    (i, s) -> i.rangeBlocks = s, i -> i.rangeBlocks)
                .add()
            .append(new KeyedCodec<>("FormatTranslationKey", Codec.STRING, false, true),
                    (i, s) -> i.formatTranslationKey = s, i -> i.formatTranslationKey)
            .add()
            .build();

    public enum ChannelType {
        Party,
        Guild,
        Global,
        Zone,
        Proximity
    }

    public enum NameDisplayType {
        AccountName,
        CharacterName;

        public boolean usesCharacterName() {
            return this == CharacterName;
        }
    }

    private boolean enabled;
    private ChannelType type;
    private String id;
    private List<String> aliases;
    private String color;
    private NameDisplayType nameType;
    private int rangeBlocks;
    private String formatTranslationKey;

    public ChatChannelDefinition() {
        this.enabled = true;
        this.type = ChannelType.Party;
        this.id = "party";
        this.aliases = new ArrayList<>();
        this.color = "";
        this.nameType = NameDisplayType.AccountName;
        this.rangeBlocks = 0;
        this.formatTranslationKey = DEFAULT_PARTY_FORMAT_TRANSLATION_KEY;
    }

    public static ChatChannelDefinition builtIn(ChannelType type, String id, List<String> aliases,
            String formatTranslationKey) {
        return builtIn(type, id, aliases, formatTranslationKey, "", NameDisplayType.AccountName, 0);
    }

    public static ChatChannelDefinition builtIn(ChannelType type, String id, List<String> aliases,
            String formatTranslationKey, String color, NameDisplayType nameType) {
        return builtIn(type, id, aliases, formatTranslationKey, color, nameType, 0);
    }

    public static ChatChannelDefinition builtIn(ChannelType type, String id, List<String> aliases,
            String formatTranslationKey, String color, NameDisplayType nameType, int rangeBlocks) {
        ChatChannelDefinition definition = new ChatChannelDefinition();
        definition.type = type;
        definition.id = id;
        definition.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        definition.color = normalizeColor(color);
        definition.nameType = nameType == null ? NameDisplayType.AccountName : nameType;
        definition.rangeBlocks = normalizeRangeBlocks(rangeBlocks);
        definition.formatTranslationKey = normalizeFormatTranslationKey(formatTranslationKey, type);
        return definition;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ChannelType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public List<String> getAliases() {
        return aliases != null ? aliases : List.of();
    }

    public String getColor() {
        return normalizeColor(color);
    }

    public NameDisplayType getNameDisplayType() {
        return nameType != null ? nameType : NameDisplayType.AccountName;
    }

    public int getRangeBlocks() {
        return normalizeRangeBlocks(rangeBlocks);
    }

    public String getFormatTranslationKey() {
        return normalizeFormatTranslationKey(formatTranslationKey, type);
    }

    private static int normalizeRangeBlocks(int rangeBlocks) {
        return Math.max(rangeBlocks, 0);
    }

    private static String normalizeColor(String color) {
        String normalized = color == null ? "" : color.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        return normalized.startsWith("#") ? normalized : "#" + normalized;
    }

    private static String normalizeFormatTranslationKey(String formatTranslationKey, ChannelType channelType) {
        String normalized = formatTranslationKey == null ? "" : formatTranslationKey.trim();
        if (normalized.isEmpty()) {
            return defaultFormatTranslationKey(channelType);
        }

        return normalized.startsWith(TRANSLATION_NAMESPACE_PREFIX)
                ? normalized
                : TRANSLATION_NAMESPACE_PREFIX + normalized;
    }

    private static String defaultFormatTranslationKey(ChannelType channelType) {
        return switch (channelType) {
            case Guild -> DEFAULT_GUILD_FORMAT_TRANSLATION_KEY;
            case Global -> DEFAULT_GLOBAL_FORMAT_TRANSLATION_KEY;
            case Zone -> DEFAULT_ZONE_FORMAT_TRANSLATION_KEY;
            case Proximity -> DEFAULT_PROXIMITY_FORMAT_TRANSLATION_KEY;
            case Party -> DEFAULT_PARTY_FORMAT_TRANSLATION_KEY;
        };
    }
}