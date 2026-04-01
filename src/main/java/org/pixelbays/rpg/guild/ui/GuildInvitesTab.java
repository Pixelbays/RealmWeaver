package org.pixelbays.rpg.guild.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager.PendingGuildApplication;
import org.pixelbays.rpg.guild.GuildManager.PendingGuildInvite;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

final class GuildInvitesTab {

    static final int DEFAULT_APPLICATION_ROW_CAPACITY = 50;
    private static final int OUTGOING_INVITE_PAGE_SIZE = 5;

    private static final String APPLICATION_ENTRY_ASSET = "Common/GuildApplicationEntry.ui";
    private static final String INVITES_LABEL = "#InvitesLabel";
    private static final String APPLICATION_LIST = "#ApplicationList";
    private static final String APPLICATION_SELECTION_LABEL = "#ApplicationSelectionLabel";
    private static final String OUTGOING_INVITE_PAGER_ROW = "#OutgoingInvitePagerRow";
    private static final String OUTGOING_INVITE_PAGE_LABEL = "#OutgoingInvitePageLabel";
    private static final String INVITE_FIELD = "#InviteeField";
    private static final String OUTGOING_INVITE_PREVIOUS_BUTTON = "#OutgoingInvitePreviousButton";
    private static final String OUTGOING_INVITE_NEXT_BUTTON = "#OutgoingInviteNextButton";
    private static final String ACCEPT_APPLICATION_BUTTON = "#AcceptApplicationButton";
    private static final String DENY_APPLICATION_BUTTON = "#DenyApplicationButton";
    private static final String INVITE_BUTTON = "#InviteButton";
    private static final String INCOMING_INVITE_ROW = "#IncomingInviteRow";
    private static final String APPLICATION_DECISION_ROW = "#ApplicationDecisionRow";
    private static final String INVITE_ROW = "#InviteRow";
    private static final String APPLY_ROW = "#ApplyRow";
    private static final String[] OUTGOING_INVITE_ROWS = {
        "#OutgoingInviteRow1",
        "#OutgoingInviteRow2",
        "#OutgoingInviteRow3",
        "#OutgoingInviteRow4",
        "#OutgoingInviteRow5"
    };
    private static final String[] OUTGOING_INVITE_LABELS = {
        "#OutgoingInviteLabel1",
        "#OutgoingInviteLabel2",
        "#OutgoingInviteLabel3",
        "#OutgoingInviteLabel4",
        "#OutgoingInviteLabel5"
    };
    private static final String[] OUTGOING_INVITE_TARGET_FIELDS = {
        "#OutgoingInviteTargetField1",
        "#OutgoingInviteTargetField2",
        "#OutgoingInviteTargetField3",
        "#OutgoingInviteTargetField4",
        "#OutgoingInviteTargetField5"
    };
    private static final String[] OUTGOING_INVITE_CANCEL_BUTTONS = {
        "#OutgoingInviteCancelButton1",
        "#OutgoingInviteCancelButton2",
        "#OutgoingInviteCancelButton3",
        "#OutgoingInviteCancelButton4",
        "#OutgoingInviteCancelButton5"
    };

    private final GuildPage page;

    GuildInvitesTab(@Nonnull GuildPage page) {
        this.page = page;
    }

    void build(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        appendApplicationRows(commandBuilder, eventBuilder);
        bindEvents(eventBuilder);
    }

