package org.pixelbays.rpg.economy.auctions.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "null", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class AuctionHouseModSettings {

    private static final FunctionCodec<ListingDurationOption[], List<ListingDurationOption>> DURATION_LIST_CODEC =
            new FunctionCodec<>(
                    new ArrayCodec<>(ListingDurationOption.CODEC, ListingDurationOption[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(ListingDurationOption[]::new));

    public static final BuilderCodec<AuctionHouseModSettings> CODEC = BuilderCodec
            .builder(AuctionHouseModSettings.class, AuctionHouseModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("BiddingAllowed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.biddingAllowed = s, i -> i.biddingAllowed)
            .add()
            .append(new KeyedCodec<>("BuyOutsAllowed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.buyOutsAllowed = s, i -> i.buyOutsAllowed)
            .add()
            .append(new KeyedCodec<>("DefaultListingLengthHours", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultListingLengthHours = s, i -> i.defaultListingLengthHours)
            .add()
            .append(new KeyedCodec<>("ListingDurations", DURATION_LIST_CODEC, false, true),
                    (i, s) -> i.listingDurations = s, i -> i.listingDurations)
            .add()
            .append(new KeyedCodec<>("SuccessfulFeePercent", Codec.INTEGER, false, true),
                    (i, s) -> i.successfulFeePercent = s, i -> i.successfulFeePercent)
            .add()
            .append(new KeyedCodec<>("BuyerItemMailDelayMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.buyerItemMailDelayMinutes = s, i -> i.buyerItemMailDelayMinutes)
            .add()
            .append(new KeyedCodec<>("SellerCurrencyDelayMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.sellerCurrencyDelayMinutes = s, i -> i.sellerCurrencyDelayMinutes)
            .add()
            .append(new KeyedCodec<>("MaxActiveListingsPerOwner", Codec.INTEGER, false, true),
                    (i, s) -> i.maxActiveListingsPerOwner = s, i -> i.maxActiveListingsPerOwner)
            .add()
            .append(new KeyedCodec<>("PostRateLimitCount", Codec.INTEGER, false, true),
                    (i, s) -> i.postRateLimitCount = s, i -> i.postRateLimitCount)
            .add()
            .append(new KeyedCodec<>("PostRateLimitWindowSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.postRateLimitWindowSeconds = s, i -> i.postRateLimitWindowSeconds)
            .add()
            .append(new KeyedCodec<>("MinimumLevelToPost", Codec.INTEGER, false, true),
                    (i, s) -> i.minimumLevelToPost = s, i -> i.minimumLevelToPost)
            .add()
            .append(new KeyedCodec<>("AllowSellerCancels", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowSellerCancels = s, i -> i.allowSellerCancels)
            .add()
            .append(new KeyedCodec<>("ExpiredListingsReturnByMail", Codec.BOOLEAN, false, true),
                    (i, s) -> i.expiredListingsReturnByMail = s, i -> i.expiredListingsReturnByMail)
            .add()
            .append(new KeyedCodec<>("PersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.persistenceEnabled = s, i -> i.persistenceEnabled)
            .add()
            .build();

    private boolean enabled;
    private boolean biddingAllowed;
    private boolean buyOutsAllowed;
    private int defaultListingLengthHours;
    private List<ListingDurationOption> listingDurations;
    private int successfulFeePercent;
    private int buyerItemMailDelayMinutes;
    private int sellerCurrencyDelayMinutes;
    private int maxActiveListingsPerOwner;
    private int postRateLimitCount;
    private int postRateLimitWindowSeconds;
    private int minimumLevelToPost;
    private boolean allowSellerCancels;
    private boolean expiredListingsReturnByMail;
    private boolean persistenceEnabled;

    public AuctionHouseModSettings() {
        this.enabled = true;
        this.biddingAllowed = true;
        this.buyOutsAllowed = true;
        this.defaultListingLengthHours = 24;
        this.listingDurations = new ArrayList<>(List.of(
                new ListingDurationOption(24, new CurrencyAmountDefinition("Gold", 1L)),
                new ListingDurationOption(48, new CurrencyAmountDefinition("Gold", 2L)),
                new ListingDurationOption(72, new CurrencyAmountDefinition("Gold", 3L))));
        this.successfulFeePercent = 5;
        this.buyerItemMailDelayMinutes = 0;
        this.sellerCurrencyDelayMinutes = 0;
        this.maxActiveListingsPerOwner = 20;
        this.postRateLimitCount = 5;
        this.postRateLimitWindowSeconds = 60;
        this.minimumLevelToPost = 1;
        this.allowSellerCancels = true;
        this.expiredListingsReturnByMail = true;
        this.persistenceEnabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBiddingAllowed() {
        return biddingAllowed;
    }

    public boolean isBuyOutsAllowed() {
        return buyOutsAllowed;
    }

    public int getDefaultListingLengthHours() {
        return Math.max(1, defaultListingLengthHours);
    }

    public List<ListingDurationOption> getListingDurations() {
        return listingDurations == null ? List.of() : Collections.unmodifiableList(listingDurations);
    }

    public ListingDurationOption getListingDuration(int hours) {
        for (ListingDurationOption option : getListingDurations()) {
            if (option != null && option.getHours() == hours) {
                return option;
            }
        }
        return null;
    }

    public ListingDurationOption getDefaultListingDuration() {
        ListingDurationOption configured = getListingDuration(getDefaultListingLengthHours());
        if (configured != null) {
            return configured;
        }
        List<ListingDurationOption> durations = getListingDurations();
        return durations.isEmpty() ? new ListingDurationOption() : durations.get(0);
    }

    public int getSuccessfulFeePercent() {
        return Math.max(0, Math.min(100, successfulFeePercent));
    }

    public int getBuyerItemMailDelayMinutes() {
        return Math.max(0, buyerItemMailDelayMinutes);
    }

    public int getSellerCurrencyDelayMinutes() {
        return Math.max(0, sellerCurrencyDelayMinutes);
    }

    public int getMaxActiveListingsPerOwner() {
        return Math.max(1, maxActiveListingsPerOwner);
    }

    public int getPostRateLimitCount() {
        return Math.max(1, postRateLimitCount);
    }

    public int getPostRateLimitWindowSeconds() {
        return Math.max(0, postRateLimitWindowSeconds);
    }

    public int getMinimumLevelToPost() {
        return Math.max(1, minimumLevelToPost);
    }

    public boolean isAllowSellerCancels() {
        return allowSellerCancels;
    }

    public boolean isExpiredListingsReturnByMail() {
        return expiredListingsReturnByMail;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public static class ListingDurationOption {

        public static final BuilderCodec<ListingDurationOption> CODEC = BuilderCodec
                .builder(ListingDurationOption.class, ListingDurationOption::new)
                .append(new KeyedCodec<>("Hours", Codec.INTEGER, false, true),
                        (i, s) -> i.hours = s, i -> i.hours)
                .add()
                .append(new KeyedCodec<>("ListingFee", CurrencyAmountDefinition.CODEC, false, true),
                        (i, s) -> i.listingFee = s, i -> i.listingFee)
                .add()
                .build();

        private int hours;
        private CurrencyAmountDefinition listingFee;

        public ListingDurationOption() {
            this(24, new CurrencyAmountDefinition());
        }

        public ListingDurationOption(int hours, CurrencyAmountDefinition listingFee) {
            this.hours = hours;
            this.listingFee = listingFee == null ? new CurrencyAmountDefinition() : listingFee;
        }

        public int getHours() {
            return Math.max(1, hours);
        }

        public CurrencyAmountDefinition getListingFee() {
            return listingFee == null ? new CurrencyAmountDefinition() : listingFee;
        }
    }
}
