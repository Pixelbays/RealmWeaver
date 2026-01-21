package org.pixelbays.rpg.leveling.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;
import org.pixelbays.rpg.global.config.builder.InteractionChainRefCodec;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

/**
 * Configuration for a level progression system loaded from asset pack.
 * Example file: Server/Entity/Stats/Level_Character.json
 */
@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "StringOperationCanBeSimplified" })
public class LevelSystemConfig implements
        com.hypixel.hytale.assetstore.map.JsonAssetWithMap<String, com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig>> {

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

    @SuppressWarnings("unused")
    private static final FunctionCodec<Map<String, Float>, Map<Integer, Float>> INT_FLOAT_MAP_CODEC = new FunctionCodec<>(
            new MapCodec<>(Codec.FLOAT, HashMap::new),
            LevelSystemConfig::toIntKeyFloatMap,
            LevelSystemConfig::toStringKeyFloatMap);

    private static final FunctionCodec<Map<String, Map<String, Float>>, Map<Integer, Map<String, Float>>> INT_FLOAT_MAP_MAP_CODEC = new FunctionCodec<>(
            new MapCodec<>(new MapCodec<>(Codec.FLOAT, HashMap::new), HashMap::new),
            LevelSystemConfig::toIntKeyNestedFloatMap,
            LevelSystemConfig::toStringKeyNestedFloatMap);

    public static final BuilderCodec<NotificationConfig> NOTIFICATION_CODEC = BuilderCodec
            .builder(NotificationConfig.class, NotificationConfig::new)
            .appendInherited(new KeyedCodec<>("PrimaryMessage", Codec.STRING, false, true), (i, s) -> i.PrimaryMessage = s,
                i -> i.PrimaryMessage,
                (i, parent) -> i.PrimaryMessage = parent.PrimaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("SecondaryMessage", Codec.STRING, false, true), (i, s) -> i.SecondaryMessage = s,
                i -> i.SecondaryMessage,
                (i, parent) -> i.SecondaryMessage = parent.SecondaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("IconItemId", Codec.STRING),
                    (i, s) -> i.IconItemId = (s == null || s.isEmpty()) ? null : s,
                    i -> i.IconItemId,
                    (i, parent) -> i.IconItemId = parent.IconItemId)
            .add()

            .build();

    public static final BuilderCodec<EventTitleConfig> EVENT_TITLE_CODEC = BuilderCodec
            .builder(EventTitleConfig.class, EventTitleConfig::new)
            .appendInherited(new KeyedCodec<>("PrimaryMessage", Codec.STRING, false, true), (i, s) -> i.PrimaryMessage = s,
                i -> i.PrimaryMessage,
                (i, parent) -> i.PrimaryMessage = parent.PrimaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("SecondaryMessage", Codec.STRING, false, true), (i, s) -> i.SecondaryMessage = s,
                i -> i.SecondaryMessage,
                (i, parent) -> i.SecondaryMessage = parent.SecondaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("Major", Codec.BOOLEAN, false, true), (i, s) -> i.Major = s, i -> i.Major,
                (i, parent) -> i.Major = parent.Major)
            .add()
            .build();

    public static final BuilderCodec<LevelUpEffects> LEVEL_UP_EFFECTS_CODEC = BuilderCodec
            .builder(LevelUpEffects.class, LevelUpEffects::new)
            .appendInherited(new KeyedCodec<>("SoundId", Codec.STRING, false, true), (i, s) -> i.SoundId = s,
                i -> i.SoundId,
                (i, parent) -> i.SoundId = parent.SoundId)
            .add()
            .appendInherited(new KeyedCodec<>("ParticleEffect", Codec.STRING, false, true), (i, s) -> i.ParticleEffect = s,
                i -> i.ParticleEffect,
                (i, parent) -> i.ParticleEffect = parent.ParticleEffect)
            .add()
            .appendInherited(new KeyedCodec<>("Notification", NotificationConfig.CODEC, false, true),
                (i, s) -> i.Notification = s, i -> i.Notification,
                (i, parent) -> i.Notification = parent.Notification)
            .add()
            .appendInherited(new KeyedCodec<>("EventTitle", EventTitleConfig.CODEC, false, true),
                (i, s) -> i.EventTitle = s, i -> i.EventTitle,
                (i, parent) -> i.EventTitle = parent.EventTitle)
            .add()
            .build();


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
            .addValidator(com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType.VALIDATOR_CACHE
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
                (i, s) -> i.LevelUpEffects = s, i -> i.LevelUpEffects,
                (i, parent) -> i.LevelUpEffects = parent.LevelUpEffects)
            .add()
            .build();

    private static final FunctionCodec<Map<String, LevelRewardConfig>, Map<Integer, LevelRewardConfig>> INT_REWARD_MAP_CODEC = new FunctionCodec<>(
            new MapCodec<>(LevelRewardConfig.CODEC, HashMap::new),
            LevelSystemConfig::toIntKeyRewardMap,
            LevelSystemConfig::toStringKeyRewardMap);

    public static final BuilderCodec<StatGrowthConfig> STAT_GROWTH_CODEC = BuilderCodec
            .builder(StatGrowthConfig.class, StatGrowthConfig::new)
            .appendInherited(new KeyedCodec<>("FlatGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                (i, s) -> i.FlatGrowth = s, i -> i.FlatGrowth,
                (i, parent) -> i.FlatGrowth = parent.FlatGrowth)
            .addValidator(com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType.VALIDATOR_CACHE
                    .getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("PercentageGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                (i, s) -> i.PercentageGrowth = s, i -> i.PercentageGrowth,
                (i, parent) -> i.PercentageGrowth = parent.PercentageGrowth)
            .addValidator(com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType.VALIDATOR_CACHE
                    .getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("MilestoneGrowth", INT_FLOAT_MAP_MAP_CODEC, false, true),
                (i, s) -> i.MilestoneGrowth = s, i -> i.MilestoneGrowth,
                (i, parent) -> i.MilestoneGrowth = parent.MilestoneGrowth)
            .add()
            .build();

    public static final com.hypixel.hytale.assetstore.codec.AssetBuilderCodec<String, LevelSystemConfig> CODEC = com.hypixel.hytale.assetstore.codec.AssetBuilderCodec
            .builder(
                    LevelSystemConfig.class,
                    LevelSystemConfig::new,
                    Codec.STRING,
                    (t, k) -> t.id = k,
                    t -> t.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data)
            .appendInherited(new KeyedCodec<>("DisplayName", Codec.STRING, false, true), (i, s) -> i.DisplayName = s,
                    i -> i.DisplayName,
                    (i, parent) -> i.DisplayName = parent.DisplayName)
            .add()
            .appendInherited(new KeyedCodec<>("Description", Codec.STRING, false, true), (i, s) -> i.Description = s,
                    i -> i.Description,
                    (i, parent) -> i.Description = parent.Description)
            .add()
            .appendInherited(new KeyedCodec<>("Parent", Codec.STRING, false, true), (i, s) -> i.Parent = s,
                    i -> i.Parent,
                    (i, parent) -> i.Parent = parent.Parent)
            .add()
            .appendInherited(new KeyedCodec<>("MaxLevel", Codec.INTEGER, false, true), (i, s) -> i.MaxLevel = s,
                    i -> i.MaxLevel,
                    (i, parent) -> i.MaxLevel = parent.MaxLevel)
            .add()
            .appendInherited(new KeyedCodec<>("StartingLevel", Codec.INTEGER, false, true),
                    (i, s) -> i.StartingLevel = s, i -> i.StartingLevel,
                    (i, parent) -> i.StartingLevel = parent.StartingLevel)
            .add()
            .appendInherited(new KeyedCodec<>("ExpCurveRef", ExpCurveDefinition.CHILD_ASSET_CODEC, false, true),
                    (i, s) -> i.ExpCurveRef = s, i -> i.ExpCurveRef,
                    (i, parent) -> i.ExpCurveRef = parent.ExpCurveRef)
            .addValidator(ExpCurveDefinition.VALIDATOR_CACHE.getValidator())
            .add()
            .appendInherited(
                    new KeyedCodec<>("Prerequisites", new MapCodec<>(Codec.INTEGER, HashMap::new), false, true),
                    (i, s) -> i.Prerequisites = s, i -> i.Prerequisites,
                    (i, parent) -> i.Prerequisites = parent.Prerequisites)
            .add()
            .appendInherited(new KeyedCodec<>("DefaultRewards", LevelRewardConfig.CODEC, false, true),
                    (i, s) -> i.DefaultRewards = s, i -> i.DefaultRewards,
                    (i, parent) -> i.DefaultRewards = parent.DefaultRewards)
            .add()
            .appendInherited(new KeyedCodec<>("LevelRewards", INT_REWARD_MAP_CODEC, false, true),
                    (i, s) -> i.LevelRewards = s, i -> i.LevelRewards,
                    (i, parent) -> i.LevelRewards = parent.LevelRewards)
            .add()
            .appendInherited(new KeyedCodec<>("StatGrowth", StatGrowthConfig.CODEC, false, true),
                    (i, s) -> i.StatGrowth = s, i -> i.StatGrowth,
                    (i, parent) -> i.StatGrowth = parent.StatGrowth)
            .add()
            .appendInherited(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true), (i, s) -> i.Enabled = s,
                    i -> i.Enabled,
                    (i, parent) -> i.Enabled = parent.Enabled)
            .add()
            .appendInherited(new KeyedCodec<>("Visible", Codec.BOOLEAN, false, true), (i, s) -> i.Visible = s,
                    i -> i.Visible,
                    (i, parent) -> i.Visible = parent.Visible)
            .add()
                .appendInherited(new KeyedCodec<>("Icon", Codec.STRING),
                    (i, s) -> i.IconId = (s == null || s.isEmpty()) ? null : s,
                    i -> i.IconId,
                    (i, parent) -> i.IconId = parent.IconId)
            .addValidator(CommonAssetValidator.ICON_ITEM)
            .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))
            .add()
            .build();



    private static com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // System identification
    private String id; // "Character Level"
    private String DisplayName; // "Character Level"
    private String Description;

    // Inheritance
    private String Parent; // Parent level config to inherit from (e.g., "base_class_level")

    // Level limits
    private int MaxLevel; // Maximum level (0 = no limit)
    private int StartingLevel; // Starting level (default: 1)

    // Experience curve
    private String ExpCurveRef; // Reference to curve file (optional)
    private String ExpCurveType; // Cached exp curve type (optional)
    private ExpCurveDefinition ExpCurve; // Resolved curve definition (optional)
    private Map<Integer, Float> ExpTable; // Optional exp table overrides

    // Prerequisites
    private Map<String, Integer> Prerequisites; // Required levels in other systems

    // Rewards per level
    private LevelRewardConfig DefaultRewards; // Applied every level
    private Map<Integer, LevelRewardConfig> LevelRewards; // Specific level rewards

    // Stat growth
    private StatGrowthConfig StatGrowth;

    // Settings
    private boolean Enabled; // Can be toggled on/off
    private boolean Visible; // Show in UI
    private String IconId; // Icon asset reference

    public LevelSystemConfig() {
        this.Enabled = true;
        this.Visible = true;
        this.StartingLevel = 1;
        this.MaxLevel = 0;
        this.Prerequisites = new HashMap<>();
        this.LevelRewards = new HashMap<>();
        this.IconId = null;
    }

    public static com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig>) AssetRegistry
                    .getAssetStore(LevelSystemConfig.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return DisplayName;
    }

    public void setDisplayName(String displayName) {
        this.DisplayName = displayName;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        this.Description = description;
    }

    public String getParent() {
        return Parent;
    }

    public void setParent(String parent) {
        this.Parent = parent;
    }

    public int getMaxLevel() {
        return MaxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.MaxLevel = maxLevel;
    }

    public int getStartingLevel() {
        return StartingLevel;
    }

    public void setStartingLevel(int startingLevel) {
        this.StartingLevel = startingLevel;
    }

    /**
     * Backwards-compatible accessor for system id.
     */
    public String getSystemId() {
        return getId();
    }

    /**
     * Backwards-compatible accessor for parent/inheritance.
     */
    public String getInheritsFrom() {
        return getParent();
    }


    public String getExpCurveRef() {
        return ExpCurveRef;
    }

    public void setExpCurveRef(String expCurveRef) {
        this.ExpCurveRef = expCurveRef;
    }

    public String getExpCurveType() {
        return ExpCurveType;
    }

    public void setExpCurveType(String expCurveType) {
        this.ExpCurveType = expCurveType;
    }

    public ExpCurveDefinition getExpCurve() {
        return ExpCurve;
    }

    public void setExpCurve(ExpCurveDefinition expCurve) {
        this.ExpCurve = expCurve;
    }

    public Map<Integer, Float> getExpTable() {
        return ExpTable;
    }

    public Map<String, Integer> getPrerequisites() {
        return Prerequisites;
    }

    public void setPrerequisites(Map<String, Integer> prerequisites) {
        this.Prerequisites = prerequisites;
    }

    public LevelRewardConfig getDefaultRewards() {
        return DefaultRewards;
    }

    public void setDefaultRewards(LevelRewardConfig defaultRewards) {
        this.DefaultRewards = defaultRewards;
    }

    public Map<Integer, LevelRewardConfig> getLevelRewards() {
        return LevelRewards;
    }

    public void setLevelRewards(Map<Integer, LevelRewardConfig> levelRewards) {
        this.LevelRewards = levelRewards;
    }

    /**
     * Convenience accessor for rewards at a given level.
     */
    public LevelRewardConfig getRewardsForLevel(int level) {
        if (LevelRewards != null && LevelRewards.containsKey(level)) {
            return LevelRewards.get(level);
        }
        return DefaultRewards;
    }

    public StatGrowthConfig getStatGrowth() {
        return StatGrowth;
    }

    public void setStatGrowth(StatGrowthConfig statGrowth) {
        this.StatGrowth = statGrowth;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public void setEnabled(boolean enabled) {
        this.Enabled = enabled;
    }

    public boolean isVisible() {
        return Visible;
    }

    public void setVisible(boolean visible) {
        this.Visible = visible;
    }

    public String getIconId() {
        return IconId;
    }

    public void setIconId(String iconId) {
        this.IconId = iconId;
    }

    /**
     * Get the unique ID for this level system (for JsonAsset interface)
     */
    @Nonnull
    @Override
    public String getId() {
        return this.id == null ? "" : this.id;
    }

    private static Map<Integer, Float> toIntKeyFloatMap(Map<String, Float> map) {
        Map<Integer, Float> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<String, Float> entry : map.entrySet()) {
            Integer key = parseIntKey(entry.getKey());
            if (key != null) {
                converted.put(key, entry.getValue());
            }
        }

        return converted;
    }

    private static Map<String, Float> toStringKeyFloatMap(Map<Integer, Float> map) {
        Map<String, Float> converted = new HashMap<>();
        if (map == null) {
            return converted;
        }

        for (Map.Entry<Integer, Float> entry : map.entrySet()) {
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return converted;
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





        // TO DO, this logic needs to move into the system file not the config file


    /**
     * Calculate experience required for a specific level
     */
    public float calculateExpForLevel(int level) {
        if (level < 1)
            return 0;

        // Check if at max level
        if (MaxLevel > 0 && level > MaxLevel) {
            return 0; // No exp required beyond max
        }


        // Fallback to simple linear
        return 100 * level;
    }

    /**
     * Level reward configuration
     */
    public static class LevelRewardConfig {
        public static final BuilderCodec<LevelRewardConfig> CODEC = LEVEL_REWARD_CODEC;

        private int StatPoints; // Allocatable stat points
        private int SkillPoints; // Skill/talent points
        private Map<String, Float> StatIncreases; // Auto stat increases (Health, Mana, etc.)
        private List<String> UnlockedAbilities; // Abilities unlocked at this level
        private List<String> UnlockedQuests; // Quests unlocked
        private Map<String, Long> CurrencyRewards; // Currency rewards
        private List<String> ItemRewards; // Item IDs to grant
        private String InteractionChain; // Optional interaction to execute

        // Level up effects
        private LevelUpEffects LevelUpEffects; // Sound, particles, notification on level up

        public LevelRewardConfig() {
            this.StatPoints = 0;
            this.SkillPoints = 0;
            this.StatIncreases = new HashMap<>();
            this.UnlockedAbilities = new ArrayList<>();
            this.UnlockedQuests = new ArrayList<>();
            this.CurrencyRewards = new HashMap<>();
            this.ItemRewards = new ArrayList<>();
            this.LevelUpEffects = new LevelUpEffects();
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
            return LevelUpEffects;
        }

        public void setLevelUpEffects(LevelUpEffects levelUpEffects) {
            this.LevelUpEffects = levelUpEffects;
        }
    }

    /**
     * Level up effects (sound, particles, notification)
     */
    public static class LevelUpEffects {
        public static final BuilderCodec<LevelUpEffects> CODEC = LEVEL_UP_EFFECTS_CODEC;

        private String SoundId; // Sound asset ID (e.g., "SFX_Level_Up")
        private String ParticleEffect; // Particle effect ID
        private NotificationConfig Notification; // Notification banner config
        private EventTitleConfig EventTitle; // Event title popup config

        public LevelUpEffects() {
            // Empty by default
        }

        public boolean isEmpty() {
            return SoundId == null && ParticleEffect == null && Notification == null && EventTitle == null;
        }

        // Getters and setters
        public String getSoundId() {
            return SoundId;
        }

        public void setSoundId(String soundId) {
            this.SoundId = soundId;
        }

        public String getParticleEffect() {
            return ParticleEffect;
        }

        public void setParticleEffect(String particleEffect) {
            this.ParticleEffect = particleEffect;
        }

        public NotificationConfig getNotification() {
            return Notification;
        }

        public void setNotification(NotificationConfig notification) {
            this.Notification = notification;
        }

        public EventTitleConfig getEventTitle() {
            return EventTitle;
        }

        public void setEventTitle(EventTitleConfig eventTitle) {
            this.EventTitle = eventTitle;
        }
    }

    /**
     * Event title configuration for level up (big screen title style)
     */
    public static class EventTitleConfig {
        public static final BuilderCodec<EventTitleConfig> CODEC = EVENT_TITLE_CODEC;

        private String PrimaryMessage; // Main message
        private String SecondaryMessage; // Secondary message
        private boolean Major; // Major style

        public EventTitleConfig() {
        }

        public EventTitleConfig(String primaryMessage, String secondaryMessage, boolean major) {
            this.PrimaryMessage = primaryMessage;
            this.SecondaryMessage = secondaryMessage;
            this.Major = major;
        }

        public String getPrimaryMessage() {
            return PrimaryMessage;
        }

        public void setPrimaryMessage(String primaryMessage) {
            this.PrimaryMessage = primaryMessage;
        }

        public String getSecondaryMessage() {
            return SecondaryMessage;
        }

        public void setSecondaryMessage(String secondaryMessage) {
            this.SecondaryMessage = secondaryMessage;
        }

        public boolean isMajor() {
            return Major;
        }

        public void setMajor(boolean major) {
            this.Major = major;
        }
    }

    /**
     * Notification configuration for level up banner
     */
    public static class NotificationConfig {
        public static final BuilderCodec<NotificationConfig> CODEC = NOTIFICATION_CODEC;

        private String PrimaryMessage; // Main message (supports color codes)
        private String SecondaryMessage; // Secondary message
        private String IconItemId; // Item icon ID to display

        public NotificationConfig() {
        }

        public NotificationConfig(String primaryMessage, String secondaryMessage, String iconItemId) {
            this.PrimaryMessage = primaryMessage;
            this.SecondaryMessage = secondaryMessage;
            this.IconItemId = iconItemId;
        }

        // Getters and setters
        public String getPrimaryMessage() {
            return PrimaryMessage;
        }

        public void setPrimaryMessage(String primaryMessage) {
            this.PrimaryMessage = primaryMessage;
        }

        public String getSecondaryMessage() {
            return SecondaryMessage;
        }

        public void setSecondaryMessage(String secondaryMessage) {
            this.SecondaryMessage = secondaryMessage;
        }

        public String getIconItemId() {
            return IconItemId;
        }

        public void setIconItemId(String iconItemId) {
            this.IconItemId = iconItemId;
        }
    }

    /**
     * Stat growth configuration (percentage or flat increases per level)
     */
    public static class StatGrowthConfig {
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
    }
}
