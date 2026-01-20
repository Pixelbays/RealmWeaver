package org.pixelbays.rpg.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.ValidatableCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import java.io.IOException;

/**
 * Configuration for a level progression system loaded from asset pack.
 * Example file: Server/Entity/Stats/Level_Character.json
 */
@SuppressWarnings({"deprecation", "ToArrayCallWithZeroLengthArrayArgument", "StringOperationCanBeSimplified"})
public class LevelSystemConfig implements com.hypixel.hytale.assetstore.map.JsonAssetWithMap<String, com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC =
        new FunctionCodec<>(Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(new String[list.size()]));

    private static final Codec<String> ABILITY_REF_CODEC = new AbilityRefCodec();

    private static final FunctionCodec<String[], List<String>> ABILITY_ID_LIST_CODEC =
        new FunctionCodec<>(new ArrayCodec<>(ABILITY_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(new String[list.size()]));

    private static final FunctionCodec<Map<String, Float>, Map<Integer, Float>> INT_FLOAT_MAP_CODEC =
        new FunctionCodec<>(new MapCodec<>(Codec.FLOAT, HashMap::new),
            LevelSystemConfig::toIntKeyFloatMap,
            LevelSystemConfig::toStringKeyFloatMap);

    private static final FunctionCodec<Map<String, Map<String, Float>>, Map<Integer, Map<String, Float>>> INT_FLOAT_MAP_MAP_CODEC =
        new FunctionCodec<>(new MapCodec<>(new MapCodec<>(Codec.FLOAT, HashMap::new), HashMap::new),
            LevelSystemConfig::toIntKeyNestedFloatMap,
            LevelSystemConfig::toStringKeyNestedFloatMap);

    public static final BuilderCodec<NotificationConfig> NOTIFICATION_CODEC = BuilderCodec.builder(NotificationConfig.class, NotificationConfig::new)
        .append(new KeyedCodec<>("primaryMessage", Codec.STRING, false, true), (i, s) -> i.primaryMessage = s, i -> i.primaryMessage)
        .add()
        .append(new KeyedCodec<>("secondaryMessage", Codec.STRING, false, true), (i, s) -> i.secondaryMessage = s, i -> i.secondaryMessage)
        .add()
        .append(new KeyedCodec<>("iconItemId", Codec.STRING, false, true), (i, s) -> i.iconItemId = s, i -> i.iconItemId)
        .add()
        .build();

    public static final BuilderCodec<EventTitleConfig> EVENT_TITLE_CODEC = BuilderCodec.builder(EventTitleConfig.class, EventTitleConfig::new)
        .append(new KeyedCodec<>("primaryMessage", Codec.STRING, false, true), (i, s) -> i.primaryMessage = s, i -> i.primaryMessage)
        .add()
        .append(new KeyedCodec<>("secondaryMessage", Codec.STRING, false, true), (i, s) -> i.secondaryMessage = s, i -> i.secondaryMessage)
        .add()
        .append(new KeyedCodec<>("major", Codec.BOOLEAN, false, true), (i, s) -> i.major = s, i -> i.major)
        .add()
        .build();

    public static final BuilderCodec<LevelUpEffects> LEVEL_UP_EFFECTS_CODEC = BuilderCodec.builder(LevelUpEffects.class, LevelUpEffects::new)
        .append(new KeyedCodec<>("soundId", Codec.STRING, false, true), (i, s) -> i.soundId = s, i -> i.soundId)
        .add()
        .append(new KeyedCodec<>("particleEffect", Codec.STRING, false, true), (i, s) -> i.particleEffect = s, i -> i.particleEffect)
        .add()
        .append(new KeyedCodec<>("notification", NotificationConfig.CODEC, false, true), (i, s) -> i.notification = s, i -> i.notification)
        .add()
        .append(new KeyedCodec<>("eventTitle", EventTitleConfig.CODEC, false, true), (i, s) -> i.eventTitle = s, i -> i.eventTitle)
        .add()
        .build();

    public static final BuilderCodec<ExpCurveConfig> EXP_CURVE_CODEC = BuilderCodec.builder(ExpCurveConfig.class, ExpCurveConfig::new)
        .append(new KeyedCodec<>("type", Codec.STRING, false, true), (i, s) -> i.type = s, i -> i.type)
        .add()
        .append(new KeyedCodec<>("baseExp", Codec.FLOAT, false, true), (i, s) -> i.baseExp = s, i -> i.baseExp)
        .add()
        .append(new KeyedCodec<>("growthRate", Codec.FLOAT, false, true), (i, s) -> i.growthRate = s, i -> i.growthRate)
        .add()
        .append(new KeyedCodec<>("exponent", Codec.FLOAT, false, true), (i, s) -> i.exponent = s, i -> i.exponent)
        .add()
        .append(new KeyedCodec<>("customFormula", Codec.STRING, false, true), (i, s) -> i.customFormula = s, i -> i.customFormula)
        .add()
        .build();

    public static final BuilderCodec<LevelRewardConfig> LEVEL_REWARD_CODEC = BuilderCodec.builder(LevelRewardConfig.class, LevelRewardConfig::new)
        .append(new KeyedCodec<>("statPoints", Codec.INTEGER, false, true), (i, s) -> i.statPoints = s, i -> i.statPoints)
        .add()
        .append(new KeyedCodec<>("skillPoints", Codec.INTEGER, false, true), (i, s) -> i.skillPoints = s, i -> i.skillPoints)
        .add()
        .append(new KeyedCodec<>("statIncreases", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
            (i, s) -> i.statIncreases = s, i -> i.statIncreases)
        .add()
        .append(new KeyedCodec<>("unlockedAbilities", ABILITY_ID_LIST_CODEC, false, true),
            (i, s) -> i.unlockedAbilities = s, i -> i.unlockedAbilities)
        .add()
        .append(new KeyedCodec<>("unlockedQuests", STRING_LIST_CODEC, false, true),
            (i, s) -> i.unlockedQuests = s, i -> i.unlockedQuests)
        .add()
        .append(new KeyedCodec<>("currencyRewards", new MapCodec<>(Codec.LONG, HashMap::new), false, true),
            (i, s) -> i.currencyRewards = s, i -> i.currencyRewards)
        .add()
        .append(new KeyedCodec<>("itemRewards", STRING_LIST_CODEC, false, true),
            (i, s) -> i.itemRewards = s, i -> i.itemRewards)
        .add()
        .append(new KeyedCodec<>("interactionChain", RootInteraction.CHILD_ASSET_CODEC, false, true),
            (i, s) -> i.interactionChain = s, i -> i.interactionChain)
        .add()
        .append(new KeyedCodec<>("levelUpEffects", LevelUpEffects.CODEC, false, true), (i, s) -> i.levelUpEffects = s, i -> i.levelUpEffects)
        .add()
        .build();

    private static final FunctionCodec<Map<String, LevelRewardConfig>, Map<Integer, LevelRewardConfig>> INT_REWARD_MAP_CODEC =
        new FunctionCodec<>(new MapCodec<>(LevelRewardConfig.CODEC, HashMap::new),
            LevelSystemConfig::toIntKeyRewardMap,
            LevelSystemConfig::toStringKeyRewardMap);

    public static final BuilderCodec<StatGrowthConfig> STAT_GROWTH_CODEC = BuilderCodec.builder(StatGrowthConfig.class, StatGrowthConfig::new)
        .append(new KeyedCodec<>("flatGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
            (i, s) -> i.flatGrowth = s, i -> i.flatGrowth)
        .add()
        .append(new KeyedCodec<>("percentageGrowth", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
            (i, s) -> i.percentageGrowth = s, i -> i.percentageGrowth)
        .add()
        .append(new KeyedCodec<>("milestoneGrowth", INT_FLOAT_MAP_MAP_CODEC, false, true),
            (i, s) -> i.milestoneGrowth = s, i -> i.milestoneGrowth)
        .add()
        .build();

    public static final com.hypixel.hytale.assetstore.codec.AssetBuilderCodec<String, LevelSystemConfig> CODEC =
        com.hypixel.hytale.assetstore.codec.AssetBuilderCodec.builder(
            LevelSystemConfig.class,
            LevelSystemConfig::new,
            Codec.STRING,
            (t, k) -> t.systemId = k,
            t -> t.systemId,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(new KeyedCodec<>("systemId", Codec.STRING, false, true), (i, s) -> i.systemId = s, i -> i.systemId)
        .add()
        .append(new KeyedCodec<>("displayName", Codec.STRING, false, true), (i, s) -> i.displayName = s, i -> i.displayName)
        .add()
        .append(new KeyedCodec<>("description", Codec.STRING, false, true), (i, s) -> i.description = s, i -> i.description)
        .add()
        .append(new KeyedCodec<>("inheritsFrom", Codec.STRING, false, true), (i, s) -> i.inheritsFrom = s, i -> i.inheritsFrom)
        .add()
        .append(new KeyedCodec<>("maxLevel", Codec.INTEGER, false, true), (i, s) -> i.maxLevel = s, i -> i.maxLevel)
        .add()
        .append(new KeyedCodec<>("startingLevel", Codec.INTEGER, false, true), (i, s) -> i.startingLevel = s, i -> i.startingLevel)
        .add()
        .append(new KeyedCodec<>("expCurveType", Codec.STRING, false, true), (i, s) -> i.expCurveType = s, i -> i.expCurveType)
        .add()
        .append(new KeyedCodec<>("expCurveRef", ExpCurveDefinition.CHILD_ASSET_CODEC, false, true),
            (i, s) -> i.expCurveRef = s, i -> i.expCurveRef)
        .addValidator(ExpCurveDefinition.VALIDATOR_CACHE.getValidator())
        .add()
        .append(new KeyedCodec<>("expCurve", ExpCurveConfig.CODEC, false, true), (i, s) -> i.expCurve = s, i -> i.expCurve)
        .add()
        .append(new KeyedCodec<>("expTable", INT_FLOAT_MAP_CODEC, false, true), (i, s) -> i.expTable = s, i -> i.expTable)
        .add()
        .append(new KeyedCodec<>("prerequisites", new MapCodec<>(Codec.INTEGER, HashMap::new), false, true),
            (i, s) -> i.prerequisites = s, i -> i.prerequisites)
        .add()
        .append(new KeyedCodec<>("defaultRewards", LevelRewardConfig.CODEC, false, true), (i, s) -> i.defaultRewards = s, i -> i.defaultRewards)
        .add()
        .append(new KeyedCodec<>("levelRewards", INT_REWARD_MAP_CODEC, false, true), (i, s) -> i.levelRewards = s, i -> i.levelRewards)
        .add()
        .append(new KeyedCodec<>("statGrowth", StatGrowthConfig.CODEC, false, true), (i, s) -> i.statGrowth = s, i -> i.statGrowth)
        .add()
        .append(new KeyedCodec<>("enabled", Codec.BOOLEAN, false, true), (i, s) -> i.enabled = s, i -> i.enabled)
        .add()
        .append(new KeyedCodec<>("visible", Codec.BOOLEAN, false, true), (i, s) -> i.visible = s, i -> i.visible)
        .add()
        .append(new KeyedCodec<>("iconId", Codec.STRING, false, true), (i, s) -> i.iconId = s, i -> i.iconId)
        .add()
        .build();

    private static final class AbilityRefCodec implements ValidatableCodec<String> {
        @Nonnull
        @SuppressWarnings("null")
        private AssetCodec<String, ClassAbilityDefinition> getAssetCodec() {
            return AssetRegistry.getAssetStore(ClassAbilityDefinition.class).getCodec();
        }

        @Override
        public String decode(org.bson.BsonValue bsonValue, ExtraInfo extraInfo) {
            return getAssetCodec().getKeyCodec().getChildCodec().decode(bsonValue, extraInfo);
        }

        @Override
        public org.bson.BsonValue encode(String value, ExtraInfo extraInfo) {
            return getAssetCodec().getKeyCodec().getChildCodec().encode(value, extraInfo);
        }

        @Override
        @Nullable
        public String decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
            return getAssetCodec().getKeyCodec().getChildCodec().decodeJson(reader, extraInfo);
        }

        @Override
        @Nonnull
        public Schema toSchema(@Nonnull SchemaContext context) {
            StringSchema schema = new StringSchema();
            schema.setTitle("Ability");
            schema.setHytaleAssetRef(ClassAbilityDefinition.class.getSimpleName());
            return schema;
        }

        @Override
        public void validate(String value, ExtraInfo extraInfo) {
            ValidationResults results = extraInfo.getValidationResults();
            if (results != null) {
                ClassAbilityDefinition.VALIDATOR_CACHE.getValidator().accept(value, results);
            }
        }

        @Override
        public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
            if (tested.add(this)) {
                // No default validation needed
            }
        }
    }

    private static com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig> ASSET_MAP;
    private AssetExtraInfo.Data data;
    
    // System identification
    private String systemId;                    // "character_level", "class_warrior", etc.
    private String displayName;                 // "Character Level"
    private String description;
    
    // Inheritance
    private String inheritsFrom;                // Parent level config to inherit from (e.g., "base_class_level")
    
    // Level limits
    private int maxLevel;                       // Maximum level (0 = no limit)
    private int startingLevel;                  // Starting level (default: 1)
    
    // Experience curve
    private String expCurveType;                // "linear", "exponential", "custom", "table"
    private String expCurveRef;                 // Reference to curve file (optional)
    private ExpCurveConfig expCurve;            // Inline curve definition
    private Map<Integer, Float> expTable;       // Level -> Exp required (for "table" type)
    
    // Prerequisites
    private Map<String, Integer> prerequisites; // Required levels in other systems
    
    // Rewards per level
    private LevelRewardConfig defaultRewards;   // Applied every level
    private Map<Integer, LevelRewardConfig> levelRewards; // Specific level rewards
    
    // Stat growth
    private StatGrowthConfig statGrowth;
    
    // Settings
    private boolean enabled;                    // Can be toggled on/off
    private boolean visible;                    // Show in UI
    private String iconId;                      // Icon asset reference
    
    public LevelSystemConfig() {
        this.enabled = true;
        this.visible = true;
        this.startingLevel = 1;
        this.maxLevel = 0;
        this.prerequisites = new HashMap<>();
        this.levelRewards = new HashMap<>();
        this.expTable = new HashMap<>();
    }

    public static com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (com.hypixel.hytale.assetstore.map.DefaultAssetMap<String, LevelSystemConfig>)
                AssetRegistry.getAssetStore(LevelSystemConfig.class).getAssetMap();
        }

        return ASSET_MAP;
    }
    
    // Getters and setters
    public String getSystemId() {
        return systemId;
    }
    
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getInheritsFrom() {
        return inheritsFrom;
    }
    
    public void setInheritsFrom(String inheritsFrom) {
        this.inheritsFrom = inheritsFrom;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
    
    public int getStartingLevel() {
        return startingLevel;
    }
    
    public void setStartingLevel(int startingLevel) {
        this.startingLevel = startingLevel;
    }
    
    public String getExpCurveType() {
        return expCurveType;
    }
    
    public void setExpCurveType(String expCurveType) {
        this.expCurveType = expCurveType;
    }
    
    public String getExpCurveRef() {
        return expCurveRef;
    }
    
    public void setExpCurveRef(String expCurveRef) {
        this.expCurveRef = expCurveRef;
    }
    
    public ExpCurveConfig getExpCurve() {
        return expCurve;
    }
    
    public void setExpCurve(ExpCurveConfig expCurve) {
        this.expCurve = expCurve;
    }
    
    public Map<Integer, Float> getExpTable() {
        return expTable;
    }
    
    public void setExpTable(Map<Integer, Float> expTable) {
        this.expTable = expTable;
    }
    
    public Map<String, Integer> getPrerequisites() {
        return prerequisites;
    }
    
    public void setPrerequisites(Map<String, Integer> prerequisites) {
        this.prerequisites = prerequisites;
    }
    
    public LevelRewardConfig getDefaultRewards() {
        return defaultRewards;
    }
    
    public void setDefaultRewards(LevelRewardConfig defaultRewards) {
        this.defaultRewards = defaultRewards;
    }
    
    public Map<Integer, LevelRewardConfig> getLevelRewards() {
        return levelRewards;
    }
    
    public void setLevelRewards(Map<Integer, LevelRewardConfig> levelRewards) {
        this.levelRewards = levelRewards;
    }
    
    public StatGrowthConfig getStatGrowth() {
        return statGrowth;
    }
    
    public void setStatGrowth(StatGrowthConfig statGrowth) {
        this.statGrowth = statGrowth;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public String getIconId() {
        return iconId;
    }
    
    public void setIconId(String iconId) {
        this.iconId = iconId;
    }
    
    /**
     * Get the unique ID for this level system (for JsonAsset interface)
     */
    @Nonnull
    @Override
    public String getId() {
        return this.systemId;
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
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /**
     * Merge settings from parent config. Child values take precedence.
     * Used for template inheritance (e.g., inheriting from "base_class_level")
     */
    public void mergeFrom(LevelSystemConfig parent) {
        if (parent == null) return;
        
        // Don't inherit systemId or inheritsFrom
        if (this.displayName == null || this.displayName.isEmpty()) this.displayName = parent.displayName;
        if (this.description == null || this.description.isEmpty()) this.description = parent.description;
        
        // Level limits (0 means not set, so inherit)
        if (this.maxLevel == 0) this.maxLevel = parent.maxLevel;
        if (this.startingLevel == 1 && parent.startingLevel != 1) this.startingLevel = parent.startingLevel;
        
        // Experience curve (only inherit if not set)
        if (this.expCurveType == null || this.expCurveType.isEmpty()) {
            this.expCurveType = parent.expCurveType;
            this.expCurveRef = parent.expCurveRef;
            this.expCurve = parent.expCurve;
        }
        
        // Merge exp table
        if (parent.expTable != null && !parent.expTable.isEmpty()) {
            Map<Integer, Float> merged = new HashMap<>(parent.expTable);
            merged.putAll(this.expTable); // Child overrides
            this.expTable = merged;
        }
        
        // Merge prerequisites
        if (parent.prerequisites != null && !parent.prerequisites.isEmpty()) {
            Map<String, Integer> merged = new HashMap<>(parent.prerequisites);
            merged.putAll(this.prerequisites); // Child overrides
            this.prerequisites = merged;
        }
        
        // Inherit default rewards if not set
        if (this.defaultRewards == null) this.defaultRewards = parent.defaultRewards;
        
        // Merge level rewards
        if (parent.levelRewards != null && !parent.levelRewards.isEmpty()) {
            Map<Integer, LevelRewardConfig> merged = new HashMap<>(parent.levelRewards);
            merged.putAll(this.levelRewards); // Child overrides
            this.levelRewards = merged;
        }
        
        // Inherit stat growth if not set
        if (this.statGrowth == null) this.statGrowth = parent.statGrowth;
        
        // Settings
        if (this.iconId == null || this.iconId.isEmpty()) this.iconId = parent.iconId;
        // enabled and visible stay as-is (child controls these)
    }
    
    /**
     * Calculate experience required for a specific level
     */
    public float calculateExpForLevel(int level) {
        if (level < 1) return 0;
        
        // Check if at max level
        if (maxLevel > 0 && level > maxLevel) {
            return 0; // No exp required beyond max
        }
        
        // Use exp table if provided
        if (expCurveType != null && expCurveType.equals("table")) {
            return expTable.getOrDefault(level, 0f);
        }
        
        // Use curve if provided
        if (expCurve != null) {
            return expCurve.calculate(level);
        }
        
        // Fallback to simple linear
        return 100 * level;
    }
    
    /**
     * Get rewards for a specific level (combines default + specific)
     */
    public LevelRewardConfig getRewardsForLevel(int level) {
        LevelRewardConfig rewards = new LevelRewardConfig();
        
        // Apply default rewards
        if (defaultRewards != null) {
            rewards.merge(defaultRewards);
        }
        
        // Apply specific level rewards
        if (levelRewards.containsKey(level)) {
            rewards.merge(levelRewards.get(level));
        }
        
        return rewards;
    }
    
    /**
     * Experience curve configuration
     */
    public static class ExpCurveConfig {
        public static final BuilderCodec<ExpCurveConfig> CODEC = EXP_CURVE_CODEC;

        private String type;            // "linear", "exponential", "custom"
        private float baseExp;          // Base exp for level 1
        private float growthRate;       // Growth multiplier per level
        private float exponent;         // For exponential curves
        private String customFormula;   // Custom formula string (advanced)
        
        public ExpCurveConfig() {
            this.type = "linear";
            this.baseExp = 100;
            this.growthRate = 1.1f;
            this.exponent = 2.0f;
        }
        
        public float calculate(int level) {
            if ("linear".equals(type)) {
                return baseExp + (level * growthRate);
            }
            if ("exponential".equals(type)) {
                return baseExp * (float) Math.pow(level, exponent);
            }
            if ("custom".equals(type)) {
                // For future implementation: parse and evaluate customFormula
                return baseExp * level;
            }
            return 100 * level;
        }
        
        // Getters and setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public float getBaseExp() {
            return baseExp;
        }
        
        public void setBaseExp(float baseExp) {
            this.baseExp = baseExp;
        }
        
        public float getGrowthRate() {
            return growthRate;
        }
        
        public void setGrowthRate(float growthRate) {
            this.growthRate = growthRate;
        }
        
        public float getExponent() {
            return exponent;
        }
        
        public void setExponent(float exponent) {
            this.exponent = exponent;
        }
        
        public String getCustomFormula() {
            return customFormula;
        }
        
        public void setCustomFormula(String customFormula) {
            this.customFormula = customFormula;
        }
    }
    
    /**
     * Level reward configuration
     */
    public static class LevelRewardConfig {
        public static final BuilderCodec<LevelRewardConfig> CODEC = LEVEL_REWARD_CODEC;

        private int statPoints;                     // Allocatable stat points
        private int skillPoints;                    // Skill/talent points
        private Map<String, Float> statIncreases;   // Auto stat increases (Health, Mana, etc.)
        private List<String> unlockedAbilities;     // Abilities unlocked at this level
        private List<String> unlockedQuests;        // Quests unlocked
        private Map<String, Long> currencyRewards;  // Currency rewards
        private List<String> itemRewards;           // Item IDs to grant
        private String interactionChain;            // Optional interaction to execute
        
        // Level up effects
        private LevelUpEffects levelUpEffects;      // Sound, particles, notification on level up
        
        public LevelRewardConfig() {
            this.statPoints = 0;
            this.skillPoints = 0;
            this.statIncreases = new HashMap<>();
            this.unlockedAbilities = new ArrayList<>();
            this.unlockedQuests = new ArrayList<>();
            this.currencyRewards = new HashMap<>();
            this.itemRewards = new ArrayList<>();
            this.levelUpEffects = new LevelUpEffects();
        }
        
        /**
         * Merge another reward config into this one (additive)
         */
        public void merge(LevelRewardConfig other) {
            this.statPoints += other.statPoints;
            this.skillPoints += other.skillPoints;
            
            // Merge stat increases
            other.statIncreases.forEach((stat, value) -> 
                this.statIncreases.merge(stat, value, Float::sum)
            );
            
            // Merge lists
            this.unlockedAbilities.addAll(other.unlockedAbilities);
            this.unlockedQuests.addAll(other.unlockedQuests);
            this.itemRewards.addAll(other.itemRewards);
            
            // Merge currency
            other.currencyRewards.forEach((currency, amount) ->
                this.currencyRewards.merge(currency, amount, Long::sum)
            );
            
            // Interaction chain (last one wins)
            if (other.interactionChain != null) {
                this.interactionChain = other.interactionChain;
            }
            
            // Merge level up effects (other overrides if present)
            if (other.levelUpEffects != null && !other.levelUpEffects.isEmpty()) {
                this.levelUpEffects = other.levelUpEffects;
            }
        }
        
        // Getters and setters
        public int getStatPoints() {
            return statPoints;
        }
        
        public void setStatPoints(int statPoints) {
            this.statPoints = statPoints;
        }
        
        public int getSkillPoints() {
            return skillPoints;
        }
        
        public void setSkillPoints(int skillPoints) {
            this.skillPoints = skillPoints;
        }
        
        public Map<String, Float> getStatIncreases() {
            return statIncreases;
        }
        
        public void setStatIncreases(Map<String, Float> statIncreases) {
            this.statIncreases = statIncreases;
        }
        
        public List<String> getUnlockedAbilities() {
            return unlockedAbilities;
        }
        
        public void setUnlockedAbilities(List<String> unlockedAbilities) {
            this.unlockedAbilities = unlockedAbilities;
        }
        
        public List<String> getUnlockedQuests() {
            return unlockedQuests;
        }
        
        public void setUnlockedQuests(List<String> unlockedQuests) {
            this.unlockedQuests = unlockedQuests;
        }
        
        public Map<String, Long> getCurrencyRewards() {
            return currencyRewards;
        }
        
        public void setCurrencyRewards(Map<String, Long> currencyRewards) {
            this.currencyRewards = currencyRewards;
        }
        
        public List<String> getItemRewards() {
            return itemRewards;
        }
        
        public void setItemRewards(List<String> itemRewards) {
            this.itemRewards = itemRewards;
        }
        
        public String getInteractionChain() {
            return interactionChain;
        }
        
        public void setInteractionChain(String interactionChain) {
            this.interactionChain = interactionChain;
        }
        
        public LevelUpEffects getLevelUpEffects() {
            return levelUpEffects;
        }
        
        public void setLevelUpEffects(LevelUpEffects levelUpEffects) {
            this.levelUpEffects = levelUpEffects;
        }
    }
    
    /**
     * Level up effects (sound, particles, notification)
     */
    public static class LevelUpEffects {
        public static final BuilderCodec<LevelUpEffects> CODEC = LEVEL_UP_EFFECTS_CODEC;

        private String soundId;              // Sound asset ID (e.g., "SFX_Level_Up")
        private String particleEffect;       // Particle effect ID
        private NotificationConfig notification; // Notification banner config
        private EventTitleConfig eventTitle; // Event title popup config
        
        public LevelUpEffects() {
            // Empty by default
        }
        
        public boolean isEmpty() {
            return soundId == null && particleEffect == null && notification == null && eventTitle == null;
        }
        
        // Getters and setters
        public String getSoundId() {
            return soundId;
        }
        
        public void setSoundId(String soundId) {
            this.soundId = soundId;
        }
        
        public String getParticleEffect() {
            return particleEffect;
        }
        
        public void setParticleEffect(String particleEffect) {
            this.particleEffect = particleEffect;
        }
        
        public NotificationConfig getNotification() {
            return notification;
        }
        
        public void setNotification(NotificationConfig notification) {
            this.notification = notification;
        }

        public EventTitleConfig getEventTitle() {
            return eventTitle;
        }

        public void setEventTitle(EventTitleConfig eventTitle) {
            this.eventTitle = eventTitle;
        }
    }

    /**
     * Event title configuration for level up (big screen title style)
     */
    public static class EventTitleConfig {
        public static final BuilderCodec<EventTitleConfig> CODEC = EVENT_TITLE_CODEC;

        private String primaryMessage;      // Main message
        private String secondaryMessage;    // Secondary message
        private boolean major;              // Major style

        public EventTitleConfig() {
        }

        public EventTitleConfig(String primaryMessage, String secondaryMessage, boolean major) {
            this.primaryMessage = primaryMessage;
            this.secondaryMessage = secondaryMessage;
            this.major = major;
        }

        public String getPrimaryMessage() {
            return primaryMessage;
        }

        public void setPrimaryMessage(String primaryMessage) {
            this.primaryMessage = primaryMessage;
        }

        public String getSecondaryMessage() {
            return secondaryMessage;
        }

        public void setSecondaryMessage(String secondaryMessage) {
            this.secondaryMessage = secondaryMessage;
        }

        public boolean isMajor() {
            return major;
        }

        public void setMajor(boolean major) {
            this.major = major;
        }
    }
    
    /**
     * Notification configuration for level up banner
     */
    public static class NotificationConfig {
        public static final BuilderCodec<NotificationConfig> CODEC = NOTIFICATION_CODEC;

        private String primaryMessage;      // Main message (supports color codes)
        private String secondaryMessage;    // Secondary message
        private String iconItemId;          // Item icon ID to display
        
        public NotificationConfig() {
        }
        
        public NotificationConfig(String primaryMessage, String secondaryMessage, String iconItemId) {
            this.primaryMessage = primaryMessage;
            this.secondaryMessage = secondaryMessage;
            this.iconItemId = iconItemId;
        }
        
        // Getters and setters
        public String getPrimaryMessage() {
            return primaryMessage;
        }
        
        public void setPrimaryMessage(String primaryMessage) {
            this.primaryMessage = primaryMessage;
        }
        
        public String getSecondaryMessage() {
            return secondaryMessage;
        }
        
        public void setSecondaryMessage(String secondaryMessage) {
            this.secondaryMessage = secondaryMessage;
        }
        
        public String getIconItemId() {
            return iconItemId;
        }
        
        public void setIconItemId(String iconItemId) {
            this.iconItemId = iconItemId;
        }
    }
    
    /**
     * Stat growth configuration (percentage or flat increases per level)
     */
    public static class StatGrowthConfig {
        public static final BuilderCodec<StatGrowthConfig> CODEC = STAT_GROWTH_CODEC;

        private Map<String, Float> flatGrowth;          // Flat increases per level
        private Map<String, Float> percentageGrowth;    // Percentage increases per level
        private Map<Integer, Map<String, Float>> milestoneGrowth; // Level -> Stat -> Bonus
        
        public StatGrowthConfig() {
            this.flatGrowth = new HashMap<>();
            this.percentageGrowth = new HashMap<>();
            this.milestoneGrowth = new HashMap<>();
        }
        
        // Getters and setters
        public Map<String, Float> getFlatGrowth() {
            return flatGrowth;
        }
        
        public void setFlatGrowth(Map<String, Float> flatGrowth) {
            this.flatGrowth = flatGrowth;
        }
        
        public Map<String, Float> getPercentageGrowth() {
            return percentageGrowth;
        }
        
        public void setPercentageGrowth(Map<String, Float> percentageGrowth) {
            this.percentageGrowth = percentageGrowth;
        }
        
        public Map<Integer, Map<String, Float>> getMilestoneGrowth() {
            return milestoneGrowth;
        }
        
        public void setMilestoneGrowth(Map<Integer, Map<String, Float>> milestoneGrowth) {
            this.milestoneGrowth = milestoneGrowth;
        }
    }
}
