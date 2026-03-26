package org.pixelbays.rpg.global.interaction;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.settings.StatModSettings.RollModifierRange;
import org.pixelbays.rpg.global.system.RNGSystem;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Rolls a dice between Min and Max, then applies an optional stat-based
 * modifier.
 * Stores the final result in the interaction context meta store.
 */
@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class DiceRollInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<DiceRollInteraction> DICE_ROLL_CODEC = BuilderCodec.builder(
            DiceRollInteraction.class, DiceRollInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Rolls a dice and applies optional stat-based modifiers.")
            .append(new KeyedCodec<>("Min", Codec.INTEGER),
                    (i, v) -> i.min = v,
                    i -> i.min)
            .add()
            .append(new KeyedCodec<>("Max", Codec.INTEGER),
                    (i, v) -> i.max = v,
                    i -> i.max)
            .add()
            .append(new KeyedCodec<>("Stat", Codec.STRING),
                    (i, v) -> i.statId = v,
                    i -> i.statId)
            .add()
            .append(new KeyedCodec<>("OutcomeRanges",
                    new ArrayCodec<>(RollOutcomeRange.CODEC, RollOutcomeRange[]::new)),
                    (i, v) -> i.outcomeRanges = v,
                    i -> i.outcomeRanges)
            .add()
            .build();

    private int min = 1;
    private int max = 20;
    private String statId = "";
    private RollOutcomeRange[] outcomeRanges = new RollOutcomeRange[0];

    public DiceRollInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        int localMin = Math.min(min, max);
        int localMax = Math.max(min, max);
        int roll = RNGSystem.roll(localMin, localMax);
        int modifier = resolveStatModifier(context, statId);
        int finalRoll = roll + modifier;
        context.getMetaStore().putMetaObject(DiceRollInteractionMeta.DICE_ROLL_RESULT, finalRoll);

        String outcomeInteractionId = resolveOutcome(finalRoll);
        if (outcomeInteractionId != null && !outcomeInteractionId.isEmpty()
                && RootInteraction.getAssetMap().getAsset(outcomeInteractionId) != null) {
            context.getState().state = InteractionState.Finished;
            context.execute(RootInteraction.getAssetMap().getAsset(outcomeInteractionId));
        }
    }

    private String resolveOutcome(int roll) {
        if (outcomeRanges == null || outcomeRanges.length == 0) {
            return null;
        }

        for (RollOutcomeRange range : outcomeRanges) {
            if (range == null) {
                continue;
            }
            if (roll >= range.minInclusive && roll <= range.maxInclusive) {
                return range.nextInteractionId;
            }
        }

        return null;
    }

    private int resolveStatModifier(@Nonnull InteractionContext context, String statId) {
        if (statId == null || statId.isEmpty()) {
            return 0;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return 0;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        EntityStatMap statMap = commandBuffer.getComponent(entityRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0;
        }

        int statIndex = EntityStatType.getAssetMap().getIndex(statId);
        if (statIndex == Integer.MIN_VALUE) {
            return 0;
        }

        EntityStatValue statValue = statMap.get(statIndex);
        if (statValue == null) {
            return 0;
        }

        int stat = (int) Math.floor(statValue.get());
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config == null) {
            return 0;
        }

        Map<String, List<RollModifierRange>> modifierMap = config.getAdvantageRollModifiers();
        if (modifierMap == null) {
            return 0;
        }

        List<RollModifierRange> ranges = modifierMap.get(statId);
        if (ranges == null || ranges.isEmpty()) {
            return 0;
        }

        for (RollModifierRange range : ranges) {
            if (range == null) {
                continue;
            }
            if (stat >= range.getMinInclusive() && stat <= range.getMaxInclusive()) {
                return range.getModifier();
            }
        }

        return 0;
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

    public static class RollOutcomeRange {
        public static final BuilderCodec<RollOutcomeRange> CODEC = BuilderCodec
                .builder(RollOutcomeRange.class, RollOutcomeRange::new)
                .append(new KeyedCodec<>("Min", Codec.INTEGER),
                        (i, v) -> i.minInclusive = v,
                        i -> i.minInclusive)
                .add()
                .append(new KeyedCodec<>("Max", Codec.INTEGER),
                        (i, v) -> i.maxInclusive = v,
                        i -> i.maxInclusive)
                .add()
                .append(new KeyedCodec<>("NextInteraction", RootInteraction.CHILD_ASSET_CODEC),
                        (i, v) -> i.nextInteractionId = v,
                        i -> i.nextInteractionId)
                .add()
                .build();

        private int minInclusive;
        private int maxInclusive;
        private String nextInteractionId;

        public RollOutcomeRange() {
            this.minInclusive = 0;
            this.maxInclusive = 0;
            this.nextInteractionId = "";
        }
    }
}
