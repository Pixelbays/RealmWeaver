package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

public final class ProgressionHudServiceModule implements PlayerHudServiceModule {

    private final LevelProgressionSystem levelSystem;

    public ProgressionHudServiceModule(@Nonnull LevelProgressionSystem levelSystem) {
        this.levelSystem = levelSystem;
    }

    @Override
    public void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        String activeClassId = context.getActiveClassId();
        if (activeClassId == null || activeClassId.isBlank()) {
            hud.getProgressionModule().primeHide();
            return;
        }

        String systemId = PlayerHudServiceSupport.resolveActiveClassSystemId(activeClassId);
        ClassDefinition classDef = PlayerHudServiceSupport.getClassDefinition(activeClassId);
        String labelPrefix = resolveActiveClassLabel(activeClassId, systemId, classDef);
        String fillColor = PlayerHudServiceSupport.resolveProgressionFillColor(classDef);
        int level = Math.max(1, levelSystem.getLevel(context.getRef(), systemId));

        float currentExp = levelSystem.getExperience(context.getRef(), systemId);
        float expToNext = levelSystem.getExpToNextLevel(context.getRef(), systemId);
        if (expToNext <= 0.0001f) {
            hud.getProgressionModule().primeMax(labelPrefix, level, fillColor);
            return;
        }

        int current = Math.max(0, Math.round(currentExp));
        int next = Math.max(0, Math.round(expToNext));
        int remaining = Math.max(0, next - current);
        hud.getProgressionModule().primeProgress(labelPrefix, level, currentExp / expToNext, current, next, remaining, fillColor);
    }

    @Override
    public void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        String activeClassId = context.getActiveClassId();
        if (activeClassId == null || activeClassId.isBlank()) {
            hud.getProgressionModule().hide();
            return;
        }

        String systemId = PlayerHudServiceSupport.resolveActiveClassSystemId(activeClassId);
        ClassDefinition classDef = PlayerHudServiceSupport.getClassDefinition(activeClassId);
        String labelPrefix = resolveActiveClassLabel(activeClassId, systemId, classDef);
        String fillColor = PlayerHudServiceSupport.resolveProgressionFillColor(classDef);
        int level = Math.max(1, levelSystem.getLevel(context.getRef(), systemId));

        float currentExp = levelSystem.getExperience(context.getRef(), systemId);
        float expToNext = levelSystem.getExpToNextLevel(context.getRef(), systemId);
        if (expToNext <= 0.0001f) {
            hud.getProgressionModule().updateMax(labelPrefix, level, fillColor);
            return;
        }

        int current = Math.max(0, Math.round(currentExp));
        int next = Math.max(0, Math.round(expToNext));
        int remaining = Math.max(0, next - current);
        hud.getProgressionModule().updateProgress(labelPrefix, level, currentExp / expToNext, current, next, remaining, fillColor);
    }

    @Nonnull
    private String resolveActiveClassLabel(@Nonnull String activeClassId, @Nonnull String resolvedSystemId) {
        return resolveActiveClassLabel(activeClassId, resolvedSystemId, PlayerHudServiceSupport.getClassDefinition(activeClassId));
    }

    @Nonnull
    private String resolveActiveClassLabel(
            @Nonnull String activeClassId,
            @Nonnull String resolvedSystemId,
            @Nullable ClassDefinition classDef) {
        if (!activeClassId.isEmpty()) {
            if (classDef != null) {
                String display = classDef.getDisplayName();
                if (display != null && !display.isBlank()) {
                    return display;
                }
            }

            return activeClassId;
        }

        LevelSystemConfig config = levelSystem.getConfig(resolvedSystemId);
        if (config != null) {
            String display = config.getDisplayName();
            if (display != null && !display.isBlank()) {
                return display;
            }
        }

        return resolvedSystemId;
    }
}