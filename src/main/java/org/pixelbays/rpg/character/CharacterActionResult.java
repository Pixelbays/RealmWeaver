package org.pixelbays.rpg.character;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.config.CharacterProfileData;

public final class CharacterActionResult {

    private final boolean success;
    private final String message;
    private final CharacterProfileData profile;

    private CharacterActionResult(boolean success, @Nonnull String message, @Nullable CharacterProfileData profile) {
        this.success = success;
        this.message = message;
        this.profile = profile;
    }

    @Nonnull
    public static CharacterActionResult success(@Nonnull String message) {
        return new CharacterActionResult(true, message, null);
    }

    @Nonnull
    public static CharacterActionResult success(@Nonnull String message, @Nullable CharacterProfileData profile) {
        return new CharacterActionResult(true, message, profile);
    }

    @Nonnull
    public static CharacterActionResult failure(@Nonnull String message) {
        return new CharacterActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nonnull
    public String getMessage() {
        return message == null ? "" : message;
    }

    @Nullable
    public CharacterProfileData getProfile() {
        return profile;
    }
}
