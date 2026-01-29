package org.pixelbays.rpg.global.drop;

import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;

/**
 * Custom ItemDropList for EXP drops as separate asset type.
 * Register with AssetRegistry.
 *
 * Example JSON:
 * {
 *   "ExpMin": 5,
 *   "ExpMax": 15,
 *   "SystemId": "character_level"
 * }
 */
@SuppressWarnings({ "deprecation", "all", "null" })
public class ExpItemDropContainer extends ItemDropContainer {

    public static final String EXP_MIN_KEY = "ExpMin";
    public static final String EXP_MAX_KEY = "ExpMax";
    public static final String EXP_SYSTEM_ID_KEY = "ExpSystemId";

    public static final BuilderCodec<ExpItemDropContainer> CODEC = BuilderCodec
            .builder(ExpItemDropContainer.class, ExpItemDropContainer::new, ItemDropContainer.DEFAULT_CODEC)
            .appendInherited(new KeyedCodec<>("ExpMin", Codec.INTEGER),
                    (container, value) -> container.expMin = value,
                    container -> container.expMin,
                    (container, parent) -> container.expMin = parent.expMin)
            .add()
            .appendInherited(new KeyedCodec<>("ExpMax", Codec.INTEGER),
                    (container, value) -> container.expMax = value,
                    container -> container.expMax,
                    (container, parent) -> container.expMax = parent.expMax)
            .add()
            .appendInherited(new KeyedCodec<>("SystemId", Codec.STRING, false, true),
                    (container, value) -> container.systemId = value,
                    container -> container.systemId,
                    (container, parent) -> container.systemId = parent.systemId)
            .add()
            .build();

    protected String id;
    private int expMin = 1;
    private int expMax = 1;
    private String systemId = "";

    public ExpItemDropContainer(double weight, int expMin, int expMax, @Nullable String systemId) {
        super(weight);
        this.expMin = expMin;
        this.expMax = expMax;
        this.systemId = systemId == null ? "" : systemId;
    }

    protected ExpItemDropContainer() {
        super();
    }

    public String getId() {
        return id;
    }

    public int getExpMin() {
        return expMin;
    }

    public int getExpMax() {
        return expMax;
    }

    @Nullable
    public String getSystemId() {
        return systemId != null && !systemId.isEmpty() ? systemId : null;
    }

    @Nonnull
    public ItemDrop createDrop() {
        int min = Math.max(0, expMin);
        int max = Math.max(min, expMax);
        if (max <= 0) {
            min = 0;
            max = 0;
        }

        BsonDocument metadata = new BsonDocument();
        if (systemId != null && !systemId.isEmpty()) {
            metadata.put(EXP_SYSTEM_ID_KEY, new BsonString(systemId));
        }
        metadata.put(EXP_MIN_KEY, new BsonInt32(min));
        metadata.put(EXP_MAX_KEY, new BsonInt32(max));

        return new ItemDrop(null, metadata, min, max);
    }

    public static boolean isExpMetadata(@Nullable BsonDocument metadata) {
        if (metadata == null) {
            return false;
        }

        return metadata.containsKey(EXP_MIN_KEY) && metadata.containsKey(EXP_MAX_KEY);
    }

    @Nullable
    public static String getSystemIdFromMetadata(@Nullable BsonDocument metadata) {
        if (metadata == null || !metadata.containsKey(EXP_SYSTEM_ID_KEY)) {
            return null;
        }

        try {
            return metadata.getString(EXP_SYSTEM_ID_KEY).getValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    protected void populateDrops(List<ItemDrop> drops, DoubleSupplier chanceProvider, Set<String> droplistReferences) {
        drops.add(createDrop());
    }

    @Override
    public List<ItemDrop> getAllDrops(List<ItemDrop> drops) {
        drops.add(createDrop());
        return drops;
    }

    @Nonnull
    @Override
    public String toString() {
        return "ExpItemDropContainer{id='" + id + "', expMin=" + expMin + ", expMax=" + expMax + ", systemId='" + systemId + "'}";
    }
}
