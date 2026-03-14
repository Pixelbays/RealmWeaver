package org.pixelbays.rpg.guild.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.GuildRole;
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
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class GuildPage extends CustomUIPage {

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String OVERVIEW_LABEL = "#OverviewLabel";
    private static final String ROSTER_LABEL = "#RosterLabel";

    private static final String INVITE_FIELD = "#InviteeField";
    private static final String GUILD_FIELD = "#GuildField";
    private static final String ROLE_NAME_FIELD = "#RoleNameField";
    private static final String ASSIGN_PLAYER_FIELD = "#AssignPlayerField";
    private static final String ASSIGN_ROLE_FIELD = "#AssignRoleField";
    private static final String PERM_ROLE_FIELD = "#PermRoleField";
    private static final String PERM_NAME_FIELD = "#PermNameField";
    private static final String PERM_ENABLED_CHECK = "#PermEnabledCheck";

    private static final String INVITE_BUTTON = "#InviteButton";
    private static final String APPLY_BUTTON = "#ApplyButton";
    private static final String CREATE_ROLE_BUTTON = "#CreateRoleButton";
    private static final String ASSIGN_ROLE_BUTTON = "#AssignRoleButton";
    private static final String SET_PERM_BUTTON = "#SetPermButton";

    private final GuildManager guildManager;

    public GuildPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/GuildPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String invitee = extractString(rawData, "@Invitee");
        String guildNameOrTag = extractString(rawData, "@Guild");
        String roleName = extractString(rawData, "@RoleName");
        String assignTarget = extractString(rawData, "@AssignTarget");
        String assignRoleId = extractString(rawData, "@AssignRoleId");
        String permRoleId = extractString(rawData, "@PermRoleId");
        String permission = extractString(rawData, "@Permission");
        Boolean enabled = extractBoolean(rawData, "@Enabled");

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> handleAction(ref, store, action, invitee, guildNameOrTag, roleName, assignTarget, assignRoleId,
                permRoleId, permission, enabled));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String invitee,
            @Nullable String guildNameOrTag,
            @Nullable String roleName,
            @Nullable String assignTarget,
            @Nullable String assignRoleId,
            @Nullable String permRoleId,
            @Nullable String permission,
            @Nullable Boolean enabled) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Message statusMessage = null;

        if ("Invite".equals(action)) {
            statusMessage = handleInvite(invitee);
        } else if ("Apply".equals(action)) {
            statusMessage = handleApply(guildNameOrTag);
        } else if ("CreateRole".equals(action)) {
            statusMessage = handleCreateRole(roleName);
        } else if ("AssignRole".equals(action)) {
            statusMessage = handleAssignRole(assignTarget, assignRoleId);
        } else if ("SetPerm".equals(action)) {
            statusMessage = handleSetPerm(permRoleId, permission, enabled);
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage);
        sendUpdate(commandBuilder);
    }

    private Message handleInvite(@Nullable String invitee) {
        if (invitee == null || invitee.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.invite");
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(invitee.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = guildManager.invitePlayer(playerRef.getUuid(), targetRef.getUuid());
        Message message = GuildCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.invitedBy").param("player", playerRef.getUsername()));
        }

        return message;
    }

    private Message handleApply(@Nullable String nameOrTag) {
        if (nameOrTag == null || nameOrTag.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.apply");
        }

        GuildActionResult result = guildManager.applyToGuild(playerRef.getUuid(), nameOrTag.trim());
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    private Message handleCreateRole(@Nullable String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.roleCreate");
        }

        GuildActionResult result = guildManager.createRole(playerRef.getUuid(), roleName.trim());
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    private Message handleAssignRole(@Nullable String targetName, @Nullable String roleId) {
        if (targetName == null || targetName.trim().isEmpty() || roleId == null || roleId.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.roleAssign");
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(targetName.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        GuildActionResult result = guildManager.assignRole(playerRef.getUuid(), targetRef.getUuid(), roleId.trim());
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    private Message handleSetPerm(@Nullable String roleId, @Nullable String permission, @Nullable Boolean enabled) {
        if (roleId == null || roleId.trim().isEmpty() || permission == null || permission.trim().isEmpty()
                || enabled == null) {
            return Message.translation("pixelbays.rpg.guild.usage.roleSetPerm");
        }

        GuildPermission perm;
        try {
            perm = GuildPermission.valueOf(permission.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Message.translation("pixelbays.rpg.guild.error.unknownPermission");
        }

        GuildActionResult result = guildManager.setRolePermission(playerRef.getUuid(), roleId.trim(), perm, enabled);
        return GuildCommandUtil.managerResultMessage(result.getMessage());
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
                APPLY_BUTTON,
                new EventData()
                        .append("Action", "Apply")
                        .append("@Guild", GUILD_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                CREATE_ROLE_BUTTON,
                new EventData()
                        .append("Action", "CreateRole")
                        .append("@RoleName", ROLE_NAME_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                ASSIGN_ROLE_BUTTON,
                new EventData()
                        .append("Action", "AssignRole")
                        .append("@AssignTarget", ASSIGN_PLAYER_FIELD + ".Value")
                        .append("@AssignRoleId", ASSIGN_ROLE_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SET_PERM_BUTTON,
                new EventData()
                        .append("Action", "SetPerm")
                        .append("@PermRoleId", PERM_ROLE_FIELD + ".Value")
                        .append("@Permission", PERM_NAME_FIELD + ".Value")
                        .append("@Enabled", PERM_ENABLED_CHECK + ".Value"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder, @Nullable Message statusMessage) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        if (guild == null) {
            commandBuilder.set(OVERVIEW_LABEL + ".Text", "");
            commandBuilder.set(ROSTER_LABEL + ".Text", "");
            return;
        }

        Map<String, String> overviewParams = new HashMap<>();
        overviewParams.put("name", guild.getName());
        overviewParams.put("tag", guild.getTag() != null ? guild.getTag() : "");
        overviewParams.put("leader", GuildCommandUtil.resolveDisplayName(guild.getLeaderId()));
        overviewParams.put("members", String.valueOf(guild.size()));
        overviewParams.put("policy", String.valueOf(guild.getJoinPolicy()).toLowerCase(Locale.ROOT));

        commandBuilder.setObject(OVERVIEW_LABEL + ".Text",
                LocalizableString.fromMessageId("pixelbays.rpg.guild.ui.overviewBody", overviewParams));

        StringBuilder rosterBuilder = new StringBuilder();
        for (var member : guild.getMemberList()) {
            if (rosterBuilder.length() > 0) {
                rosterBuilder.append("\n");
            }

            String memberName = GuildCommandUtil.resolveDisplayName(member.getMemberId());
            String roleId = member.getRoleId();
            GuildRole role = guild.getRole(roleId);
            String roleName = role != null ? role.getName() : roleId;

            rosterBuilder.append(memberName).append(" (").append(roleName).append(")");
        }

        commandBuilder.set(ROSTER_LABEL + ".Text", rosterBuilder.toString());
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

        int secondQuote = rawData.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return rawData.substring(firstQuote + 1, secondQuote);
    }

    @Nullable
    private static Boolean extractBoolean(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < rawData.length() && Character.isWhitespace(rawData.charAt(valueStart))) {
            valueStart++;
        }

        if (rawData.startsWith("true", valueStart)) {
            return Boolean.TRUE;
        }
        if (rawData.startsWith("false", valueStart)) {
            return Boolean.FALSE;
        }

        return null;
    }
}
