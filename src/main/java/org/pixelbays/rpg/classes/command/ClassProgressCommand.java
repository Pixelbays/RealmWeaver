package org.pixelbays.rpg.classes.command;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

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
 * /class progress [className] - Show class progression (defaults to primary
 * learned class)
 */
public class ClassProgressCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final LevelProgressionSystem levelSystem;
    private final ClassAbilitySystem abilitySystem;
    private final OptionalArg<String> classNameArg;

    public ClassProgressCommand() {
        super("progress", "View class progression");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.classNameArg = this.withOptionalArg("className", "The class to view (defaults to primary)",
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
        ClassAbilityComponent abilityComp = store.getComponent(ref, ExamplePlugin.get().getClassAbilityComponentType());

        String classId;
        if (this.classNameArg.provided(ctx)) {
            classId = this.classNameArg.get(ctx);
        } else {
            String primaryClassId = classComp != null ? classComp.getPrimaryClassId() : null;
            if (primaryClassId == null || primaryClassId.isEmpty()) {
                player.sendMessage(Message.raw("No learned class. Specify class name: /class progress <className>"));
                return;
            }
            classId = primaryClassId;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.raw("Class not found: " + classId));
            return;
        }

        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.raw("You have not learned " + classDef.getDisplayName()));
            return;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        boolean isPrimary = classId.equals(classComp.getPrimaryClassId());

        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        int currentLevel = (systemId != null && !systemId.isEmpty()) ? levelSystem.getLevel(ref, systemId) : 0;
        float currentExp = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExperience(ref, systemId) : 0f;
        float expToNext = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExpToNextLevel(ref, systemId) : 0f;
        float expProgress = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExpProgress(ref, systemId) : 0f;

        player.sendMessage(Message.raw("=== " + classDef.getDisplayName() + " Progress ==="));
        player.sendMessage(Message.raw("Status: " + (isPrimary ? "Primary" : "Inactive")));
        player.sendMessage(Message.raw("Level System: " + systemId));
        player.sendMessage(Message.raw("Current Level: " + currentLevel));
        player.sendMessage(Message.raw("Current XP: " + String.format("%.0f", currentExp)
            + " / " + String.format("%.0f", expToNext)
            + " (" + (int) (expProgress * 100) + "%)"));

        java.util.List<String> abilityIds = new java.util.ArrayList<>(classDef.getAbilityIds());
        RpgLogging.debugDeveloper("Ability IDs: %s", abilityIds);
        abilityIds.sort(String::compareToIgnoreCase);

        if (!abilityIds.isEmpty()) {
            java.util.List<String> displayList = new java.util.ArrayList<>();
            for (String abilityId : abilityIds) {
                ClassAbilityDefinition abilityDef = abilitySystem.getAbilityDefinition(abilityId);
                String displayName = abilityDef != null && abilityDef.getDisplayName() != null && !abilityDef.getDisplayName().isEmpty()
                        ? abilityDef.getDisplayName()
                        : abilityId;
                boolean unlocked = abilityComp != null && abilityComp.hasAbility(abilityId);
                displayList.add(displayName + " (" + abilityId + ")" + (unlocked ? "" : " [LOCKED]"));
            }

            player.sendMessage(Message.raw("Abilities: " + String.join(", ", displayList)));
        } else {
            player.sendMessage(Message.raw("Abilities: none"));
        }

        // Show stat bonuses
        if (!classDef.getBaseStatModifiers().isEmpty()) {
            player.sendMessage(Message.raw(""));
            player.sendMessage(Message.raw("Base Stat Bonuses:"));

            for (Map.Entry<String, Float> entry : classDef.getBaseStatModifiers().getAdditiveModifiers().entrySet()) {
                player.sendMessage(Message.raw("  +" + entry.getValue() + " " + entry.getKey()));
            }

            for (Map.Entry<String, Float> entry : classDef.getBaseStatModifiers().getMultiplicativeModifiers()
                    .entrySet()) {
                player.sendMessage(Message.raw("  +" + (entry.getValue() * 100) + "% " + entry.getKey()));
            }
        }
    }
}
