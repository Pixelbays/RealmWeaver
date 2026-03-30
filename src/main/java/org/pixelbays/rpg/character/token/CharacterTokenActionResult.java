package org.pixelbays.rpg.character.token;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CharacterTokenActionResult {

    private final boolean success;
    private final String message;
    private final String tokenId;
    private final long balance;

    private CharacterTokenActionResult(boolean success,
            @Nonnull String message,
            @Nullable String tokenId,
            long balance) {
        this.success = success;
        this.message = message;
        this.tokenId = tokenId;
        this.balance = balance;
    }

    @Nonnull
    public static CharacterTokenActionResult success(@Nonnull String message, @Nullable String tokenId, long balance) {
        return new CharacterTokenActionResult(true, message, tokenId, balance);
    }

    @Nonnull
    public static CharacterTokenActionResult failure(@Nonnull String message, @Nullable String tokenId) {
        return new CharacterTokenActionResult(false, message, tokenId, 0L);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nonnull
    public String getMessage() {
        return message == null ? "" : message;
    }

    @Nullable
    public String getTokenId() {
        return tokenId;
    }

    public long getBalance() {
        return balance;
    }
}