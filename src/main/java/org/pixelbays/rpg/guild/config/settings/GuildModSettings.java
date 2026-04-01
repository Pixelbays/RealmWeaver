package org.pixelbays.rpg.guild.config.settings;

import org.pixelbays.rpg.guild.GuildJoinPolicy;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class GuildModSettings {

    public static final BuilderCodec<GuildModSettings> CODEC = BuilderCodec
            .builder(GuildModSettings.class, GuildModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("GuildEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.guildEnabled = s, i -> i.guildEnabled)
            .add()
            .append(new KeyedCodec<>("GuildMaxMembers", Codec.INTEGER, false, true),
                    (i, s) -> i.guildMaxMembers = s, i -> i.guildMaxMembers)
            .add()
            .append(new KeyedCodec<>("GuildInviteExpirySeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.guildInviteExpirySeconds = s, i -> i.guildInviteExpirySeconds)
            .add()
                .append(new KeyedCodec<>("GuildApplicationExpirySeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.guildApplicationExpirySeconds = s, i -> i.guildApplicationExpirySeconds)
                .add()
            .append(new KeyedCodec<>("GuildNameMinLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildNameMinLength = s, i -> i.guildNameMinLength)
            .add()
            .append(new KeyedCodec<>("GuildNameMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildNameMaxLength = s, i -> i.guildNameMaxLength)
            .add()
            .append(new KeyedCodec<>("GuildTagMinLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildTagMinLength = s, i -> i.guildTagMinLength)
            .add()
            .append(new KeyedCodec<>("GuildTagMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildTagMaxLength = s, i -> i.guildTagMaxLength)
            .add()
                .append(new KeyedCodec<>("GuildDescriptionMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildDescriptionMaxLength = s, i -> i.guildDescriptionMaxLength)
                .add()
                .append(new KeyedCodec<>("GuildApplicationMessageMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildApplicationMessageMaxLength = s, i -> i.guildApplicationMessageMaxLength)
                .add()
                .append(new KeyedCodec<>("GuildMotdMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildMotdMaxLength = s, i -> i.guildMotdMaxLength)
                .add()
                .append(new KeyedCodec<>("GuildAllowNameTagUpdates", Codec.BOOLEAN, false, true),
                    (i, s) -> i.guildAllowNameTagUpdates = s, i -> i.guildAllowNameTagUpdates)
                .add()
            .append(new KeyedCodec<>("GuildDefaultJoinPolicy", new EnumCodec<>(GuildJoinPolicy.class), false, true),
                    (i, s) -> i.guildDefaultJoinPolicy = s, i -> i.guildDefaultJoinPolicy)
            .add()
            .append(new KeyedCodec<>("GuildPersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.guildPersistenceEnabled = s, i -> i.guildPersistenceEnabled)
            .add()
            .append(new KeyedCodec<>("GuildPersistenceIntervalSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.guildPersistenceIntervalSeconds = s, i -> i.guildPersistenceIntervalSeconds)
            .add()
            .build();

    private boolean enabled;
    private boolean guildEnabled;
    private int guildMaxMembers;
    private int guildInviteExpirySeconds;
    private int guildApplicationExpirySeconds;
    private int guildNameMinLength;
    private int guildNameMaxLength;
    private int guildTagMinLength;
    private int guildTagMaxLength;
    private int guildDescriptionMaxLength;
    private int guildApplicationMessageMaxLength;
    private int guildMotdMaxLength;
    private boolean guildAllowNameTagUpdates;
    private GuildJoinPolicy guildDefaultJoinPolicy;
    private boolean guildPersistenceEnabled;
    private int guildPersistenceIntervalSeconds;

    public GuildModSettings() {
        this.enabled = true;
        this.guildEnabled = true;
        this.guildMaxMembers = 50;
        this.guildInviteExpirySeconds = 3600;
        this.guildApplicationExpirySeconds = 604800;
        this.guildNameMinLength = 3;
        this.guildNameMaxLength = 24;
        this.guildTagMinLength = 2;
        this.guildTagMaxLength = 5;
        this.guildDescriptionMaxLength = 256;
        this.guildApplicationMessageMaxLength = 512;
        this.guildMotdMaxLength = 128;
        this.guildAllowNameTagUpdates = false;
        this.guildDefaultJoinPolicy = GuildJoinPolicy.INVITE_ONLY;
        this.guildPersistenceEnabled = true;
        this.guildPersistenceIntervalSeconds = 120;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isGuildEnabled() {
        return guildEnabled;
    }

    public int getGuildMaxMembers() {
        return guildMaxMembers;
    }

    public int getGuildInviteExpirySeconds() {
        return guildInviteExpirySeconds;
    }

    public int getGuildApplicationExpirySeconds() {
        return guildApplicationExpirySeconds;
    }

    public int getGuildNameMinLength() {
        return guildNameMinLength;
    }

    public int getGuildNameMaxLength() {
        return guildNameMaxLength;
    }

    public int getGuildTagMinLength() {
        return guildTagMinLength;
    }

    public int getGuildTagMaxLength() {
        return guildTagMaxLength;
    }

    public int getGuildDescriptionMaxLength() {
        return guildDescriptionMaxLength;
    }

    public int getGuildApplicationMessageMaxLength() {
        return guildApplicationMessageMaxLength;
    }

    public int getGuildMotdMaxLength() {
        return guildMotdMaxLength;
    }

    public boolean isGuildAllowNameTagUpdates() {
        return guildAllowNameTagUpdates;
    }

    public GuildJoinPolicy getGuildDefaultJoinPolicy() {
        return guildDefaultJoinPolicy;
    }

    public boolean isGuildPersistenceEnabled() {
        return guildPersistenceEnabled;
    }

    public int getGuildPersistenceIntervalSeconds() {
        return guildPersistenceIntervalSeconds;
    }
}
