package org.pixelbays.rpg.classes.talent.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.classes.talent.ui.TalentTreePage;

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
 * /class talent ui [classId]
 *
 * <p>Opens the interactive talent tree page for the player's primary learned class
 * (or an explicitly specified class id).</p>
 */
public class ClassTalentUiCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final RequiredArg<String> classIdArg;

    public ClassTalentUiCommand() {
        super("ui", "Open the talent tree UI");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.classIdArg = null;
        this.addUsageVariant(new ClassTalentUiCommand("Open the talent tree UI"));
    }

    private ClassTalentUiCommand(String description) {
        super(description);
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.classIdArg = this.withRequiredArg("classId", "Class to view", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        // Resolve class id
        String classId;
        if (classIdArg != null) {
            String requestedClassId = classIdArg.get(ctx);
            classId = classSystem.resolveClassId(requestedClassId);
            if (classId == null) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.error.unknownClass").param("classId", requestedClassId));
                return;
            }
        } else {
            ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
            classId = classComp != null ? classComp.getPrimaryClassId() : null;
        }

        if (classId == null || classId.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.noLearnedClass"));
            return;
        }

        // Validate the player has learned this class
        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.noLearnedClass"));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new TalentTreePage(playerRef, classId));
        player.sendMessage(Message.translation("pixelbays.rpg.class.talent.ui.opened").param("classId", classId));
    }
}
