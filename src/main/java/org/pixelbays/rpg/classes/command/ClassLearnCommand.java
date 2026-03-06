package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
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
 * /class learn <className> - Learn a new class
 */
public class ClassLearnCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final RequiredArg<String> classNameArg;

    public ClassLearnCommand() {
        super("learn", "Learn a new class");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        @SuppressWarnings("null")
        RequiredArg<String> className = this.withRequiredArg("className", "The class to learn", ArgTypes.STRING);
        this.classNameArg = className;
    }

    @Override
    @SuppressWarnings("null")
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classNameArg.get(ctx);

        String result = classSystem.learnClass(ref, classId, store);
        if (result == null) {
            player.sendMessage(Message.translation("server.rpg.common.unknownError"));
            return;
        }

        player.sendMessage(ClassCommandUtil.managerResultMessage(result));
    }
}
