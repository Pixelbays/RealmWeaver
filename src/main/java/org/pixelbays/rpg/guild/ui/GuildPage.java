package org.pixelbays.rpg.guild.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class GuildPage extends CustomUIPage {

    static final String GUILD_TABS = "#GuildTabs";
    static final String STATUS_LABEL = "#StatusLabel";
    static final String OVERVIEW_PANEL = "#OverviewPanel";
    static final String ROSTER_PANEL = "#RosterPanel";
    static final String MANAGEMENT_PANEL = "#ManagementPanel";
    static final String MANAGEMENT_HEADING_LABEL = "#ManagementHeadingLabel";
    static final String MANAGEMENT_SUMMARY_LABEL = "#ManagementSummaryLabel";
    static final String MANAGEMENT_INFO_PANEL = "#ManagementInfoPanel";
    static final String MANAGEMENT_ROLES_PANEL = "#ManagementRolesPanel";
    static final String INVITES_PANEL = "#InvitesPanel";
    static final String CALENDAR_PANEL = "#CalendarPanel";

    final GuildManager guildManager;
    final GuildOverviewTab overviewTab;
    final GuildRosterTab rosterTab;
    final GuildInvitesTab invitesTab;
    final GuildCalendarTab calendarTab;
    final GuildManagementTab managementTab;
    final GuildRolesTab rolesTab;

    GuildTab activeTab = GuildTab.OVERVIEW;
    int outgoingInvitePage;
    int roleTabPage;
    @Nullable
    String selectedRoleEditorId;
    @Nullable
    String selectedApplicationApplicantId;
    @Nullable
    String selectedRosterMemberId;
    @Nullable
    String selectedRosterRoleId;
    @Nullable
    GuildRosterTab.RosterModerationAction pendingRosterModerationAction;
    @Nullable
    String pendingRosterModerationTargetId;
    @Nullable
    Message lastStatusMessage;
    int rosterRowCapacity = GuildRosterTab.DEFAULT_ROSTER_ROW_CAPACITY;
    int applicationRowCapacity = GuildInvitesTab.DEFAULT_APPLICATION_ROW_CAPACITY;

    public GuildPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.guildManager = Realmweavers.get().getGuildManager();
        this.overviewTab = new GuildOverviewTab();
        this.rosterTab = new GuildRosterTab(this);
        this.invitesTab = new GuildInvitesTab(this);
        this.calendarTab = new GuildCalendarTab();
        this.managementTab = new GuildManagementTab(this);
        this.rolesTab = new GuildRolesTab(this);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/GuildPage.ui");
        bindTabEvents(eventBuilder);
        rosterTab.build(commandBuilder, eventBuilder);
        invitesTab.build(commandBuilder, eventBuilder);
        managementTab.bindEvents(eventBuilder);
        rolesTab.build(eventBuilder);
        appendView(commandBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String rawData) {
        String action = GuildPageSupport.extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        GuildPageActionData actionData = GuildPageActionData.from(action, rawData);
        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, actionData));
    }

    private void bindTabEvents(@Nonnull UIEventBuilder eventBuilder) {
        bindTabEvent(eventBuilder, "#overviewTab", GuildTab.OVERVIEW);
        bindTabEvent(eventBuilder, "#RosterTab", GuildTab.ROSTER);
        bindTabEvent(eventBuilder, "#InvitesTab", GuildTab.INVITES);
        bindTabEvent(eventBuilder, "#CalendarTab", GuildTab.CALENDAR);
        bindTabEvent(eventBuilder, "#GuildManagementTab", GuildTab.MANAGEMENT);
        bindTabEvent(eventBuilder, "#RoleManagementTab", GuildTab.ROLE_MANAGEMENT);
    }

    private void bindTabEvent(@Nonnull UIEventBuilder eventBuilder,
            @Nonnull String selector,
            @Nonnull GuildTab tab) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData().append("Action", "SelectTab").append("Tab", tab.wireId));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull GuildPageActionData actionData) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        rosterTab.updateRosterSelection(actionData.rosterMemberId(), actionData.rosterRoleId());

        Message statusMessage = switch (actionData.action()) {
            case "SelectTab" -> handleSelectTab(actionData.tab());
            case "UpdateRosterSelection", "AssignRole", "RequestKickMember", "RequestPromoteMember",
                    "CancelRosterModeration", "ConfirmRosterModeration" -> rosterTab.handleAction(actionData);
            case "PreviousOutgoingInvitePage", "NextOutgoingInvitePage", "Invite", "UpdateApplicationSelection",
                    "CancelOutgoingInvite", "AcceptApplication", "DenyApplication" -> invitesTab.handleAction(actionData);
            case "SetJoinPolicy", "UpdateGuildInfo" -> managementTab.handleAction(actionData);
            case "PreviousRoleTabPage", "NextRoleTabPage", "CreateRole", "LoadRoleEditor", "RenameRole",
                    "SaveRolePermissions" -> rolesTab.handleAction(actionData);
            default -> null;
        };

        if (statusMessage != null) {
            lastStatusMessage = statusMessage;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder);
        sendUpdate(commandBuilder);
    }

    @Nullable
    private Message handleSelectTab(@Nullable String tab) {
        activeTab = GuildTab.fromWire(tab);
        clearPendingRosterModeration();
        return null;
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder) {
        if (lastStatusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", GuildPageSupport.toLocalizableString(lastStatusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        commandBuilder.set(GUILD_TABS + ".SelectedTab", activeTab.clientTabId);
        commandBuilder.set(OVERVIEW_PANEL + ".Visible", activeTab == GuildTab.OVERVIEW);
        commandBuilder.set(ROSTER_PANEL + ".Visible", activeTab == GuildTab.ROSTER);
        commandBuilder.set(
                MANAGEMENT_PANEL + ".Visible",
                activeTab == GuildTab.MANAGEMENT || activeTab == GuildTab.ROLE_MANAGEMENT);
        commandBuilder.set(INVITES_PANEL + ".Visible", activeTab == GuildTab.INVITES);
        commandBuilder.set(CALENDAR_PANEL + ".Visible", activeTab == GuildTab.CALENDAR);

        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        GuildPageSupport.setLocalizedText(
                commandBuilder,
                MANAGEMENT_HEADING_LABEL,
                activeTab == GuildTab.ROLE_MANAGEMENT
                        ? "pixelbays.rpg.guild.ui.managementRolesTab"
                        : "pixelbays.rpg.guild.ui.managementHeading");
        commandBuilder.set(MANAGEMENT_INFO_PANEL + ".Visible", activeTab == GuildTab.MANAGEMENT && guild != null);
        commandBuilder.set(MANAGEMENT_ROLES_PANEL + ".Visible", activeTab == GuildTab.ROLE_MANAGEMENT && guild != null);
        if (guild == null && (activeTab == GuildTab.MANAGEMENT || activeTab == GuildTab.ROLE_MANAGEMENT)) {
            GuildPageSupport.setLocalizedText(commandBuilder, MANAGEMENT_SUMMARY_LABEL, "pixelbays.rpg.guild.ui.noGuild");
        } else {
            commandBuilder.set(MANAGEMENT_SUMMARY_LABEL + ".Text", "");
        }

        overviewTab.populate(commandBuilder, guild);
        rosterTab.populate(commandBuilder, guild);
        managementTab.populate(commandBuilder, guild);
        rolesTab.populate(commandBuilder, guild);
        invitesTab.populate(commandBuilder, guild);
        calendarTab.populate(commandBuilder, guild);
    }

    void clearPendingRosterModeration() {
        pendingRosterModerationAction = null;
        pendingRosterModerationTargetId = null;
    }

    @Nonnull
    PlayerRef playerRef() {
        return playerRef;
    }

    enum GuildTab {
        OVERVIEW("overview", "Tab1"),
        ROSTER("roster", "Tab2"),
        INVITES("invites", "Tab3"),
        CALENDAR("calendar", "Tab4"),
        MANAGEMENT("management", "Tab5"),
        ROLE_MANAGEMENT("role_management", "Tab6");

        final String wireId;
        final String clientTabId;

        GuildTab(String wireId, String clientTabId) {
            this.wireId = wireId;
            this.clientTabId = clientTabId;
        }

        @Nonnull
        static GuildTab fromWire(@Nullable String wireId) {
            if (wireId == null) {
                return OVERVIEW;
            }

            for (GuildTab tab : values()) {
                if (tab.wireId.equalsIgnoreCase(wireId)) {
                    return tab;
                }
            }
            return OVERVIEW;
        }
    }
}
