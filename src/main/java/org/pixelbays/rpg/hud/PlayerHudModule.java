package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public interface PlayerHudModule {

    void build(@Nonnull UICommandBuilder cmd);
}