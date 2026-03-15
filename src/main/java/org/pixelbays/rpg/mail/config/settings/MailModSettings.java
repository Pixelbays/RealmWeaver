package org.pixelbays.rpg.mail.config.settings;

import org.pixelbays.rpg.mail.MailOwnershipMode;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

@SuppressWarnings("deprecation")
public class MailModSettings {

    public static final BuilderCodec<MailModSettings> CODEC = BuilderCodec
            .builder(MailModSettings.class, MailModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("MailEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.mailEnabled = s, i -> i.mailEnabled)
            .add()
            .append(new KeyedCodec<>("OwnershipMode", new EnumCodec<>(MailOwnershipMode.class), false, true),
                    (i, s) -> i.ownershipMode = s, i -> i.ownershipMode)
            .add()
            .append(new KeyedCodec<>("AllowItemAttachments", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowItemAttachments = s, i -> i.allowItemAttachments)
            .add()
            .append(new KeyedCodec<>("AllowCurrencyAttachments", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowCurrencyAttachments = s, i -> i.allowCurrencyAttachments)
            .add()
            .append(new KeyedCodec<>("AllowCashOnDelivery", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowCashOnDelivery = s, i -> i.allowCashOnDelivery)
            .add()
            .append(new KeyedCodec<>("RateLimitMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.rateLimitMinutes = s, i -> i.rateLimitMinutes)
            .add()
            .append(new KeyedCodec<>("BaseDeliveryDelayMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.baseDeliveryDelayMinutes = s, i -> i.baseDeliveryDelayMinutes)
            .add()
            .append(new KeyedCodec<>("GuildDeliveryReductionMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.guildDeliveryReductionMinutes = s, i -> i.guildDeliveryReductionMinutes)
            .add()
            .append(new KeyedCodec<>("FriendDeliveryReductionMinutes", Codec.INTEGER, false, true),
                    (i, s) -> i.friendDeliveryReductionMinutes = s, i -> i.friendDeliveryReductionMinutes)
            .add()
            .append(new KeyedCodec<>("MaxSubjectLength", Codec.INTEGER, false, true),
                    (i, s) -> i.maxSubjectLength = s, i -> i.maxSubjectLength)
            .add()
            .append(new KeyedCodec<>("MaxBodyLength", Codec.INTEGER, false, true),
                    (i, s) -> i.maxBodyLength = s, i -> i.maxBodyLength)
            .add()
            .append(new KeyedCodec<>("MaxItemAttachments", Codec.INTEGER, false, true),
                    (i, s) -> i.maxItemAttachments = s, i -> i.maxItemAttachments)
            .add()
            .append(new KeyedCodec<>("MailExpiryDays", Codec.INTEGER, false, true),
                    (i, s) -> i.mailExpiryDays = s, i -> i.mailExpiryDays)
            .add()
            .append(new KeyedCodec<>("PersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.persistenceEnabled = s, i -> i.persistenceEnabled)
            .add()
            .build();

    private boolean enabled;
    private boolean mailEnabled;
    private MailOwnershipMode ownershipMode;
    private boolean allowItemAttachments;
    private boolean allowCurrencyAttachments;
    private boolean allowCashOnDelivery;
    private int rateLimitMinutes;
    private int baseDeliveryDelayMinutes;
    private int guildDeliveryReductionMinutes;
    private int friendDeliveryReductionMinutes;
    private int maxSubjectLength;
    private int maxBodyLength;
    private int maxItemAttachments;
    private int mailExpiryDays;
    private boolean persistenceEnabled;

    public MailModSettings() {
        this.enabled = true;
        this.mailEnabled = true;
        this.ownershipMode = MailOwnershipMode.Account;
        this.allowItemAttachments = true;
        this.allowCurrencyAttachments = true;
        this.allowCashOnDelivery = true;
        this.rateLimitMinutes = 0;
        this.baseDeliveryDelayMinutes = 0;
        this.guildDeliveryReductionMinutes = 0;
        this.friendDeliveryReductionMinutes = 0;
        this.maxSubjectLength = 64;
        this.maxBodyLength = 2000;
        this.maxItemAttachments = 6;
        this.mailExpiryDays = 30;
        this.persistenceEnabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public MailOwnershipMode getOwnershipMode() {
        return ownershipMode == null ? MailOwnershipMode.Account : ownershipMode;
    }

    public boolean isAllowItemAttachments() {
        return allowItemAttachments;
    }

    public boolean isAllowCurrencyAttachments() {
        return allowCurrencyAttachments;
    }

    public boolean isAllowCashOnDelivery() {
        return allowCashOnDelivery;
    }

    public int getRateLimitMinutes() {
        return rateLimitMinutes;
    }

    public int getBaseDeliveryDelayMinutes() {
        return baseDeliveryDelayMinutes;
    }

    public int getGuildDeliveryReductionMinutes() {
        return guildDeliveryReductionMinutes;
    }

    public int getFriendDeliveryReductionMinutes() {
        return friendDeliveryReductionMinutes;
    }

    public int getMaxSubjectLength() {
        return maxSubjectLength;
    }

    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    public int getMaxItemAttachments() {
        return maxItemAttachments;
    }

    public int getMailExpiryDays() {
        return mailExpiryDays;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }
}
