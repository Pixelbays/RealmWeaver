package org.pixelbays.rpg.economy.auctions;

import javax.annotation.Nullable;

public class AuctionActionResult {

    private final boolean success;
    private final String message;
    private final AuctionListing auctionListing;

    private AuctionActionResult(boolean success, String message, @Nullable AuctionListing auctionListing) {
        this.success = success;
        this.message = message;
        this.auctionListing = auctionListing;
    }

    public static AuctionActionResult success(String message, @Nullable AuctionListing auctionListing) {
        return new AuctionActionResult(true, message, auctionListing);
    }

    public static AuctionActionResult success(String message) {
        return new AuctionActionResult(true, message, null);
    }

    public static AuctionActionResult failure(String message) {
        return new AuctionActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public AuctionListing getAuctionListing() {
        return auctionListing;
    }
}
