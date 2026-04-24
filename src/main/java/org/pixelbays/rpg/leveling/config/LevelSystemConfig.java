package org.pixelbays.rpg.leveling.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

/**
 * Configuration for a level progression system loaded from asset pack.
 * Example file: Server/Entity/Stats/Level_Character.json
 */
@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "StringOperationCanBeSimplified" })
public class LevelSystemConfig implements
        JsonAssetWithMap<String, DefaultAssetMap<String, LevelSystemConfig>> {

    private static final FunctionCodec<Map<String, Float>, Map<Integer, Float>> INT_FLOAT_MAP_CODEC =
        new FunctionCodec<>(
            new MapCodec<>(Codec.FLOAT, HashMap::new),
            LevelSystemConfig::toIntKeyFloatMap,
            LevelSystemConfig::toStringKeyFloatMap);

    public static final AssetBuilderCodec<String, LevelSystemConfig> CODEC = AssetBuilderCodec
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
                .appendInherited(new KeyedCodec<>("ExpCurveType", Codec.STRING, false, true),
                    (i, s) -> i.ExpCurveType = s, i -> i.ExpCurveType,
                    (i, parent) -> i.ExpCurveType = parent.ExpCurveType)
                .add()
                .appendInherited(new KeyedCodec<>("ExpCurve", ExpCurveDefinition.CHILD_ASSET_CODEC, false, true),
                    (i, s) -> i.ExpCurveRef = s, i -> i.ExpCurveRef,
                    (i, parent) -> i.ExpCurveRef = parent.ExpCurveRef)
                .addValidator(ExpCurveDefinition.VALIDATOR_CACHE.getValidator())
                .add()
                .appendInherited(new KeyedCodec<>("ExpTable", INT_FLOAT_MAP_CODEC, false, true),
                    (i, s) -> i.ExpTable = s, i -> i.ExpTable,
                    (i, parent) -> i.ExpTable = parent.ExpTable)
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
                .appendInherited(new KeyedCodec<>("LevelRewards", LevelRewardConfig.INT_REWARD_MAP_CODEC, false, true),
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

    private static DefaultAssetMap<String, LevelSystemConfig> ASSET_MAP;
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

    private static AssetStore<String, LevelSystemConfig, DefaultAssetMap<String, LevelSystemConfig>> ASSET_STORE;

    public static AssetStore<String, LevelSystemConfig, DefaultAssetMap<String, LevelSystemConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry
                    .getAssetStore(LevelSystemConfig.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, LevelSystemConfig> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, LevelSystemConfig>) getAssetStore().getAssetMap();
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
        this.ExpCurve = null;
    }

    public String getExpCurveType() {
        if (ExpCurveType != null && !ExpCurveType.isBlank()) {
            return ExpCurveType;
        }

        ExpCurveDefinition resolvedCurve = getExpCurve();
        return resolvedCurve == null ? null : resolvedCurve.getType();
    }

    public void setExpCurveType(String expCurveType) {
        this.ExpCurveType = expCurveType;
    }

    public ExpCurveDefinition getExpCurve() {
        if (ExpCurve != null) {
            if (ExpCurveRef == null || ExpCurveRef.isBlank() || ExpCurve.getId().equals(ExpCurveRef)) {
                return ExpCurve;
            }
        }

        if (ExpCurveRef == null || ExpCurveRef.isBlank()) {
            return ExpCurve;
        }

        DefaultAssetMap<String, ExpCurveDefinition> assetMap = ExpCurveDefinition.getAssetMap();
        if (assetMap == null) {
            return ExpCurve;
        }

        ExpCurveDefinition resolvedCurve = assetMap.getAsset(ExpCurveRef);
        if (resolvedCurve != null) {
            ExpCurve = resolvedCurve;
            if (ExpCurveType == null || ExpCurveType.isBlank()) {
                ExpCurveType = resolvedCurve.getType();
            }
        }

        return ExpCurve;
    }

    public void setExpCurve(ExpCurveDefinition expCurve) {
        this.ExpCurve = expCurve;
        this.ExpCurveRef = expCurve == null ? this.ExpCurveRef : expCurve.getId();
        if (expCurve != null && (this.ExpCurveType == null || this.ExpCurveType.isBlank())) {
            this.ExpCurveType = expCurve.getType();
        }
    }

    public Map<Integer, Float> getExpTable() {
        return ExpTable;
    }

    public void setExpTable(Map<Integer, Float> expTable) {
        this.ExpTable = expTable;
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

    // TO DO, this logic needs to move into the system file not the config file

    /**
     * Calculate experience required for a specific level
     */
    public float calculateExpForLevel(int level) {
        if (level < 1) {
            return 0;
        }

        if (ExpTable != null) {
            Float configuredValue = ExpTable.get(level);
            if (configuredValue != null) {
                return Math.max(0f, configuredValue);
            }
        }

        ExpCurveDefinition resolvedCurve = getExpCurve();
        if (resolvedCurve != null) {
            return Math.max(0f, resolvedCurve.calculate(level));
        }

        // Fallback to simple linear
        return 100 * level;
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
