package org.pixelbays.rpg.leveling.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;
import org.pixelbays.rpg.global.config.builder.InteractionChainRefCodec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

/**
 * Level reward configuration
 */
public class LevelRewardConfig {
    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final Codec<String> ABILITY_REF_CODEC = new AbilityRefCodec();
    private static final Codec<String> INTERACTION_CHAIN_REF_CODEC = new InteractionChainRefCodec();

    private static final FunctionCodec<String[], List<String>> ABILITY_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ABILITY_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    public static final BuilderCodec<LevelRewardConfig> LEVEL_REWARD_CODEC = BuilderCodec
            .builder(LevelRewardConfig.class, LevelRewardConfig::new)
            .appendInherited(new KeyedCodec<>("StatPoints", Codec.INTEGER, false, true), (i, s) -> i.StatPoints = s,
                    i -> i.StatPoints,
                    (i, parent) -> i.StatPoints = parent.StatPoints)
            .add()
            .appendInherited(new KeyedCodec<>("SkillPoints", Codec.INTEGER, false, true), (i, s) -> i.SkillPoints = s,
                    i -> i.SkillPoints,
                    (i, parent) -> i.SkillPoints = parent.SkillPoints)
            .add()
            .appendInherited(new KeyedCodec<>("StatIncreases", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.StatIncreases = s, i -> i.StatIncreases,
                    (i, parent) -> i.StatIncreases = parent.StatIncreases)
            .addValidator(EntityStatType.VALIDATOR_CACHE
                    .getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("UnlockedAbilities", ABILITY_ID_LIST_CODEC, false, true),
                    (i, s) -> i.UnlockedAbilities = s, i -> i.UnlockedAbilities,
                    (i, parent) -> i.UnlockedAbilities = parent.UnlockedAbilities)
            .add()
            .appendInherited(new KeyedCodec<>("UnlockedQuests", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.UnlockedQuests = s, i -> i.UnlockedQuests,
                    (i, parent) -> i.UnlockedQuests = parent.UnlockedQuests)
            .add()
            .appendInherited(new KeyedCodec<>("CurrencyRewards", new MapCodec<>(Codec.LONG, HashMap::new), false, true),
                    (i, s) -> i.CurrencyRewards = s, i -> i.CurrencyRewards,
                    (i, parent) -> i.CurrencyRewards = parent.CurrencyRewards)
            .add()
            .appendInherited(new KeyedCodec<>("ItemRewards", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.ItemRewards = s, i -> i.ItemRewards,
                    (i, parent) -> i.ItemRewards = parent.ItemRewards)
            .add()
            .appendInherited(new KeyedCodec<>("InteractionChain", INTERACTION_CHAIN_REF_CODEC, false, true),
                    (i, s) -> i.InteractionChain = s, i -> i.InteractionChain,
                    (i, parent) -> i.InteractionChain = parent.InteractionChain)
            .add()
                .appendInherited(new KeyedCodec<>("LevelUpEffects", LevelUpEffects.CODEC, false, true),
                    (i, s) -> i.levelUpEffects = s, i -> i.levelUpEffects,
                    (i, parent) -> i.levelUpEffects = parent.levelUpEffects)
            .add()
            .build();

    public static final BuilderCodec<LevelRewardConfig> CODEC = LEVEL_REWARD_CODEC;

    public static final FunctionCodec<Map<String, LevelRewardConfig>, Map<Integer, LevelRewardConfig>> INT_REWARD_MAP_CODEC = new FunctionCodec<>(
            new MapCodec<>(LevelRewardConfig.CODEC, HashMap::new),
            LevelRewardConfig::toIntKeyRewardMap,
            LevelRewardConfig::toStringKeyRewardMap);

    private int StatPoints; // Allocatable stat points
    private int SkillPoints; // Skill/talent points
    private Map<String, Float> StatIncreases; // Auto stat increases (Health, Mana, etc.)
    private List<String> UnlockedAbilities; // Abilities unlocked at this level
    private List<String> UnlockedQuests; // Quests unlocked
    private Map<String, Long> CurrencyRewards; // Currency rewards
    private List<String> ItemRewards; // Item IDs to grant
    private String InteractionChain; // Optional interaction to execute

    // Level up effects
    private LevelUpEffects levelUpEffects; // Sound, particles, notification on level up

    public LevelRewardConfig() {
        this.StatPoints = 0;
        this.SkillPoints = 0;
        this.StatIncreases = new HashMap<>();
        this.UnlockedAbilities = new ArrayList<>();
        this.UnlockedQuests = new ArrayList<>();
        this.CurrencyRewards = new HashMap<>();
        this.ItemRewards = new ArrayList<>();
        this.levelUpEffects = new LevelUpEffects();
    }

    // Getters and setters
    public int getStatPoints() {
        return StatPoints;
    }

    public void setStatPoints(int statPoints) {
        this.StatPoints = statPoints;
    }

    public int getSkillPoints() {
        return SkillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.SkillPoints = skillPoints;
    }

    public Map<String, Float> getStatIncreases() {
        return StatIncreases;
    }

    public void setStatIncreases(Map<String, Float> statIncreases) {
        this.StatIncreases = statIncreases;
    }

    public List<String> getUnlockedAbilities() {
        return UnlockedAbilities;
    }

    public void setUnlockedAbilities(List<String> unlockedAbilities) {
        this.UnlockedAbilities = unlockedAbilities;
    }

    public List<String> getUnlockedQuests() {
        return UnlockedQuests;
    }

    public void setUnlockedQuests(List<String> unlockedQuests) {
        this.UnlockedQuests = unlockedQuests;
    }

    public Map<String, Long> getCurrencyRewards() {
        return CurrencyRewards;
    }

    public void setCurrencyRewards(Map<String, Long> currencyRewards) {
        this.CurrencyRewards = currencyRewards;
    }

    public List<String> getItemRewards() {
        return ItemRewards;
    }

    public void setItemRewards(List<String> itemRewards) {
        this.ItemRewards = itemRewards;
    }

    public String getInteractionChain() {
        return InteractionChain;
    }

    public void setInteractionChain(String interactionChain) {
        this.InteractionChain = interactionChain;
    }

    public LevelUpEffects getLevelUpEffects() {
        return levelUpEffects;
    }

    public void setLevelUpEffects(LevelUpEffects levelUpEffects) {
        this.levelUpEffects = levelUpEffects;
    }

    private static Map<Integer, LevelRewardConfig> toIntKeyRewardMap(Map<String, LevelRewardConfig> map) {
        Map<Integer, LevelRewardConfig> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<String, LevelRewardConfig> entry : map.entrySet()) {
            Integer key = parseIntKey(entry.getKey());
            if (key != null) {
                converted.put(key, entry.getValue());
            }
        }

        return converted;
    }

    private static Map<String, LevelRewardConfig> toStringKeyRewardMap(Map<Integer, LevelRewardConfig> map) {
        Map<String, LevelRewardConfig> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<Integer, LevelRewardConfig> entry : map.entrySet()) {
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