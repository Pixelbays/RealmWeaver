package org.pixelbays.rpg.classes.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.builder.AbilityRefCodec;
import org.pixelbays.rpg.global.config.builder.LevelSystemRefCodec;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorPreview;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.codec.schema.metadata.ui.UITypeIcon;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.codec.validation.ValidatableCodec;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.codec.validation.validator.MapKeyValidator;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Configuration for a class/job system.
 * Loaded from /Server/Classes/{ClassName}.json
 * 
 * Supports various class types:
 * - Combat classes (Warrior, Mage, etc.)
 * - Profession classes (Woodworking, Mining, etc.)
 * - Hybrid systems
 */

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ClassDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, ClassDefinition>> {

    private static final Validator<Integer> TALENT_NODE_X_VALIDATOR = new Validator<>() {
        @Override
        public void accept(Integer value, ValidationResults results) {
            if (value == null) {
                return;
            }
            if (value < -3 || value > 3) {
                results.fail("Talent node PositionX must be between -3 and 3");
            }
        }

        @Override
        public void updateSchema(SchemaContext context, Schema target) {
            // no-op
        }
    };

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

        private static final Codec<String> ENTITY_STAT_REF_CODEC = new EntityStatRefCodec();

        private static final Codec<String> CLASS_REF_CODEC = new ClassRefCodec();
        private static final Codec<String> ABILITY_REF_CODEC = new AbilityRefCodec();
        private static final Codec<String> LEVEL_SYSTEM_REF_CODEC = new LevelSystemRefCodec();

        private static final FunctionCodec<String[], List<String>> STAT_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ENTITY_STAT_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final FunctionCodec<String[], List<String>> CLASS_ID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(CLASS_REF_CODEC, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final Validator<String> CLASS_ASSET_KEY_VALIDATOR = new Validator<>() {
        @Override
        public void accept(String key, ValidationResults results) {
            if (key == null || key.isEmpty()) {
                results.fail("Class id is empty");
                return;
            }

            ClassDefinition def = AssetRegistry.getAssetStore(ClassDefinition.class).getAssetMap().getAsset(key);
            if (def == null) {
                results.fail("Unknown class id: " + key);
            }
        }

        @Override
        public void updateSchema(SchemaContext context, Schema target) {
            if (target instanceof StringSchema stringSchema) {
                stringSchema.setTitle("Class");
                stringSchema.setHytaleAssetRef(ClassDefinition.class.getSimpleName());
            } else {
                throw new IllegalArgumentException();
            }
        }
    };

    private static final FunctionCodec<AbilityUnlock[], List<AbilityUnlock>> Ability_UNLOCK_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(AbilityUnlock.CODEC, AbilityUnlock[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(AbilityUnlock[]::new));

    private static final FunctionCodec<LevelMilestone[], List<LevelMilestone>> LEVEL_MILESTONE_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(LevelMilestone.CODEC, LevelMilestone[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(LevelMilestone[]::new));

    private static final FunctionCodec<TalentTree[], List<TalentTree>> TALENT_TREE_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(TalentTree.CODEC, TalentTree[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(TalentTree[]::new));



    public static final AssetBuilderCodec<String, ClassDefinition> CODEC = AssetBuilderCodec.builder(
            ClassDefinition.class,
            ClassDefinition::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .appendInherited(new KeyedCodec<>("Parent", Codec.STRING, false, true), (i, s) -> i.Parent = s,
                    i -> i.Parent,
                    (i, parent) -> i.Parent = parent.Parent)
            .add()
            .appendInherited(new KeyedCodec<>("DisplayName", Codec.STRING, false, true), (i, s) -> i.DisplayName = s,
                    i -> i.DisplayName,
                    (i, parent) -> i.DisplayName = parent.DisplayName)
            .add()
            .appendInherited(new KeyedCodec<>("Description", Codec.STRING, false, true), (i, s) -> i.Description = s,
                    i -> i.Description,
                    (i, parent) -> i.Description = parent.Description)
            .add()

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
            .appendInherited(new KeyedCodec<>("Icon", Codec.STRING),
                    (i, s) -> i.Icon = (s == null || s.isEmpty()) ? null : s,
                    i -> i.Icon,
                    (i, parent) -> i.Icon = parent.Icon)
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
                    .append(
                        new KeyedCodec<>("Prerequisites",
                                new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new), true),
                        (i,
                            stringObject2IntMap) -> i.Prerequisites = stringObject2IntMap,
                        i -> i.Prerequisites)
            .addValidator(Validators.nonNull())
            .addValidator(Validators.nonEmptyMap())
                        .addValidator(new MapKeyValidator<>(CLASS_ASSET_KEY_VALIDATOR.late()))
                .documentation("Required class levels to unlock this class.")
            .add()
                .appendInherited(new KeyedCodec<>("RequiredClasses", CLASS_ID_LIST_CODEC, false, true),
                    (i, s) -> i.RequiredClasses = s, i -> i.RequiredClasses,
                    (i, parent) -> i.RequiredClasses = parent.RequiredClasses)
            .add()
            .appendInherited(new KeyedCodec<>("ExclusiveWith", CLASS_ID_LIST_CODEC, false, true),
                    (i, s) -> i.ExclusiveWith = s, i -> i.ExclusiveWith,
                    (i, parent) -> i.ExclusiveWith = parent.ExclusiveWith)
            .add()
                .appendInherited(new KeyedCodec<>("LevelSystemId", LEVEL_SYSTEM_REF_CODEC, false, true),
                    (i, s) -> i.LevelSystemId = s, i -> i.LevelSystemId,
                    (i, parent) -> i.LevelSystemId = parent.LevelSystemId)
            .add()
            .appendInherited(new KeyedCodec<>("UsesCharacterLevel", Codec.BOOLEAN, false, true),
                    (i, s) -> i.UsesCharacterLevel = s, i -> i.UsesCharacterLevel,
                    (i, parent) -> i.UsesCharacterLevel = parent.UsesCharacterLevel)
            .add()
            .appendInherited(new KeyedCodec<>("IsStartingClass", Codec.BOOLEAN, false, true),
                    (i, s) -> i.IsStartingClass = s, i -> i.IsStartingClass,
                    (i, parent) -> i.IsStartingClass = parent.IsStartingClass)
            .add()
                .appendInherited(new KeyedCodec<>("IsHeroClass", Codec.BOOLEAN, false, true),
                    (i, s) -> i.IsHeroClass = s, i -> i.IsHeroClass,
                    (i, parent) -> i.IsHeroClass = parent.IsHeroClass)
                .add()
                .appendInherited(new KeyedCodec<>("HeroStartingLevel", Codec.INTEGER, false, true),
                    (i, s) -> i.HeroStartingLevel = s, i -> i.HeroStartingLevel,
                    (i, parent) -> i.HeroStartingLevel = parent.HeroStartingLevel)
                .add()
                .append(
                    new KeyedCodec<>("ResourceStats", STAT_ID_LIST_CODEC, false, true),
                    (i, s) -> i.ResourceStats = s,
                    i -> i.ResourceStats)
            .add()
            .appendInherited(new KeyedCodec<>("BaseStatModifiers", StatModifiers.CODEC, false, true),
                    (i, s) -> i.BaseStatModifiers = s, i -> i.BaseStatModifiers,
                    (i, parent) -> i.BaseStatModifiers = parent.BaseStatModifiers)
            .add()
            .appendInherited(new KeyedCodec<>("PerLevelModifiers", StatModifiers.CODEC, false, true),
                    (i, s) -> i.PerLevelModifiers = s, i -> i.PerLevelModifiers,
                    (i, parent) -> i.PerLevelModifiers = parent.PerLevelModifiers)
            .add()
            .appendInherited(
                    new KeyedCodec<>("EquipmentRestrictions", ClassDefinition.EquipmentRestrictions.CODEC, false, true),
                    (i, s) -> i.equipmentRestrictions = s, i -> i.equipmentRestrictions,
                    (i, parent) -> i.equipmentRestrictions = parent.equipmentRestrictions)
            .add()
            .appendInherited(new KeyedCodec<>("AbilityUnlocks", Ability_UNLOCK_LIST_CODEC, false, true),
                    (i, s) -> i.AbilityUnlocks = s, i -> i.AbilityUnlocks,
                    (i, parent) -> i.AbilityUnlocks = parent.AbilityUnlocks)
            .add()
            .appendInherited(new KeyedCodec<>("LevelMilestones", LEVEL_MILESTONE_LIST_CODEC, false, true),
                    (i, s) -> i.LevelMilestones = s, i -> i.LevelMilestones,
                    (i, parent) -> i.LevelMilestones = parent.LevelMilestones)
            .add()
            .appendInherited(new KeyedCodec<>("TalentTrees", TALENT_TREE_LIST_CODEC, false, true),
                    (i, s) -> i.TalentTrees = s, i -> i.TalentTrees,
                    (i, parent) -> i.TalentTrees = parent.TalentTrees)
            .add()
            .appendInherited(new KeyedCodec<>("SwitchingRules", ClassSwitchingRules.CODEC, false, true),
                    (i, s) -> i.SwitchingRules = s, i -> i.SwitchingRules,
                    (i, parent) -> i.SwitchingRules = parent.SwitchingRules)
            .add()
                .appendInherited(new KeyedCodec<>("RelearnExpPenalty", Codec.FLOAT, false, true),
                    (i, s) -> i.RelearnExpPenalty = s, i -> i.RelearnExpPenalty,
                    (i, parent) -> i.RelearnExpPenalty = parent.RelearnExpPenalty)
                .add()
            .appendInherited(new KeyedCodec<>("AbilityControlTypeOverride", new EnumCodec<>(AbilityControlType.class), true),
                    (i, s) -> i.AbilityControlTypeOverride = s, i -> i.AbilityControlTypeOverride,
                    (i, parent) -> i.AbilityControlTypeOverride = parent.AbilityControlTypeOverride)
            .add()
            .build();

    private static final class EntityStatRefCodec implements ValidatableCodec<String> {
        @Nonnull
        @SuppressWarnings("null")
        private AssetCodec<String, EntityStatType> getAssetCodec() {
            return AssetRegistry.getAssetStore(EntityStatType.class).getCodec();
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
            schema.setTitle("Entity Stat");
            schema.setHytaleAssetRef(EntityStatType.class.getSimpleName());
            return schema;
        }

        @Override
        public void validate(String value, ExtraInfo extraInfo) {
            ValidationResults results = extraInfo.getValidationResults();
            if (results != null) {
                EntityStatType.VALIDATOR_CACHE.getValidator().accept(value, results);
            }
        }

        @Override
        public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
            if (tested.add(this)) {
                // No default validation needed
            }
        }
    }

    private static final class ClassRefCodec implements ValidatableCodec<String> {
        @Nonnull
        @SuppressWarnings("null")
        private AssetCodec<String, ClassDefinition> getAssetCodec() {
            return AssetRegistry.getAssetStore(ClassDefinition.class).getCodec();
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
            schema.setTitle("Class");
            schema.setHytaleAssetRef(ClassDefinition.class.getSimpleName());
            return schema;
        }

        @Override
        public void validate(String value, ExtraInfo extraInfo) {
            com.hypixel.hytale.codec.validation.ValidationResults results = extraInfo.getValidationResults();
            if (results != null) {
                AssetRegistry.getAssetStore(ClassDefinition.class).validate(value, results, extraInfo);
            }
        }

        @Override
        public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
            if (tested.add(this)) {
                // No default validation needed
            }
        }
    }

    private static DefaultAssetMap<String, ClassDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    // === Basic Info ===
    private String id; // Display name
    private String DisplayName; // Display name
    private String Description; // Class description
    private String Icon; // Icon asset ID
    private boolean Enabled; // Can this class be learned?
    private boolean Visible; // Show in class selection UI?
    private List<String> requiredExpansionIds;
    // === Inheritance ===
    private String Parent; // Parent class to extend from (e.g., "warrior" for Paladin)

    // === Prerequisites ===
    private Object2IntMap<String> Prerequisites; // Required class levels (e.g., "warrior": 10)
    private List<String> RequiredClasses; // Must have learned these classes first
    private List<String> ExclusiveWith; // Cannot have these classes simultaneously

    // === Leveling Integration ===
    private String LevelSystemId; // Links to LevelProgressionSystem (e.g., "class_warrior")
    private boolean UsesCharacterLevel; // Use character level instead of separate class level?
    private boolean IsStartingClass; // Available from character creation?
    private boolean IsHeroClass; // Starts above the baseline when first learned
    private int HeroStartingLevel; // Starting level used for hero classes

    // === Resources ===
    private List<String> ResourceStats; // Priority list of resource stats (e.g., ["Mana", "Energy"])

    // === Stat Bonuses ===
    private StatModifiers BaseStatModifiers; // Stat bonuses when class is active
    private StatModifiers PerLevelModifiers; // Stat bonuses per class level
    // === Equipment Rules ===
    private EquipmentRestrictions equipmentRestrictions; // Equipment restrictions for this class
    // === Abilities ===
    private List<AbilityUnlock> AbilityUnlocks; // Abilities unlocked at various levels
    // === Level Milestones ===
    private List<LevelMilestone> LevelMilestones; // Special rewards at specific levels
    // === Talent Trees ===
    private List<TalentTree> TalentTrees; // Talent tree definitions
    // === Class Switching ===
    private ClassSwitchingRules SwitchingRules;
    // === Relearning Penalty ===
    private float RelearnExpPenalty; // % of total exp lost when relearning (0.0 to 1.0)
    // === Ability Controls ===
    private AbilityControlType AbilityControlTypeOverride; // Override global ability control type (null = use global)
    // === Constructors ===
    public ClassDefinition() {
        this.id = "";
        this.DisplayName = "";
        this.Description = "";
        this.Icon = null;
        this.Enabled = true;
        this.Visible = true;
        this.requiredExpansionIds = new ArrayList<>();
        this.Prerequisites = new Object2IntOpenHashMap<>();
        this.RequiredClasses = new ArrayList<>();
        this.ExclusiveWith = new ArrayList<>();
        this.LevelSystemId = "";
        this.UsesCharacterLevel = false;
        this.IsStartingClass = false;
        this.IsHeroClass = false;
        this.HeroStartingLevel = 1;
        this.ResourceStats = new ArrayList<>();
        this.BaseStatModifiers = new StatModifiers();
        this.PerLevelModifiers = new StatModifiers();
        this.equipmentRestrictions = new EquipmentRestrictions();
        this.AbilityUnlocks = new ArrayList<>();
        this.LevelMilestones = new ArrayList<>();
        this.TalentTrees = new ArrayList<>();
        this.SwitchingRules = new ClassSwitchingRules();
        this.RelearnExpPenalty = 0.0f;
        this.AbilityControlTypeOverride = null; // null = use global default
    }

    private static AssetStore<String, ClassDefinition, DefaultAssetMap<String, ClassDefinition>> ASSET_STORE;

    public static AssetStore<String, ClassDefinition, DefaultAssetMap<String, ClassDefinition>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(ClassDefinition.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, ClassDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = getAssetStore().getAssetMap();
        }

        return ASSET_MAP;
    }

    // === Getters and Setters ===

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

    public String getIconId() {
        return Icon;
    }

    public void setIconId(String Icon) {
        this.Icon = (Icon == null || Icon.isEmpty()) ? null : Icon;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public void setEnabled(boolean Enabled) {
        this.Enabled = Enabled;
    }

    public boolean isVisible() {
        return Visible;
    }

    public void setVisible(boolean Visible) {
        this.Visible = Visible;
    }

    public List<String> getRequiredExpansionIds() {
        return requiredExpansionIds == null ? new ArrayList<>() : requiredExpansionIds;
    }

    public Object2IntMap<String> getPrerequisites() {
        return Prerequisites;
    }

    public void setPrerequisites(Object2IntMap<String> Prerequisites) {
        this.Prerequisites = Prerequisites;
    }

    public List<String> getRequiredClasses() {
        return RequiredClasses;
    }

    public List<String> getExclusiveWith() {
        return ExclusiveWith;
    }

    public String getLevelSystemId() {
        return LevelSystemId;
    }

    public void setLevelSystemId(String LevelSystemId) {
        this.LevelSystemId = LevelSystemId;
    }

    public boolean usesCharacterLevel() {
        return UsesCharacterLevel;
    }

    public boolean isStartingClass() {
        return IsStartingClass;
    }

    public void setIsStartingClass(boolean IsStartingClass) {
        this.IsStartingClass = IsStartingClass;
    }

    public boolean isHeroClass() {
        return IsHeroClass;
    }

    public int getHeroStartingLevel() {
        return Math.max(1, HeroStartingLevel);
    }

    public int getInitialClassLevel() {
        return isHeroClass() ? getHeroStartingLevel() : 1;
    }

    public List<String> getResourceStats() {
        return ResourceStats;
    }

    public Map<String, List<String>> getTags() {
        Map<String, List<String>> result = new HashMap<>();
        if (data == null) {
            return result;
        }

        Map<String, String[]> rawTags = data.getRawTags();
        if (rawTags == null || rawTags.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, String[]> entry : rawTags.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (key == null || values == null || values.length == 0) {
                continue;
            }

            List<String> list = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    list.add(value);
                }
            }

            if (!list.isEmpty()) {
                result.put(key, list);
            }
        }

        return result;
    }

    public boolean hasTag(@Nonnull String tagKey, @Nonnull String tagValue) {
        if (data == null) {
            return false;
        }

        Map<String, String[]> rawTags = data.getRawTags();
        if (rawTags == null || rawTags.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, String[]> entry : rawTags.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (key == null || values == null || values.length == 0) {
                continue;
            }

            if (!key.equalsIgnoreCase(tagKey)) {
                continue;
            }

            for (String value : values) {
                if (value != null && value.equalsIgnoreCase(tagValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    public StatModifiers getBaseStatModifiers() {
        return BaseStatModifiers;
    }

    public void setBaseStatModifiers(StatModifiers BaseStatModifiers) {
        if (BaseStatModifiers != null) {
            this.BaseStatModifiers = BaseStatModifiers;
        }
        this.BaseStatModifiers = BaseStatModifiers;
    }

    public StatModifiers getPerLevelModifiers() {
        return PerLevelModifiers;
    }

    public void setPerLevelModifiers(StatModifiers PerLevelModifiers) {
        if (PerLevelModifiers != null) {

        }
        this.PerLevelModifiers = PerLevelModifiers;
    }

    public EquipmentRestrictions getEquipmentRestrictions() {
        return equipmentRestrictions;
    }

    public void setEquipmentRestrictions(EquipmentRestrictions equipmentRestrictions) {
        this.equipmentRestrictions = equipmentRestrictions;
    }

    public List<AbilityUnlock> getAbilityUnlocks() {
        return AbilityUnlocks;
    }

    public void setAbilityUnlocks(List<AbilityUnlock> AbilityUnlocks) {
        this.AbilityUnlocks = AbilityUnlocks;
    }

    /**
     * Get all ability ids defined for this class.
     */
    public java.util.Set<String> getAbilityIds() {
        java.util.Set<String> abilityIds = new java.util.HashSet<>();
        if (AbilityUnlocks != null) {
            for (AbilityUnlock unlock : AbilityUnlocks) {
                if (unlock != null && unlock.getAbilityId() != null && !unlock.getAbilityId().isEmpty()) {
                    abilityIds.add(unlock.getAbilityId());
                }
            }
        }
        return abilityIds;
    }

    public List<LevelMilestone> getLevelMilestones() {
        return LevelMilestones;
    }

    public void setLevelMilestones(List<LevelMilestone> LevelMilestones) {
        this.LevelMilestones = LevelMilestones;
    }

    @Nullable
    public AbilityControlType getAbilityControlTypeOverride() {
        return AbilityControlTypeOverride;
    }

    public void setAbilityControlTypeOverride(@Nullable AbilityControlType abilityControlTypeOverride) {
        this.AbilityControlTypeOverride = abilityControlTypeOverride;
    }

    /**
     * Get the effective ability control type for this class.
     * If override is set, uses that; otherwise uses global default from config.
     * 
     * @param configId The RpgModConfig ID to fetch global default from
     * @return The effective ability control type
     */
    @Nonnull
    public AbilityControlType getEffectiveAbilityControlType(String configId) {
        if (AbilityControlTypeOverride != null) {
            return AbilityControlTypeOverride;
        }
        
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset(configId);
        if (config != null) {
            return config.getAbilityControlType();
        }
        
        // Fallback to Hotbar if config not found
        return AbilityControlType.Hotbar;
    }

    public List<TalentTree> getTalentTrees() {
        return TalentTrees;
    }

    public void setTalentTrees(List<TalentTree> TalentTrees) {
        this.TalentTrees = TalentTrees;
    }

    public ClassSwitchingRules getSwitchingRules() {
        return SwitchingRules;
    }

    public void setSwitchingRules(ClassSwitchingRules SwitchingRules) {
        this.SwitchingRules = SwitchingRules;
    }

    public float getRelearnExpPenalty() {
        return RelearnExpPenalty;
    }

    public void setRelearnExpPenalty(float RelearnExpPenalty) {
        this.RelearnExpPenalty = RelearnExpPenalty;
    }

    /**
     * Get the unique ID for this class definition (for JsonAsset interface)
     */
    @Override
    public String getId() {
        return id;
    }

    // === Helper Methods ===

    /**
     * Get abilities that unlock up to a specific level.
     */
    public List<String> getAbilitiesAtLevel(int level) {
        List<String> Abilities = new ArrayList<>();
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (unlock.getUnlockLevel() <= level) {
                Abilities.add(unlock.getAbilityId());
            }
        }
        return Abilities;
    }

    /**
     * Get full AbilityUnlock objects up to a specific level (includes item rewards)
     */
    @Nonnull
    public List<AbilityUnlock> getAbilityUnlocksAtLevel(int level) {
        List<AbilityUnlock> unlocks = new ArrayList<>();
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (unlock.getUnlockLevel() <= level) {
                unlocks.add(unlock);
            }
        }
        return unlocks;
    }

    /**
     * Get AbilityUnlock objects that unlock exactly at a specific level.
     */
    @Nonnull
    public List<AbilityUnlock> getAbilityUnlocksExactLevel(int level) {
        List<AbilityUnlock> unlocks = new ArrayList<>();
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (unlock.getUnlockLevel() == level) {
                unlocks.add(unlock);
            }
        }
        return unlocks;
    }

    /**
     * Check if an ability is unlocked at a given level.
     */
    public boolean isAbilityUnlockedAtLevel(@Nonnull String abilityId, int level) {
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (abilityId.equals(unlock.getAbilityId()) && unlock.getUnlockLevel() <= level) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the configured unlock level for an ability. Returns -1 if not found.
     */
    public int getUnlockLevelForAbility(@Nonnull String abilityId) {
        int best = Integer.MAX_VALUE;
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (abilityId.equals(unlock.getAbilityId())) {
                best = Math.min(best, unlock.getUnlockLevel());
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    /**
     * Get the configured unlock entry for an ability. Returns null if not found.
     */
    @Nullable
    public AbilityUnlock getAbilityUnlock(@Nonnull String abilityId) {
        for (AbilityUnlock unlock : AbilityUnlocks) {
            if (abilityId.equals(unlock.getAbilityId())) {
                return unlock;
            }
        }
        return null;
    }

    /**
     * Get level milestone at a specific level (if any)
     */
    @Nullable
    public LevelMilestone getMilestoneAtLevel(int level) {
        for (LevelMilestone milestone : LevelMilestones) {
            if (milestone.getLevel() == level) {
                return milestone;
            }
        }
        return null;
    }

    // === Nested Classes ===

    /**
     * Stat modifiers (additive and multiplicative)
     */
    public static class StatModifiers {
        public static final BuilderCodec<StatModifiers> CODEC = BuilderCodec
                .builder(StatModifiers.class, StatModifiers::new)
                .append(
                        new KeyedCodec<>("AdditiveModifiers",
                                new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                        (i,
                                stringObject2DoubleMap) -> i.AdditiveModifiers = stringObject2DoubleMap,
                        i -> i.AdditiveModifiers)
                .addValidator(Validators.nonNull())
                .addValidator(Validators.nonEmptyMap())
                .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
                .documentation("Modifiers to apply to EntityStats.")
                .add()
                .append(
                        new KeyedCodec<>("MultiplicativeModifiers",
                                new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                        (i,
                                stringObject2DoubleMap) -> i.MultiplicativeModifiers = stringObject2DoubleMap,
                        i -> i.MultiplicativeModifiers)
                .addValidator(Validators.nonNull())
                .addValidator(Validators.nonEmptyMap())
                .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
                .documentation("Modifiers to apply to EntityStats.")
                .add()
                .build();

        private Object2FloatMap<String> AdditiveModifiers; // Flat bonuses (e.g., +10 Strength)
        private Object2FloatMap<String> MultiplicativeModifiers; // % bonuses (e.g., +10% Strength)

        public StatModifiers() {
            this.AdditiveModifiers = new Object2FloatOpenHashMap<>();
            this.MultiplicativeModifiers = new Object2FloatOpenHashMap<>();
        }

        public Object2FloatMap<String> getAdditiveModifiers() {
            return AdditiveModifiers;
        }

        public void setAdditiveModifiers(Object2FloatMap<String> additiveModifiers) {
            this.AdditiveModifiers = additiveModifiers;
        }

        public Object2FloatMap<String> getMultiplicativeModifiers() {
            return MultiplicativeModifiers;
        }

        public void setMultiplicativeModifiers(Object2FloatMap<String> multiplicativeModifiers) {
            this.MultiplicativeModifiers = multiplicativeModifiers;
        }

        public boolean isEmpty() {
            return (AdditiveModifiers == null || AdditiveModifiers.isEmpty())
                    && (MultiplicativeModifiers == null || MultiplicativeModifiers.isEmpty());
        }

    }

    /**
     * Equipment restrictions
     */
    public static class EquipmentRestrictions {
        public static final BuilderCodec<EquipmentRestrictions> CODEC = BuilderCodec
                .builder(EquipmentRestrictions.class, EquipmentRestrictions::new)
                .append(new KeyedCodec<>("RestrictionMode", new EnumCodec<>(EquipmentRestrictions.RestrictMode.class),
                        false, true),
                        (i, s) -> i.restrictionMode = s, i -> i.restrictionMode)
                .add()
                .append(new KeyedCodec<>("AllowedWeaponTypes", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.allowedWeaponTypes = s, i -> i.allowedWeaponTypes)
                .add()
                .append(new KeyedCodec<>("AllowedArmorTypes", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.allowedArmorTypes = s, i -> i.allowedArmorTypes)
                .add()
                .append(new KeyedCodec<>("RequiredItems", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.requiredItems = s, i -> i.requiredItems)
                .add()
                .build();

        private RestrictMode restrictionMode; // Hard, Soft, None
        private List<String> allowedWeaponTypes; // Weapon types this class can use
        private List<String> allowedArmorTypes; // Armor types this class can use
        private List<String> requiredItems; // Items that must be equipped (e.g., woodcutting axe)

        public EquipmentRestrictions() {
            this.restrictionMode = RestrictMode.None;
            this.allowedWeaponTypes = new ArrayList<>();
            this.allowedArmorTypes = new ArrayList<>();
            this.requiredItems = new ArrayList<>();
        }

        public RestrictMode getRestrictionMode() {
            return restrictionMode;
        }

        public void setRestrictionMode(RestrictMode restrictionMode) {
            this.restrictionMode = restrictionMode;
        }

        public List<String> getAllowedWeaponTypes() {
            return allowedWeaponTypes;
        }

        public void setAllowedWeaponTypes(List<String> allowedWeaponTypes) {
            this.allowedWeaponTypes = allowedWeaponTypes;
        }

        public List<String> getAllowedArmorTypes() {
            return allowedArmorTypes;
        }

        public void setAllowedArmorTypes(List<String> allowedArmorTypes) {
            this.allowedArmorTypes = allowedArmorTypes;
        }

        public List<String> getRequiredItems() {
            return requiredItems;
        }

        public void setRequiredItems(List<String> requiredItems) {
            this.requiredItems = requiredItems;
        }

        public enum RestrictMode {
            None, // No restrictions
            Soft, // Can equip but lose class bonuses
            Hard // Cannot equip at all
        }
    }

    /**
     * Ability unlock configuration
     */
    public static class AbilityUnlock {
        public static final BuilderCodec<AbilityUnlock> CODEC = BuilderCodec
                .builder(AbilityUnlock.class, AbilityUnlock::new)
                .append(new KeyedCodec<>("AbilityId", ABILITY_REF_CODEC, false, true),
                        (i, s) -> i.abilityId = s, i -> i.abilityId)
                .add()
                .append(new KeyedCodec<>("UnlockLevel", Codec.INTEGER, false, true), (i, s) -> i.unlockLevel = s,
                        i -> i.unlockLevel)
                .add()
                .append(new KeyedCodec<>("MaxRank", Codec.INTEGER, false, true), (i, s) -> i.maxRank = s,
                        i -> i.maxRank)
                .add()
                .append(new KeyedCodec<>("ItemRewards", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.itemRewards = s, i -> i.itemRewards)
                .add()
                .append(new KeyedCodec<>("LearnCosts", new ArrayCodec<>(AbilityLearnCost.CODEC, AbilityLearnCost[]::new), false, true),
                    (i, s) -> i.learnCosts = s, i -> i.learnCosts)
                .add()
                .build();

        private String abilityId;
        private int unlockLevel; // Level at which ability unlocks
        private int maxRank; // Maximum rank for this ability (0 = no ranks)
        private List<String> itemRewards; // Items rewarded when this ability unlocks
            private AbilityLearnCost[] learnCosts; // Optional trainer-style costs to learn this ability

        public AbilityUnlock() {
            this.abilityId = "";
            this.unlockLevel = 1;
            this.maxRank = 0;
            this.itemRewards = new ArrayList<>();
                this.learnCosts = new AbilityLearnCost[0];
        }

        public String getAbilityId() {
            return abilityId;
        }

        public void setAbilityId(String abilityId) {
            this.abilityId = abilityId;
        }

        public int getUnlockLevel() {
            return unlockLevel;
        }

        public void setUnlockLevel(int unlockLevel) {
            this.unlockLevel = unlockLevel;
        }

        public int getMaxRank() {
            return maxRank;
        }

        public void setMaxRank(int maxRank) {
            this.maxRank = maxRank;
        }

        public List<String> getItemRewards() {
            return itemRewards;
        }

        public void setItemRewards(List<String> itemRewards) {
            this.itemRewards = itemRewards;
        }

        public List<AbilityLearnCost> getLearnCosts() {
            return learnCosts == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(learnCosts));
        }

        public void setLearnCosts(List<AbilityLearnCost> learnCosts) {
            this.learnCosts = learnCosts == null ? new AbilityLearnCost[0] : learnCosts.toArray(AbilityLearnCost[]::new);
        }

        public boolean hasLearnCosts() {
            return learnCosts != null && learnCosts.length > 0;
        }
    }

    public static class AbilityLearnCost {
        public static final BuilderCodec<AbilityLearnCost> CODEC = BuilderCodec
                .builder(AbilityLearnCost.class, AbilityLearnCost::new)
                .append(new KeyedCodec<>("CurrencyId", Codec.STRING, false, true),
                        (i, s) -> i.currencyId = s, i -> i.currencyId)
                .add()
                .append(new KeyedCodec<>("Amount", Codec.LONG, false, true),
                        (i, s) -> i.amount = s, i -> i.amount)
                .add()
                .append(new KeyedCodec<>("CurrencyScope", new EnumCodec<>(CurrencyScope.class), false, true),
                        (i, s) -> i.currencyScope = s, i -> i.currencyScope)
                .add()
                .build();

        private String currencyId;
        private long amount;
        private CurrencyScope currencyScope;

        public AbilityLearnCost() {
            this.currencyId = "";
            this.amount = 0L;
            this.currencyScope = CurrencyScope.Character;
        }

        public String getCurrencyId() {
            return currencyId == null ? "" : currencyId;
        }

        public void setCurrencyId(String currencyId) {
            this.currencyId = currencyId;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        @Nonnull
        public CurrencyScope getCurrencyScope() {
            return currencyScope == null ? CurrencyScope.Character : currencyScope;
        }

        public void setCurrencyScope(@Nullable CurrencyScope currencyScope) {
            this.currencyScope = currencyScope == null ? CurrencyScope.Character : currencyScope;
        }

        public boolean isFree() {
            return amount <= 0L || getCurrencyId().isEmpty();
        }

        @Nonnull
        public CurrencyAmountDefinition toCurrencyAmountDefinition() {
            return new CurrencyAmountDefinition(getCurrencyId(), amount);
        }
    }

    /**
     * Level milestone rewards (for non-ability unlocks)
     */
    public static class LevelMilestone {
        public static final BuilderCodec<LevelMilestone> CODEC = BuilderCodec
                .builder(LevelMilestone.class, LevelMilestone::new)
                .append(new KeyedCodec<>("Level", Codec.INTEGER, false, true), (i, s) -> i.level = s, i -> i.level)
                .add()
                .append(new KeyedCodec<>("SkillPoints", Codec.INTEGER, false, true), (i, s) -> i.skillPoints = s,
                        i -> i.skillPoints)
                .add()
                .append(new KeyedCodec<>("ItemRewards", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.itemRewards = s, i -> i.itemRewards)
                .add()
                .append(new KeyedCodec<>("InteractionChain", Codec.STRING, false, true),
                        (i, s) -> i.interactionChain = s, i -> i.interactionChain)
                .add()
                .build();

        private int level;
        private int skillPoints; // Bonus skill points at this level
        private List<String> itemRewards; // Items rewarded at this level
        private String interactionChain; // Special interaction/ceremony at this level

        public LevelMilestone() {
            this.level = 1;
            this.skillPoints = 0;
            this.itemRewards = new ArrayList<>();
            this.interactionChain = null;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getSkillPoints() {
            return skillPoints;
        }

        public void setSkillPoints(int skillPoints) {
            this.skillPoints = skillPoints;
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
    }

    /**
     * Class switching rules
     */
    public static class ClassSwitchingRules {
        public static final BuilderCodec<ClassSwitchingRules> CODEC = BuilderCodec
                .builder(ClassSwitchingRules.class, ClassSwitchingRules::new)
                .append(new KeyedCodec<>("CanSwitch", Codec.BOOLEAN, false, true), (i, s) -> i.canSwitch = s,
                        i -> i.canSwitch)
                .add()
                .append(new KeyedCodec<>("CanSwitchInCombat", Codec.BOOLEAN, false, true),
                        (i, s) -> i.canSwitchInCombat = s, i -> i.canSwitchInCombat)
                .add()
                .append(new KeyedCodec<>("SwitchCooldown", Codec.FLOAT, false, true), (i, s) -> i.switchCooldown = s,
                        i -> i.switchCooldown)
                .add()
                .build();

        private boolean canSwitch; // Can player switch to this class if already active?
        private boolean canSwitchInCombat; // Can switch during combat?
        private float switchCooldown; // Cooldown in seconds between switches

        public ClassSwitchingRules() {
            this.canSwitch = true;
            this.canSwitchInCombat = false;
            this.switchCooldown = 0f;
        }

        public boolean canSwitch() {
            return canSwitch;
        }

        public void setCanSwitch(boolean canSwitch) {
            this.canSwitch = canSwitch;
        }

        public boolean canSwitchInCombat() {
            return canSwitchInCombat;
        }

        public void setCanSwitchInCombat(boolean canSwitchInCombat) {
            this.canSwitchInCombat = canSwitchInCombat;
        }

        public float getSwitchCooldown() {
            return switchCooldown;
        }

        public void setSwitchCooldown(float switchCooldown) {
            this.switchCooldown = switchCooldown;
        }
    }

    /**
     * Talent tree definition for a class
     */
    public static class TalentTree {
        public static final BuilderCodec<TalentTree> CODEC = BuilderCodec.builder(TalentTree.class, TalentTree::new)
                .append(new KeyedCodec<>("TreeId", Codec.STRING, false, true), (i, s) -> i.treeId = s, i -> i.treeId)
                .add()
                .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true), (i, s) -> i.displayName = s,
                        i -> i.displayName)
                .add()
                .append(new KeyedCodec<>("Description", Codec.STRING, false, true), (i, s) -> i.description = s,
                        i -> i.description)
                .add()
                .append(new KeyedCodec<>("Icon", Codec.STRING, false, true), (i, s) -> i.iconId = s, i -> i.iconId)
                .add()
                .append(new KeyedCodec<>("MaxPoints", Codec.INTEGER, false, true), (i, s) -> i.maxPoints = s,
                        i -> i.maxPoints)
                .add()
            .append(new KeyedCodec<>("PrerequisiteRankMode", new EnumCodec<>(TalentPrerequisiteRankMode.class), false, true),
                (i, s) -> i.prerequisiteRankMode = s, i -> i.prerequisiteRankMode)
            .add()
                .append(new KeyedCodec<>("Nodes", new ArrayCodec<>(TalentNode.CODEC, TalentNode[]::new), false, true),
                        (i, s) -> i.nodes = s, i -> i.nodes)
                .add()
                .build();

        private String treeId;
        private String displayName;
        private String description;
        private String iconId;
        private int maxPoints;
        private TalentPrerequisiteRankMode prerequisiteRankMode;
        private TalentNode[] nodes;

        public TalentTree() {
            this.treeId = "";
            this.displayName = "";
            this.description = "";
            this.iconId = "";
            this.maxPoints = 0;
            this.prerequisiteRankMode = TalentPrerequisiteRankMode.OnePoint;
            this.nodes = new TalentNode[0];
        }

        public String getTreeId() {
            return treeId;
        }

        public void setTreeId(String treeId) {
            this.treeId = treeId;
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

        public int getMaxPoints() {
            return maxPoints;
        }

        public void setMaxPoints(int maxPoints) {
            this.maxPoints = maxPoints;
        }

        @Nonnull
        public TalentPrerequisiteRankMode getPrerequisiteRankMode() {
            return prerequisiteRankMode == null ? TalentPrerequisiteRankMode.OnePoint : prerequisiteRankMode;
        }

        public void setPrerequisiteRankMode(@Nullable TalentPrerequisiteRankMode prerequisiteRankMode) {
            this.prerequisiteRankMode = prerequisiteRankMode == null
                    ? TalentPrerequisiteRankMode.OnePoint
                    : prerequisiteRankMode;
        }

        public TalentNode[] getNodes() {
            return nodes;
        }

        public void setNodes(TalentNode[] nodes) {
            this.nodes = nodes;
        }
    }

    public enum TalentPrerequisiteRankMode {
        OnePoint,
        FullRank
    }

    /**
     * Talent tree node definition
     */
    public static class TalentNode {
        public static final BuilderCodec<TalentNode> CODEC = BuilderCodec.builder(TalentNode.class, TalentNode::new)
                .append(new KeyedCodec<>("NodeId", Codec.STRING, false, true), (i, s) -> i.nodeId = s, i -> i.nodeId)
                .add()
                .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true), (i, s) -> i.displayName = s,
                        i -> i.displayName)
                .add()
                .append(new KeyedCodec<>("Description", Codec.STRING, false, true), (i, s) -> i.description = s,
                        i -> i.description)
                .add()
                .append(new KeyedCodec<>("Icon", Codec.STRING, false, true), (i, s) -> i.iconId = s, i -> i.iconId)
                .add()
                .append(new KeyedCodec<>("MaxRank", Codec.INTEGER, false, true), (i, s) -> i.maxRank = s,
                        i -> i.maxRank)
                .add()
                .append(new KeyedCodec<>("RequiredLevel", Codec.INTEGER, false, true), (i, s) -> i.requiredLevel = s,
                        i -> i.requiredLevel)
                .add()
                .append(new KeyedCodec<>("RequiredNodes", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.requiredNodes = s, i -> i.requiredNodes)
                .add()
                .append(new KeyedCodec<>("GrantsAbilityId", ABILITY_REF_CODEC, false, true),
                        (i, s) -> i.grantsAbilityId = s, i -> i.grantsAbilityId)
                .add()
                .append(new KeyedCodec<>("StatModifiers", StatModifiers.CODEC, false, true),
                        (i, s) -> i.statModifiers = s, i -> i.statModifiers)
                .add()
                .append(new KeyedCodec<>("PositionX", Codec.INTEGER, false, true), (i, s) -> i.positionX = s,
                        i -> i.positionX)
                .addValidator(TALENT_NODE_X_VALIDATOR)
                .add()
                .append(new KeyedCodec<>("PositionY", Codec.INTEGER, false, true), (i, s) -> i.positionY = s,
                        i -> i.positionY)
                .add()
                .build();

        private String nodeId;
        private String displayName;
        private String description;
        private String iconId;
        private int maxRank;
        private int requiredLevel;
        private List<String> requiredNodes;
        private String grantsAbilityId;
        private StatModifiers statModifiers; // Stat bonuses granted by this node (stacks per rank)
        private int positionX;
        private int positionY;

        public TalentNode() {
            this.nodeId = "";
            this.displayName = "";
            this.description = "";
            this.iconId = "";
            this.maxRank = 1;
            this.requiredLevel = 1;
            this.requiredNodes = new ArrayList<>();
            this.grantsAbilityId = "";
            this.statModifiers = null;
            this.positionX = 0;
            this.positionY = 0;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
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

        public int getMaxRank() {
            return maxRank;
        }

        public void setMaxRank(int maxRank) {
            this.maxRank = maxRank;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public void setRequiredLevel(int requiredLevel) {
            this.requiredLevel = requiredLevel;
        }

        public List<String> getRequiredNodes() {
            return requiredNodes;
        }

        public void setRequiredNodes(List<String> requiredNodes) {
            this.requiredNodes = requiredNodes;
        }

        public String getGrantsAbilityId() {
            return grantsAbilityId;
        }

        public void setGrantsAbilityId(String grantsAbilityId) {
            this.grantsAbilityId = grantsAbilityId;
        }

        @javax.annotation.Nullable
        public StatModifiers getStatModifiers() {
            return statModifiers;
        }

        public void setStatModifiers(@javax.annotation.Nullable StatModifiers statModifiers) {
            this.statModifiers = statModifiers;
        }

        public boolean hasStatModifiers() {
            return statModifiers != null && !statModifiers.isEmpty();
        }

        public int getPositionX() {
            return positionX;
        }

        public void setPositionX(int positionX) {
            this.positionX = positionX;
        }

        public int getPositionY() {
            return positionY;
        }

        public void setPositionY(int positionY) {
            this.positionY = positionY;
        }
    }
}
