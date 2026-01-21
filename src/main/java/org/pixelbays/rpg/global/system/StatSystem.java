package org.pixelbays.rpg.global.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;

/**
 * Global stat system that centralizes stat calculations and application.
 *
 * This system is intended to be the single integration point for stats
 * (class bonuses, level growth, rewards, and modifiers).
 */
@SuppressWarnings("null")
public class StatSystem {

    private final ClassManagementSystem classManagementSystem;
    private final LevelProgressionSystem levelProgressionSystem;

    // Cache of applied class modifiers by entity
    private final Map<Ref<EntityStore>, AppliedModifiers> classModifiersCache;

    public StatSystem(@Nonnull ClassManagementSystem classManagementSystem,
            @Nonnull LevelProgressionSystem levelProgressionSystem) {
        this.classManagementSystem = classManagementSystem;
        this.levelProgressionSystem = levelProgressionSystem;
        this.classModifiersCache = new HashMap<>();
    }

    /**
     * Apply EXP rate gain stat modifier.
     */
    public float applyExpRateGain(@Nonnull Ref<EntityStore> entityRef, float baseExp,
            @Nonnull Store<EntityStore> store) {
        if (baseExp <= 0f) {
            return baseExp;
        }

        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return baseExp;
        }

        int statIndex = EntityStatType.getAssetMap().getIndex("Exp_Rate_Gain");
        if (statIndex == Integer.MIN_VALUE) {
            return baseExp;
        }

        EntityStatValue statValue = statMap.get(statIndex);
        if (statValue == null) {
            return baseExp;
        }

        float ratePercent = statValue.get();
        float multiplier = 1.0f + (ratePercent / 100.0f);
        if (multiplier < 0f) {
            multiplier = 0f;
        }

        return baseExp * multiplier;
    }

    /**
     * Recalculate and apply class stat modifiers for the entity.
     */
    public void recalculateClassStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null) {
            return;
        }

        String activeClassId = classComp.getActiveClassId();
        if (activeClassId == null || activeClassId.isEmpty()) {
            clearClassStatBonuses(entityRef, store);
            return;
        }

        ClassDefinition classDef = classManagementSystem.getClassDefinition(activeClassId);
        if (classDef == null) {
            return;
        }

        AppliedModifiers modifiers = calculateClassModifiers(entityRef, classDef);

        AppliedModifiers oldModifiers = classModifiersCache.get(entityRef);
        if (oldModifiers != null) {
            removeModifiers(entityRef, oldModifiers, store);
        }

        applyModifiers(entityRef, modifiers, store);
        classModifiersCache.put(entityRef, modifiers);
    }

    /**
     * Clear class stat modifiers for an entity.
     */
    public void clearClassStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        AppliedModifiers oldModifiers = classModifiersCache.remove(entityRef);
        if (oldModifiers != null) {
            removeModifiers(entityRef, oldModifiers, store);
        }
    }

    /**
     * Apply stat increases (e.g., from level rewards).
     */
    @SuppressWarnings("unused")
    public void applyStatIncreases(@Nonnull Ref<EntityStore> entityRef,
            @Nullable Map<String, Float> statIncreases,
            @Nonnull Store<EntityStore> store) {
        if (statIncreases == null || statIncreases.isEmpty()) {
            return;
        }

        // TODO: Integration with EntityStatMap
        System.out.println("[StatSystem] Applied stat increases for entity " + entityRef.getIndex() + ": "
                + statIncreases);
    }

    /**
     * Apply stat growth from leveling config.
     */
    @SuppressWarnings("unused")
    public void applyStatGrowth(@Nonnull Ref<EntityStore> entityRef, int newLevel,
            @Nullable LevelSystemConfig.StatGrowthConfig growth,
            @Nonnull Store<EntityStore> store) {
        if (growth == null) {
            return;
        }

        if (growth.getFlatGrowth() != null) {
            applyStatIncreases(entityRef, growth.getFlatGrowth(), store);
        }

        // TODO: Implement percentage-based growth using current stat values.

        if (growth.getMilestoneGrowth() != null && growth.getMilestoneGrowth().containsKey(newLevel)) {
            applyStatIncreases(entityRef, growth.getMilestoneGrowth().get(newLevel), store);
        }
    }

    /**
     * Get current class modifiers for debugging.
     */
    @Nullable
    public AppliedModifiers getClassModifiers(@Nonnull Ref<EntityStore> entityRef) {
        return classModifiersCache.get(entityRef);
    }

    /**
     * Cleanup cache when entity is removed.
     */
    public void onEntityRemoved(@Nonnull Ref<EntityStore> entityRef) {
        classModifiersCache.remove(entityRef);
    }

    private AppliedModifiers calculateClassModifiers(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ClassDefinition classDef) {
        AppliedModifiers total = new AppliedModifiers();

        ClassDefinition.StatModifiers baseStats = classDef.getBaseStatModifiers();
        if (baseStats != null && !baseStats.isEmpty()) {
            total.additiveModifiers.putAll(baseStats.getAdditiveModifiers());
            total.multiplicativeModifiers.putAll(baseStats.getMultiplicativeModifiers());
        }

        ClassDefinition.StatModifiers perLevelStats = classDef.getPerLevelModifiers();
        if (perLevelStats != null && !perLevelStats.isEmpty()) {
            int classLevel = getClassLevel(entityRef, classDef);

            for (Object2FloatMap.Entry<String> entry : perLevelStats.getAdditiveModifiers().object2FloatEntrySet()) {
                float bonus = entry.getFloatValue() * classLevel;
                total.additiveModifiers.merge(entry.getKey(), bonus, Float::sum);
            }

            for (Object2FloatMap.Entry<String> entry : perLevelStats.getMultiplicativeModifiers()
                    .object2FloatEntrySet()) {
                float bonus = entry.getFloatValue() * classLevel;
                total.multiplicativeModifiers.merge(entry.getKey(), bonus, Float::sum);
            }
        }

        return total;
    }

    private int getClassLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ClassDefinition classDef) {
        String systemId = classDef.usesCharacterLevel() ? "character_level" : classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            return 1;
        }
        int level = levelProgressionSystem.getLevel(entityRef, systemId);
        return level > 0 ? level : 1;
    }

    @SuppressWarnings("unused")
    private void applyModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store) {
        // TODO: Integration with EntityStatMap required
        if (!modifiers.additiveModifiers.isEmpty()) {
            System.out.println("[StatSystem] Additive modifiers: " + modifiers.additiveModifiers);
        }
        if (!modifiers.multiplicativeModifiers.isEmpty()) {
            System.out.println("[StatSystem] Multiplicative modifiers: " + modifiers.multiplicativeModifiers);
        }
    }

    @SuppressWarnings("unused")
    private void removeModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store) {
        // TODO: Integration with EntityStatMap required
    }

    /**
     * Container for applied stat modifiers.
     */
    public static class AppliedModifiers {
        public final Map<String, Float> additiveModifiers;
        public final Map<String, Float> multiplicativeModifiers;

        public AppliedModifiers() {
            this.additiveModifiers = new HashMap<>();
            this.multiplicativeModifiers = new HashMap<>();
        }

        public boolean isEmpty() {
            return additiveModifiers.isEmpty() && multiplicativeModifiers.isEmpty();
        }

        @Override
        public String toString() {
            return "AppliedModifiers{" +
                    "additive=" + additiveModifiers +
                    ", multiplicative=" + multiplicativeModifiers +
                    '}';
        }
    }
}
