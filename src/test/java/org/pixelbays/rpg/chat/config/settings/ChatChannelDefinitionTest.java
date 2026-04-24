package org.pixelbays.rpg.chat.config.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChatChannelDefinitionTest {

    @Test
    void builtIn_globalUsesNamespacedDefaultTranslationKey() {
        ChatChannelDefinition definition = ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Global,
                "global",
                List.of("all"),
                "");

        assertEquals(ChatChannelDefinition.DEFAULT_GLOBAL_FORMAT_TRANSLATION_KEY, definition.getFormatTranslationKey());
    }

    @Test
    void getColor_addsLeadingHashWhenMissing() {
        ChatChannelDefinition definition = ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Party,
                "party",
                List.of("p"),
                ChatChannelDefinition.DEFAULT_PARTY_FORMAT_TRANSLATION_KEY,
                "79C8FF",
                ChatChannelDefinition.NameDisplayType.AccountName);

        assertEquals("#79C8FF", definition.getColor());
    }

    @Test
    void builtIn_defaultsNameTypeToAccountName() {
        ChatChannelDefinition definition = ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Zone,
                "zone",
                List.of("z"),
                ChatChannelDefinition.DEFAULT_ZONE_FORMAT_TRANSLATION_KEY);

        assertEquals(ChatChannelDefinition.NameDisplayType.AccountName, definition.getNameDisplayType());
    }

    @Test
    void builtIn_proximityKeepsConfiguredRange() {
        ChatChannelDefinition definition = ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Proximity,
                "local",
                List.of("l"),
                ChatChannelDefinition.DEFAULT_PROXIMITY_FORMAT_TRANSLATION_KEY,
                "#F1A763",
                ChatChannelDefinition.NameDisplayType.AccountName,
                18);

        assertEquals(18, definition.getRangeBlocks());
    }
}