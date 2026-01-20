package org.pixelbays.rpg.commands.classes;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.component.ClassComponent;
import org.pixelbays.rpg.config.ClassAbilityDefinition;
import org.pixelbays.rpg.config.ClassDefinition;
import org.pixelbays.rpg.system.ClassAbilitySystem;
import org.pixelbays.rpg.system.ClassManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class abilities [className] - Show abilities for a class (defaults to active
 * class)
 */
public class ClassAbilitiesCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final ClassAbilitySystem abilitySystem;
    private final OptionalArg<String> classNameArg;

    public ClassAbilitiesCommand() {
        super("abilities", "View class abilities");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.classNameArg = this.withOptionalArg("className", "The class to view (defaults to active)",
                ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());

        String classId;
        if (this.classNameArg.provided(ctx)) {
            classId = this.classNameArg.get(ctx);
        } else {
            if (classComp == null || classComp.getActiveClassId() == null || classComp.getActiveClassId().isEmpty()) {
                player.sendMessage(Message.raw("No active class. Specify class name: /class abilities <className>"));
                return;
            }
            classId = classComp.getActiveClassId();
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.raw("Class not found: " + classId));
            return;
        }

        player.sendMessage(Message.raw("=== " + classDef.getDisplayName() + " Abilities ==="));

        java.util.List<String> abilityIds = new java.util.ArrayList<>(classDef.getAbilityIds());
        abilityIds.sort(String::compareToIgnoreCase);

        if (abilityIds.isEmpty()) {
            player.sendMessage(Message.raw("No abilities for this class"));
            return;
        }

        for (String abilityId : abilityIds) {
            ClassAbilityDefinition abilityDef = abilitySystem.getAbilityDefinition(abilityId);
            String display = abilityDef != null && abilityDef.getDisplayName() != null && !abilityDef.getDisplayName().isEmpty()
                    ? abilityDef.getDisplayName() + " (" + abilityId + ")"
                    : abilityId;
            player.sendMessage(Message.raw("- " + display));
        }
    }
}
