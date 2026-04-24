package org.pixelbays.rpg.nameplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.pixelbays.rpg.global.config.settings.NameplateModSettings;

class PlayerNameplateFormatterTest {

    @Test
    void format_prefersCharacterNameAndAddsConfiguredDefaults() {
        NameplateModSettings settings = new NameplateModSettings();

        String formatted = PlayerNameplateFormatter.format(
                settings,
            new PlayerNameplateFormatter.Content("AccountHero", "CharacterHero", "Dragonslayer", "", "", "Skyward", "SW"));

        assertEquals("Dragonslayer CharacterHero [SW]", formatted);
    }

    @Test
    void format_fallsBackToAccountNameWhenCharacterNameMissing() {
        NameplateModSettings settings = new NameplateModSettings();

        String formatted = PlayerNameplateFormatter.format(
                settings,
            new PlayerNameplateFormatter.Content("AccountHero", "", "", "", "", "", ""));

        assertEquals("AccountHero", formatted);
    }

    @Test
    void format_supportsGuildNameAboveAndTagPrefix() {
        NameplateModSettings settings = new NameplateModSettings();
        setField(settings, "showGuildName", true);
        setField(settings, "guildNamePlacement", NameplateModSettings.GuildNamePlacement.AboveName);
        setField(settings, "guildTagPlacement", NameplateModSettings.GuildTagPlacement.Prefix);
        setField(settings, "titlePlacement", NameplateModSettings.TitlePlacement.Suffix);

        String formatted = PlayerNameplateFormatter.format(
                settings,
                new PlayerNameplateFormatter.Content("AccountHero", "CharacterHero", "Warden", "", "", "Skyward", "SW"));

        assertEquals("Skyward\n[SW] CharacterHero Warden", formatted);
    }

    @Test
    void format_supportsTitleDefinedPrefixAndSuffix() {
        NameplateModSettings settings = new NameplateModSettings();

        String formatted = PlayerNameplateFormatter.format(
                settings,
                new PlayerNameplateFormatter.Content("AccountHero", "CharacterHero", "", "Lord", "the Wise", "Skyward", "SW"));

        assertEquals("Lord CharacterHero the Wise [SW]", formatted);
    }

    @Test
    void format_canHideNameAndKeepSecondaryLines() {
        NameplateModSettings settings = new NameplateModSettings();
        setField(settings, "showName", false);
        setField(settings, "titlePlacement", NameplateModSettings.TitlePlacement.BelowName);
        setField(settings, "showGuildName", true);

        String formatted = PlayerNameplateFormatter.format(
                settings,
            new PlayerNameplateFormatter.Content("AccountHero", "CharacterHero", "Warden", "", "", "Skyward", "SW"));

        assertEquals("[SW]\nWarden\nSkyward", formatted);
    }

    private static void setField(NameplateModSettings settings, String fieldName, Object value) {
        try {
            var field = NameplateModSettings.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(settings, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}