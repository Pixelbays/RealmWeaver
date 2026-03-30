package org.pixelbays.rpg.character.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.token.CharacterTokenActionResult;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class CharacterTokenSetCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> tokenIdArg;
    private final RequiredArg<Integer> amountArg;
    private final CharacterManager characterManager;

    public CharacterTokenSetCommand() {
        super("set", "Set character tokens for an online player");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.tokenIdArg = this.withRequiredArg("tokenId", "Token id", ArgTypes.STRING);
        this.amountArg = this.withRequiredArg("amount", "Amount", ArgTypes.INTEGER);
        this.characterManager = Realmweavers.get().getCharacterManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String targetName = playerArg.get(ctx);
        String tokenId = tokenIdArg.get(ctx);
        Integer amount = amountArg.get(ctx);
        if (player == null || targetName == null || tokenId == null || amount == null) {
            return;
        }

        PlayerRef targetRef = CharacterTokenCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        CharacterTokenActionResult result = characterManager.setAccountTokenBalance(
                targetRef.getUuid(),
                targetRef.getUsername(),
                tokenId,
                amount.longValue());
        player.sendMessage(CharacterTokenCommandUtil.resultMessage(result));
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.character.token.notify.changed")
                    .param("token", CharacterTokenCommandUtil.nonNull(tokenId))
                    .param("balance", CharacterTokenCommandUtil.nonNull(Long.toString(result.getBalance()))));
        }
    }
}