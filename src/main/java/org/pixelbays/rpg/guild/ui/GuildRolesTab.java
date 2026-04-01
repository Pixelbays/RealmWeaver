package org.pixelbays.rpg.guild.ui;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.GuildRole;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class GuildRolesTab {

    static final int ROLE_TAB_PAGE_SIZE = 6;

    private static final String ROLE_EDITOR_STATUS_LABEL = "#RoleEditorStatusLabel";
    private static final String ROLE_TAB_PAGE_LABEL = "#RoleTabPageLabel";
    private static final String ROLE_RENAME_ROW = "#RoleRenameRow";
    private static final String ROLE_CREATE_ROW = "#RoleCreateRow";
    private static final String ROLE_NAME_FIELD = "#RoleNameField";
    private static final String ROLE_EDITOR_ROLE_FIELD = "#RoleEditorRoleField";
    private static final String ROLE_EDITOR_NAME_FIELD = "#RoleEditorNameField";
    private static final String CREATE_ROLE_BUTTON = "#CreateRoleButton";
    private static final String ROLE_TAB_PREVIOUS_BUTTON = "#RoleTabPreviousButton";
    private static final String ROLE_TAB_NEXT_BUTTON = "#RoleTabNextButton";
    private static final String RENAME_ROLE_BUTTON = "#RenameRoleButton";
    private static final String SAVE_ROLE_PERMISSIONS_BUTTON = "#SaveRolePermissionsButton";
    private static final String[] ROLE_TAB_BUTTONS = {
        "#RoleTabButton1",
        "#RoleTabButton2",
        "#RoleTabButton3",
        "#RoleTabButton4",
        "#RoleTabButton5",
        "#RoleTabButton6"
    };
    private static final String[] ROLE_TAB_VALUE_FIELDS = {
        "#RoleTabValueField1",
        "#RoleTabValueField2",
        "#RoleTabValueField3",
        "#RoleTabValueField4",
        "#RoleTabValueField5",
        "#RoleTabValueField6"
    };
    private static final String PERMISSION_INVITE_CHECK = "#InvitePermissionCheck";
    private static final String PERMISSION_KICK_CHECK = "#KickPermissionCheck";
    private static final String PERMISSION_PROMOTE_CHECK = "#PromotePermissionCheck";
    private static final String PERMISSION_MANAGE_ROLES_CHECK = "#ManageRolesPermissionCheck";
    private static final String PERMISSION_SET_JOIN_POLICY_CHECK = "#SetJoinPolicyPermissionCheck";
    private static final String PERMISSION_ACCEPT_APPLICATIONS_CHECK = "#AcceptApplicationsPermissionCheck";
    private static final String PERMISSION_REJECT_APPLICATIONS_CHECK = "#RejectApplicationsPermissionCheck";
    private static final String PERMISSION_MANAGE_GUILD_INFO_CHECK = "#ManageGuildInfoPermissionCheck";
    private static final String PERMISSION_MANAGE_GUILD_BANK_CHECK = "#ManageGuildBankPermissionCheck";
    private static final String PERMISSION_MANAGE_GUILD_EVENTS_CHECK = "#ManageGuildEventsPermissionCheck";
    private static final String PERMISSION_MANAGE_GUILD_WAR_CHECK = "#ManageGuildWarPermissionCheck";
    private static final String PERMISSION_MANAGE_GUILD_ALLIANCES_CHECK = "#ManageGuildAlliancesPermissionCheck";
    private static final String PERMISSION_USE_GUILD_CHAT_CHECK = "#UseGuildChatPermissionCheck";
    private static final String PERMISSION_USE_GUILD_BANK_CHECK = "#UseGuildBankPermissionCheck";

    private final GuildPage page;

    GuildRolesTab(@Nonnull GuildPage page) {
        this.page = page;
    }

    void build(@Nonnull UIEventBuilder eventBuilder) {
        bindEvents(eventBuilder);
    }

    @Nullable
    Message handleAction(@Nonnull GuildPageActionData actionData) {
        return switch (actionData.action()) {
            case "PreviousRoleTabPage" -> {
                page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
                page.roleTabPage = Math.max(0, page.roleTabPage - 1);
                yield null;
            }
            case "NextRoleTabPage" -> {
                page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
                page.roleTabPage++;
                yield null;
            }
            case "CreateRole" -> handleCreateRole(actionData.roleName());
            case "LoadRoleEditor" -> handleLoadRoleEditor(actionData.editorRoleId());
            case "RenameRole" -> handleRenameRole(actionData.editorRoleId(), actionData.editorRoleName());
            case "SaveRolePermissions" -> handleSaveRolePermissions(actionData.editorRoleId(), actionData.permissionValues());
            default -> null;
        };
    }

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        boolean canManageRoles = guild != null
                && guild.hasPermission(page.playerRef().getUuid(), GuildPermission.MANAGE_ROLES);
        commandBuilder.set(ROLE_CREATE_ROW + ".Visible", canManageRoles);

        if (guild == null) {
            commandBuilder.set(ROLE_EDITOR_ROLE_FIELD + ".Value", "");
            commandBuilder.set(ROLE_EDITOR_NAME_FIELD + ".Value", "");
            commandBuilder.set(ROLE_RENAME_ROW + ".Visible", false);
            commandBuilder.set(ROLE_TAB_PAGE_LABEL + ".Text", "");
            commandBuilder.set(ROLE_EDITOR_STATUS_LABEL + ".Text", "");
            commandBuilder.set(SAVE_ROLE_PERMISSIONS_BUTTON + ".Visible", false);
            clearRoleTabControls(commandBuilder);
            setRolePermissionValues(commandBuilder, null);
            return;
        }

        List<GuildRole> roles = GuildPageSupport.getSortedRoles(guild);
        GuildRole selectedRole = page.selectedRoleEditorId == null ? null : guild.getRole(page.selectedRoleEditorId);
        if (selectedRole == null && page.selectedRoleEditorId != null) {
            page.selectedRoleEditorId = null;
        }

        commandBuilder.set(ROLE_EDITOR_ROLE_FIELD + ".Value", page.selectedRoleEditorId == null ? "" : page.selectedRoleEditorId);
        commandBuilder.set(ROLE_EDITOR_NAME_FIELD + ".Value", selectedRole == null ? "" : selectedRole.getName());
        commandBuilder.set(ROLE_RENAME_ROW + ".Visible", canManageRoles && selectedRole != null);
        populateRoleTabControls(commandBuilder, roles, selectedRole);
        setRolePermissionValues(commandBuilder, selectedRole);

        if (selectedRole == null) {
            GuildPageSupport.setLocalizedText(
                    commandBuilder,
                    ROLE_EDITOR_STATUS_LABEL,
                    "pixelbays.rpg.guild.ui.roleEditorNoSelection");
            commandBuilder.set(SAVE_ROLE_PERMISSIONS_BUTTON + ".Visible", false);
            return;
        }

        commandBuilder.set(
                ROLE_EDITOR_STATUS_LABEL + ".Text",
                GuildPageSupport.resolveLocalizedText(
                        "pixelbays.rpg.guild.ui.roleEditorSelected",
                        Map.of("name", selectedRole.getName(), "id", selectedRole.getId())));
        commandBuilder.set(
                SAVE_ROLE_PERMISSIONS_BUTTON + ".Visible",
                canManageRoles && !GuildRole.LEADER_ID.equals(selectedRole.getId()));

        if (GuildRole.LEADER_ID.equals(selectedRole.getId())) {
            GuildPageSupport.setLocalizedText(
                    commandBuilder,
                    ROLE_EDITOR_STATUS_LABEL,
                    "pixelbays.rpg.guild.ui.roleEditorLeaderLocked");
        }
    }

    @Nonnull
    static Map<GuildPermission, Boolean> collectPermissionValues(@Nonnull String rawData) {
        Map<GuildPermission, Boolean> values = new HashMap<>();
        for (GuildPermission permission : GuildPermission.values()) {
            Boolean enabled = GuildPageSupport.extractBoolean(rawData, permissionEventKey(permission));
            if (enabled != null) {
                values.put(permission, enabled);
            }
        }
        return values;
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                CREATE_ROLE_BUTTON,
                new EventData()
                        .append("Action", "CreateRole")
                        .append("@RoleName", ROLE_NAME_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROLE_TAB_PREVIOUS_BUTTON,
                new EventData().append("Action", "PreviousRoleTabPage"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ROLE_TAB_NEXT_BUTTON,
                new EventData().append("Action", "NextRoleTabPage"));

        for (int i = 0; i < ROLE_TAB_BUTTONS.length; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    ROLE_TAB_BUTTONS[i],
                    new EventData()
                            .append("Action", "LoadRoleEditor")
                            .append("@RoleEditorRoleId", ROLE_TAB_VALUE_FIELDS[i] + ".Value"));
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                RENAME_ROLE_BUTTON,
                new EventData()
                        .append("Action", "RenameRole")
                        .append("@RoleEditorRoleId", ROLE_EDITOR_ROLE_FIELD + ".Value")
                        .append("@RoleEditorRoleName", ROLE_EDITOR_NAME_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SAVE_ROLE_PERMISSIONS_BUTTON,
                buildSaveRolePermissionsEvent());
    }

    @Nonnull
    private Message handleCreateRole(@Nullable String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.roleCreate");
        }

        GuildActionResult result = page.guildManager.createRole(page.playerRef().getUuid(), roleName.trim());
        if (result.isSuccess()) {
            page.selectedRoleEditorId = GuildPageSupport.normalizeRoleId(roleName.trim());
            page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
            page.roleTabPage = GuildPageSupport.resolveRoleTabPage(
                    result.getGuild(),
                    page.selectedRoleEditorId,
                    ROLE_TAB_PAGE_SIZE);
        }
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    @Nonnull
    private Message handleLoadRoleEditor(@Nullable String roleId) {
        Guild guild = page.guildManager.getGuildForMember(page.playerRef().getUuid());
        if (guild == null) {
            return Message.translation("pixelbays.rpg.guild.ui.noGuild");
        }

        String normalizedRoleId = GuildPageSupport.normalizeRoleId(roleId);
        if (normalizedRoleId == null) {
            return Message.translation("pixelbays.rpg.guild.error.roleNotFound");
        }

        GuildRole role = guild.getRole(normalizedRoleId);
        if (role == null) {
            return Message.translation("pixelbays.rpg.guild.error.roleNotFound");
        }

        page.selectedRoleEditorId = role.getId();
        page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
        page.roleTabPage = GuildPageSupport.resolveRoleTabPage(guild, role.getId(), ROLE_TAB_PAGE_SIZE);
        return Message.translation("pixelbays.rpg.guild.ui.roleEditorLoaded").param("role", role.getName());
    }

    @Nonnull
    private Message handleRenameRole(@Nullable String roleId, @Nullable String roleName) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.error.roleNotFound");
        }

        GuildActionResult result = page.guildManager.renameRole(page.playerRef().getUuid(), roleId.trim(), roleName);
        if (result.isSuccess() && result.getGuild() != null) {
            page.selectedRoleEditorId = roleId.trim().toLowerCase(Locale.ROOT);
            page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
            page.roleTabPage = GuildPageSupport.resolveRoleTabPage(
                    result.getGuild(),
                    page.selectedRoleEditorId,
                    ROLE_TAB_PAGE_SIZE);
        }
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    @Nonnull
    private Message handleSaveRolePermissions(@Nullable String roleId,
            @Nonnull Map<GuildPermission, Boolean> permissionValues) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.error.roleNotFound");
        }

        Set<GuildPermission> permissions = EnumSet.noneOf(GuildPermission.class);
        for (var entry : permissionValues.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                permissions.add(entry.getKey());
            }
        }

        GuildActionResult result = page.guildManager.setRolePermissions(page.playerRef().getUuid(), roleId.trim(), permissions);
        if (result.isSuccess() && result.getGuild() != null) {
            GuildRole role = result.getGuild().getRole(roleId.trim().toLowerCase(Locale.ROOT));
            page.selectedRoleEditorId = role != null ? role.getId() : roleId.trim().toLowerCase(Locale.ROOT);
            page.activeTab = GuildPage.GuildTab.ROLE_MANAGEMENT;
            page.roleTabPage = GuildPageSupport.resolveRoleTabPage(
                    result.getGuild(),
                    page.selectedRoleEditorId,
                    ROLE_TAB_PAGE_SIZE);
        }
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    private void clearRoleTabControls(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set(ROLE_TAB_PREVIOUS_BUTTON + ".Visible", false);
        commandBuilder.set(ROLE_TAB_NEXT_BUTTON + ".Visible", false);
        for (int i = 0; i < ROLE_TAB_BUTTONS.length; i++) {
            commandBuilder.set(ROLE_TAB_BUTTONS[i] + ".Visible", false);
            commandBuilder.set(ROLE_TAB_BUTTONS[i] + ".Text", "");
            commandBuilder.set(ROLE_TAB_VALUE_FIELDS[i] + ".Value", "");
        }
    }

    private void populateRoleTabControls(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<GuildRole> roles,
            @Nullable GuildRole selectedRole) {
        int pageCount = Math.max(1, (roles.size() + ROLE_TAB_PAGE_SIZE - 1) / ROLE_TAB_PAGE_SIZE);
        page.roleTabPage = Math.max(0, Math.min(page.roleTabPage, pageCount - 1));

        commandBuilder.set(ROLE_TAB_PREVIOUS_BUTTON + ".Visible", pageCount > 1);
        commandBuilder.set(ROLE_TAB_NEXT_BUTTON + ".Visible", pageCount > 1);
        commandBuilder.set(
                ROLE_TAB_PAGE_LABEL + ".Text",
                GuildPageSupport.resolveLocalizedText(
                        "pixelbays.rpg.guild.ui.roleTabsPage",
                        Map.of(
                                "current", String.valueOf(page.roleTabPage + 1),
                                "total", String.valueOf(pageCount))));

        int startIndex = page.roleTabPage * ROLE_TAB_PAGE_SIZE;
        for (int i = 0; i < ROLE_TAB_BUTTONS.length; i++) {
            int roleIndex = startIndex + i;
            boolean visible = roleIndex < roles.size();
            commandBuilder.set(ROLE_TAB_BUTTONS[i] + ".Visible", visible);
            commandBuilder.set(ROLE_TAB_VALUE_FIELDS[i] + ".Value", visible ? roles.get(roleIndex).getId() : "");
            commandBuilder.set(
                    ROLE_TAB_BUTTONS[i] + ".Text",
                    visible ? buildRoleTabText(roles.get(roleIndex), selectedRole) : "");
        }
    }

    @Nonnull
    private static String buildRoleTabText(@Nonnull GuildRole role, @Nullable GuildRole selectedRole) {
        String roleName = role.getName() == null || role.getName().isBlank() ? role.getId() : role.getName();
        if (selectedRole != null && role.getId().equalsIgnoreCase(selectedRole.getId())) {
            return "[" + roleName + "]";
        }
        return roleName;
    }

    private void setRolePermissionValues(@Nonnull UICommandBuilder commandBuilder, @Nullable GuildRole role) {
        for (GuildPermission permission : GuildPermission.values()) {
            commandBuilder.set(permissionCheckboxSelector(permission) + ".Value", role != null && role.hasPermission(permission));
        }
    }

    @Nonnull
    private static EventData buildSaveRolePermissionsEvent() {
        EventData eventData = new EventData()
                .append("Action", "SaveRolePermissions")
                .append("@RoleEditorRoleId", ROLE_EDITOR_ROLE_FIELD + ".Value");
        for (GuildPermission permission : GuildPermission.values()) {
            eventData.append(permissionEventKey(permission), permissionCheckboxSelector(permission) + ".Value");
        }
        return eventData;
    }

    @Nonnull
    private static String permissionEventKey(@Nonnull GuildPermission permission) {
        return "@Perm_" + permission.name();
    }

    @Nonnull
    private static String permissionCheckboxSelector(@Nonnull GuildPermission permission) {
        return switch (permission) {
            case INVITE -> PERMISSION_INVITE_CHECK;
            case KICK -> PERMISSION_KICK_CHECK;
            case PROMOTE -> PERMISSION_PROMOTE_CHECK;
            case MANAGE_ROLES -> PERMISSION_MANAGE_ROLES_CHECK;
            case SET_JOIN_POLICY -> PERMISSION_SET_JOIN_POLICY_CHECK;
            case ACCEPT_APPLICATIONS -> PERMISSION_ACCEPT_APPLICATIONS_CHECK;
            case REJECT_APPLICATIONS -> PERMISSION_REJECT_APPLICATIONS_CHECK;
            case MANAGE_GUILD_INFO -> PERMISSION_MANAGE_GUILD_INFO_CHECK;
            case MANAGE_GUILD_BANK -> PERMISSION_MANAGE_GUILD_BANK_CHECK;
            case MANAGE_GUILD_EVENTS -> PERMISSION_MANAGE_GUILD_EVENTS_CHECK;
            case MANAGE_GUILD_WAR -> PERMISSION_MANAGE_GUILD_WAR_CHECK;
            case MANAGE_GUILD_ALLIANCES -> PERMISSION_MANAGE_GUILD_ALLIANCES_CHECK;
            case USE_GUILD_CHAT -> PERMISSION_USE_GUILD_CHAT_CHECK;
            case USE_GUILD_BANK -> PERMISSION_USE_GUILD_BANK_CHECK;
        };
    }
}
