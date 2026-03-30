package org.pixelbays.rpg.classes.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.expansion.ExpansionManager;

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
 * /class list - Lists all available classes with their status
 */
@SuppressWarnings("null")
public class ClassListCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final ExpansionManager expansionManager;

    public ClassListCommand() {
        super("list", "List all available classes");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.classSystem = Realmweavers.get().getClassManagementSystem();
        this.expansionManager = Realmweavers.get().getExpansionManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, 
                          @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, 
                          @Nonnull PlayerRef playerRef, 
                          @Nonnull World world) {
        
        Player player = store.getComponent(ref, Player.getComponentType());
        ClassComponent classComp = store.getComponent(ref, Realmweavers.get().getClassComponentType());
        
        player.sendMessage(Message.translation("pixelbays.rpg.class.list.header"));
        
        Map<String, ClassDefinition> allClasses = classSystem.getAllClassDefinitions();
        
        for (Map.Entry<String, ClassDefinition> entry : allClasses.entrySet()) {
            ClassDefinition classDef = entry.getValue();
            
            if (!classDef.isVisible()) continue;
            
            String classId = classDef.getId();
            boolean learned = classComp != null && classComp.hasLearnedClass(classId);
            boolean isPrimary = classComp != null && classId.equals(classComp.getPrimaryClassId());
                boolean hasExpansionAccess = expansionManager.hasAccess(playerRef, classDef.getRequiredExpansionIds());

                if (!hasExpansionAccess) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.entry.expansionLocked")
                    .param("name", classDef.getDisplayName())
                    .param("id", classId));
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.expansionRequired")
                    .param("expansions", expansionManager.describeRequirements(classDef.getRequiredExpansionIds())));
                continue;
                }

            if (learned && isPrimary) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.entry.primary")
                        .param("name", classDef.getDisplayName())
                        .param("id", classId));
            } else if (learned) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.entry.learned")
                        .param("name", classDef.getDisplayName())
                        .param("id", classId));
            } else {
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.entry.locked")
                        .param("name", classDef.getDisplayName())
                        .param("id", classId));
            }
            
            if (learned && classComp != null) {
                ClassComponent.ClassData classData = classComp.getClassData(classId);
                if (classData != null) {
                    player.sendMessage(Message.translation("pixelbays.rpg.class.list.learnedAt")
                            .param("time", new java.util.Date(classData.getLearnedTime()).toString()));
                }
            } else {
                player.sendMessage(Message.translation("pixelbays.rpg.class.list.description")
                        .param("description", classDef.getDescription()));
            }
        }
    }
}
