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

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        boolean learned = classComp != null && classComp.hasLearnedClass(classId);

        player.sendMessage(Message.translation("pixelbays.rpg.class.info.header").param("name", classDef.getDisplayName()));
        player.sendMessage(Message.translation("pixelbays.rpg.class.info.description").param("description", classDef.getDescription()));
        player.sendMessage(Message.translation("pixelbays.rpg.class.common.blank"));

        if (learned) {
            if (classComp == null) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.info.status")
                        .param("status", Message.translation("pixelbays.rpg.class.status.learned")));
                player.sendMessage(Message.translation("pixelbays.rpg.class.info.learnedUnknown"));
                return;
            }
            ClassComponent.ClassData classData = classComp.getClassData(classId);
            boolean isPrimary = classId.equals(classComp.getPrimaryClassId());

            player.sendMessage(Message.translation("pixelbays.rpg.class.info.status")
                    .param("status", Message.translation(
                            isPrimary ? "pixelbays.rpg.class.status.primary" : "pixelbays.rpg.class.status.learned")));
            if (classData != null) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.info.learnedAt")
                        .param("time", new java.util.Date(classData.getLearnedTime()).toString()));
            } else {
                player.sendMessage(Message.translation("pixelbays.rpg.class.info.learnedUnknown"));
            }
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.class.info.status")
                    .param("status", Message.translation("pixelbays.rpg.class.status.locked")));
            player.sendMessage(Message.translation("pixelbays.rpg.class.common.blank"));
            player.sendMessage(Message.translation("pixelbays.rpg.class.info.requirementsHeader"));


            if (!classDef.getRequiredClasses().isEmpty()) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.info.requirementsClasses")
                        .param("classes", String.join(", ", classDef.getRequiredClasses())));
            }
        }

        int abilityCount = classDef.getAbilityIds().size();
        if (abilityCount > 0) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.info.abilitiesCount")
                    .param("count", Integer.toString(abilityCount)));
        }
    }
}
