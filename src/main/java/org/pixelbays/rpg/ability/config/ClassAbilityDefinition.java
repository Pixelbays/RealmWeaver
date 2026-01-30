package org.pixelbays.rpg.ability.config;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.config.ClassAbilityDefinition.AbilityInputBinding;
import org.pixelbays.rpg.global.config.InteractionVarsEntry;
import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIButton;
import com.hypixel.hytale.codec.schema.metadata.ui.UICreateButtons;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorPreview;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorSectionStart;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.codec.schema.metadata.ui.UISidebarButtons;
import com.hypixel.hytale.codec.schema.metadata.ui.UITypeIcon;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.protocol.AssetIconProperties;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemPullbackConfig;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import com.hypixel.hytale.server.core.asset.type.itemsound.config.ItemSoundSet;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.UnarmedInteractions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionConfiguration;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ClassAbilityDefinition
        implements JsonAssetWithMap<String, DefaultAssetMap<String, ClassAbilityDefinition>> {
    
    private static final Codec<List<Integer>> INT_LIST_CODEC = new com.hypixel.hytale.codec.function.FunctionCodec<>(
            new ArrayCodec<>(Codec.INTEGER, Integer[]::new),
            arr -> arr == null ? Collections.emptyList() : Arrays.asList(arr),
            list -> list == null ? new Integer[0] : list.toArray(new Integer[0]));
            
    private static final AssetBuilderCodec.Builder<String, ClassAbilityDefinition> CODEC_BUILDER = AssetBuilderCodec
            .builder(
                    ClassAbilityDefinition.class,
                    ClassAbilityDefinition::new,
                    Codec.STRING,
                    (item, blockTypeKey) -> item.id = blockTypeKey,
                    item -> item.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data)
            .metadata(new UIEditorPreview(UIEditorPreview.PreviewType.ITEM))
            .metadata(new UITypeIcon("Item.png"))
            .metadata(
                    new UIRebuildCaches(
                            false,
                            UIRebuildCaches.ClientCache.MODELS,
                            UIRebuildCaches.ClientCache.BLOCK_TEXTURES,
                            UIRebuildCaches.ClientCache.MODEL_TEXTURES,
                            UIRebuildCaches.ClientCache.MAP_GEOMETRY,
                            UIRebuildCaches.ClientCache.ITEM_ICONS))
            .metadata(new UISidebarButtons(new UIButton("server.assetEditor.buttons.equipItem", "EquipItem")))
            .metadata(new UICreateButtons(new UIButton("server.assetEditor.buttons.createAndEquipItem", "EquipItem")))
            .<String>appendInherited(new KeyedCodec<>("Icon", Codec.STRING), 
                    (i, s) -> i.Icon = (s == null || s.isEmpty()) ? null : s,
                    i -> i.Icon,
                    (i, parent) -> i.Icon = parent.Icon)
            .addValidator(CommonAssetValidator.ICON_ITEM)
            .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))
            .add()
            .<String[]>appendInherited(
                    new KeyedCodec<>("Categories",
                            new ArrayCodec<>(Codec.STRING, String[]::new)
                                    .metadata(new UIEditor(new UIEditor.Dropdown("AbilityCategories")))),
                    (ability, s) -> ability.Categories = s,
                    ability -> ability.Categories,
                    (ability, parent) -> ability.Categories = parent.Categories)
            .addValidatorLate(() -> AbilityCategory.VALIDATOR_CACHE.getArrayValidator().late())
            .documentation("A list of categories this Ability will be shown in on the creative library menu.")
            .add()
            .<AbilityTranslationProperties>appendInherited(
                    new KeyedCodec<>("TranslationProperties", AbilityTranslationProperties.CODEC),
                    (ability, s) -> ability.translationProperties = s,
                    ability -> ability.translationProperties,
                    (ability, parent) -> ability.translationProperties = parent.translationProperties)
            .documentation("The translation properties for this Ability asset.")
            .add()
            .appendInherited(
                    new KeyedCodec<>("AbilityLevel", Codec.INTEGER),
                    (ability, s) -> ability.abilityLevel = s,
                    ability -> ability.abilityLevel,
                    (ability, parent) -> ability.abilityLevel = parent.abilityLevel)
            .add()
            .<String>appendInherited(
                    new KeyedCodec<>("SoundEventId", Codec.STRING),
                    (ability, s) -> ability.soundEventId = s,
                    ability -> ability.soundEventId,
                    (ability, parent) -> ability.soundEventId = parent.soundEventId)
            .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
            .add()
            .<String>appendInherited(
                    new KeyedCodec<>("AbilitySoundSetId", Codec.STRING),
                    (ability, s) -> ability.abilitySoundSetId = s,
                    ability -> ability.abilitySoundSetId,
                    (ability, parent) -> ability.abilitySoundSetId = parent.abilitySoundSetId)
            .addValidator(Validators.nonNull())
            .addValidator(ItemSoundSet.VALIDATOR_CACHE.getValidator())
            .add()
            .appendInherited(
                    new KeyedCodec<>("UsePlayerAnimations", Codec.BOOLEAN),
                    (ability, s) -> ability.usePlayerAnimations = s,
                    ability -> ability.usePlayerAnimations,
                    (ability, parent) -> ability.usePlayerAnimations = parent.usePlayerAnimations)
            .add()
            .<String>appendInherited(
                    new KeyedCodec<>("PlayerAnimationsId", ItemPlayerAnimations.CHILD_CODEC),
                    (ability, s) -> ability.playerAnimationsId = s,
                    ability -> ability.playerAnimationsId,
                    (ability, parent) -> ability.playerAnimationsId = parent.playerAnimationsId)
            .addValidator(Validators.nonNull())
            .addValidator(ItemPlayerAnimations.VALIDATOR_CACHE.getValidator())
            .add()
            .<ModelParticle[]>appendInherited(
                    new KeyedCodec<>("Particles", ModelParticle.ARRAY_CODEC),
                    (ability, s) -> ability.particles = s,
                    ability -> ability.particles,
                    (ability, parent) -> ability.particles = parent.particles)
            .metadata(new UIPropertyTitle("Item Particles"))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.MODELS))
            .documentation(
                    "The particles played for this item. If this is a block, block specific properties should be used instead.")
            .add()
            .<ModelParticle[]>appendInherited(
                    new KeyedCodec<>("FirstPersonParticles", ModelParticle.ARRAY_CODEC),
                    (ability, s) -> ability.firstPersonParticles = s,
                    ability -> ability.firstPersonParticles,
                    (ability, parent) -> ability.firstPersonParticles = parent.firstPersonParticles)
            .metadata(new UIPropertyTitle("Item First Person Particles"))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.MODELS))
            .documentation(
                    "The particles played for this item when in first person. If this is a block, block specific properties should be used instead.")
            .add()
            .<Map<InteractionType, String>>appendInherited(
                    new KeyedCodec<>("Interactions",
                            new EnumMapCodec<>(InteractionType.class, RootInteraction.CHILD_ASSET_CODEC)),
                    (ability, v) -> ability.interactions = MapUtil.combineUnmodifiable(ability.interactions, v,
                            () -> new EnumMap<>(InteractionType.class)),
                    ability -> ability.interactions,
                    (ability, parent) -> ability.interactions = parent.interactions)
            .addValidator(RootInteraction.VALIDATOR_CACHE.getMapValueValidator())
            .metadata(new UIEditorSectionStart("Interactions"))
            .add()
            .<InteractionConfiguration>appendInherited(
                    new KeyedCodec<>("InteractionConfig", InteractionConfiguration.CODEC),
                    (ability, v) -> ability.interactionConfig = v,
                    ability -> ability.interactionConfig,
                    (ability, parent) -> ability.interactionConfig = parent.interactionConfig)
            .addValidator(Validators.nonNull())
            .add()
            .<Map<String, InteractionVarsEntry>>appendInherited(
                    new KeyedCodec<>("InteractionVars",
                            new MapCodec<>(InteractionVarsEntry.CODEC, HashMap::new)),
                    (ability, v) -> ability.interactionVars = MapUtil.combineUnmodifiable(ability.interactionVars, v),
                    ability -> ability.interactionVars,
                    (ability, parent) -> ability.interactionVars = parent.interactionVars)
            .documentation(
                    "Map of named interaction overrides with inline Interaction definitions. " +
                    "Each Interaction can specify Parent, DamageCalculator, DamageEffects, Effects, and other properties."
            )
            .add()

            .<ItemPullbackConfig>appendInherited(
                    new KeyedCodec<>("PullbackConfig", ItemPullbackConfig.CODEC),
                    (ability, s) -> ability.pullbackConfig = s,
                    ability -> ability.pullbackConfig,
                    (ability, parent) -> ability.pullbackConfig = parent.pullbackConfig)
            .documentation("Overrides the offset of first person arms when close to obstacles")
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
            .appendInherited(new KeyedCodec<>("GlobalCooldown", Codec.FLOAT, false, true),
                    (i, s) -> i.GlobalCooldown = s, i -> i.GlobalCooldown,
                    (i, parent) -> i.GlobalCooldown = parent.GlobalCooldown)
            .add()
            .appendInherited(new KeyedCodec<>("GlobalCooldownCategories", new ArrayCodec<>(Codec.STRING,
                    String[]::new), false, true),
                    (i, s) -> i.GlobalCooldownCategories = s, i -> i.GlobalCooldownCategories,
                    (i, parent) -> i.GlobalCooldownCategories = parent.GlobalCooldownCategories)
            .add()
            .appendInherited(new KeyedCodec<>("AbilityType", new EnumCodec<>(AbilityType.class), false, true),
                    (i, s) -> i.AbilityTypeValue = s, i -> i.AbilityTypeValue,
                    (i, parent) -> i.AbilityTypeValue = parent.AbilityTypeValue)
            .add()
            .afterDecode(ClassAbilityDefinition::processConfig);

    public static final AssetCodec<String, ClassAbilityDefinition> CODEC = CODEC_BUILDER.build();
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(
            new AssetKeyValidator<>(ClassAbilityDefinition::getAssetStore));
    private static AssetStore<String, ClassAbilityDefinition, DefaultAssetMap<String, ClassAbilityDefinition>> ASSET_STORE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String Icon;
    protected AssetIconProperties iconProperties;
    protected AbilityTranslationProperties translationProperties;
    protected AbilityType AbilityTypeValue;
    protected float GlobalCooldown;
    protected String[] GlobalCooldownCategories;
    protected List<String> PrerequisiteAbilities;
    protected Map<String, Integer> StatRequirements;
    protected int abilityLevel;
    protected String qualityId;
    protected int qualityIndex = 0;
    protected String playerAnimationsId = "Default";
    protected boolean usePlayerAnimations = false;
    protected String animation;
    protected String[] Categories;
    protected String soundEventId;
    protected transient int soundEventIndex;
    protected String abilitySoundSetId = "ISS_Default";
    protected transient int abilitySoundSetIndex;
    protected ModelParticle[] particles;
    protected ModelParticle[] firstPersonParticles;
    protected Map<InteractionType, String> interactions = Collections.emptyMap();
    protected Map<String, InteractionVarsEntry> interactionVars = Collections.emptyMap();
    protected InteractionConfiguration interactionConfig;
    protected boolean HasRanks = false;
    protected int MaxRank = 0;
    protected boolean Enabled = true;
    protected String Tooltip;
    protected AbilityInputBinding InputBinding;
    protected List<Integer> HotbarKeyOverrides;

    @Nullable
    protected ItemPullbackConfig pullbackConfig;

    private transient SoftReference<org.pixelbays.rpg.ability.protocol.AbilityBase> cachedPacket;

    public static AssetStore<String, ClassAbilityDefinition, DefaultAssetMap<String, ClassAbilityDefinition>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(ClassAbilityDefinition.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, ClassAbilityDefinition> getAssetMap() {
        return (DefaultAssetMap<String, ClassAbilityDefinition>) getAssetStore().getAssetMap();
    }

    protected ClassAbilityDefinition() {
    }

    public ClassAbilityDefinition(String id) {
        this.id = id;
    }

    public ClassAbilityDefinition(@Nonnull ClassAbilityDefinition other) {
        this.data = other.data;
        this.id = other.id;
        this.Icon = other.Icon;
        this.iconProperties = other.iconProperties;
        this.translationProperties = other.translationProperties;
        this.abilityLevel = other.abilityLevel;
        this.qualityId = other.qualityId;
        this.playerAnimationsId = other.playerAnimationsId;
        this.usePlayerAnimations = other.usePlayerAnimations;
        this.animation = other.animation;
        this.Categories = other.Categories;
        this.soundEventId = other.soundEventId;
        this.soundEventIndex = other.soundEventIndex;
        this.abilitySoundSetId = other.abilitySoundSetId;
        this.abilitySoundSetIndex = other.abilitySoundSetIndex;
        this.particles = other.particles;
        this.firstPersonParticles = other.firstPersonParticles;
        this.interactions = other.interactions;
        this.interactionVars = other.interactionVars;
        this.interactionConfig = other.interactionConfig;
        this.HasRanks = other.HasRanks;
        this.MaxRank = other.MaxRank;
        this.Enabled = other.Enabled;
        this.Tooltip = other.Tooltip;
        this.InputBinding = other.InputBinding;
        this.HotbarKeyOverrides = other.HotbarKeyOverrides;
        this.PrerequisiteAbilities = other.PrerequisiteAbilities;
        this.StatRequirements = other.StatRequirements;
        this.GlobalCooldown = other.GlobalCooldown;
        this.GlobalCooldownCategories = other.GlobalCooldownCategories;
        this.pullbackConfig = other.pullbackConfig;
        this.AbilityTypeValue = other.AbilityTypeValue;
    }

    @Nonnull
    public org.pixelbays.rpg.ability.protocol.AbilityBase toPacket() {
        org.pixelbays.rpg.ability.protocol.AbilityBase cached = this.cachedPacket == null ? null : this.cachedPacket.get();
        if (cached != null) {
            return cached;
        } else {
            org.pixelbays.rpg.ability.protocol.AbilityBase packet = new org.pixelbays.rpg.ability.protocol.AbilityBase();
            packet.id = this.id;
            if (this.Icon != null) {
                packet.icon = this.Icon;
            }

            if (this.iconProperties != null) {
                packet.iconProperties = this.iconProperties;
            }

            if (this.translationProperties != null) {
                packet.translationProperties = this.translationProperties.toPacket();
            }

            if (this.animation != null) {
                packet.animation = this.animation;
            }

            packet.playerAnimationsId = this.playerAnimationsId;
            packet.usePlayerAnimations = this.usePlayerAnimations;
            packet.abilityLevel = this.abilityLevel;
            packet.qualityIndex = this.qualityIndex;

            if (this.Categories != null && this.Categories.length > 0) {
                packet.categories = this.Categories;
            }

            packet.soundEventIndex = this.soundEventIndex;
            packet.abilitySoundSetIndex = this.abilitySoundSetIndex;
            if (this.particles != null && this.particles.length > 0) {
                packet.particles = new com.hypixel.hytale.protocol.ModelParticle[this.particles.length];

                for (int i = 0; i < this.particles.length; i++) {
                    packet.particles[i] = this.particles[i].toPacket();
                }
            }

            if (this.firstPersonParticles != null && this.firstPersonParticles.length > 0) {
                packet.firstPersonParticles = new com.hypixel.hytale.protocol.ModelParticle[this.firstPersonParticles.length];

                for (int i = 0; i < this.firstPersonParticles.length; i++) {
                    packet.firstPersonParticles[i] = this.firstPersonParticles[i].toPacket();
                }
            }

            Object2IntOpenHashMap<InteractionType> interactionsIntMap = new Object2IntOpenHashMap<>();

            for (Entry<InteractionType, String> e : this.interactions.entrySet()) {
                interactionsIntMap.put(e.getKey(), RootInteraction.getRootInteractionIdOrUnknown(e.getValue()));
            }

            packet.interactions = interactionsIntMap;
            
            Object2IntOpenHashMap<String> interactionVarsIntMap = new Object2IntOpenHashMap<>();
            
            packet.interactionVars = interactionVarsIntMap.isEmpty() ? null : interactionVarsIntMap;
            packet.interactionConfig = this.interactionConfig.toPacket();

            if (this.data != null) {
                IntSet expandedTagIndexes = this.data.getExpandedTagIndexes();
                if (expandedTagIndexes != null) {
                    packet.tagIndexes = expandedTagIndexes.toIntArray();
                }
            }

            this.cachedPacket = new SoftReference<>(packet);
            return packet;
        }
    }

    @Nullable
    public ClassAbilityDefinition getAbilityForState(String state) {
        String id = this.getAbilityIdForState(state);
        return id == null ? null : getAssetMap().getAsset(id);
    }

    @Nullable
    public String getAbilityIdForState(String state) {
        // TODO: Implement state logic if needed
        return null;
    }

    public boolean isState() {
        return this.getStateForAbility(this.id) != null;
    }

    @Nullable
    public String getStateForAbility(@Nonnull ClassAbilityDefinition ability) {
        return this.getStateForAbility(ability.getId());
    }

    @Nullable
    public String getStateForAbility(String abilityId) {
        // TODO: Implement state logic if needed
        return null;
    }

    public AssetExtraInfo.Data getData() {
        return this.data;
    }

    public String getId() {
        return this.id;
    }

    @Nonnull
    public String getTranslationKey() {
        if (this.translationProperties != null) {
            String nameTranslation = this.translationProperties.getName();
            if (nameTranslation != null) {
                return nameTranslation;
            }
        }

        return "server.abilities." + this.id + ".name";
    }

    @Nonnull
    public String getDescriptionTranslationKey() {
        if (this.translationProperties != null) {
            String descriptionTranslation = this.translationProperties.getDescription();
            if (descriptionTranslation != null) {
                return descriptionTranslation;
            }
        }

        return "server.abilities." + this.id + ".description";
    }

    public boolean getUsePlayerAnimations() {
        return this.usePlayerAnimations;
    }

    public String getPlayerAnimationsId() {
        return this.playerAnimationsId;
    }

    public String getIcon() {
        return this.Icon;
    }

    public AssetIconProperties getIconProperties() {
        return this.iconProperties;
    }

    public AbilityTranslationProperties getTranslationProperties() {
        return this.translationProperties;
    }

    public int getAbilityLevel() {
        return this.abilityLevel;
    }

    public int getQualityIndex() {
        return this.qualityIndex;
    }

    public String[] getCategories() {
        return this.Categories;
    }

    public String getSoundEventId() {
        return this.soundEventId;
    }

    public int getSoundEventIndex() {
        return this.soundEventIndex;
    }

    public Map<InteractionType, String> getInteractions() {
        return this.interactions;
    }

    public Map<String, InteractionVarsEntry> getInteractionVars() {
        return this.interactionVars;
    }

    public InteractionConfiguration getInteractionConfig() {
        return this.interactionConfig;
    }

    public int getAbilitySoundSetIndex() {
        return this.abilitySoundSetIndex;
    }

    public String getDisplayName() {
        return this.getTranslationKey();
    }

    public String getInteractionChainId() {
        return this.interactions.getOrDefault(InteractionType.Primary, null);
    }

    public AbilityInputBinding getInputBinding() {
        return this.InputBinding;
    }

    public List<String> getPrerequisiteAbilities() {
        return this.PrerequisiteAbilities;
    }

    public Map<String, Integer> getStatRequirements() {
        return this.StatRequirements;
    }

    public boolean getHasRanks() {
        return this.HasRanks;
    }

    public int getMaxRank() {
        return this.MaxRank;
    }

    public float getGlobalCooldown() {
        return this.GlobalCooldown;
    }

    public String[] getGlobalCooldownCategories() {
        return this.GlobalCooldownCategories;
    }

    public AbilityType getAbilityType() {
        return this.AbilityTypeValue;
    }

    public List<Integer> getHotbarKeyOverrides() {
        return this.HotbarKeyOverrides;
    }

    public boolean isEnabled() {
        return this.Enabled;
    }

    public String getTooltip() {
        return this.Tooltip;
    }

    protected void processConfig() {

        Map<InteractionType, String> interactions = this.interactions.isEmpty() ? new EnumMap<>(InteractionType.class)
                : new EnumMap<>(this.interactions);
        DefaultAssetMap<String, UnarmedInteractions> unarmedInteractionsAssetMap = UnarmedInteractions.getAssetMap();
        UnarmedInteractions fallbackInteractions = this.playerAnimationsId != null
                ? unarmedInteractionsAssetMap.getAsset(this.playerAnimationsId)
                : null;
        if (fallbackInteractions != null) {
            for (Entry<InteractionType, String> entry : fallbackInteractions.getInteractions().entrySet()) {
                interactions.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        UnarmedInteractions defaultUnarmedInteractions = unarmedInteractionsAssetMap.getAsset("Empty");
        if (defaultUnarmedInteractions != null) {
            for (Entry<InteractionType, String> entry : defaultUnarmedInteractions.getInteractions().entrySet()) {
                interactions.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        this.interactions = Collections.unmodifiableMap(interactions);

        IndexedLookupTableAssetMap<String, AbilityQuality> abilityQualityAssetMap = AbilityQuality.getAssetMap();
        if (this.qualityId != null && abilityQualityAssetMap != null) {
            this.qualityIndex = abilityQualityAssetMap.getIndexOrDefault(this.qualityId, 0);
            AbilityQuality abilityQuality = abilityQualityAssetMap.getAsset(this.qualityIndex);
        }

        if (this.interactionConfig == null) {
            this.interactionConfig = InteractionConfiguration.DEFAULT;
        }

        if (this.soundEventId != null) {
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEventId);
        }

        this.abilitySoundSetIndex = ItemSoundSet.getAssetMap().getIndex(this.abilitySoundSetId);
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

    /*
     * // LEAVE THIS IN FOR REFERENCE
     * static {
     * CODEC_BUILDER.<Map>appendInherited(
     * new KeyedCodec<>("State",
     * new MapCodec(
     * new ContainedAssetCodec<>(Item.class, CODEC,
     * ContainedAssetCodec.Mode.INJECT_PARENT),
     * HashMap::new)),
     * (item, m) -> item.stateToBlock = m,
     * item -> item.stateToBlock,
     * (item, parent) -> item.stateToBlock = parent.stateToBlock)
     * .metadata(new UIEditorSectionStart("State"))
     * .add();
     * }
     */
}
