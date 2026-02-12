package org.pixelbays.rpg.party.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PartyJoinCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> playerArg;
    private final PartyManager partyManager;

    public PartyJoinCommand() {
        super("join", "Join a party you were invited to");
        this.playerArg = this.withOptionalArg("player", "Party leader or member", ArgTypes.STRING);
        this.partyManager = ExamplePlugin.get().getPartyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!playerArg.provided(ctx)) {
            player.sendMessage(Message.raw("Usage: /party join <player>"));
            return;
        }

        String targetName = playerArg.get(ctx);
        if (targetName == null || targetName.isEmpty()) {
            player.sendMessage(Message.raw("Usage: /party join <player>"));
            return;
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.raw("Player not found."));
            return;
        }

        PartyActionResult result = partyManager.joinParty(playerRef.getUuid(), targetRef.getUuid());
        player.sendMessage(Message.raw(result.getMessage()));
    }
}
