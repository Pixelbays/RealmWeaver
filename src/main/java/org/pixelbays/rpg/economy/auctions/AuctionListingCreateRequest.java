package org.pixelbays.rpg.economy.auctions;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings("null")
public class AuctionListingCreateRequest {

    private final UUID sellerAccountId;
    private final String sellerOwnerId;
    private final CurrencyScope sellerCurrencyScope;
    private final int sellerLevel;
    private final ItemStack itemStack;
    private final int listingDurationHours;
    private final CurrencyAmountDefinition startingBid;
    private final CurrencyAmountDefinition buyoutPrice;

    public AuctionListingCreateRequest(@Nonnull UUID sellerAccountId,
            @Nullable String sellerOwnerId,
            @Nullable CurrencyScope sellerCurrencyScope,
            int sellerLevel,
            @Nullable ItemStack itemStack,
            int listingDurationHours,
            @Nullable CurrencyAmountDefinition startingBid,
            @Nullable CurrencyAmountDefinition buyoutPrice) {
        this.sellerAccountId = sellerAccountId;
        this.sellerOwnerId = sellerOwnerId == null ? "" : sellerOwnerId;
        this.sellerCurrencyScope = sellerCurrencyScope == null ? CurrencyScope.Character : sellerCurrencyScope;
        this.sellerLevel = sellerLevel;
        this.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack;
        this.listingDurationHours = listingDurationHours;
        this.startingBid = startingBid == null ? new CurrencyAmountDefinition() : startingBid;
        this.buyoutPrice = buyoutPrice == null ? new CurrencyAmountDefinition() : buyoutPrice;
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

    public int getSellerLevel() {
        return sellerLevel;
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
    public CurrencyAmountDefinition getBuyoutPrice() {
        return buyoutPrice == null ? new CurrencyAmountDefinition() : buyoutPrice;
    }
}
