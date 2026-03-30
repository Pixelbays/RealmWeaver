package org.pixelbays.rpg.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.mail.config.MailData;
import org.pixelbays.rpg.mail.config.settings.MailModSettings;

import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings({ "null", "unused" })
public class MailManager {

    private static final UUID SYSTEM_ACCOUNT_ID = new UUID(0L, 0L);
    private static final String SYSTEM_OWNER_ID = "system";
    private static final String SYSTEM_SENDER_NAME = "System";

    private final Map<UUID, MailMessage> mailById = new ConcurrentHashMap<>();
    private final Map<String, Long> senderCooldowns = new ConcurrentHashMap<>();
    private final MailPersistence persistence = new MailPersistence();

    public void loadFromAssets() {
        clear();

        RpgModConfig config = resolveConfig();
        if (config != null && !config.isMailEnabled()) {
            return;
        }

        for (MailData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }

            MailMessage mailMessage = data.toMailMessage();
            mailById.put(mailMessage.getMessageId(), mailMessage);
        }
    }

    public void clear() {
        mailById.clear();
        senderCooldowns.clear();
    }

    @Nonnull
    public MailActionResult sendMail(@Nonnull MailSendRequest request) {
        return queueMail(request, false, false, null, null, null);
        }

        @Nonnull
        public MailActionResult sendSystemMail(@Nonnull UUID recipientAccountId,
            @Nullable String senderName,
            @Nullable String subject,
            @Nullable String body,
            @Nullable List<ItemStack> itemAttachments,
            @Nullable CurrencyAmountDefinition attachedCurrency,
            @Nullable CurrencyAmountDefinition cashOnDelivery) {
        MailSendRequest request = new MailSendRequest(
            SYSTEM_ACCOUNT_ID,
            senderName == null || senderName.isBlank() ? SYSTEM_SENDER_NAME : senderName,
            recipientAccountId,
            subject,
            body,
            itemAttachments,
            attachedCurrency,
            cashOnDelivery);
        return queueMail(request, true, true, SYSTEM_OWNER_ID, SYSTEM_ACCOUNT_ID,
            senderName == null || senderName.isBlank() ? SYSTEM_SENDER_NAME : senderName);
        }

        @Nonnull
        public MailBroadcastResult sendMailToAllKnownPlayers(@Nonnull MailSendRequest request) {
        return sendMailToRecipients(resolveKnownRecipientAccountIds(), request, true, false, null, null, null);
        }

        @Nonnull
        public MailBroadcastResult sendSystemMailToAllKnownPlayers(@Nullable String senderName,
            @Nullable String subject,
            @Nullable String body,
            @Nullable List<ItemStack> itemAttachments,
            @Nullable CurrencyAmountDefinition attachedCurrency,
            @Nullable CurrencyAmountDefinition cashOnDelivery) {
        MailSendRequest request = new MailSendRequest(
            SYSTEM_ACCOUNT_ID,
            senderName == null || senderName.isBlank() ? SYSTEM_SENDER_NAME : senderName,
            SYSTEM_ACCOUNT_ID,
            subject,
            body,
            itemAttachments,
            attachedCurrency,
            cashOnDelivery);
        return sendMailToRecipients(resolveKnownRecipientAccountIds(),
            request,
            true,
            true,
            SYSTEM_OWNER_ID,
            SYSTEM_ACCOUNT_ID,
            senderName == null || senderName.isBlank() ? SYSTEM_SENDER_NAME : senderName);
        }

        @Nonnull
        private MailActionResult queueMail(@Nonnull MailSendRequest request,
            boolean bypassRateLimit,
            boolean bypassCurrencyReserve,
            @Nullable String senderOwnerIdOverride,
            @Nullable UUID senderAccountIdOverride,
            @Nullable String senderNameOverride) {
        RpgModConfig config = resolveConfig();
        MailModSettings settings = config == null ? new MailModSettings() : config.getMailSettings();
        if (!settings.isEnabled() || !settings.isMailEnabled()) {
            return MailActionResult.failure("Mail is disabled.");
        }

        String subject = sanitizeText(request.getSubject());
        String body = sanitizeText(request.getBody());
        List<ItemStack> requestedItems = sanitizeItems(request.getItemAttachments());
        CurrencyAmountDefinition attachedCurrency = request.getAttachedCurrency();
        CurrencyAmountDefinition cashOnDelivery = request.getCashOnDelivery();

        if (subject.length() > Math.max(1, settings.getMaxSubjectLength())) {
            return MailActionResult.failure("Mail subject is too long.");
        }
        if (body.length() > Math.max(1, settings.getMaxBodyLength())) {
            return MailActionResult.failure("Mail body is too long.");
        }
        if (requestedItems.size() > Math.max(0, settings.getMaxItemAttachments())) {
            return MailActionResult.failure("Too many item attachments.");
        }
        if (!requestedItems.isEmpty() && !settings.isAllowItemAttachments()) {
            return MailActionResult.failure("Item attachments are disabled.");
        }
        if (!attachedCurrency.isFree() && !settings.isAllowCurrencyAttachments()) {
            return MailActionResult.failure("Currency attachments are disabled.");
        }
        if (!cashOnDelivery.isFree() && !settings.isAllowCashOnDelivery()) {
            return MailActionResult.failure("Cash on delivery is disabled.");
        }
        if (!cashOnDelivery.isFree() && requestedItems.isEmpty() && attachedCurrency.isFree()) {
            return MailActionResult.failure("Cash on delivery requires an attachment.");
        }
        if (body.isBlank() && requestedItems.isEmpty() && attachedCurrency.isFree()) {
            return MailActionResult.failure("Mail must contain text, items, or currency.");
        }

        MailOwnershipMode ownershipMode = settings.getOwnershipMode();
        UUID senderAccountId = senderAccountIdOverride == null ? request.getSenderAccountId() : senderAccountIdOverride;
        String senderOwnerId = senderOwnerIdOverride == null
            ? resolveMailboxOwnerId(senderAccountId, ownershipMode)
            : senderOwnerIdOverride;
        String recipientOwnerId = resolveMailboxOwnerId(request.getRecipientAccountId(), ownershipMode);
        if (recipientOwnerId.isBlank()) {
            return MailActionResult.failure("Recipient mailbox could not be resolved.");
        }

        long now = System.currentTimeMillis();
        long cooldownExpiresAt = bypassRateLimit ? 0L : getCooldownExpiresAt(senderOwnerId, settings);
        if (cooldownExpiresAt > now) {
            return MailActionResult.failure("Mail is on cooldown.");
        }

        if (!bypassCurrencyReserve) {
            CurrencyActionResult reserveCurrencyResult = reserveAttachedCurrency(
                ownershipMode,
                senderOwnerId,
                attachedCurrency);
            if (!reserveCurrencyResult.isSuccess()) {
            return MailActionResult.failure(reserveCurrencyResult.getMessage());
            }
        }

        long deliveryDelayMillis = resolveDeliveryDelayMillis(settings,
            senderAccountId,
                request.getRecipientAccountId());
        long deliverAtMillis = now + deliveryDelayMillis;
        long expiresAtMillis = resolveExpiresAtMillis(settings, deliverAtMillis);
        MailMessage mailMessage = new MailMessage(
                UUID.randomUUID(),
                recipientOwnerId,
                request.getRecipientAccountId(),
                senderOwnerId,
            senderAccountId,
            sanitizeText(senderNameOverride == null ? request.getSenderName() : senderNameOverride),
                subject,
                body,
                wrapAttachments(requestedItems),
                attachedCurrency,
                cashOnDelivery,
                ownershipMode,
                now,
                deliverAtMillis,
                expiresAtMillis,
                false,
                false,
                false);

        mailById.put(mailMessage.getMessageId(), mailMessage);
        saveMailIfEnabled(mailMessage, settings);
        if (!bypassRateLimit) {
            rememberSenderCooldown(senderOwnerId, settings, now);
        }
        return MailActionResult.success("Mail queued.", mailMessage);
    }

    @Nonnull
    private MailBroadcastResult sendMailToRecipients(@Nonnull Collection<UUID> recipientAccountIds,
            @Nonnull MailSendRequest request,
            boolean bypassRateLimit,
            boolean bypassCurrencyReserve,
            @Nullable String senderOwnerIdOverride,
            @Nullable UUID senderAccountIdOverride,
            @Nullable String senderNameOverride) {
        int attempted = 0;
        int success = 0;
        int failed = 0;
        String lastFailure = "";

        Set<UUID> uniqueRecipients = new HashSet<>(recipientAccountIds);
        uniqueRecipients.removeIf(recipientId -> recipientId == null || recipientId.equals(request.getSenderAccountId()));

        for (UUID recipientAccountId : uniqueRecipients) {
            attempted++;
            MailActionResult result = queueMail(
                    new MailSendRequest(
                            senderAccountIdOverride == null ? request.getSenderAccountId() : senderAccountIdOverride,
                            senderNameOverride == null ? request.getSenderName() : senderNameOverride,
                            recipientAccountId,
                            request.getSubject(),
                            request.getBody(),
                            request.getItemAttachments(),
                            request.getAttachedCurrency(),
                            request.getCashOnDelivery()),
                    bypassRateLimit,
                    bypassCurrencyReserve,
                    senderOwnerIdOverride,
                    senderAccountIdOverride,
                    senderNameOverride);
            if (result.isSuccess()) {
                success++;
            } else {
                failed++;
                lastFailure = result.getMessage();
            }
        }

        return new MailBroadcastResult(attempted, success, failed, lastFailure);
    }

    @Nonnull
    public List<MailMessage> getInbox(@Nonnull UUID accountId) {
        long now = System.currentTimeMillis();
        Set<String> candidateOwners = resolveMailboxOwnerCandidates(accountId);
        List<MailMessage> results = new ArrayList<>();
        for (MailMessage mailMessage : mailById.values()) {
            if (!belongsToMailbox(mailMessage, accountId, candidateOwners)) {
                continue;
            }
            if (mailMessage.isDeleted() || mailMessage.isExpired(now)) {
                continue;
            }
            results.add(mailMessage);
        }
        results.sort(Comparator.comparingLong(MailMessage::getDeliverAtMillis).reversed());
        return results;
    }

    @Nullable
    public MailMessage getMail(@Nonnull UUID accountId, @Nonnull UUID messageId) {
        MailMessage mailMessage = mailById.get(messageId);
        if (mailMessage == null) {
            return null;
        }
        if (!belongsToMailbox(mailMessage, accountId, resolveMailboxOwnerCandidates(accountId))) {
            return null;
        }
        if (mailMessage.isDeleted() || mailMessage.isExpired(System.currentTimeMillis())) {
            return null;
        }
        return mailMessage;
    }

    @Nonnull
    public MailActionResult markRead(@Nonnull UUID accountId, @Nonnull UUID messageId) {
        MailMessage mailMessage = getMail(accountId, messageId);
        if (mailMessage == null) {
            return MailActionResult.failure("Mail not found.");
        }
        if (!mailMessage.isDelivered(System.currentTimeMillis())) {
            return MailActionResult.failure("Mail has not been delivered yet.");
        }
        mailMessage.setRead(true);
        saveMailIfEnabled(mailMessage, currentSettings());
        return MailActionResult.success("Mail marked as read.", mailMessage);
    }

    public boolean hasUndeliveredMail(@Nonnull UUID accountId) {
        long now = System.currentTimeMillis();
        for (MailMessage mailMessage : getInbox(accountId)) {
            if (!mailMessage.isDelivered(now)) {
                return true;
            }
        }
        return false;
    }

    private CurrencyActionResult reserveAttachedCurrency(MailOwnershipMode ownershipMode,
            String senderOwnerId,
            CurrencyAmountDefinition attachedCurrency) {
        if (attachedCurrency == null || attachedCurrency.isFree()) {
            return CurrencyActionResult.success("No attached currency.", "", 0L);
        }

        CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
        CurrencyScope scope = ownershipMode == MailOwnershipMode.Character ? CurrencyScope.Character : CurrencyScope.Account;
        return currencyManager.removeBalance(scope,
                senderOwnerId,
                attachedCurrency.getCurrencyId(),
                attachedCurrency.getAmount());
    }

    private long resolveDeliveryDelayMillis(MailModSettings settings, UUID senderAccountId, UUID recipientAccountId) {
        int delayMinutes = Math.max(0, settings.getBaseDeliveryDelayMinutes());
        if (delayMinutes == 0) {
            return 0L;
        }

        if (shareGuild(senderAccountId, recipientAccountId)) {
            delayMinutes -= Math.max(0, settings.getGuildDeliveryReductionMinutes());
        }
        if (areFriends(senderAccountId, recipientAccountId)) {
            delayMinutes -= Math.max(0, settings.getFriendDeliveryReductionMinutes());
        }
        return Math.max(0, delayMinutes) * 60_000L;
    }

    private long resolveExpiresAtMillis(MailModSettings settings, long deliverAtMillis) {
        int expiryDays = settings.getMailExpiryDays();
        if (expiryDays <= 0) {
            return 0L;
        }
        return deliverAtMillis + (expiryDays * 86_400_000L);
    }

    private boolean shareGuild(UUID senderAccountId, UUID recipientAccountId) {
        Guild senderGuild = resolveGuildForMember(senderAccountId);
        Guild recipientGuild = resolveGuildForMember(recipientAccountId);
        return senderGuild != null && recipientGuild != null && senderGuild.getId().equals(recipientGuild.getId());
    }

    private boolean areFriends(UUID senderAccountId, UUID recipientAccountId) {
        return false;
    }

    @Nullable
    private Guild resolveGuildForMember(@Nonnull UUID accountId) {
        try {
            Object plugin = Realmweavers.get();
            Method getGuildManager = plugin.getClass().getMethod("getGuildManager");
            Object guildManager = getGuildManager.invoke(plugin);
            if (guildManager == null) {
                return null;
            }
            Method getGuildForMember = guildManager.getClass().getMethod("getGuildForMember", UUID.class);
            Object guild = getGuildForMember.invoke(guildManager, accountId);
            return guild instanceof Guild resolved ? resolved : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean belongsToMailbox(MailMessage mailMessage, UUID accountId, Set<String> candidateOwners) {
        if (mailMessage.getOwnershipMode() == MailOwnershipMode.Account) {
            return mailMessage.getRecipientAccountId().equals(accountId)
                    || candidateOwners.contains(mailMessage.getRecipientOwnerId());
        }
        return candidateOwners.contains(mailMessage.getRecipientOwnerId());
    }

    private Set<String> resolveMailboxOwnerCandidates(UUID accountId) {
        Set<String> owners = new HashSet<>();
        owners.add(accountId.toString());

        CharacterManager characterManager = Realmweavers.get().getCharacterManager();
        String characterOwnerId = characterManager.resolveCharacterOwnerId(accountId);
        if (!characterOwnerId.isBlank()) {
            owners.add(characterOwnerId);
        }
        return owners;
    }

    private String resolveMailboxOwnerId(UUID accountId, MailOwnershipMode ownershipMode) {
        if (ownershipMode == MailOwnershipMode.Character) {
            CharacterManager characterManager = Realmweavers.get().getCharacterManager();
            String characterOwnerId = characterManager.resolveCharacterOwnerId(accountId);
            if (!characterOwnerId.isBlank()) {
                return characterOwnerId;
            }
        }
        return accountId.toString();
    }

    private long getCooldownExpiresAt(String senderOwnerId, MailModSettings settings) {
        if (senderOwnerId == null || senderOwnerId.isBlank()) {
            return 0L;
        }
        int rateLimitMinutes = settings.getRateLimitMinutes();
        if (rateLimitMinutes <= 0) {
            return 0L;
        }
        Long lastSentAt = senderCooldowns.get(senderOwnerId);
        if (lastSentAt == null) {
            return 0L;
        }
        return lastSentAt + (rateLimitMinutes * 60_000L);
    }

    private void rememberSenderCooldown(String senderOwnerId, MailModSettings settings, long now) {
        if (senderOwnerId == null || senderOwnerId.isBlank() || settings.getRateLimitMinutes() <= 0) {
            return;
        }
        senderCooldowns.put(senderOwnerId, now);
    }

    @Nonnull
    private List<UUID> resolveKnownRecipientAccountIds() {
        CharacterManager characterManager = Realmweavers.get().getCharacterManager();
        return characterManager == null ? List.of() : characterManager.getKnownAccountIds();
    }

    private List<ItemStack> sanitizeItems(List<ItemStack> requestedItems) {
        List<ItemStack> sanitized = new ArrayList<>();
        if (requestedItems == null) {
            return sanitized;
        }
        for (ItemStack itemStack : requestedItems) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            sanitized.add(itemStack);
        }
        return sanitized;
    }

    private List<MailAttachmentItem> wrapAttachments(List<ItemStack> itemStacks) {
        List<MailAttachmentItem> wrapped = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            wrapped.add(new MailAttachmentItem(itemStack));
        }
        return wrapped;
    }

    private String sanitizeText(@Nullable String text) {
        return text == null ? "" : text.trim();
    }

    private void saveMailIfEnabled(MailMessage mailMessage, MailModSettings settings) {
        if (mailMessage == null || settings == null || !settings.isPersistenceEnabled()) {
            return;
        }
        persistence.saveMail(mailMessage);
    }

    private MailModSettings currentSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new MailModSettings() : config.getMailSettings();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config == null) {
            config = assetMap.getAsset("default");
        }
        if (config == null) {
            RpgLogging.debugDeveloper("[Mail] RpgModConfig not found; using defaults.");
        }
        return config;
    }
}
