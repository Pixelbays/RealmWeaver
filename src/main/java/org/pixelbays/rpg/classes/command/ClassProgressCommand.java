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
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class progress [className] - Show class progression (defaults to primary
 * learned class)
 */
public class ClassProgressCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final LevelProgressionSystem levelSystem;
    private final ClassAbilitySystem abilitySystem;
    private final RequiredArg<String> classNameArg;

    public ClassProgressCommand() {
        super("progress", "View class progression");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.classNameArg = null;
        this.addUsageVariant(new ClassProgressCommand("View class progression"));
    }

    private ClassProgressCommand(String description) {
        super(description);
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
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
        ClassAbilityComponent abilityComp = store.getComponent(ref, ExamplePlugin.get().getClassAbilityComponentType());

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
                player.sendMessage(Message.translation("pixelbays.rpg.class.progress.noLearnedClass"));
                return;
            }
            classId = primaryClassId;
        }

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notFound").param("classId", classId));
            return;
        }

        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.error.notLearned")
                    .param("class", classDef.getDisplayName()));
            return;
        }

        boolean isPrimary = classId.equals(classComp.getPrimaryClassId());

        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        String displaySystemId = systemId != null && !systemId.isEmpty() ? systemId : "-";
        int currentLevel = (systemId != null && !systemId.isEmpty()) ? levelSystem.getLevel(ref, systemId) : 0;
        float currentExp = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExperience(ref, systemId) : 0f;
        float expToNext = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExpToNextLevel(ref, systemId) : 0f;
        float expProgress = (systemId != null && !systemId.isEmpty()) ? levelSystem.getExpProgress(ref, systemId) : 0f;

        player.sendMessage(Message.translation("pixelbays.rpg.class.progress.header").param("name", classDef.getDisplayName()));
        player.sendMessage(Message.translation("pixelbays.rpg.class.progress.status")
            .param("status", Message.translation(
                    isPrimary ? "pixelbays.rpg.class.status.primary" : "pixelbays.rpg.class.status.inactive")));
        player.sendMessage(Message.translation("pixelbays.rpg.class.progress.levelSystem").param("systemId", displaySystemId));
        player.sendMessage(Message.translation("pixelbays.rpg.class.progress.currentLevel")
            .param("level", Integer.toString(currentLevel)));
        player.sendMessage(Message.translation("pixelbays.rpg.class.progress.currentXp")
            .param("current", String.format("%.0f", currentExp))
            .param("next", String.format("%.0f", expToNext))
            .param("percent", Integer.toString((int) (expProgress * 100))));

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

            player.sendMessage(Message.translation("pixelbays.rpg.class.progress.abilities")
                    .param("abilities", String.join(", ", displayList)));
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.class.progress.abilitiesNone"));
        }

        // Show stat bonuses
        if (!classDef.getBaseStatModifiers().isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.common.blank"));
            player.sendMessage(Message.translation("pixelbays.rpg.class.progress.baseStatBonuses"));

            for (Map.Entry<String, Float> entry : classDef.getBaseStatModifiers().getAdditiveModifiers().entrySet()) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.progress.baseStatAdd")
                        .param("value", Float.toString(entry.getValue()))
                        .param("stat", entry.getKey()));
            }

            for (Map.Entry<String, Float> entry : classDef.getBaseStatModifiers().getMultiplicativeModifiers()
                    .entrySet()) {
                player.sendMessage(Message.translation("pixelbays.rpg.class.progress.baseStatMult")
                        .param("value", Float.toString(entry.getValue() * 100f))
                        .param("stat", entry.getKey()));
            }
        }
    }
}
