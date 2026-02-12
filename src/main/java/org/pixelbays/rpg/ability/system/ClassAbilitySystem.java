package org.pixelbays.rpg.ability.system;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.AbilityEmpowerComponent;
import org.pixelbays.rpg.ability.component.AbilityTriggerBlockComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition.AbilityType;
import org.pixelbays.rpg.ability.event.AbilityTriggerFailedEvent;
import org.pixelbays.rpg.ability.event.AbilityTriggeredEvent;
import org.pixelbays.rpg.ability.event.BlockAbilityTriggerEvent;
import org.pixelbays.rpg.ability.interaction.AbilityInteractionMeta;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * System that manages class abilities - registration, lookup, and
 * interaction binding.
 * Unlock logic is handled elsewhere.
 */
public class ClassAbilitySystem {

    private final GlobalCoolDown globalCoolDown = new GlobalCoolDown();

    public ClassAbilitySystem(@Nonnull ClassManagementSystem classManagementSystem) {
        // Abilities are loaded via Hytale's asset system
    }

    /**
     * Get ability definition from asset store
     */
    public ClassAbilityDefinition getAbilityDefinition(String abilityId) {
        return ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
    }

    /**
     * Get all registered ability IDs from asset store
     */
    public java.util.Set<String> getRegisteredAbilities() {
        return ClassAbilityDefinition.getAssetMap().getAssetMap().keySet();
    }

    /**
     * Check if an ability is unlocked for an entity.
     */
    public boolean isAbilityUnlocked(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String abilityId) {
        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        return abilityComp != null && abilityComp.hasAbility(abilityId);
    }

    /**
     * Central function to trigger an ability.
     * Handles ability lookup, validation, interaction execution, and error
     * handling.
     * 
     * This is the main entry point for triggering RPG abilities from any system.
     * 
     * @param entityRef       The entity reference triggering the ability
     * @param store           The entity store
     * @param abilityId       The ID of the ability to trigger
     * @param interactionType The type of interaction to execute (Primary, Ability1,
     *                        Ability2, etc.)
     * @return TriggerResult containing success status and any error message
     */
    @Nonnull
    public TriggerResult triggerAbility(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String abilityId,
            @Nonnull InteractionType interactionType) {

        AbilityTriggerBlockComponent blockComponent = store.getComponent(entityRef,
            AbilityTriggerBlockComponent.getComponentType());
        if (blockComponent != null && blockComponent.isBlocked()) {
            String reason = blockComponent.getReason();
            String message = reason == null || reason.isEmpty()
                ? "Abilities are currently blocked."
                : "Abilities are currently blocked: " + reason;
            BlockAbilityTriggerEvent.dispatch(entityRef, abilityId, interactionType, "");
            return fail(entityRef, abilityId, message);
        }

        // Get ability definition
        ClassAbilityDefinition abilityDef = getAbilityDefinition(abilityId);
        if (abilityDef == null) {
            return fail(entityRef, abilityId, "Ability not found: " + abilityId);
        }

        // Check if ability is enabled
        if (!abilityDef.isEnabled()) {
            return fail(entityRef, abilityId, "Ability is disabled: " + abilityDef.getDisplayName());
        }

        GlobalCoolDown.GcdResult gcdResult = globalCoolDown.tryConsume(entityRef, store, abilityDef);
        if (!gcdResult.allowed()) {
            return fail(entityRef, abilityId,
                    "Global cooldown active (" + gcdResult.remainingMs() + "ms)");
        }

        // Get the interaction chain ID from the ability definition
        String chainId = abilityDef.getInteractionChainId();
        if (chainId == null || chainId.isEmpty()) {
            RpgLogging.debugDeveloper("Ability %s has no interaction chain defined", abilityId);
            return fail(entityRef, abilityId,
                    "Ability has no interaction chain: " + abilityDef.getDisplayName());
        }

        // Load the root interaction
        RootInteraction root = RootInteraction.getAssetMap().getAsset(chainId);
        if (root == null) {
            RpgLogging.debugDeveloper("Root interaction not found: %s for ability %s", chainId, abilityId);
            return fail(entityRef, abilityId,
                    "Interaction not found for ability: " + abilityDef.getDisplayName());
        }

        // Get the interaction manager
        InteractionManager manager = store.getComponent(entityRef,
                InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            RpgLogging.debugDeveloper("Interaction manager not available for entity %s", entityRef);
            return fail(entityRef, abilityId, "Interaction manager not available");
        }

        // Create interaction context
        InteractionContext context = InteractionContext.forInteraction(manager, entityRef, interactionType, store);

        // Consume empowerment (if any) and store multiplier for interactions
        AbilityEmpowerComponent empowerComponent = store.getComponent(entityRef, AbilityEmpowerComponent.getComponentType());
        if (empowerComponent != null) {
            float empowerMultiplier = empowerComponent.consumeEmpowerment(abilityId);
            if (empowerMultiplier > 1.0f) {
                context.getMetaStore().putMetaObject(AbilityInteractionMeta.EMPOWER_MULTIPLIER, empowerMultiplier);
                RpgLogging.debugDeveloper("Ability empowered: %s x%.2f", abilityId, empowerMultiplier);
            }
        }

        // Execute the ability
        InteractionChain chain = manager.initChain(interactionType, context, root, false);
        manager.queueExecuteChain(chain);

        RpgLogging.debugDeveloper(
            "Ability triggered: entity=%s, ability=%s, chain=%s, interactionType=%s",
            entityRef,
            abilityId,
            chainId,
            interactionType);

        AbilityTriggeredEvent.dispatch(entityRef, abilityId, interactionType, chainId);

        return TriggerResult.success(abilityDef, chainId);
    }

