package org.pixelbays.rpg.classes.system;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.event.ActiveClassChangedEvent;
import org.pixelbays.rpg.classes.event.ClassLearnedEvent;
import org.pixelbays.rpg.classes.event.ClassUnlearnedEvent;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.system.RaceSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * System that manages class/job learning, switching, and prerequisites.
 * Integrates with LevelProgressionSystem for class leveling.
 */
public class ClassManagementSystem {
    // Reference to level progression system for class leveling
    private final LevelProgressionSystem levelProgressionSystem;
    private StatSystem statSystem;
    private RaceSystem raceSystem;

    public ClassManagementSystem(@Nonnull LevelProgressionSystem levelProgressionSystem) {
        this.levelProgressionSystem = levelProgressionSystem;
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    public void setRaceSystem(@Nullable RaceSystem raceSystem) {
        this.raceSystem = raceSystem;
    }

    /**
     * Get class definition from asset store
     */
    public ClassDefinition getClassDefinition(String classId) {
        return ClassDefinition.getAssetMap().getAsset(classId);
    }

    /**
     * Learn a new class
     * 
     * @param entityRef Entity learning the class
     * @param classId   Class to learn
     * @param store     Entity store
     * @return Success message or error
     */
    public String learnClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        // Get class definition
        ClassDefinition classDef = getClassDefinition(classId);
        if (classDef == null) {
            return "ERROR: Unknown class: " + classId;
        }

        if (!classDef.isEnabled()) {
            return "ERROR: Class " + classId + " is not available";
        }

        // Get or create class component
        ClassComponent classComp = getOrCreateClassComponent(entityRef, store);

        // Check if already learned
        if (classComp.hasLearnedClass(classId)) {
            return "ERROR: Already learned class " + classId;
        }

        // Check prerequisites
        String prereqError = checkPrerequisites(entityRef, classDef, store);
        if (prereqError != null) {
            return prereqError;
        }

        // Check exclusive classes
        for (String exclusiveClass : classDef.getExclusiveWith()) {
            if (classComp.hasLearnedClass(exclusiveClass)) {
                ClassDefinition exclusiveDef = getClassDefinition(exclusiveClass);
                String exclusiveName = exclusiveDef != null ? exclusiveDef.getDisplayName() : exclusiveClass;
                return "ERROR: Cannot learn " + classDef.getDisplayName() +
                        " while " + exclusiveName + " is learned";
            }
        }

        // Learn the class
        classComp.learnClass(classId);

        // Initialize class level system if needed
        if (!classDef.getLevelSystemId().isEmpty() && !classDef.usesCharacterLevel()) {
            levelProgressionSystem.initializeLevelSystem(entityRef, classDef.getLevelSystemId());
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        // Unlock abilities available at current level
        int currentLevel = 1;
        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        if (systemId != null && !systemId.isEmpty()) {
            currentLevel = levelProgressionSystem.getLevel(entityRef, systemId);
            if (currentLevel <= 0) {
                currentLevel = 1;
            }
        }
        String resolvedSystemId = systemId == null ? "" : systemId;

        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp == null) {
            abilityComp = store.addComponent(entityRef, ClassAbilityComponent.getComponentType());
        }

        for (ClassDefinition.AbilityUnlock unlock : classDef.getAbilityUnlocksAtLevel(currentLevel)) {
            String abilityId = unlock.getAbilityId();
            if (abilityId != null && !abilityId.isEmpty() && !abilityComp.hasAbility(abilityId)) {
                abilityComp.unlockAbility(abilityId, classId, 1);
                ClassAbilityUnlockedEvent.dispatch(entityRef, classId, abilityId, 1, resolvedSystemId, false);
            }
        }

        ClassLearnedEvent.dispatch(entityRef, classId);

        RpgLogging.debugDeveloper("[ClassSystem] Entity learned class: %s", classId);
        return "SUCCESS: Learned " + classDef.getDisplayName();
    }

    /**
     * Unlearn a class
     * 
     * @param entityRef Entity unlearning the class
     * @param classId   Class to unlearn
     * @param store     Entity store
     * @return Success message or error
     */
    public String unlearnClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        ClassDefinition classDef = getClassDefinition(classId);
        if (classDef == null) {
            return "ERROR: Unknown class: " + classId;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: Class " + classId + " is not learned";
        }

        // Apply relearn penalty using current level system exp
        if (classDef.getRelearnExpPenalty() > 0) {
            String levelSystemId = classDef.usesCharacterLevel()
                    ? "Base_Character_Level"
                    : classDef.getLevelSystemId();

            if (levelSystemId != null && !levelSystemId.isEmpty()) {
                LevelProgressionComponent levelComp = store.getComponent(entityRef,
                        LevelProgressionComponent.getComponentType());
                if (levelComp != null && levelComp.getSystem(levelSystemId) != null) {
                    float currentExp = levelProgressionSystem.getExperience(entityRef, levelSystemId);
                    float penalty = currentExp * classDef.getRelearnExpPenalty();

                    if (penalty > 0f) {
                        levelProgressionSystem.removeExperience(entityRef, levelSystemId, penalty);
                        RpgLogging.debugDeveloper(
                                "[ClassSystem] Applied %s%% XP penalty (%s XP) for unlearning %s",
                                classDef.getRelearnExpPenalty() * 100,
                                penalty,
                                classId);
                    }
                }
            }
        }

