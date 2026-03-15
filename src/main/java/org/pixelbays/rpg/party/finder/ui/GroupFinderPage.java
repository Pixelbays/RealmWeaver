package org.pixelbays.rpg.party.finder.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.command.PartyCommandUtil;
import org.pixelbays.rpg.party.finder.GroupFinderActionResult;
import org.pixelbays.rpg.party.finder.GroupFinderManager;
import org.pixelbays.rpg.party.finder.GroupFinderSnapshot;
import org.pixelbays.rpg.party.ui.PartyPage;

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
public class GroupFinderPage extends CustomUIPage {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String MY_LISTING_LABEL = "#MyListingLabel";
    private static final String LISTINGS_LABEL = "#ListingsLabel";
    private static final String MY_APPLICATION_LABEL = "#MyApplicationLabel";
    private static final String APPLICATIONS_LABEL = "#ApplicationsLabel";

    private static final String ACTIVITY_FIELD = "#ActivityField";
    private static final String DESCRIPTION_FIELD = "#DescriptionField";
    private static final String REQUIREMENTS_FIELD = "#RequirementsField";
    private static final String LISTING_INDEX_FIELD = "#ListingIndexField";
    private static final String DESIRED_ROLE_FIELD = "#DesiredRoleField";
    private static final String APPLICATION_NOTE_FIELD = "#ApplicationNoteField";
    private static final String APPLICANT_INDEX_FIELD = "#ApplicantIndexField";

    private static final String REFRESH_BUTTON = "#RefreshButton";
    private static final String SAVE_LISTING_BUTTON = "#SaveListingButton";
    private static final String REMOVE_LISTING_BUTTON = "#RemoveListingButton";
    private static final String APPLY_BUTTON = "#ApplyButton";
    private static final String WITHDRAW_BUTTON = "#WithdrawButton";
    private static final String INVITE_BUTTON = "#InviteApplicantButton";
    private static final String REJECT_BUTTON = "#RejectApplicantButton";
    private static final String BACK_BUTTON = "#BackToPartyButton";

    private final GroupFinderManager groupFinderManager;

