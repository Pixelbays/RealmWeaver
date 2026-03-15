package org.pixelbays.rpg.mail.command;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.character.CharacterManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

@SuppressWarnings("null")
public final class MailCommandUtil {

    private MailCommandUtil() {
    }

    @Nullable
    public static UUID resolveRecipientAccountId(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String trimmed = name.trim();
        PlayerRef playerRef = Universe.get().getPlayerByUsername(trimmed, NameMatching.DEFAULT);
        if (playerRef != null) {
            return playerRef.getUuid();
        }

        CharacterManager characterManager = ExamplePlugin.get().getCharacterManager();
        for (UUID accountId : characterManager.getKnownAccountIds()) {
            String username = characterManager.resolveAccountUsername(accountId);
            if (username != null && username.equalsIgnoreCase(trimmed)) {
                return accountId;
            }
        }
        return null;
    }

    @Nonnull
    public static String resolveDisplayName(@Nonnull UUID accountId) {
        PlayerRef playerRef = Universe.get().getPlayer(accountId);
        if (playerRef != null && playerRef.getUsername() != null && !playerRef.getUsername().isBlank()) {
            return playerRef.getUsername();
        }

        String username = ExamplePlugin.get().getCharacterManager().resolveAccountUsername(accountId);
        return username == null || username.isBlank() ? accountId.toString() : username;
    }

    @Nonnull
    public static Message managerResultMessage(@Nullable String message) {
        if (message == null || message.isBlank()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        return switch (message) {
            case "Recipient required." -> Message.translation("pixelbays.rpg.mail.ui.error.recipientRequired");
            case "Player not found." -> Message.translation("pixelbays.rpg.common.playerNotFound");
            case "Mail is disabled." -> Message.translation("pixelbays.rpg.mail.error.disabled");
            case "Mail subject is too long." -> Message.translation("pixelbays.rpg.mail.error.subjectTooLong");
            case "Mail body is too long." -> Message.translation("pixelbays.rpg.mail.error.bodyTooLong");
            case "Too many item attachments." -> Message.translation("pixelbays.rpg.mail.error.tooManyAttachments");
            case "Item attachments are disabled." -> Message.translation("pixelbays.rpg.mail.error.itemAttachmentsDisabled");
            case "Currency attachments are disabled." -> Message.translation("pixelbays.rpg.mail.error.currencyAttachmentsDisabled");
            case "Cash on delivery is disabled." -> Message.translation("pixelbays.rpg.mail.error.codDisabled");
            case "Cash on delivery requires an attachment." -> Message.translation("pixelbays.rpg.mail.error.codRequiresAttachment");
            case "Mail must contain text, items, or currency." -> Message.translation("pixelbays.rpg.mail.error.emptyMail");
            case "Recipient mailbox could not be resolved." -> Message.translation("pixelbays.rpg.mail.error.recipientUnavailable");
            case "Mail is on cooldown." -> Message.translation("pixelbays.rpg.mail.error.cooldown");
            case "Mail queued." -> Message.translation("pixelbays.rpg.mail.success.queued");
            case "Mail not found." -> Message.translation("pixelbays.rpg.mail.error.notFound");
            case "Mail has not been delivered yet." -> Message.translation("pixelbays.rpg.mail.error.notDelivered");
            case "Mail marked as read." -> Message.translation("pixelbays.rpg.mail.success.markedRead");
            default -> Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", message);
        };
    }
}
