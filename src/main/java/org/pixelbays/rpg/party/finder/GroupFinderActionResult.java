package org.pixelbays.rpg.party.finder;

import java.util.UUID;

import javax.annotation.Nullable;

public class GroupFinderActionResult {

    private final boolean success;
    private final String message;
    @Nullable
    private final UUID listingPartyId;

    private GroupFinderActionResult(boolean success, String message, @Nullable UUID listingPartyId) {
        this.success = success;
        this.message = message;
        this.listingPartyId = listingPartyId;
    }

    public static GroupFinderActionResult success(String message) {
        return new GroupFinderActionResult(true, message, null);
    }

    public static GroupFinderActionResult success(String message, UUID listingPartyId) {
        return new GroupFinderActionResult(true, message, listingPartyId);
    }

    public static GroupFinderActionResult failure(String message) {
        return new GroupFinderActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public UUID getListingPartyId() {
        return listingPartyId;
    }
}
