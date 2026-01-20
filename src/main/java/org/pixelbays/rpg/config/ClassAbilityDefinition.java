package org.pixelbays.rpg.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;

/**
 * Configuration for an RPG ability (class or race).
 * Loaded from /Server/RPGAbilities/** (recursive)
 * 
 * This defines the ability properties. Class or race assets reference
 * these by id.
 */
@SuppressWarnings({"deprecation"})
public class ClassAbilityDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, ClassAbilityDefinition>> {

        private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC =
            new FunctionCodec<>(Codec.STRING_ARRAY,
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? new String[0] : list.toArray(String[]::new));

        private static final FunctionCodec<RankModifier[], List<RankModifier>> RANK_MODIFIER_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(RankModifier.CODEC, RankModifier[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? new RankModifier[0] : list.toArray(RankModifier[]::new));

        private static final FunctionCodec<Integer[], List<Integer>> INT_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(Codec.INTEGER, Integer[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? new Integer[0] : list.toArray(Integer[]::new));

    public static final AssetBuilderCodec<String, ClassAbilityDefinition> CODEC = AssetBuilderCodec.builder(
        ClassAbilityDefinition.class,
        ClassAbilityDefinition::new,
        Codec.STRING,
        (t, k) -> t.abilityId = k,
        t -> t.abilityId,
        (asset, data) -> asset.data = data,
        asset -> asset.data
    )
        .append(new KeyedCodec<>("abilityId", Codec.STRING, false, true), (i, s) -> i.abilityId = s, i -> i.abilityId)
        .add()
        .append(new KeyedCodec<>("displayName", Codec.STRING, false, true), (i, s) -> i.displayName = s, i -> i.displayName)
        .add()
        .append(new KeyedCodec<>("description", Codec.STRING, false, true), (i, s) -> i.description = s, i -> i.description)
        .add()
        .append(new KeyedCodec<>("iconId", Codec.STRING, false, true), (i, s) -> i.iconId = s, i -> i.iconId)
        .add()
        .append(new KeyedCodec<>("parentAbilityId", Codec.STRING, false, true), (i, s) -> i.parentAbilityId = s, i -> i.parentAbilityId)
        .add()
        .append(new KeyedCodec<>("abilityType", new EnumCodec<>(AbilityType.class), false, true),
            (i, s) -> i.abilityType = s, i -> i.abilityType)
        .add()
        .append(new KeyedCodec<>("targetingType", new EnumCodec<>(TargetingType.class), false, true),
            (i, s) -> i.targetingType = s, i -> i.targetingType)
        .add()
        .append(new KeyedCodec<>("resourceCosts", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
            (i, s) -> i.resourceCosts = s, i -> i.resourceCosts)
        .add()
        .append(new KeyedCodec<>("cooldown", Codec.FLOAT, false, true), (i, s) -> i.cooldown = s, i -> i.cooldown)
        .add()
        .append(new KeyedCodec<>("globalCooldown", Codec.FLOAT, false, true), (i, s) -> i.globalCooldown = s, i -> i.globalCooldown)
        .add()
        .append(new KeyedCodec<>("cooldownCategories", STRING_LIST_CODEC, false, true),
            (i, s) -> i.cooldownCategories = s, i -> i.cooldownCategories)
        .add()
        .append(new KeyedCodec<>("castTime", Codec.FLOAT, false, true), (i, s) -> i.castTime = s, i -> i.castTime)
        .add()
        .append(new KeyedCodec<>("channelDuration", Codec.FLOAT, false, true), (i, s) -> i.channelDuration = s, i -> i.channelDuration)
        .add()
        .append(new KeyedCodec<>("interruptible", Codec.BOOLEAN, false, true), (i, s) -> i.interruptible = s, i -> i.interruptible)
        .add()
        .append(new KeyedCodec<>("range", Codec.FLOAT, false, true), (i, s) -> i.range = s, i -> i.range)
        .add()
        .append(new KeyedCodec<>("aoeRadius", Codec.FLOAT, false, true), (i, s) -> i.aoeRadius = s, i -> i.aoeRadius)
        .add()
        .append(new KeyedCodec<>("coneAngle", Codec.FLOAT, false, true), (i, s) -> i.coneAngle = s, i -> i.coneAngle)
        .add()
        .append(new KeyedCodec<>("prerequisiteAbilities", STRING_LIST_CODEC, false, true),
            (i, s) -> i.prerequisiteAbilities = s, i -> i.prerequisiteAbilities)
        .add()
        .append(new KeyedCodec<>("statRequirements", new MapCodec<>(Codec.INTEGER, HashMap::new), false, true),
            (i, s) -> i.statRequirements = s, i -> i.statRequirements)
        .add()
        .append(new KeyedCodec<>("hasRanks", Codec.BOOLEAN, false, true), (i, s) -> i.hasRanks = s, i -> i.hasRanks)
        .add()
        .append(new KeyedCodec<>("maxRank", Codec.INTEGER, false, true), (i, s) -> i.maxRank = s, i -> i.maxRank)
        .add()
        .append(new KeyedCodec<>("rankModifiers", RANK_MODIFIER_LIST_CODEC, false, true),
            (i, s) -> i.rankModifiers = s, i -> i.rankModifiers)
        .add()
        .append(new KeyedCodec<>("interactionChainId", Codec.STRING, false, true),
            (i, s) -> i.interactionChainId = s, i -> i.interactionChainId)
        .add()
        .append(new KeyedCodec<>("interactionParams", new MapCodec<>(Codec.STRING, HashMap::new), false, true),
            (i, s) -> i.interactionParams = s, i -> i.interactionParams)
        .add()
        .append(new KeyedCodec<>("castAnimation", Codec.STRING, false, true), (i, s) -> i.castAnimation = s, i -> i.castAnimation)
        .add()
        .append(new KeyedCodec<>("castSound", Codec.STRING, false, true), (i, s) -> i.castSound = s, i -> i.castSound)
        .add()
        .append(new KeyedCodec<>("impactSound", Codec.STRING, false, true), (i, s) -> i.impactSound = s, i -> i.impactSound)
        .add()
        .append(new KeyedCodec<>("particleEffect", Codec.STRING, false, true), (i, s) -> i.particleEffect = s, i -> i.particleEffect)
        .add()
        .append(new KeyedCodec<>("enabled", Codec.BOOLEAN, false, true), (i, s) -> i.enabled = s, i -> i.enabled)
        .add()
        .append(new KeyedCodec<>("tooltip", Codec.STRING, false, true), (i, s) -> i.tooltip = s, i -> i.tooltip)
        .add()
        .append(new KeyedCodec<>("inputBinding", new EnumCodec<>(AbilityInputBinding.class), false, true),
            (i, s) -> i.inputBinding = s, i -> i.inputBinding)
        .add()
        .append(new KeyedCodec<>("hotbarKeyOverrides", INT_LIST_CODEC, false, true),
            (i, s) -> i.hotbarKeyOverrides = s, i -> i.hotbarKeyOverrides)
        .add()
        .build();

    public static final ContainedAssetCodec<String, ClassAbilityDefinition, ?> CHILD_ASSET_CODEC =
        new ContainedAssetCodec<>(ClassAbilityDefinition.class, CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE =
        new ValidatorCache<>(new AssetKeyValidator<>(ClassAbilityDefinition::getAssetStore));

    private static DefaultAssetMap<String, ClassAbilityDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // === Basic Info ===
    private String abilityId; // Unique identifier (e.g., "warrior_charge")
    private String displayName; // Display name
    private String description; // Ability description
    private String iconId; // Icon asset ID

    // === Inheritance ===
    private String parentAbilityId; // Parent ability to extend from (e.g., "basic_strike")

    // === Ability Type ===
    private AbilityType abilityType; // Active, Passive, Toggle
    private TargetingType targetingType; // Self, Target, Ground, AoE, Cone, etc.

    // === Costs ===
    private Map<String, Float> resourceCosts; // Resource costs (e.g., "Mana": 50.0)

    // === Cooldown ===
    private float cooldown; // Cooldown in seconds
    private float globalCooldown; // GCD override (0 = use default)
    private List<String> cooldownCategories; // Categories this ability shares cooldowns with

    // === Casting ===
    private float castTime; // Cast time in seconds (0 = instant)
    private float channelDuration; // Channel duration (0 = not channeled)
    private boolean interruptible; // Can be interrupted?

    // === Range and Targeting ===
    private float range; // Maximum range (0 = melee)
    private float aoeRadius; // AoE radius (if applicable)
    private float coneAngle; // Cone angle in degrees (if applicable)

    // === Requirements ===
    private List<String> prerequisiteAbilities; // Required abilities before this can be learned
    private Map<String, Integer> statRequirements; // Required stats (e.g., "Strength": 50)

    // === Ranks ===
    private boolean hasRanks; // Does this ability have multiple ranks?
    private int maxRank; // Maximum rank (if hasRanks = true)
    private List<RankModifier> rankModifiers; // How each rank modifies the ability

    // === Interaction Chain ===
    private String interactionChainId; // Hytale interaction chain to execute
    private Map<String, String> interactionParams; // Parameters to pass to interaction

    // === Visual/Audio ===
    private String castAnimation; // Animation to play when casting
    private String castSound; // Sound to play when casting
    private String impactSound; // Sound to play on impact
    private String particleEffect; // Particle effect

    // === Misc ===
    private boolean enabled; // Is this ability enabled?
    private String tooltip; // Detailed tooltip text

    // === Input Binding ===
    private AbilityInputBinding inputBinding; // Ability1/2/3 binding
    private List<Integer> hotbarKeyOverrides; // Placeholder for 1-9 hotbar overrides

    // === Constructors ===
    public ClassAbilityDefinition() {
        this.abilityId = "";
        this.displayName = "";
        this.description = "";
        this.iconId = "";
        this.abilityType = AbilityType.Active;
        this.targetingType = TargetingType.Self;
        this.resourceCosts = new HashMap<>();
        this.cooldown = 0f;
        this.globalCooldown = 0f;
        this.cooldownCategories = new ArrayList<>();
        this.castTime = 0f;
        this.channelDuration = 0f;
        this.interruptible = true;
        this.range = 0f;
        this.aoeRadius = 0f;
        this.coneAngle = 0f;
        this.prerequisiteAbilities = new ArrayList<>();
        this.statRequirements = new HashMap<>();
        this.hasRanks = false;
        this.maxRank = 1;
        this.rankModifiers = new ArrayList<>();
        this.interactionChainId = "";
        this.interactionParams = new HashMap<>();
        this.castAnimation = "";
        this.castSound = "";
        this.impactSound = "";
        this.particleEffect = "";
        this.enabled = true;
        this.tooltip = "";
        this.inputBinding = null;
        this.hotbarKeyOverrides = new ArrayList<>();
    }

    public static DefaultAssetMap<String, ClassAbilityDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, ClassAbilityDefinition>) AssetRegistry.getAssetStore(ClassAbilityDefinition.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    public static AssetStore<String, ClassAbilityDefinition, ?> getAssetStore() {
        return (AssetStore<String, ClassAbilityDefinition, ?>) AssetRegistry.getAssetStore(ClassAbilityDefinition.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return this.abilityId;
    }

    // === Getters and Setters ===

    public String getAbilityId() {
        return abilityId;
    }

    public void setAbilityId(String abilityId) {
        this.abilityId = abilityId;
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

    public String getParentAbilityId() {
        return parentAbilityId;
    }

    public void setParentAbilityId(String parentAbilityId) {
        this.parentAbilityId = parentAbilityId;
    }

    public AbilityType getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(AbilityType abilityType) {
        this.abilityType = abilityType;
    }

    public TargetingType getTargetingType() {
        return targetingType;
    }

    public void setTargetingType(TargetingType targetingType) {
        this.targetingType = targetingType;
    }

    public Map<String, Float> getResourceCosts() {
        return resourceCosts;
    }

    public void setResourceCosts(Map<String, Float> resourceCosts) {
        this.resourceCosts = resourceCosts;
    }

    public float getCooldown() {
        return cooldown;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public float getGlobalCooldown() {
        return globalCooldown;
    }

    public void setGlobalCooldown(float globalCooldown) {
        this.globalCooldown = globalCooldown;
    }

    public List<String> getCooldownCategories() {
        return cooldownCategories;
    }

    public void setCooldownCategories(List<String> cooldownCategories) {
        this.cooldownCategories = cooldownCategories;
    }

    public float getCastTime() {
        return castTime;
    }

    public void setCastTime(float castTime) {
        this.castTime = castTime;
    }

    public float getChannelDuration() {
        return channelDuration;
    }

    public void setChannelDuration(float channelDuration) {
        this.channelDuration = channelDuration;
    }

    public boolean isInterruptible() {
        return interruptible;
    }

    public void setInterruptible(boolean interruptible) {
        this.interruptible = interruptible;
    }

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public float getAoeRadius() {
        return aoeRadius;
    }

    public void setAoeRadius(float aoeRadius) {
        this.aoeRadius = aoeRadius;
    }

    public float getConeAngle() {
        return coneAngle;
    }

    public void setConeAngle(float coneAngle) {
        this.coneAngle = coneAngle;
    }

    public List<String> getPrerequisiteAbilities() {
        return prerequisiteAbilities;
    }

    public void setPrerequisiteAbilities(List<String> prerequisiteAbilities) {
        this.prerequisiteAbilities = prerequisiteAbilities;
    }

    public Map<String, Integer> getStatRequirements() {
        return statRequirements;
    }

    public void setStatRequirements(Map<String, Integer> statRequirements) {
        this.statRequirements = statRequirements;
    }

    public boolean hasRanks() {
        return hasRanks;
    }

    public void setHasRanks(boolean hasRanks) {
        this.hasRanks = hasRanks;
    }

    public int getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    public List<RankModifier> getRankModifiers() {
        return rankModifiers;
    }

    public void setRankModifiers(List<RankModifier> rankModifiers) {
        this.rankModifiers = rankModifiers;
    }

    public String getInteractionChainId() {
        return interactionChainId;
    }

    public void setInteractionChainId(String interactionChainId) {
        this.interactionChainId = interactionChainId;
    }

    public Map<String, String> getInteractionParams() {
        return interactionParams;
    }

    public void setInteractionParams(Map<String, String> interactionParams) {
        this.interactionParams = interactionParams;
    }

    public String getCastAnimation() {
        return castAnimation;
    }

    public void setCastAnimation(String castAnimation) {
        this.castAnimation = castAnimation;
    }

    public String getCastSound() {
        return castSound;
    }

    public void setCastSound(String castSound) {
        this.castSound = castSound;
    }

    public String getImpactSound() {
        return impactSound;
    }

    public void setImpactSound(String impactSound) {
        this.impactSound = impactSound;
    }

    public String getParticleEffect() {
        return particleEffect;
    }

    public void setParticleEffect(String particleEffect) {
        this.particleEffect = particleEffect;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public AbilityInputBinding getInputBinding() {
        return inputBinding;
    }

    public void setInputBinding(AbilityInputBinding inputBinding) {
        this.inputBinding = inputBinding;
    }

    public List<Integer> getHotbarKeyOverrides() {
        return hotbarKeyOverrides;
    }

    public void setHotbarKeyOverrides(List<Integer> hotbarKeyOverrides) {
        this.hotbarKeyOverrides = hotbarKeyOverrides;
    }

    /**
     * Merge settings from parent ability. Child values take precedence.
     * Used for ability extension (e.g., "Holy Strike" extending "Strike")
     */
    public void mergeFrom(ClassAbilityDefinition parent) {
        if (parent == null) return;
        
        // Don't inherit abilityId, parentAbilityId
        if (this.displayName == null || this.displayName.isEmpty()) this.displayName = parent.displayName;
        if (this.description == null || this.description.isEmpty()) this.description = parent.description;
        if (this.iconId == null || this.iconId.isEmpty()) this.iconId = parent.iconId;
        
        // Inherit ability type and targeting if not set
        if (this.abilityType == null) this.abilityType = parent.abilityType;
        if (this.targetingType == null) this.targetingType = parent.targetingType;
        
        // Merge resource costs (child can override specific resources)
        if (parent.resourceCosts != null && !parent.resourceCosts.isEmpty()) {
            Map<String, Float> merged = new HashMap<>(parent.resourceCosts);
            merged.putAll(this.resourceCosts); // Child overrides
            this.resourceCosts = merged;
        }
        
        // Cooldowns - inherit if not set (0 = not set)
        if (this.cooldown == 0f && parent.cooldown != 0f) this.cooldown = parent.cooldown;
        if (this.globalCooldown == 0f && parent.globalCooldown != 0f) this.globalCooldown = parent.globalCooldown;
        
        // Merge cooldown categories
        if (parent.cooldownCategories != null && !parent.cooldownCategories.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.cooldownCategories);
            for (String cat : this.cooldownCategories) {
                if (!merged.contains(cat)) merged.add(cat);
            }
            this.cooldownCategories = merged;
        }
        
        // Casting - inherit if not set
        if (this.castTime == 0f && parent.castTime != 0f) this.castTime = parent.castTime;
        if (this.channelDuration == 0f && parent.channelDuration != 0f) this.channelDuration = parent.channelDuration;
        // interruptible defaults to true, so only inherit if parent is false
        if (!parent.interruptible) this.interruptible = parent.interruptible;
        
        // Range and targeting - inherit if not set
        if (this.range == 0f && parent.range != 0f) this.range = parent.range;
        if (this.aoeRadius == 0f && parent.aoeRadius != 0f) this.aoeRadius = parent.aoeRadius;
        if (this.coneAngle == 0f && parent.coneAngle != 0f) this.coneAngle = parent.coneAngle;
        
        // Merge prerequisites (child can add more)
        if (parent.prerequisiteAbilities != null && !parent.prerequisiteAbilities.isEmpty()) {
            List<String> merged = new ArrayList<>(parent.prerequisiteAbilities);
            for (String prereq : this.prerequisiteAbilities) {
                if (!merged.contains(prereq)) merged.add(prereq);
            }
            this.prerequisiteAbilities = merged;
        }
        
        // Merge stat requirements (child can add more)
        if (parent.statRequirements != null && !parent.statRequirements.isEmpty()) {
            Map<String, Integer> merged = new HashMap<>(parent.statRequirements);
            merged.putAll(this.statRequirements); // Child overrides
            this.statRequirements = merged;
        }
        
        // Ranks - inherit if not set
        if (!this.hasRanks && parent.hasRanks) {
            this.hasRanks = parent.hasRanks;
            this.maxRank = parent.maxRank;
            this.rankModifiers = new ArrayList<>(parent.rankModifiers);
        }
        
        // Interaction chain - inherit if not set
        if (this.interactionChainId == null || this.interactionChainId.isEmpty()) {
            this.interactionChainId = parent.interactionChainId;
        }
        
        // Merge interaction params
        if (parent.interactionParams != null && !parent.interactionParams.isEmpty()) {
            Map<String, String> merged = new HashMap<>(parent.interactionParams);
            merged.putAll(this.interactionParams); // Child overrides
            this.interactionParams = merged;
        }
        
        // Visual/Audio - inherit if not set
        if (this.castAnimation == null || this.castAnimation.isEmpty()) this.castAnimation = parent.castAnimation;
        if (this.castSound == null || this.castSound.isEmpty()) this.castSound = parent.castSound;
        if (this.impactSound == null || this.impactSound.isEmpty()) this.impactSound = parent.impactSound;
        if (this.particleEffect == null || this.particleEffect.isEmpty()) this.particleEffect = parent.particleEffect;
        
        // Tooltip - inherit if not set
        if (this.tooltip == null || this.tooltip.isEmpty()) this.tooltip = parent.tooltip;

        // Input binding - inherit if not set
        if (this.inputBinding == null) this.inputBinding = parent.inputBinding;
        if ((this.hotbarKeyOverrides == null || this.hotbarKeyOverrides.isEmpty())
            && parent.hotbarKeyOverrides != null && !parent.hotbarKeyOverrides.isEmpty()) {
            this.hotbarKeyOverrides = new ArrayList<>(parent.hotbarKeyOverrides);
        }
        
        // enabled stays as-is (child controls this)
    }

    // === Enums ===

    public enum AbilityType {
        Active, // Must be manually activated
        Passive, // Always active
        Toggle // Can be toggled on/off
    }

    public enum TargetingType {
        Self, // Self-cast only
        Target, // Requires target
        Ground, // Ground-targeted
        Aoe, // Area of effect around caster
        Cone, // Cone in front of caster
        Line, // Line from caster
        Chain // Chains between targets
    }

    public enum AbilityInputBinding {
        Ability1,
        Ability2,
        Ability3
    }

    // === Nested Classes ===

    /**
     * Describes how ability properties change with rank
     */
    public static class RankModifier {
        public static final BuilderCodec<RankModifier> CODEC = BuilderCodec.builder(RankModifier.class, RankModifier::new)
            .append(new KeyedCodec<>("rank", Codec.INTEGER, false, true), (i, s) -> i.rank = s, i -> i.rank)
            .add()
            .append(new KeyedCodec<>("propertyChanges", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                (i, s) -> i.propertyChanges = s, i -> i.propertyChanges)
            .add()
            .build();

        private int rank;
        private Map<String, Float> propertyChanges; // Property -> new value (e.g., "damage": 150.0)

        public RankModifier() {
            this.rank = 1;
            this.propertyChanges = new HashMap<>();
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public Map<String, Float> getPropertyChanges() {
            return propertyChanges;
        }

        public void setPropertyChanges(Map<String, Float> propertyChanges) {
            this.propertyChanges = propertyChanges;
        }
    }
}
