package org.pixelbays.rpg.npc.corecomponents;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.AbilityUnlock;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.ClassLevel;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.LevelSystemLevel;
import org.pixelbays.rpg.npc.corecomponents.builders.BuilderActionRpgSetup;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

public class ActionRpgSetup extends ActionBase {

    private final String raceId;
    private final List<String> parentRaces;
    private final List<String> raceAbilities;
    private final List<String> classes;
    private final String primaryClass;
    private final List<ClassLevel> classLevels;
    private final List<LevelSystemLevel> levelSystems;
    private final List<AbilityUnlock> abilities;

    public ActionRpgSetup(@Nonnull BuilderActionRpgSetup builder, @Nonnull BuilderSupport support) {
        super(builder);
        this.raceId = builder.getRaceId(support);
        this.parentRaces = builder.getParentRaces(support);
        this.raceAbilities = builder.getRaceAbilities(support);
        this.classes = builder.getClasses(support);
        this.primaryClass = builder.getPrimaryClass(support);
        this.classLevels = builder.getClassLevels();
        this.levelSystems = builder.getLevelSystems();
        this.abilities = builder.getAbilities();
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt,
            @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);

        NpcRpgSetupComponent setup = store.getComponent(ref, NpcRpgSetupComponent.getComponentType());
        if (setup == null) {
            setup = store.addComponent(ref, NpcRpgSetupComponent.getComponentType());
        }

        setup.setRaceId(this.raceId);
        setup.setParentRaces(this.parentRaces);
        setup.setRaceAbilities(this.raceAbilities);
        setup.setClasses(this.classes);
        setup.setPrimaryClass(this.primaryClass);
        setup.setClassLevels(copyClassLevels(this.classLevels));
        setup.setLevelSystems(copyLevelSystems(this.levelSystems));
        setup.setAbilities(copyAbilities(this.abilities));
        setup.setApplied(false);

        return true;
    }

    private List<ClassLevel> copyClassLevels(@Nonnull List<ClassLevel> source) {
        List<ClassLevel> copy = new ArrayList<>(source.size());
        for (ClassLevel entry : source) {
            copy.add(entry.copy());
        }
        return copy;
    }

    private List<LevelSystemLevel> copyLevelSystems(@Nonnull List<LevelSystemLevel> source) {
        List<LevelSystemLevel> copy = new ArrayList<>(source.size());
        for (LevelSystemLevel entry : source) {
            copy.add(entry.copy());
        }
        return copy;
    }

    private List<AbilityUnlock> copyAbilities(@Nonnull List<AbilityUnlock> source) {
        List<AbilityUnlock> copy = new ArrayList<>(source.size());
        for (AbilityUnlock entry : source) {
            copy.add(entry.copy());
        }
        return copy;
    }
}
