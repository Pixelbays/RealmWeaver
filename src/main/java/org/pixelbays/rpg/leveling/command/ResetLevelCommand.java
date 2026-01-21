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
 * Command to reset all level progression data for a player
 * Usage: /resetlevels
 * This removes all level systems and resets the player to default state
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
        
        // Get the level progression component
        LevelProgressionComponent levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
        
        if (levelComp == null) {
            player.sendMessage(Message.raw("No level data to reset."));
            return;
        }
        
        // Store count before clearing
        int systemCount = levelComp.getAllSystems().size();
        
        // Clear all level systems
        levelComp.getAllSystems().clear();
        
        player.sendMessage(Message.raw("Successfully reset " + systemCount + " level system(s)"));
        player.sendMessage(Message.raw("All level progression data has been cleared."));
        
        System.out.println("[LevelSystem] Reset all level data for player: " + player.getDisplayName());
    }
}
