package org.pixelbays.rpg.guild.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.config.CharacterProfileData;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildMember;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.GuildRole;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;

final class GuildRosterTab {

    static final int DEFAULT_ROSTER_ROW_CAPACITY = 50;

    private static final String ROSTER_ENTRY_ASSET = "Common/GuildRosterEntry.ui";
    private static final String ROSTER_LABEL = "#RosterLabel";
    private static final String ROSTER_MEMBER_LIST = "#RosterMemberList";
    private static final String ROSTER_ROLE_ROW = "#RosterRoleRow";
    private static final String ROSTER_MODERATION_ROW = "#RosterModerationRow";
    private static final String ROSTER_CONFIRM_OVERLAY = "#RosterModerationConfirmOverlay";
    private static final String ROSTER_CONFIRM_TITLE = "#RosterModerationConfirmTitle";
    private static final String ROSTER_CONFIRM_MESSAGE = "#RosterModerationConfirmMessage";
    private static final String ROSTER_ROLE_DROPDOWN = "#RosterRoleDropdown";
    private static final String ROSTER_ASSIGN_ROLE_BUTTON = "#RosterAssignRoleButton";
    private static final String ROSTER_KICK_BUTTON = "#RosterKickButton";
    private static final String ROSTER_PROMOTE_BUTTON = "#RosterPromoteButton";
    private static final String ROSTER_CONFIRM_CANCEL_BUTTON = "#RosterModerationConfirmCancelButton";
    private static final String ROSTER_CONFIRM_ACCEPT_BUTTON = "#RosterModerationConfirmAcceptButton";

    private final GuildPage page;

    GuildRosterTab(@Nonnull GuildPage page) {
        this.page = page;
    }

    void build(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        appendRosterRows(commandBuilder, eventBuilder);
        bindEvents(eventBuilder);
    }

    void updateRosterSelection(@Nullable String memberId, @Nullable String roleId) {
        if (memberId != null) {
            String normalizedMemberId = GuildPageSupport.normalizeSelectionValue(memberId);
            if (!Objects.equals(page.selectedRosterMemberId, normalizedMemberId)) {
                page.selectedRosterMemberId = normalizedMemberId;
                page.selectedRosterRoleId = null;
                page.clearPendingRosterModeration();
            }
        }

        if (roleId != null) {
            page.selectedRosterRoleId = GuildPageSupport.normalizeSelectionValue(roleId);
        }
    }

    @Nullable
    Message handleAction(@Nonnull GuildPageActionData actionData) {
        return switch (actionData.action()) {
            case "UpdateRosterSelection" -> null;
            case "AssignRole" -> handleAssignRole(actionData.rosterMemberId(), actionData.rosterRoleId());
            case "RequestKickMember" -> requestRosterModerationConfirmation(
                    actionData.rosterMemberId(),
                    RosterModerationAction.KICK);
            case "RequestPromoteMember" -> requestRosterModerationConfirmation(
                    actionData.rosterMemberId(),
                    RosterModerationAction.PROMOTE);
            case "CancelRosterModeration" -> {
                page.clearPendingRosterModeration();
                yield null;
            }
            case "ConfirmRosterModeration" -> handleConfirmedRosterModeration();
            default -> null;
        };
    }

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        boolean canAssignRoles = guild != null && guild.hasPermission(page.playerRef().getUuid(), GuildPermission.PROMOTE);
        boolean canKickMembers = guild != null && guild.hasPermission(page.playerRef().getUuid(), GuildPermission.KICK);
        boolean canPromoteLeader = guild != null && guild.isLeader(page.playerRef().getUuid());

        commandBuilder.set(ROSTER_ROLE_ROW + ".Visible", canAssignRoles);
        commandBuilder.set(ROSTER_MODERATION_ROW + ".Visible", canKickMembers || canPromoteLeader);
        commandBuilder.set(ROSTER_KICK_BUTTON + ".Visible", canKickMembers);
        commandBuilder.set(ROSTER_PROMOTE_BUTTON + ".Visible", canPromoteLeader);

