package org.pixelbays.rpg.race.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.expansion.ExpansionManager;
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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /race list - Lists all available races
 */
@SuppressWarnings("null")
public class RaceListCommand extends AbstractPlayerCommand {

    private final RaceManagementSystem raceManagementSystem;
    private final RaceSystem raceSystem;
    private final ExpansionManager expansionManager;

    public RaceListCommand() {
        super("list", "List all available races");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.raceManagementSystem = Realmweavers.get().getRaceManagementSystem();
        this.raceSystem = Realmweavers.get().getRaceSystem();
        this.expansionManager = Realmweavers.get().getExpansionManager();
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
                boolean hasExpansionAccess = expansionManager.hasAccess(playerRef, raceDef.getRequiredExpansionIds());
                Message status = Message.translation(isActive
                    ? "pixelbays.rpg.race.status.selected"
                    : (!raceDef.isEnabled()
                        ? "pixelbays.rpg.race.status.disabled"
                        : (hasExpansionAccess
                            ? "pixelbays.rpg.race.status.available"
                            : "pixelbays.rpg.race.status.expansionLocked")));

            String displayName = raceDef.getDisplayName() != null && !raceDef.getDisplayName().isEmpty()
                    ? raceDef.getDisplayName()
                    : raceId;

                player.sendMessage(Message.translation("pixelbays.rpg.race.list.entry")
                    .param("status", status)
                    .param("name", displayName)
                    .param("id", raceId));
                if (!hasExpansionAccess) {
                player.sendMessage(Message.translation("pixelbays.rpg.race.list.expansionRequired")
                    .param("expansions", expansionManager.describeRequirements(raceDef.getRequiredExpansionIds())));
                }
        }
    }
}
