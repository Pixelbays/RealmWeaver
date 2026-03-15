package org.pixelbays.rpg.economy.auctions.config;

import java.util.UUID;

import org.pixelbays.rpg.economy.auctions.AuctionListing;
import org.pixelbays.rpg.economy.auctions.AuctionListingStatus;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings({ "deprecation", "null" })
public class AuctionData implements JsonAssetWithMap<String, DefaultAssetMap<String, AuctionData>> {

    public static final AssetBuilderCodec<String, AuctionData> CODEC = AssetBuilderCodec.builder(
            AuctionData.class,
            AuctionData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("SellerAccountId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.sellerAccountId = s, i -> i.sellerAccountId)
            .add()
            .append(new KeyedCodec<>("SellerOwnerId", Codec.STRING, false, true),
                    (i, s) -> i.sellerOwnerId = s, i -> i.sellerOwnerId)
            .add()
            .append(new KeyedCodec<>("SellerCurrencyScope", new EnumCodec<>(CurrencyScope.class), false, true),
                    (i, s) -> i.sellerCurrencyScope = s, i -> i.sellerCurrencyScope)
            .add()
            .append(new KeyedCodec<>("ItemStack", ItemStack.CODEC, false, true),
                    (i, s) -> i.itemStack = s, i -> i.itemStack)
            .add()
            .append(new KeyedCodec<>("ListingDurationHours", Codec.INTEGER, false, true),
                    (i, s) -> i.listingDurationHours = s, i -> i.listingDurationHours)
            .add()
            .append(new KeyedCodec<>("StartingBid", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.startingBid = s, i -> i.startingBid)
            .add()
            .append(new KeyedCodec<>("CurrentBid", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.currentBid = s, i -> i.currentBid)
            .add()
            .append(new KeyedCodec<>("CurrentBidderAccountId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.currentBidderAccountId = s, i -> i.currentBidderAccountId)
            .add()
            .append(new KeyedCodec<>("CurrentBidderOwnerId", Codec.STRING, false, true),
                    (i, s) -> i.currentBidderOwnerId = s, i -> i.currentBidderOwnerId)
            .add()
            .append(new KeyedCodec<>("BuyoutPrice", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.buyoutPrice = s, i -> i.buyoutPrice)
            .add()
            .append(new KeyedCodec<>("ListingFee", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.listingFee = s, i -> i.listingFee)
            .add()
            .append(new KeyedCodec<>("SuccessfulFeePercent", Codec.INTEGER, false, true),
                    (i, s) -> i.successfulFeePercent = s, i -> i.successfulFeePercent)
            .add()
            .append(new KeyedCodec<>("CreatedAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.createdAtMillis = s, i -> i.createdAtMillis)
            .add()
            .append(new KeyedCodec<>("ExpiresAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.expiresAtMillis = s, i -> i.expiresAtMillis)
            .add()
            .append(new KeyedCodec<>("SellerCurrencyDeliverAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.sellerCurrencyDeliverAtMillis = s, i -> i.sellerCurrencyDeliverAtMillis)
            .add()
            .append(new KeyedCodec<>("BuyerItemDeliverAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.buyerItemDeliverAtMillis = s, i -> i.buyerItemDeliverAtMillis)
            .add()
            .append(new KeyedCodec<>("Status", new EnumCodec<>(AuctionListingStatus.class), false, true),
                    (i, s) -> i.status = s, i -> i.status)
            .add()
            .build();

    private static DefaultAssetMap<String, AuctionData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private UUID sellerAccountId;
    private String sellerOwnerId;
    private CurrencyScope sellerCurrencyScope;
    private ItemStack itemStack;
    private int listingDurationHours;
    private CurrencyAmountDefinition startingBid;
    private CurrencyAmountDefinition currentBid;
    private UUID currentBidderAccountId;
    private String currentBidderOwnerId;
    private CurrencyAmountDefinition buyoutPrice;
    private CurrencyAmountDefinition listingFee;
    private int successfulFeePercent;
    private long createdAtMillis;
    private long expiresAtMillis;
    private long sellerCurrencyDeliverAtMillis;
    private long buyerItemDeliverAtMillis;
    private AuctionListingStatus status;

    public AuctionData() {
        this.id = "";
        this.sellerAccountId = new UUID(0L, 0L);
        this.sellerOwnerId = "";
        this.sellerCurrencyScope = CurrencyScope.Character;
        this.itemStack = ItemStack.EMPTY;
        this.listingDurationHours = 24;
        this.startingBid = new CurrencyAmountDefinition();
        this.currentBid = new CurrencyAmountDefinition();
        this.currentBidderAccountId = new UUID(0L, 0L);
        this.currentBidderOwnerId = "";
        this.buyoutPrice = new CurrencyAmountDefinition();
        this.listingFee = new CurrencyAmountDefinition();
        this.successfulFeePercent = 0;
        this.createdAtMillis = 0L;
        this.expiresAtMillis = 0L;
        this.sellerCurrencyDeliverAtMillis = 0L;
        this.buyerItemDeliverAtMillis = 0L;
        this.status = AuctionListingStatus.Active;
    }

    public static DefaultAssetMap<String, AuctionData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(AuctionData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, AuctionData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public AuctionListing toAuctionListing() {
        return new AuctionListing(
                parseUuid(id),
                sellerAccountId == null ? new UUID(0L, 0L) : sellerAccountId,
                sellerOwnerId,
                sellerCurrencyScope,
                itemStack,
                listingDurationHours,
                startingBid,
                currentBid,
                currentBidderAccountId,
                currentBidderOwnerId,
                buyoutPrice,
                listingFee,
                successfulFeePercent,
                createdAtMillis,
                expiresAtMillis,
                sellerCurrencyDeliverAtMillis,
                buyerItemDeliverAtMillis,
                status);
    }

    public static AuctionData fromAuctionListing(AuctionListing listing) {
        AuctionData data = new AuctionData();
        data.id = listing.getListingId().toString();
        data.sellerAccountId = listing.getSellerAccountId();
        data.sellerOwnerId = listing.getSellerOwnerId();
        data.sellerCurrencyScope = listing.getSellerCurrencyScope();
        data.itemStack = listing.getItemStack();
        data.listingDurationHours = listing.getListingDurationHours();
        data.startingBid = listing.getStartingBid();
        data.currentBid = listing.getCurrentBid();
        data.currentBidderAccountId = listing.getCurrentBidderAccountId();
        data.currentBidderOwnerId = listing.getCurrentBidderOwnerId();
        data.buyoutPrice = listing.getBuyoutPrice();
        data.listingFee = listing.getListingFee();
        data.successfulFeePercent = listing.getSuccessfulFeePercent();
        data.createdAtMillis = listing.getCreatedAtMillis();
        data.expiresAtMillis = listing.getExpiresAtMillis();
        data.sellerCurrencyDeliverAtMillis = listing.getSellerCurrencyDeliverAtMillis();
        data.buyerItemDeliverAtMillis = listing.getBuyerItemDeliverAtMillis();
        data.status = listing.getStatus();
        return data;
    }

    private static UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new UUID(0L, 0L);
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }
}