        // Remove class data
        classComp.unlearnClass(classId);

        // Remove all abilities from this class
        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp != null) {
            abilityComp.removeAbilitiesForClass(classId);
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        ClassUnlearnedEvent.dispatch(entityRef, classId);

        RpgLogging.debugDeveloper("[ClassSystem] Entity unlearned class: %s", classId);
        return "SUCCESS: Unlearned " + classDef.getDisplayName();
    }

    /**
     * Prioritize a learned class
     * 
     * @param entityRef Entity changing class
     * @param classId   Class to activate
     * @param store     Entity store
     * @return Success message or error
     */
    public String setActiveClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        ClassDefinition classDef = getClassDefinition(classId);
        if (classDef == null) {
            return "ERROR: Unknown class: " + classId;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: Must learn class before activating";
        }

        // Check switching rules
        if (!classDef.getSwitchingRules().canSwitch()) {
            return "ERROR: Cannot switch to this class";
        }
        if (classDef.getSwitchingRules().canSwitchInCombat() == false) {
            return "ERROR: Cannot switch to this class while in combat";
        }
        // TODO: Check cooldown, if not allowed return error with time remaining until
        // next switch

        // Prioritize the requested class in learned class order
        String oldActiveClassId = classComp.getPrimaryClassId();
        classComp.prioritizeClass(classId);

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        ActiveClassChangedEvent.dispatch(entityRef,
            oldActiveClassId == null ? "" : oldActiveClassId,
            classId);

        RpgLogging.debugDeveloper("[ClassSystem] Entity activated class: %s", classId);
        return "SUCCESS: Activated " + classDef.getDisplayName();
    }

    /**
     * Check if entity meets prerequisites for a class
     */
    @Nullable
    private String checkPrerequisites(@Nonnull Ref<EntityStore> entityRef, @Nonnull ClassDefinition classDef,
            @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());

        // Enforce max combat/profession class limits based on tags
        RpgModConfig config = resolveConfig();
        if (config != null && classComp != null) {
            int maxCombat = config.getMaxCombatClasses();
            int maxProfession = config.getMaxProfessionClasses();

            boolean isCombat = hasTag(classDef, "type", "combat");
            boolean isProfession = hasTag(classDef, "type", "profession");
            boolean isJob = hasTag(classDef, "type", "job");
            boolean jobExempt = isJob && isJobForKnownClass(classDef, classComp);

            if (!jobExempt) {
                if (isCombat && maxCombat > 0) {
                    int combatCount = countLearnedByTag(classComp, "type", "combat");
                    if (combatCount >= maxCombat) {
                        return "ERROR: Maximum combat classes reached";
                    }
                }

                if (isProfession && maxProfession > 0) {
                    int professionCount = countLearnedByTag(classComp, "type", "profession");
                    if (professionCount >= maxProfession) {
                        return "ERROR: Maximum profession classes reached";
                    }
                }
            }
        }

        // Enforce class race tags (Tags.races) if present
        List<String> allowedRaces = getTagValues(classDef, "races");
        if (allowedRaces != null && !allowedRaces.isEmpty() && raceSystem != null) {
            String raceId = raceSystem.getRaceId(entityRef);
            if (raceId.isEmpty()) {
                return "ERROR: You must select a race before learning " + classDef.getDisplayName();
            }

            boolean allowsAll = false;
            boolean allowsRace = false;
            for (String allowed : allowedRaces) {
                if (allowed == null || allowed.isEmpty()) {
                    continue;
                }

                if (allowed.equalsIgnoreCase("Global")) {
                    allowsAll = true;
                    break;
                }

                if (allowed.equalsIgnoreCase(raceId)) {
                    allowsRace = true;
                    break;
                }
            }

            if (!allowsAll && !allowsRace) {
                return "ERROR: Your race cannot learn " + classDef.getDisplayName();
            }
        }

        // Check class level prerequisites
        LevelProgressionComponent levelComp = store.getComponent(entityRef,
                LevelProgressionComponent.getComponentType());
        if (levelComp != null && classDef.getPrerequisites() != null && !classDef.getPrerequisites().isEmpty()) {
            for (Map.Entry<String, Integer> prereq : classDef.getPrerequisites().entrySet()) {
                String requiredClassId = prereq.getKey();
                int requiredLevel = prereq.getValue();

                ClassDefinition requiredClass = getClassDefinition(requiredClassId);
                if (requiredClass == null) {
                    return "ERROR: Requires unknown class " + requiredClassId;
                }

                String levelSystemId = requiredClass.usesCharacterLevel()
                        ? "Base_Character_Level"
                        : requiredClass.getLevelSystemId();
                if (levelSystemId == null || levelSystemId.isEmpty()) {
                    return "ERROR: Requires " + requiredClass.getDisplayName() + " level " + requiredLevel;
                }

                LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(levelSystemId);
                int currentLevel = systemData != null ? systemData.getCurrentLevel() : 0;
                if (currentLevel < requiredLevel) {
                    return "ERROR: Requires " + requiredClass.getDisplayName() + " level " + requiredLevel;
                }
            }
        }

        // Check required classes
        if (classComp != null) {
            for (String requiredClass : classDef.getRequiredClasses()) {
                if (!classComp.hasLearnedClass(requiredClass)) {
                    ClassDefinition requiredDef = getClassDefinition(requiredClass);
                    String requiredName = requiredDef != null ? requiredDef.getDisplayName() : requiredClass;
                    return "ERROR: Requires " + requiredName + " class";
                }
            }
        }

        return null; // All prerequisites met
    }

    /**
     * Get all class definitions from asset store
     */
    public Map<String, ClassDefinition> getAllClassDefinitions() {
        return ClassDefinition.getAssetMap().getAssetMap();
    }

    /**
     * Get all registered class IDs
     */
    public java.util.Set<String> getRegisteredClasses() {
        return getAllClassDefinitions().keySet();
    }

    /**
     * Get class IDs that use the given level system ID.
     */
    public List<String> getClassesForLevelSystem(@Nonnull String systemId) {
        List<String> results = new java.util.ArrayList<>();
        for (ClassDefinition classDef : getAllClassDefinitions().values()) {
            String classSystemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
            if (systemId.equals(classSystemId)) {
                results.add(classDef.getId());
            }
        }
        return results;
    }

    /**
     * Check if entity can learn a class
     */
    public boolean canLearnClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        ClassDefinition classDef = getClassDefinition(classId);
        if (classDef == null || !classDef.isEnabled()) {
            return false;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp != null && classComp.hasLearnedClass(classId)) {
            return false; // Already learned
        }

        return checkPrerequisites(entityRef, classDef, store) == null;
    }

    /**
     * Ensure the entity has at least one class learned.
     *
     * @return true if a class was learned during this call
     */
    public boolean ensureStartingClass(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp != null && !classComp.getLearnedClassIds().isEmpty()) {
            return false;
        }

        ClassComponent resolved = getOrCreateClassComponent(entityRef, store);

        ClassDefinition startingClass = null;
        java.util.List<ClassDefinition> candidates = new java.util.ArrayList<>(getAllClassDefinitions().values());
        candidates.sort((a, b) -> {
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return 1;
            }
            if (b == null) {
                return -1;
            }
            return a.getId().compareToIgnoreCase(b.getId());
        });

        for (ClassDefinition def : candidates) {
            if (def == null || !def.isEnabled() || !def.isStartingClass()) {
                continue;
            }
            startingClass = def;
            break;
        }

        if (startingClass == null) {
            return false;
        }

        String result = learnClass(entityRef, startingClass.getId(), store);
        if (result != null && result.startsWith("SUCCESS")) {
            return true;
        }

        RpgLogging.debugDeveloper("[ClassSystem] Failed to auto-learn starting class %s: %s",
                startingClass.getId(), result);
        return false;
    }

    // === Helper Methods ===

    @Nonnull
    private ClassComponent getOrCreateClassComponent(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        ClassComponent component = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (component == null) {
            component = store.addComponent(entityRef, ClassComponent.getComponentType());
        }
        return component;
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }

    private boolean hasTag(@Nullable ClassDefinition classDef, @Nonnull String tagKey, @Nonnull String tagValue) {
        return classDef != null && classDef.hasTag(tagKey, tagValue);
    }

    private boolean isJobForKnownClass(@Nonnull ClassDefinition classDef, @Nonnull ClassComponent classComp) {
        if (!hasTag(classDef, "type", "job")) {
            return false;
        }

        for (String requiredClass : classDef.getRequiredClasses()) {
            if (classComp.hasLearnedClass(requiredClass)) {
                return true;
            }
        }

        String parent = classDef.getParent();
        return parent != null && !parent.isEmpty() && classComp.hasLearnedClass(parent);
    }

    private int countLearnedByTag(@Nonnull ClassComponent classComp, @Nonnull String tagKey, @Nonnull String tagValue) {
        int count = 0;
        for (String learnedId : classComp.getLearnedClassIds()) {
            ClassDefinition learnedDef = getClassDefinition(learnedId);
            if (!hasTag(learnedDef, tagKey, tagValue)) {
                continue;
            }

            if (learnedDef != null && hasTag(learnedDef, "type", "job")
                    && isJobForKnownClass(learnedDef, classComp)) {
                continue;
            }

            count++;
        }
        return count;
    }

    @Nullable
    private List<String> getTagValues(@Nullable ClassDefinition classDef, @Nonnull String tagKey) {
        if (classDef == null) {
            return null;
        }

        Map<String, List<String>> tags = classDef.getTags();
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(tagKey)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
