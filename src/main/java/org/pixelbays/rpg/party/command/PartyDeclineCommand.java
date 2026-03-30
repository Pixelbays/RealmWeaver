package org.pixelbays.rpg.party.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PartyDeclineCommand extends AbstractPlayerCommand {

    private final PartyManager partyManager;

    public PartyDeclineCommand() {
        super("decline", "Decline your current party invite");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.partyManager = ExamplePlugin.get().getPartyManager();
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

        PartyActionResult result = partyManager.declineInvite(playerRef.getUuid());
        player.sendMessage(PartyCommandUtil.managerResultMessage(result.getMessage()));
    }
}
