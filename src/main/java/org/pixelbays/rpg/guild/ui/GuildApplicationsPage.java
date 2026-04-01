package org.pixelbays.rpg.guild.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildManager.PendingGuildApplication;
import org.pixelbays.rpg.guild.GuildManager.PendingGuildInvite;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

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
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class GuildApplicationsPage extends CustomUIPage {

    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final int RESULT_PAGE_SIZE = 5;

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String INVITE_SUMMARY_LABEL = "#InviteSummaryLabel";
    private static final String CURRENT_APPLICATION_LABEL = "#CurrentApplicationLabel";
    private static final String SEARCH_FIELD = "#SearchField";
    private static final String SEARCH_BY_NAME_BUTTON = "#SearchByNameButton";
    private static final String SEARCH_BY_DESCRIPTION_BUTTON = "#SearchByDescriptionButton";
    private static final String SEARCH_BUTTON = "#SearchButton";
    private static final String RESULT_PAGER_ROW = "#ResultPagerRow";
    private static final String RESULT_PAGE_LABEL = "#ResultPageLabel";
    private static final String RESULT_PREVIOUS_BUTTON = "#ResultPreviousButton";
    private static final String RESULT_NEXT_BUTTON = "#ResultNextButton";
    private static final String SELECTED_GUILD_LABEL = "#SelectedGuildLabel";
    private static final String APPLICATION_REASON_FIELD = "#ApplicationReasonField";
    private static final String APPLY_BUTTON = "#ApplyButton";
    private static final String INVITE_ACTION_ROW = "#InviteActionRow";
    private static final String ACCEPT_INVITE_BUTTON = "#AcceptInviteButton";
    private static final String DECLINE_INVITE_BUTTON = "#DeclineInviteButton";
    private static final String[] RESULT_ROWS = {
        "#ResultRow1",
        "#ResultRow2",
        "#ResultRow3",
        "#ResultRow4",
        "#ResultRow5"
    };
    private static final String[] RESULT_BUTTONS = {
        "#ResultButton1",
        "#ResultButton2",
        "#ResultButton3",
        "#ResultButton4",
        "#ResultButton5"
    };
    private static final String[] RESULT_VALUE_FIELDS = {
        "#ResultValueField1",
        "#ResultValueField2",
        "#ResultValueField3",
        "#ResultValueField4",
        "#ResultValueField5"
    };

    private final GuildManager guildManager;
    private SearchMode searchMode = SearchMode.NAME;
    private String searchQuery = "";
    private int resultPage;
    @Nullable
    private String selectedGuildId;
    @Nullable
    private Message lastStatusMessage;

    public GuildApplicationsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.guildManager = Realmweavers.get().getGuildManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/GuildApplicationsPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String submittedSearchQuery = extractString(rawData, "@SearchQuery");
        String submittedGuildId = extractString(rawData, "@SelectedGuildId");
        String applicationReason = extractString(rawData, "@ApplicationReason");

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, submittedSearchQuery, submittedGuildId, applicationReason));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String submittedSearchQuery,
            @Nullable String submittedGuildId,
            @Nullable String applicationReason) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (guildManager.getGuildForMember(playerRef.getUuid()) != null) {
            player.getPageManager().openCustomPage(ref, store, new GuildPage(playerRef));
            return;
        }

        boolean clearReason = false;
        Message statusMessage = null;
        switch (action) {
            case "Search" -> {
                searchQuery = normalizeInput(submittedSearchQuery);
                resultPage = 0;
            }
            case "SearchByName" -> {
                searchMode = SearchMode.NAME;
                searchQuery = normalizeInput(submittedSearchQuery);
                resultPage = 0;
            }
            case "SearchByDescription" -> {
                searchMode = SearchMode.DESCRIPTION;
                searchQuery = normalizeInput(submittedSearchQuery);
                resultPage = 0;
            }
            case "PreviousResultsPage" -> resultPage = Math.max(0, resultPage - 1);
            case "NextResultsPage" -> resultPage++;
            case "SelectGuild" -> selectedGuildId = normalizeInput(submittedGuildId);
            case "AcceptInvite" -> {
                GuildActionResult result = guildManager.acceptInvite(playerRef.getUuid());
                statusMessage = GuildCommandUtil.managerResultMessage(result.getMessage());
                if (result.isSuccess()) {
                    player.getPageManager().openCustomPage(ref, store, new GuildPage(playerRef));
                    return;
                }
            }
            case "DeclineInvite" -> {
                GuildActionResult result = guildManager.declineInvite(playerRef.getUuid());
                statusMessage = GuildCommandUtil.managerResultMessage(result.getMessage());
            }
            case "Apply" -> {
                GuildActionResult result = handleApply(applicationReason);
                statusMessage = GuildCommandUtil.managerResultMessage(result.getMessage());
                clearReason = result.isSuccess();
            }
            default -> {
            }
        }

        if (statusMessage != null) {
            lastStatusMessage = statusMessage;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, clearReason);
        sendUpdate(commandBuilder);
    }

    @Nullable
    @Nonnull
    private GuildActionResult handleApply(@Nullable String applicationReason) {
        Guild selectedGuild = findApplicationGuildById(selectedGuildId);
        if (selectedGuild == null) {
            return GuildActionResult.failure("Select a guild from the results first.");
        }

        return guildManager.applyToGuild(
                playerRef.getUuid(),
                selectedGuild.getName(),
                applicationReason == null ? "" : applicationReason);
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SEARCH_BUTTON,
                new EventData().append("Action", "Search").append("@SearchQuery", SEARCH_FIELD + ".Value"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SEARCH_BY_NAME_BUTTON,
                new EventData().append("Action", "SearchByName").append("@SearchQuery", SEARCH_FIELD + ".Value"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SEARCH_BY_DESCRIPTION_BUTTON,
                new EventData()
                        .append("Action", "SearchByDescription")
                        .append("@SearchQuery", SEARCH_FIELD + ".Value"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                RESULT_PREVIOUS_BUTTON,
                new EventData().append("Action", "PreviousResultsPage"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                RESULT_NEXT_BUTTON,
                new EventData().append("Action", "NextResultsPage"));
        for (int i = 0; i < RESULT_BUTTONS.length; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    RESULT_BUTTONS[i],
                    new EventData()
                            .append("Action", "SelectGuild")
                            .append("@SelectedGuildId", RESULT_VALUE_FIELDS[i] + ".Value"));
        }
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ACCEPT_INVITE_BUTTON,
                new EventData().append("Action", "AcceptInvite"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                DECLINE_INVITE_BUTTON,
                new EventData().append("Action", "DeclineInvite"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                APPLY_BUTTON,
                new EventData()
                        .append("Action", "Apply")
                        .append("@ApplicationReason", APPLICATION_REASON_FIELD + ".Value"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder, boolean clearReason) {
        if (lastStatusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(lastStatusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        PendingGuildInvite ownInvite = guildManager.getPendingInvite(playerRef.getUuid());
        PendingGuildApplication ownApplication = guildManager.getPendingApplication(playerRef.getUuid());
        List<Guild> searchResults = resolveSearchResults();
        Guild selectedGuild = syncSelectedGuild(searchResults);

        commandBuilder.set(SEARCH_FIELD + ".Value", searchQuery);
        commandBuilder.set(SEARCH_BY_NAME_BUTTON + ".Text", searchMode == SearchMode.NAME
                ? rawText("pixelbays.rpg.guild.applications.ui.searchByNameActive", "[Name]")
                : rawText("pixelbays.rpg.guild.applications.ui.searchByName", "Name"));
        commandBuilder.set(SEARCH_BY_DESCRIPTION_BUTTON + ".Text", searchMode == SearchMode.DESCRIPTION
                ? rawText("pixelbays.rpg.guild.applications.ui.searchByDescriptionActive", "[Description]")
                : rawText("pixelbays.rpg.guild.applications.ui.searchByDescription", "Description"));

        commandBuilder.set(INVITE_ACTION_ROW + ".Visible", ownInvite != null);
        commandBuilder.set(INVITE_SUMMARY_LABEL + ".Text", buildInviteSummary(ownInvite));
        commandBuilder.set(CURRENT_APPLICATION_LABEL + ".Text", buildCurrentApplicationSummary(ownApplication));
        commandBuilder.set(SELECTED_GUILD_LABEL + ".Text", buildSelectedGuildSummary(selectedGuild));
        commandBuilder.set(APPLY_BUTTON + ".Visible", selectedGuild != null);
        if (clearReason) {
            commandBuilder.set(APPLICATION_REASON_FIELD + ".Value", "");
        }

        populateResultControls(commandBuilder, searchResults, selectedGuild);
    }

    private void populateResultControls(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<Guild> searchResults,
            @Nullable Guild selectedGuild) {
        int pageCount = Math.max(1, (searchResults.size() + RESULT_PAGE_SIZE - 1) / RESULT_PAGE_SIZE);
        resultPage = Math.max(0, Math.min(resultPage, pageCount - 1));
        boolean showPager = searchResults.size() > RESULT_PAGE_SIZE;

        commandBuilder.set(RESULT_PAGER_ROW + ".Visible", showPager);
        commandBuilder.set(RESULT_PREVIOUS_BUTTON + ".Visible", showPager);
        commandBuilder.set(RESULT_NEXT_BUTTON + ".Visible", showPager);
        commandBuilder.set(RESULT_PAGE_LABEL + ".Text",
                searchResults.isEmpty()
                        ? rawText("pixelbays.rpg.guild.applications.ui.noSearchResults", "No guilds matched your search.")
                        : resolveLocalizedText(
                                "pixelbays.rpg.guild.applications.ui.resultPage",
                                Map.of(
                                        "current", String.valueOf(resultPage + 1),
                                        "total", String.valueOf(pageCount),
                                        "count", String.valueOf(searchResults.size()))));

        int startIndex = resultPage * RESULT_PAGE_SIZE;
        for (int i = 0; i < RESULT_ROWS.length; i++) {
            int resultIndex = startIndex + i;
            boolean visible = resultIndex < searchResults.size();
            commandBuilder.set(RESULT_ROWS[i] + ".Visible", visible);
            commandBuilder.set(RESULT_VALUE_FIELDS[i] + ".Value", visible ? searchResults.get(resultIndex).getId().toString() : "");
            commandBuilder.set(RESULT_BUTTONS[i] + ".Text", visible ? buildResultButtonText(searchResults.get(resultIndex), selectedGuild) : "");
        }
    }

    @Nonnull
    private List<Guild> resolveSearchResults() {
        List<Guild> results = new ArrayList<>();
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (Guild guild : guildManager.getGuilds()) {
            if (guild.getJoinPolicy() != GuildJoinPolicy.APPLICATION) {
                continue;
            }

            if (!normalizedQuery.isBlank()) {
                String normalizedName = guild.getName().toLowerCase(Locale.ROOT);
                String normalizedTag = guild.getTag() == null ? "" : guild.getTag().toLowerCase(Locale.ROOT);
                String normalizedDescription = guild.getDescription().toLowerCase(Locale.ROOT);
                boolean matches = switch (searchMode) {
                    case NAME -> normalizedName.contains(normalizedQuery) || normalizedTag.contains(normalizedQuery);
                    case DESCRIPTION -> normalizedDescription.contains(normalizedQuery);
                };
                if (!matches) {
                    continue;
                }
            }

            results.add(guild);
        }

        results.sort(Comparator.comparing(Guild::getName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    @Nullable
    private Guild syncSelectedGuild(@Nonnull List<Guild> searchResults) {
        if (selectedGuildId == null || selectedGuildId.isBlank()) {
            return null;
        }

        for (Guild guild : searchResults) {
            if (guild.getId().toString().equalsIgnoreCase(selectedGuildId)) {
                return guild;
            }
        }

        selectedGuildId = null;
        return null;
    }

    @Nullable
    private Guild findApplicationGuildById(@Nullable String guildId) {
        if (guildId == null || guildId.isBlank()) {
            return null;
        }

        for (Guild guild : guildManager.getGuilds()) {
            if (guild.getJoinPolicy() == GuildJoinPolicy.APPLICATION
                    && guild.getId().toString().equalsIgnoreCase(guildId)) {
                return guild;
            }
        }

        return null;
    }

    @Nonnull
    private String buildInviteSummary(@Nullable PendingGuildInvite ownInvite) {
        if (ownInvite == null) {
            return rawText("pixelbays.rpg.guild.applications.ui.noInvite", "No pending guild invite.");
        }

        return resolveLocalizedText(
                "pixelbays.rpg.guild.applications.ui.inviteSummary",
                Map.of(
                        "guild", formatGuildName(ownInvite.guildName(), ownInvite.guildTag()),
                        "inviter", GuildCommandUtil.resolveDisplayName(ownInvite.inviterId()),
                        "expires", formatRemainingTime(ownInvite.expiresAtMillis())));
    }

    @Nonnull
    private String buildCurrentApplicationSummary(@Nullable PendingGuildApplication ownApplication) {
        if (ownApplication == null) {
            return rawText(
                    "pixelbays.rpg.guild.applications.ui.noCurrentApplication",
                    "You do not have an active guild application.");
        }

        return resolveLocalizedText(
                "pixelbays.rpg.guild.applications.ui.currentApplicationSummary",
                Map.of(
                        "guild", formatGuildName(ownApplication.guildName(), ownApplication.guildTag()),
                        "submitted", formatElapsedTime(ownApplication.createdAtMillis()),
                        "expires", ownApplication.expiresAtMillis() > 0L
                                ? formatRemainingTime(ownApplication.expiresAtMillis())
                                : rawText("pixelbays.rpg.common.none", "None"),
                        "reason", ownApplication.applicationMessage() == null || ownApplication.applicationMessage().isBlank()
                                ? rawText("pixelbays.rpg.common.none", "None")
                                : ownApplication.applicationMessage()));
    }

    @Nonnull
    private String buildSelectedGuildSummary(@Nullable Guild selectedGuild) {
        if (selectedGuild == null) {
            return rawText(
                    "pixelbays.rpg.guild.applications.ui.selectedGuildPrompt",
                    "Search for a guild and pick one from the results to review its details.");
        }

        return resolveLocalizedText(
                "pixelbays.rpg.guild.applications.ui.selectedGuildSummary",
                Map.of(
                        "name", selectedGuild.getName(),
                        "tag", selectedGuild.getTag() == null || selectedGuild.getTag().isBlank() ? "-" : selectedGuild.getTag(),
                        "members", String.valueOf(selectedGuild.size()),
                        "description", selectedGuild.getDescription().isBlank()
                                ? rawText("pixelbays.rpg.common.none", "None")
                                : selectedGuild.getDescription(),
                        "motd", selectedGuild.getMotd().isBlank()
                                ? rawText("pixelbays.rpg.common.none", "None")
                                : selectedGuild.getMotd()));
    }

    @Nonnull
    private String buildResultButtonText(@Nonnull Guild guild, @Nullable Guild selectedGuild) {
        String tagSegment = guild.getTag() == null || guild.getTag().isBlank() ? "" : " [" + guild.getTag() + "]";
        String prefix = selectedGuild != null && selectedGuild.getId().equals(guild.getId()) ? "[Selected] " : "";
        return prefix + guild.getName() + tagSegment;
    }

    @Nonnull
    private static String normalizeInput(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Nonnull
    private static String formatGuildName(@Nonnull String name, @Nullable String tag) {
        if (tag == null || tag.isBlank()) {
            return name;
        }
        return name + " [" + tag + "]";
    }

    @Nonnull
    private static String formatRemainingTime(long expiresAtMillis) {
        if (expiresAtMillis <= 0L) {
            return rawText("pixelbays.rpg.common.none", "None");
        }

        long remainingMillis = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
        long totalMinutes = remainingMillis / 60_000L;
        if (totalMinutes >= 60L) {
            long hours = totalMinutes / 60L;
            long minutes = totalMinutes % 60L;
            return minutes == 0L ? hours + "h" : hours + "h " + minutes + "m";
        }
        return totalMinutes + "m";
    }

    @Nonnull
    private static String formatElapsedTime(long createdAtMillis) {
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - createdAtMillis);
        long totalMinutes = elapsedMillis / 60_000L;
        if (totalMinutes >= 60L) {
            long hours = totalMinutes / 60L;
            long minutes = totalMinutes % 60L;
            return minutes == 0L ? hours + "h ago" : hours + "h " + minutes + "m ago";
        }
        return totalMinutes + "m ago";
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
        if (value instanceof StringParamValue sp) {
            return sp.value;
        }
        if (value instanceof IntParamValue ip) {
            return String.valueOf(ip.value);
        }
        if (value instanceof LongParamValue lp) {
            return String.valueOf(lp.value);
        }
        if (value instanceof DoubleParamValue dp) {
            return String.valueOf(dp.value);
        }
        if (value instanceof BoolParamValue bp) {
            return String.valueOf(bp.value);
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

        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = firstQuote + 1; i < rawData.length(); i++) {
            char current = rawData.charAt(i);
            if (escaping) {
                switch (current) {
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case '"', '\\', '/' -> builder.append(current);
                    default -> builder.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            if (current == '"') {
                return builder.toString();
            }

            builder.append(current);
        }

        return null;
    }

    @Nonnull
    private static String resolveLocalizedText(@Nonnull String messageId, @Nonnull Map<String, String> params) {
        String translated = resolveTranslation(messageId, "");
        for (var entry : params.entrySet()) {
            translated = translated.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return translated;
    }

    @Nonnull
    private static String rawText(@Nonnull String messageId, @Nonnull String fallback) {
        return resolveTranslation(messageId, fallback);
    }

    @Nonnull
    private static String resolveTranslation(@Nonnull String messageId, @Nonnull String fallback) {
        I18nModule i18n = I18nModule.get();
        if (i18n == null) {
            return fallback;
        }

        String translated = i18n.getMessage(DEFAULT_LANGUAGE, messageId);
        if (translated != null && !translated.isBlank()) {
            return translated;
        }

        if (messageId.startsWith("pixelbays.")) {
            String unscopedKey = messageId.substring("pixelbays.".length());
            translated = i18n.getMessage(DEFAULT_LANGUAGE, unscopedKey);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }

        return fallback;
    }

    private enum SearchMode {
        NAME,
        DESCRIPTION
    }
}