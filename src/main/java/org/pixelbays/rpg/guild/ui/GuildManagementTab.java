package org.pixelbays.rpg.guild.ui;

import java.util.Map;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

final class GuildManagementTab {

    private static final String GUILD_INFO_SUMMARY_LABEL = "#GuildInfoSummaryLabel";
    private static final String JOIN_POLICY_ROW = "#JoinPolicyRow";
    private static final String GUILD_NAME_TAG_ROW = "#GuildNameTagRow";
    private static final String GUILD_DESCRIPTION_ROW = "#GuildDescriptionRow";
    private static final String GUILD_MOTD_ROW = "#GuildMotdRow";
    private static final String GUILD_NAME_FIELD = "#GuildNameField";
    private static final String GUILD_TAG_FIELD = "#GuildTagField";
    private static final String DESCRIPTION_FIELD = "#DescriptionField";
    private static final String MOTD_FIELD = "#MotdField";
    private static final String SAVE_GUILD_INFO_BUTTON = "#SaveGuildInfoButton";
    private static final String JOIN_POLICY_INVITE_BUTTON = "#JoinPolicyInviteButton";
    private static final String JOIN_POLICY_OPEN_BUTTON = "#JoinPolicyOpenButton";
    private static final String JOIN_POLICY_APPLICATION_BUTTON = "#JoinPolicyApplicationButton";

    private final GuildPage page;

    GuildManagementTab(@Nonnull GuildPage page) {
        this.page = page;
    }

    void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                SAVE_GUILD_INFO_BUTTON,
                new EventData()
                        .append("Action", "UpdateGuildInfo")
                        .append("@GuildName", GUILD_NAME_FIELD + ".Value")
                        .append("@GuildTag", GUILD_TAG_FIELD + ".Value")
                        .append("@Description", DESCRIPTION_FIELD + ".Value")
                        .append("@Motd", MOTD_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                JOIN_POLICY_INVITE_BUTTON,
                new EventData().append("Action", "SetJoinPolicy").append("JoinPolicy", "invite"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                JOIN_POLICY_OPEN_BUTTON,
                new EventData().append("Action", "SetJoinPolicy").append("JoinPolicy", "open"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                JOIN_POLICY_APPLICATION_BUTTON,
                new EventData().append("Action", "SetJoinPolicy").append("JoinPolicy", "application"));
    }

    @Nullable
    Message handleAction(@Nonnull GuildPageActionData actionData) {
        return switch (actionData.action()) {
            case "SetJoinPolicy" -> handleJoinPolicy(actionData.joinPolicy());
            case "UpdateGuildInfo" -> handleUpdateGuildInfo(
                    actionData.guildName(),
                    actionData.guildTag(),
                    actionData.description(),
                    actionData.motd());
            default -> null;
        };
    }

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        boolean canSetJoinPolicy = guild != null
                && guild.hasPermission(page.playerRef().getUuid(), GuildPermission.SET_JOIN_POLICY);
        boolean canManageInfo = guild != null
                && guild.hasPermission(page.playerRef().getUuid(), GuildPermission.MANAGE_GUILD_INFO);
        RpgModConfig config = GuildPageSupport.resolveConfig();
        boolean allowIdentityUpdates = config == null || config.isGuildAllowNameTagUpdates();

        commandBuilder.set(JOIN_POLICY_ROW + ".Visible", canSetJoinPolicy);
        commandBuilder.set(GUILD_NAME_TAG_ROW + ".Visible", canManageInfo && allowIdentityUpdates);
        commandBuilder.set(GUILD_DESCRIPTION_ROW + ".Visible", canManageInfo);
        commandBuilder.set(GUILD_MOTD_ROW + ".Visible", canManageInfo);
        commandBuilder.set(SAVE_GUILD_INFO_BUTTON + ".Visible", canManageInfo);

        if (guild == null) {
            commandBuilder.set(GUILD_NAME_FIELD + ".Value", "");
            commandBuilder.set(GUILD_TAG_FIELD + ".Value", "");
            commandBuilder.set(DESCRIPTION_FIELD + ".Value", "");
            commandBuilder.set(MOTD_FIELD + ".Value", "");
            commandBuilder.set(GUILD_INFO_SUMMARY_LABEL + ".Text", "");
            return;
        }

        commandBuilder.set(GUILD_NAME_FIELD + ".Value", guild.getName());
        commandBuilder.set(GUILD_TAG_FIELD + ".Value", guild.getTag() == null ? "" : guild.getTag());
        commandBuilder.set(DESCRIPTION_FIELD + ".Value", guild.getDescription());
        commandBuilder.set(MOTD_FIELD + ".Value", guild.getMotd());
        commandBuilder.set(
                GUILD_INFO_SUMMARY_LABEL + ".Text",
                GuildPageSupport.resolveLocalizedText(
                        "pixelbays.rpg.guild.ui.guildInfoSummary",
                        Map.of(
                                "name", guild.getName(),
                                "tag", guild.getTag() == null || guild.getTag().isBlank() ? "-" : guild.getTag(),
                                "description", guild.getDescription().isBlank()
                                        ? GuildPageSupport.rawText("pixelbays.rpg.common.none", "None")
                                        : guild.getDescription(),
                                "motd", guild.getMotd().isBlank()
                                        ? GuildPageSupport.rawText("pixelbays.rpg.common.none", "None")
                                        : guild.getMotd())));
    }

    @Nonnull
    private Message handleUpdateGuildInfo(@Nullable String guildName,
            @Nullable String guildTag,
            @Nullable String description,
            @Nullable String motd) {
        GuildActionResult result = page.guildManager.updateGuildInfo(
                page.playerRef().getUuid(),
                guildName,
                guildTag,
                description,
                motd);
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }

    @Nonnull
    private Message handleJoinPolicy(@Nullable String rawPolicy) {
        if (rawPolicy == null || rawPolicy.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.guild.usage.joinpolicy");
        }

        GuildJoinPolicy joinPolicy = switch (rawPolicy.trim().toLowerCase(Locale.ROOT)) {
            case "invite", "invite_only", "invited" -> GuildJoinPolicy.INVITE_ONLY;
            case "open", "public" -> GuildJoinPolicy.OPEN;
            case "application", "apply", "apps" -> GuildJoinPolicy.APPLICATION;
            default -> null;
        };
        if (joinPolicy == null) {
            return Message.translation("pixelbays.rpg.guild.error.invalidPolicy");
        }

        GuildActionResult result = page.guildManager.setJoinPolicy(page.playerRef().getUuid(), joinPolicy);
        return GuildCommandUtil.managerResultMessage(result.getMessage());
    }
}
