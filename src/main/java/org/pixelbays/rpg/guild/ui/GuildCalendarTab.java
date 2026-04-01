package org.pixelbays.rpg.guild.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.guild.Guild;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

final class GuildCalendarTab {

    private static final String CALENDAR_LABEL = "#CalendarLabel";

    void populate(@Nonnull UICommandBuilder commandBuilder, @Nullable Guild guild) {
        if (guild == null) {
            GuildPageSupport.setLocalizedText(commandBuilder, CALENDAR_LABEL, "pixelbays.rpg.guild.ui.calendarNoGuild");
            return;
        }

        GuildPageSupport.setLocalizedText(commandBuilder, CALENDAR_LABEL, "pixelbays.rpg.guild.ui.calendarUnavailable");
    }
}
