package org.pixelbays.rpg.guild.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.GuildRole;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

final class GuildPageSupport {

    private static final String DEFAULT_LANGUAGE = "en-US";

    private GuildPageSupport() {
    }

    static void setLocalizedText(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull String selector,
            @Nonnull String messageId) {
        commandBuilder.setObject(selector + ".Text", LocalizableString.fromMessageId(messageId, null));
    }

    static void setLocalizedText(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull String selector,
            @Nonnull String messageId,
            @Nonnull Map<String, String> params) {
        commandBuilder.set(selector + ".Text", resolveLocalizedText(messageId, params));
    }

    @Nonnull
    static String resolveLocalizedText(@Nonnull String messageId, @Nonnull Map<String, String> params) {
        String translated = resolveTranslation(messageId, "");
        for (var entry : params.entrySet()) {
            translated = translated.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return translated;
    }

    @Nonnull
    static String rawText(@Nonnull String messageId, @Nonnull String fallback) {
        return resolveTranslation(messageId, fallback);
    }

    @Nonnull
    static String resolveTranslation(@Nonnull String messageId, @Nonnull String fallback) {
        I18nModule i18n = I18nModule.get();
        if (i18n == null) {
            return fallback;
        }

        String translated = i18n.getMessage(DEFAULT_LANGUAGE, messageId);
        if (translated != null && !translated.isBlank()) {
            return translated;
        }

        if (messageId.startsWith("pixelbays.")) {
            String unscopedKey = messageId.substring("pixelbays.".length());
            translated = i18n.getMessage(DEFAULT_LANGUAGE, unscopedKey);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }

        return fallback;
    }

    @Nullable
    static RpgModConfig resolveConfig() {
        try {
            var assetMap = RpgModConfig.getAssetMap();
            if (assetMap == null) {
                return null;
            }

            RpgModConfig config = assetMap.getAsset("Default");
            return config != null ? config : assetMap.getAsset("default");
        } catch (Throwable ex) {
            return null;
        }
    }

    @Nullable
    static String normalizeSelectionValue(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    static UUID resolveGuildTargetUuid(@Nullable String targetSelection) {
        String normalized = normalizeSelectionValue(targetSelection);
        if (normalized == null) {
            return null;
        }

        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException ignored) {
            PlayerRef targetRef = GuildCommandUtil.findPlayerByName(normalized);
            return targetRef == null ? null : targetRef.getUuid();
        }
    }

    @Nullable
    static String normalizeRoleId(@Nullable String roleName) {
        if (roleName == null) {
            return null;
        }

        String input = roleName.trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        boolean lastUnderscore = false;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
                lastUnderscore = false;
            } else if (!lastUnderscore) {
                builder.append('_');
                lastUnderscore = true;
            }
        }

        String roleId = builder.toString().replaceAll("_+", "_");
        while (roleId.startsWith("_")) {
            roleId = roleId.substring(1);
        }
        while (roleId.endsWith("_")) {
            roleId = roleId.substring(0, roleId.length() - 1);
        }
        return roleId.isEmpty() ? null : roleId;
    }

    @Nonnull
    static List<GuildRole> getSortedRoles(@Nonnull Guild guild) {
        List<GuildRole> roles = new java.util.ArrayList<>(guild.getRoles().values());
        roles.sort(java.util.Comparator.comparing(GuildRole::getId, String.CASE_INSENSITIVE_ORDER));
        return roles;
    }

    static int resolveRoleTabPage(@Nullable Guild guild, @Nullable String roleId, int pageSize) {
        if (guild == null || roleId == null || roleId.isBlank()) {
            return 0;
        }

        List<GuildRole> roles = getSortedRoles(guild);
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).getId().equalsIgnoreCase(roleId)) {
                return i / pageSize;
            }
        }

        return 0;
    }

    @Nonnull
    static String joinPolicyDisplayName(@Nonnull GuildJoinPolicy joinPolicy) {
        return switch (joinPolicy) {
            case INVITE_ONLY -> rawText("pixelbays.rpg.guild.ui.policyInviteOnly", "Invite Only");
            case OPEN -> rawText("pixelbays.rpg.guild.ui.policyOpen", "Open");
            case APPLICATION -> rawText("pixelbays.rpg.guild.ui.policyApplication", "Application");
        };
    }

    @Nonnull
    static String formatRemainingTime(long expiresAtMillis) {
        if (expiresAtMillis <= 0L) {
            return "Never";
        }

        long remainingMillis = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
        long totalMinutes = remainingMillis / 60_000L;
        if (totalMinutes >= 60L) {
            long hours = totalMinutes / 60L;
            long minutes = totalMinutes % 60L;
            return minutes == 0L ? hours + "h" : hours + "h " + minutes + "m";
        }
        return totalMinutes + "m";
    }

    @Nonnull
    static String formatElapsedTime(long createdAtMillis) {
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - createdAtMillis);
        long totalMinutes = elapsedMillis / 60_000L;
        if (totalMinutes >= 60L) {
            long hours = totalMinutes / 60L;
            long minutes = totalMinutes % 60L;
            return minutes == 0L ? hours + "h ago" : hours + "h " + minutes + "m ago";
        }
        return totalMinutes + "m ago";
    }

    @Nonnull
    static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
    }

    @Nullable
    static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue sp) {
            return sp.value;
        }
        if (value instanceof IntParamValue ip) {
            return String.valueOf(ip.value);
        }
        if (value instanceof LongParamValue lp) {
            return String.valueOf(lp.value);
        }
        if (value instanceof DoubleParamValue dp) {
            return String.valueOf(dp.value);
        }
        if (value instanceof BoolParamValue bp) {
            return String.valueOf(bp.value);
        }
        return String.valueOf(value);
    }

    @Nullable
    static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = firstQuote + 1; i < rawData.length(); i++) {
            char current = rawData.charAt(i);
            if (escaping) {
                switch (current) {
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case '"', '\\', '/' -> builder.append(current);
                    default -> builder.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            if (current == '"') {
                return builder.toString();
            }

            builder.append(current);
        }

        return null;
    }

    @Nullable
    static Boolean extractBoolean(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < rawData.length() && Character.isWhitespace(rawData.charAt(valueStart))) {
            valueStart++;
        }

        if (rawData.startsWith("true", valueStart)) {
            return Boolean.TRUE;
        }
        if (rawData.startsWith("false", valueStart)) {
            return Boolean.FALSE;
        }

        return null;
    }
}
