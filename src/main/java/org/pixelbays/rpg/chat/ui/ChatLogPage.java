package org.pixelbays.rpg.chat.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.chat.ChatLogManager;
import org.pixelbays.rpg.chat.config.ChatLogData;

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
public final class ChatLogPage extends CustomUIPage {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_VISIBLE_ENTRIES = 25;
    private static final String HISTORY_ENTRY_ASSET = "Common/ChatLogEntry.ui";

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String TARGET_LABEL = "#TargetLabel";
    private static final String SUMMARY_LABEL = "#SummaryLabel";
    private static final String HISTORY_ENTRY_LIST = "#HistoryEntryList";
    private static final String HISTORY_EMPTY_LABEL = "#HistoryEmptyLabel";
    private static final String DETAIL_HEADER_LABEL = "#DetailHeaderLabel";
    private static final String DETAIL_META_LABEL = "#DetailMetaLabel";
    private static final String DETAIL_BODY_LABEL = "#DetailBodyLabel";
    private static final String REFRESH_BUTTON = "#RefreshButton";

    private final ChatLogManager chatLogManager;
    private final UUID targetAccountId;
    @Nullable
    private Integer selectedEntryIndex;

    public ChatLogPage(@Nonnull PlayerRef playerRef, @Nonnull UUID targetAccountId) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.chatLogManager = Realmweavers.get().getChatLogManager();
        this.targetAccountId = targetAccountId;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ChatLogPage.ui");
        appendHistoryRows(commandBuilder, eventBuilder);
        bindEvents(eventBuilder);
        appendView(commandBuilder, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String selectedIndex = extractString(rawData, "@SelectedIndex");
        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, selectedIndex));
    }

    private void handleAction(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String selectedIndex) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Message statusMessage = switch (action) {
            case "Refresh" -> Message.translation("pixelbays.rpg.chatlog.ui.status.refreshed");
            case "Select" -> handleSelect(selectedIndex);
            default -> Message.translation("pixelbays.rpg.common.unknownError");
        };

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage);
        sendUpdate(commandBuilder);
    }

    @Nonnull
    private Message handleSelect(@Nullable String selectedIndex) {
        List<ChatLogData.ChatLogEntryData> displayedEntries = getDisplayedEntries();
        Integer requestedIndex = parseSelectionIndex(selectedIndex);
        if (requestedIndex == null || requestedIndex < 0 || requestedIndex >= displayedEntries.size()) {
            return Message.translation("pixelbays.rpg.chatlog.ui.error.invalidSelection");
        }

        selectedEntryIndex = requestedIndex;
        ChatLogData.ChatLogEntryData selectedEntry = displayedEntries.get(selectedEntryIndex);
        return Message.translation("pixelbays.rpg.chatlog.ui.status.selected")
        .param("channel", selectedEntry.getChannelId().isBlank() ? "base" : selectedEntry.getChannelId());
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                REFRESH_BUTTON,
                new EventData().append("Action", "Refresh"));
    }

    private void appendHistoryRows(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        for (int index = 0; index < MAX_VISIBLE_ENTRIES; index++) {
            String selector = appendHistoryRow(commandBuilder, index);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData()
                            .append("Action", "Select")
                            .append("@SelectedIndex", selector + " #EntryIndexField.Value"),
                    false);
        }
    }

    @Nonnull
    private String appendHistoryRow(@Nonnull UICommandBuilder commandBuilder, int index) {
        commandBuilder.append(HISTORY_ENTRY_LIST, HISTORY_ENTRY_ASSET);
        return HISTORY_ENTRY_LIST + "[" + index + "]";
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder, @Nullable Message statusMessage) {
        if (statusMessage != null) {
            commandBuilder.set(STATUS_LABEL + ".Text", resolveMessageText(statusMessage, ""));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        ChatLogData logData = chatLogManager.getLog(targetAccountId);
        String accountName = chatLogManager.resolveDisplayName(targetAccountId);
        String characterName = chatLogManager.resolveLastKnownCharacterName(targetAccountId);
        List<ChatLogData.ChatLogEntryData> displayedEntries = getDisplayedEntries();

        if (selectedEntryIndex != null
                && (selectedEntryIndex < 0 || selectedEntryIndex >= displayedEntries.size())) {
            selectedEntryIndex = null;
        }

        commandBuilder.set(TARGET_LABEL + ".Text", buildTargetText(accountName, characterName));
        commandBuilder.set(SUMMARY_LABEL + ".Text", buildSummaryText(logData, displayedEntries));
        populateHistoryEntries(commandBuilder, displayedEntries);

        ChatLogData.ChatLogEntryData selectedEntry = resolveSelectedEntry(displayedEntries);
        if (selectedEntry == null) {
            commandBuilder.set(DETAIL_HEADER_LABEL + ".Text",
                    rawText("pixelbays.rpg.chatlog.ui.noSelectionTitle", "No log entry selected"));
            commandBuilder.set(DETAIL_META_LABEL + ".Text",
                    rawText("pixelbays.rpg.chatlog.ui.noSelectionBody", "Select a chat entry to inspect a message."));
            commandBuilder.set(DETAIL_BODY_LABEL + ".Text", "");
            return;
        }

        commandBuilder.set(DETAIL_HEADER_LABEL + ".Text", buildDetailHeader(selectedEntry));
        commandBuilder.set(DETAIL_META_LABEL + ".Text", buildDetailMetaText(selectedEntry));
        commandBuilder.set(DETAIL_BODY_LABEL + ".Text", buildDetailBodyText(selectedEntry));
    }

    private void populateHistoryEntries(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<ChatLogData.ChatLogEntryData> displayedEntries) {
        boolean hasEntries = !displayedEntries.isEmpty();
        commandBuilder.set(HISTORY_ENTRY_LIST + ".Visible", hasEntries);
        commandBuilder.set(HISTORY_EMPTY_LABEL + ".Visible", !hasEntries);

        for (int index = 0; index < MAX_VISIBLE_ENTRIES; index++) {
            String selector = HISTORY_ENTRY_LIST + "[" + index + "]";
            if (index >= displayedEntries.size()) {
                commandBuilder.set(selector + ".Visible", false);
                commandBuilder.set(selector + " #SelectedBar.Visible", false);
                commandBuilder.set(selector + " #TitleLabel.Text", "");
                commandBuilder.set(selector + " #SubtitleLabel.Text", "");
                commandBuilder.set(selector + " #MetaLabel.Text", "");
                commandBuilder.set(selector + " #EntryIndexField.Value", "");
                continue;
            }

            ChatLogData.ChatLogEntryData entry = displayedEntries.get(index);
            boolean isSelected = selectedEntryIndex != null && selectedEntryIndex == index;

            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", isSelected);
            commandBuilder.set(selector + " #TitleLabel.Text", buildHistoryTitle(entry));
            commandBuilder.set(selector + " #SubtitleLabel.Text", buildHistoryPreview(entry));
            commandBuilder.set(selector + " #MetaLabel.Text", buildHistoryMeta(entry));
            commandBuilder.set(selector + " #EntryIndexField.Value", Integer.toString(index));
        }
    }

    @Nonnull
    private List<ChatLogData.ChatLogEntryData> getDisplayedEntries() {
        ChatLogData logData = chatLogManager.getLog(targetAccountId);
        if (logData == null) {
            return List.of();
        }

        List<ChatLogData.ChatLogEntryData> allEntries = logData.getEntries();
        List<ChatLogData.ChatLogEntryData> displayedEntries = new ArrayList<>();
        for (int index = allEntries.size() - 1; index >= 0 && displayedEntries.size() < MAX_VISIBLE_ENTRIES; index--) {
            displayedEntries.add(allEntries.get(index));
        }
        return displayedEntries;
    }

    @Nullable
    private ChatLogData.ChatLogEntryData resolveSelectedEntry(@Nonnull List<ChatLogData.ChatLogEntryData> displayedEntries) {
        if (selectedEntryIndex == null || selectedEntryIndex < 0 || selectedEntryIndex >= displayedEntries.size()) {
            return null;
        }
        return displayedEntries.get(selectedEntryIndex);
    }

    @Nonnull
    private String buildTargetText(@Nonnull String accountName, @Nonnull String characterName) {
        String accountLine = rawText("pixelbays.rpg.chatlog.ui.targetAccount", "Account: {value}")
                .replace("{value}", accountName);
        String characterValue = characterName.isBlank()
                ? rawText("pixelbays.rpg.common.none", "None.")
                : characterName;
        String characterLine = rawText("pixelbays.rpg.chatlog.ui.targetCharacter", "Character: {value}")
                .replace("{value}", characterValue);
        return accountLine + "\n" + characterLine;
    }

    @Nonnull
    private String buildSummaryText(
            @Nullable ChatLogData logData,
            @Nonnull List<ChatLogData.ChatLogEntryData> displayedEntries) {
        int totalEntries = logData == null ? 0 : logData.getEntries().size();
        int filteredEntries = 0;
        for (ChatLogData.ChatLogEntryData entry : displayedEntries) {
            if (entry.isFiltered()) {
                filteredEntries++;
            }
        }

        return rawText("pixelbays.rpg.chatlog.ui.summaryTemplate", "Stored: {stored}\\nShowing: {showing}\\nFiltered: {filtered}")
                .replace("{stored}", Integer.toString(totalEntries))
                .replace("{showing}", Integer.toString(displayedEntries.size()))
                .replace("{filtered}", Integer.toString(filteredEntries));
    }

    @Nonnull
    private String buildHistoryTitle(@Nonnull ChatLogData.ChatLogEntryData entry) {
        return rawText("pixelbays.rpg.chatlog.ui.entryTitle", "[{channel}] {time}")
                .replace("{channel}", entry.getChannelId().isBlank() ? "base" : entry.getChannelId())
                .replace("{time}", formatTimestamp(entry.getTimestampMillis()));
    }

    @Nonnull
    private String buildHistoryPreview(@Nonnull ChatLogData.ChatLogEntryData entry) {
        String message = entry.isFiltered() ? entry.getOriginalMessage() : entry.getDeliveredMessage();
        return previewMessage(message);
    }

    @Nonnull
    private String buildHistoryMeta(@Nonnull ChatLogData.ChatLogEntryData entry) {
        String targets = Integer.toString(entry.getTargetCount());
        if (!entry.isFiltered()) {
            return rawText("pixelbays.rpg.chatlog.ui.entryMetaTemplate", "Targets: {targets}")
                    .replace("{targets}", targets);
        }

        if (entry.getMatchedWords().isEmpty()) {
            return rawText("pixelbays.rpg.chatlog.ui.entryMetaFilteredLegacy", "Filtered | Targets: {targets}")
                    .replace("{targets}", targets);
        }

        return rawText("pixelbays.rpg.chatlog.ui.entryMetaFilteredTemplate", "Matched: {matched} | Targets: {targets}")
                .replace("{matched}", String.join(", ", entry.getMatchedWords()))
                .replace("{targets}", targets);
    }

    @Nonnull
    private String buildDetailHeader(@Nonnull ChatLogData.ChatLogEntryData entry) {
        return rawText("pixelbays.rpg.chatlog.ui.detailHeader", "{channel} at {time}")
                .replace("{channel}", entry.getChannelId().isBlank() ? "base" : entry.getChannelId())
                .replace("{time}", formatTimestamp(entry.getTimestampMillis()));
    }

    @Nonnull
    private String buildDetailMetaText(@Nonnull ChatLogData.ChatLogEntryData entry) {
        String characterValue = entry.getCharacterName().isBlank()
                ? rawText("pixelbays.rpg.common.none", "None.")
                : entry.getCharacterName();
        String filterValue = entry.isFiltered()
                ? rawText("pixelbays.rpg.chatlog.ui.filteredYes", "Yes")
                : rawText("pixelbays.rpg.chatlog.ui.filteredNo", "No");

        String speakerLine = rawText("pixelbays.rpg.chatlog.ui.detailSpeaker", "Account: {account}\\nCharacter: {character}")
                .replace("{account}", entry.getAccountName())
                .replace("{character}", characterValue);
        String targetsLine = rawText("pixelbays.rpg.chatlog.ui.detailTargets", "Targets: {value}")
                .replace("{value}", Integer.toString(entry.getTargetCount()));
        String filteredLine = rawText("pixelbays.rpg.chatlog.ui.detailFiltered", "Filtered: {value}")
                .replace("{value}", filterValue);

        if (!entry.isFiltered()) {
            return String.join("\n", speakerLine, targetsLine, filteredLine);
        }

        String matchedLine = entry.getMatchedWords().isEmpty()
                ? rawText(
                        "pixelbays.rpg.chatlog.ui.detailMatchedUnavailable",
                        "Matched words: unavailable for this legacy entry.")
                : rawText("pixelbays.rpg.chatlog.ui.detailMatched", "Matched words: {value}")
                        .replace("{value}", String.join(", ", entry.getMatchedWords()));
        return String.join("\n", speakerLine, targetsLine, filteredLine, matchedLine);
    }

    @Nonnull
    private String buildDetailBodyText(@Nonnull ChatLogData.ChatLogEntryData entry) {
        if (!entry.isFiltered()) {
            return entry.getDeliveredMessage().isBlank()
                    ? rawText("pixelbays.rpg.chatlog.ui.emptyMessage", "(empty message)")
                    : entry.getDeliveredMessage();
        }

        return rawText("pixelbays.rpg.chatlog.ui.filteredBodyTemplate", "Original:\\n{original}\\n\\nDelivered:\\n{delivered}")
                .replace("{original}", entry.getOriginalMessage())
                .replace("{delivered}", entry.getDeliveredMessage());
    }

    @Nonnull
    private String previewMessage(@Nonnull String message) {
        if (message.isBlank()) {
            return rawText("pixelbays.rpg.chatlog.ui.emptyMessage", "(empty message)");
        }
        if (message.length() <= 88) {
            return message;
        }
        return message.substring(0, 85) + "...";
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
        try {
            return Integer.valueOf(selectedIndex.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    private String resolveMessageText(@Nonnull Message message, @Nonnull String fallback) {
        var formatted = message.getFormattedMessage();

        String resolved = formatted.messageId != null
                ? rawText(formatted.messageId, fallback)
                : formatted.rawText;
        if (resolved == null || resolved.isBlank()) {
            resolved = fallback;
        }

        if (formatted.params != null && !formatted.params.isEmpty()) {
            for (var entry : formatted.params.entrySet()) {
                String value = paramToString(entry.getValue());
                resolved = resolved.replace("{" + entry.getKey() + "}", value == null ? "" : value);
            }
        }

        return normalizeUiText(resolved);
    }

    @Nonnull
    private String rawText(@Nonnull String translationKey, @Nonnull String fallback) {
        var formatted = Message.translation(translationKey).getFormattedMessage();
        String resolved = formatted.rawText != null && !formatted.rawText.isBlank() ? formatted.rawText : fallback;
        return normalizeUiText(resolved);
    }

    @Nonnull
    private String normalizeUiText(@Nonnull String value) {
        return value.replace("\\n", "\n");
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