        if (guild == null) {
            commandBuilder.set(ROSTER_MEMBER_LIST + ".Visible", false);
            commandBuilder.set(ROSTER_ROLE_DROPDOWN + ".Entries", List.of());
            commandBuilder.set(ROSTER_ROLE_DROPDOWN + ".Value", "");
            clearRosterRows(commandBuilder);
            page.clearPendingRosterModeration();
            populateRosterModerationConfirmation(commandBuilder, null, null);
            GuildPageSupport.setLocalizedText(commandBuilder, ROSTER_LABEL, "pixelbays.rpg.guild.ui.noGuild");
            return;
        }

        CharacterManager characterManager = Realmweavers.get().getCharacterManager();
        List<GuildMember> members = getSortedMembers(guild, characterManager);
        List<GuildRole> roles = GuildPageSupport.getSortedRoles(guild);
        GuildMember selectedMember = resolveSelectedRosterMember(members);
        resolveSelectedRosterRoleId(guild, selectedMember, roles);

        commandBuilder.set(ROSTER_MEMBER_LIST + ".Visible", !members.isEmpty());
        commandBuilder.set(ROSTER_ROLE_DROPDOWN + ".Entries", buildRoleEntries(roles));
        commandBuilder.set(ROSTER_ROLE_DROPDOWN + ".Value", page.selectedRosterRoleId == null ? "" : page.selectedRosterRoleId);
        populateRosterRows(commandBuilder, guild, members, characterManager, selectedMember);

        if (members.isEmpty()) {
            populateRosterModerationConfirmation(commandBuilder, guild, characterManager);
            GuildPageSupport.setLocalizedText(commandBuilder, ROSTER_LABEL, "pixelbays.rpg.guild.ui.rosterEmpty");
            return;
        }

