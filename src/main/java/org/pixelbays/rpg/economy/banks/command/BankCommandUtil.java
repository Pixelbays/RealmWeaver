package org.pixelbays.rpg.economy.banks.command;

import org.pixelbays.rpg.economy.banks.config.BankScope;

import com.hypixel.hytale.server.core.Message;

public final class BankCommandUtil {

    private BankCommandUtil() {
    }

    @SuppressWarnings("null")
    public static Message managerResultMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        return switch (message) {
            case "Owner id cannot be empty." -> Message.translation("pixelbays.rpg.bank.error.ownerRequired");
            case "Loaded existing bank." -> Message.translation("pixelbays.rpg.bank.success.loaded");
            case "Created bank." -> Message.translation("pixelbays.rpg.bank.success.created");
            case "Unlocked bank tab." -> Message.translation("pixelbays.rpg.bank.success.tabUnlocked");
            case "Deposited bank currency." -> Message.translation("pixelbays.rpg.bank.success.currencyDeposited");
            case "Withdrew bank currency." -> Message.translation("pixelbays.rpg.bank.success.currencyWithdrew");
            case "No locked bank tabs available." -> Message.translation("pixelbays.rpg.bank.error.noLockedTabs");
            case "Bank does not support stored currency." -> Message.translation("pixelbays.rpg.bank.error.currencyStorageUnavailable");
            case "Currency amount must be greater than zero." -> Message.translation("pixelbays.rpg.bank.error.currencyAmountRequired");
            case "Currency source unavailable for bank scope." -> Message.translation("pixelbays.rpg.bank.error.currencySourceUnavailable");
            default -> {
                if (message.startsWith("Unknown bank type: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.unknownBankType")
                            .param("type", message.substring("Unknown bank type: ".length()));
                }
                if (message.startsWith("Bank type is disabled: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.bankTypeDisabled")
                            .param("type", message.substring("Bank type is disabled: ".length()));
                }
                if (message.startsWith("No configured bank type for scope ")) {
                    String scope = message.substring("No configured bank type for scope ".length(), message.length() - 1);
                    yield Message.translation("pixelbays.rpg.bank.error.noConfiguredType")
                            .param("scope", scope.toLowerCase());
                }
                if (message.startsWith("Insufficient bank cost: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.insufficientCost")
                            .param("cost", message.substring("Insufficient bank cost: ".length()));
                }
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
                if (message.startsWith("Insufficient currency: ")) {
                    yield Message.translation("pixelbays.rpg.currency.error.insufficient")
                            .param("type", message.substring("Insufficient currency: ".length()));
                }
                if (message.startsWith("Bank cannot store currency type: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.currencyTypeUnsupported")
                            .param("type", message.substring("Bank cannot store currency type: ".length()));
                }
                if (message.startsWith("Bank currency storage is full for: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.currencyStorageFull")
                            .param("type", message.substring("Bank currency storage is full for: ".length()));
                }
                if (message.startsWith("Insufficient stored bank currency: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.currencyStoredInsufficient")
                            .param("type", message.substring("Insufficient stored bank currency: ".length()));
                }
                if (message.startsWith("Currency destination is full for: ")) {
                    yield Message.translation("pixelbays.rpg.bank.error.currencyDestinationFull")
                            .param("type", message.substring("Currency destination is full for: ".length()));
                }
                yield Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", message);
            }
        };
    }

    public static Message scopeMessage(BankScope scope) {
        return switch (scope) {
            case Character -> Message.translation("pixelbays.rpg.bank.scope.character");
            case Player -> Message.translation("pixelbays.rpg.bank.scope.player");
            case Account -> Message.translation("pixelbays.rpg.bank.scope.account");
            case Guild -> Message.translation("pixelbays.rpg.bank.scope.guild");
            case Warband -> Message.translation("pixelbays.rpg.bank.scope.warband");
            case Profession -> Message.translation("pixelbays.rpg.bank.scope.profession");
            case Global -> Message.translation("pixelbays.rpg.bank.scope.global");
            case Custom -> Message.translation("pixelbays.rpg.bank.scope.custom");
        };
    }
}
