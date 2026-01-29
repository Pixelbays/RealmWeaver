package org.pixelbays.rpg.leveling.config;

import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

/**
 * Stat growth configuration (percentage or flat increases per level)
 */
public class StatGrowthConfig {
    private static final FunctionCodec<Map<String, Map<String, Float>>, Map<Integer, Map<String, Float>>> INT_FLOAT_MAP_MAP_CODEC = new FunctionCodec<>(
            new MapCodec<>(new MapCodec<>(Codec.FLOAT, HashMap::new), HashMap::new),
            StatGrowthConfig::toIntKeyNestedFloatMap,
            StatGrowthConfig::toStringKeyNestedFloatMap);

        public static final BuilderCodec<StatGrowthConfig> STAT_GROWTH_CODEC = BuilderCodec
            .builder(StatGrowthConfig.class, StatGrowthConfig::new)
            .appendInherited(new KeyedCodec<>("FlatGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.FlatGrowth = s, i -> i.FlatGrowth,
                    (i, parent) -> i.FlatGrowth = parent.FlatGrowth)
            .addValidator(EntityStatType.VALIDATOR_CACHE
                    .getMapKeyValidator())
            .add()
            .appendInherited(
                    new KeyedCodec<>("PercentageGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.PercentageGrowth = s, i -> i.PercentageGrowth,
                    (i, parent) -> i.PercentageGrowth = parent.PercentageGrowth)
            .addValidator(EntityStatType.VALIDATOR_CACHE
                    .getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("MilestoneGrowth", INT_FLOAT_MAP_MAP_CODEC, false, true),
                    (i, s) -> i.MilestoneGrowth = s, i -> i.MilestoneGrowth,
                    (i, parent) -> i.MilestoneGrowth = parent.MilestoneGrowth)
            .add()
            .build();
    public static final BuilderCodec<StatGrowthConfig> CODEC = STAT_GROWTH_CODEC;

    private Map<String, Float> FlatGrowth; // Flat increases per level
    private Map<String, Float> PercentageGrowth; // Percentage increases per level
    private Map<Integer, Map<String, Float>> MilestoneGrowth; // Level -> Stat -> Bonus

    public StatGrowthConfig() {
        this.FlatGrowth = new HashMap<>();
        this.PercentageGrowth = new HashMap<>();
        this.MilestoneGrowth = new HashMap<>();
    }

    // Getters and setters
    public Map<String, Float> getFlatGrowth() {
        return FlatGrowth;
    }

    public void setFlatGrowth(Map<String, Float> flatGrowth) {
        this.FlatGrowth = flatGrowth;
    }

    public Map<String, Float> getPercentageGrowth() {
        return PercentageGrowth;
    }

    public void setPercentageGrowth(Map<String, Float> percentageGrowth) {
        this.PercentageGrowth = percentageGrowth;
    }

    public Map<Integer, Map<String, Float>> getMilestoneGrowth() {
        return MilestoneGrowth;
    }

    public void setMilestoneGrowth(Map<Integer, Map<String, Float>> milestoneGrowth) {
        this.MilestoneGrowth = milestoneGrowth;
    }

    private static Map<Integer, Map<String, Float>> toIntKeyNestedFloatMap(Map<String, Map<String, Float>> map) {
        Map<Integer, Map<String, Float>> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<String, Map<String, Float>> entry : map.entrySet()) {
            Integer key = parseIntKey(entry.getKey());
            if (key != null) {
                converted.put(key, entry.getValue());
            }
        }

        return converted;
    }

    private static Map<String, Map<String, Float>> toStringKeyNestedFloatMap(Map<Integer, Map<String, Float>> map) {
        Map<String, Map<String, Float>> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<Integer, Map<String, Float>> entry : map.entrySet()) {
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return converted;
    }

    private static Integer parseIntKey(String key) {
        if (key == null) {
            return null;
        }

        try {
            return Integer.valueOf(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}