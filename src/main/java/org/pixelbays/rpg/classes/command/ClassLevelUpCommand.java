package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

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
 * /class levelup <className> - Grant exactly enough XP to reach next level
 */
public class ClassLevelUpCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final LevelProgressionSystem levelSystem;
    private final RequiredArg<String> classNameArg;

    public ClassLevelUpCommand() {
        super("levelup", "Grant exactly enough XP to reach the next level");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to level up", ArgTypes.STRING);
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

        float expToNext = levelSystem.getExpToNextLevel(ref, systemId);
        if (expToNext <= 0f) {
            player.sendMessage(Message.raw(classDef.getDisplayName() + " is already at max level."));
            return;
        }

        levelSystem.grantExperience(ref, systemId, expToNext, "command:class_levelup", store, world);
        int newLevel = levelSystem.getLevel(ref, systemId);

        player.sendMessage(Message.raw("Leveled up " + classDef.getDisplayName() + " to " + newLevel + "."));
    }
}
