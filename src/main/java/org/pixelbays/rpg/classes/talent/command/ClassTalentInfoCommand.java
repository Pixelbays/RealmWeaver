package org.pixelbays.rpg.classes.talent.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.talent.TalentSystem;

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
 * /class talent info [classId]
 * Prints a summary of talent allocations for a class (defaults to primary class).
 */
public class ClassTalentInfoCommand extends AbstractPlayerCommand {

    private final TalentSystem talentSystem;
    private final RequiredArg<String> classIdArg;

    public ClassTalentInfoCommand() {
        super("info", "View talent tree allocation summary");
        this.talentSystem = ExamplePlugin.get().getTalentSystem();
        this.classIdArg = null;
        this.addUsageVariant(new ClassTalentInfoCommand("View talent tree allocation summary"));
    }

    private ClassTalentInfoCommand(String description) {
        super(description);
        this.talentSystem = ExamplePlugin.get().getTalentSystem();
        this.classIdArg = this.withRequiredArg("classId", "The class to inspect", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        String classId;
        if (this.classIdArg != null) {
            classId = this.classIdArg.get(ctx);
        } else {
            ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
            String primaryClassId = classComp != null ? classComp.getPrimaryClassId() : null;
            if (primaryClassId == null || primaryClassId.isEmpty()) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.talent.noLearnedClass"));
                return;
            }
            classId = primaryClassId;
        }

        ClassDefinition classDef = ExamplePlugin.get().getClassManagementSystem().getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.unknownClass").param("classId", classId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notLearned").param("class", classDef.getDisplayName()));
            return;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        if (classData == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.error.classDataMissing").param("classId", classId));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.class.talent.summary.header")
                .param("class", classDef.getDisplayName()));
        player.sendMessage(Message.translation("pixelbays.rpg.class.talent.summary.points")
                .param("available", talentSystem.getAvailablePoints(ref, classId))
                .param("spent", classData.getSpentTalentPoints()));

        if (classDef.getTalentTrees() == null || classDef.getTalentTrees().isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.summary.none"));
            return;
        }

        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.summary.tree")
                    .param("id", tree.getTreeId())
                    .param("name", tree.getDisplayName())
                    .param("spent", classData.getTreePointsSpent(tree.getTreeId()))
                    .param("max", tree.getMaxPoints()));

            if (tree.getNodes() == null) {
                continue;
            }

            for (ClassDefinition.TalentNode node : tree.getNodes()) {
                int rank = classData.getNodeRank(tree.getTreeId(), node.getNodeId());
                if (rank <= 0 && node.getMaxRank() <= 0) {
                    continue;
                }

                player.sendMessage(Message.translation("pixelbays.rpg.class.talent.summary.node")
                        .param("name", node.getDisplayName())
                        .param("id", node.getNodeId())
                        .param("rank", rank)
                        .param("max", node.getMaxRank()));
            }
        }
    }
}
