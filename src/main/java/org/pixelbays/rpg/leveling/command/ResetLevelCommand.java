package org.pixelbays.rpg.leveling.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;

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
 * /resetlevels - Clear all level progression data for the player
 */
public class ResetLevelCommand extends AbstractPlayerCommand {

    public ResetLevelCommand() {
        super("resetlevels", "Resets all level progression data to default");
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        LevelProgressionComponent levelComp = store.getComponent(ref,
                ExamplePlugin.get().getLevelProgressionComponentType());

        if (levelComp == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.level.reset.none"));
            return;
        }

        int systemCount = levelComp.getAllSystems().size();
        levelComp.getAllSystems().clear();

        player.sendMessage(Message.translation("pixelbays.rpg.level.reset.success").param("count", systemCount));
        player.sendMessage(Message.translation("pixelbays.rpg.level.reset.cleared"));
    }
}
