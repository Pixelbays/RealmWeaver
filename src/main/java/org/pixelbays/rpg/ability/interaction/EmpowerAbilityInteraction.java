package org.pixelbays.rpg.ability.interaction;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.ability.component.AbilityEmpowerComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionCooldown;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Grants a chance to empower an ability and/or reset its cooldown for future casts.
 */
@SuppressWarnings({"FieldHidesSuperclassField", "null"})
public class EmpowerAbilityInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<EmpowerAbilityInteraction> CODEC = BuilderCodec.builder(
            EmpowerAbilityInteraction.class, EmpowerAbilityInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Grants a chance to empower an ability and/or reset its cooldown.")
            .append(new KeyedCodec<>("AbilityId", Codec.STRING),
                    (i, v) -> i.abilityId = v,
                    i -> i.abilityId)
            .add()
            .append(new KeyedCodec<>("Chance", Codec.FLOAT),
                    (i, v) -> i.chance = v,
                    i -> i.chance)
                .addValidator(Validators.greaterThanOrEqual(0.0f))
                .addValidator(Validators.max(1.0f))
            .add()
            .append(new KeyedCodec<>("ResetCooldown", Codec.BOOLEAN),
                    (i, v) -> i.resetCooldown = v,
                    i -> i.resetCooldown)
            .add()
            .append(new KeyedCodec<>("EmpowerCasts", Codec.INTEGER),
                    (i, v) -> i.empowerCasts = v,
                    i -> i.empowerCasts)
            .addValidator(Validators.greaterThanOrEqual(0))
            .add()
            .append(new KeyedCodec<>("EmpowerMultiplier", Codec.FLOAT),
                    (i, v) -> i.empowerMultiplier = v,
                    i -> i.empowerMultiplier)
            .addValidator(Validators.greaterThanOrEqual(1.0f))
            .add()
            .build();

    private String abilityId = "";
    private float chance = 1.0f;
    private boolean resetCooldown = true;
    private int empowerCasts = 0;
    private float empowerMultiplier = 1.0f;

    public EmpowerAbilityInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        if (abilityId == null || abilityId.isEmpty()) {
            return;
        }

        if (!rollChance()) {
            return;
        }

        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            return;
        }

        if (resetCooldown) {
            resetAbilityCooldown(abilityDef, cooldownHandler);
        }

        if (empowerCasts > 0) {
            Ref<EntityStore> casterRef = context.getEntity();
            if (casterRef != null && casterRef.isValid()) {
                CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
                if (commandBuffer != null) {
                    AbilityEmpowerComponent empowerComponent = commandBuffer.getComponent(
                            casterRef, AbilityEmpowerComponent.getComponentType());
                    if (empowerComponent == null) {
                        empowerComponent = commandBuffer.addComponent(casterRef, AbilityEmpowerComponent.getComponentType());
                    }

                    if (empowerComponent != null) {
                        empowerComponent.addEmpowerment(abilityId, empowerCasts, empowerMultiplier);
                    }
                }
            }
        }
    }

    private boolean rollChance() {
        if (chance >= 1.0f) {
            return true;
        }
        if (chance <= 0.0f) {
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() <= chance;
    }

    private void resetAbilityCooldown(@Nonnull ClassAbilityDefinition abilityDef,
            @Nonnull CooldownHandler cooldownHandler) {
        String chainId = abilityDef.getInteractionChainId();
        if (chainId == null || chainId.isEmpty()) {
            return;
        }

        RootInteraction root = RootInteraction.getAssetMap().getAsset(chainId);
        if (root == null) {
            return;
        }

        InteractionCooldown cooldown = root.getCooldown();
        if (cooldown == null) {
            return;
        }

        String cooldownId = (cooldown.cooldownId != null && !cooldown.cooldownId.isEmpty())
                ? cooldown.cooldownId
                : chainId;
        float cooldownTime = cooldown.cooldown;
        float[] chargeTimes = cooldown.chargeTimes != null && cooldown.chargeTimes.length > 0
                ? cooldown.chargeTimes
                : new float[] { cooldownTime };

        CooldownHandler.Cooldown handlerCooldown = cooldownHandler.getCooldown(
                cooldownId,
                cooldownTime,
                chargeTimes,
                true,
                cooldown.interruptRecharge);
        if (handlerCooldown == null) {
            return;
        }

        // Clear remaining cooldown and restore charges
        handlerCooldown.increaseTime(-cooldownTime);
        handlerCooldown.replenishCharge(chargeTimes.length, true);
    }

    @Nonnull
    @Override
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }

    @Override
    protected void configurePacket(Interaction packet) {
        super.configurePacket(packet);
    }
}
