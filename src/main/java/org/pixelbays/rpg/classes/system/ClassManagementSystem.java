package org.pixelbays.rpg.classes.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * System that manages class/job learning, switching, and prerequisites.
 * Integrates with LevelProgressionSystem for class leveling.
 */
public class ClassManagementSystem {
    // Loaded class definitions from asset pack
    private final Map<String, ClassDefinition> classDefinitions;

    // Reference to level progression system for class leveling
    private final LevelProgressionSystem levelProgressionSystem;
    private StatSystem statSystem;

    public ClassManagementSystem(@Nonnull LevelProgressionSystem levelProgressionSystem) {
        this.levelProgressionSystem = levelProgressionSystem;
        this.classDefinitions = new HashMap<>();
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    /**
     * Register a class definition from asset file
     */
    public void registerClass(ClassDefinition classDefinition) {
        classDefinitions.put(classDefinition.getId(), classDefinition);
        System.out.println("[ClassSystem] Registered class: " + classDefinition.getId() +
                " (" + classDefinition.getDisplayName() + ")");
    }

    /**
     * Load class definitions from JSON files in the asset pack
     * Path: Server/Classes/*.json
     * 
     * This method scans all registered asset packs for class definitions and loads
     * them.
     * Classes are loaded using Gson JSON parsing and support inheritance from
     * parent classes.
     */

    public void loadClassDefinitionsFromAssets() {
        System.out.println("[ClassSystem] Loading class definitions from asset registry...");

        classDefinitions.clear();

        try {
            var store = AssetRegistry.getAssetStore(ClassDefinition.class);
            if (store != null) {
                for (ClassDefinition classDef : store.getAssetMap().getAssetMap().values()) {
                    registerClass(classDef);
                }
                System.out.println(
                        "[ClassSystem] Loaded " + classDefinitions.size() + " classes from asset registry");
            }
        } catch (Exception e) {
            System.err.println("[ClassSystem] Failed to load classes from asset registry: " + e.getMessage());
        }
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
        ClassDefinition classDef = classDefinitions.get(classId);
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
                ClassDefinition exclusiveDef = classDefinitions.get(exclusiveClass);
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

        // If this is the first class or no active class, make it active
        if (classComp.getActiveClassId().isEmpty()) {
            setActiveClass(entityRef, classId, store);
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        // Unlock abilities available at current level
        int currentLevel = 1;
        String systemId = classDef.usesCharacterLevel() ? "character_level" : classDef.getLevelSystemId();
        if (systemId != null && !systemId.isEmpty()) {
            currentLevel = levelProgressionSystem.getLevel(entityRef, systemId);
            if (currentLevel <= 0) {
                currentLevel = 1;
            }
        }

        for (ClassDefinition.AbilityUnlock unlock : classDef.getAbilityUnlocksAtLevel(currentLevel)) {
            String abilityId = unlock.getAbilityId();
            if (abilityId != null && !abilityId.isEmpty() && !classComp.hasUnlockedSpell(abilityId)) {
                classComp.unlockSpell(abilityId, 1);
            }
        }

        System.out.println("[ClassSystem] Entity learned class: " + classId);
        return "SUCCESS: Learned " + classDef.getDisplayName();
    }

    /**
     * Unlearn a class (with XP penalty if relearning)
     * 
     * @param entityRef Entity unlearning the class
     * @param classId   Class to unlearn
     * @param store     Entity store
     * @return Success message or error
     */
    public String unlearnClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        ClassDefinition classDef = classDefinitions.get(classId);
        if (classDef == null) {
            return "ERROR: Unknown class: " + classId;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: Class " + classId + " is not learned";
        }

        // Apply relearn penalty if configured
        if (classDef.getRelearnExpPenalty() > 0 && !classDef.getLevelSystemId().isEmpty()) {
            ClassComponent.ClassData classData = classComp.getClassData(classId);
            if (classData != null) {
                float totalExp = classData.getTotalExpEarned();
                float penalty = totalExp * classDef.getRelearnExpPenalty();

                // Remove experience from level system
                levelProgressionSystem.removeExperience(entityRef, classDef.getLevelSystemId(), penalty);

                System.out.println("[ClassSystem] Applied " + (classDef.getRelearnExpPenalty() * 100) +
                        "% XP penalty (" + penalty + " XP) for unlearning " + classId);
            }
        }

        // Remove class data
        classComp.unlearnClass(classId);

        // Remove all abilities from this class
        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp != null) {
            abilityComp.removeAbilitiesForClass(classId);
        }

        // If this was the active class, clear active
        if (classComp.getActiveClassId().equals(classId)) {
            classComp.setActiveClassId("");
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        System.out.println("[ClassSystem] Entity unlearned class: " + classId);
        return "SUCCESS: Unlearned " + classDef.getDisplayName();
    }

    /**
     * Set active class
     * 
     * @param entityRef Entity changing class
     * @param classId   Class to activate
     * @param store     Entity store
     * @return Success message or error
     */
    public String setActiveClass(@Nonnull Ref<EntityStore> entityRef, String classId,
            @Nonnull Store<EntityStore> store) {
        ClassDefinition classDef = classDefinitions.get(classId);
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

        // TODO: Check combat state if canSwitchInCombat is false
        // TODO: Check cooldown

        // Set active class
        classComp.setActiveClassId(classId);

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        System.out.println("[ClassSystem] Entity activated class: " + classId);
        return "SUCCESS: Activated " + classDef.getDisplayName();
    }

    /**
     * Check if entity meets prerequisites for a class
     */
    @Nullable
    private String checkPrerequisites(@Nonnull Ref<EntityStore> entityRef, ClassDefinition classDef,
            @Nonnull Store<EntityStore> store) {
        // Check class level prerequisites
        LevelProgressionComponent levelComp = store.getComponent(entityRef,
            LevelProgressionComponent.getComponentType());
        if (levelComp != null && classDef.getPrerequisites() != null && !classDef.getPrerequisites().isEmpty()) {
            for (Map.Entry<String, Integer> prereq : classDef.getPrerequisites().entrySet()) {
                String requiredClassId = prereq.getKey();
                int requiredLevel = prereq.getValue();

                ClassDefinition requiredClass = classDefinitions.get(requiredClassId);
                if (requiredClass == null) {
                    return "ERROR: Requires unknown class " + requiredClassId;
                }

                String levelSystemId = requiredClass.usesCharacterLevel()
                        ? "character_level"
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
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp != null) {
            for (String requiredClass : classDef.getRequiredClasses()) {
                if (!classComp.hasLearnedClass(requiredClass)) {
                    ClassDefinition requiredDef = classDefinitions.get(requiredClass);
                    String requiredName = requiredDef != null ? requiredDef.getDisplayName() : requiredClass;
                    return "ERROR: Requires " + requiredName + " class";
                }
            }
        }

        return null; // All prerequisites met
    }

    /**
     * Get class definition
     */
    public ClassDefinition getClassDefinition(String classId) {
        return classDefinitions.get(classId);
    }

    /**
     * Get all class definitions
     */
    public Map<String, ClassDefinition> getAllClassDefinitions() {
        return classDefinitions;
    }

    /**
     * Get all registered class IDs
     */
    public java.util.Set<String> getRegisteredClasses() {
        return classDefinitions.keySet();
    }

    /**
     * Get class IDs that use the given level system ID.
     */
    public List<String> getClassesForLevelSystem(@Nonnull String systemId) {
        List<String> results = new java.util.ArrayList<>();
        for (ClassDefinition classDef : classDefinitions.values()) {
            String classSystemId = classDef.usesCharacterLevel() ? "character_level" : classDef.getLevelSystemId();
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
        ClassDefinition classDef = classDefinitions.get(classId);
        if (classDef == null || !classDef.isEnabled()) {
            return false;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp != null && classComp.hasLearnedClass(classId)) {
            return false; // Already learned
        }

        return checkPrerequisites(entityRef, classDef, store) == null;
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

}
