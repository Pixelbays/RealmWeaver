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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class switch <className> - Prioritize a learned class
 */
public class ClassSwitchCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final RequiredArg<String> classNameArg;

    public ClassSwitchCommand() {
        super("switch", "Prioritize your learned class");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to switch to", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, 
                          @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, 
                          @Nonnull PlayerRef playerRef, 
                          @Nonnull World world) {
        
        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classNameArg.get(ctx);
        
        String result = classSystem.setActiveClass(ref, classId, store);
        player.sendMessage(ClassCommandUtil.managerResultMessage(result));
    }
}
