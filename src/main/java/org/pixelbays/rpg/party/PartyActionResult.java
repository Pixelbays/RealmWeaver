package org.pixelbays.rpg.party;

public class PartyActionResult {

    private final boolean success;
    private final String message;
    private final Party party;

    private PartyActionResult(boolean success, String message, Party party) {
        this.success = success;
        this.message = message;
        this.party = party;
    }

    public static PartyActionResult success(String message, Party party) {
        return new PartyActionResult(true, message, party);
    }

    public static PartyActionResult success(String message) {
        return new PartyActionResult(true, message, null);
    }

    public static PartyActionResult failure(String message) {
        return new PartyActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Party getParty() {
        return party;
    }
}
