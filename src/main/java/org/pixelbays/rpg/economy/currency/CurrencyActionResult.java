package org.pixelbays.rpg.economy.currency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CurrencyActionResult {

    private final boolean success;
    private final String message;
    private final String currencyId;
    private final long balance;

    private CurrencyActionResult(boolean success, @Nonnull String message, @Nullable String currencyId, long balance) {
        this.success = success;
        this.message = message;
        this.currencyId = currencyId;
        this.balance = balance;
    }

    @Nonnull
    public static CurrencyActionResult success(@Nonnull String message, @Nullable String currencyId, long balance) {
        return new CurrencyActionResult(true, message, currencyId, balance);
    }

    @Nonnull
    public static CurrencyActionResult failure(@Nonnull String message) {
        return new CurrencyActionResult(false, message, null, 0L);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getCurrencyId() {
        return currencyId;
    }

    public long getBalance() {
        return balance;
    }
}
