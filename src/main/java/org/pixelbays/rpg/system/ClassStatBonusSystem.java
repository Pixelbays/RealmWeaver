package org.pixelbays.rpg.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.pixelbays.rpg.component.ClassComponent;
import org.pixelbays.rpg.component.LevelProgressionComponent;
import org.pixelbays.rpg.config.ClassDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * System that applies stat bonuses from classes to entities.
 * Integrates with Hytale's EntityStatMap system.
 * 
 * Stat modifiers are applied in this order:
 * 1. Base stats
 * 2. Additive modifiers (flat bonuses)
 * 3. Multiplicative modifiers (percentage bonuses)
 * 
 * TODO: This system needs integration with Hytale's EntityStatMap
 * which is not fully accessible in the current API. The structure
 * is prepared for when that integration becomes available.
 */
public class ClassStatBonusSystem {

    // Reference to class management system
    private final ClassManagementSystem classManagementSystem;

    // Cache of applied stat modifiers (for recalculation)
    private final Map<Ref<EntityStore>, AppliedModifiers> appliedModifiersCache;

    public ClassStatBonusSystem(@Nonnull ClassManagementSystem classManagementSystem) {
        this.classManagementSystem = classManagementSystem;
        this.appliedModifiersCache = new HashMap<>();
    }

    /**
     * Recalculate and apply all stat bonuses for an entity
     * Should be called when:
     * - Class is learned/unlearned
     * - Active class changes
     * - Class levels up
     * 
     * @param entityRef Entity to recalculate stats for
     * @param store     Entity store
     */
    public void recalculateStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        // Get class component
        ClassComponent classComp = getClassComponent(entityRef, store);
        if (classComp == null) {
            return; // No classes learned
        }

        // Get active class
        String activeClassId = classComp.getActiveClassId();
        if (activeClassId.isEmpty()) {
            // No active class, clear all bonuses
            clearStatBonuses(entityRef, store);
            return;
        }

        // Get class definition
        ClassDefinition classDef = classManagementSystem.getClassDefinition(activeClassId);
        if (classDef == null) {
            return;
        }

        // Calculate total modifiers
        AppliedModifiers modifiers = calculateTotalModifiers(entityRef, classDef, store);

        // Remove old modifiers
        AppliedModifiers oldModifiers = appliedModifiersCache.get(entityRef);
        if (oldModifiers != null) {
            removeModifiers(entityRef, oldModifiers, store);
        }

        // Apply new modifiers
        applyModifiers(entityRef, modifiers, store);

        // Cache applied modifiers
        appliedModifiersCache.put(entityRef, modifiers);

