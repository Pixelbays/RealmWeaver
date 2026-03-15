package org.pixelbays.rpg.race.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;
import org.pixelbays.rpg.global.config.builder.ClassRefCodec;
import org.pixelbays.rpg.global.config.builder.RaceRefCodec;
import org.pixelbays.rpg.global.config.builder.StatModifiers;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

/**
 * Configuration for a race definition.
 * Loaded from /Server/Races (recursive)
 */
@SuppressWarnings({ "deprecation" })
public class RaceDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, RaceDefinition>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    private static final Codec<String> ABILITY_REF_CODEC = new AbilityRefCodec();
    private static final Codec<String> RACE_REF_CODEC = new RaceRefCodec();
    private static final Codec<String> CLASS_REF_CODEC = new ClassRefCodec();

    private static final FunctionCodec<String[], List<String>> ABILITY_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ABILITY_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    private static final FunctionCodec<String[], List<String>> RACE_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(RACE_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

        private static final FunctionCodec<String[], List<String>> CLASS_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(CLASS_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    private static final FunctionCodec<RaceAbilityUnlock[], List<RaceAbilityUnlock>> Ability_UNLOCK_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(RaceAbilityUnlock.CODEC, RaceAbilityUnlock[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new RaceAbilityUnlock[0] : list.toArray(RaceAbilityUnlock[]::new));

    public static final AssetBuilderCodec<String, RaceDefinition> CODEC = AssetBuilderCodec.builder(
            RaceDefinition.class,
            RaceDefinition::new,
            Codec.STRING,
            (t, k) -> t.RaceId = k,
            t -> t.RaceId,
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
            .appendInherited(new KeyedCodec<>("IconId", Codec.STRING, false, true),
                    (i, s) -> i.IconId = (s == null || s.isEmpty()) ? null : s,
                    i -> i.IconId,
                    (i, parent) -> i.IconId = parent.IconId)
            .addValidator(CommonAssetValidator.ICON_ITEM)
            .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))
            .add()
            .appendInherited(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true), (i, s) -> i.Enabled = s,
                    i -> i.Enabled,
                    (i, parent) -> i.Enabled = parent.Enabled)
            .add()
            .appendInherited(new KeyedCodec<>("Visible", Codec.BOOLEAN, false, true), (i, s) -> i.Visible = s,
                    i -> i.Visible,
                    (i, parent) -> i.Visible = parent.Visible)
            .add()
                .appendInherited(new KeyedCodec<>("RequiredExpansionIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.requiredExpansionIds = s, i -> i.requiredExpansionIds,
                    (i, parent) -> i.requiredExpansionIds = parent.requiredExpansionIds)
                .add()
                .appendInherited(new KeyedCodec<>("IsHeroRace", Codec.BOOLEAN, false, true), (i, s) -> i.IsHeroRace = s,
                    i -> i.IsHeroRace,
                    (i, parent) -> i.IsHeroRace = parent.IsHeroRace)
                .add()
                .appendInherited(new KeyedCodec<>("HeroStartingLevel", Codec.INTEGER, false, true),
                    (i, s) -> i.HeroStartingLevel = s, i -> i.HeroStartingLevel,
                    (i, parent) -> i.HeroStartingLevel = parent.HeroStartingLevel)
                .add()
            .appendInherited(new KeyedCodec<>("IsHybrid", Codec.BOOLEAN, false, true), (i, s) -> i.IsHybrid = s,
                    i -> i.IsHybrid,
                    (i, parent) -> i.IsHybrid = parent.IsHybrid)
            .add()
            .appendInherited(new KeyedCodec<>("ParentRaces", RACE_ID_LIST_CODEC, false, true),
                    (i, s) -> i.ParentRaces = s, i -> i.ParentRaces,
                    (i, parent) -> i.ParentRaces = parent.ParentRaces)
            .add()
            .appendInherited(new KeyedCodec<>("CompatibleHybridRaces", RACE_ID_LIST_CODEC, false, true),
                    (i, s) -> i.CompatibleHybridRaces = s, i -> i.CompatibleHybridRaces,
                    (i, parent) -> i.CompatibleHybridRaces = parent.CompatibleHybridRaces)
            .add()
            .appendInherited(new KeyedCodec<>("IncompatibleHybridRaces", RACE_ID_LIST_CODEC, false, true),
                    (i, s) -> i.IncompatibleHybridRaces = s, i -> i.IncompatibleHybridRaces,
                    (i, parent) -> i.IncompatibleHybridRaces = parent.IncompatibleHybridRaces)
            .add()
            .appendInherited(new KeyedCodec<>("MinSize", Codec.FLOAT, false, true), (i, s) -> i.MinSize = s,
                    i -> i.MinSize,
                    (i, parent) -> i.MinSize = parent.MinSize)
            .add()
            .appendInherited(new KeyedCodec<>("MaxSize", Codec.FLOAT, false, true), (i, s) -> i.MaxSize = s,
                    i -> i.MaxSize,
                    (i, parent) -> i.MaxSize = parent.MaxSize)
            .add()
            .appendInherited(new KeyedCodec<>("StartingStats", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.StartingStats = s, i -> i.StartingStats,
                    (i, parent) -> i.StartingStats = parent.StartingStats)
            .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("StatModifiers", StatModifiers.CODEC, false, true),
                    (i, s) -> i.StatModifiersValue = s, i -> i.StatModifiersValue,
                    (i, parent) -> i.StatModifiersValue = parent.StatModifiersValue)
            .add()
            .appendInherited(new KeyedCodec<>("AbilityUnlocks", Ability_UNLOCK_LIST_CODEC, false, true),
                    (i, s) -> i.AbilityUnlocks = s, i -> i.AbilityUnlocks,
                    (i, parent) -> i.AbilityUnlocks = parent.AbilityUnlocks)
            .add()
            .appendInherited(new KeyedCodec<>("AbilityIds", ABILITY_ID_LIST_CODEC, false, true),
                    (i, s) -> i.AbilityIds = s, i -> i.AbilityIds,
                    (i, parent) -> i.AbilityIds = parent.AbilityIds)
            .add()
            .appendInherited(new KeyedCodec<>("AllowedCosmeticCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.AllowedCosmeticCategories = s, i -> i.AllowedCosmeticCategories,
                    (i, parent) -> i.AllowedCosmeticCategories = parent.AllowedCosmeticCategories)
            .add()
            .appendInherited(new KeyedCodec<>("AllowedCosmeticIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.AllowedCosmeticIds = s, i -> i.AllowedCosmeticIds,
                    (i, parent) -> i.AllowedCosmeticIds = parent.AllowedCosmeticIds)
            .add()
            .appendInherited(new KeyedCodec<>("NotAllowedCosmeticCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.NotAllowedCosmeticCategories = s, i -> i.NotAllowedCosmeticCategories,
                    (i, parent) -> i.NotAllowedCosmeticCategories = parent.NotAllowedCosmeticCategories)
            .add()
            .appendInherited(new KeyedCodec<>("NotAllowedCosmeticIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.NotAllowedCosmeticIds = s, i -> i.NotAllowedCosmeticIds,
                    (i, parent) -> i.NotAllowedCosmeticIds = parent.NotAllowedCosmeticIds)
            .add()
            .appendInherited(new KeyedCodec<>("InheritanceRules", InheritanceRules.CODEC, false, true),
                    (i, s) -> i.InheritanceRulesValue = s, i -> i.InheritanceRulesValue,
                    (i, parent) -> i.InheritanceRulesValue = parent.InheritanceRulesValue)
            .add()
            .build();




    private static DefaultAssetMap<String, RaceDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // Basic info
    private String RaceId;
    private String DisplayName;
    private String Description;
    private String IconId;
    private boolean Enabled;
    private boolean Visible;
    private List<String> requiredExpansionIds;
    private boolean IsHeroRace;
    private int HeroStartingLevel;

    // Hybrid
    private boolean IsHybrid;
    private List<String> ParentRaces;
    private List<String> CompatibleHybridRaces;
    private List<String> IncompatibleHybridRaces;
    private InheritanceRules InheritanceRulesValue;

    // Size
    private float MinSize;
    private float MaxSize;

    // Stats
    private Map<String, Float> StartingStats;
    private StatModifiers StatModifiersValue;

    // Abilities
    private List<RaceAbilityUnlock> AbilityUnlocks;
    private List<String> AbilityIds;

    // Cosmetics
    private List<String> AllowedCosmeticCategories;
    private List<String> AllowedCosmeticIds;
    private List<String> NotAllowedCosmeticCategories;
    private List<String> NotAllowedCosmeticIds;


    public RaceDefinition() {
        this.RaceId = "";
        this.DisplayName = "";
        this.Description = "";
        this.IconId = null;
        this.Enabled = true;
        this.Visible = true;
        this.requiredExpansionIds = new ArrayList<>();
        this.IsHeroRace = false;
        this.HeroStartingLevel = 1;
        this.IsHybrid = false;
        this.ParentRaces = new ArrayList<>();
        this.CompatibleHybridRaces = new ArrayList<>();
        this.IncompatibleHybridRaces = new ArrayList<>();
        this.InheritanceRulesValue = new InheritanceRules();
        this.MinSize = 1.0f;
        this.MaxSize = 1.0f;
        this.StartingStats = new HashMap<>();
        this.StatModifiersValue = new StatModifiers();
        this.AbilityUnlocks = new ArrayList<>();
        this.AbilityIds = new ArrayList<>();
        this.AllowedCosmeticCategories = new ArrayList<>();
        this.AllowedCosmeticIds = new ArrayList<>();
        this.NotAllowedCosmeticCategories = new ArrayList<>();
        this.NotAllowedCosmeticIds = new ArrayList<>();
    }

    public static DefaultAssetMap<String, RaceDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, RaceDefinition>) AssetRegistry.getAssetStore(RaceDefinition.class)
                    .getAssetMap();
        }

        return ASSET_MAP;
    }

    @Nonnull
    @Override
    public String getId() {
        return this.RaceId == null ? "" : this.RaceId;
    }

    // Getters and setters

    public String getRaceId() {
        return RaceId;
    }

    public void setRaceId(String raceId) {
        this.RaceId = raceId;
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

    public String getIconId() {
        return IconId;
    }

    public void setIconId(String iconId) {
        this.IconId = (iconId == null || iconId.isEmpty()) ? null : iconId;
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

    public List<String> getRequiredExpansionIds() {
        return requiredExpansionIds == null ? new ArrayList<>() : requiredExpansionIds;
    }

    public boolean isHeroRace() {
        return IsHeroRace;
    }

    public int getHeroStartingLevel() {
        return Math.max(1, HeroStartingLevel);
    }

    public int getInitialCharacterLevel() {
        return isHeroRace() ? getHeroStartingLevel() : 1;
    }

    public boolean isHybrid() {
        return IsHybrid;
    }

    public void setHybrid(boolean hybrid) {
        IsHybrid = hybrid;
    }

    public List<String> getParentRaces() {
        return ParentRaces;
    }

    public void setParentRaces(List<String> parentRaces) {
        this.ParentRaces = parentRaces;
    }

    public List<String> getCompatibleHybridRaces() {
        return CompatibleHybridRaces;
    }

    public void setCompatibleHybridRaces(List<String> compatibleHybridRaces) {
        this.CompatibleHybridRaces = compatibleHybridRaces;
    }

    public List<String> getIncompatibleHybridRaces() {
        return IncompatibleHybridRaces;
    }

    public void setIncompatibleHybridRaces(List<String> incompatibleHybridRaces) {
        this.IncompatibleHybridRaces = incompatibleHybridRaces;
    }

    public InheritanceRules getInheritanceRules() {
        return InheritanceRulesValue;
    }

    public void setInheritanceRules(InheritanceRules inheritanceRules) {
        this.InheritanceRulesValue = inheritanceRules;
    }

    public float getMinSize() {
        return MinSize;
    }

    public void setMinSize(float minSize) {
        this.MinSize = minSize;
    }

    public float getMaxSize() {
        return MaxSize;
    }

    public void setMaxSize(float maxSize) {
        this.MaxSize = maxSize;
    }

    public Map<String, Float> getStartingStats() {
        return StartingStats;
    }

    public void setStartingStats(Map<String, Float> startingStats) {
        this.StartingStats = startingStats;
    }

    public StatModifiers getStatModifiers() {
        return StatModifiersValue;
    }

    public void setStatModifiers(StatModifiers statModifiers) {
        this.StatModifiersValue = statModifiers;
    }

    public List<RaceAbilityUnlock> getAbilityUnlocks() {
        return AbilityUnlocks;
    }

    public void setAbilityUnlocks(List<RaceAbilityUnlock> abilityUnlocks) {
        this.AbilityUnlocks = abilityUnlocks;
    }

    public List<String> getAbilityIds() {
        return AbilityIds;
    }

    public void setAbilityIds(List<String> abilityIds) {
        this.AbilityIds = abilityIds;
    }

    public List<String> getAllowedCosmeticCategories() {
        return AllowedCosmeticCategories;
    }

    public void setAllowedCosmeticCategories(List<String> allowedCosmeticCategories) {
        this.AllowedCosmeticCategories = allowedCosmeticCategories;
    }

    public List<String> getAllowedCosmeticIds() {
        return AllowedCosmeticIds;
    }

    public void setAllowedCosmeticIds(List<String> allowedCosmeticIds) {
        this.AllowedCosmeticIds = allowedCosmeticIds;
    }

    public List<String> getNotAllowedCosmeticCategories() {
        return NotAllowedCosmeticCategories;
    }

    public void setNotAllowedCosmeticCategories(List<String> notAllowedCosmeticCategories) {
        this.NotAllowedCosmeticCategories = notAllowedCosmeticCategories;
    }

    public List<String> getNotAllowedCosmeticIds() {
        return NotAllowedCosmeticIds;
    }

    public void setNotAllowedCosmeticIds(List<String> notAllowedCosmeticIds) {
        this.NotAllowedCosmeticIds = notAllowedCosmeticIds;
    }

    /**
     * Ability unlock configuration for races.
     */
    public static class RaceAbilityUnlock {
        public static final BuilderCodec<RaceAbilityUnlock> CODEC = BuilderCodec
                .builder(RaceAbilityUnlock.class, RaceAbilityUnlock::new)
                .append(new KeyedCodec<>("AbilityId", ABILITY_REF_CODEC, false, true),
                        (i, s) -> i.AbilityId = s, i -> i.AbilityId)
                .add()
                .append(new KeyedCodec<>("AbilityType", new EnumCodec<>(AbilityType.class), false, true),
                        (i, s) -> i.AbilityTypeValue = s, i -> i.AbilityTypeValue)
                .add()
                .append(new KeyedCodec<>("UnlockType", new EnumCodec<>(UnlockType.class), false, true),
                        (i, s) -> i.UnlockTypeValue = s, i -> i.UnlockTypeValue)
                .add()
                .append(new KeyedCodec<>("RequiredLevel", Codec.INTEGER, false, true),
                        (i, s) -> i.RequiredLevel = s, i -> i.RequiredLevel)
                .add()
                .build();

        private String AbilityId;
        private AbilityType AbilityTypeValue;
        private UnlockType UnlockTypeValue;
        private int RequiredLevel;

        public RaceAbilityUnlock() {
            this.AbilityId = "";
            this.AbilityTypeValue = AbilityType.Race;
            this.UnlockTypeValue = UnlockType.Starting;
            this.RequiredLevel = 1;
        }

        public String getAbilityId() {
            return AbilityId;
        }

        public void setAbilityId(String abilityId) {
            this.AbilityId = abilityId;
        }

        public AbilityType getAbilityType() {
            return AbilityTypeValue;
        }

        public void setAbilityType(AbilityType abilityType) {
            this.AbilityTypeValue = abilityType;
        }

        public UnlockType getUnlockType() {
            return UnlockTypeValue;
        }

        public void setUnlockType(UnlockType unlockType) {
            this.UnlockTypeValue = unlockType;
        }

        public int getRequiredLevel() {
            return RequiredLevel;
        }

        public void setRequiredLevel(int requiredLevel) {
            this.RequiredLevel = requiredLevel;
        }
    }


    public static class InheritanceRules {
        public static final BuilderCodec<InheritanceRules> CODEC = BuilderCodec
                .builder(InheritanceRules.class, InheritanceRules::new)
                .append(new KeyedCodec<>("StatBlendMode", new EnumCodec<>(StatBlendMode.class), false, true),
                        (i, s) -> i.StatBlendModeValue = s, i -> i.StatBlendModeValue)
                .add()
                .append(new KeyedCodec<>("AbilityMerge", new EnumCodec<>(AbilityMergeMode.class), false, true),
                        (i, s) -> i.AbilityMergeValue = s, i -> i.AbilityMergeValue)
                .add()
                .append(new KeyedCodec<>("CosmeticMerge", new EnumCodec<>(CosmeticMergeMode.class), false, true),
                        (i, s) -> i.CosmeticMergeValue = s, i -> i.CosmeticMergeValue)
                .add()
                .append(new KeyedCodec<>("WeightA", Codec.FLOAT, false, true), (i, s) -> i.WeightA = s, i -> i.WeightA)
                .add()
                .append(new KeyedCodec<>("WeightB", Codec.FLOAT, false, true), (i, s) -> i.WeightB = s, i -> i.WeightB)
                .add()
                .build();

        private StatBlendMode StatBlendModeValue;
        private AbilityMergeMode AbilityMergeValue;
        private CosmeticMergeMode CosmeticMergeValue;
        private float WeightA;
        private float WeightB;

        public InheritanceRules() {
            this.StatBlendModeValue = StatBlendMode.Average;
            this.AbilityMergeValue = AbilityMergeMode.Union;
            this.CosmeticMergeValue = CosmeticMergeMode.WhiteList_Union;
            this.WeightA = 0.5f;
            this.WeightB = 0.5f;
        }

        public StatBlendMode getStatBlendMode() {
            return StatBlendModeValue;
        }

        public void setStatBlendMode(StatBlendMode statBlendMode) {
            this.StatBlendModeValue = statBlendMode;
        }

        public AbilityMergeMode getAbilityMerge() {
            return AbilityMergeValue;
        }

        public void setAbilityMerge(AbilityMergeMode abilityMerge) {
            this.AbilityMergeValue = abilityMerge;
        }

        public CosmeticMergeMode getCosmeticMerge() {
            return CosmeticMergeValue;
        }

        public void setCosmeticMerge(CosmeticMergeMode cosmeticMerge) {
            this.CosmeticMergeValue = cosmeticMerge;
        }

        public float getWeightA() {
            return WeightA;
        }

        public void setWeightA(float weightA) {
            this.WeightA = weightA;
        }

        public float getWeightB() {
            return WeightB;
        }

        public void setWeightB(float weightB) {
            this.WeightB = weightB;
        }
    }

    public enum AbilityType {
        Race,
        Class
    }

    public enum UnlockType {
        Starting,
        Level,
        Quest,
        Item,
        Other
    }

    public enum StatBlendMode {
        Average,
        Weighted,
        Override
    }

    public enum AbilityMergeMode {
        Union,
        Override
    }

    public enum CosmeticMergeMode {
        WhiteList_Union,
        WhiteList_Intersect,
        Override
    }
}
