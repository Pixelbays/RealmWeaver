package org.pixelbays.rpg.classes.talent.command;

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
 * /class talent grant <classId> <amount>
 * Debug helper that adds talent points to the class level system.
 */
public class ClassTalentGrantCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final LevelProgressionSystem levelSystem;
    private final RequiredArg<String> classIdArg;
    private final RequiredArg<Integer> amountArg;

    public ClassTalentGrantCommand() {
        super("grant", "Debug: grant talent points to a class");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.classIdArg = this.withRequiredArg("classId", "The class id to grant points to", ArgTypes.STRING);
        this.amountArg = this.withRequiredArg("amount", "The amount of talent points to add", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classIdArg.get(ctx);
        int amount = this.amountArg.get(ctx);

        if (amount <= 0) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.grant.invalidAmount"));
            return;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.unknownClass").param("classId", classId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notLearned").param("class", classId));
            return;
        }

        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.noLevelSystem").param("classId", classId));
            return;
        }

        if (!levelSystem.refundSkillPoints(ref, systemId, amount)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.grant.failed").param("classId", classId));
            return;
        }

        int available = levelSystem.getSkillPoints(ref, systemId);
        player.sendMessage(
                Message.translation("pixelbays.rpg.class.talent.grant.success")
                        .param("amount", Integer.toString(amount))
                        .param("classId", classId)
                        .param("available", Integer.toString(available)));
    }
}