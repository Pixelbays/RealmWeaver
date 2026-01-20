package org.pixelbays.rpg.config;

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
import com.hypixel.hytale.codec.validation.ValidatorCache;

/**
 * Configuration for an exp curve definition loaded from asset pack.
 * Example file: Server/Entity/ExpCurves/Curve_Linear.json
 */
@SuppressWarnings({"deprecation", "FieldHidesSuperclassField"})
public class ExpCurveDefinition extends LevelSystemConfig.ExpCurveConfig
        implements JsonAssetWithMap<String, DefaultAssetMap<String, ExpCurveDefinition>> {

    public static final AssetBuilderCodec<String, ExpCurveDefinition> EXP_CURVE_CODEC = AssetBuilderCodec.builder(
            ExpCurveDefinition.class,
            ExpCurveDefinition::new,
            Codec.STRING,
            (t, k) -> t.curveId = k,
            t -> t.curveId,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(new KeyedCodec<>("curveId", Codec.STRING, false, true), (i, s) -> i.curveId = s, i -> i.curveId)
        .add()
        .append(new KeyedCodec<>("name", Codec.STRING, false, true), (i, s) -> i.name = s, i -> i.name)
        .add()
        .append(new KeyedCodec<>("description", Codec.STRING, false, true), (i, s) -> i.description = s, i -> i.description)
        .add()
        .append(new KeyedCodec<>("type", Codec.STRING, false, true), (i, s) -> i.setType(s), i -> i.getType())
        .add()
        .append(new KeyedCodec<>("baseExp", Codec.FLOAT, false, true), (i, s) -> i.setBaseExp(s), i -> i.getBaseExp())
        .add()
        .append(new KeyedCodec<>("growthRate", Codec.FLOAT, false, true), (i, s) -> i.setGrowthRate(s), i -> i.getGrowthRate())
        .add()
        .append(new KeyedCodec<>("exponent", Codec.FLOAT, false, true), (i, s) -> i.setExponent(s), i -> i.getExponent())
        .add()
        .append(new KeyedCodec<>("customFormula", Codec.STRING, false, true), (i, s) -> i.setCustomFormula(s), i -> i.getCustomFormula())
        .add()
        .build();

    public static final ContainedAssetCodec<String, ExpCurveDefinition, ?> CHILD_ASSET_CODEC =
        new ContainedAssetCodec<>(ExpCurveDefinition.class, EXP_CURVE_CODEC);

    public static final ValidatorCache<String> VALIDATOR_CACHE =
        new ValidatorCache<>(new AssetKeyValidator<>(ExpCurveDefinition::getAssetStore));

    private static DefaultAssetMap<String, ExpCurveDefinition> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String curveId;
    private String name;
    private String description;

    public ExpCurveDefinition() {
        super();
        this.curveId = "";
        this.name = "";
        this.description = "";
    }

    public static DefaultAssetMap<String, ExpCurveDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            ASSET_MAP = (DefaultAssetMap<String, ExpCurveDefinition>) AssetRegistry.getAssetStore(ExpCurveDefinition.class).getAssetMap();
        }

        return ASSET_MAP;
    }

    public static AssetStore<String, ExpCurveDefinition, ?> getAssetStore() {
        return (AssetStore<String, ExpCurveDefinition, ?>) AssetRegistry.getAssetStore(ExpCurveDefinition.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return this.curveId;
    }

    public String getCurveId() {
        return curveId;
    }

    public void setCurveId(String curveId) {
        this.curveId = curveId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
