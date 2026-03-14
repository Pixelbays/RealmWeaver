package org.pixelbays.rpg.race.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;
import org.pixelbays.rpg.race.system.RaceSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /race list - Lists all available races
 */
@SuppressWarnings("null")
public class RaceListCommand extends AbstractPlayerCommand {

    private final RaceManagementSystem raceManagementSystem;
    private final RaceSystem raceSystem;

    public RaceListCommand() {
        super("list", "List all available races");
        this.raceManagementSystem = ExamplePlugin.get().getRaceManagementSystem();
        this.raceSystem = ExamplePlugin.get().getRaceSystem();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String activeRaceId = raceSystem.getRaceId(ref);

        player.sendMessage(Message.translation("pixelbays.rpg.race.list.header"));

        Map<String, RaceDefinition> allRaces = raceManagementSystem.getRaceDefinitions();
        if (allRaces.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.race.list.none"));
            return;
        }

        for (Map.Entry<String, RaceDefinition> entry : allRaces.entrySet()) {
            RaceDefinition raceDef = entry.getValue();
            if (raceDef == null || !raceDef.isVisible()) {
                continue;
            }

            String raceId = raceDef.getRaceId();
            boolean isActive = raceId != null && raceId.equals(activeRaceId);
                Message status = Message.translation(isActive
                    ? "pixelbays.rpg.race.status.selected"
                    : (raceDef.isEnabled() ? "pixelbays.rpg.race.status.available" : "pixelbays.rpg.race.status.disabled"));

            String displayName = raceDef.getDisplayName() != null && !raceDef.getDisplayName().isEmpty()
                    ? raceDef.getDisplayName()
                    : raceId;

                player.sendMessage(Message.translation("pixelbays.rpg.race.list.entry")
                    .param("status", status)
                    .param("name", displayName)
                    .param("id", raceId));
        }
    }
}
