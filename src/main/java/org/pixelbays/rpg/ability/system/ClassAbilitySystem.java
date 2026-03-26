package org.pixelbays.rpg.ability.system;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionCooldown;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.RootInteractionSettings;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.UnarmedInteractions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionTypeUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * System that manages class abilities - registration, lookup, and
 * interaction binding.
 * Unlock logic is handled elsewhere.
 */
public class ClassAbilitySystem {

    private static final long MIN_PARTICLE_DURATION_MS = 50L;
    private static final int PARTICLE_FADE_STEPS = 8;
    private static final float[] DEFAULT_CHARGE_TIMES = new float[] { 0.0F };
    private static final Field INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD = resolveField(InteractionManager.class,
        "cooldownHandler");
    private static final Field COOLDOWN_REMAINING_FIELD = resolveField(CooldownHandler.Cooldown.class,
        "remainingCooldown");

    private final GlobalCoolDown globalCoolDown = new GlobalCoolDown();
    private final Map<ParticleEffectKey, ScheduledParticleEffect> activeParticleEffects = new ConcurrentHashMap<>();

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

        CooldownCheckResult interactionCooldown = checkInteractionCooldown(entityRef, store, manager, interactionType, root);
        if (!interactionCooldown.ready()) {
            sendInteractionCooldownMessage(entityRef, store, abilityDef, interactionCooldown.remainingSeconds());
            return fail(entityRef, abilityId,
                    "Interaction cooldown active (" + formatCooldownSeconds(interactionCooldown.remainingSeconds()) + "s)");
        }

        GlobalCoolDown.GcdResult gcdResult = globalCoolDown.tryConsume(entityRef, store, abilityDef);
        if (!gcdResult.allowed()) {
            return fail(entityRef, abilityId,
                    "Global cooldown active (" + gcdResult.remainingMs() + "ms)");
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
        triggerAbilityEffects(entityRef, store, manager, interactionType, abilityDef, chainId, context);

        RpgLogging.debugDeveloper(
            "Ability triggered: entity=%s, ability=%s, chain=%s, interactionType=%s",
            entityRef,
            abilityId,
            chainId,
            interactionType);

        AbilityTriggeredEvent.dispatch(entityRef, abilityId, interactionType, chainId);

        return TriggerResult.success(abilityDef, chainId);
    }

    private void triggerAbilityEffects(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull InteractionManager manager,
            @Nonnull InteractionType interactionType,
            @Nonnull ClassAbilityDefinition abilityDef,
            @Nonnull String mainChainId,
            @Nonnull InteractionContext context) {
        triggerAnimationChain(entityRef, store, manager, interactionType, abilityDef, mainChainId, context);
        triggerSoundEffect(entityRef, store, abilityDef);
        triggerParticleEffects(entityRef, store, abilityDef);
    }

    private void triggerAnimationChain(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull InteractionManager manager,
            @Nonnull InteractionType interactionType,
            @Nonnull ClassAbilityDefinition abilityDef,
            @Nonnull String mainChainId,
            @Nonnull InteractionContext context) {
        if (!abilityDef.getUsePlayerAnimations()) {
            return;
        }

        String playerAnimationsId = abilityDef.getPlayerAnimationsId();
        if (playerAnimationsId == null || playerAnimationsId.isEmpty()) {
            return;
        }

        UnarmedInteractions animationSet = UnarmedInteractions.getAssetMap().getAsset(playerAnimationsId);
        if (animationSet == null || animationSet.getInteractions() == null) {
            RpgLogging.debugDeveloper("Animation set not found for ability %s: %s", abilityDef.getId(), playerAnimationsId);
            return;
        }

        String animationChainId = animationSet.getInteractions().get(interactionType);
        if (animationChainId == null || animationChainId.isEmpty()) {
            animationChainId = animationSet.getInteractions().get(InteractionType.Primary);
        }
        if (animationChainId == null || animationChainId.isEmpty() || animationChainId.equals(mainChainId)) {
            return;
        }

        RootInteraction animationRoot = RootInteraction.getAssetMap().getAsset(animationChainId);
        if (animationRoot == null) {
            RpgLogging.debugDeveloper(
                "Animation root interaction not found for ability %s: %s",
                abilityDef.getId(),
                animationChainId);
            return;
        }

        InteractionChain animationChain = manager.initChain(interactionType, context.duplicate(), animationRoot, false);
        manager.queueExecuteChain(animationChain);
        RpgLogging.debugDeveloper(
            "Ability animation triggered: entity=%s, ability=%s, animationSet=%s, chain=%s",
            entityRef,
            abilityDef.getId(),
            playerAnimationsId,
            animationChainId);
    }

