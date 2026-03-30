package org.pixelbays.rpg.party.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;

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

public class PartyLeaveCommand extends AbstractPlayerCommand {

    private final PartyManager partyManager;

    public PartyLeaveCommand() {
        super("leave", "Leave your current party");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.partyManager = Realmweavers.get().getPartyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        PartyActionResult result = partyManager.leaveParty(playerRef.getUuid());
        player.sendMessage(PartyCommandUtil.managerResultMessage(result.getMessage()));
    }
}
