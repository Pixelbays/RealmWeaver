package org.pixelbays.rpg.global.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.config.builder.StatModifiers;
import org.pixelbays.rpg.leveling.config.StatGrowthConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
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
    private final RaceManagementSystem raceManagementSystem;
    private final LevelProgressionSystem levelProgressionSystem;

    // Cache of applied class modifiers by entity
    private final Map<Ref<EntityStore>, AppliedModifiers> classModifiersCache;
    // Cache of applied race modifiers by entity
    private final Map<Ref<EntityStore>, AppliedModifiers> raceModifiersCache;

    public StatSystem(@Nonnull ClassManagementSystem classManagementSystem,
            @Nonnull RaceManagementSystem raceManagementSystem,
            @Nonnull LevelProgressionSystem levelProgressionSystem) {
        this.classManagementSystem = classManagementSystem;
        this.raceManagementSystem = raceManagementSystem;
        this.levelProgressionSystem = levelProgressionSystem;
        this.classModifiersCache = new HashMap<>();
        this.raceModifiersCache = new HashMap<>();
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
            removeModifiers(entityRef, oldModifiers, store, "rpg_class");
        }

        applyModifiers(entityRef, modifiers, store, "rpg_class");
        classModifiersCache.put(entityRef, modifiers);
    }

    /**
     * Clear class stat modifiers for an entity.
     */
    public void clearClassStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        AppliedModifiers oldModifiers = classModifiersCache.remove(entityRef);
        if (oldModifiers != null) {
            removeModifiers(entityRef, oldModifiers, store, "rpg_class");
        }
    }

    /**
     * Apply stat increases (e.g., from level rewards).
     */
    public void applyStatIncreases(@Nonnull Ref<EntityStore> entityRef,
            @Nullable Map<String, Float> statIncreases,
            @Nonnull Store<EntityStore> store) {
        if (statIncreases == null || statIncreases.isEmpty()) {
            return;
        }

        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            RpgLogging.debugDeveloper("[StatSystem] No EntityStatMap found for entity %s", entityRef.getIndex());
            return;
        }

        for (Map.Entry<String, Float> entry : statIncreases.entrySet()) {
            String statId = entry.getKey();
            Float increase = entry.getValue();

            if (increase == null || increase == 0f) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                RpgLogging.debugDeveloper("[StatSystem] Unknown stat type: %s", statId);
                continue;
            }

            // Apply flat increase to current value using EntityStatMap
            float oldValue = statMap.addStatValue(statIndex, increase);

            RpgLogging.debugDeveloper("[StatSystem] Applied stat increase to %s: %s -> %s", statId, oldValue,
                    oldValue + increase);
        }
    }

    /**
     * Apply stat growth from leveling config.
     */
    public void applyStatGrowth(@Nonnull Ref<EntityStore> entityRef, int newLevel,
            @Nullable StatGrowthConfig growth,
            @Nonnull Store<EntityStore> store) {
        if (growth == null) {
            return;
        }

        // Apply flat growth per level
        if (growth.getFlatGrowth() != null && !growth.getFlatGrowth().isEmpty()) {
            applyStatIncreases(entityRef, growth.getFlatGrowth(), store);
        }

        // Apply percentage-based growth
        if (growth.getPercentageGrowth() != null && !growth.getPercentageGrowth().isEmpty()) {
            EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }

            for (Map.Entry<String, Float> entry : growth.getPercentageGrowth().entrySet()) {
                String statId = entry.getKey();
                Float percentIncrease = entry.getValue();

                if (percentIncrease == null || percentIncrease == 0f) {
                    continue;
                }

                int statIndex = EntityStatType.getAssetMap().getIndex(statId);
                if (statIndex == Integer.MIN_VALUE) {
                    RpgLogging.debugDeveloper("[StatSystem] Unknown stat type: %s", statId);
                    continue;
                }

                EntityStatValue statValue = statMap.get(statIndex);
                if (statValue == null) {
                    continue;
                }

                // Apply percentage increase based on current value
                float currentValue = statValue.get();
                float increase = currentValue * (percentIncrease / 100.0f);
                float newValue = statMap.addStatValue(statIndex, increase);

                RpgLogging.debugDeveloper(
                    "[StatSystem] Applied percentage stat growth to %s: %s -> %s (+%s%%)",
                    statId,
                    currentValue,
                    newValue,
                    percentIncrease);
            }
        }

        // Apply milestone growth for specific levels
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
     * Get current race modifiers for debugging.
     */
    @Nullable
    public AppliedModifiers getRaceModifiers(@Nonnull Ref<EntityStore> entityRef) {
        return raceModifiersCache.get(entityRef);
    }

    /**
     * Recalculate and apply race stat bonuses.
     * Call this when the entity's race changes.
     */
    public void recalculateRaceStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        // Remove old race modifiers if they exist
        AppliedModifiers oldModifiers = raceModifiersCache.get(entityRef);
        if (oldModifiers != null && !oldModifiers.isEmpty()) {
            removeModifiers(entityRef, oldModifiers, store, "rpg_race");
        }

        // Get entity's race
        RaceComponent raceComponent = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (raceComponent == null || raceComponent.getRaceId() == null || raceComponent.getRaceId().isEmpty()) {
            raceModifiersCache.remove(entityRef);
            return;
        }

        // Get race definition
        RaceDefinition raceDef = raceManagementSystem.getRaceDefinition(raceComponent.getRaceId());
        if (raceDef == null) {
            RpgLogging.debugDeveloper("[StatSystem] Race definition not found: %s", raceComponent.getRaceId());
            raceModifiersCache.remove(entityRef);
            return;
        }

        // Calculate new race modifiers
        AppliedModifiers newModifiers = calculateRaceModifiers(raceDef);

        // Apply new modifiers
        if (!newModifiers.isEmpty()) {
            applyModifiers(entityRef, newModifiers, store, "rpg_race");
            raceModifiersCache.put(entityRef, newModifiers);
            RpgLogging.debugDeveloper("[StatSystem] Applied race stat bonuses for %s: %s", raceDef.getId(),
                    newModifiers);
        } else {
            raceModifiersCache.remove(entityRef);
        }
    }

    /**
     * Cleanup cache when entity is removed.
     */
    public void onEntityRemoved(@Nonnull Ref<EntityStore> entityRef) {
        classModifiersCache.remove(entityRef);
        raceModifiersCache.remove(entityRef);
    }

    private AppliedModifiers calculateRaceModifiers(@Nonnull RaceDefinition raceDef) {
        AppliedModifiers total = new AppliedModifiers();

        StatModifiers raceStats = raceDef.getStatModifiers();
        if (raceStats != null && !raceStats.isEmpty()) {
            if (raceStats.getAdditiveModifiers() != null) {
                for (Object2FloatMap.Entry<String> entry : raceStats.getAdditiveModifiers().object2FloatEntrySet()) {
                    total.additiveModifiers.merge(entry.getKey(), entry.getFloatValue(), Float::sum);
                }
            }
            if (raceStats.getMultiplicativeModifiers() != null) {
                for (Object2FloatMap.Entry<String> entry : raceStats.getMultiplicativeModifiers().object2FloatEntrySet()) {
                    total.multiplicativeModifiers.merge(entry.getKey(), entry.getFloatValue(), Float::sum);
                }
            }
        }

        return total;
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

    private void applyModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store, @Nonnull String modifierKeyPrefix) {
        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            RpgLogging.debugDeveloper("[StatSystem] No EntityStatMap found for entity %s", entityRef.getIndex());
            return;
        }

        // Apply additive modifiers via StaticModifier objects
        for (Map.Entry<String, Float> entry : modifiers.additiveModifiers.entrySet()) {
            String statId = entry.getKey();
            Float modifier = entry.getValue();

            if (modifier == null || modifier == 0f) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                RpgLogging.debugDeveloper("[StatSystem] Unknown stat type for additive modifier: %s", statId);
                continue;
            }

            EntityStatValue statValue = statMap.get(statIndex);
            if (statValue == null) {
                RpgLogging.debugDeveloper("[StatSystem] No stat value found for: %s", statId);
                continue;
            }

            // Create and add additive modifier to MAX target (increases the stat's max)
            String modifierKey = modifierKeyPrefix + "_additive_" + statId;
            StaticModifier staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX, 
                StaticModifier.CalculationType.ADDITIVE, modifier);
            statMap.putModifier(statIndex, modifierKey, staticModifier);
            RpgLogging.debugDeveloper("[StatSystem] Applied additive modifier to %s: +%s", statId, modifier);
        }

        // Apply multiplicative modifiers via StaticModifier objects
        for (Map.Entry<String, Float> entry : modifiers.multiplicativeModifiers.entrySet()) {
            String statId = entry.getKey();
            Float modifier = entry.getValue();

            if (modifier == null || modifier == 0f) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                RpgLogging.debugDeveloper("[StatSystem] Unknown stat type for multiplicative modifier: %s", statId);
                continue;
            }

            EntityStatValue statValue = statMap.get(statIndex);
            if (statValue == null) {
                RpgLogging.debugDeveloper("[StatSystem] No stat value found for: %s", statId);
                continue;
            }

            // Create and add multiplicative modifier to MAX target
            // Convert percentage to multiplier (e.g., 10% = 1.1)
            String modifierKey = modifierKeyPrefix + "_multiplicative_" + statId;
            float multiplier = 1.0f + (modifier / 100.0f);
            StaticModifier staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX, 
                StaticModifier.CalculationType.MULTIPLICATIVE, multiplier);
            statMap.putModifier(statIndex, modifierKey, staticModifier);
            RpgLogging.debugDeveloper("[StatSystem] Applied multiplicative modifier to %s: +%s%%", statId, modifier);
        }
    }

    private void removeModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store, @Nonnull String modifierKeyPrefix) {
        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        // Remove additive modifiers
        for (Map.Entry<String, Float> entry : modifiers.additiveModifiers.entrySet()) {
            String statId = entry.getKey();
            Float modifier = entry.getValue();

            if (modifier == null || modifier == 0f) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                continue;
            }

            EntityStatValue statValue = statMap.get(statIndex);
            if (statValue == null) {
                continue;
            }

            // Remove the modifier by key
            String modifierKey = modifierKeyPrefix + "_additive_" + statId;
            statMap.removeModifier(statIndex, modifierKey);
            RpgLogging.debugDeveloper("[StatSystem] Removed additive modifier from %s: -%s", statId, modifier);
        }

        // Remove multiplicative modifiers
        for (Map.Entry<String, Float> entry : modifiers.multiplicativeModifiers.entrySet()) {
            String statId = entry.getKey();
            Float modifier = entry.getValue();

            if (modifier == null || modifier == 0f) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                continue;
            }

            EntityStatValue statValue = statMap.get(statIndex);
            if (statValue == null) {
                continue;
            }

            // Remove the modifier by key
            String modifierKey = modifierKeyPrefix + "_multiplicative_" + statId;
            statMap.removeModifier(statIndex, modifierKey);
            RpgLogging.debugDeveloper("[StatSystem] Removed multiplicative modifier from %s: -%s%%", statId, modifier);
        }
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