    private void triggerSoundEffect(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ClassAbilityDefinition abilityDef) {
        String soundEventId = abilityDef.getSoundEventId();
        if (soundEventId == null || soundEventId.isEmpty()) {
            return;
        }

        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == 0 || soundEventIndex == Integer.MIN_VALUE) {
            RpgLogging.debugDeveloper("Ability sound not found for ability %s: %s", abilityDef.getId(), soundEventId);
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
            if (transform == null) {
                return;
            }

            try {
                SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX, transform.getPosition(), store);
            } catch (Exception e) {
                RpgLogging.debugDeveloper("Failed to play ability sound %s for %s: %s", soundEventId, abilityDef.getId(), e.getMessage());
            }
        });
    }

    private void triggerParticleEffects(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ClassAbilityDefinition abilityDef) {
        ModelParticle[] particles = abilityDef.getParticles();
        if (particles == null || particles.length == 0) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            NetworkId networkId = store.getComponent(entityRef, NetworkId.getComponentType());
            if (networkId == null) {
                return;
            }

            TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
            if (transform == null) {
                return;
            }

            ParticleEffectKey effectKey = new ParticleEffectKey(networkId.getId(), abilityDef.getId());
            cancelParticleLifecycle(effectKey);
            sendModelParticles(store, transform, networkId.getId(), particles, 1.0f);
        });
    }

    private void scheduleParticleLifecycle(
            @Nonnull World world,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ParticleEffectKey effectKey,
            int entityId,
            @Nonnull ModelParticle[] particles,
            float durationSeconds,
            boolean fadeOut) {
        cancelParticleLifecycle(effectKey);

        long durationMs = Math.max(MIN_PARTICLE_DURATION_MS, (long) (durationSeconds * 1000.0f));
        ScheduledParticleEffect effect = new ScheduledParticleEffect();
        activeParticleEffects.put(effectKey, effect);

        if (fadeOut) {
            for (int step = 1; step <= PARTICLE_FADE_STEPS; step++) {
                long delayMs = Math.max(1L,
                        Math.round((durationMs * step) / (double) (PARTICLE_FADE_STEPS + 1)));
                float scaleFactor = Math.max(0.0f, 1.0f - (step / (float) (PARTICLE_FADE_STEPS + 1)));
                effect.add(HytaleServer.SCHEDULED_EXECUTOR.schedule(
                        () -> world.execute(() -> updateFadingParticleEffect(
                                entityRef,
                                store,
                                effectKey,
                                effect,
                                entityId,
                                particles,
                                scaleFactor)),
                        delayMs,
                        TimeUnit.MILLISECONDS));
            }
        }

        effect.add(HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> world.execute(() -> clearParticleEffect(entityRef, store, effectKey, effect, entityId)),
                durationMs,
                TimeUnit.MILLISECONDS));
    }

    private void updateFadingParticleEffect(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ParticleEffectKey effectKey,
            @Nonnull ScheduledParticleEffect expectedEffect,
            int entityId,
            @Nonnull ModelParticle[] particles,
            float scaleFactor) {
        if (!isCurrentEffect(effectKey, expectedEffect) || !entityRef.isValid()) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
        if (transform == null) {
            return;
        }

        sendModelParticles(store, transform, entityId, particles, scaleFactor);
    }

    private void clearParticleEffect(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ParticleEffectKey effectKey,
            @Nonnull ScheduledParticleEffect expectedEffect,
            int entityId) {
        if (!isCurrentEffect(effectKey, expectedEffect)) {
            return;
        }

        activeParticleEffects.remove(effectKey, expectedEffect);

        if (!entityRef.isValid()) {
            expectedEffect.cancelAll();
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
        if (transform != null) {
            sendEmptyModelParticles(store, transform, entityId);
        }

        expectedEffect.cancelAll();
    }

    private boolean isCurrentEffect(
            @Nonnull ParticleEffectKey effectKey,
            @Nonnull ScheduledParticleEffect expectedEffect) {
        return activeParticleEffects.get(effectKey) == expectedEffect;
    }

    private void cancelParticleLifecycle(@Nonnull ParticleEffectKey effectKey) {
        ScheduledParticleEffect existing = activeParticleEffects.remove(effectKey);
        if (existing != null) {
            existing.cancelAll();
        }
    }

    private void sendEmptyModelParticles(
            @Nonnull Store<EntityStore> store,
            @Nonnull TransformComponent transform,
            int entityId) {
        sendModelParticlesPacket(store, transform, new SpawnModelParticles(entityId, new com.hypixel.hytale.protocol.ModelParticle[0]));
    }

    private void sendModelParticles(
            @Nonnull Store<EntityStore> store,
            @Nonnull TransformComponent transform,
            int entityId,
            @Nonnull ModelParticle[] particles,
            float scaleFactor) {
        com.hypixel.hytale.protocol.ModelParticle[] protocolParticles =
                new com.hypixel.hytale.protocol.ModelParticle[particles.length];
        for (int i = 0; i < particles.length; i++) {
            ModelParticle scaledParticle = new ModelParticle(particles[i]);
            if (scaleFactor != 1.0f) {
                scaledParticle.scale(scaleFactor);
            }
            protocolParticles[i] = scaledParticle.toPacket();
        }

        sendModelParticlesPacket(store, transform, new SpawnModelParticles(entityId, protocolParticles));
    }

    private void sendModelParticlesPacket(
            @Nonnull Store<EntityStore> store,
            @Nonnull TransformComponent transform,
            @Nonnull SpawnModelParticles packet) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        ObjectList<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(transform.getPosition(), 96.0, nearbyPlayers);

        for (Ref<EntityStore> playerRef : nearbyPlayers) {
            if (!playerRef.isValid()) {
                continue;
            }

            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                playerRefComponent.getPacketHandler().writeNoCache(packet);
            }
        }
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

    @Nonnull
    private CooldownCheckResult checkInteractionCooldown(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull InteractionManager manager,
            @Nonnull InteractionType interactionType,
            @Nonnull RootInteraction root) {
        CooldownHandler cooldownHandler = getCooldownHandler(manager);
        if (cooldownHandler == null) {
            return CooldownCheckResult.available();
        }

        ResolvedCooldown resolved = resolveInteractionCooldown(entityRef, store, interactionType, root);
        if (resolved.cooldownTime() <= 0.0f) {
            return CooldownCheckResult.available();
        }

        CooldownHandler.Cooldown cooldown = cooldownHandler.getCooldown(
                resolved.cooldownId(),
                resolved.cooldownTime(),
                resolved.chargeTimes(),
                root.resetCooldownOnStart(),
                resolved.interruptRecharge());
        if (cooldown == null || !cooldown.hasCooldown(false)) {
            return CooldownCheckResult.available();
        }

        return CooldownCheckResult.blocked(Math.max(0.0f, getRemainingCooldownSeconds(cooldown)));
    }

    @Nonnull
    private ResolvedCooldown resolveInteractionCooldown(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull InteractionType interactionType,
            @Nonnull RootInteraction root) {
        String cooldownId = root.getId();
        float cooldownTime = InteractionTypeUtils.getDefaultCooldown(interactionType);
        float[] chargeTimes = DEFAULT_CHARGE_TIMES;
        boolean interruptRecharge = false;

        InteractionCooldown cooldown = root.getCooldown();
        if (cooldown != null) {
            cooldownTime = cooldown.cooldown;
            if (cooldown.chargeTimes != null && cooldown.chargeTimes.length > 0) {
                chargeTimes = cooldown.chargeTimes;
            }
            if (cooldown.cooldownId != null && !cooldown.cooldownId.isEmpty()) {
                cooldownId = cooldown.cooldownId;
            }
            interruptRecharge = cooldown.interruptRecharge;
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        GameMode gameMode = player != null ? player.getGameMode() : GameMode.Adventure;
        RootInteractionSettings settings = root.getSettings().get(gameMode);
        if (settings != null && settings.cooldown != null) {
            InteractionCooldown settingsCooldown = settings.cooldown;
            cooldownTime = settingsCooldown.cooldown;
            if (settingsCooldown.chargeTimes != null && settingsCooldown.chargeTimes.length > 0) {
                chargeTimes = settingsCooldown.chargeTimes;
            }
            if (settingsCooldown.cooldownId != null && !settingsCooldown.cooldownId.isEmpty()) {
                cooldownId = settingsCooldown.cooldownId;
            }
            interruptRecharge = interruptRecharge || settingsCooldown.interruptRecharge;
        }

        return new ResolvedCooldown(cooldownId, cooldownTime, chargeTimes, interruptRecharge);
    }

    @Nullable
    private CooldownHandler getCooldownHandler(@Nonnull InteractionManager manager) {
        if (INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD == null) {
            return null;
        }

        try {
            return (CooldownHandler) INTERACTION_MANAGER_COOLDOWN_HANDLER_FIELD.get(manager);
        } catch (IllegalAccessException e) {
            RpgLogging.debugDeveloper("Failed to access interaction cooldown handler: %s", e.getMessage());
            return null;
        }
    }

    private float getRemainingCooldownSeconds(@Nonnull CooldownHandler.Cooldown cooldown) {
        if (COOLDOWN_REMAINING_FIELD == null) {
            return cooldown.getCooldown();
        }

        try {
            return COOLDOWN_REMAINING_FIELD.getFloat(cooldown);
        } catch (IllegalAccessException e) {
            RpgLogging.debugDeveloper("Failed to read interaction cooldown remaining time: %s", e.getMessage());
            return cooldown.getCooldown();
        }
    }

    private void sendInteractionCooldownMessage(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull ClassAbilityDefinition abilityDef,
            float remainingSeconds) {
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        playerRef.sendMessage(Message.translation("pixelbays.rpg.ability.error.cooldown")
            .param("ability", Message.translation(abilityDef.getTranslationKey()))
            .param("time", formatCooldownSeconds(remainingSeconds)));
    }

    @Nonnull
    private String formatCooldownSeconds(float remainingSeconds) {
        if (remainingSeconds >= 10.0f) {
            return Integer.toString((int) Math.ceil(remainingSeconds));
        }

        float rounded = Math.round(remainingSeconds * 10.0f) / 10.0f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.0001f) {
            return Integer.toString(Math.round(rounded));
        }

        return String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }

    @Nullable
    private static Field resolveField(@Nonnull Class<?> owner, @Nonnull String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
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

    private static final class ParticleEffectKey {
        private final int entityId;
        private final String abilityId;

        private ParticleEffectKey(int entityId, @Nonnull String abilityId) {
            this.entityId = entityId;
            this.abilityId = abilityId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof ParticleEffectKey other)) {
                return false;
            }

            return this.entityId == other.entityId && this.abilityId.equals(other.abilityId);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(this.entityId);
            return 31 * result + this.abilityId.hashCode();
        }
    }

    private static final class ScheduledParticleEffect {
        private final List<ScheduledFuture<?>> futures = new ArrayList<>();

        private void add(@Nonnull ScheduledFuture<?> future) {
            this.futures.add(future);
        }

        private void cancelAll() {
            for (ScheduledFuture<?> future : this.futures) {
                future.cancel(false);
            }
            this.futures.clear();
        }
    }

    private record ResolvedCooldown(
            @Nonnull String cooldownId,
            float cooldownTime,
            @Nonnull float[] chargeTimes,
            boolean interruptRecharge) {
    }

    private record CooldownCheckResult(boolean ready, float remainingSeconds) {
        private static CooldownCheckResult available() {
            return new CooldownCheckResult(true, 0.0f);
        }

        private static CooldownCheckResult blocked(float remainingSeconds) {
            return new CooldownCheckResult(false, remainingSeconds);
        }
    }

}
