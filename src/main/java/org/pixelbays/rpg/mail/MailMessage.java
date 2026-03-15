package org.pixelbays.rpg.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;

import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings("null")
public class MailMessage {

    private final UUID messageId;
    private final String recipientOwnerId;
    private final UUID recipientAccountId;
    private final String senderOwnerId;
    private final UUID senderAccountId;
    private final String senderName;
    private final String subject;
    private final String body;
    private final List<MailAttachmentItem> itemAttachments;
    private final CurrencyAmountDefinition attachedCurrency;
    private final CurrencyAmountDefinition cashOnDelivery;
    private final MailOwnershipMode ownershipMode;
    private final long createdAtMillis;
    private final long deliverAtMillis;
    private final long expiresAtMillis;
    private boolean read;
    private boolean claimed;
    private boolean deleted;

    public MailMessage(@Nonnull UUID messageId,
            @Nonnull String recipientOwnerId,
            @Nonnull UUID recipientAccountId,
            @Nonnull String senderOwnerId,
            @Nonnull UUID senderAccountId,
            @Nonnull String senderName,
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull List<MailAttachmentItem> itemAttachments,
            @Nonnull CurrencyAmountDefinition attachedCurrency,
            @Nonnull CurrencyAmountDefinition cashOnDelivery,
            @Nonnull MailOwnershipMode ownershipMode,
            long createdAtMillis,
            long deliverAtMillis,
            long expiresAtMillis,
            boolean read,
            boolean claimed,
            boolean deleted) {
        this.messageId = messageId;
        this.recipientOwnerId = recipientOwnerId;
        this.recipientAccountId = recipientAccountId;
        this.senderOwnerId = senderOwnerId;
        this.senderAccountId = senderAccountId;
        this.senderName = senderName;
        this.subject = subject;
        this.body = body;
        this.itemAttachments = new ArrayList<>(itemAttachments);
        this.attachedCurrency = attachedCurrency;
        this.cashOnDelivery = cashOnDelivery;
        this.ownershipMode = ownershipMode;
        this.createdAtMillis = createdAtMillis;
        this.deliverAtMillis = deliverAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.read = read;
        this.claimed = claimed;
        this.deleted = deleted;
    }

    @Nonnull
    public UUID getMessageId() {
        return messageId == null ? new UUID(0L, 0L) : messageId;
    }

    @Nonnull
    public String getRecipientOwnerId() {
        return recipientOwnerId == null ? "" : recipientOwnerId;
    }

    @Nonnull
    public UUID getRecipientAccountId() {
        return recipientAccountId == null ? new UUID(0L, 0L) : recipientAccountId;
    }

    @Nonnull
    public String getSenderOwnerId() {
        return senderOwnerId == null ? "" : senderOwnerId;
    }

    @Nonnull
    public UUID getSenderAccountId() {
        return senderAccountId == null ? new UUID(0L, 0L) : senderAccountId;
    }

    @Nonnull
    public String getSenderName() {
        return senderName == null ? "" : senderName;
    }

    @Nonnull
    public String getSubject() {
        return subject == null ? "" : subject;
    }

    @Nonnull
    public String getBody() {
        return body == null ? "" : body;
    }

    @Nonnull
    public List<MailAttachmentItem> getItemAttachments() {
        return new ArrayList<>(itemAttachments);
    }

    @Nonnull
    public List<ItemStack> getAttachedItemStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (MailAttachmentItem attachment : itemAttachments) {
            if (attachment == null || attachment.getItemStack().isEmpty()) {
                continue;
            }
            stacks.add(attachment.getItemStack());
        }
        return stacks;
    }

    @Nonnull
    public CurrencyAmountDefinition getAttachedCurrency() {
        return attachedCurrency == null ? new CurrencyAmountDefinition() : attachedCurrency;
    }

    @Nonnull
    public CurrencyAmountDefinition getCashOnDelivery() {
        return cashOnDelivery == null ? new CurrencyAmountDefinition() : cashOnDelivery;
    }

    @Nonnull
    public MailOwnershipMode getOwnershipMode() {
        return ownershipMode == null ? MailOwnershipMode.Account : ownershipMode;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getDeliverAtMillis() {
        return deliverAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean hasItemAttachments() {
        for (MailAttachmentItem attachment : itemAttachments) {
            if (attachment != null && !attachment.getItemStack().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCurrencyAttachment() {
        return !getAttachedCurrency().isFree();
    }

    public boolean hasCashOnDelivery() {
        return !getCashOnDelivery().isFree();
    }

    public boolean hasAnyAttachment() {
        return hasItemAttachments() || hasCurrencyAttachment();
    }

    public boolean isSystemMail() {
        return getSenderOwnerId().equalsIgnoreCase("system")
                || getSenderAccountId().equals(new UUID(0L, 0L));
    }

    public boolean isDelivered(long nowMillis) {
        return nowMillis >= deliverAtMillis;
    }

    public boolean isExpired(long nowMillis) {
        return expiresAtMillis > 0L && nowMillis >= expiresAtMillis;
    }
}
