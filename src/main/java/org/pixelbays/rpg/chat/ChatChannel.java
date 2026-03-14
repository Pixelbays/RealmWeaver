package org.pixelbays.rpg.chat;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public interface ChatChannel {

    @Nonnull
    String getId();

    @Nonnull
    default List<String> getAliases() {
        return List.of();
    }

    boolean canSend(@Nonnull PlayerRef sender);

    @Nonnull
    List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender);

    @Nonnull
    PlayerChatEvent.Formatter getFormatter();
}
