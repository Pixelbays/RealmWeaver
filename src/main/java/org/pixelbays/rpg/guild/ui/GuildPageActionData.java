package org.pixelbays.rpg.guild.ui;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.guild.GuildPermission;

record GuildPageActionData(
        @Nonnull String action,
        @Nullable String tab,
        @Nullable String invitee,
        @Nullable String applicationApplicantId,
        @Nullable String outgoingInviteTargetId,
        @Nullable String roleName,
        @Nullable String guildName,
        @Nullable String guildTag,
        @Nullable String description,
        @Nullable String motd,
        @Nullable String rosterMemberId,
        @Nullable String rosterRoleId,
        @Nullable String editorRoleId,
        @Nullable String editorRoleName,
        @Nullable String joinPolicy,
        @Nonnull Map<GuildPermission, Boolean> permissionValues) {

    @Nonnull
    static GuildPageActionData from(@Nonnull String action, @Nonnull String rawData) {
        return new GuildPageActionData(
                action,
                GuildPageSupport.extractString(rawData, "Tab"),
                GuildPageSupport.extractString(rawData, "@Invitee"),
                GuildPageSupport.extractString(rawData, "@ApplicationApplicantId"),
                GuildPageSupport.extractString(rawData, "@OutgoingInviteTargetId"),
                GuildPageSupport.extractString(rawData, "@RoleName"),
                GuildPageSupport.extractString(rawData, "@GuildName"),
                GuildPageSupport.extractString(rawData, "@GuildTag"),
                GuildPageSupport.extractString(rawData, "@Description"),
                GuildPageSupport.extractString(rawData, "@Motd"),
                GuildPageSupport.extractString(rawData, "@RosterMemberId"),
                GuildPageSupport.extractString(rawData, "@RosterRoleId"),
                GuildPageSupport.extractString(rawData, "@RoleEditorRoleId"),
                GuildPageSupport.extractString(rawData, "@RoleEditorRoleName"),
                GuildPageSupport.extractString(rawData, "JoinPolicy"),
                GuildRolesTab.collectPermissionValues(rawData));
    }
}
