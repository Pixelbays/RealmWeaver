package org.pixelbays.rpg.character.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.token.CharacterTokenDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class CharacterTokenInfoCommand extends AbstractPlayerCommand {

    private final CharacterManager characterManager;

    public CharacterTokenInfoCommand() {
        super("info", "Show your account character tokens");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.characterManager = Realmweavers.get().getCharacterManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Map<CharacterTokenDefinition, Long> balances = characterManager.getVisibleAccountTokenBalances(
                playerRef.getUuid(),
                playerRef.getUsername());
        if (balances.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.character.token.info.none"));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.character.token.info.header"));
        for (Map.Entry<CharacterTokenDefinition, Long> entry : balances.entrySet()) {
            CharacterTokenDefinition definition = entry.getKey();
            if (definition == null) {
                continue;
            }
            player.sendMessage(Message.translation("pixelbays.rpg.character.token.info.entry")
                    .param("token", CharacterTokenCommandUtil.nonNull(definition.getDisplayName()))
                    .param("balance", CharacterTokenCommandUtil.nonNull(Long.toString(entry.getValue()))));
        }
    }
}