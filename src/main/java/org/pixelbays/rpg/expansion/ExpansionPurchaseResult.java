package org.pixelbays.rpg.expansion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;

public class ExpansionPurchaseResult {

    public enum Status {
        Success,
        UnknownExpansion,
        Disabled,
        AlreadyOwned,
        FreeAccess,
        FreeNotLive,
        AccessGrantedByPermission,
        CurrencyModuleDisabled,
        InvalidPurchaseScope,
        OwnerUnavailable,
        CurrencyFailed
    }

    private final Status status;
    private final GeneralModSettings.ExpansionDefinition expansion;
    private final CurrencyActionResult currencyResult;

    private ExpansionPurchaseResult(@Nonnull Status status,
            @Nullable GeneralModSettings.ExpansionDefinition expansion,
            @Nullable CurrencyActionResult currencyResult) {
        this.status = status;
        this.expansion = expansion;
        this.currencyResult = currencyResult;
    }

    public static ExpansionPurchaseResult of(@Nonnull Status status,
            @Nullable GeneralModSettings.ExpansionDefinition expansion) {
        return new ExpansionPurchaseResult(status, expansion, null);
    }

    public static ExpansionPurchaseResult currencyFailure(
            @Nullable GeneralModSettings.ExpansionDefinition expansion,
            @Nonnull CurrencyActionResult currencyResult) {
        return new ExpansionPurchaseResult(Status.CurrencyFailed, expansion, currencyResult);
    }

    public boolean isSuccess() {
        return status == Status.Success;
    }

    @Nonnull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public GeneralModSettings.ExpansionDefinition getExpansion() {
        return expansion;
    }

    @Nullable
    public CurrencyActionResult getCurrencyResult() {
        return currencyResult;
    }
}
