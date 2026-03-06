package org.pixelbays.rpg.race.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;
import org.pixelbays.rpg.race.system.RaceSystem;

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

/**
 * /race select <raceId>
 */
@SuppressWarnings("null")
public class RaceSelectCommand extends AbstractPlayerCommand {

    private final RaceManagementSystem raceManagementSystem;
    private final RaceSystem raceSystem;
    private final RequiredArg<String> raceIdArg;

    public RaceSelectCommand() {
        super("select", "Select a race");
        this.raceManagementSystem = ExamplePlugin.get().getRaceManagementSystem();
        this.raceSystem = ExamplePlugin.get().getRaceSystem();
        this.raceIdArg = this.withRequiredArg("raceId", "Race to select", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String raceId = raceIdArg.get(ctx);

        RaceDefinition raceDef = raceManagementSystem.getRaceDefinition(raceId);
        if (raceDef == null) {
            player.sendMessage(Message.translation("server.rpg.race.error.unknown").param("id", raceId));
            return;
        }

        if (!raceDef.isEnabled()) {
            player.sendMessage(Message.translation("server.rpg.race.error.disabled").param("id", raceId));
            return;
        }

        String activeRaceId = raceSystem.getRaceId(ref);
        if (activeRaceId != null && !activeRaceId.isEmpty() && activeRaceId.equals(raceId)) {
            player.sendMessage(Message.translation("server.rpg.race.error.alreadySelected"));
            return;
        }

        boolean applied = raceSystem.setRace(ref, raceId, store);
        if (!applied) {
            player.sendMessage(Message.translation("server.rpg.race.error.unableToSelect").param("id", raceId));
            return;
        }

        String displayName = raceDef.getDisplayName() != null && !raceDef.getDisplayName().isEmpty()
                ? raceDef.getDisplayName()
                : raceId;

        if (activeRaceId != null && !activeRaceId.isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.race.success.switched").param("name", displayName));
        } else {
            player.sendMessage(Message.translation("server.rpg.race.success.selected").param("name", displayName));
        }
    }
}
