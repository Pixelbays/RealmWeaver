package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;

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
 * /class info <className> - Shows detailed information about a class
 */
public class ClassInfoCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final RequiredArg<String> classNameArg;

    public ClassInfoCommand() {
        super("info", "Show detailed class information");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to view", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classNameArg.get(ctx);

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.raw("Class not found: " + classId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        boolean learned = classComp != null && classComp.hasLearnedClass(classId);

        player.sendMessage(Message.raw("=== " + classDef.getDisplayName() + " ==="));
        player.sendMessage(Message.raw(classDef.getDescription()));
        player.sendMessage(Message.raw(""));

        if (learned) {
            if (classComp == null) {
                player.sendMessage(Message.raw("Status: LEARNED"));
                player.sendMessage(Message.raw("Learned: unknown"));
                player.sendMessage(Message.raw("Total XP: unknown"));
                return;
            }
            ClassComponent.ClassData classData = classComp.getClassData(classId);
            boolean isActive = classId.equals(classComp.getActiveClassId());

            player.sendMessage(Message.raw("Status: " + (isActive ? "Active" : "LEARNED")));
            if (classData != null) {
                player.sendMessage(Message.raw("Learned: " + new java.util.Date(classData.getLearnedTime())));
                player.sendMessage(Message.raw("Total XP: " + String.format("%.0f", classData.getTotalExpEarned())));
            } else {
                player.sendMessage(Message.raw("Learned: unknown"));
                player.sendMessage(Message.raw("Total XP: unknown"));
            }
        } else {
            player.sendMessage(Message.raw("Status: LOCKED"));
            player.sendMessage(Message.raw(""));
            player.sendMessage(Message.raw("Requirements:"));


            if (!classDef.getRequiredClasses().isEmpty()) {
                player.sendMessage(Message.raw("  - Classes: " + String.join(", ", classDef.getRequiredClasses())));
            }
        }

        int abilityCount = classDef.getAbilityIds().size();
        if (abilityCount > 0) {
            player.sendMessage(Message.raw("Abilities: " + abilityCount));
        }
    }
}
