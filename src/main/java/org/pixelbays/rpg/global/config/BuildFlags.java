package org.pixelbays.rpg.global.config;

/**
 * Compile-time feature flags for Realmweaver.
 *
 * Set a flag to {@code false} to completely exclude a module from the build:
 * its settings block will not appear in RpgModConfig, and all related
 * systems / components / commands will be skipped at server startup.
 *
 * Because these are {@code public static final} primitive constants the Java
 * compiler inlines their values at every reference site, meaning dead
 * branches are eliminated entirely and no runtime overhead is added.
 *
 * To re-enable a module, flip the constant back to {@code true} and rebuild.
 */
public final class BuildFlags {

    private BuildFlags() {}

    public static final boolean CLASS_MODULE         = true;

    public static final boolean CHARACTER_MODULE     = false;

    public static final boolean ACHIEVEMENT_MODULE   = false;

    public static final boolean TALENT_MODULE        = true;

    public static final boolean LEVELING_MODULE      = true;

    public static final boolean ABILITY_MODULE       = true;

    public static final boolean INVENTORY_MODULE     = false;

    public static final boolean ITEM_MODULE          = true;

    public static final boolean PARTY_MODULE         = true;

    public static final boolean GUILD_MODULE         = true;

    public static final boolean CHAT_MODULE          = true;

    public static final boolean NPC_MODULE           = false;

    public static final boolean CAMERA_MODULE        = false;

    public static final boolean BANK_MODULE          = false;

    public static final boolean CURRENCY_MODULE      = true;

    public static final boolean AUCTION_HOUSE_MODULE = false;

    public static final boolean MAIL_MODULE          = false;

    public static final boolean WORLD_MODULE         = false;

    public static final boolean LOCKPICKING_MODULE   = true;

    public static final boolean RESTED_XP            = false;
}