    public GroupFinderPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.groupFinderManager = ExamplePlugin.get().getGroupFinderManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/GroupFinderPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null, false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String activity = extractString(rawData, "@Activity");
        String description = extractString(rawData, "@Description");
        String requirements = extractString(rawData, "@Requirements");
        String listingIndex = extractString(rawData, "@ListingIndex");
        String desiredRole = extractString(rawData, "@DesiredRole");
        String applicationNote = extractString(rawData, "@ApplicationNote");
        String applicantIndex = extractString(rawData, "@ApplicantIndex");

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, activity, description, requirements, listingIndex,
                desiredRole, applicationNote, applicantIndex));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String activity,
            @Nullable String description,
            @Nullable String requirements,
            @Nullable String listingIndex,
            @Nullable String desiredRole,
            @Nullable String applicationNote,
            @Nullable String applicantIndex) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if ("Back".equals(action)) {
            player.getPageManager().openCustomPage(ref, store, new PartyPage(playerRef));
            return;
        }

        boolean clearApplicationFields = false;
        Message statusMessage;
        switch (action) {
            case "Refresh" -> statusMessage = Message.translation("pixelbays.rpg.groupFinder.ui.status.refreshed");
            case "SaveListing" -> statusMessage = toMessage(
                groupFinderManager.upsertListing(playerRef.getUuid(), activity, description, requirements));
            case "RemoveListing" -> statusMessage = toMessage(groupFinderManager.removeListing(playerRef.getUuid()));
            case "Apply" -> {
            GroupFinderActionResult result = groupFinderManager.applyToListing(
                playerRef.getUuid(),
                resolveListingPartyId(listingIndex),
                desiredRole,
                applicationNote);
            clearApplicationFields = result.isSuccess();
            statusMessage = toMessage(result);
            }
            case "Withdraw" -> statusMessage = toMessage(groupFinderManager.withdrawApplication(playerRef.getUuid()));
                case "InviteApplicant" -> {
                UUID resolvedApplicantId = resolveApplicantId(applicantIndex);
                statusMessage = resolvedApplicantId == null
                    ? Message.translation("pixelbays.rpg.groupFinder.error.applicationNotFound")
                    : toMessage(groupFinderManager.inviteApplicant(playerRef.getUuid(), resolvedApplicantId));
                }
                case "RejectApplicant" -> {
                UUID resolvedApplicantId = resolveApplicantId(applicantIndex);
                statusMessage = resolvedApplicantId == null
                    ? Message.translation("pixelbays.rpg.groupFinder.error.applicationNotFound")
                    : toMessage(groupFinderManager.rejectApplicant(playerRef.getUuid(), resolvedApplicantId));
                }
            default -> statusMessage = Message.translation("pixelbays.rpg.common.unknownError");
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage, clearApplicationFields);
        sendUpdate(commandBuilder);
    }

    @Nullable
    private UUID resolveListingPartyId(@Nullable String selectedIndexRaw) {
        Integer selectedIndex = parseSelectionIndex(selectedIndexRaw);
        if (selectedIndex == null || selectedIndex < 1) {
            return null;
        }

        List<GroupFinderSnapshot.ListingView> listings = groupFinderManager.getSnapshot(playerRef.getUuid()).getListings();
        if (selectedIndex > listings.size()) {
            return null;
        }
        return listings.get(selectedIndex - 1).getPartyId();
    }

    @Nullable
    private UUID resolveApplicantId(@Nullable String selectedIndexRaw) {
        Integer selectedIndex = parseSelectionIndex(selectedIndexRaw);
        if (selectedIndex == null || selectedIndex < 1) {
            return null;
        }

        GroupFinderSnapshot.LeaderListingView ownedListing = groupFinderManager.getSnapshot(playerRef.getUuid()).getOwnedListing();
        if (ownedListing == null || selectedIndex > ownedListing.getApplications().size()) {
            return null;
        }
        return ownedListing.getApplications().get(selectedIndex - 1).getApplicantId();
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, REFRESH_BUTTON,
                new EventData().append("Action", "Refresh"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, SAVE_LISTING_BUTTON,
                new EventData()
                        .append("Action", "SaveListing")
                        .append("@Activity", ACTIVITY_FIELD + ".Value")
                        .append("@Description", DESCRIPTION_FIELD + ".Value")
                        .append("@Requirements", REQUIREMENTS_FIELD + ".Value"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, REMOVE_LISTING_BUTTON,
                new EventData().append("Action", "RemoveListing"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, APPLY_BUTTON,
                new EventData()
                        .append("Action", "Apply")
                        .append("@ListingIndex", LISTING_INDEX_FIELD + ".Value")
                        .append("@DesiredRole", DESIRED_ROLE_FIELD + ".Value")
                        .append("@ApplicationNote", APPLICATION_NOTE_FIELD + ".Value"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, WITHDRAW_BUTTON,
                new EventData().append("Action", "Withdraw"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, INVITE_BUTTON,
                new EventData()
                        .append("Action", "InviteApplicant")
                        .append("@ApplicantIndex", APPLICANT_INDEX_FIELD + ".Value"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, REJECT_BUTTON,
                new EventData()
                        .append("Action", "RejectApplicant")
                        .append("@ApplicantIndex", APPLICANT_INDEX_FIELD + ".Value"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, BACK_BUTTON,
                new EventData().append("Action", "Back"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nullable Message statusMessage,
            boolean clearApplicationFields) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        GroupFinderSnapshot snapshot = groupFinderManager.getSnapshot(playerRef.getUuid());
        GroupFinderSnapshot.LeaderListingView ownedListing = snapshot.getOwnedListing();

        commandBuilder.set(MY_LISTING_LABEL + ".Text", buildOwnedListingText(ownedListing));
        commandBuilder.set(LISTINGS_LABEL + ".Text", buildListingsText(snapshot.getListings()));
        commandBuilder.set(MY_APPLICATION_LABEL + ".Text", buildCurrentApplicationText(snapshot.getCurrentApplication()));
        commandBuilder.set(APPLICATIONS_LABEL + ".Text", buildApplicationsText(ownedListing));

        if (ownedListing != null) {
            commandBuilder.set(ACTIVITY_FIELD + ".Value", ownedListing.getActivity());
            commandBuilder.set(DESCRIPTION_FIELD + ".Value", ownedListing.getDescription());
            commandBuilder.set(REQUIREMENTS_FIELD + ".Value", ownedListing.getRequirements());
        }

        if (clearApplicationFields) {
            commandBuilder.set(LISTING_INDEX_FIELD + ".Value", "");
            commandBuilder.set(DESIRED_ROLE_FIELD + ".Value", "");
            commandBuilder.set(APPLICATION_NOTE_FIELD + ".Value", "");
        }
    }

    @Nonnull
    private String buildOwnedListingText(@Nullable GroupFinderSnapshot.LeaderListingView ownedListing) {
        if (ownedListing == null) {
            return rawText("pixelbays.rpg.groupFinder.ui.noOwnedListing", "You do not currently lead a listed group.");
        }

        return rawText("pixelbays.rpg.groupFinder.ui.ownedListingTemplate",
                "Activity: {activity}\\nType: {type}\\nParty Size: {size}/{max}\\nNeeds: {requirements}\\nDescription: {description}\\nRecruiting: {recruiting}\\nUpdated: {updated}")
                .replace("{activity}", safeText(ownedListing.getActivity()))
                .replace("{type}", ownedListing.getPartyType().name())
                .replace("{size}", Integer.toString(ownedListing.getCurrentSize()))
                .replace("{max}", Integer.toString(ownedListing.getMaxSize()))
                .replace("{requirements}", fallbackText(ownedListing.getRequirements()))
                .replace("{description}", fallbackText(ownedListing.getDescription()))
                .replace("{recruiting}", ownedListing.isRecruiting()
                        ? rawText("pixelbays.rpg.groupFinder.ui.recruitingYes", "Yes")
                        : rawText("pixelbays.rpg.groupFinder.ui.recruitingNo", "No"))
                .replace("{updated}", formatTimestamp(ownedListing.getUpdatedAtMillis()));
    }

    @Nonnull
    private String buildListingsText(@Nonnull List<GroupFinderSnapshot.ListingView> listings) {
        if (listings.isEmpty()) {
            return rawText("pixelbays.rpg.groupFinder.ui.emptyListings", "No groups are currently listed.");
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < listings.size(); index++) {
            GroupFinderSnapshot.ListingView listing = listings.get(index);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }

            builder.append(index + 1)
                    .append(". ")
                    .append(listing.getActivity())
                    .append(" [")
                    .append(listing.getPartyType().name())
                    .append("]\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.leaderLabel", "Leader: "))
                    .append(listing.getLeaderName())
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.partySizeLabel", "Party Size: "))
                    .append(listing.getCurrentSize())
                    .append("/")
                    .append(listing.getMaxSize())
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.needsLabel", "Needs: "))
                    .append(fallbackText(listing.getRequirements()))
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.descriptionLabel", "Description: "))
                    .append(fallbackText(listing.getDescription()))
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.applicantsLabel", "Applicants: "))
                    .append(listing.getApplicantCount())
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.updatedLabel", "Updated: "))
                    .append(formatTimestamp(listing.getUpdatedAtMillis()));
        }
        return builder.toString();
    }

    @Nonnull
    private String buildCurrentApplicationText(@Nullable GroupFinderSnapshot.ApplicationView application) {
        if (application == null) {
            return rawText("pixelbays.rpg.groupFinder.ui.noApplication", "You do not have an active application.");
        }

        return rawText("pixelbays.rpg.groupFinder.ui.currentApplicationTemplate",
                "Activity: {activity}\\nDesired Role: {role}\\nNote: {note}\\nSubmitted: {submitted}")
                .replace("{activity}", safeText(application.getActivity()))
                .replace("{role}", fallbackText(application.getDesiredRole()))
                .replace("{note}", fallbackText(application.getNote()))
                .replace("{submitted}", formatTimestamp(application.getSubmittedAtMillis()));
    }

    @Nonnull
    private String buildApplicationsText(@Nullable GroupFinderSnapshot.LeaderListingView ownedListing) {
        if (ownedListing == null || ownedListing.getApplications().isEmpty()) {
            return rawText("pixelbays.rpg.groupFinder.ui.noApplicants", "No pending applicants.");
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < ownedListing.getApplications().size(); index++) {
            GroupFinderSnapshot.ApplicationView application = ownedListing.getApplications().get(index);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }

            builder.append(index + 1)
                    .append(". ")
                    .append(application.getApplicantName())
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.desiredRoleLabel", "Desired Role: "))
                    .append(fallbackText(application.getDesiredRole()))
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.noteLabel", "Note: "))
                    .append(fallbackText(application.getNote()))
                    .append("\n")
                    .append(rawText("pixelbays.rpg.groupFinder.ui.submittedLabel", "Submitted: "))
                    .append(formatTimestamp(application.getSubmittedAtMillis()));
        }
        return builder.toString();
    }

    @Nonnull
    private Message toMessage(@Nonnull GroupFinderActionResult result) {
        return switch (result.getMessage()) {
            case "Group finder is disabled." -> Message.translation("pixelbays.rpg.groupFinder.error.disabled");
            case "You must be the party leader to manage a listing." -> Message.translation("pixelbays.rpg.groupFinder.error.leaderOnly");
            case "Activity is required." -> Message.translation("pixelbays.rpg.groupFinder.error.activityRequired");
            case "You do not have an active listing." -> Message.translation("pixelbays.rpg.groupFinder.error.noOwnedListing");
            case "Listing updated." -> Message.translation("pixelbays.rpg.groupFinder.success.listingUpdated");
            case "Listing removed." -> Message.translation("pixelbays.rpg.groupFinder.success.listingRemoved");
            case "You are already in a party." -> Message.translation("pixelbays.rpg.party.error.alreadyInParty");
            case "You already have an active application." -> Message.translation("pixelbays.rpg.groupFinder.error.alreadyApplied");
            case "That listing does not exist." -> Message.translation("pixelbays.rpg.groupFinder.error.listingNotFound");
            case "You cannot apply to your own party listing." -> Message.translation("pixelbays.rpg.groupFinder.error.ownListing");
            case "That group is no longer recruiting." -> Message.translation("pixelbays.rpg.groupFinder.error.notRecruiting");
            case "Application submitted." -> Message.translation("pixelbays.rpg.groupFinder.success.applicationSubmitted");
            case "You do not have an active application." -> Message.translation("pixelbays.rpg.groupFinder.error.noApplication");
            case "Application withdrawn." -> Message.translation("pixelbays.rpg.groupFinder.success.applicationWithdrawn");
            case "No application found for that player." -> Message.translation("pixelbays.rpg.groupFinder.error.applicationNotFound");
            case "Applicant rejected." -> Message.translation("pixelbays.rpg.groupFinder.success.applicationRejected");
            case "Party invite sent." -> Message.translation("pixelbays.rpg.groupFinder.success.partyInviteSent");
            default -> PartyCommandUtil.managerResultMessage(result.getMessage());
        };
    }

    @Nonnull
    private String rawText(@Nonnull String translationKey, @Nonnull String fallback) {
        var formatted = Message.translation(translationKey).getFormattedMessage();
        return formatted.rawText != null && !formatted.rawText.isBlank() ? formatted.rawText : fallback;
    }

    @Nonnull
    private String fallbackText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return rawText("pixelbays.rpg.common.none", "None.");
        }
        return value;
    }

    @Nonnull
    private String safeText(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nonnull
    private String formatTimestamp(long epochMillis) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    @Nullable
    private Integer parseSelectionIndex(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String resolved = paramToString(entry.getValue());
                    if (resolved != null) {
                        params.put(entry.getKey(), resolved);
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
