package org.pixelbays.rpg.economy.banks;

import javax.annotation.Nullable;

public class BankActionResult {

    private final boolean success;
    private final String message;
    private final BankAccount bankAccount;

    private BankActionResult(boolean success, String message, @Nullable BankAccount bankAccount) {
        this.success = success;
        this.message = message;
        this.bankAccount = bankAccount;
    }

    public static BankActionResult success(String message, BankAccount bankAccount) {
        return new BankActionResult(true, message, bankAccount);
    }

    public static BankActionResult success(String message) {
        return new BankActionResult(true, message, null);
    }

    public static BankActionResult failure(String message) {
        return new BankActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public BankAccount getBankAccount() {
        return bankAccount;
    }
}
