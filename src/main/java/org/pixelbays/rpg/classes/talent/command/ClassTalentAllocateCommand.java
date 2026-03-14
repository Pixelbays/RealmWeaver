package org.pixelbays.rpg.classes.talent.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
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
 * /class talent allocate &lt;classId&gt; &lt;treeId&gt; &lt;nodeId&gt;
 * Spends one skill point to allocate a rank in a talent node.
 */
public class ClassTalentAllocateCommand extends AbstractPlayerCommand {

    private static final String CONFIG_ID = "default";

    private final TalentSystem talentSystem;
    private final RequiredArg<String> classIdArg;
    private final RequiredArg<String> treeIdArg;
    private final RequiredArg<String> nodeIdArg;

    public ClassTalentAllocateCommand() {
        super("allocate", "Spend a skill point in a talent node");
        this.talentSystem = ExamplePlugin.get().getTalentSystem();
        this.classIdArg = this.withRequiredArg("classId", "The class to allocate points for", ArgTypes.STRING);
        this.treeIdArg = this.withRequiredArg("treeId", "The talent tree ID", ArgTypes.STRING);
        this.nodeIdArg = this.withRequiredArg("nodeId", "The talent node ID", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classIdArg.get(ctx);
        String treeId = this.treeIdArg.get(ctx);
        String nodeId = this.nodeIdArg.get(ctx);

        String result = talentSystem.allocateTalentPoint(ref, classId, treeId, nodeId, CONFIG_ID, store);
        if (result == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.allocate.success")
                    .param("classId", classId)
                    .param("treeId", treeId)
                    .param("nodeId", nodeId));
            return;
        }

        player.sendMessage(TalentCommandUtil.translateResult(result));
    }
}
