package org.pixelbays.rpg.guild.ui;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.command.GuildCommandUtil;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

final class GuildOverviewTab {

    private static final String OVERVIEW_LABEL = "#OverviewLabel";

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        if (guild == null) {
            GuildPageSupport.setLocalizedText(commandBuilder, OVERVIEW_LABEL, "pixelbays.rpg.guild.ui.noGuild");
            return;
        }

        Map<String, String> overviewParams = new HashMap<>();
        overviewParams.put("name", guild.getName());
        overviewParams.put("tag", guild.getTag() != null && !guild.getTag().isBlank() ? guild.getTag() : "-");
        overviewParams.put("leader", GuildCommandUtil.resolveDisplayName(guild.getLeaderId()));
        overviewParams.put("members", String.valueOf(guild.size()));
        overviewParams.put("policy", GuildPageSupport.joinPolicyDisplayName(guild.getJoinPolicy()));
        overviewParams.put("description", guild.getDescription().isBlank()
                ? GuildPageSupport.rawText("pixelbays.rpg.common.none", "None")
                : guild.getDescription());
        overviewParams.put("motd", guild.getMotd().isBlank()
                ? GuildPageSupport.rawText("pixelbays.rpg.common.none", "None")
                : guild.getMotd());

        GuildPageSupport.setLocalizedText(
                commandBuilder,
                OVERVIEW_LABEL,
                "pixelbays.rpg.guild.ui.overviewBody",
                overviewParams);
    }
}
