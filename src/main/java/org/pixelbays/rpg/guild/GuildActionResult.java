package org.pixelbays.rpg.guild;

public class GuildActionResult {

    private final boolean success;
    private final String message;
    private final Guild guild;

    private GuildActionResult(boolean success, String message, Guild guild) {
        this.success = success;
        this.message = message;
        this.guild = guild;
    }

    public static GuildActionResult success(String message, Guild guild) {
        return new GuildActionResult(true, message, guild);
    }

    public static GuildActionResult success(String message) {
        return new GuildActionResult(true, message, null);
    }

    public static GuildActionResult failure(String message) {
        return new GuildActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Guild getGuild() {
        return guild;
    }
}
