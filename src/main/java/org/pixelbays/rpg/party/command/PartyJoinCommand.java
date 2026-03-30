package org.pixelbays.rpg.party.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;

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

public class PartyJoinCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;
    private final PartyManager partyManager;

    public PartyJoinCommand() {
        super("join", "Join a party you were invited to");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.playerArg = this.withRequiredArg("player", "Party leader or member", ArgTypes.STRING);
        this.partyManager = ExamplePlugin.get().getPartyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String targetName = playerArg.get(ctx);
        if (targetName == null || targetName.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.party.usage.join"));
            return;
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        PartyActionResult result = partyManager.joinParty(playerRef.getUuid(), targetRef.getUuid());
        player.sendMessage(PartyCommandUtil.managerResultMessage(result.getMessage()));
    }
}
