package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;
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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
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
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.classSystem = Realmweavers.get().getClassManagementSystem();
        this.levelSystem = Realmweavers.get().getLevelProgressionSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to level up", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String requestedClassId = this.classNameArg.get(ctx);
        String classId = classSystem.resolveClassId(requestedClassId);
        if (classId == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notFound").param("classId", requestedClassId));
            return;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notFound").param("classId", requestedClassId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, Realmweavers.get().getClassComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notLearned")
                    .param("class", classDef.getDisplayName()));
            return;
        }

        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.noLevelSystem")
                    .param("classId", classId));
            return;
        }

        if (!levelSystem.hasLevelSystem(ref, systemId)) {
            levelSystem.initializeLevelSystem(ref, systemId);
        }

        float expToNext = levelSystem.getExpToNextLevel(ref, systemId);
        if (expToNext <= 0f) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.level.max")
                    .param("class", classDef.getDisplayName()));
            return;
        }

        long expToGrant = (long) Math.ceil(expToNext);
        GiveXPEvent.dispatch(ref, expToGrant, systemId);
        int newLevel = levelSystem.getLevel(ref, systemId);

        player.sendMessage(Message.translation("pixelbays.rpg.class.level.up")
            .param("class", classDef.getDisplayName())
            .param("level", Integer.toString(newLevel)));
    }
}
