package org.pixelbays.rpg.classes.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.config.ClassDefinition;

@SuppressWarnings("null")
public class ClassAbilityLearnResult {

    private final boolean success;
    @Nonnull
    private final String message;
    @Nonnull
    private final String classId;
    @Nonnull
    private final String abilityId;
    private final ClassDefinition.AbilityUnlock abilityUnlock;

    private ClassAbilityLearnResult(boolean success,
            @Nonnull String message,
            @Nonnull String classId,
            @Nonnull String abilityId,
            @Nullable ClassDefinition.AbilityUnlock abilityUnlock) {
        this.success = success;
        this.message = message;
        this.classId = classId;
        this.abilityId = abilityId;
        this.abilityUnlock = abilityUnlock;
    }

    @Nonnull
    public static ClassAbilityLearnResult success(@Nonnull String message,
            @Nonnull String classId,
            @Nonnull String abilityId,
            @Nullable ClassDefinition.AbilityUnlock abilityUnlock) {
        return new ClassAbilityLearnResult(true, message, classId, abilityId, abilityUnlock);
    }

    @Nonnull
    public static ClassAbilityLearnResult failure(@Nonnull String message,
            @Nonnull String classId,
            @Nonnull String abilityId) {
        return new ClassAbilityLearnResult(false, message, classId, abilityId, null);
    }

    @Nonnull
    public static ClassAbilityLearnResult failure(@Nonnull String message,
            @Nonnull String classId,
            @Nonnull String abilityId,
            @Nullable ClassDefinition.AbilityUnlock abilityUnlock) {
        return new ClassAbilityLearnResult(false, message, classId, abilityId, abilityUnlock);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    @Nonnull
    public String getClassId() {
        return classId;
    }

    @Nonnull
    public String getAbilityId() {
        return abilityId;
    }

    @Nullable
    public ClassDefinition.AbilityUnlock getAbilityUnlock() {
        return abilityUnlock;
    }
}