    /**
     * Trigger an ability with automatic interaction type determination.
     * Uses the ability's InputBinding to determine the interaction type.
     * 
     * @param entityRef The entity reference triggering the ability
     * @param store     The entity store
     * @param abilityId The ID of the ability to trigger
     * @return TriggerResult containing success status and any error message
     */
    @Nonnull
    public TriggerResult triggerAbility(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String abilityId) {

        // Get ability definition to determine interaction type
        ClassAbilityDefinition abilityDef = getAbilityDefinition(abilityId);
        if (abilityDef == null) {
            return TriggerResult.failure("Ability not found: " + abilityId);
        }

        // Determine interaction type from ability binding
        ClassAbilityDefinition.AbilityInputBinding binding = abilityDef.getInputBinding();
        InteractionType type = switch (binding == null ? ClassAbilityDefinition.AbilityInputBinding.Ability1
                : binding) {
            case Ability2 -> InteractionType.Ability2;
            case Ability3 -> InteractionType.Ability3;
            case Ability1 -> InteractionType.Ability1;
        };

        return triggerAbility(entityRef, store, abilityId, type);
    }

    /**
     * Get all passive abilities
     */
    public Set<String> getPassiveAbilities() {
        return getAbilitiesByType(AbilityType.Passive);
    }

    /**
     * Get all toggle abilities
     */
    public Set<String> getToggleAbilities() {
        return getAbilitiesByType(AbilityType.Toggle);
    }

    /**
     * Get all active abilities
     */
    public Set<String> getActiveAbilities() {
        return getAbilitiesByType(AbilityType.Active);
    }

    public void setAbilityTriggersBlocked(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            boolean blocked,
            @Nullable String reason) {
        AbilityTriggerBlockComponent blockComponent = store.getComponent(entityRef,
                AbilityTriggerBlockComponent.getComponentType());
        if (blockComponent == null) {
            blockComponent = store.addComponent(entityRef, AbilityTriggerBlockComponent.getComponentType());
        }
        blockComponent.setBlocked(blocked, reason);
    }

    /**
     * Get all abilities of a specific type
     */
    private Set<String> getAbilitiesByType(AbilityType type) {
        Set<String> abilities = new HashSet<>();
        for (String abilityId : getRegisteredAbilities()) {
            ClassAbilityDefinition def = getAbilityDefinition(abilityId);
            if (def != null && def.getAbilityType() == type) {
                abilities.add(abilityId);
            }
        }
        return abilities;
    }

    private TriggerResult fail(@Nonnull Ref<EntityStore> entityRef, @Nonnull String abilityId,
            @Nonnull String reason) {
        AbilityTriggerFailedEvent.dispatch(entityRef, abilityId, reason);
        return TriggerResult.failure(reason);
    }

    /**
     * Result of an ability trigger attempt.
     * Contains success status, error messages, and ability information.
     */
    public static class TriggerResult {
        private final boolean success;
        private final String errorMessage;
        private final ClassAbilityDefinition abilityDefinition;
        private final String interactionChainId;

        private TriggerResult(boolean success, String errorMessage,
                ClassAbilityDefinition abilityDefinition, String interactionChainId) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.abilityDefinition = abilityDefinition;
            this.interactionChainId = interactionChainId;
        }

        /**
         * Create a successful trigger result
         */
        public static TriggerResult success(@Nonnull ClassAbilityDefinition abilityDefinition,
                @Nonnull String interactionChainId) {
            return new TriggerResult(true, null, abilityDefinition, interactionChainId);
        }

        /**
         * Create a failed trigger result
         */
        public static TriggerResult failure(@Nonnull String errorMessage) {
            return new TriggerResult(false, errorMessage, null, null);
        }

        /**
         * Check if the ability was successfully triggered
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Check if the ability trigger failed
         */
        public boolean isFailure() {
            return !success;
        }

        /**
         * Get the error message (null if successful)
         */
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * Get the ability definition (null if failed)
         */
        @Nullable
        public ClassAbilityDefinition getAbilityDefinition() {
            return abilityDefinition;
        }

        /**
         * Get the interaction chain ID (null if failed)
         */
        @Nullable
        public String getInteractionChainId() {
            return interactionChainId;
        }

        /**
         * Get the ability display name (returns error message if failed)
         */
        @Nonnull
        public String getDisplayName() {
            if (success && abilityDefinition != null) {
                return abilityDefinition.getDisplayName();
            }
            return errorMessage != null ? errorMessage : "Unknown";
        }
    }

}
