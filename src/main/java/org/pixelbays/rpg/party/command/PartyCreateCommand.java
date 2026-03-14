package org.pixelbays.rpg.party.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyType;

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

public class PartyCreateCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> typeArg;
    private final PartyManager partyManager;

    public PartyCreateCommand() {
        super("create", "Create a new party or raid");
        this.typeArg = null;
        this.partyManager = ExamplePlugin.get().getPartyManager();
        this.addUsageVariant(new PartyCreateCommand("Create a new party or raid"));
    }

    private PartyCreateCommand(String description) {
        super(description);
        this.typeArg = this.withRequiredArg("type", "party or raid", ArgTypes.STRING);
        this.partyManager = ExamplePlugin.get().getPartyManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        PartyType type = PartyType.PARTY;
        if (typeArg != null) {
            String rawType = typeArg.get(ctx);
            if (rawType == null || rawType.isBlank()) {
                player.sendMessage(Message.translation("pixelbays.rpg.party.error.invalidType"));
                return;
            }

            String normalizedType = rawType.trim().toLowerCase();
            if ("raid".equals(normalizedType)) {
                type = PartyType.RAID;
            } else if (!"party".equals(normalizedType)) {
                player.sendMessage(Message.translation("pixelbays.rpg.party.error.invalidType"));
                return;
            }
        }

        PartyActionResult result = partyManager.createParty(playerRef.getUuid(), type);
        player.sendMessage(PartyCommandUtil.managerResultMessage(result.getMessage()));
    }
}