        System.out.println("[ClassStatSystem] Recalculated stat bonuses for " + activeClassId);
    }

    /**
     * Calculate total stat modifiers from active class
     */
    private AppliedModifiers calculateTotalModifiers(@Nonnull Ref<EntityStore> entityRef,
            ClassDefinition classDef,
            @Nonnull Store<EntityStore> store) {
        AppliedModifiers total = new AppliedModifiers();

        // Add base stat modifiers
        ClassDefinition.StatModifiers baseStats = classDef.getBaseStatModifiers();
        if (!baseStats.isEmpty()) {
            total.additiveModifiers.putAll(baseStats.getAdditiveModifiers());
            total.multiplicativeModifiers.putAll(baseStats.getMultiplicativeModifiers());
        }

        // Add per-level modifiers
        ClassDefinition.StatModifiers perLevelStats = classDef.getPerLevelModifiers();
        if (!perLevelStats.isEmpty()) {
            int classLevel = getClassLevel(entityRef, classDef, store);

            // Multiply per-level bonuses by class level
            for (Map.Entry<String, Float> entry : perLevelStats.getAdditiveModifiers().entrySet()) {
                String stat = entry.getKey();
                float bonus = entry.getValue() * classLevel;
                total.additiveModifiers.merge(stat, bonus, Float::sum);
            }

            for (Map.Entry<String, Float> entry : perLevelStats.getMultiplicativeModifiers().entrySet()) {
                String stat = entry.getKey();
                float bonus = entry.getValue() * classLevel;
                total.multiplicativeModifiers.merge(stat, bonus, Float::sum);
            }
        }

        return null;
    }

    /**
     * Get class level (from level system or character level)
     */
    private int getClassLevel(@Nonnull Ref<EntityStore> entityRef, ClassDefinition classDef,
            @Nonnull Store<EntityStore> store) {
        if (classDef.usesCharacterLevel()) {
            // Use character level
            LevelProgressionComponent levelComp = getLevelComponent(entityRef, store);
            if (levelComp != null) {
                LevelProgressionComponent.LevelSystemData charLevel = levelComp.getSystem("character_level");
                return charLevel != null ? charLevel.getCurrentLevel() : 1;
            }
            return 1;
        } else {
            // Use class-specific level
            String levelSystemId = classDef.getLevelSystemId();
            if (!levelSystemId.isEmpty()) {
                LevelProgressionComponent levelComp = getLevelComponent(entityRef, store);
                if (levelComp != null) {
                    LevelProgressionComponent.LevelSystemData classLevel = levelComp.getSystem(levelSystemId);
                    return classLevel != null ? classLevel.getCurrentLevel() : 1;
                }
            }
            return 1;
        }
    }

    /**
     * Apply stat modifiers to entity
     * 
     * TODO: Integration with EntityStatMap required
     * This is a placeholder that logs what would be applied
     */
    private void applyModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store) {
        System.out.println("[ClassStatSystem] TODO: Apply stat modifiers to EntityStatMap");

        // Log what would be applied
        if (!modifiers.additiveModifiers.isEmpty()) {
            System.out.println("  Additive modifiers: " + modifiers.additiveModifiers);
        }
        if (!modifiers.multiplicativeModifiers.isEmpty()) {
            System.out.println("  Multiplicative modifiers: " + modifiers.multiplicativeModifiers);
        }

        /*
         * TODO: When EntityStatMap API is available, apply like this:
         * 
         * EntityStatMap statMap = store.getComponent(entityRef,
         * EntityStatMap.getComponentType());
         * if (statMap == null) return;
         * 
         * // Apply additive modifiers
         * for (Map.Entry<String, Float> entry : modifiers.additiveModifiers.entrySet())
         * {
         * String statName = entry.getKey();
         * float value = entry.getValue();
         * 
         * EntityStatType statType = EntityStatType.getByName(statName);
         * if (statType != null) {
         * statMap.addModifier(statType, value, ModifierType.FLAT, "class_bonus");
         * }
         * }
         * 
         * // Apply multiplicative modifiers
         * for (Map.Entry<String, Float> entry :
         * modifiers.multiplicativeModifiers.entrySet()) {
         * String statName = entry.getKey();
         * float value = entry.getValue();
         * 
         * EntityStatType statType = EntityStatType.getByName(statName);
         * if (statType != null) {
         * statMap.addModifier(statType, value, ModifierType.MULTIPLICATIVE,
         * "class_bonus");
         * }
         * }
         */
    }

    /**
     * Remove stat modifiers from entity
     * 
     * TODO: Integration with EntityStatMap required
     */
    private void removeModifiers(@Nonnull Ref<EntityStore> entityRef, AppliedModifiers modifiers,
            @Nonnull Store<EntityStore> store) {
        System.out.println("[ClassStatSystem] TODO: Remove stat modifiers from EntityStatMap");

        /*
         * TODO: When EntityStatMap API is available, remove like this:
         * 
         * EntityStatMap statMap = store.getComponent(entityRef,
         * EntityStatMap.getComponentType());
         * if (statMap == null) return;
         * 
         * // Remove modifiers with "class_bonus" tag
         * statMap.removeModifiersByTag("class_bonus");
         */
    }

    /**
     * Clear all stat bonuses from an entity
     */
    public void clearStatBonuses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        AppliedModifiers oldModifiers = appliedModifiersCache.remove(entityRef);
        if (oldModifiers != null) {
            removeModifiers(entityRef, oldModifiers, store);
            System.out.println("[ClassStatSystem] Cleared all stat bonuses");
        }
    }

    /**
     * Get currently applied modifiers for an entity (for display/debugging)
     */
    @Nullable
    public AppliedModifiers getAppliedModifiers(@Nonnull Ref<EntityStore> entityRef) {
        return appliedModifiersCache.get(entityRef);
    }

    /**
     * Cleanup cache when entity is removed
     */
    public void onEntityRemoved(@Nonnull Ref<EntityStore> entityRef) {
        appliedModifiersCache.remove(entityRef);
    }

    // === Helper Methods ===

    @Nullable
    private ClassComponent getClassComponent(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return store.getComponent(entityRef, ClassComponent.getComponentType());
    }

    @Nullable
    private LevelProgressionComponent getLevelComponent(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return store.getComponent(entityRef, LevelProgressionComponent.getComponentType());
    }

    // === Data Classes ===

    /**
     * Container for applied stat modifiers
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
