package org.pixelbays.rpg.npc.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone"})
public class NpcRpgSetupComponent implements Component<EntityStore>, Cloneable {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.STRING, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    private static final FunctionCodec<ClassLevel[], List<ClassLevel>> CLASS_LEVEL_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(ClassLevel.CODEC, ClassLevel[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new ClassLevel[0] : list.toArray(ClassLevel[]::new));

    private static final FunctionCodec<LevelSystemLevel[], List<LevelSystemLevel>> LEVEL_SYSTEM_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(LevelSystemLevel.CODEC, LevelSystemLevel[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new LevelSystemLevel[0] : list.toArray(LevelSystemLevel[]::new));

    private static final FunctionCodec<AbilityUnlock[], List<AbilityUnlock>> ABILITY_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(AbilityUnlock.CODEC, AbilityUnlock[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new AbilityUnlock[0] : list.toArray(AbilityUnlock[]::new));

    public static final BuilderCodec<NpcRpgSetupComponent> CODEC = BuilderCodec
            .builder(NpcRpgSetupComponent.class, NpcRpgSetupComponent::new)
            .append(new KeyedCodec<>("RaceId", Codec.STRING),
                    (component, value) -> component.raceId = value,
                    component -> component.raceId)
            .add()
            .append(new KeyedCodec<>("ParentRaces", STRING_LIST_CODEC, false, true),
                    (component, value) -> component.parentRaces = value,
                    component -> component.parentRaces)
            .add()
            .append(new KeyedCodec<>("RaceAbilities", STRING_LIST_CODEC, false, true),
                    (component, value) -> component.raceAbilities = value,
                    component -> component.raceAbilities)
            .add()
            .append(new KeyedCodec<>("Classes", STRING_LIST_CODEC, false, true),
                    (component, value) -> component.classes = value,
                    component -> component.classes)
            .add()
            .append(new KeyedCodec<>("PrimaryClass", Codec.STRING),
                    (component, value) -> component.primaryClass = value,
                    component -> component.primaryClass)
            .add()
            .append(new KeyedCodec<>("ClassLevels", CLASS_LEVEL_LIST_CODEC, false, true),
                    (component, value) -> component.classLevels = value,
                    component -> component.classLevels)
            .add()
            .append(new KeyedCodec<>("LevelSystems", LEVEL_SYSTEM_LIST_CODEC, false, true),
                    (component, value) -> component.levelSystems = value,
                    component -> component.levelSystems)
            .add()
            .append(new KeyedCodec<>("Abilities", ABILITY_LIST_CODEC, false, true),
                    (component, value) -> component.abilities = value,
                    component -> component.abilities)
            .add()
            .append(new KeyedCodec<>("Applied", Codec.BOOLEAN),
                    (component, value) -> component.applied = value,
                    component -> component.applied)
            .add()
            .build();

    private String raceId;
    private List<String> parentRaces;
    private List<String> raceAbilities;
    private List<String> classes;
    private String primaryClass;
    private List<ClassLevel> classLevels;
    private List<LevelSystemLevel> levelSystems;
    private List<AbilityUnlock> abilities;
    private boolean applied;

    public NpcRpgSetupComponent() {
        this.raceId = "";
        this.parentRaces = new ArrayList<>();
        this.raceAbilities = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.primaryClass = "";
        this.classLevels = new ArrayList<>();
        this.levelSystems = new ArrayList<>();
        this.abilities = new ArrayList<>();
        this.applied = false;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(@Nullable String raceId) {
        this.raceId = raceId == null ? "" : raceId;
    }

    public List<String> getParentRaces() {
        return parentRaces;
    }

    public void setParentRaces(@Nullable List<String> parentRaces) {
        this.parentRaces = parentRaces == null ? new ArrayList<>() : new ArrayList<>(parentRaces);
    }

    public List<String> getRaceAbilities() {
        return raceAbilities;
    }

    public void setRaceAbilities(@Nullable List<String> raceAbilities) {
        this.raceAbilities = raceAbilities == null ? new ArrayList<>() : new ArrayList<>(raceAbilities);
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(@Nullable List<String> classes) {
        this.classes = classes == null ? new ArrayList<>() : new ArrayList<>(classes);
    }

    public String getPrimaryClass() {
        return primaryClass;
    }

    public void setPrimaryClass(@Nullable String primaryClass) {
        this.primaryClass = primaryClass == null ? "" : primaryClass;
    }

    public List<ClassLevel> getClassLevels() {
        return classLevels;
    }

    public void setClassLevels(@Nullable List<ClassLevel> classLevels) {
        this.classLevels = classLevels == null ? new ArrayList<>() : new ArrayList<>(classLevels);
    }

    public List<LevelSystemLevel> getLevelSystems() {
        return levelSystems;
    }

    public void setLevelSystems(@Nullable List<LevelSystemLevel> levelSystems) {
        this.levelSystems = levelSystems == null ? new ArrayList<>() : new ArrayList<>(levelSystems);
    }

    public List<AbilityUnlock> getAbilities() {
        return abilities;
    }

    public void setAbilities(@Nullable List<AbilityUnlock> abilities) {
        this.abilities = abilities == null ? new ArrayList<>() : new ArrayList<>(abilities);
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public static ComponentType<EntityStore, NpcRpgSetupComponent> getComponentType() {
        return Realmweavers.get().getNpcRpgSetupComponentType();
    }

    @Nonnull
    @Override
    @SuppressWarnings({"all", "clone", "CloneDoesntDeclareCloneNotSupportedException"})
    public Component<EntityStore> clone() {
        NpcRpgSetupComponent cloned = new NpcRpgSetupComponent();
        cloned.raceId = this.raceId;
        cloned.parentRaces = new ArrayList<>(this.parentRaces);
        cloned.raceAbilities = new ArrayList<>(this.raceAbilities);
        cloned.classes = new ArrayList<>(this.classes);
        cloned.primaryClass = this.primaryClass;
        cloned.classLevels = new ArrayList<>();
        for (ClassLevel entry : this.classLevels) {
            cloned.classLevels.add(entry.copy());
        }
        cloned.levelSystems = new ArrayList<>();
        for (LevelSystemLevel entry : this.levelSystems) {
            cloned.levelSystems.add(entry.copy());
        }
        cloned.abilities = new ArrayList<>();
        for (AbilityUnlock entry : this.abilities) {
            cloned.abilities.add(entry.copy());
        }
        cloned.applied = this.applied;
        return cloned;
    }

    public static class ClassLevel {
        public static final BuilderCodec<ClassLevel> CODEC = BuilderCodec
                .builder(ClassLevel.class, ClassLevel::new)
                .append(new KeyedCodec<>("ClassId", Codec.STRING),
                        (entry, value) -> entry.classId = value,
                        entry -> entry.classId)
                .add()
                .append(new KeyedCodec<>("Level", Codec.INTEGER),
                        (entry, value) -> entry.level = value,
                        entry -> entry.level)
                .add()
                .build();

        private String classId;
        private int level;

        public ClassLevel() {
            this.classId = "";
            this.level = 1;
        }

        public ClassLevel(String classId, int level) {
            this.classId = classId == null ? "" : classId;
            this.level = Math.max(1, level);
        }

        public String getClassId() {
            return classId;
        }

        public int getLevel() {
            return level;
        }

        public ClassLevel copy() {
            return new ClassLevel(this.classId, this.level);
        }
    }

    public static class LevelSystemLevel {
        public static final BuilderCodec<LevelSystemLevel> CODEC = BuilderCodec
                .builder(LevelSystemLevel.class, LevelSystemLevel::new)
                .append(new KeyedCodec<>("SystemId", Codec.STRING),
                        (entry, value) -> entry.systemId = value,
                        entry -> entry.systemId)
                .add()
                .append(new KeyedCodec<>("Level", Codec.INTEGER),
                        (entry, value) -> entry.level = value,
                        entry -> entry.level)
                .add()
                .build();

        private String systemId;
        private int level;

        public LevelSystemLevel() {
            this.systemId = "";
            this.level = 1;
        }

        public LevelSystemLevel(String systemId, int level) {
            this.systemId = systemId == null ? "" : systemId;
            this.level = Math.max(1, level);
        }

        public String getSystemId() {
            return systemId;
        }

        public int getLevel() {
            return level;
        }

        public LevelSystemLevel copy() {
            return new LevelSystemLevel(this.systemId, this.level);
        }
    }

    public static class AbilityUnlock {
        public static final BuilderCodec<AbilityUnlock> CODEC = BuilderCodec
                .builder(AbilityUnlock.class, AbilityUnlock::new)
                .append(new KeyedCodec<>("AbilityId", Codec.STRING),
                        (entry, value) -> entry.abilityId = value,
                        entry -> entry.abilityId)
                .add()
                .append(new KeyedCodec<>("ClassId", Codec.STRING),
                        (entry, value) -> entry.classId = value,
                        entry -> entry.classId)
                .add()
                .append(new KeyedCodec<>("Rank", Codec.INTEGER),
                        (entry, value) -> entry.rank = value,
                        entry -> entry.rank)
                .add()
                .build();

        private String abilityId;
        private String classId;
        private int rank;

        public AbilityUnlock() {
            this.abilityId = "";
            this.classId = "";
            this.rank = 1;
        }

        public AbilityUnlock(String abilityId, @Nullable String classId, int rank) {
            this.abilityId = abilityId == null ? "" : abilityId;
            this.classId = classId == null ? "" : classId;
            this.rank = Math.max(1, rank);
        }

        public String getAbilityId() {
            return abilityId;
        }

        public String getClassId() {
            return classId;
        }

        public int getRank() {
            return rank;
        }

        public AbilityUnlock copy() {
            return new AbilityUnlock(this.abilityId, this.classId, this.rank);
        }
    }
}
