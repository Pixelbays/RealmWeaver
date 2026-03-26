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
            .append(new KeyedCodec<>("Channels", CHANNEL_LIST_CODEC, false, true),
                    (i, s) -> i.channels = s, i -> i.channels)
            .add()
            .build();

    private boolean enabled;
    private List<ChatChannelDefinition> channels;

    public ChatModSettings() {
        this.enabled = true;
        this.channels = new ArrayList<>();
        this.channels.add(ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Party,
                "party",
                List.of("p"),
                "rpg.chat.party.message"));
        this.channels.add(ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Guild,
                "guild",
                List.of("g"),
                "rpg.chat.guild.message"));
    }

    public boolean isEnabled() {
        return enabled;
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
}