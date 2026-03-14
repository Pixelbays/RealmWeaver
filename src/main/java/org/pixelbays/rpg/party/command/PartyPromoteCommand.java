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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PartyPromoteCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> roleArg;
    private final PartyManager partyManager;

    public PartyPromoteCommand() {
        super("promote", "Promote a party member");
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.roleArg = null;
        this.partyManager = ExamplePlugin.get().getPartyManager();
        this.addUsageVariant(new PartyPromoteCommand("Promote a party member"));
    }

    private PartyPromoteCommand(String description) {
        super(description);
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.roleArg = this.withRequiredArg("role", "assistant or leader", ArgTypes.STRING);
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
            player.sendMessage(Message.translation("pixelbays.rpg.party.usage.promote"));
            return;
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        String role = roleArg == null ? "assistant" : roleArg.get(ctx);
        PartyActionResult result;
        if (role != null && role.equalsIgnoreCase("leader")) {
            result = partyManager.promoteToLeader(playerRef.getUuid(), targetRef.getUuid());
        } else if (role == null || role.equalsIgnoreCase("assistant")) {
            result = partyManager.promoteToAssistant(playerRef.getUuid(), targetRef.getUuid());
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.party.error.invalidPromoteRole"));
            return;
        }

        player.sendMessage(PartyCommandUtil.managerResultMessage(result.getMessage()));
    }
}
