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
            player.sendMessage(Message.translation("server.rpg.party.error.notInParty"));
            return;
        }

        String leaderName = PartyCommandUtil.resolveDisplayName(party.getLeaderId());
        String assistants = party.getAssistants().stream()
                .map(PartyCommandUtil::resolveDisplayName)
                .collect(Collectors.joining(", "));
        String members = party.getMembers().keySet().stream()
                .map(PartyCommandUtil::resolveDisplayName)
                .collect(Collectors.joining(", "));

        player.sendMessage(Message.translation("server.rpg.party.info.id").param("id", party.getId().toString()));
        player.sendMessage(Message.translation("server.rpg.party.info.type").param("type", party.getType().name()));
        player.sendMessage(Message.translation("server.rpg.party.info.leader").param("leader", leaderName));
        player.sendMessage(Message.translation("server.rpg.party.info.assistants")
            .param("assistants", assistants.isEmpty() ? "None" : assistants));
        player.sendMessage(Message.translation("server.rpg.party.info.members")
            .param("members", members.isEmpty() ? "None" : members));
    }
}
