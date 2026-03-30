package org.pixelbays.rpg.character.command;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.character.token.CharacterTokenActionResult;
import org.pixelbays.rpg.character.token.CharacterTokenDefinition;
import org.pixelbays.rpg.character.token.CharacterTokenRegistry;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

@SuppressWarnings("null")
public final class CharacterTokenCommandUtil {

    private CharacterTokenCommandUtil() {
    }

        public static PlayerRef findPlayerByName(String name) {
                return Universe.get().getPlayerByUsername(nonNull(name), NameMatching.DEFAULT);
    }

    @Nonnull
    public static Message resultMessage(@Nonnull CharacterTokenActionResult result) {
        String tokenId = nonNull(result.getTokenId());
        CharacterTokenDefinition definition = CharacterTokenRegistry.get(tokenId);
        String tokenName = definition == null ? tokenId : nonNull(definition.getDisplayName());

        return switch (result.getMessage()) {
            case "Unknown token type." -> Message.translation("pixelbays.rpg.character.token.error.unknownType")
                    .param("token", nonNull(tokenId));
            case "Token type is disabled." -> Message.translation("pixelbays.rpg.character.token.error.disabledType")
                    .param("token", nonNull(tokenName));
            case "Amount must be zero or greater." -> Message.translation("pixelbays.rpg.character.token.error.invalidAmount");
            case "Insufficient tokens." -> Message.translation("pixelbays.rpg.character.token.error.insufficient")
                    .param("token", nonNull(tokenName));
            case "Added token balance." -> Message.translation("pixelbays.rpg.character.token.success.added")
                    .param("token", nonNull(tokenName))
                    .param("balance", nonNull(Long.toString(result.getBalance())));
            case "Removed token balance." -> Message.translation("pixelbays.rpg.character.token.success.removed")
                    .param("token", nonNull(tokenName))
                    .param("balance", nonNull(Long.toString(result.getBalance())));
            case "Set token balance." -> Message.translation("pixelbays.rpg.character.token.success.set")
                    .param("token", nonNull(tokenName))
                    .param("balance", nonNull(Long.toString(result.getBalance())));
            case "Spent token balance." -> Message.translation("pixelbays.rpg.character.token.success.spent")
                    .param("token", nonNull(tokenName))
                    .param("balance", nonNull(Long.toString(result.getBalance())));
            default -> Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", nonNull(result.getMessage()));
        };
    }

    @Nonnull
    static String nonNull(String value) {
        return value == null ? "" : value;
    }
}