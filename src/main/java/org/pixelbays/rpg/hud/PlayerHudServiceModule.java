package org.pixelbays.rpg.hud;

import java.util.UUID;

import javax.annotation.Nonnull;

interface PlayerHudServiceModule {

    void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context);

    void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context);

    default void remove(@Nonnull UUID playerId) {
    }
}