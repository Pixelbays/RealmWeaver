package org.pixelbays.rpg.mail.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.mail.MailActionResult;
import org.pixelbays.rpg.mail.MailManager;
import org.pixelbays.rpg.mail.MailMessage;
import org.pixelbays.rpg.mail.MailSendRequest;
import org.pixelbays.rpg.mail.command.MailCommandUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class MailPage extends CustomUIPage {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String SUMMARY_LABEL = "#SummaryLabel";
    private static final String INBOX_LABEL = "#InboxLabel";
    private static final String DETAIL_HEADER_LABEL = "#DetailHeaderLabel";
    private static final String DETAIL_META_LABEL = "#DetailMetaLabel";
    private static final String DETAIL_BODY_LABEL = "#DetailBodyLabel";

    private static final String SELECT_INDEX_FIELD = "#SelectIndexField";
    private static final String RECIPIENT_FIELD = "#RecipientField";
    private static final String SUBJECT_FIELD = "#SubjectField";
    private static final String BODY_FIELD = "#BodyField";

    private static final String REFRESH_BUTTON = "#RefreshButton";
    private static final String SELECT_BUTTON = "#SelectButton";
    private static final String MARK_READ_BUTTON = "#MarkReadButton";
    private static final String SEND_BUTTON = "#SendButton";

    private final MailManager mailManager;
    @Nullable
    private UUID selectedMessageId;

    public MailPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.mailManager = Realmweavers.get().getMailManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/MailPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null, false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String selectedIndex = extractString(rawData, "@SelectedIndex");
        String recipientName = extractString(rawData, "@Recipient");
        String subject = extractString(rawData, "@Subject");
        String body = extractString(rawData, "@Body");

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, selectedIndex, recipientName, subject, body));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String selectedIndex,
            @Nullable String recipientName,
            @Nullable String subject,
            @Nullable String body) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef currentPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || currentPlayerRef == null) {
            return;
        }

        MailActionResult sendResult = null;
        Message statusMessage = switch (action) {
            case "Refresh" -> Message.translation("pixelbays.rpg.mail.ui.status.refreshed");
            case "Select" -> handleSelect(currentPlayerRef, selectedIndex);
            case "MarkRead" -> handleMarkRead(currentPlayerRef);
            case "Send" -> {
                sendResult = handleSend(currentPlayerRef, recipientName, subject, body);
                yield MailCommandUtil.managerResultMessage(sendResult.getMessage());
            }
            default -> Message.translation("pixelbays.rpg.common.unknownError");
        };
        boolean clearCompose = sendResult != null && sendResult.isSuccess();

        if (selectedMessageId != null && mailManager.getMail(currentPlayerRef.getUuid(), selectedMessageId) == null) {
            selectedMessageId = null;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage, clearCompose);
        sendUpdate(commandBuilder);
    }

    @Nonnull
    private Message handleSelect(@Nonnull PlayerRef currentPlayerRef, @Nullable String selectedIndex) {
        List<MailMessage> inbox = mailManager.getInbox(currentPlayerRef.getUuid());
        Integer index = parseSelectionIndex(selectedIndex);
        if (index == null || index < 1 || index > inbox.size()) {
            return Message.translation("pixelbays.rpg.mail.ui.error.invalidSelection");
        }

        MailMessage selectedMail = inbox.get(index - 1);
        selectedMessageId = selectedMail.getMessageId();
        return Message.translation("pixelbays.rpg.mail.ui.status.selected")
                .param("subject", displaySubject(selectedMail));
    }

    @Nonnull
    private Message handleMarkRead(@Nonnull PlayerRef currentPlayerRef) {
        if (selectedMessageId == null) {
            return Message.translation("pixelbays.rpg.mail.ui.error.selectMailFirst");
        }

        MailActionResult result = mailManager.markRead(currentPlayerRef.getUuid(), selectedMessageId);
        return MailCommandUtil.managerResultMessage(result.getMessage());
    }

    @Nonnull
    private MailActionResult handleSend(@Nonnull PlayerRef currentPlayerRef,
            @Nullable String recipientName,
            @Nullable String subject,
            @Nullable String body) {
        if (recipientName == null || recipientName.isBlank()) {
            return MailActionResult.failure("Recipient required.");
        }

        UUID recipientAccountId = MailCommandUtil.resolveRecipientAccountId(recipientName.trim());
        if (recipientAccountId == null) {
            return MailActionResult.failure("Player not found.");
        }

        return mailManager.sendMail(new MailSendRequest(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername(),
                recipientAccountId,
                subject,
                body,
                List.of(),
                new CurrencyAmountDefinition(),
                new CurrencyAmountDefinition()));
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                REFRESH_BUTTON,
                new EventData().append("Action", "Refresh"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SELECT_BUTTON,
                new EventData().append("Action", "Select")
                        .append("@SelectedIndex", SELECT_INDEX_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                MARK_READ_BUTTON,
                new EventData().append("Action", "MarkRead"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SEND_BUTTON,
                new EventData().append("Action", "Send")
                        .append("@Recipient", RECIPIENT_FIELD + ".Value")
                        .append("@Subject", SUBJECT_FIELD + ".Value")
                        .append("@Body", BODY_FIELD + ".Value"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nullable Message statusMessage,
            boolean clearCompose) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        List<MailMessage> inbox = mailManager.getInbox(playerRef.getUuid());
        commandBuilder.set(SUMMARY_LABEL + ".Text", buildSummaryText(inbox));
        commandBuilder.set(INBOX_LABEL + ".Text", buildInboxText(inbox));

        MailMessage selectedMail = resolveSelectedMail(inbox);
        int selectedIndex = selectedMail == null ? -1 : inbox.indexOf(selectedMail) + 1;
        if (selectedIndex > 0) {
            commandBuilder.set(SELECT_INDEX_FIELD + ".Value", Integer.toString(selectedIndex));
        } else {
            commandBuilder.set(SELECT_INDEX_FIELD + ".Value", "");
        }

        if (selectedMail == null) {
            commandBuilder.set(DETAIL_HEADER_LABEL + ".Text", rawText("pixelbays.rpg.mail.ui.noSelectionTitle", "No mail selected"));
            commandBuilder.set(DETAIL_META_LABEL + ".Text", rawText("pixelbays.rpg.mail.ui.noSelectionBody", "Select a mail from the inbox list to inspect it."));
            commandBuilder.set(DETAIL_BODY_LABEL + ".Text", "");
        } else {
            commandBuilder.set(DETAIL_HEADER_LABEL + ".Text", displaySubject(selectedMail));
            commandBuilder.set(DETAIL_META_LABEL + ".Text", buildDetailMetaText(selectedMail));
            commandBuilder.set(DETAIL_BODY_LABEL + ".Text", buildDetailBodyText(selectedMail));
        }

        if (clearCompose) {
            commandBuilder.set(RECIPIENT_FIELD + ".Value", "");
            commandBuilder.set(SUBJECT_FIELD + ".Value", "");
            commandBuilder.set(BODY_FIELD + ".Value", "");
        }
    }

    @Nullable
    private MailMessage resolveSelectedMail(@Nonnull List<MailMessage> inbox) {
        if (selectedMessageId == null) {
            return null;
        }

        for (MailMessage mailMessage : inbox) {
            if (mailMessage.getMessageId().equals(selectedMessageId)) {
                return mailMessage;
            }
        }

        selectedMessageId = null;
        return null;
    }

    @Nonnull
    private String buildSummaryText(@Nonnull List<MailMessage> inbox) {
        long now = System.currentTimeMillis();
        int unread = 0;
        int incoming = 0;
        for (MailMessage mailMessage : inbox) {
            if (!mailMessage.isDelivered(now)) {
                incoming++;
            }
            if (mailMessage.isDelivered(now) && !mailMessage.isRead()) {
                unread++;
            }
        }

        return rawText("pixelbays.rpg.mail.ui.summaryTemplate",
                "Total: {total}\\nUnread: {unread}\\nIn Transit: {incoming}")
                .replace("{total}", Integer.toString(inbox.size()))
                .replace("{unread}", Integer.toString(unread))
                .replace("{incoming}", Integer.toString(incoming));
    }

    @Nonnull
    private String buildInboxText(@Nonnull List<MailMessage> inbox) {
        if (inbox.isEmpty()) {
            return rawText("pixelbays.rpg.mail.ui.emptyInbox", "Your inbox is empty.");
        }

        long now = System.currentTimeMillis();
        String unreadLabel = rawText("pixelbays.rpg.mail.ui.unread", "UNREAD");
        String incomingLabel = rawText("pixelbays.rpg.mail.ui.inTransit", "IN TRANSIT");
        String readLabel = rawText("pixelbays.rpg.mail.ui.read", "READ");

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < inbox.size(); index++) {
            MailMessage mailMessage = inbox.get(index);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }

            String state = !mailMessage.isDelivered(now)
                    ? incomingLabel
                    : (mailMessage.isRead() ? readLabel : unreadLabel);
            builder.append(index + 1)
                    .append(". [")
                    .append(state)
                    .append("] ")
                    .append(displaySubject(mailMessage))
                    .append("\n")
                    .append(MailCommandUtil.resolveDisplayName(mailMessage.getSenderAccountId()))
                    .append(" • ")
                    .append(formatTimestamp(mailMessage.isDelivered(now)
                            ? mailMessage.getDeliverAtMillis()
                            : mailMessage.getDeliverAtMillis()));
        }

        return builder.toString();
    }

    @Nonnull
    private String buildDetailMetaText(@Nonnull MailMessage mailMessage) {
        long now = System.currentTimeMillis();
        String senderLine = rawText("pixelbays.rpg.mail.ui.detailSender", "From: {value}")
                .replace("{value}", MailCommandUtil.resolveDisplayName(mailMessage.getSenderAccountId()));
        String statusLine = rawText("pixelbays.rpg.mail.ui.detailStatus", "Status: {value}")
                .replace("{value}", resolveStatusLabel(mailMessage, now));
        String deliveryLine = rawText("pixelbays.rpg.mail.ui.detailDelivery", "Delivery: {value}")
                .replace("{value}", formatTimestamp(mailMessage.getDeliverAtMillis()));
        String attachmentLine = rawText("pixelbays.rpg.mail.ui.detailAttachments", "Attachments: {value}")
                .replace("{value}", resolveAttachmentSummary(mailMessage));
        return String.join("\n", senderLine, statusLine, deliveryLine, attachmentLine);
    }

    @Nonnull
    private String buildDetailBodyText(@Nonnull MailMessage mailMessage) {
        String body = mailMessage.getBody();
        if (body.isBlank()) {
            body = rawText("pixelbays.rpg.mail.ui.noBody", "No message body.");
        }
        return body;
    }

    @Nonnull
    private String resolveStatusLabel(@Nonnull MailMessage mailMessage, long now) {
        if (!mailMessage.isDelivered(now)) {
            return rawText("pixelbays.rpg.mail.ui.inTransit", "IN TRANSIT");
        }
        if (mailMessage.isRead()) {
            return rawText("pixelbays.rpg.mail.ui.read", "READ");
        }
        return rawText("pixelbays.rpg.mail.ui.unread", "UNREAD");
    }

    @Nonnull
    private String resolveAttachmentSummary(@Nonnull MailMessage mailMessage) {
        if (!mailMessage.hasAnyAttachment() && !mailMessage.hasCashOnDelivery()) {
            return rawText("pixelbays.rpg.common.none", "None.");
        }

        StringBuilder builder = new StringBuilder();
        int itemCount = mailMessage.getAttachedItemStacks().size();
        if (itemCount > 0) {
            builder.append(rawText("pixelbays.rpg.mail.ui.attachmentItems", "Items: {value}")
                    .replace("{value}", Integer.toString(itemCount)));
        }
        if (mailMessage.hasCurrencyAttachment()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(rawText("pixelbays.rpg.mail.ui.attachmentCurrency", "Currency: {value}")
                    .replace("{value}", mailMessage.getAttachedCurrency().getCurrencyId()
                            + " x" + mailMessage.getAttachedCurrency().getAmount()));
        }
        if (mailMessage.hasCashOnDelivery()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(rawText("pixelbays.rpg.mail.ui.attachmentCod", "COD: {value}")
                    .replace("{value}", mailMessage.getCashOnDelivery().getCurrencyId()
                            + " x" + mailMessage.getCashOnDelivery().getAmount()));
        }
        return builder.toString();
    }

    @Nonnull
    private String displaySubject(@Nonnull MailMessage mailMessage) {
        String subject = mailMessage.getSubject();
        return subject.isBlank() ? rawText("pixelbays.rpg.mail.ui.noSubject", "(No subject)") : subject;
    }

    @Nonnull
    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return rawText("pixelbays.rpg.common.none", "None.");
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    @Nullable
    private Integer parseSelectionIndex(@Nullable String selectedIndex) {
        if (selectedIndex == null || selectedIndex.isBlank()) {
            return null;
        }
        String trimmed = selectedIndex.trim();
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    private String rawText(@Nonnull String translationKey, @Nonnull String fallback) {
        var formatted = Message.translation(translationKey).getFormattedMessage();
        return formatted.rawText != null && !formatted.rawText.isBlank() ? formatted.rawText : fallback;
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
    }

    @Nullable
    private static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue stringValue) {
            return stringValue.value;
        }
        if (value instanceof IntParamValue intValue) {
            return Integer.toString(intValue.value);
        }
        if (value instanceof LongParamValue longValue) {
            return Long.toString(longValue.value);
        }
        if (value instanceof DoubleParamValue doubleValue) {
            return Double.toString(doubleValue.value);
        }
        if (value instanceof BoolParamValue boolValue) {
            return Boolean.toString(boolValue.value);
        }
        return String.valueOf(value);
    }

    @Nullable
    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = rawData.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return rawData.substring(firstQuote + 1, secondQuote);
    }
}