    @Nullable
    Message handleAction(@Nonnull GuildPageActionData actionData) {
        return switch (actionData.action()) {
            case "PreviousOutgoingInvitePage" -> {
                page.activeTab = GuildPage.GuildTab.INVITES;
                page.outgoingInvitePage = Math.max(0, page.outgoingInvitePage - 1);
                yield null;
            }
            case "NextOutgoingInvitePage" -> {
                page.activeTab = GuildPage.GuildTab.INVITES;
                page.outgoingInvitePage++;
                yield null;
            }
            case "Invite" -> handleInvite(actionData.invitee());
            case "UpdateApplicationSelection" -> {
                page.selectedApplicationApplicantId = GuildPageSupport.normalizeSelectionValue(actionData.applicationApplicantId());
                yield null;
            }
            case "CancelOutgoingInvite" -> handleCancelOutgoingInvite(actionData.outgoingInviteTargetId());
            case "AcceptApplication" -> handleAcceptApplication(actionData.applicationApplicantId());
            case "DenyApplication" -> handleDenyApplication(actionData.applicationApplicantId());
            default -> null;
        };
    }

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        if (guild == null) {
            populateOutgoingInviteControls(commandBuilder, List.of(), false);
            commandBuilder.set(APPLICATION_LIST + ".Visible", false);
            commandBuilder.set(INCOMING_INVITE_ROW + ".Visible", false);
            commandBuilder.set(APPLICATION_DECISION_ROW + ".Visible", false);
            commandBuilder.set(INVITE_ROW + ".Visible", false);
            commandBuilder.set(APPLY_ROW + ".Visible", false);
            commandBuilder.set(ACCEPT_APPLICATION_BUTTON + ".Visible", false);
            commandBuilder.set(DENY_APPLICATION_BUTTON + ".Visible", false);
            clearApplicationRows(commandBuilder);
            GuildPageSupport.setLocalizedText(commandBuilder, INVITES_LABEL, "pixelbays.rpg.guild.ui.noGuild");
            return;
        }

        boolean canInvite = guild.hasPermission(page.playerRef().getUuid(), GuildPermission.INVITE);
        boolean canAcceptApplications = guild.hasPermission(page.playerRef().getUuid(), GuildPermission.ACCEPT_APPLICATIONS);
        boolean canRejectApplications = guild.hasPermission(page.playerRef().getUuid(), GuildPermission.REJECT_APPLICATIONS);
        var outgoingInvites = page.guildManager.getPendingInvitesForGuild(guild.getId());
        var pendingApplications = page.guildManager.getPendingApplicationsForGuild(guild.getId());

        populateOutgoingInviteControls(commandBuilder, outgoingInvites, canInvite);
        populatePendingApplicationControls(commandBuilder, pendingApplications, canAcceptApplications, canRejectApplications);

        commandBuilder.set(INCOMING_INVITE_ROW + ".Visible", false);
        commandBuilder.set(INVITE_ROW + ".Visible", canInvite);
        commandBuilder.set(APPLY_ROW + ".Visible", false);
        commandBuilder.set(
                INVITES_LABEL + ".Text",
                buildGuildInvitesSummary(outgoingInvites, pendingApplications, canInvite));
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                INVITE_BUTTON,
                new EventData()
                        .append("Action", "Invite")
                        .append("@Invitee", INVITE_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                OUTGOING_INVITE_PREVIOUS_BUTTON,
                new EventData().append("Action", "PreviousOutgoingInvitePage"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                OUTGOING_INVITE_NEXT_BUTTON,
                new EventData().append("Action", "NextOutgoingInvitePage"));

        for (int i = 0; i < OUTGOING_INVITE_CANCEL_BUTTONS.length; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    OUTGOING_INVITE_CANCEL_BUTTONS[i],
                    new EventData()
                            .append("Action", "CancelOutgoingInvite")
                            .append("@OutgoingInviteTargetId", OUTGOING_INVITE_TARGET_FIELDS[i] + ".Value"));
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ACCEPT_APPLICATION_BUTTON,
                new EventData().append("Action", "AcceptApplication"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                DENY_APPLICATION_BUTTON,
                new EventData().append("Action", "DenyApplication"));
    }

