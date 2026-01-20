package org.pixelbays.rpg.config;

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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a race definition.
 * Loaded from /Server/Races (recursive)
 */
public class RaceDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, RaceDefinition>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC =
            new FunctionCodec<>(Codec.STRING_ARRAY,
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? new String[0] : list.toArray(new String[0]));

    private static final FunctionCodec<RaceAbilityUnlock[], List<RaceAbilityUnlock>> Ability_UNLOCK_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(RaceAbilityUnlock.CODEC, RaceAbilityUnlock[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? new RaceAbilityUnlock[0] : list.toArray(new RaceAbilityUnlock[0]));

    public static final AssetBuilderCodec<String, RaceDefinition> CODEC = AssetBuilderCodec.builder(
                    RaceDefinition.class,
                    RaceDefinition::new,
                    Codec.STRING,
                    (t, k) -> t.raceId = k,
                    t -> t.raceId,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(new KeyedCodec<>("raceId", Codec.STRING, false, true), (i, s) -> i.raceId = s, i -> i.raceId)
            .add()
            .append(new KeyedCodec<>("displayName", Codec.STRING, false, true), (i, s) -> i.displayName = s, i -> i.displayName)
            .add()
            .append(new KeyedCodec<>("description", Codec.STRING, false, true), (i, s) -> i.description = s, i -> i.description)
            .add()
            .append(new KeyedCodec<>("iconId", Codec.STRING, false, true), (i, s) -> i.iconId = s, i -> i.iconId)
            .add()
            .append(new KeyedCodec<>("enabled", Codec.BOOLEAN, false, true), (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("visible", Codec.BOOLEAN, false, true), (i, s) -> i.visible = s, i -> i.visible)
            .add()
            .append(new KeyedCodec<>("isHybrid", Codec.BOOLEAN, false, true), (i, s) -> i.isHybrid = s, i -> i.isHybrid)
            .add()
            .append(new KeyedCodec<>("parentRaces", STRING_LIST_CODEC, false, true), (i, s) -> i.parentRaces = s, i -> i.parentRaces)
            .add()
            .append(new KeyedCodec<>("compatibleHybridRaces", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.compatibleHybridRaces = s, i -> i.compatibleHybridRaces)
            .add()
            .append(new KeyedCodec<>("incompatibleHybridRaces", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.incompatibleHybridRaces = s, i -> i.incompatibleHybridRaces)
            .add()
            .append(new KeyedCodec<>("minSize", Codec.FLOAT, false, true), (i, s) -> i.minSize = s, i -> i.minSize)
            .add()
            .append(new KeyedCodec<>("maxSize", Codec.FLOAT, false, true), (i, s) -> i.maxSize = s, i -> i.maxSize)
            .add()
            .append(new KeyedCodec<>("startingStats", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.startingStats = s, i -> i.startingStats)
            .add()
            .append(new KeyedCodec<>("statModifiers", StatModifiers.CODEC, false, true),
                    (i, s) -> i.statModifiers = s, i -> i.statModifiers)
            .add()
            .append(new KeyedCodec<>("abilityUnlocks", Ability_UNLOCK_LIST_CODEC, false, true),
                    (i, s) -> i.abilityUnlocks = s, i -> i.abilityUnlocks)
            .add()
                .append(new KeyedCodec<>("abilityIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.abilityIds = s, i -> i.abilityIds)
                .add()
            .append(new KeyedCodec<>("allowedCosmeticCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.allowedCosmeticCategories = s, i -> i.allowedCosmeticCategories)
            .add()
            .append(new KeyedCodec<>("allowedCosmeticIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.allowedCosmeticIds = s, i -> i.allowedCosmeticIds)
            .add()
            .append(new KeyedCodec<>("notAllowedCosmeticCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.notAllowedCosmeticCategories = s, i -> i.notAllowedCosmeticCategories)
            .add()
            .append(new KeyedCodec<>("notAllowedCosmeticIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.notAllowedCosmeticIds = s, i -> i.notAllowedCosmeticIds)
            .add()
            .append(new KeyedCodec<>("inheritanceRules", InheritanceRules.CODEC, false, true),
                    (i, s) -> i.inheritanceRules = s, i -> i.inheritanceRules)
            .add()
            .build();

    private static DefaultAssetMap<String, RaceDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // Basic info
    private String raceId;
    private String displayName;
    private String description;
    private String iconId;
    private boolean enabled;
    private boolean visible;

    // Hybrid
    private boolean isHybrid;
    private List<String> parentRaces;
    private List<String> compatibleHybridRaces;
    private List<String> incompatibleHybridRaces;
    private InheritanceRules inheritanceRules;

    // Size
    private float minSize;
    private float maxSize;

    // Stats
    private Map<String, Float> startingStats;
    private StatModifiers statModifiers;

    // Abilities
    private List<RaceAbilityUnlock> abilityUnlocks;
    private List<String> abilityIds;

    // Cosmetics
    private List<String> allowedCosmeticCategories;
    private List<String> allowedCosmeticIds;
    private List<String> notAllowedCosmeticCategories;
    private List<String> notAllowedCosmeticIds;

    public RaceDefinition() {
        this.raceId = "";
        this.displayName = "";
        this.description = "";
        this.iconId = "";
        this.enabled = true;
        this.visible = true;
        this.isHybrid = false;
        this.parentRaces = new ArrayList<>();
        this.compatibleHybridRaces = new ArrayList<>();
        this.incompatibleHybridRaces = new ArrayList<>();
        this.inheritanceRules = new InheritanceRules();
        this.minSize = 1.0f;
        this.maxSize = 1.0f;
        this.startingStats = new HashMap<>();
        this.statModifiers = new StatModifiers();
        this.abilityUnlocks = new ArrayList<>();
        this.abilityIds = new ArrayList<>();
        this.allowedCosmeticCategories = new ArrayList<>();
        this.allowedCosmeticIds = new ArrayList<>();
        this.notAllowedCosmeticCategories = new ArrayList<>();
        this.notAllowedCosmeticIds = new ArrayList<>();
    }

    public static DefaultAssetMap<String, RaceDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, RaceDefinition>) AssetRegistry.getAssetStore(RaceDefinition.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    @Nonnull
    public String getId() {
        return this.raceId;
    }

    public void mergeFrom(RaceDefinition parent) {
        if (parent == null) return;

        if (this.displayName == null || this.displayName.isEmpty()) this.displayName = parent.displayName;
        if (this.description == null || this.description.isEmpty()) this.description = parent.description;
        if (this.iconId == null || this.iconId.isEmpty()) this.iconId = parent.iconId;
        if (this.minSize == 0f && parent.minSize != 0f) this.minSize = parent.minSize;
        if (this.maxSize == 0f && parent.maxSize != 0f) this.maxSize = parent.maxSize;

        if (parent.startingStats != null && !parent.startingStats.isEmpty()) {
            Map<String, Float> merged = new HashMap<>(parent.startingStats);
            merged.putAll(this.startingStats);
            this.startingStats = merged;
        }

        this.statModifiers.mergeFrom(parent.statModifiers);

        if (parent.abilityUnlocks != null && !parent.abilityUnlocks.isEmpty()) {
            List<RaceAbilityUnlock> merged = new ArrayList<>(parent.abilityUnlocks);
            merged.addAll(this.abilityUnlocks);
            this.abilityUnlocks = merged;
        }

        if (parent.abilityIds != null && !parent.abilityIds.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.abilityIds);
            merged.addAll(this.abilityIds);
            this.abilityIds = merged;
        }

        if (parent.allowedCosmeticCategories != null && !parent.allowedCosmeticCategories.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.allowedCosmeticCategories);
            merged.addAll(this.allowedCosmeticCategories);
            this.allowedCosmeticCategories = merged;
        }

        if (parent.allowedCosmeticIds != null && !parent.allowedCosmeticIds.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.allowedCosmeticIds);
            merged.addAll(this.allowedCosmeticIds);
            this.allowedCosmeticIds = merged;
        }

        if (parent.notAllowedCosmeticCategories != null && !parent.notAllowedCosmeticCategories.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.notAllowedCosmeticCategories);
            merged.addAll(this.notAllowedCosmeticCategories);
            this.notAllowedCosmeticCategories = merged;
        }

        if (parent.notAllowedCosmeticIds != null && !parent.notAllowedCosmeticIds.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.notAllowedCosmeticIds);
            merged.addAll(this.notAllowedCosmeticIds);
            this.notAllowedCosmeticIds = merged;
        }
    }

    // Getters and setters

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
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

    public String getIconId() {
        return iconId;
    }

    public void setIconId(String iconId) {
        this.iconId = iconId;
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

    public boolean isHybrid() {
        return isHybrid;
    }

    public void setHybrid(boolean hybrid) {
        isHybrid = hybrid;
    }

    public List<String> getParentRaces() {
        return parentRaces;
    }

    public void setParentRaces(List<String> parentRaces) {
        this.parentRaces = parentRaces;
    }

    public List<String> getCompatibleHybridRaces() {
        return compatibleHybridRaces;
    }

    public void setCompatibleHybridRaces(List<String> compatibleHybridRaces) {
        this.compatibleHybridRaces = compatibleHybridRaces;
    }

    public List<String> getIncompatibleHybridRaces() {
        return incompatibleHybridRaces;
    }

    public void setIncompatibleHybridRaces(List<String> incompatibleHybridRaces) {
        this.incompatibleHybridRaces = incompatibleHybridRaces;
    }

    public InheritanceRules getInheritanceRules() {
        return inheritanceRules;
    }

    public void setInheritanceRules(InheritanceRules inheritanceRules) {
        this.inheritanceRules = inheritanceRules;
    }

    public float getMinSize() {
        return minSize;
    }

    public void setMinSize(float minSize) {
        this.minSize = minSize;
    }

    public float getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(float maxSize) {
        this.maxSize = maxSize;
    }

    public Map<String, Float> getStartingStats() {
        return startingStats;
    }

    public void setStartingStats(Map<String, Float> startingStats) {
        this.startingStats = startingStats;
    }

    public StatModifiers getStatModifiers() {
        return statModifiers;
    }

    public void setStatModifiers(StatModifiers statModifiers) {
        this.statModifiers = statModifiers;
    }

    public List<RaceAbilityUnlock> getAbilityUnlocks() {
        return abilityUnlocks;
    }

    public void setAbilityUnlocks(List<RaceAbilityUnlock> abilityUnlocks) {
        this.abilityUnlocks = abilityUnlocks;
    }

    public List<String> getAbilityIds() {
        return abilityIds;
    }

    public void setAbilityIds(List<String> abilityIds) {
        this.abilityIds = abilityIds;
    }

    public List<String> getAllowedCosmeticCategories() {
        return allowedCosmeticCategories;
    }

    public void setAllowedCosmeticCategories(List<String> allowedCosmeticCategories) {
        this.allowedCosmeticCategories = allowedCosmeticCategories;
    }

    public List<String> getAllowedCosmeticIds() {
        return allowedCosmeticIds;
    }

    public void setAllowedCosmeticIds(List<String> allowedCosmeticIds) {
        this.allowedCosmeticIds = allowedCosmeticIds;
    }

    public List<String> getNotAllowedCosmeticCategories() {
        return notAllowedCosmeticCategories;
    }

    public void setNotAllowedCosmeticCategories(List<String> notAllowedCosmeticCategories) {
        this.notAllowedCosmeticCategories = notAllowedCosmeticCategories;
    }

    public List<String> getNotAllowedCosmeticIds() {
        return notAllowedCosmeticIds;
    }

    public void setNotAllowedCosmeticIds(List<String> notAllowedCosmeticIds) {
        this.notAllowedCosmeticIds = notAllowedCosmeticIds;
    }

    // Nested types

    public static class StatModifiers {
        public static final BuilderCodec<StatModifiers> CODEC = BuilderCodec.builder(StatModifiers.class, StatModifiers::new)
                .append(new KeyedCodec<>("additiveModifiers", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                        (i, s) -> i.additiveModifiers = s, i -> i.additiveModifiers)
                .add()
                .append(new KeyedCodec<>("multiplicativeModifiers", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                        (i, s) -> i.multiplicativeModifiers = s, i -> i.multiplicativeModifiers)
                .add()
                .build();

        private Map<String, Float> additiveModifiers;
        private Map<String, Float> multiplicativeModifiers;

        public StatModifiers() {
            this.additiveModifiers = new HashMap<>();
            this.multiplicativeModifiers = new HashMap<>();
        }

        public Map<String, Float> getAdditiveModifiers() {
            return additiveModifiers;
        }

        public void setAdditiveModifiers(Map<String, Float> additiveModifiers) {
            this.additiveModifiers = additiveModifiers;
        }

        public Map<String, Float> getMultiplicativeModifiers() {
            return multiplicativeModifiers;
        }

        public void setMultiplicativeModifiers(Map<String, Float> multiplicativeModifiers) {
            this.multiplicativeModifiers = multiplicativeModifiers;
        }

        public void mergeFrom(StatModifiers parent) {
            if (parent == null) return;

            for (Map.Entry<String, Float> entry : parent.additiveModifiers.entrySet()) {
                String stat = entry.getKey();
                float parentValue = entry.getValue();
                float childValue = this.additiveModifiers.getOrDefault(stat, 0f);
                this.additiveModifiers.put(stat, childValue + parentValue);
            }

            for (Map.Entry<String, Float> entry : parent.multiplicativeModifiers.entrySet()) {
                String stat = entry.getKey();
                float parentValue = entry.getValue();
                float childValue = this.multiplicativeModifiers.getOrDefault(stat, 0f);
                this.multiplicativeModifiers.put(stat, childValue + parentValue);
            }
        }
    }

    public static class RaceAbilityUnlock {
        public static final BuilderCodec<RaceAbilityUnlock> CODEC = BuilderCodec.builder(RaceAbilityUnlock.class, RaceAbilityUnlock::new)
                .append(new KeyedCodec<>("abilityId", Codec.STRING, false, true), (i, s) -> i.abilityId = s, i -> i.abilityId)
                .add()
                .append(new KeyedCodec<>("abilityType", new EnumCodec<>(AbilityType.class), false, true), (i, s) -> i.abilityType = s, i -> i.abilityType)
                .add()
                .append(new KeyedCodec<>("unlockType", new EnumCodec<>(UnlockType.class), false, true), (i, s) -> i.unlockType = s, i -> i.unlockType)
                .add()
                .append(new KeyedCodec<>("requiredLevel", Codec.INTEGER, false, true), (i, s) -> i.requiredLevel = s, i -> i.requiredLevel)
                .add()
                .build();

        private String abilityId;
        private AbilityType abilityType;
        private UnlockType unlockType;
        private int requiredLevel;

        public RaceAbilityUnlock() {
            this.abilityId = "";
            this.abilityType = AbilityType.Race;
            this.unlockType = UnlockType.Starting;
            this.requiredLevel = 1;
        }

        public String getAbilityId() {
            return abilityId;
        }

        public void setAbilityId(String abilityId) {
            this.abilityId = abilityId;
        }

        public AbilityType getAbilityType() {
            return abilityType;
        }

        public void setAbilityType(AbilityType abilityType) {
            this.abilityType = abilityType;
        }

        public UnlockType getUnlockType() {
            return unlockType;
        }

        public void setUnlockType(UnlockType unlockType) {
            this.unlockType = unlockType;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public void setRequiredLevel(int requiredLevel) {
            this.requiredLevel = requiredLevel;
        }
    }

    public static class InheritanceRules {
        public static final BuilderCodec<InheritanceRules> CODEC = BuilderCodec.builder(InheritanceRules.class, InheritanceRules::new)
                .append(new KeyedCodec<>("statBlendMode", new EnumCodec<>(StatBlendMode.class), false, true),
                        (i, s) -> i.statBlendMode = s, i -> i.statBlendMode)
                .add()
                .append(new KeyedCodec<>("abilityMerge", new EnumCodec<>(AbilityMergeMode.class), false, true),
                        (i, s) -> i.abilityMerge = s, i -> i.abilityMerge)
                .add()
                .append(new KeyedCodec<>("cosmeticMerge", new EnumCodec<>(CosmeticMergeMode.class), false, true),
                        (i, s) -> i.cosmeticMerge = s, i -> i.cosmeticMerge)
                .add()
                .append(new KeyedCodec<>("weightA", Codec.FLOAT, false, true), (i, s) -> i.weightA = s, i -> i.weightA)
                .add()
                .append(new KeyedCodec<>("weightB", Codec.FLOAT, false, true), (i, s) -> i.weightB = s, i -> i.weightB)
                .add()
                .build();

        private StatBlendMode statBlendMode;
        private AbilityMergeMode abilityMerge;
        private CosmeticMergeMode cosmeticMerge;
        private float weightA;
        private float weightB;

        public InheritanceRules() {
            this.statBlendMode = StatBlendMode.Average;
            this.abilityMerge = AbilityMergeMode.Union;
            this.cosmeticMerge = CosmeticMergeMode.WhiteList_Union;
            this.weightA = 0.5f;
            this.weightB = 0.5f;
        }

        public StatBlendMode getStatBlendMode() {
            return statBlendMode;
        }

        public void setStatBlendMode(StatBlendMode statBlendMode) {
            this.statBlendMode = statBlendMode;
        }

        public AbilityMergeMode getAbilityMerge() {
            return abilityMerge;
        }

        public void setAbilityMerge(AbilityMergeMode abilityMerge) {
            this.abilityMerge = abilityMerge;
        }

        public CosmeticMergeMode getCosmeticMerge() {
            return cosmeticMerge;
        }

        public void setCosmeticMerge(CosmeticMergeMode cosmeticMerge) {
            this.cosmeticMerge = cosmeticMerge;
        }

        public float getWeightA() {
            return weightA;
        }

        public void setWeightA(float weightA) {
            this.weightA = weightA;
        }

        public float getWeightB() {
            return weightB;
        }

        public void setWeightB(float weightB) {
            this.weightB = weightB;
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
