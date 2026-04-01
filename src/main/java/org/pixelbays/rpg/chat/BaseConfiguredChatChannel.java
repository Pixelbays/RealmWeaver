package org.pixelbays.rpg.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public abstract class BaseConfiguredChatChannel implements ChatChannel {

    @Nullable
    private final CharacterManager characterManager;
    private final String id;
    private final List<String> aliases;
    private final String formatTranslationKey;
    private final String color;
    private final ChatChannelDefinition.NameDisplayType nameDisplayType;

    protected BaseConfiguredChatChannel(
            @Nullable CharacterManager characterManager,
            @Nonnull ChatChannelDefinition definition) {
        this.characterManager = characterManager;
        this.id = definition.getId();
        this.aliases = List.copyOf(definition.getAliases());
        this.formatTranslationKey = definition.getFormatTranslationKey();
        this.color = definition.getColor();
        this.nameDisplayType = definition.getNameDisplayType();
    }

    @Override
    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    @Nonnull
    public List<String> getAliases() {
        return aliases;
    }

    @Nonnull
    protected Message createBaseMessage(@Nonnull PlayerRef sender, @Nonnull String message) {
        return Message.translation(formatTranslationKey)
                .param("channel", id)
                .param("username", resolveDisplayName(sender))
                .param("message", message);
    }

    @Nonnull
    protected Message finalizeMessage(@Nonnull Message message) {
        if (!color.isBlank()) {
            message.color(color);
        }
        return message;
    }

    @Nonnull
    protected String resolveDisplayName(@Nonnull PlayerRef sender) {
        if (characterManager == null) {
            return sender.getUsername();
        }
        return characterManager.resolveChatDisplayName(
                sender.getUuid(),
                sender.getUsername(),
                nameDisplayType.usesCharacterName());
    }
}