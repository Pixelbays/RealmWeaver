package org.pixelbays.rpg.expansion;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

@SuppressWarnings({ "deprecation", "null" })
public class ExpansionUnlockData implements JsonAssetWithMap<String, DefaultAssetMap<String, ExpansionUnlockData>> {

    public static final AssetBuilderCodec<String, ExpansionUnlockData> CODEC = AssetBuilderCodec.builder(
            ExpansionUnlockData.class,
            ExpansionUnlockData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("AccountId", Codec.STRING, false, true),
                    (i, s) -> i.accountId = s, i -> i.accountId)
            .add()
            .append(new KeyedCodec<>("UnlockedExpansions",
                    new MapCodec<>(Codec.LONG, LinkedHashMap::new, false), false, true),
                    (i, s) -> i.unlockedExpansions = s, i -> i.unlockedExpansions)
            .add()
            .append(new KeyedCodec<>("CreatedAt", Codec.LONG, false, true),
                    (i, s) -> i.createdAt = s, i -> i.createdAt)
            .add()
            .append(new KeyedCodec<>("UpdatedAt", Codec.LONG, false, true),
                    (i, s) -> i.updatedAt = s, i -> i.updatedAt)
            .add()
            .build();

    private static DefaultAssetMap<String, ExpansionUnlockData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private String accountId;
    private Map<String, Long> unlockedExpansions;
    private long createdAt;
    private long updatedAt;

    public ExpansionUnlockData() {
        this.id = "";
        this.accountId = "";
        this.unlockedExpansions = new LinkedHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public static DefaultAssetMap<String, ExpansionUnlockData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(ExpansionUnlockData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, ExpansionUnlockData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getAccountId() {
        return accountId == null ? "" : accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId == null ? "" : accountId;
    }

    public Map<String, Long> getUnlockedExpansions() {
        if (unlockedExpansions == null) {
            unlockedExpansions = new LinkedHashMap<>();
        }
        return unlockedExpansions;
    }

    public boolean hasUnlocked(String expansionId) {
        return expansionId != null && !expansionId.isBlank() && getUnlockedExpansions().containsKey(expansionId);
    }

    public long getUnlockedAt(String expansionId) {
        if (expansionId == null || expansionId.isBlank()) {
            return 0L;
        }
        return getUnlockedExpansions().getOrDefault(expansionId, 0L);
    }

    public void unlock(String expansionId, long unlockedAt) {
        if (expansionId == null || expansionId.isBlank()) {
            return;
        }
        getUnlockedExpansions().put(expansionId, unlockedAt);
        touch(unlockedAt);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch(long timestamp) {
        if (createdAt <= 0L) {
            createdAt = timestamp;
        }
        updatedAt = timestamp;
    }
}
