package org.pixelbays.rpg.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class GlobalChatChannel extends BaseConfiguredChatChannel {

    private final ChatManager chatManager;

    public GlobalChatChannel(@Nonnull ChatManager chatManager, @Nonnull ChatChannelDefinition definition) {
        this(chatManager, null, definition);
    }

    public GlobalChatChannel(
            @Nonnull ChatManager chatManager,
            @Nullable CharacterManager characterManager,
            @Nonnull ChatChannelDefinition definition) {
        super(characterManager, definition);
        this.chatManager = chatManager;
    }

    @Override
    public boolean isJoinable() {
        return true;
    }

    @Override
    public boolean canSend(@Nonnull PlayerRef sender) {
        return chatManager.isJoined(sender.getUuid(), getId());
    }

    @Override
    @Nonnull
    public List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender) {
        return chatManager.getJoinedPlayers(getId());
    }

    @Override
    @Nonnull
    public PlayerChatEvent.Formatter getFormatter() {
        return (sender, msg) -> finalizeMessage(createBaseMessage(sender, msg));
    }
}