    private void appendApplicationRows(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        page.applicationRowCapacity = resolveApplicationRowCapacity();
        for (int index = 0; index < page.applicationRowCapacity; index++) {
            String selector = appendApplicationRow(commandBuilder, index);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData()
                            .append("Action", "UpdateApplicationSelection")
                            .append("@ApplicationApplicantId", selector + " #ApplicantIdField.Value"),
                    false);
        }
    }

    @Nonnull
    private String appendApplicationRow(@Nonnull UICommandBuilder commandBuilder, int index) {
        commandBuilder.append(APPLICATION_LIST, APPLICATION_ENTRY_ASSET);
        return APPLICATION_LIST + "[" + index + "]";
    }

    @Nonnull
    private Message handleInvite(@Nullable String invitee) {
        if (invitee == null || invitee.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.invite");
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(invitee.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = page.guildManager.invitePlayer(page.playerRef().getUuid(), targetRef.getUuid());
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.invitedBy")
                    .param("player", page.playerRef().getUsername()));
        }
        return message;
    }

    @Nonnull
    private Message handleCancelOutgoingInvite(@Nullable String targetId) {
        if (targetId == null || targetId.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.error.outgoingInviteNotFound");
        }

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetId.trim());
        } catch (IllegalArgumentException ex) {
            return Message.translation("pixelbays.rpg.guild.error.outgoingInviteNotFound");
        }

        GuildActionResult result = page.guildManager.cancelInvite(page.playerRef().getUuid(), targetUuid);
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    @Nonnull
    private Message handleAcceptApplication(@Nullable String applicantSelection) {
        String targetSelection = fallbackToSelectedApplication(applicantSelection);
        if (targetSelection == null) {
            return Message.translation("pixelbays.rpg.guild.ui.applicationSelectionRequired");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(targetSelection);
        if (targetUuid == null) {
            return Message.translation("pixelbays.rpg.guild.error.applicationNotFound");
        }

        GuildActionResult result = page.guildManager.acceptApplication(page.playerRef().getUuid(), targetUuid);
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            page.selectedApplicationApplicantId = null;
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.applicationAccepted"));
            }
        }
        return message;
    }

    @Nonnull
    private Message handleDenyApplication(@Nullable String applicantSelection) {
        String targetSelection = fallbackToSelectedApplication(applicantSelection);
        if (targetSelection == null) {
            return Message.translation("pixelbays.rpg.guild.ui.applicationSelectionRequired");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(targetSelection);
        if (targetUuid == null) {
            return Message.translation("pixelbays.rpg.guild.error.applicationNotFound");
        }

        GuildActionResult result = page.guildManager.denyApplication(page.playerRef().getUuid(), targetUuid);
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            page.selectedApplicationApplicantId = null;
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.applicationDenied"));
            }
        }
        return message;
    }

    @Nullable
    private String fallbackToSelectedApplication(@Nullable String targetSelection) {
        String normalized = GuildPageSupport.normalizeSelectionValue(targetSelection);
        return normalized != null ? normalized : page.selectedApplicationApplicantId;
    }

    private void populateOutgoingInviteControls(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<PendingGuildInvite> outgoingInvites,
            boolean canCancelOutgoingInvites) {
        boolean showControls = canCancelOutgoingInvites && !outgoingInvites.isEmpty();
        if (!showControls) {
            page.outgoingInvitePage = 0;
        }

        int pageCount = showControls
                ? Math.max(1, (outgoingInvites.size() + OUTGOING_INVITE_PAGE_SIZE - 1) / OUTGOING_INVITE_PAGE_SIZE)
                : 1;
        page.outgoingInvitePage = showControls
                ? Math.max(0, Math.min(page.outgoingInvitePage, pageCount - 1))
                : 0;

        commandBuilder.set(OUTGOING_INVITE_PAGER_ROW + ".Visible", showControls && pageCount > 1);
        commandBuilder.set(OUTGOING_INVITE_PREVIOUS_BUTTON + ".Visible", showControls && pageCount > 1);
        commandBuilder.set(OUTGOING_INVITE_NEXT_BUTTON + ".Visible", showControls && pageCount > 1);
        commandBuilder.set(
                OUTGOING_INVITE_PAGE_LABEL + ".Text",
                showControls
                        ? GuildPageSupport.resolveLocalizedText(
                                "pixelbays.rpg.guild.ui.outgoingInvitePage",
                                Map.of(
                                        "current", String.valueOf(page.outgoingInvitePage + 1),
                                        "total", String.valueOf(pageCount)))
                        : "");

        int startIndex = page.outgoingInvitePage * OUTGOING_INVITE_PAGE_SIZE;
        for (int i = 0; i < OUTGOING_INVITE_ROWS.length; i++) {
            int inviteIndex = startIndex + i;
            boolean visible = showControls && inviteIndex < outgoingInvites.size();
            commandBuilder.set(OUTGOING_INVITE_ROWS[i] + ".Visible", visible);
            commandBuilder.set(
                    OUTGOING_INVITE_TARGET_FIELDS[i] + ".Value",
                    visible ? outgoingInvites.get(inviteIndex).targetId().toString() : "");
            commandBuilder.set(
                    OUTGOING_INVITE_LABELS[i] + ".Text",
                    visible ? buildOutgoingInviteRowText(outgoingInvites.get(inviteIndex)) : "");
        }
    }

    @Nonnull
    private String buildGuildInvitesSummary(@Nonnull List<PendingGuildInvite> outgoingInvites,
            @Nonnull List<PendingGuildApplication> pendingApplications,
            boolean canCancelOutgoingInvites) {
        StringBuilder builder = new StringBuilder();
        builder.append("Outgoing Invites:");
        if (outgoingInvites.isEmpty()) {
            builder.append("\n- none");
        } else if (canCancelOutgoingInvites) {
            builder.append("\n")
                    .append(GuildPageSupport.resolveLocalizedText(
                            "pixelbays.rpg.guild.ui.outgoingInviteSummaryManaged",
                            Map.of("count", String.valueOf(outgoingInvites.size()))));
        } else {
            for (PendingGuildInvite invite : outgoingInvites) {
                builder.append("\n- ")
                        .append(GuildCommandUtil.resolveDisplayName(invite.targetId()))
                        .append(" (expires ")
                        .append(GuildPageSupport.formatRemainingTime(invite.expiresAtMillis()))
                        .append(")");
            }
        }

        builder.append("\n\nPending Applications:");
        if (pendingApplications.isEmpty()) {
            builder.append("\n- none");
        }

        return builder.toString();
    }

    @Nonnull
    private String buildOutgoingInviteRowText(@Nonnull PendingGuildInvite invite) {
        return GuildPageSupport.resolveLocalizedText(
                "pixelbays.rpg.guild.ui.outgoingInviteEntry",
                Map.of(
                        "player", GuildCommandUtil.resolveDisplayName(invite.targetId()),
                        "expires", GuildPageSupport.formatRemainingTime(invite.expiresAtMillis())));
    }

    private void populatePendingApplicationControls(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<PendingGuildApplication> pendingApplications,
            boolean canAcceptApplications,
            boolean canRejectApplications) {
        boolean canReviewApplications = canAcceptApplications || canRejectApplications;
        boolean showControls = canReviewApplications && !pendingApplications.isEmpty();
        PendingGuildApplication selectedApplication = resolveSelectedApplication(pendingApplications);

        commandBuilder.set(APPLICATION_DECISION_ROW + ".Visible", showControls);
        commandBuilder.set(APPLICATION_LIST + ".Visible", showControls);
        commandBuilder.set(
                ACCEPT_APPLICATION_BUTTON + ".Visible",
                showControls && canAcceptApplications && selectedApplication != null);
        commandBuilder.set(
                DENY_APPLICATION_BUTTON + ".Visible",
                showControls && canRejectApplications && selectedApplication != null);

        if (showControls) {
            populateApplicationRows(commandBuilder, pendingApplications, selectedApplication);
        } else {
            page.selectedApplicationApplicantId = null;
            commandBuilder.set(APPLICATION_SELECTION_LABEL + ".Text", "");
            clearApplicationRows(commandBuilder);
        }
    }

    @Nullable
    private PendingGuildApplication resolveSelectedApplication(@Nonnull List<PendingGuildApplication> pendingApplications) {
        PendingGuildApplication selectedApplication = findPendingApplication(pendingApplications, page.selectedApplicationApplicantId);
        if (selectedApplication == null && !pendingApplications.isEmpty()) {
            selectedApplication = pendingApplications.get(0);
        }

        page.selectedApplicationApplicantId = selectedApplication == null
                ? null
                : selectedApplication.applicantId().toString();
        return selectedApplication;
    }

    @Nullable
    private static PendingGuildApplication findPendingApplication(@Nonnull List<PendingGuildApplication> pendingApplications,
            @Nullable String applicantSelection) {
        UUID applicantUuid = GuildPageSupport.resolveGuildTargetUuid(applicantSelection);
        if (applicantUuid == null) {
            return null;
        }

        for (PendingGuildApplication application : pendingApplications) {
            if (application.applicantId().equals(applicantUuid)) {
                return application;
            }
        }
        return null;
    }

    private void populateApplicationRows(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<PendingGuildApplication> pendingApplications,
            @Nullable PendingGuildApplication selectedApplication) {
        for (int index = 0; index < page.applicationRowCapacity; index++) {
            String selector = APPLICATION_LIST + "[" + index + "]";
            if (index >= pendingApplications.size()) {
                commandBuilder.set(selector + ".Visible", false);
                commandBuilder.set(selector + " #SelectedBar.Visible", false);
                commandBuilder.set(selector + " #TitleLabel.Text", "");
                commandBuilder.set(selector + " #SubtitleLabel.Text", "");
                commandBuilder.set(selector + " #MetaLabel.Text", "");
                commandBuilder.set(selector + " #ApplicantIdField.Value", "");
                continue;
            }

            PendingGuildApplication application = pendingApplications.get(index);
            boolean isSelected = selectedApplication != null
                    && application.applicantId().equals(selectedApplication.applicantId());

            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", isSelected);
            commandBuilder.set(selector + " #TitleLabel.Text", GuildCommandUtil.resolveDisplayName(application.applicantId()));
            commandBuilder.set(
                    selector + " #SubtitleLabel.Text",
                    GuildPageSupport.resolveLocalizedText(
                            application.expiresAtMillis() > 0L
                                    ? "pixelbays.rpg.guild.ui.applicationRowSubtitle"
                                    : "pixelbays.rpg.guild.ui.applicationRowSubtitleNoExpiry",
                            Map.of(
                                    "submitted", GuildPageSupport.formatElapsedTime(application.createdAtMillis()),
                                    "expires", application.expiresAtMillis() > 0L
                                            ? GuildPageSupport.formatRemainingTime(application.expiresAtMillis())
                                            : GuildPageSupport.rawText("pixelbays.rpg.common.none", "None"))));
            commandBuilder.set(selector + " #MetaLabel.Text", buildApplicationRowMeta(application));
            commandBuilder.set(selector + " #ApplicantIdField.Value", application.applicantId().toString());
        }
    }

    private void clearApplicationRows(@Nonnull UICommandBuilder commandBuilder) {
        for (int index = 0; index < page.applicationRowCapacity; index++) {
            String selector = APPLICATION_LIST + "[" + index + "]";
            commandBuilder.set(selector + ".Visible", false);
            commandBuilder.set(selector + " #SelectedBar.Visible", false);
            commandBuilder.set(selector + " #TitleLabel.Text", "");
            commandBuilder.set(selector + " #SubtitleLabel.Text", "");
            commandBuilder.set(selector + " #MetaLabel.Text", "");
            commandBuilder.set(selector + " #ApplicantIdField.Value", "");
        }
    }

    @Nonnull
    private String buildApplicationRowMeta(@Nonnull PendingGuildApplication application) {
        String reason = application.applicationMessage();
        if (reason == null || reason.isBlank()) {
            return GuildPageSupport.rawText("pixelbays.rpg.common.none", "None");
        }

        String condensed = reason.replace("\r", " ").replace("\n", " ").trim();
        if (condensed.length() <= 96) {
            return condensed;
        }
        return condensed.substring(0, 93) + "...";
    }

    private int resolveApplicationRowCapacity() {
        var config = GuildPageSupport.resolveConfig();
        int configuredCapacity = config == null ? DEFAULT_APPLICATION_ROW_CAPACITY : config.getGuildMaxMembers();
        return Math.max(DEFAULT_APPLICATION_ROW_CAPACITY, configuredCapacity);
    }
}
