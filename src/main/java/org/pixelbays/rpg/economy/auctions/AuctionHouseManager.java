package org.pixelbays.rpg.economy.auctions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.auctions.config.AuctionData;
import org.pixelbays.rpg.economy.auctions.config.settings.AuctionHouseModSettings;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

@SuppressWarnings("null")
public class AuctionHouseManager {

    private final Map<UUID, AuctionListing> listingsById = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> postingHistoryByOwner = new ConcurrentHashMap<>();
    private final AuctionPersistence persistence = new AuctionPersistence();

    public void loadFromAssets() {
        clear();

        RpgModConfig config = resolveConfig();
        if (config != null && !config.isAuctionHouseModuleEnabled()) {
            return;
        }

        for (AuctionData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }
            AuctionListing listing = data.toAuctionListing();
            listingsById.put(listing.getListingId(), listing);
        }
    }

    public void clear() {
        listingsById.clear();
        postingHistoryByOwner.clear();
    }

    @Nullable
    public AuctionListing getListing(@Nonnull UUID listingId) {
        return listingsById.get(listingId);
    }

    @Nonnull
    public List<AuctionListing> getAllListings() {
        List<AuctionListing> listings = new ArrayList<>(listingsById.values());
        listings.sort(Comparator.comparingLong(AuctionListing::getCreatedAtMillis).reversed());
        return listings;
    }

    @Nonnull
    public List<AuctionListing> getListingsForSeller(@Nonnull String sellerOwnerId) {
        List<AuctionListing> listings = new ArrayList<>();
        for (AuctionListing listing : listingsById.values()) {
            if (listing != null && sellerOwnerId.equalsIgnoreCase(listing.getSellerOwnerId())) {
                listings.add(listing);
            }
        }
        listings.sort(Comparator.comparingLong(AuctionListing::getCreatedAtMillis).reversed());
        return listings;
    }

    @Nonnull
    public List<AuctionListing> getActiveListings() {
        List<AuctionListing> active = new ArrayList<>();
        for (AuctionListing listing : getAllListings()) {
            if (listing.isActive()) {
                active.add(listing);
            }
        }
        return active;
    }

    @Nonnull
    public AuctionActionResult createListing(@Nonnull AuctionListingCreateRequest request) {
        AuctionHouseModSettings settings = currentSettings();
        if (!settings.isEnabled()) {
            return AuctionActionResult.failure("Auction house is disabled.");
        }
        if (request.getSellerOwnerId().isBlank()) {
            return AuctionActionResult.failure("Seller owner id cannot be empty.");
        }
        if (request.getItemStack().isEmpty()) {
            return AuctionActionResult.failure("Listing item cannot be empty.");
        }
        if (request.getSellerLevel() < settings.getMinimumLevelToPost()) {
            return AuctionActionResult.failure("Seller does not meet the minimum level to post.");
        }
        if (getActiveListingCount(request.getSellerOwnerId()) >= settings.getMaxActiveListingsPerOwner()) {
            return AuctionActionResult.failure("Seller has reached the maximum active listings.");
        }
        if (isRateLimited(request.getSellerOwnerId(), settings)) {
            return AuctionActionResult.failure("Seller is rate limited from posting new listings.");
        }

        AuctionHouseModSettings.ListingDurationOption duration = settings.getListingDuration(request.getListingDurationHours());
        if (duration == null) {
            return AuctionActionResult.failure("Unsupported listing duration: " + request.getListingDurationHours());
        }

        CurrencyAmountDefinition startingBid = request.getStartingBid();
        CurrencyAmountDefinition buyoutPrice = request.getBuyoutPrice();
        if (startingBid.isFree() && buyoutPrice.isFree()) {
            return AuctionActionResult.failure("Listing must define a bid, a buyout, or both.");
        }
        if (!startingBid.isFree() && !settings.isBiddingAllowed()) {
            return AuctionActionResult.failure("Bidding is disabled.");
        }
        if (!buyoutPrice.isFree() && !settings.isBuyOutsAllowed()) {
            return AuctionActionResult.failure("Buyouts are disabled.");
        }
        if (!startingBid.isFree() && !buyoutPrice.isFree()
                && !startingBid.getCurrencyId().equalsIgnoreCase(buyoutPrice.getCurrencyId())) {
            return AuctionActionResult.failure("Bid and buyout currencies must match for the same listing.");
        }

        CurrencyAmountDefinition saleCurrency = !startingBid.isFree() ? startingBid : buyoutPrice;
        if (!saleCurrency.isFree() && !isAuctionHouseCurrencyAllowed(saleCurrency.getCurrencyId())) {
            return AuctionActionResult.failure("Currency is not allowed in the auction house: " + saleCurrency.getCurrencyId());
        }

        CurrencyAmountDefinition listingFee = duration.getListingFee();
        if (!listingFee.isFree()) {
            CurrencyManager currencyManager = ExamplePlugin.get().getCurrencyManager();
            CurrencyActionResult feeResult = currencyManager.removeBalance(
                    request.getSellerCurrencyScope(),
                    request.getSellerOwnerId(),
                    listingFee.getCurrencyId(),
                    listingFee.getAmount());
            if (!feeResult.isSuccess()) {
                return AuctionActionResult.failure(feeResult.getMessage());
            }
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + (duration.getHours() * 3_600_000L);
        AuctionListing listing = new AuctionListing(
                UUID.randomUUID(),
                request.getSellerAccountId(),
                request.getSellerOwnerId(),
                request.getSellerCurrencyScope(),
                request.getItemStack(),
                duration.getHours(),
                startingBid,
                new CurrencyAmountDefinition(),
                new UUID(0L, 0L),
                "",
                buyoutPrice,
                listingFee,
                settings.getSuccessfulFeePercent(),
                now,
                expiresAt,
                0L,
                0L,
                AuctionListingStatus.Active);

        listingsById.put(listing.getListingId(), listing);
        rememberPost(request.getSellerOwnerId(), now, settings);
        saveListingIfEnabled(listing);
        RpgLogging.debugDeveloper("Created auction listing %s for seller %s", listing.getListingId(), request.getSellerOwnerId());
        return AuctionActionResult.success("Auction listing created.", listing);
    }

    @Nonnull
    public AuctionActionResult cancelListing(@Nonnull UUID listingId, @Nonnull String sellerOwnerId) {
        AuctionHouseModSettings settings = currentSettings();
        if (!settings.isAllowSellerCancels()) {
            return AuctionActionResult.failure("Seller cancels are disabled.");
        }

        AuctionListing listing = listingsById.get(listingId);
        if (listing == null) {
            return AuctionActionResult.failure("Listing not found.");
        }
        if (!sellerOwnerId.equalsIgnoreCase(listing.getSellerOwnerId())) {
            return AuctionActionResult.failure("Listing does not belong to the seller.");
        }
        if (!listing.isActive()) {
            return AuctionActionResult.failure("Listing is no longer active.");
        }

        listing.setStatus(AuctionListingStatus.Cancelled);
        saveListingIfEnabled(listing);
        return AuctionActionResult.success("Auction listing cancelled.", listing);
    }

    @Nonnull
    public AuctionActionResult markExpired(@Nonnull UUID listingId) {
        AuctionListing listing = listingsById.get(listingId);
        if (listing == null) {
            return AuctionActionResult.failure("Listing not found.");
        }
        if (!listing.isActive()) {
            return AuctionActionResult.failure("Listing is no longer active.");
        }

        listing.setStatus(AuctionListingStatus.Expired);
        saveListingIfEnabled(listing);
        return AuctionActionResult.success("Auction listing expired.", listing);
    }

    @Nonnull
    public AuctionActionResult markSold(@Nonnull UUID listingId,
            @Nonnull UUID buyerAccountId,
            @Nonnull String buyerOwnerId,
            @Nonnull CurrencyAmountDefinition salePrice) {
        AuctionListing listing = listingsById.get(listingId);
        if (listing == null) {
            return AuctionActionResult.failure("Listing not found.");
        }
        if (!listing.isActive()) {
            return AuctionActionResult.failure("Listing is no longer active.");
        }
        if (salePrice.isFree()) {
            return AuctionActionResult.failure("Sale price must be greater than zero.");
        }

        AuctionHouseModSettings settings = currentSettings();
        long now = System.currentTimeMillis();
        listing.setCurrentBid(salePrice);
        listing.setCurrentBidderAccountId(buyerAccountId);
        listing.setCurrentBidderOwnerId(buyerOwnerId);
        listing.setBuyerItemDeliverAtMillis(now + (settings.getBuyerItemMailDelayMinutes() * 60_000L));
        listing.setSellerCurrencyDeliverAtMillis(now + (settings.getSellerCurrencyDelayMinutes() * 60_000L));
        listing.setStatus(AuctionListingStatus.Sold);
        saveListingIfEnabled(listing);
        return AuctionActionResult.success("Auction listing sold.", listing);
    }

    public int getActiveListingCount(@Nonnull String sellerOwnerId) {
        int count = 0;
        for (AuctionListing listing : listingsById.values()) {
            if (listing != null && listing.isActive() && sellerOwnerId.equalsIgnoreCase(listing.getSellerOwnerId())) {
                count++;
            }
        }
        return count;
    }

    public boolean isAuctionHouseCurrencyAllowed(@Nullable String currencyId) {
        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(currencyId);
        return definition != null && definition.isEnabled() && definition.isAuctionHouseAllowed();
    }

    @Nonnull
    private AuctionHouseModSettings currentSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new AuctionHouseModSettings() : config.getAuctionHouseSettings();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }
        RpgModConfig config = assetMap.getAsset("default");
        if (config != null) {
            return config;
        }
        return assetMap.getAsset("Default");
    }

    private boolean isRateLimited(@Nonnull String sellerOwnerId, @Nonnull AuctionHouseModSettings settings) {
        int windowSeconds = settings.getPostRateLimitWindowSeconds();
        if (windowSeconds <= 0) {
            return false;
        }

        long cutoff = System.currentTimeMillis() - (windowSeconds * 1000L);
        List<Long> timestamps = postingHistoryByOwner.computeIfAbsent(sellerOwnerId, ignored -> new ArrayList<>());
        timestamps.removeIf(timestamp -> timestamp == null || timestamp < cutoff);
        return timestamps.size() >= settings.getPostRateLimitCount();
    }

    private void rememberPost(@Nonnull String sellerOwnerId, long timestamp, @Nonnull AuctionHouseModSettings settings) {
        List<Long> timestamps = postingHistoryByOwner.computeIfAbsent(sellerOwnerId, ignored -> new ArrayList<>());
        timestamps.add(timestamp);
        int windowSeconds = settings.getPostRateLimitWindowSeconds();
        if (windowSeconds > 0) {
            long cutoff = timestamp - (windowSeconds * 1000L);
            timestamps.removeIf(value -> value == null || value < cutoff);
        }
    }

    private void saveListingIfEnabled(@Nonnull AuctionListing listing) {
        if (!currentSettings().isPersistenceEnabled()) {
            return;
        }
        persistence.saveListing(listing);
    }
}
