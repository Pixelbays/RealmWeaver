package org.pixelbays.rpg.global.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

/**
 * Placeholder interaction to force a target entity to focus the caster.
 * Currently implemented by adding the caster to the target's TargetMemory hostiles list.
 */
@SuppressWarnings({"FieldHidesSuperclassField", "null"})
public class ForceTargetInteraction extends SimpleInstantInteraction {
    @Nonnull
    @SuppressWarnings("all")
    public static final BuilderCodec<ForceTargetInteraction> FORCE_TARGET_CODEC = BuilderCodec.builder(
            ForceTargetInteraction.class, ForceTargetInteraction::new, SimpleInstantInteraction.CODEC
        )
        .documentation("Adds the caster as a hostile target on the selected entity (placeholder taunt).")
        .<Float>appendInherited(
            new KeyedCodec<>("Duration", Codec.FLOAT),
            (o, i) -> o.durationSeconds = i,
            o -> o.durationSeconds,
            (o, p) -> o.durationSeconds = p.durationSeconds
        )
        .addValidator(Validators.greaterThanOrEqual(0.0F))
        .documentation("How long the target should remember the caster as hostile.")
        .add()
        .build();

    private float durationSeconds = 3.0f;

    public ForceTargetInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }

        TargetMemory targetMemory = commandBuffer.getComponent(targetRef, TargetMemory.getComponentType());
        if (targetMemory == null) {
            return;
        }

        Ref<EntityStore> casterRef = context.getEntity();
        if (casterRef == null || !casterRef.isValid()) {
            return;
        }

        Int2FloatOpenHashMap hostiles = targetMemory.getKnownHostiles();
        float duration = durationSeconds > 0.0f ? durationSeconds : targetMemory.getRememberFor();
        if (hostiles.put(casterRef.getIndex(), duration) <= 0.0f) {
            targetMemory.getKnownHostilesList().add(casterRef);
        }
        targetMemory.setClosestHostile(casterRef);
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
