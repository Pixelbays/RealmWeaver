package org.pixelbays.rpg.ability.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition.AbilityType;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.global.system.RpgLogging;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Ticking system for passive and toggled abilities.
 * Runs every tick and processes entities that have the ClassAbilityComponent.
 * 
 * Handles:
 * - Passive abilities (always active effects)
 * - Toggle abilities (on/off state effects)
 * - Periodic ability effects
 */
public class PassiveAbilityTickingSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, ClassComponent> classComponentType;
    private final ComponentType<EntityStore, ClassAbilityComponent> classAbilityComponentType;
    private final ClassAbilitySystem classAbilitySystem;

    public PassiveAbilityTickingSystem(
            @Nonnull ComponentType<EntityStore, ClassComponent> classComponentType,
            @Nonnull ComponentType<EntityStore, ClassAbilityComponent> classAbilityComponentType,
            @Nonnull ClassAbilitySystem classAbilitySystem) {
        this.classComponentType = classComponentType;
        this.classAbilityComponentType = classAbilityComponentType;
        this.classAbilitySystem = classAbilitySystem;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        // Get the ClassComponent for this entity (has unlocked spells)
        ClassComponent classComponent = archetypeChunk.getComponent(index, classComponentType);
        if (classComponent == null) {
            return;
        }

        // Get entity reference for potential command buffer operations
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        // Get ClassAbilityComponent for toggle states (create if missing)
        ClassAbilityComponent abilityComponent = archetypeChunk.getComponent(index, classAbilityComponentType);
        if (abilityComponent == null) {
            // Auto-create ClassAbilityComponent using CommandBuffer (thread-safe)
            commandBuffer.addComponent(ref, classAbilityComponentType);
            // Skip processing this tick - component will be available next tick
            return;
        }

        // Legacy migration: move unlocks from ClassComponent to ClassAbilityComponent
        if (abilityComponent.getAllAbilities().isEmpty() && !classComponent.getAllUnlockedSpells().isEmpty()) {
            String classId = classComponent.getActiveClassId();
            for (ClassComponent.SpellUnlockData legacy : classComponent.getAllUnlockedSpells().values()) {
                if (legacy == null || legacy.getAbilityId() == null || legacy.getAbilityId().isEmpty()) {
                    continue;
                }
                int rank = legacy.getRank();
                abilityComponent.unlockAbility(legacy.getAbilityId(), classId, rank);
            }
            classComponent.clearLegacyUnlockedSpells();
        }

        // Check if there are unlocked abilities
        if (abilityComponent.getAllAbilities().isEmpty()) {
            // No abilities unlocked, skip silently
            return;
        }

        // Process each unlocked ability
        for (String abilityId : abilityComponent.getAllAbilities().keySet()) {
            ClassAbilityDefinition abilityDef = classAbilitySystem.getAbilityDefinition(abilityId);
            if (abilityDef == null) {
                RpgLogging.debugDeveloper("Ability definition not found for unlocked ability: %s", abilityId);
                continue;
            }
            
            RpgLogging.debugDeveloper("Processing ability: %s, Type: %s, Enabled: %s", 
                abilityId, abilityDef.getAbilityType(), abilityDef.isEnabled());
            
            if (!abilityDef.isEnabled()) {
                continue;
            }

            AbilityType abilityType = abilityDef.getAbilityType();
            if (abilityType == null) {
                RpgLogging.debugDeveloper("Ability %s has null AbilityType", abilityId);
                continue;
            }

            // Only process passive and toggle abilities
            switch (abilityType) {
                case Passive:
                    // Process passive ability effects
                    RpgLogging.debugDeveloper("Triggering passive ability: %s", abilityId);
                    processPassiveAbility(ref, abilityDef, store);
                    break;

                case Toggle:
                    // Process toggled ability effects (if active)
                    processToggleAbility(ref, abilityDef, abilityComponent, store);
                    break;

                case Active:
                default:
                    // Active abilities are handled by input handlers, not ticking
                    break;
            }
        }
    }

    /**
     * Process a passive ability's effects.
     * Passive abilities are always active and run every tick.
     * Uses the central triggerAbility() to execute the ability's interaction chain.
     */
    private void processPassiveAbility(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ClassAbilityDefinition abilityDef,
            @Nonnull Store<EntityStore> store) {

        // Trigger the passive ability using the central system
        // Use Primary interaction type for passive abilities
        ClassAbilitySystem.TriggerResult result = classAbilitySystem.triggerAbility(
                ref,
                store,
                abilityDef.getId(),
                InteractionType.Primary
        );

        // Log if the passive ability failed to trigger
        if (result.isFailure()) {
            RpgLogging.debugDeveloper(
                    "Failed to trigger passive ability '%s' for entity %s: %s",
                    abilityDef.getId(),
                    ref,
                    result.getErrorMessage());
        }
    }

    /**
     * Process a toggled ability's effects.
     * Toggle abilities only apply effects when they are in the "on" state.
     * Uses the central triggerAbility() to execute the ability's interaction chain.
     */
    private void processToggleAbility(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ClassAbilityDefinition abilityDef,
            @Nonnull ClassAbilityComponent abilityComponent,
            @Nonnull Store<EntityStore> store) {

        // Check if this toggle ability is currently active
        if (!abilityComponent.isToggleActive(abilityDef.getId())) {
            return; // Toggle is off, don't apply effects
        }

        // Trigger the toggled ability using the central system
        // Use the ability's configured interaction type
        ClassAbilitySystem.TriggerResult result = classAbilitySystem.triggerAbility(
                ref,
                store,
                abilityDef.getId()
        );

        // Log if the toggle ability failed to trigger
        if (result.isFailure()) {
            RpgLogging.debugDeveloper(
                    "Failed to trigger toggle ability '%s' for entity %s: %s",
                    abilityDef.getId(),
                    ref,
                    result.getErrorMessage());
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Process entities that have ClassComponent (where unlocked spells are stored)
        // ClassAbilityComponent will be auto-created if missing
        return Query.and(classComponentType);
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        // For now, we don't belong to a specific system group
        // In the future, this could be part of an ability effects group
        return null;
    }
}
