package org.pixelbays.rpg.npc.system;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.AbilityUnlock;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.ClassLevel;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.LevelSystemLevel;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.system.RaceSystem;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class NpcRpgSetupSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, NpcRpgSetupComponent> setupComponentType;
    private final ClassManagementSystem classManagementSystem;
    private final LevelProgressionSystem levelProgressionSystem;
    private final RaceSystem raceSystem;

    public NpcRpgSetupSystem(@Nonnull ComponentType<EntityStore, NpcRpgSetupComponent> setupComponentType) {
        this.setupComponentType = setupComponentType;
        this.classManagementSystem = Realmweavers.get().getClassManagementSystem();
        this.levelProgressionSystem = Realmweavers.get().getLevelProgressionSystem();
        this.raceSystem = Realmweavers.get().getRaceSystem();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NpcRpgSetupComponent setup = archetypeChunk.getComponent(index, setupComponentType);
        if (setup == null || setup.isApplied()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        applyRace(ref, setup, store);
        applyClasses(ref, setup, store);
        applyLevelSystems(ref, setup, store);
        applyClassLevels(ref, setup, store);
        applyAbilities(ref, setup, store);

        setup.setApplied(true);
    }

    private void applyRace(@Nonnull Ref<EntityStore> ref, @Nonnull NpcRpgSetupComponent setup,
            @Nonnull Store<EntityStore> store) {
        String raceId = setup.getRaceId();
        if (raceId != null && !raceId.isEmpty()) {
            boolean applied = raceSystem.setRace(ref, raceId, store);
            if (!applied) {
                RpgLogging.debugDeveloper("[NpcRpgSetup] Failed to set race: %s for %s", raceId, ref);
            }
        }

        List<String> parentRaces = setup.getParentRaces();
        List<String> raceAbilities = setup.getRaceAbilities();
        if (!parentRaces.isEmpty() || !raceAbilities.isEmpty()) {
            RaceComponent raceComponent = store.getComponent(ref, RaceComponent.getComponentType());
            if (raceComponent == null) {
                raceComponent = store.addComponent(ref, RaceComponent.getComponentType());
            }
            if (!parentRaces.isEmpty()) {
                raceComponent.setParentRaces(parentRaces);
            }
            if (!raceAbilities.isEmpty()) {
                raceComponent.setUnlockedRaceAbilities(raceAbilities);
            }
        }
    }

    private void applyClasses(@Nonnull Ref<EntityStore> ref, @Nonnull NpcRpgSetupComponent setup,
            @Nonnull Store<EntityStore> store) {
        List<String> classes = setup.getClasses();
        if (classes.isEmpty()) {
            return;
        }

        for (String classId : classes) {
            if (classId == null || classId.isEmpty()) {
                continue;
            }
            String result = classManagementSystem.learnClass(ref, classId, store);
            if (!result.startsWith("SUCCESS")) {
                RpgLogging.debugDeveloper("[NpcRpgSetup] %s", result);
            }
        }

        String primaryClass = setup.getPrimaryClass();
        if (primaryClass != null && !primaryClass.isEmpty()) {
            String result = classManagementSystem.setActiveClass(ref, primaryClass, store);
            if (!result.startsWith("SUCCESS")) {
                RpgLogging.debugDeveloper("[NpcRpgSetup] %s", result);
            }
        }
    }

    private void applyLevelSystems(@Nonnull Ref<EntityStore> ref, @Nonnull NpcRpgSetupComponent setup,
            @Nonnull Store<EntityStore> store) {
        for (LevelSystemLevel entry : setup.getLevelSystems()) {
            String systemId = entry.getSystemId();
            if (systemId == null || systemId.isEmpty()) {
                continue;
            }
            levelProgressionSystem.setLevel(ref, systemId, entry.getLevel(), store, null);
        }
    }

    private void applyClassLevels(@Nonnull Ref<EntityStore> ref, @Nonnull NpcRpgSetupComponent setup,
            @Nonnull Store<EntityStore> store) {
        if (setup.getClassLevels().isEmpty()) {
            return;
        }

        for (ClassLevel entry : setup.getClassLevels()) {
            String classId = entry.getClassId();
            if (classId == null || classId.isEmpty()) {
                continue;
            }
            ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
            if (classDef == null) {
                RpgLogging.debugDeveloper("[NpcRpgSetup] Unknown class for level setup: %s", classId);
                continue;
            }
            String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
            if (systemId == null || systemId.isEmpty()) {
                continue;
            }
            levelProgressionSystem.setLevel(ref, systemId, entry.getLevel(), store, null);
        }
    }

    private void applyAbilities(@Nonnull Ref<EntityStore> ref, @Nonnull NpcRpgSetupComponent setup,
            @Nonnull Store<EntityStore> store) {
        if (setup.getAbilities().isEmpty()) {
            return;
        }

        ClassAbilityComponent abilityComponent = store.getComponent(ref, ClassAbilityComponent.getComponentType());
        if (abilityComponent == null) {
            abilityComponent = store.addComponent(ref, ClassAbilityComponent.getComponentType());
        }

        String fallbackClass = resolveFallbackClass(setup, store, ref);
        for (AbilityUnlock entry : setup.getAbilities()) {
            String abilityId = entry.getAbilityId();
            if (abilityId == null || abilityId.isEmpty()) {
                continue;
            }
            String classId = entry.getClassId();
            if (classId == null || classId.isEmpty()) {
                classId = fallbackClass;
            }
            abilityComponent.unlockAbility(abilityId, classId == null ? "" : classId, entry.getRank());
        }
    }

    @Nullable
    private String resolveFallbackClass(@Nonnull NpcRpgSetupComponent setup, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref) {
        String primaryClass = setup.getPrimaryClass();
        if (primaryClass != null && !primaryClass.isEmpty()) {
            return primaryClass;
        }
        if (!setup.getClasses().isEmpty()) {
            return setup.getClasses().get(0);
        }
        ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
        if (classComponent == null) {
            return null;
        }
        return classComponent.getPrimaryClassId();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return setupComponentType;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }
}
