package org.pixelbays.rpg.commands.levels;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.component.LevelProgressionComponent;
import org.pixelbays.rpg.system.LevelProgressionSystem;

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
 * Command to test the leveling system functionality
 * Usage: /testlevel
 * This runs a full automated test of the leveling system
 */
public class TestLevelCommand extends AbstractPlayerCommand {

    private final LevelProgressionSystem levelSystem;

    public TestLevelCommand() {
        super("testlevel", "Tests the RPG leveling system - runs automated test");
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, 
                          @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, 
                          @Nonnull PlayerRef playerRef, 
                          @Nonnull World world) {
        
       /*  Player player = store.getComponent(ref, Player.getComponentType());
        
        player.sendMessage(Message.raw("=== Running Level System Test ==="));
        
        // Test 1: Initialize character level
        player.sendMessage(Message.raw("[1/5] Initializing character_level..."));
        levelSystem.initializeLevelSystem(ref, "character_level");
        player.sendMessage(Message.raw("Initialized"));
        
        // Test 2: Add some exp (should level up once)
        player.sendMessage(Message.raw("[2/5] Granting 150 exp..."));
        levelSystem.grantExperience(ref, "character_level", 150f, "test", store, world);
        
        LevelProgressionComponent levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
        LevelProgressionComponent.LevelSystemData charData = levelComp.getSystem("character_level");
        player.sendMessage(Message.raw("Now level " + charData.getCurrentLevel()));
        
        // Test 3: Add more exp (multiple level-ups to reach level 10+)
        player.sendMessage(Message.raw("[3/5] Granting 2000 exp to reach level 10..."));
        levelSystem.grantExperience(ref, "character_level", 2000f, "test", store, world);
        
        // Refetch component to get updated data
        levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
        charData = levelComp.getSystem("character_level");
        player.sendMessage(Message.raw("Now level " + charData.getCurrentLevel() + " (need 10+ for warrior class)"));
        
        // Test 4: Initialize warrior class (requires level 10)
        player.sendMessage(Message.raw("[4/5] Initializing class_warrior..."));
        levelSystem.initializeLevelSystem(ref, "class_warrior");
        
        // Refetch component to check for warrior system
        levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
        
        // Debug: Check what's in the map
        player.sendMessage(Message.raw("DEBUG: Systems in map: " + levelComp.getAllSystems().keySet()));
        player.sendMessage(Message.raw("DEBUG: hasSystem(class_warrior): " + levelComp.hasSystem("class_warrior")));
        
        LevelProgressionComponent.LevelSystemData warriorData = levelComp.getSystem("class_warrior");
        player.sendMessage(Message.raw("DEBUG: warriorData is null: " + (warriorData == null)));
        
        if (warriorData != null) {
            player.sendMessage(Message.raw("Warrior class initialized at level " + warriorData.getCurrentLevel()));
            levelSystem.grantExperience(ref, "class_warrior", 300f, "test", store, world);
            
            // Refetch to get updated warrior data
            levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
            warriorData = levelComp.getSystem("class_warrior");
            player.sendMessage(Message.raw("Warrior level " + warriorData.getCurrentLevel()));
        } else {
            player.sendMessage(Message.raw("ERROR: warriorData is null but system claims to exist!"));
        }
        
        // Test 5: Show final status (refetch one last time for accuracy)
        player.sendMessage(Message.raw("[5/5] Final status:"));
        levelComp = store.getComponent(ref, ExamplePlugin.get().getLevelProgressionComponentType());
        charData = levelComp.getSystem("character_level");
        warriorData = levelComp.getSystem("class_warrior");
        showSystemStatus(player, "character_level", charData);
        if (warriorData != null) {
            showSystemStatus(player, "class_warrior", warriorData);
        }
        
        // Show all systems
        player.sendMessage(Message.raw("=== All Level Systems ==="));
        for (java.util.Map.Entry<String, LevelProgressionComponent.LevelSystemData> entry : levelComp.getAllSystems().entrySet()) {
            showSystemStatus(player, entry.getKey(), entry.getValue());
        }
        
        player.sendMessage(Message.raw("=== Test Complete ===")); */
    }
    
    private void showSystemStatus(Player player, String systemId, LevelProgressionComponent.LevelSystemData data) {
        player.sendMessage(Message.raw(String.format(
            "%s: Level %d (%.1f/%.1f exp) [Stats: %d, Skills: %d]",
            systemId,
            data.getCurrentLevel(),
            data.getCurrentExp(),
            data.getExpToNextLevel(),
            data.getAvailableStatPoints(),
            data.getAvailableSkillPoints()
        )));
    }
}
