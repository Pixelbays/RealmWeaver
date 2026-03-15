package org.pixelbays.rpg.economy.auctions;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings("null")
public class AuctionListing {

    private final UUID listingId;
    private final UUID sellerAccountId;
    private final String sellerOwnerId;
    private final CurrencyScope sellerCurrencyScope;
    private final ItemStack itemStack;
    private final int listingDurationHours;
    private final CurrencyAmountDefinition startingBid;
    private CurrencyAmountDefinition currentBid;
    private UUID currentBidderAccountId;
    private String currentBidderOwnerId;
    private final CurrencyAmountDefinition buyoutPrice;
    private final CurrencyAmountDefinition listingFee;
    private final int successfulFeePercent;
    private final long createdAtMillis;
    private final long expiresAtMillis;
    private long sellerCurrencyDeliverAtMillis;
    private long buyerItemDeliverAtMillis;
    private AuctionListingStatus status;

    public AuctionListing(@Nonnull UUID listingId,
            @Nonnull UUID sellerAccountId,
            @Nullable String sellerOwnerId,
            @Nullable CurrencyScope sellerCurrencyScope,
            @Nullable ItemStack itemStack,
            int listingDurationHours,
            @Nullable CurrencyAmountDefinition startingBid,
            @Nullable CurrencyAmountDefinition currentBid,
            @Nullable UUID currentBidderAccountId,
            @Nullable String currentBidderOwnerId,
            @Nullable CurrencyAmountDefinition buyoutPrice,
            @Nullable CurrencyAmountDefinition listingFee,
            int successfulFeePercent,
            long createdAtMillis,
            long expiresAtMillis,
            long sellerCurrencyDeliverAtMillis,
            long buyerItemDeliverAtMillis,
            @Nullable AuctionListingStatus status) {
        this.listingId = listingId;
        this.sellerAccountId = sellerAccountId;
        this.sellerOwnerId = sellerOwnerId == null ? "" : sellerOwnerId;
        this.sellerCurrencyScope = sellerCurrencyScope == null ? CurrencyScope.Character : sellerCurrencyScope;
        this.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack;
        this.listingDurationHours = Math.max(1, listingDurationHours);
        this.startingBid = startingBid == null ? new CurrencyAmountDefinition() : startingBid;
        this.currentBid = currentBid == null ? new CurrencyAmountDefinition() : currentBid;
        this.currentBidderAccountId = currentBidderAccountId == null ? new UUID(0L, 0L) : currentBidderAccountId;
        this.currentBidderOwnerId = currentBidderOwnerId == null ? "" : currentBidderOwnerId;
        this.buyoutPrice = buyoutPrice == null ? new CurrencyAmountDefinition() : buyoutPrice;
        this.listingFee = listingFee == null ? new CurrencyAmountDefinition() : listingFee;
        this.successfulFeePercent = Math.max(0, Math.min(100, successfulFeePercent));
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.sellerCurrencyDeliverAtMillis = Math.max(0L, sellerCurrencyDeliverAtMillis);
        this.buyerItemDeliverAtMillis = Math.max(0L, buyerItemDeliverAtMillis);
        this.status = status == null ? AuctionListingStatus.Active : status;
    }

    @Nonnull
    public UUID getListingId() {
        return listingId;
    }

    @Nonnull
    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    @Nonnull
    public String getSellerOwnerId() {
        return sellerOwnerId;
    }

    @Nonnull
    public CurrencyScope getSellerCurrencyScope() {
        return sellerCurrencyScope;
    }

    @Nonnull
    public ItemStack getItemStack() {
        return itemStack == null ? ItemStack.EMPTY : itemStack;
    }

    public int getListingDurationHours() {
        return listingDurationHours;
    }

    @Nonnull
    public CurrencyAmountDefinition getStartingBid() {
        return startingBid == null ? new CurrencyAmountDefinition() : startingBid;
    }

    @Nonnull
    public CurrencyAmountDefinition getCurrentBid() {
        return currentBid == null ? new CurrencyAmountDefinition() : currentBid;
    }

    public void setCurrentBid(@Nullable CurrencyAmountDefinition currentBid) {
        this.currentBid = currentBid == null ? new CurrencyAmountDefinition() : currentBid;
    }

    @Nonnull
    public UUID getCurrentBidderAccountId() {
        return currentBidderAccountId == null ? new UUID(0L, 0L) : currentBidderAccountId;
    }

    public void setCurrentBidderAccountId(@Nullable UUID currentBidderAccountId) {
        this.currentBidderAccountId = currentBidderAccountId == null ? new UUID(0L, 0L) : currentBidderAccountId;
    }

    @Nonnull
    public String getCurrentBidderOwnerId() {
        return currentBidderOwnerId == null ? "" : currentBidderOwnerId;
    }

    public void setCurrentBidderOwnerId(@Nullable String currentBidderOwnerId) {
        this.currentBidderOwnerId = currentBidderOwnerId == null ? "" : currentBidderOwnerId;
    }

    @Nonnull
    public CurrencyAmountDefinition getBuyoutPrice() {
        return buyoutPrice == null ? new CurrencyAmountDefinition() : buyoutPrice;
    }

    @Nonnull
    public CurrencyAmountDefinition getListingFee() {
        return listingFee == null ? new CurrencyAmountDefinition() : listingFee;
    }

    public int getSuccessfulFeePercent() {
        return successfulFeePercent;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public long getSellerCurrencyDeliverAtMillis() {
        return sellerCurrencyDeliverAtMillis;
    }

    public void setSellerCurrencyDeliverAtMillis(long sellerCurrencyDeliverAtMillis) {
        this.sellerCurrencyDeliverAtMillis = Math.max(0L, sellerCurrencyDeliverAtMillis);
    }

    public long getBuyerItemDeliverAtMillis() {
        return buyerItemDeliverAtMillis;
    }

    public void setBuyerItemDeliverAtMillis(long buyerItemDeliverAtMillis) {
        this.buyerItemDeliverAtMillis = Math.max(0L, buyerItemDeliverAtMillis);
    }

    @Nonnull
    public AuctionListingStatus getStatus() {
        return status == null ? AuctionListingStatus.Active : status;
    }

    public void setStatus(@Nullable AuctionListingStatus status) {
        this.status = status == null ? AuctionListingStatus.Active : status;
    }

    public boolean isActive() {
        return getStatus() == AuctionListingStatus.Active;
    }
}
