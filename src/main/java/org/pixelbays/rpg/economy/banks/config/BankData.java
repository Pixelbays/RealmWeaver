package org.pixelbays.rpg.economy.banks.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.economy.banks.BankAccount;
import org.pixelbays.rpg.economy.banks.BankTabItem;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation", "null" })
public class BankData implements JsonAssetWithMap<String, DefaultAssetMap<String, BankData>> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.STRING, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final FunctionCodec<BankTabItem[], List<BankTabItem>> TAB_ITEM_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(BankTabItem.CODEC, BankTabItem[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(BankTabItem[]::new));

    public static final AssetBuilderCodec<String, BankData> CODEC = AssetBuilderCodec.builder(
            BankData.class,
            BankData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("BankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.bankTypeId = s, i -> i.bankTypeId)
            .add()
            .append(new KeyedCodec<>("OwnerScope", new EnumCodec<>(BankScope.class), false, true),
                    (i, s) -> i.ownerScope = s, i -> i.ownerScope)
            .add()
            .append(new KeyedCodec<>("OwnerId", Codec.STRING, false, true),
                    (i, s) -> i.ownerId = s, i -> i.ownerId)
            .add()
            .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                    (i, s) -> i.displayName = s, i -> i.displayName)
            .add()
            .append(new KeyedCodec<>("UnlockedTabIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.unlockedTabIds = s, i -> i.unlockedTabIds)
            .add()
            .append(new KeyedCodec<>("TabSlotCounts", new MapCodec<>(Codec.INTEGER, LinkedHashMap::new, false), false, true),
                    (i, s) -> i.tabSlotCounts = s, i -> i.tabSlotCounts)
            .add()
            .append(new KeyedCodec<>("TabItems",
                    new MapCodec<>(TAB_ITEM_LIST_CODEC, LinkedHashMap::new, false), false, true),
                    (i, s) -> i.tabItems = s, i -> i.tabItems)
            .add()
            .append(new KeyedCodec<>("CreatedAt", Codec.LONG, false, true),
                    (i, s) -> i.createdAt = s, i -> i.createdAt)
            .add()
            .append(new KeyedCodec<>("UpdatedAt", Codec.LONG, false, true),
                    (i, s) -> i.updatedAt = s, i -> i.updatedAt)
            .add()
            .build();

    private static DefaultAssetMap<String, BankData> assetMap;
    private AssetExtraInfo.Data data;

    private String id;
    private String bankTypeId;
    private BankScope ownerScope;
    private String ownerId;
    private String displayName;
    private List<String> unlockedTabIds;
    private Map<String, Integer> tabSlotCounts;
        private Map<String, List<BankTabItem>> tabItems;
    private long createdAt;
    private long updatedAt;

    public BankData() {
        this.id = "";
        this.bankTypeId = "";
        this.ownerScope = BankScope.Player;
        this.ownerId = "";
        this.displayName = "";
        this.unlockedTabIds = new ArrayList<>();
        this.tabSlotCounts = new LinkedHashMap<>();
        this.tabItems = new LinkedHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public static DefaultAssetMap<String, BankData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(BankData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, BankData>) assetStore.getAssetMap();
        }

        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public BankAccount toBankAccount() {
        return new BankAccount(
                id,
                bankTypeId,
                ownerScope == null ? BankScope.Player : ownerScope,
                ownerId == null ? "" : ownerId,
                displayName == null ? "" : displayName,
                unlockedTabIds == null ? List.of() : unlockedTabIds,
                tabSlotCounts == null ? Map.of() : tabSlotCounts,
                tabItems == null ? Map.of() : tabItems,
                createdAt,
                updatedAt);
    }

    public static BankData fromBankAccount(BankAccount bankAccount) {
        BankData bankData = new BankData();
        bankData.id = bankAccount.getId();
        bankData.bankTypeId = bankAccount.getBankTypeId();
        bankData.ownerScope = bankAccount.getOwnerScope();
        bankData.ownerId = bankAccount.getOwnerId();
        bankData.displayName = bankAccount.getDisplayName();
        bankData.unlockedTabIds = new ArrayList<>(bankAccount.getUnlockedTabIds());
        bankData.tabSlotCounts = new LinkedHashMap<>(bankAccount.getTabSlotCounts());
        bankData.tabItems = new LinkedHashMap<>(bankAccount.getAllTabItems());
        bankData.createdAt = bankAccount.getCreatedAt();
        bankData.updatedAt = bankAccount.getUpdatedAt();
        return bankData;
    }
}
