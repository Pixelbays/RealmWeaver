package org.pixelbays.rpg.global.interaction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.settings.StatModSettings.RollModifierRange;
import org.pixelbays.rpg.global.system.RNGSystem;
import org.pixelbays.rpg.hud.XpBarHud;
import org.pixelbays.rpg.hud.XpBarHudService;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
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
            .append(new KeyedCodec<>("DiceCount", Codec.INTEGER, false, true),
                    (i, v) -> i.diceCount = v,
                    i -> i.diceCount)
            .add()
            .append(new KeyedCodec<>("DisplayMode", new EnumCodec<>(RollDisplayMode.class), false, true),
                    (i, v) -> i.displayMode = v,
                    i -> i.displayMode)
            .add()
            .append(new KeyedCodec<>("RollTime", Codec.FLOAT, false, true),
                    (i, v) -> i.rollTimeSeconds = v,
                    i -> i.rollTimeSeconds)
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
    private int diceCount = 1;
    private RollDisplayMode displayMode = RollDisplayMode.OFF;
    private float rollTimeSeconds = 1.6f;
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
        RolledDice rolledDice = rollDice(localMin, localMax, diceCount);
        int modifier = resolveStatModifier(context, statId);
        int finalRoll = rolledDice.total + modifier;
        context.getMetaStore().putMetaObject(DiceRollInteractionMeta.DICE_ROLL_RESULT, finalRoll);

        showDiceRoll(context, rolledDice, modifier, finalRoll, localMin, localMax);

        String outcomeInteractionId = resolveOutcome(finalRoll);
        RootInteraction outcomeRoot = resolveOutcomeRoot(outcomeInteractionId);
        if (outcomeRoot != null) {
            triggerOutcome(type, context, outcomeRoot);
        }
    }

    private void triggerOutcome(
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull RootInteraction outcomeRoot) {
        if (displayMode == RollDisplayMode.OFF) {
            context.getState().state = InteractionState.Finished;
            context.execute(outcomeRoot);
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null || store.getExternalData() == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        InteractionContext delayedContext = context.duplicate();
        long delayMillis = Math.max(200L, Math.round(Math.max(0.2f, rollTimeSeconds) * 1000f));
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
            if (!entityRef.isValid()) {
                return;
            }

            InteractionManager manager = store.getComponent(entityRef, InteractionModule.get().getInteractionManagerComponent());
            if (manager == null) {
                return;
            }

            InteractionChain chain = manager.initChain(type, delayedContext, outcomeRoot, false);
            manager.queueExecuteChain(chain);
        }), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void showDiceRoll(
            @Nonnull InteractionContext context,
            @Nonnull RolledDice rolledDice,
            int modifier,
            int finalRoll,
            int localMin,
            int localMax) {
        if (displayMode == RollDisplayMode.OFF) {
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        XpBarHudService hudService = Realmweavers.get().getXpBarHudService();
        if (hudService == null) {
            return;
        }

        hudService.showDiceRoll(entityRef, entityRef.getStore(), new XpBarHudService.DiceRollRequest(
                displayMode.toHudMode(),
                Math.max(0.2f, rollTimeSeconds),
                resolveDiceNotation(localMin, localMax, rolledDice),
                rolledDice.rolls,
                rolledDice.minimums,
                rolledDice.maximums,
                modifier,
                finalRoll));
    }

    @Nonnull
    private RolledDice rollDice(int minInclusive, int maxInclusive, int requestedDiceCount) {
        int safeDiceCount = Math.max(1, requestedDiceCount);
        int[] minimums = distributeTotal(minInclusive, safeDiceCount);
        int[] maximums = distributeTotal(maxInclusive, safeDiceCount);
        int[] rolls = new int[safeDiceCount];
        int total = 0;

        for (int i = 0; i < safeDiceCount; i++) {
            int dieMin = Math.min(minimums[i], maximums[i]);
            int dieMax = Math.max(minimums[i], maximums[i]);
            rolls[i] = RNGSystem.roll(dieMin, dieMax);
            total += rolls[i];
            minimums[i] = dieMin;
            maximums[i] = dieMax;
        }

        return new RolledDice(rolls, minimums, maximums, total);
    }

    @Nonnull
    private static int[] distributeTotal(int total, int count) {
        int safeCount = Math.max(1, count);
        int[] values = new int[safeCount];
        int base = Math.floorDiv(total, safeCount);
        int remainder = total - (base * safeCount);

        Arrays.fill(values, base);

        if (remainder > 0) {
            for (int i = 0; i < remainder; i++) {
                values[i]++;
            }
        } else if (remainder < 0) {
            for (int i = 0; i < Math.abs(remainder); i++) {
                values[safeCount - 1 - i]--;
            }
        }

        return values;
    }

    @Nonnull
    private static String resolveDiceNotation(int minInclusive, int maxInclusive, @Nonnull RolledDice rolledDice) {
        if (rolledDice.rolls.length <= 1) {
            if (rolledDice.minimums[0] == 1) {
                return "d" + rolledDice.maximums[0];
            }
            return minInclusive + "-" + maxInclusive;
        }

        if (allValuesEqual(rolledDice.minimums, 1) && allValuesIdentical(rolledDice.maximums)) {
            return rolledDice.rolls.length + "d" + rolledDice.maximums[0];
        }

        return minInclusive + "-" + maxInclusive;
    }

    private static boolean allValuesEqual(@Nonnull int[] values, int expected) {
        for (int value : values) {
            if (value != expected) {
                return false;
            }
        }
        return values.length > 0;
    }

    private static boolean allValuesIdentical(@Nonnull int[] values) {
        if (values.length == 0) {
            return false;
        }

        int first = values[0];
        for (int value : values) {
            if (value != first) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private RootInteraction resolveOutcomeRoot(int roll) {
        return resolveOutcomeRoot(resolveOutcome(roll));
    }

    @Nullable
    private RootInteraction resolveOutcomeRoot(@Nullable String outcomeInteractionId) {
        if (outcomeInteractionId == null || outcomeInteractionId.isEmpty()) {
            return null;
        }

        RootInteraction rootInteraction = RootInteraction.getAssetMap().getAsset(outcomeInteractionId);
        if (rootInteraction == null) {
            return null;
        }

        return rootInteraction;
    }

    @Nullable
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

    public enum RollDisplayMode {
        OFF,
        SMALL,
        LARGE;

        @Nonnull
        private XpBarHud.DiceOverlayMode toHudMode() {
            return this == LARGE ? XpBarHud.DiceOverlayMode.LARGE : XpBarHud.DiceOverlayMode.SMALL;
        }
    }

    private static final class RolledDice {
        private final int[] rolls;
        private final int[] minimums;
        private final int[] maximums;
        private final int total;

        private RolledDice(@Nonnull int[] rolls, @Nonnull int[] minimums, @Nonnull int[] maximums, int total) {
            this.rolls = rolls;
            this.minimums = minimums;
            this.maximums = maximums;
            this.total = total;
        }
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
