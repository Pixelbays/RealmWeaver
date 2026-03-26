package org.pixelbays.rpg.global.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class StatModSettings {

    private static final FunctionCodec<RollModifierRange[], List<RollModifierRange>> ROLL_MODIFIER_RANGE_LIST_CODEC =
            new FunctionCodec<>(
                    new ArrayCodec<>(RollModifierRange.CODEC, RollModifierRange[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(RollModifierRange[]::new));

    public static class RollModifierRange {
        public static final BuilderCodec<RollModifierRange> CODEC = BuilderCodec
                .builder(RollModifierRange.class, RollModifierRange::new)
                .append(new KeyedCodec<>("Min", Codec.INTEGER),
                        (i, s) -> i.minInclusive = s, i -> i.minInclusive)
                .add()
                .append(new KeyedCodec<>("Max", Codec.INTEGER),
                        (i, s) -> i.maxInclusive = s, i -> i.maxInclusive)
                .add()
                .append(new KeyedCodec<>("Modifier", Codec.INTEGER),
                        (i, s) -> i.modifier = s, i -> i.modifier)
                .add()
                .build();

        private int minInclusive;
        private int maxInclusive;
        private int modifier;

        public RollModifierRange() {
            this.minInclusive = 0;
            this.maxInclusive = 0;
            this.modifier = 0;
        }

        public int getMinInclusive() {
            return minInclusive;
        }

        public int getMaxInclusive() {
            return maxInclusive;
        }

        public int getModifier() {
            return modifier;
        }
    }

    public static final BuilderCodec<StatModSettings> CODEC = BuilderCodec
            .builder(StatModSettings.class, StatModSettings::new)
            .append(new KeyedCodec<>("AdvantageRollModifiers",
                    new MapCodec<>(ROLL_MODIFIER_RANGE_LIST_CODEC, HashMap::new, false), true),
                    (i, s) -> i.advantageRollModifiers = s, i -> i.advantageRollModifiers)
            .add()
            .build();

    private Map<String, List<RollModifierRange>> advantageRollModifiers;

    public StatModSettings() {
        this.advantageRollModifiers = new HashMap<>();
    }

    public Map<String, List<RollModifierRange>> getAdvantageRollModifiers() {
        return advantageRollModifiers == null ? new HashMap<>() : advantageRollModifiers;
    }
}
