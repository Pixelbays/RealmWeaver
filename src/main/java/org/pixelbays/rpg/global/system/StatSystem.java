package org.pixelbays.rpg.global.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.config.builder.StatModifiers;
import org.pixelbays.rpg.global.event.ClassStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.RaceStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.StatGrowthAppliedEvent;
import org.pixelbays.rpg.global.event.StatIncreasesAppliedEvent;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.config.StatGrowthConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
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
            @Nonnull LevelProgressionSystem levelProgressionSystem,
            @Nonnull EventRegistry eventRegistry) {
        this.classManagementSystem = classManagementSystem;
        this.raceManagementSystem = raceManagementSystem;
        this.levelProgressionSystem = levelProgressionSystem;
        this.classModifiersCache = new HashMap<>();
        this.raceModifiersCache = new HashMap<>();

        eventRegistry.register(PlayerConnectEvent.class, onPlayerConnect());
    }

    private java.util.function.Consumer<PlayerConnectEvent> onPlayerConnect() {
        return event -> {
            var playerRef = event.getPlayerRef();
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> store = entityRef.getStore();
            boolean classInitialized = classManagementSystem.ensureStartingClass(entityRef, store);
            if (!classInitialized) {
                recalculateClassStatBonuses(entityRef, store);
            }
            recalculateRaceStatBonuses(entityRef, store);
            recalculateStatGrowthBonuses(entityRef, store);
        };
    }

    private void recalculateStatGrowthBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        LevelProgressionComponent levelComp = store.getComponent(entityRef, LevelProgressionComponent.getComponentType());
        if (levelComp == null) {
            return;
        }

        for (LevelProgressionComponent.LevelSystemData data : levelComp.getAllSystems().values()) {
            if (data == null) {
                continue;
            }

            String systemId = data.getSystemId();
            LevelSystemConfig config = levelProgressionSystem.getConfig(systemId);
            if (config == null || config.getStatGrowth() == null) {
                continue;
            }

            int currentLevel = data.getCurrentLevel();
            int lastApplied = data.getLastGrowthAppliedLevel();
            if (currentLevel <= 1 || currentLevel <= lastApplied) {
                continue;
            }

            int startLevel = Math.max(2, lastApplied + 1);
            for (int level = startLevel; level <= currentLevel; level++) {
                applyStatGrowth(entityRef, level, config.getStatGrowth(), store);
            }

            data.setLastGrowthAppliedLevel(currentLevel);
            RpgLogging.debugDeveloper("[StatSystem] Reapplied stat growth for %s: %d -> %d",
                    systemId, startLevel, currentLevel);
        }
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

        if (classComp.getLearnedClassIds().isEmpty()) {
            clearClassStatBonuses(entityRef, store);
            return;
        }

        AppliedModifiers modifiers = new AppliedModifiers();
        for (String classId : classComp.getLearnedClassIds()) {
            if (classId == null || classId.isEmpty()) {
                continue;
            }
            ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
            if (classDef == null) {
                continue;
            }
            AppliedModifiers classModifiers = calculateClassModifiers(entityRef, classDef);
            mergeModifiers(modifiers, classModifiers);
        }

        AppliedModifiers oldModifiers = classModifiersCache.get(entityRef);
        updateModifiers(entityRef, modifiers, oldModifiers, store, "rpg_class");

        if (!modifiers.isEmpty()) {
            classModifiersCache.put(entityRef, modifiers);
        } else {
            classModifiersCache.remove(entityRef);
        }

        ClassStatBonusesRecalculatedEvent.dispatch(entityRef, classComp.getLearnedClassIds());
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

        ClassStatBonusesRecalculatedEvent.dispatch(entityRef, java.util.Set.of());
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

        Map<String, Float> applied = new HashMap<>();

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
            applied.put(statId, increase);

            RpgLogging.debugDeveloper("[StatSystem] Applied stat increase to %s: %s -> %s", statId, oldValue,
                    oldValue + increase);
        }

        if (!applied.isEmpty()) {
            StatIncreasesAppliedEvent.dispatch(entityRef, applied);
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

        StatGrowthAppliedEvent.dispatch(entityRef, newLevel, growth);
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
        AppliedModifiers oldModifiers = raceModifiersCache.get(entityRef);

        // Get entity's race
        RaceComponent raceComponent = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (raceComponent == null || raceComponent.getRaceId() == null || raceComponent.getRaceId().isEmpty()) {
            updateModifiers(entityRef, new AppliedModifiers(), oldModifiers, store, "rpg_race");
            raceModifiersCache.remove(entityRef);
            RaceStatBonusesRecalculatedEvent.dispatch(entityRef, "");
            return;
        }

        // Get race definition
        RaceDefinition raceDef = raceManagementSystem.getRaceDefinition(raceComponent.getRaceId());
        if (raceDef == null) {
            RpgLogging.debugDeveloper("[StatSystem] Race definition not found: %s", raceComponent.getRaceId());
            updateModifiers(entityRef, new AppliedModifiers(), oldModifiers, store, "rpg_race");
            raceModifiersCache.remove(entityRef);
            RaceStatBonusesRecalculatedEvent.dispatch(entityRef, "");
            return;
        }

        // Calculate new race modifiers
        AppliedModifiers newModifiers = calculateRaceModifiers(raceDef);

        updateModifiers(entityRef, newModifiers, oldModifiers, store, "rpg_race");

        if (!newModifiers.isEmpty()) {
            raceModifiersCache.put(entityRef, newModifiers);
            RpgLogging.debugDeveloper("[StatSystem] Applied race stat bonuses for %s: %s", raceDef.getId(),
                    newModifiers);
        } else {
            raceModifiersCache.remove(entityRef);
        }

        RaceStatBonusesRecalculatedEvent.dispatch(entityRef, raceDef.getId());
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

    private void mergeModifiers(@Nonnull AppliedModifiers target, @Nonnull AppliedModifiers source) {
        if (source.additiveModifiers != null) {
            for (Map.Entry<String, Float> entry : source.additiveModifiers.entrySet()) {
                target.additiveModifiers.merge(entry.getKey(), entry.getValue(), Float::sum);
            }
        }

        if (source.multiplicativeModifiers != null) {
            for (Map.Entry<String, Float> entry : source.multiplicativeModifiers.entrySet()) {
                target.multiplicativeModifiers.merge(entry.getKey(), entry.getValue(), Float::sum);
            }
        }
    }

    private int getClassLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ClassDefinition classDef) {
        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
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

    private void updateModifiers(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull AppliedModifiers newModifiers,
            @Nullable AppliedModifiers oldModifiers,
            @Nonnull Store<EntityStore> store,
            @Nonnull String modifierKeyPrefix) {
        EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            RpgLogging.debugDeveloper("[StatSystem] No EntityStatMap found for entity %s", entityRef.getIndex());
            return;
        }

        AppliedModifiers oldSafe = oldModifiers == null ? new AppliedModifiers() : oldModifiers;

        updateModifierMap(statMap, newModifiers.additiveModifiers, oldSafe.additiveModifiers,
                modifierKeyPrefix + "_additive_", true);
        updateModifierMap(statMap, newModifiers.multiplicativeModifiers, oldSafe.multiplicativeModifiers,
                modifierKeyPrefix + "_multiplicative_", false);
    }

    private void updateModifierMap(@Nonnull EntityStatMap statMap,
            @Nonnull Map<String, Float> newMap,
            @Nonnull Map<String, Float> oldMap,
            @Nonnull String keyPrefix,
            boolean additive) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        keys.addAll(newMap.keySet());
        keys.addAll(oldMap.keySet());

        for (String statId : keys) {
            Float newValue = newMap.get(statId);
            Float oldValue = oldMap.get(statId);

            boolean newPresent = newValue != null && newValue != 0f;
            boolean oldPresent = oldValue != null && oldValue != 0f;

            if (!newPresent && !oldPresent) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            if (statIndex == Integer.MIN_VALUE) {
                RpgLogging.debugDeveloper("[StatSystem] Unknown stat type for modifier update: %s", statId);
                continue;
            }

            String modifierKey = keyPrefix + statId;

            if (!newPresent && oldPresent) {
                statMap.removeModifier(statIndex, modifierKey);
                continue;
            }

            if (newPresent && oldPresent && floatEquals(newValue, oldValue)) {
                continue;
            }

            boolean increased = newPresent && (oldValue == null || newValue > oldValue + 0.0001f);

            StaticModifier staticModifier;
            if (additive) {
                staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.ADDITIVE, newValue);
            } else {
                float multiplier = 1.0f + (newValue / 100.0f);
                staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE, multiplier);
            }

            statMap.putModifier(statIndex, modifierKey, staticModifier);

            if (increased && isRegeneratingStat(statIndex) && !isChargeStat(statId)) {
                statMap.maximizeStatValue(statIndex);
            }
        }
    }

    private boolean floatEquals(float a, float b) {
        return Math.abs(a - b) < 0.0001f;
    }

    private boolean isRegeneratingStat(int statIndex) {
        EntityStatType statType = EntityStatType.getAssetMap().getAsset(statIndex);
        return statType != null && statType.getRegenerating() != null && statType.getRegenerating().length > 0;
    }

    private boolean isChargeStat(@Nonnull String statId) {
        return "Ammo".equalsIgnoreCase(statId) || statId.endsWith("Charges");
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
