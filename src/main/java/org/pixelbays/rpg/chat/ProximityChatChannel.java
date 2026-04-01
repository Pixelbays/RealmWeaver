package org.pixelbays.rpg.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

@SuppressWarnings("null")
public final class ProximityChatChannel extends BaseConfiguredChatChannel {

    private final int rangeBlocks;

    public ProximityChatChannel(@Nonnull ChatChannelDefinition definition) {
        this(null, definition);
    }

    public ProximityChatChannel(@Nullable CharacterManager characterManager, @Nonnull ChatChannelDefinition definition) {
        super(characterManager, definition);
        this.rangeBlocks = definition.getRangeBlocks();
    }

    @Override
    public boolean canSend(@Nonnull PlayerRef sender) {
        return rangeBlocks > 0 && sender.getWorldUuid() != null;
    }

    @Override
    @Nonnull
    public List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender) {
        return ChatTargetingSupport.resolvePlayersInRange(sender, rangeBlocks);
    }

    @Override
    @Nonnull
    public PlayerChatEvent.Formatter getFormatter() {
        return (sender, msg) -> finalizeMessage(createBaseMessage(sender, msg)
                .param("range", Integer.toString(rangeBlocks)));
    }
}