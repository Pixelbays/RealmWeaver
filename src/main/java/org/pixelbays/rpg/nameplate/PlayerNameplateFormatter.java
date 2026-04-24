package org.pixelbays.rpg.nameplate;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.settings.NameplateModSettings;

public final class PlayerNameplateFormatter {

    private PlayerNameplateFormatter() {
    }

    @Nonnull
    public static String format(@Nonnull NameplateModSettings settings, @Nonnull Content content) {
        List<String> lines = new ArrayList<>();
        List<String> mainParts = new ArrayList<>();
        List<String> bottomLines = new ArrayList<>();

        String resolvedName = settings.isShowName() ? resolveName(settings, content) : "";
        String legacyTitle = sanitize(content.legacyTitle());
        String titlePrefix = sanitize(content.titlePrefix());
        String titleSuffix = sanitize(content.titleSuffix());
        String guildName = sanitize(content.guildName());
        String guildTag = formatGuildTag(content.guildTag());

        if (settings.isShowTitle() && !legacyTitle.isBlank()) {
            switch (settings.getTitlePlacement()) {
                case Prefix -> mainParts.add(legacyTitle);
                case Suffix -> {
                }
                case AboveName -> lines.add(legacyTitle);
                case BelowName -> bottomLines.add(legacyTitle);
                default -> {
                }
            }
        }

        if (settings.isShowTitle() && !titlePrefix.isBlank()) {
            mainParts.add(titlePrefix);
        }

        if (settings.isShowGuildTag() && !guildTag.isBlank()
                && settings.getGuildTagPlacement() == NameplateModSettings.GuildTagPlacement.Prefix) {
            mainParts.add(guildTag);
        }

        if (!resolvedName.isBlank()) {
            mainParts.add(resolvedName);
        }

        if (settings.isShowTitle() && !legacyTitle.isBlank()
                && settings.getTitlePlacement() == NameplateModSettings.TitlePlacement.Suffix) {
            mainParts.add(legacyTitle);
        }

        if (settings.isShowTitle() && !titleSuffix.isBlank()) {
            mainParts.add(titleSuffix);
        }

        if (settings.isShowGuildTag() && !guildTag.isBlank()
                && settings.getGuildTagPlacement() == NameplateModSettings.GuildTagPlacement.Suffix) {
            mainParts.add(guildTag);
        }

        if (settings.isShowGuildName() && !guildName.isBlank()) {
            if (settings.getGuildNamePlacement() == NameplateModSettings.GuildNamePlacement.AboveName) {
                lines.add(guildName);
            } else {
                bottomLines.add(guildName);
            }
        }

        String mainLine = joinNonBlank(mainParts);
        if (!mainLine.isBlank()) {
            lines.add(mainLine);
        }

        for (String line : bottomLines) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        return String.join("\n", lines);
    }

    @Nonnull
    private static String resolveName(@Nonnull NameplateModSettings settings, @Nonnull Content content) {
        String accountName = sanitize(content.accountName());
        String characterName = sanitize(content.characterName());

        if (settings.getNameSource() == NameplateModSettings.NameSource.AccountName) {
            return accountName.isBlank() ? characterName : accountName;
        }
        return characterName.isBlank() ? accountName : characterName;
    }

    @Nonnull
    private static String formatGuildTag(@Nullable String rawGuildTag) {
        String guildTag = sanitize(rawGuildTag);
        if (guildTag.isBlank()) {
            return "";
        }
        return "[" + guildTag + "]";
    }

    @Nonnull
    private static String joinNonBlank(@Nonnull List<String> parts) {
        List<String> filtered = new ArrayList<>();
        for (String part : parts) {
            String sanitized = sanitize(part);
            if (!sanitized.isBlank()) {
                filtered.add(sanitized);
            }
        }
        return filtered.isEmpty() ? "" : String.join(" ", filtered);
    }

    @Nonnull
    private static String sanitize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    public record Content(
            @Nullable String accountName,
            @Nullable String characterName,
            @Nullable String legacyTitle,
            @Nullable String titlePrefix,
            @Nullable String titleSuffix,
            @Nullable String guildName,
            @Nullable String guildTag) {
    }
}