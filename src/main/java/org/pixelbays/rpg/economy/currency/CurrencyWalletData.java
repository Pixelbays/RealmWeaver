package org.pixelbays.rpg.economy.currency;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

@SuppressWarnings({ "deprecation", "null" })
public class CurrencyWalletData implements JsonAssetWithMap<String, DefaultAssetMap<String, CurrencyWalletData>> {

    public static final AssetBuilderCodec<String, CurrencyWalletData> CODEC = AssetBuilderCodec.builder(
            CurrencyWalletData.class,
            CurrencyWalletData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("Scope", new EnumCodec<>(CurrencyScope.class), false, true),
                    (i, s) -> i.scope = s, i -> i.scope)
            .add()
            .append(new KeyedCodec<>("OwnerId", Codec.STRING, false, true),
                    (i, s) -> i.ownerId = s, i -> i.ownerId)
            .add()
            .append(new KeyedCodec<>("Balances", new MapCodec<>(Codec.LONG, LinkedHashMap::new, false), false, true),
                    (i, s) -> i.balances = s, i -> i.balances)
            .add()
            .append(new KeyedCodec<>("CreatedAt", Codec.LONG, false, true),
                    (i, s) -> i.createdAt = s, i -> i.createdAt)
            .add()
            .append(new KeyedCodec<>("UpdatedAt", Codec.LONG, false, true),
                    (i, s) -> i.updatedAt = s, i -> i.updatedAt)
            .add()
            .build();

    private static DefaultAssetMap<String, CurrencyWalletData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private CurrencyScope scope;
    private String ownerId;
    private Map<String, Long> balances;
    private long createdAt;
    private long updatedAt;

    public CurrencyWalletData() {
        this.id = "";
        this.scope = CurrencyScope.Character;
        this.ownerId = "";
        this.balances = new LinkedHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public static DefaultAssetMap<String, CurrencyWalletData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(CurrencyWalletData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, CurrencyWalletData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public CurrencyWallet toWallet() {
        return new CurrencyWallet(
                id,
                scope == null ? CurrencyScope.Character : scope,
                ownerId == null ? "" : ownerId,
                balances == null ? Map.of() : balances,
                createdAt,
                updatedAt);
    }

    public static CurrencyWalletData fromWallet(CurrencyWallet wallet) {
        CurrencyWalletData walletData = new CurrencyWalletData();
        walletData.id = wallet.getId();
        walletData.scope = wallet.getScope();
        walletData.ownerId = wallet.getOwnerId();
        walletData.balances = new LinkedHashMap<>(wallet.getBalances());
        walletData.createdAt = wallet.getCreatedAt();
        walletData.updatedAt = wallet.getUpdatedAt();
        return walletData;
    }
}
