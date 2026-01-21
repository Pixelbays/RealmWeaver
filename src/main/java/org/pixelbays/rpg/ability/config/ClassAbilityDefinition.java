package org.pixelbays.rpg.ability.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;
import org.pixelbays.rpg.global.config.builder.InteractionChainRefCodec;

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
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

/**
 * Configuration for an RPG ability (class or race).
 * Loaded from /Server/RPGAbilities/** (recursive)
 * 
 * This defines the ability properties. Class or race assets reference
 * these by id.
 */
@SuppressWarnings({ "deprecation" })
public class ClassAbilityDefinition
        implements JsonAssetWithMap<String, DefaultAssetMap<String, ClassAbilityDefinition>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    private static final FunctionCodec<RankModifier[], List<RankModifier>> RANK_MODIFIER_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(RankModifier.CODEC, RankModifier[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new RankModifier[0] : list.toArray(RankModifier[]::new));

    private static final FunctionCodec<Integer[], List<Integer>> INT_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.INTEGER, Integer[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new Integer[0] : list.toArray(Integer[]::new));

    private static final Codec<String> ABILITY_REF_CODEC = new AbilityRefCodec();
    private static final Codec<String> INTERACTION_CHAIN_REF_CODEC = new InteractionChainRefCodec();

    public static final AssetBuilderCodec<String, ClassAbilityDefinition> CODEC = AssetBuilderCodec.builder(
            ClassAbilityDefinition.class,
            ClassAbilityDefinition::new,
            Codec.STRING,
            (t, k) -> t.AbilityId = k,
            t -> t.AbilityId,
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
            .appendInherited(new KeyedCodec<>("Parent", ABILITY_REF_CODEC, false, true),
                    (i, s) -> i.ParentAbilityId = s, i -> i.ParentAbilityId,
                    (i, parent) -> i.ParentAbilityId = parent.ParentAbilityId)
            .add()
            .appendInherited(new KeyedCodec<>("AbilityType", new EnumCodec<>(AbilityType.class), false, true),
                    (i, s) -> i.AbilityTypeValue = s, i -> i.AbilityTypeValue,
                    (i, parent) -> i.AbilityTypeValue = parent.AbilityTypeValue)
            .add()
            .appendInherited(new KeyedCodec<>("TargetingType", new EnumCodec<>(TargetingType.class), false, true),
                    (i, s) -> i.TargetingTypeValue = s, i -> i.TargetingTypeValue,
                    (i, parent) -> i.TargetingTypeValue = parent.TargetingTypeValue)
            .add()
            .appendInherited(new KeyedCodec<>("ResourceCosts", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                    (i, s) -> i.ResourceCosts = s, i -> i.ResourceCosts,
                    (i, parent) -> i.ResourceCosts = parent.ResourceCosts)
            .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("Cooldown", Codec.FLOAT, false, true), (i, s) -> i.Cooldown = s,
                    i -> i.Cooldown,
                    (i, parent) -> i.Cooldown = parent.Cooldown)
            .add()
            .appendInherited(new KeyedCodec<>("GlobalCooldown", Codec.FLOAT, false, true),
                    (i, s) -> i.GlobalCooldown = s, i -> i.GlobalCooldown,
                    (i, parent) -> i.GlobalCooldown = parent.GlobalCooldown)
            .add()
            .appendInherited(new KeyedCodec<>("CooldownCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.CooldownCategories = s, i -> i.CooldownCategories,
                    (i, parent) -> i.CooldownCategories = parent.CooldownCategories)
            .add()
            .appendInherited(new KeyedCodec<>("CastTime", Codec.FLOAT, false, true), (i, s) -> i.CastTime = s,
                    i -> i.CastTime,
                    (i, parent) -> i.CastTime = parent.CastTime)
            .add()
            .appendInherited(new KeyedCodec<>("ChannelDuration", Codec.FLOAT, false, true),
                    (i, s) -> i.ChannelDuration = s, i -> i.ChannelDuration,
                    (i, parent) -> i.ChannelDuration = parent.ChannelDuration)
            .add()
            .appendInherited(new KeyedCodec<>("Interruptible", Codec.BOOLEAN, false, true),
                    (i, s) -> i.Interruptible = s, i -> i.Interruptible,
                    (i, parent) -> i.Interruptible = parent.Interruptible)
            .add()
            .appendInherited(new KeyedCodec<>("Range", Codec.FLOAT, false, true), (i, s) -> i.Range = s, i -> i.Range,
                    (i, parent) -> i.Range = parent.Range)
            .add()
            .appendInherited(new KeyedCodec<>("AoeRadius", Codec.FLOAT, false, true), (i, s) -> i.AoeRadius = s,
                    i -> i.AoeRadius,
                    (i, parent) -> i.AoeRadius = parent.AoeRadius)
            .add()
            .appendInherited(new KeyedCodec<>("ConeAngle", Codec.FLOAT, false, true), (i, s) -> i.ConeAngle = s,
                    i -> i.ConeAngle,
                    (i, parent) -> i.ConeAngle = parent.ConeAngle)
            .add()
            .appendInherited(new KeyedCodec<>("PrerequisiteAbilities", AbilityRefCodec.CODEC, false, true),
                    (i, s) -> i.PrerequisiteAbilities = s, i -> i.PrerequisiteAbilities,
                    (i, parent) -> i.PrerequisiteAbilities = parent.PrerequisiteAbilities)
            .add()
            .appendInherited(
                    new KeyedCodec<>("StatRequirements", new MapCodec<>(Codec.INTEGER, HashMap::new), false, true),
                    (i, s) -> i.StatRequirements = s, i -> i.StatRequirements,
                    (i, parent) -> i.StatRequirements = parent.StatRequirements)
            .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
            .add()
            .appendInherited(new KeyedCodec<>("HasRanks", Codec.BOOLEAN, false, true), (i, s) -> i.HasRanks = s,
                    i -> i.HasRanks,
                    (i, parent) -> i.HasRanks = parent.HasRanks)
            .add()
            .appendInherited(new KeyedCodec<>("MaxRank", Codec.INTEGER, false, true), (i, s) -> i.MaxRank = s,
                    i -> i.MaxRank,
                    (i, parent) -> i.MaxRank = parent.MaxRank)
            .add()
            .appendInherited(new KeyedCodec<>("RankModifiers", RANK_MODIFIER_LIST_CODEC, false, true),
                    (i, s) -> i.RankModifiers = s, i -> i.RankModifiers,
                    (i, parent) -> i.RankModifiers = parent.RankModifiers)
            .add()
            .appendInherited(new KeyedCodec<>("InteractionChainId", INTERACTION_CHAIN_REF_CODEC, false, true),
                    (i, s) -> i.InteractionChainId = s, i -> i.InteractionChainId,
                    (i, parent) -> i.InteractionChainId = parent.InteractionChainId)
            .add()
            .appendInherited(
                    new KeyedCodec<>("InteractionParams", new MapCodec<>(Codec.STRING, HashMap::new), false, true),
                    (i, s) -> i.InteractionParams = s, i -> i.InteractionParams,
                    (i, parent) -> i.InteractionParams = parent.InteractionParams)
            .add()
            .appendInherited(new KeyedCodec<>("CastAnimation", Codec.STRING, false, true),
                    (i, s) -> i.CastAnimation = s, i -> i.CastAnimation,
                    (i, parent) -> i.CastAnimation = parent.CastAnimation)
            .add()
            .appendInherited(new KeyedCodec<>("CastSound", Codec.STRING, false, true), (i, s) -> i.CastSound = s,
                    i -> i.CastSound,
                    (i, parent) -> i.CastSound = parent.CastSound)
            .add()
            .appendInherited(new KeyedCodec<>("ImpactSound", Codec.STRING, false, true), (i, s) -> i.ImpactSound = s,
                    i -> i.ImpactSound,
                    (i, parent) -> i.ImpactSound = parent.ImpactSound)
            .add()
            .appendInherited(new KeyedCodec<>("ParticleEffect", Codec.STRING, false, true),
                    (i, s) -> i.ParticleEffect = s, i -> i.ParticleEffect,
                    (i, parent) -> i.ParticleEffect = parent.ParticleEffect)
            .add()
            .appendInherited(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true), (i, s) -> i.Enabled = s,
                    i -> i.Enabled,
                    (i, parent) -> i.Enabled = parent.Enabled)
            .add()
            .appendInherited(new KeyedCodec<>("Tooltip", Codec.STRING, false, true), (i, s) -> i.Tooltip = s,
                    i -> i.Tooltip,
                    (i, parent) -> i.Tooltip = parent.Tooltip)
            .add()
            .appendInherited(new KeyedCodec<>("InputBinding", new EnumCodec<>(AbilityInputBinding.class), false, true),
                    (i, s) -> i.InputBinding = s, i -> i.InputBinding,
                    (i, parent) -> i.InputBinding = parent.InputBinding)
            .add()
            .appendInherited(new KeyedCodec<>("HotbarKeyOverrides", INT_LIST_CODEC, false, true),
                    (i, s) -> i.HotbarKeyOverrides = s, i -> i.HotbarKeyOverrides,
                    (i, parent) -> i.HotbarKeyOverrides = parent.HotbarKeyOverrides)
            .add()
            .build();

    public static AssetBuilderCodec<String, ClassAbilityDefinition> getCODEC() {
        return CODEC;
    }

    public static final ContainedAssetCodec<String, ClassAbilityDefinition, ?> CHILD_ASSET_CODEC = new ContainedAssetCodec<>(
            ClassAbilityDefinition.class, CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(
            new AssetKeyValidator<>(ClassAbilityDefinition::getAssetStore));

    private static DefaultAssetMap<String, ClassAbilityDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // === Basic Info ===
    private String AbilityId; // Unique identifier (e.g., "warrior_charge")
    private String DisplayName; // Display name
    private String Description; // Ability description
    private String IconId; // Icon asset ID

    // === Inheritance ===
    private String ParentAbilityId; // Parent ability to extend from (e.g., "basic_strike")

    // === Ability Type ===
    private AbilityType AbilityTypeValue; // Active, Passive, Toggle
    private TargetingType TargetingTypeValue; // Self, Target, Ground, AoE, Cone, etc.

    // === Costs ===
    private Map<String, Float> ResourceCosts; // Resource costs (e.g., "Mana": 50.0)

    // === Cooldown ===
    private float Cooldown; // Cooldown in seconds
    private float GlobalCooldown; // GCD override (0 = use default)
    private List<String> CooldownCategories; // Categories this ability shares cooldowns with

    // === Casting ===
    private float CastTime; // Cast time in seconds (0 = instant)
    private float ChannelDuration; // Channel duration (0 = not channeled)
    private boolean Interruptible; // Can be interrupted?

    // === Range and Targeting ===
    private float Range; // Maximum range (0 = melee)
    private float AoeRadius; // AoE radius (if applicable)
    private float ConeAngle; // Cone angle in degrees (if applicable)

    // === Requirements ===
    private List<String> PrerequisiteAbilities; // Required abilities before this can be learned
    private Map<String, Integer> StatRequirements; // Required stats (e.g., "Strength": 50)

    // === Ranks ===
    private boolean HasRanks; // Does this ability have multiple ranks?
    private int MaxRank; // Maximum rank (if hasRanks = true)
    private List<RankModifier> RankModifiers; // How each rank modifies the ability

    // === Interaction Chain ===
    private String InteractionChainId; // Hytale interaction chain to execute
    private Map<String, String> InteractionParams; // Parameters to pass to interaction

    // === Visual/Audio ===
    private String CastAnimation; // Animation to play when casting
    private String CastSound; // Sound to play when casting
    private String ImpactSound; // Sound to play on impact
    private String ParticleEffect; // Particle effect

    // === Misc ===
    private boolean Enabled; // Is this ability enabled?
    private String Tooltip; // Detailed tooltip text

    // === Input Binding ===
    private AbilityInputBinding InputBinding; // Ability1/2/3 binding
    private List<Integer> HotbarKeyOverrides; // Placeholder for 1-9 hotbar overrides

    // === Constructors ===
    public ClassAbilityDefinition() {
        this.AbilityId = "";
        this.DisplayName = "";
        this.Description = "";
        this.IconId = null;
        this.AbilityTypeValue = AbilityType.Active;
        this.TargetingTypeValue = TargetingType.Self;
        this.ResourceCosts = new HashMap<>();
        this.Cooldown = 0f;
        this.GlobalCooldown = 0f;
        this.CooldownCategories = new ArrayList<>();
        this.CastTime = 0f;
        this.ChannelDuration = 0f;
        this.Interruptible = true;
        this.Range = 0f;
        this.AoeRadius = 0f;
        this.ConeAngle = 0f;
        this.PrerequisiteAbilities = new ArrayList<>();
        this.StatRequirements = new HashMap<>();
        this.HasRanks = false;
        this.MaxRank = 1;
        this.RankModifiers = new ArrayList<>();
        this.InteractionChainId = "";
        this.InteractionParams = new HashMap<>();
        this.CastAnimation = "";
        this.CastSound = "";
        this.ImpactSound = "";
        this.ParticleEffect = "";
        this.Enabled = true;
        this.Tooltip = "";
        this.InputBinding = null;
        this.HotbarKeyOverrides = new ArrayList<>();
    }

    public static DefaultAssetMap<String, ClassAbilityDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, ClassAbilityDefinition>) AssetRegistry
                    .getAssetStore(ClassAbilityDefinition.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    public static AssetStore<String, ClassAbilityDefinition, ?> getAssetStore() {
        return (AssetStore<String, ClassAbilityDefinition, ?>) AssetRegistry
                .getAssetStore(ClassAbilityDefinition.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return this.AbilityId == null ? "" : this.AbilityId;
    }

    // === Getters and Setters ===

    public String getAbilityId() {
        return AbilityId;
    }

    public void setAbilityId(String abilityId) {
        this.AbilityId = abilityId;
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
        this.IconId = iconId;
    }

    public String getParentAbilityId() {
        return ParentAbilityId;
    }

    public void setParentAbilityId(String parentAbilityId) {
        this.ParentAbilityId = parentAbilityId;
    }

    public AbilityType getAbilityType() {
        return AbilityTypeValue;
    }

    public void setAbilityType(AbilityType abilityType) {
        this.AbilityTypeValue = abilityType;
    }

    public TargetingType getTargetingType() {
        return TargetingTypeValue;
    }

    public void setTargetingType(TargetingType targetingType) {
        this.TargetingTypeValue = targetingType;
    }

    public Map<String, Float> getResourceCosts() {
        return ResourceCosts;
    }

    public void setResourceCosts(Map<String, Float> resourceCosts) {
        this.ResourceCosts = resourceCosts;
    }

    public float getCooldown() {
        return Cooldown;
    }

    public void setCooldown(float cooldown) {
        this.Cooldown = cooldown;
    }

    public float getGlobalCooldown() {
        return GlobalCooldown;
    }

    public void setGlobalCooldown(float globalCooldown) {
        this.GlobalCooldown = globalCooldown;
    }

    public List<String> getCooldownCategories() {
        return CooldownCategories;
    }

    public void setCooldownCategories(List<String> cooldownCategories) {
        this.CooldownCategories = cooldownCategories;
    }

    public float getCastTime() {
        return CastTime;
    }

    public void setCastTime(float castTime) {
        this.CastTime = castTime;
    }

    public float getChannelDuration() {
        return ChannelDuration;
    }

    public void setChannelDuration(float channelDuration) {
        this.ChannelDuration = channelDuration;
    }

    public boolean isInterruptible() {
        return Interruptible;
    }

    public void setInterruptible(boolean interruptible) {
        this.Interruptible = interruptible;
    }

    public float getRange() {
        return Range;
    }

    public void setRange(float range) {
        this.Range = range;
    }

    public float getAoeRadius() {
        return AoeRadius;
    }

    public void setAoeRadius(float aoeRadius) {
        this.AoeRadius = aoeRadius;
    }

    public float getConeAngle() {
        return ConeAngle;
    }

    public void setConeAngle(float coneAngle) {
        this.ConeAngle = coneAngle;
    }

    public List<String> getPrerequisiteAbilities() {
        return PrerequisiteAbilities;
    }

    public void setPrerequisiteAbilities(List<String> prerequisiteAbilities) {
        this.PrerequisiteAbilities = prerequisiteAbilities;
    }

    public Map<String, Integer> getStatRequirements() {
        return StatRequirements;
    }

    public void setStatRequirements(Map<String, Integer> statRequirements) {
        this.StatRequirements = statRequirements;
    }

    public boolean hasRanks() {
        return HasRanks;
    }

    public void setHasRanks(boolean hasRanks) {
        this.HasRanks = hasRanks;
    }

    public int getMaxRank() {
        return MaxRank;
    }

    public void setMaxRank(int maxRank) {
        this.MaxRank = maxRank;
    }

    public List<RankModifier> getRankModifiers() {
        return RankModifiers;
    }

    public void setRankModifiers(List<RankModifier> rankModifiers) {
        this.RankModifiers = rankModifiers;
    }

    public String getInteractionChainId() {
        return InteractionChainId;
    }

    public void setInteractionChainId(String interactionChainId) {
        this.InteractionChainId = interactionChainId;
    }

    public Map<String, String> getInteractionParams() {
        return InteractionParams;
    }

    public void setInteractionParams(Map<String, String> interactionParams) {
        this.InteractionParams = interactionParams;
    }

    public String getCastAnimation() {
        return CastAnimation;
    }

    public void setCastAnimation(String castAnimation) {
        this.CastAnimation = castAnimation;
    }

    public String getCastSound() {
        return CastSound;
    }

    public void setCastSound(String castSound) {
        this.CastSound = castSound;
    }

    public String getImpactSound() {
        return ImpactSound;
    }

    public void setImpactSound(String impactSound) {
        this.ImpactSound = impactSound;
    }

    public String getParticleEffect() {
        return ParticleEffect;
    }

    public void setParticleEffect(String particleEffect) {
        this.ParticleEffect = particleEffect;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public void setEnabled(boolean enabled) {
        this.Enabled = enabled;
    }

    public String getTooltip() {
        return Tooltip;
    }

    public void setTooltip(String tooltip) {
        this.Tooltip = tooltip;
    }

    public AbilityInputBinding getInputBinding() {
        return InputBinding;
    }

    public void setInputBinding(AbilityInputBinding inputBinding) {
        this.InputBinding = inputBinding;
    }

    public List<Integer> getHotbarKeyOverrides() {
        return HotbarKeyOverrides;
    }

    public void setHotbarKeyOverrides(List<Integer> hotbarKeyOverrides) {
        this.HotbarKeyOverrides = hotbarKeyOverrides;
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
        public static final BuilderCodec<RankModifier> CODEC = BuilderCodec
                .builder(RankModifier.class, RankModifier::new)
                .append(new KeyedCodec<>("Rank", Codec.INTEGER, false, true), (i, s) -> i.Rank = s, i -> i.Rank)
                .add()
                .append(new KeyedCodec<>("PropertyChanges", new MapCodec<>(Codec.FLOAT, HashMap::new), false, true),
                        (i, s) -> i.PropertyChanges = s, i -> i.PropertyChanges)
                .add()
                .build();

        private int Rank;
        private Map<String, Float> PropertyChanges; // Property -> new value (e.g., "damage": 150.0)

        public RankModifier() {
            this.Rank = 1;
            this.PropertyChanges = new HashMap<>();
        }

        public int getRank() {
            return Rank;
        }

        public void setRank(int rank) {
            this.Rank = rank;
        }

        public Map<String, Float> getPropertyChanges() {
            return PropertyChanges;
        }

        public void setPropertyChanges(Map<String, Float> propertyChanges) {
            this.PropertyChanges = propertyChanges;
        }
    }
}
