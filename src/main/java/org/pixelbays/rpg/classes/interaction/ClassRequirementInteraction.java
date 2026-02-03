package org.pixelbays.rpg.classes.interaction;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Interaction that only succeeds if the entity has learned one of the required classes.
 */
@SuppressWarnings({"FieldHidesSuperclassField", "null"})
public class ClassRequirementInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<ClassRequirementInteraction> CODEC = BuilderCodec.builder(
            ClassRequirementInteraction.class, ClassRequirementInteraction::new, SimpleInstantInteraction.CODEC
        )
        .documentation("Requires the entity to have learned at least one of the listed class IDs.")
        .appendInherited(
            new KeyedCodec<>("RequiredClassIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
            (o, i) -> o.requiredClassIds = i,
            o -> o.requiredClassIds,
            (o, p) -> o.requiredClassIds = p.requiredClassIds
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RequiredLevel", Codec.INTEGER),
            (o, i) -> o.requiredLevel = i,
            o -> o.requiredLevel,
            (o, p) -> o.requiredLevel = p.requiredLevel
        )
        .add()
        .build();

    private String[] requiredClassIds = new String[0];
    private int requiredLevel = 1;

    public ClassRequirementInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            RpgLogging.debugDeveloper("[ClassRequirement] Missing CommandBuffer for entity %s", context.getEntity());
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = context.getEntity();
        ClassComponent classComponent = commandBuffer.getComponent(ref, ClassComponent.getComponentType());
        if (classComponent == null || requiredClassIds == null || requiredClassIds.length == 0) {
            RpgLogging.debugDeveloper(
                "[ClassRequirement] Failed for entity %s (classComponent=%s, requiredClassIds=%s)",
                ref,
                classComponent != null,
                requiredClassIds == null ? "null" : requiredClassIds.length
            );
            context.getState().state = InteractionState.Failed;
            return;
        }

        for (String classId : requiredClassIds) {
            if (classId == null || classId.isEmpty() || !classComponent.hasLearnedClass(classId)) {
                continue;
            }

            if (requiredLevel <= 1) {
                RpgLogging.debugDeveloper("[ClassRequirement] Passed for entity %s with class %s", ref, classId);
                context.getState().state = InteractionState.Finished;
                return;
            }

            ClassDefinition classDefinition = ClassDefinition.getAssetMap().getAsset(classId);
            if (classDefinition == null) {
                continue;
            }

            String levelSystemId = classDefinition.usesCharacterLevel()
                ? "Base_Character_Level"
                : classDefinition.getLevelSystemId();

            if (levelSystemId == null || levelSystemId.isEmpty()) {
                continue;
            }

            LevelProgressionComponent levelComponent = commandBuffer.getComponent(ref, LevelProgressionComponent.getComponentType());
            if (levelComponent == null) {
                continue;
            }

            LevelProgressionComponent.LevelSystemData levelSystemData = levelComponent.getSystem(levelSystemId);
            int currentLevel = levelSystemData != null ? levelSystemData.getCurrentLevel() : 1;
            if (currentLevel >= requiredLevel) {
                RpgLogging.debugDeveloper(
                    "[ClassRequirement] Passed for entity %s with class %s (level %s >= %s)",
                    ref,
                    classId,
                    currentLevel,
                    requiredLevel
                );
                context.getState().state = InteractionState.Finished;
                return;
            }
        }

        RpgLogging.debugDeveloper("[ClassRequirement] Failed for entity %s (no matching class/level)", ref);
        context.getState().state = InteractionState.Failed;
    }
}