package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class abilities [className] - Show abilities for a class (defaults to primary
 * learned class)
 */
public class ClassAbilitiesCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final ClassAbilitySystem abilitySystem;
    private final RequiredArg<String> classNameArg;

    public ClassAbilitiesCommand() {
        super("abilities", "View class abilities");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.classNameArg = null;
        this.addUsageVariant(new ClassAbilitiesCommand("View class abilities"));
    }

    private ClassAbilitiesCommand(String description) {
        super(description);
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.classNameArg = this.withRequiredArg("className", "The class to view", ArgTypes.STRING);
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
        if (this.classNameArg != null) {
            String requestedClassId = this.classNameArg.get(ctx);
            classId = classSystem.resolveClassId(requestedClassId);
            if (classId == null) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.error.notFound").param("classId", requestedClassId));
                return;
            }
        } else {
            String primaryClassId = classComp != null ? classComp.getPrimaryClassId() : null;
            if (primaryClassId == null || primaryClassId.isEmpty()) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.abilities.noLearnedClass"));
                return;
            }
            classId = primaryClassId;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notFound").param("classId", classId));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.class.abilities.header")
                .param("name", classDef.getDisplayName()));

        java.util.List<String> abilityIds = new java.util.ArrayList<>(classDef.getAbilityIds());
        abilityIds.sort(String::compareToIgnoreCase);

        if (abilityIds.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.abilities.none"));
            return;
        }

        for (String abilityId : abilityIds) {
            ClassAbilityDefinition abilityDef = abilitySystem.getAbilityDefinition(abilityId);
            String display = abilityDef != null && abilityDef.getDisplayName() != null && !abilityDef.getDisplayName().isEmpty()
                    ? abilityDef.getDisplayName() + " (" + abilityId + ")"
                    : abilityId;
            player.sendMessage(Message.translation("pixelbays.rpg.class.abilities.entry").param("ability", display));
        }
    }
}
