package org.pixelbays.rpg.commands.classes;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.component.ClassComponent;
import org.pixelbays.rpg.config.ClassDefinition;
import org.pixelbays.rpg.system.ClassManagementSystem;
import org.pixelbays.rpg.system.LevelProgressionSystem;

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
 * /class setlevel <className> <level> - Set a class level directly
 */
public class ClassSetLevelCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final LevelProgressionSystem levelSystem;
    private final RequiredArg<String> classNameArg;
    private final RequiredArg<Integer> levelArg;

    public ClassSetLevelCommand() {
        super("setlevel", "Set a class level directly");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to set", ArgTypes.STRING);
        this.levelArg = this.withRequiredArg("level", "Target class level", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classNameArg.get(ctx);
        int targetLevel = this.levelArg.get(ctx);

        if (targetLevel < 1) {
            player.sendMessage(Message.raw("Level must be >= 1"));
            return;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.raw("Class not found: " + classId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.raw("You have not learned " + classDef.getDisplayName()));
            return;
        }

        String systemId = classDef.usesCharacterLevel() ? "character_level" : classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            player.sendMessage(Message.raw("Class has no level system configured: " + classId));
            return;
        }

        if (!levelSystem.hasLevelSystem(ref, systemId)) {
            levelSystem.initializeLevelSystem(ref, systemId);
        }

        int currentLevel = levelSystem.getLevel(ref, systemId);
        if (targetLevel > currentLevel) {
            levelSystem.addLevels(ref, systemId, targetLevel - currentLevel, store, world);
        } else {
            levelSystem.setLevel(ref, systemId, targetLevel, store, world);
        }

        player.sendMessage(Message.raw("Set " + classDef.getDisplayName() + " level to " + targetLevel + "."));
    }
}
