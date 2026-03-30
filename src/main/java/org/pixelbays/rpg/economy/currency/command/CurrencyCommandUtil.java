package org.pixelbays.rpg.economy.currency.command;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class CurrencyCommandUtil {

    private CurrencyCommandUtil() {
    }

    @SuppressWarnings("null")
    public static Message managerResultMessage(@Nonnull CurrencyActionResult result) {
        String message = result.getMessage();
        if (message == null || message.isEmpty()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        return switch (message) {
            case "Added currency." -> Message.translation("pixelbays.rpg.currency.success.added")
                    .param("currency", safe(result.getCurrencyId()))
                    .param("balance", Long.toString(result.getBalance()));
            case "Removed currency." -> Message.translation("pixelbays.rpg.currency.success.removed")
                    .param("currency", safe(result.getCurrencyId()))
                    .param("balance", Long.toString(result.getBalance()));
            case "Set currency." -> Message.translation("pixelbays.rpg.currency.success.set")
                    .param("currency", safe(result.getCurrencyId()))
                    .param("balance", Long.toString(result.getBalance()));
            case "Owner id cannot be empty." -> Message.translation("pixelbays.rpg.currency.error.ownerRequired");
            case "Amount must be zero or greater." -> Message.translation("pixelbays.rpg.currency.error.amountNonNegative");
            default -> {
                if (message.startsWith("Unknown currency type: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.unknownType")
                            .param("type", message.substring("Unknown currency type: ".length()));
                }
                if (message.startsWith("Currency type is disabled: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.disabledType")
                            .param("type", message.substring("Currency type is disabled: ".length()));
                }
                if (message.startsWith("Currency type does not support scope: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.unsupportedScope")
                            .param("scope", message.substring("Currency type does not support scope: ".length()).toLowerCase());
                }
                if (message.startsWith("Currency type cannot be modified directly: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.directModifyNotAllowed")
                            .param("type", message.substring("Currency type cannot be modified directly: ".length()));
                }
                if (message.startsWith("Insufficient currency: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.insufficient")
                            .param("type", message.substring("Insufficient currency: ".length()));
                }
                yield Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", message);
            }
        };
    }

    @Nullable
    public static CurrencyScope parseScope(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        for (CurrencyScope scope : CurrencyScope.values()) {
            if (scope.name().equalsIgnoreCase(raw)) {
                return scope;
            }
        }
        return null;
    }

    @Nullable
    public static String resolveOwnerId(@Nonnull CurrencyScope scope, @Nonnull PlayerRef playerRef) {
        return switch (scope) {
            case Character -> Realmweavers.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
            case Account -> String.valueOf(playerRef.getUuid());
            case Guild -> resolveGuildOwnerId(playerRef.getUuid().toString());
            case Global -> "global";
            case Custom -> null;
        };
    }

    @Nullable
    private static String resolveGuildOwnerId(@Nonnull String memberId) {
        GuildManager guildManager = Realmweavers.get().getGuildManager();
        Guild guild = guildManager.getGuildForMember(java.util.UUID.fromString(memberId));
        return guild == null ? null : String.valueOf(guild.getId());
    }

    public static Message scopeMessage(@Nonnull CurrencyScope scope) {
        return switch (scope) {
            case Character -> Message.translation("pixelbays.rpg.currency.scope.character");
            case Account -> Message.translation("pixelbays.rpg.currency.scope.account");
            case Guild -> Message.translation("pixelbays.rpg.currency.scope.guild");
            case Global -> Message.translation("pixelbays.rpg.currency.scope.global");
            case Custom -> Message.translation("pixelbays.rpg.currency.scope.custom");
        };
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
