package org.pixelbays.rpg.party.command;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyRole;
import org.pixelbays.rpg.party.ui.PartyUiSnapshot;

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
public class PartyInfoCommand extends AbstractPlayerCommand {

    private final PartyManager partyManager;

    public PartyInfoCommand() {
        super("info", "Show info about your party");
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
        PartyUiSnapshot snapshot = partyManager.getPartyUiSnapshot(playerRef.getUuid());
        if (snapshot == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.party.error.notInParty"));
            return;
        }

        String leaderName = snapshot.getMembers().stream()
                .filter(member -> member.getRole() == PartyRole.LEADER)
                .map(PartyUiSnapshot.MemberView::getDisplayName)
                .findFirst()
                .orElse("None");
        String assistants = snapshot.getMembers().stream()
                .filter(member -> member.getRole() == PartyRole.ASSISTANT)
                .map(PartyUiSnapshot.MemberView::getDisplayName)
                .collect(Collectors.joining(", "));
        String members = snapshot.getMembers().stream()
                .filter(member -> member.getRole() == PartyRole.MEMBER)
                .map(PartyUiSnapshot.MemberView::getDisplayName)
                .collect(Collectors.joining(", "));

        player.sendMessage(Message.translation("pixelbays.rpg.party.info.id").param("id", nn(snapshot.getPartyId().toString())));
        player.sendMessage(Message.translation("pixelbays.rpg.party.info.type")
                .param("type", PartyCommandUtil.partyTypeMessage(snapshot.getPartyType())));
        player.sendMessage(Message.translation("pixelbays.rpg.party.info.size")
                .param("size", nn(String.valueOf(snapshot.getMemberCount())))
                .param("max", nn(String.valueOf(snapshot.getMaxSize()))));
        player.sendMessage(Message.translation("pixelbays.rpg.party.info.leader").param("leader", nn(leaderName)));
        if (assistants.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.party.info.assistants")
                    .param("assistants", Message.translation("pixelbays.rpg.common.none")));
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.party.info.assistants")
                    .param("assistants", assistants));
        }
        if (members.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.party.info.members")
                    .param("members", Message.translation("pixelbays.rpg.common.none")));
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.party.info.members")
                    .param("members", members));
        }
    }

        @Nonnull
        private static String nn(@Nonnull String value) {
                return value;
        }
}
