package org.pixelbays.rpg.party.command;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PartyInfoCommand extends AbstractPlayerCommand {

    private final PartyManager partyManager;

    public PartyInfoCommand() {
        super("info", "Show info about your party");
        this.partyManager = ExamplePlugin.get().getPartyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        Party party = partyManager.getPartyForMember(playerRef.getUuid());
        if (party == null) {
            player.sendMessage(Message.raw("You are not in a party."));
            return;
        }

        String leaderName = PartyCommandUtil.resolveDisplayName(party.getLeaderId());
        String assistants = party.getAssistants().stream()
                .map(PartyCommandUtil::resolveDisplayName)
                .collect(Collectors.joining(", "));
        String members = party.getMembers().keySet().stream()
                .map(PartyCommandUtil::resolveDisplayName)
                .collect(Collectors.joining(", "));

        player.sendMessage(Message.raw("Party: " + party.getId()));
        player.sendMessage(Message.raw("Type: " + party.getType()));
        player.sendMessage(Message.raw("Leader: " + leaderName));
        player.sendMessage(Message.raw("Assistants: " + (assistants.isEmpty() ? "None" : assistants)));
        player.sendMessage(Message.raw("Members: " + (members.isEmpty() ? "None" : members)));
    }
}