        GuildPageSupport.setLocalizedText(
                commandBuilder,
                ROSTER_LABEL,
                "pixelbays.rpg.guild.ui.rosterSelectedSummary",
                Map.of(
                        "account", resolveMemberAccountName(characterManager, selectedMember.getMemberId()),
                        "role", resolveMemberRoleName(guild, selectedMember)));
        populateRosterModerationConfirmation(commandBuilder, guild, characterManager);
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                ROSTER_ROLE_DROPDOWN,
                new EventData()
                        .append("Action", "UpdateRosterSelection")
                        .append("@RosterRoleId", ROSTER_ROLE_DROPDOWN + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROSTER_ASSIGN_ROLE_BUTTON,
                new EventData()
                        .append("Action", "AssignRole")
                        .append("@RosterRoleId", ROSTER_ROLE_DROPDOWN + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROSTER_KICK_BUTTON,
                new EventData().append("Action", "RequestKickMember"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROSTER_PROMOTE_BUTTON,
                new EventData().append("Action", "RequestPromoteMember"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROSTER_CONFIRM_CANCEL_BUTTON,
                new EventData().append("Action", "CancelRosterModeration"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROSTER_CONFIRM_ACCEPT_BUTTON,
                new EventData().append("Action", "ConfirmRosterModeration"));
    }

    private void appendRosterRows(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        page.rosterRowCapacity = resolveRosterRowCapacity();
        for (int index = 0; index < page.rosterRowCapacity; index++) {
            String selector = appendRosterRow(commandBuilder, index);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData()
                            .append("Action", "UpdateRosterSelection")
                            .append("@RosterMemberId", selector + " #MemberIdField.Value"),
                    false);
        }
    }

    @Nonnull
    private String appendRosterRow(@Nonnull UICommandBuilder commandBuilder, int index) {
        commandBuilder.append(ROSTER_MEMBER_LIST, ROSTER_ENTRY_ASSET);
        return ROSTER_MEMBER_LIST + "[" + index + "]";
    }

    @Nullable
    private Message handleAssignRole(@Nullable String targetName, @Nullable String roleId) {
        String targetSelection = fallbackToSelectedRosterMember(targetName);
        if (targetSelection == null || roleId == null || roleId.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.roleAssign");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(targetSelection);
        if (targetUuid == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = page.guildManager.assignRole(page.playerRef().getUuid(), targetUuid, roleId.trim());
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.roleUpdated"));
            }
        }
        return message;
    }

    @Nullable
    private Message handleKickMember(@Nullable String targetName) {
        String targetSelection = fallbackToSelectedRosterMember(targetName);
        if (targetSelection == null) {
            return Message.translation("pixelbays.rpg.guild.usage.kick");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(targetSelection);
        if (targetUuid == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = page.guildManager.kickMember(page.playerRef().getUuid(), targetUuid);
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.kicked"));
            }
        }
        return message;
    }

    @Nullable
    private Message handlePromoteMember(@Nullable String targetName) {
        String targetSelection = fallbackToSelectedRosterMember(targetName);
        if (targetSelection == null) {
            return Message.translation("pixelbays.rpg.guild.usage.transfer");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(targetSelection);
        if (targetUuid == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = page.guildManager.transferLeadership(page.playerRef().getUuid(), targetUuid);
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
            if (targetRef != null) {
                targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.nowLeader"));
            }
        }
        return message;
    }

    @Nullable
    private Message requestRosterModerationConfirmation(@Nullable String targetSelection,
            @Nonnull RosterModerationAction action) {
        Guild guild = page.guildManager.getGuildForMember(page.playerRef().getUuid());
        if (guild == null) {
            return Message.translation("pixelbays.rpg.guild.ui.noGuild");
        }

        UUID targetUuid = GuildPageSupport.resolveGuildTargetUuid(fallbackToSelectedRosterMember(targetSelection));
        if (targetUuid == null || guild.getMember(targetUuid) == null) {
            return Message.translation("pixelbays.rpg.guild.ui.rosterSelectionRequired");
        }

        page.pendingRosterModerationAction = action;
        page.pendingRosterModerationTargetId = targetUuid.toString();
        return null;
    }

    @Nullable
    private Message handleConfirmedRosterModeration() {
        RosterModerationAction action = page.pendingRosterModerationAction;
        String targetId = page.pendingRosterModerationTargetId;
        page.clearPendingRosterModeration();
        if (action == null || targetId == null || targetId.isBlank()) {
            return null;
        }

        return switch (action) {
            case KICK -> handleKickMember(targetId);
            case PROMOTE -> handlePromoteMember(targetId);
        };
    }

    @Nullable
    private String fallbackToSelectedRosterMember(@Nullable String targetSelection) {
        String normalized = GuildPageSupport.normalizeSelectionValue(targetSelection);
        return normalized != null ? normalized : page.selectedRosterMemberId;
    }

    @Nonnull
    private static List<GuildMember> getSortedMembers(@Nonnull Guild guild, @Nonnull CharacterManager characterManager) {
        List<GuildMember> members = new ArrayList<>(guild.getMemberList());
        members.sort(Comparator
                .comparing((GuildMember member) -> !guild.isLeader(member.getMemberId()))
                .thenComparing(
                        member -> resolveMemberAccountName(characterManager, member.getMemberId()),
                        String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    @Nullable
    private GuildMember resolveSelectedRosterMember(@Nonnull List<GuildMember> members) {
        GuildMember selectedMember = findGuildMember(members, page.selectedRosterMemberId);
        if (selectedMember == null && !members.isEmpty()) {
            for (GuildMember member : members) {
                if (!member.getMemberId().equals(page.playerRef().getUuid())) {
                    selectedMember = member;
                    break;
                }
            }
            if (selectedMember == null) {
                selectedMember = members.get(0);
            }
        }

        page.selectedRosterMemberId = selectedMember == null ? null : selectedMember.getMemberId().toString();
        if (selectedMember == null) {
            page.selectedRosterRoleId = null;
        }
        return selectedMember;
    }

    private void resolveSelectedRosterRoleId(@Nonnull Guild guild,
            @Nullable GuildMember selectedMember,
            @Nonnull List<GuildRole> roles) {
        if (page.selectedRosterRoleId != null && guild.getRole(page.selectedRosterRoleId) != null) {
            return;
        }

        if (selectedMember != null && guild.getRole(selectedMember.getRoleId()) != null) {
            page.selectedRosterRoleId = selectedMember.getRoleId();
            return;
        }

        page.selectedRosterRoleId = roles.isEmpty() ? null : roles.get(0).getId();
    }

    @Nonnull
    private static List<DropdownEntryInfo> buildRoleEntries(@Nonnull List<GuildRole> roles) {
        List<DropdownEntryInfo> entries = new ArrayList<>();
        for (GuildRole role : roles) {
            String roleName = role.getName() == null || role.getName().isBlank() ? role.getId() : role.getName();
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(roleName), role.getId()));
        }
        return entries;
    }

    private void populateRosterModerationConfirmation(@Nonnull UICommandBuilder commandBuilder,
            @Nullable Guild guild,
            @Nullable CharacterManager characterManager) {
        boolean visible = guild != null
                && characterManager != null
                && page.pendingRosterModerationAction != null
                && page.pendingRosterModerationTargetId != null;
        commandBuilder.set(ROSTER_CONFIRM_OVERLAY + ".Visible", visible);

        if (!visible) {
            GuildPageSupport.setLocalizedText(
                    commandBuilder,
                    ROSTER_CONFIRM_TITLE,
                    "pixelbays.rpg.guild.ui.confirmActionTitle");
            commandBuilder.set(ROSTER_CONFIRM_MESSAGE + ".Text", "");
            GuildPageSupport.setLocalizedText(
                    commandBuilder,
                    ROSTER_CONFIRM_ACCEPT_BUTTON,
                    "pixelbays.rpg.guild.ui.confirmActionAccept");
            return;
        }

        GuildMember targetMember = findGuildMember(guild.getMemberList(), page.pendingRosterModerationTargetId);
        String targetName = targetMember == null
                ? page.pendingRosterModerationTargetId
                : resolveMemberAccountName(characterManager, targetMember.getMemberId());

        GuildPageSupport.setLocalizedText(commandBuilder, ROSTER_CONFIRM_TITLE, page.pendingRosterModerationAction.titleKey);
        GuildPageSupport.setLocalizedText(
                commandBuilder,
                ROSTER_CONFIRM_MESSAGE,
                page.pendingRosterModerationAction.messageKey,
                Map.of("player", targetName));
        GuildPageSupport.setLocalizedText(
                commandBuilder,
                ROSTER_CONFIRM_ACCEPT_BUTTON,
                page.pendingRosterModerationAction.acceptKey);
    }

    private void populateRosterRows(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull Guild guild,
            @Nonnull List<GuildMember> members,
            @Nonnull CharacterManager characterManager,
            @Nullable GuildMember selectedMember) {
        for (int index = 0; index < page.rosterRowCapacity; index++) {
            String selector = ROSTER_MEMBER_LIST + "[" + index + "]";
            if (index >= members.size()) {
                commandBuilder.set(selector + ".Visible", false);
                commandBuilder.set(selector + " #SelectedBar.Visible", false);
                commandBuilder.set(selector + " #TitleLabel.Text", "");
                commandBuilder.set(selector + " #SubtitleLabel.Text", "");
                commandBuilder.set(selector + " #MetaLabel.Text", "");
                commandBuilder.set(selector + " #MemberIdField.Value", "");
                continue;
            }

            GuildMember member = members.get(index);
            boolean isSelected = selectedMember != null && member.getMemberId().equals(selectedMember.getMemberId());
            String details = buildRosterDetails(resolveMemberActiveProfile(characterManager, member.getMemberId()), characterManager);

            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", isSelected);
            commandBuilder.set(selector + " #TitleLabel.Text", resolveMemberAccountName(characterManager, member.getMemberId()));
            commandBuilder.set(
                    selector + " #SubtitleLabel.Text",
                    GuildPageSupport.resolveLocalizedText(
                            "pixelbays.rpg.guild.ui.rosterRoleLine",
                            Map.of("role", resolveMemberRoleName(guild, member))));
            commandBuilder.set(selector + " #MetaLabel.Text", details);
            commandBuilder.set(selector + " #MemberIdField.Value", member.getMemberId().toString());
        }
    }

    private void clearRosterRows(@Nonnull UICommandBuilder commandBuilder) {
        for (int index = 0; index < page.rosterRowCapacity; index++) {
            String selector = ROSTER_MEMBER_LIST + "[" + index + "]";
            commandBuilder.set(selector + ".Visible", false);
            commandBuilder.set(selector + " #SelectedBar.Visible", false);
            commandBuilder.set(selector + " #TitleLabel.Text", "");
            commandBuilder.set(selector + " #SubtitleLabel.Text", "");
            commandBuilder.set(selector + " #MetaLabel.Text", "");
            commandBuilder.set(selector + " #MemberIdField.Value", "");
        }
    }

    @Nonnull
    private String buildRosterDetails(@Nullable CharacterProfileData profile, @Nonnull CharacterManager characterManager) {
        if (profile == null) {
            return "";
        }

        List<String> details = new ArrayList<>();
        if (!profile.getCharacterName().isBlank()) {
            details.add(GuildPageSupport.resolveLocalizedText(
                    "pixelbays.rpg.guild.ui.rosterCharacterSegment",
                    Map.of("value", profile.getCharacterName())));
        }

        details.add(GuildPageSupport.resolveLocalizedText(
                "pixelbays.rpg.guild.ui.rosterLevelSegment",
                Map.of("value", String.valueOf(characterManager.resolveDisplayedLevel(profile)))));

        String raceName = resolveRaceDisplayName(profile.getRaceId());
        if (!raceName.isBlank()) {
            details.add(GuildPageSupport.resolveLocalizedText(
                    "pixelbays.rpg.guild.ui.rosterRaceSegment",
                    Map.of("value", raceName)));
        }

        String className = characterManager.resolveDisplayedClassName(profile);
        if (!className.isBlank()) {
            details.add(GuildPageSupport.resolveLocalizedText(
                    "pixelbays.rpg.guild.ui.rosterClassSegment",
                    Map.of("value", className)));
        }

        return String.join(" | ", details);
    }

    @Nullable
    private static CharacterProfileData resolveMemberActiveProfile(@Nonnull CharacterManager characterManager,
            @Nonnull UUID memberId) {
        String username = resolveMemberRosterUsername(characterManager, memberId);
        if (username == null || username.isBlank()) {
            return null;
        }
        return characterManager.getActiveProfile(memberId, username);
    }

    @Nonnull
    private static String resolveMemberAccountName(@Nonnull CharacterManager characterManager, @Nonnull UUID memberId) {
        String username = resolveMemberRosterUsername(characterManager, memberId);
        return username == null || username.isBlank() ? memberId.toString() : username;
    }

    @Nonnull
    private static String resolveMemberRoleName(@Nonnull Guild guild, @Nonnull GuildMember member) {
        GuildRole role = guild.getRole(member.getRoleId());
        return role != null ? role.getName() : member.getRoleId();
    }

    @Nullable
    private static String resolveMemberRosterUsername(@Nonnull CharacterManager characterManager, @Nonnull UUID memberId) {
        PlayerRef liveRef = Universe.get().getPlayer(memberId);
        if (liveRef != null && liveRef.getUsername() != null && !liveRef.getUsername().isBlank()) {
            return liveRef.getUsername();
        }

        String storedUsername = characterManager.resolveAccountUsername(memberId);
        return storedUsername.isBlank() ? null : storedUsername;
    }

    @Nonnull
    private static String resolveRaceDisplayName(@Nullable String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return "";
        }

        var raceDefinition = Realmweavers.get().getRaceManagementSystem().getRaceDefinition(raceId);
        if (raceDefinition != null && raceDefinition.getDisplayName() != null && !raceDefinition.getDisplayName().isBlank()) {
            return raceDefinition.getDisplayName();
        }

        return raceId;
    }

    private int resolveRosterRowCapacity() {
        var config = GuildPageSupport.resolveConfig();
        int configuredCapacity = config == null ? DEFAULT_ROSTER_ROW_CAPACITY : config.getGuildMaxMembers();
        return Math.max(DEFAULT_ROSTER_ROW_CAPACITY, configuredCapacity);
    }

    @Nullable
    private static GuildMember findGuildMember(@Nonnull Iterable<GuildMember> members, @Nullable String memberSelection) {
        UUID memberUuid = GuildPageSupport.resolveGuildTargetUuid(memberSelection);
        if (memberUuid == null) {
            return null;
        }

        for (GuildMember member : members) {
            if (member.getMemberId().equals(memberUuid)) {
                return member;
            }
        }
        return null;
    }

    enum RosterModerationAction {
        KICK(
                "pixelbays.rpg.guild.ui.confirmKickTitle",
                "pixelbays.rpg.guild.ui.confirmKickMessage",
                "pixelbays.rpg.guild.ui.confirmKickAccept"),
        PROMOTE(
                "pixelbays.rpg.guild.ui.confirmPromoteTitle",
                "pixelbays.rpg.guild.ui.confirmPromoteMessage",
                "pixelbays.rpg.guild.ui.confirmPromoteAccept");

        final String titleKey;
        final String messageKey;
        final String acceptKey;

        RosterModerationAction(String titleKey, String messageKey, String acceptKey) {
            this.titleKey = titleKey;
            this.messageKey = messageKey;
            this.acceptKey = acceptKey;
        }
    }
}
