package org.pixelbays.rpg.classes.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;

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
 * /class list - Lists all available classes with their status
 */
public class ClassListCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;

    public ClassListCommand() {
        super("list", "List all available classes");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, 
                          @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, 
                          @Nonnull PlayerRef playerRef, 
                          @Nonnull World world) {
        
        Player player = store.getComponent(ref, Player.getComponentType());
        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        
        player.sendMessage(Message.raw("=== Available Classes ==="));
        
        Map<String, ClassDefinition> allClasses = classSystem.getAllClassDefinitions();
        
        for (Map.Entry<String, ClassDefinition> entry : allClasses.entrySet()) {
            ClassDefinition classDef = entry.getValue();
            
            if (!classDef.isVisible()) continue;
            
            String classId = classDef.getId();
            boolean learned = classComp != null && classComp.hasLearnedClass(classId);
            boolean isActive = classComp != null && classId.equals(classComp.getActiveClassId());
            
            String status = learned ? (isActive ? "[Active]" : "[LEARNED]") : "[LOCKED]";
            
            player.sendMessage(Message.raw(status + " " + classDef.getDisplayName() + " (" + classId + ")"));
            
            if (learned && classComp != null) {
                ClassComponent.ClassData classData = classComp.getClassData(classId);
                player.sendMessage(Message.raw("  Learned: " + new java.util.Date(classData.getLearnedTime())));
            } else {
                player.sendMessage(Message.raw("  " + classDef.getDescription()));
            }
        }
    }
}
