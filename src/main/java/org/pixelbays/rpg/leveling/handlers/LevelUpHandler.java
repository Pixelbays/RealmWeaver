package org.pixelbays.rpg.leveling.handlers;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;

@SuppressWarnings("null")
public class LevelUpHandler implements Consumer<LevelUpEvent> {

    @Override
    public void accept(@Nonnull LevelUpEvent event) {
        if (!event.playerRef().isValid()) {
            return;
        }

        RpgLogging.debugDeveloper(
            "[LevelUpEvent] player=%s system=%s oldLevel=%s newLevel=%s",
                event.playerRef(),
            event.systemId(),
                event.oldLevel(),
                event.newLevel());
    }
}
