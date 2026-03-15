package org.pixelbays.rpg.mail.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.mail.MailAttachmentItem;
import org.pixelbays.rpg.mail.MailMessage;
import org.pixelbays.rpg.mail.MailOwnershipMode;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "null", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class MailData implements JsonAssetWithMap<String, DefaultAssetMap<String, MailData>> {

    private static final FunctionCodec<MailAttachmentItem[], List<MailAttachmentItem>> ATTACHMENT_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(MailAttachmentItem.CODEC, MailAttachmentItem[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(MailAttachmentItem[]::new));

    public static final AssetBuilderCodec<String, MailData> CODEC = AssetBuilderCodec.builder(
            MailData.class,
            MailData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("RecipientOwnerId", Codec.STRING, false, true),
                    (i, s) -> i.recipientOwnerId = s, i -> i.recipientOwnerId)
            .add()
            .append(new KeyedCodec<>("RecipientAccountId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.recipientAccountId = s, i -> i.recipientAccountId)
            .add()
            .append(new KeyedCodec<>("SenderOwnerId", Codec.STRING, false, true),
                    (i, s) -> i.senderOwnerId = s, i -> i.senderOwnerId)
            .add()
            .append(new KeyedCodec<>("SenderAccountId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.senderAccountId = s, i -> i.senderAccountId)
            .add()
            .append(new KeyedCodec<>("SenderName", Codec.STRING, false, true),
                    (i, s) -> i.senderName = s, i -> i.senderName)
            .add()
            .append(new KeyedCodec<>("Subject", Codec.STRING, false, true),
                    (i, s) -> i.subject = s, i -> i.subject)
            .add()
            .append(new KeyedCodec<>("Body", Codec.STRING, false, true),
                    (i, s) -> i.body = s, i -> i.body)
            .add()
            .append(new KeyedCodec<>("ItemAttachments", ATTACHMENT_LIST_CODEC, false, true),
                    (i, s) -> i.itemAttachments = s, i -> i.itemAttachments)
            .add()
            .append(new KeyedCodec<>("AttachedCurrency", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.attachedCurrency = s, i -> i.attachedCurrency)
            .add()
            .append(new KeyedCodec<>("CashOnDelivery", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.cashOnDelivery = s, i -> i.cashOnDelivery)
            .add()
            .append(new KeyedCodec<>("OwnershipMode", new EnumCodec<>(MailOwnershipMode.class), false, true),
                    (i, s) -> i.ownershipMode = s, i -> i.ownershipMode)
            .add()
            .append(new KeyedCodec<>("CreatedAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.createdAtMillis = s, i -> i.createdAtMillis)
            .add()
            .append(new KeyedCodec<>("DeliverAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.deliverAtMillis = s, i -> i.deliverAtMillis)
            .add()
            .append(new KeyedCodec<>("ExpiresAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.expiresAtMillis = s, i -> i.expiresAtMillis)
            .add()
            .append(new KeyedCodec<>("Read", Codec.BOOLEAN, false, true),
                    (i, s) -> i.read = s, i -> i.read)
            .add()
            .append(new KeyedCodec<>("Claimed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.claimed = s, i -> i.claimed)
            .add()
            .append(new KeyedCodec<>("Deleted", Codec.BOOLEAN, false, true),
                    (i, s) -> i.deleted = s, i -> i.deleted)
            .add()
            .build();

    private static DefaultAssetMap<String, MailData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private String recipientOwnerId;
    private UUID recipientAccountId;
    private String senderOwnerId;
    private UUID senderAccountId;
    private String senderName;
    private String subject;
    private String body;
    private List<MailAttachmentItem> itemAttachments;
    private CurrencyAmountDefinition attachedCurrency;
    private CurrencyAmountDefinition cashOnDelivery;
    private MailOwnershipMode ownershipMode;
    private long createdAtMillis;
    private long deliverAtMillis;
    private long expiresAtMillis;
    private boolean read;
    private boolean claimed;
    private boolean deleted;

    public MailData() {
        this.id = "";
        this.recipientOwnerId = "";
        this.recipientAccountId = new UUID(0L, 0L);
        this.senderOwnerId = "";
        this.senderAccountId = new UUID(0L, 0L);
        this.senderName = "";
        this.subject = "";
        this.body = "";
        this.itemAttachments = new ArrayList<>();
        this.attachedCurrency = new CurrencyAmountDefinition();
        this.cashOnDelivery = new CurrencyAmountDefinition();
        this.ownershipMode = MailOwnershipMode.Account;
        this.createdAtMillis = 0L;
        this.deliverAtMillis = 0L;
        this.expiresAtMillis = 0L;
        this.read = false;
        this.claimed = false;
        this.deleted = false;
    }

    public static DefaultAssetMap<String, MailData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(MailData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, MailData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public MailMessage toMailMessage() {
        return new MailMessage(
                parseUuid(id),
                recipientOwnerId == null ? "" : recipientOwnerId,
                recipientAccountId == null ? new UUID(0L, 0L) : recipientAccountId,
                senderOwnerId == null ? "" : senderOwnerId,
                senderAccountId == null ? new UUID(0L, 0L) : senderAccountId,
                senderName == null ? "" : senderName,
                subject == null ? "" : subject,
                body == null ? "" : body,
                itemAttachments == null ? List.of() : itemAttachments,
                attachedCurrency == null ? new CurrencyAmountDefinition() : attachedCurrency,
                cashOnDelivery == null ? new CurrencyAmountDefinition() : cashOnDelivery,
                ownershipMode == null ? MailOwnershipMode.Account : ownershipMode,
                createdAtMillis,
                deliverAtMillis,
                expiresAtMillis,
                read,
                claimed,
                deleted);
    }

    public static MailData fromMailMessage(MailMessage mailMessage) {
        MailData data = new MailData();
        data.id = mailMessage.getMessageId().toString();
        data.recipientOwnerId = mailMessage.getRecipientOwnerId();
        data.recipientAccountId = mailMessage.getRecipientAccountId();
        data.senderOwnerId = mailMessage.getSenderOwnerId();
        data.senderAccountId = mailMessage.getSenderAccountId();
        data.senderName = mailMessage.getSenderName();
        data.subject = mailMessage.getSubject();
        data.body = mailMessage.getBody();
        data.itemAttachments = mailMessage.getItemAttachments();
        data.attachedCurrency = mailMessage.getAttachedCurrency();
        data.cashOnDelivery = mailMessage.getCashOnDelivery();
        data.ownershipMode = mailMessage.getOwnershipMode();
        data.createdAtMillis = mailMessage.getCreatedAtMillis();
        data.deliverAtMillis = mailMessage.getDeliverAtMillis();
        data.expiresAtMillis = mailMessage.getExpiresAtMillis();
        data.read = mailMessage.isRead();
        data.claimed = mailMessage.isClaimed();
        data.deleted = mailMessage.isDeleted();
        return data;
    }

    private static UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new UUID(0L, 0L);
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }
}
