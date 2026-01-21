package org.pixelbays.rpg.leveling.config;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.builder.codec.ExpCurveRefCodec;
import org.pixelbays.rpg.global.config.validator.ExpCurveRefValidator;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

/**
 * Configuration for an exp curve definition loaded from asset pack.
 * Example file: Server/Entity/ExpCurves/Curve_Linear.json
 */
@SuppressWarnings({ "deprecation", "FieldHidesSuperclassField" })
public class ExpCurveDefinition
        implements JsonAssetWithMap<String, DefaultAssetMap<String, ExpCurveDefinition>> {

    public static final com.hypixel.hytale.codec.validation.ValidatorCache<String> VALIDATOR_CACHE =
            new com.hypixel.hytale.codec.validation.ValidatorCache<>(new ExpCurveRefValidator());

    private static final Codec<String> EXP_CURVE_REF_CODEC = new ExpCurveRefCodec();

    public static final AssetBuilderCodec<String, ExpCurveDefinition> EXP_CURVE_CODEC = AssetBuilderCodec.builder(
            ExpCurveDefinition.class,
            ExpCurveDefinition::new,
            Codec.STRING,
            (t, k) -> t.CurveId = k,
            t -> t.CurveId,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .appendInherited(new KeyedCodec<>("Name", Codec.STRING, false, true), (i, s) -> i.Name = s, i -> i.Name,
                    (i, parent) -> i.Name = parent.Name)
            .add()
            .appendInherited(new KeyedCodec<>("Description", Codec.STRING, false, true), (i, s) -> i.Description = s,
                    i -> i.Description,
                    (i, parent) -> i.Description = parent.Description)
            .add()
                    .appendInherited(new KeyedCodec<>("Parent", EXP_CURVE_REF_CODEC, false, true),
                    (i, s) -> i.Parent = s, i -> i.Parent,
                    (i, parent) -> i.Parent = parent.Parent)
            .addValidator(ExpCurveDefinition.VALIDATOR_CACHE.getValidator())
            .add()
            .appendInherited(
                    new KeyedCodec<>("Type", new com.hypixel.hytale.codec.codecs.EnumCodec<>(ExpCurveType.class), false,
                            true),
                    (i, s) -> i.setTypeEnum(s),
                    i -> i.getTypeEnum(),
                    (i, parent) -> i.setTypeEnum(parent.getTypeEnum()))
            .add()
            .appendInherited(new KeyedCodec<>("BaseExp", Codec.FLOAT, false, true), (i, s) -> i.setBaseExp(s),
                    i -> i.getBaseExp(),
                    (i, parent) -> i.setBaseExp(parent.getBaseExp()))
            .add()
            .appendInherited(new KeyedCodec<>("GrowthRate", Codec.FLOAT, false, true), (i, s) -> i.setGrowthRate(s),
                    i -> i.getGrowthRate(),
                    (i, parent) -> i.setGrowthRate(parent.getGrowthRate()))
            .add()
            .appendInherited(new KeyedCodec<>("Exponent", Codec.FLOAT, false, true), (i, s) -> i.setExponent(s),
                    i -> i.getExponent(),
                    (i, parent) -> i.setExponent(parent.getExponent()))
            .add()
            .appendInherited(new KeyedCodec<>("CustomFormula", Codec.STRING, false, true),
                    (i, s) -> i.setCustomFormula(s), i -> i.getCustomFormula(),
                    (i, parent) -> i.setCustomFormula(parent.getCustomFormula()))
            .add()
            .build();

    public static final ContainedAssetCodec<String, ExpCurveDefinition, ?> CHILD_ASSET_CODEC = new ContainedAssetCodec<>(
            ExpCurveDefinition.class, EXP_CURVE_CODEC);

    private static DefaultAssetMap<String, ExpCurveDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String CurveId;
    private String Name;
    private String Description;
    private String Parent;
    private String Type;
    private float BaseExp;
    private float GrowthRate;
    private float Exponent;
    private String CustomFormula;

    public ExpCurveDefinition() {
        this.CurveId = "";
        this.Name = "";
        this.Description = "";
        this.Parent = "";
        this.Type = ExpCurveType.Linear.toConfigValue();
        this.BaseExp = 100f;
        this.GrowthRate = 1.1f;
        this.Exponent = 2.0f;
        this.CustomFormula = null;
    }


    public static DefaultAssetMap<String, ExpCurveDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, ExpCurveDefinition>) AssetRegistry
                    .getAssetStore(ExpCurveDefinition.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    public static AssetStore<String, ExpCurveDefinition, ?> getAssetStore() {
        return (AssetStore<String, ExpCurveDefinition, ?>) AssetRegistry.getAssetStore(ExpCurveDefinition.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return this.CurveId == null ? "" : this.CurveId;
    }

    public String getCurveId() {
        return CurveId;
    }

    public void setCurveId(String curveId) {
        this.CurveId = curveId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
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

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        this.Type = type;
    }

    public float getBaseExp() {
        return BaseExp;
    }

    public void setBaseExp(float baseExp) {
        this.BaseExp = baseExp;
    }

    public float getGrowthRate() {
        return GrowthRate;
    }

    public void setGrowthRate(float growthRate) {
        this.GrowthRate = growthRate;
    }

    public float getExponent() {
        return Exponent;
    }

    public void setExponent(float exponent) {
        this.Exponent = exponent;
    }

    public String getCustomFormula() {
        return CustomFormula;
    }

    public void setCustomFormula(String customFormula) {
        this.CustomFormula = customFormula;
    }

    public float calculate(int level) {
        ExpCurveType type = getTypeEnum();
        if (type == ExpCurveType.Linear) {
            return BaseExp + (level * GrowthRate);
        }
        if (type == ExpCurveType.Exponential) {
            return BaseExp * (float) Math.pow(level, Exponent);
        }
        if (type == ExpCurveType.Custom) {
            return BaseExp * level;
        }
        return 100f * level;
    }

    public ExpCurveType getTypeEnum() {
        return ExpCurveType.fromString(getType());
    }

    public void setTypeEnum(ExpCurveType type) {
        setType(type == null ? null : type.toConfigValue());
    }

    public enum ExpCurveType {
        Linear("linear"),
        Exponential("exponential"),
        Custom("custom"),
        Table("table");

        private final String configValue;

        ExpCurveType(String configValue) {
            this.configValue = configValue;
        }

        public String toConfigValue() {
            return configValue;
        }

        public static ExpCurveType fromString(String value) {
            if (value == null || value.isEmpty()) {
                return Linear;
            }
            for (ExpCurveType type : values()) {
                if (type.configValue.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return Linear;
        }
    }
}