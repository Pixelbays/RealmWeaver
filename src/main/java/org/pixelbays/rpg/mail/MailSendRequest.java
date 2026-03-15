package org.pixelbays.rpg.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;

import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Caller is responsible for detaching item attachments from the sender before enqueueing mail.
 */
public class MailSendRequest {

    private final UUID senderAccountId;
    private final String senderName;
    private final UUID recipientAccountId;
    private final String subject;
    private final String body;
    private final List<ItemStack> itemAttachments;
    private final CurrencyAmountDefinition attachedCurrency;
    private final CurrencyAmountDefinition cashOnDelivery;

    public MailSendRequest(@Nonnull UUID senderAccountId,
            @Nullable String senderName,
            @Nonnull UUID recipientAccountId,
            @Nullable String subject,
            @Nullable String body,
            @Nullable List<ItemStack> itemAttachments,
            @Nullable CurrencyAmountDefinition attachedCurrency,
            @Nullable CurrencyAmountDefinition cashOnDelivery) {
        this.senderAccountId = senderAccountId;
        this.senderName = senderName == null ? "" : senderName;
        this.recipientAccountId = recipientAccountId;
        this.subject = subject == null ? "" : subject;
        this.body = body == null ? "" : body;
        this.itemAttachments = itemAttachments == null ? new ArrayList<>() : new ArrayList<>(itemAttachments);
        this.attachedCurrency = attachedCurrency == null ? new CurrencyAmountDefinition() : attachedCurrency;
        this.cashOnDelivery = cashOnDelivery == null ? new CurrencyAmountDefinition() : cashOnDelivery;
    }

    @Nonnull
    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    @Nonnull
    public String getSenderName() {
        return senderName;
    }

    @Nonnull
    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    @Nonnull
    public String getSubject() {
        return subject;
    }

    @Nonnull
    public String getBody() {
        return body;
    }

    @Nonnull
    public List<ItemStack> getItemAttachments() {
        return new ArrayList<>(itemAttachments);
    }

    @Nonnull
    public CurrencyAmountDefinition getAttachedCurrency() {
        return attachedCurrency;
    }

    @Nonnull
    public CurrencyAmountDefinition getCashOnDelivery() {
        return cashOnDelivery;
    }
}
