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
            .append(new KeyedCodec<>("FormatTranslationKey", Codec.STRING, false, true),
                    (i, s) -> i.formatTranslationKey = s, i -> i.formatTranslationKey)
            .add()
            .build();

    public enum ChannelType {
        Party,
        Guild
    }

    private boolean enabled;
    private ChannelType type;
    private String id;
    private List<String> aliases;
    private String formatTranslationKey;

    public ChatChannelDefinition() {
        this.enabled = true;
        this.type = ChannelType.Party;
        this.id = "party";
        this.aliases = new ArrayList<>();
        this.formatTranslationKey = "rpg.chat.party.message";
    }

    public static ChatChannelDefinition builtIn(ChannelType type, String id, List<String> aliases,
            String formatTranslationKey) {
        ChatChannelDefinition definition = new ChatChannelDefinition();
        definition.type = type;
        definition.id = id;
        definition.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        definition.formatTranslationKey = formatTranslationKey;
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

    public String getFormatTranslationKey() {
        return formatTranslationKey;
    }
}