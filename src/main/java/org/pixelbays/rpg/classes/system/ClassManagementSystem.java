package org.pixelbays.rpg.classes.system;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.event.ActiveClassChangedEvent;
import org.pixelbays.rpg.classes.event.ClassLearnedEvent;
import org.pixelbays.rpg.classes.event.ClassUnlearnedEvent;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.expansion.ExpansionManager;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.system.RaceSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * System that manages class/job learning, switching, and prerequisites.
 * Integrates with LevelProgressionSystem for class leveling.
 */
@SuppressWarnings("null")
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
    public ClassDefinition getClassDefinition(@Nullable String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }

        var assetMap = ClassDefinition.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        ClassDefinition directMatch = assetMap.getAsset(classId);
        if (directMatch != null) {
            return directMatch;
        }

        for (ClassDefinition classDefinition : assetMap.getAssetMap().values()) {
            if (classDefinition == null) {
                continue;
            }

            if (classId.equalsIgnoreCase(classDefinition.getId())
                    || classId.equalsIgnoreCase(classDefinition.getDisplayName())) {
                return classDefinition;
            }
        }

        return null;
    }

    @Nullable
    public String resolveClassId(@Nullable String classId) {
        ClassDefinition classDefinition = getClassDefinition(classId);
        return classDefinition == null ? null : classDefinition.getId();
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
        pruneUnknownClasses(entityRef, store);

        String resolvedClassId = resolveClassId(classId);
        // Get class definition
        ClassDefinition classDef = getClassDefinition(resolvedClassId == null ? classId : resolvedClassId);
        if (classDef == null || resolvedClassId == null) {
            return "ERROR: Unknown class: " + classId;
        }
        classId = resolvedClassId;

        if (!classDef.isEnabled()) {
            return "ERROR: Class " + classId + " is not available";
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            ExpansionManager expansionManager = Realmweavers.get().getExpansionManager();
            if (!expansionManager.hasAccess(playerRef, classDef.getRequiredExpansionIds())) {
                return "ERROR: Requires expansion access: "
                        + expansionManager.describeRequirements(classDef.getRequiredExpansionIds());
            }
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
        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        boolean initializedLevelSystem = false;
        if (systemId != null && !systemId.isEmpty()
                && !levelProgressionSystem.getAllLevelSystems(entityRef).contains(systemId)) {
            levelProgressionSystem.initializeLevelSystem(entityRef, systemId);
            initializedLevelSystem = true;
        }

        if (initializedLevelSystem) {
            int initialClassLevel = classDef.getInitialClassLevel();
            if (initialClassLevel > 1) {
                levelProgressionSystem.addLevels(entityRef, systemId, initialClassLevel - 1, store,
                        store.getExternalData().getWorld());
            }
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
        }

        // Unlock abilities available at current level
        int currentLevel = 1;
        if (systemId != null && !systemId.isEmpty()) {
            currentLevel = levelProgressionSystem.getLevel(entityRef, systemId);
            if (currentLevel <= 0) {
                currentLevel = 1;
            }
        }
        String resolvedSystemId = systemId == null ? "" : systemId;

        if (shouldAutoLearnAbilitiesOnLevelUp()) {
            for (ClassDefinition.AbilityUnlock unlock : classDef.getAbilityUnlocksAtLevel(currentLevel)) {
                String abilityId = unlock.getAbilityId();
                if (abilityId != null && !abilityId.isEmpty()) {
                    unlockAbility(entityRef, classId, resolvedSystemId, abilityId, store);
                }
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
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null) {
            return "ERROR: Class " + classId + " is not learned";
        }

        String resolvedClassId = resolveClassId(classId);
        ClassDefinition classDef = getClassDefinition(resolvedClassId == null ? classId : resolvedClassId);
        if (classDef == null || resolvedClassId == null) {
            if (!classComp.hasLearnedClass(classId)) {
                return "ERROR: Unknown class: " + classId;
            }

            removeClassState(entityRef, classComp, classId, store);
            RpgLogging.debugDeveloper("[ClassSystem] Entity unlearned unknown class: %s", classId);
            return "SUCCESS: Unlearned " + classId;
        }
        classId = resolvedClassId;

        if (!classComp.hasLearnedClass(classId)) {
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

        removeClassState(entityRef, classComp, classId, store);

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
        pruneUnknownClasses(entityRef, store);

        String resolvedClassId = resolveClassId(classId);
        ClassDefinition classDef = getClassDefinition(resolvedClassId == null ? classId : resolvedClassId);
        if (classDef == null || resolvedClassId == null) {
            return "ERROR: Unknown class: " + classId;
        }
        classId = resolvedClassId;

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
        pruneUnknownClasses(entityRef, store);

        String resolvedClassId = resolveClassId(classId);
        ClassDefinition classDef = getClassDefinition(resolvedClassId == null ? classId : resolvedClassId);
        if (classDef == null || resolvedClassId == null || !classDef.isEnabled()) {
            return false;
        }
        classId = resolvedClassId;

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null && !Realmweavers.get().getExpansionManager().hasAccess(playerRef, classDef.getRequiredExpansionIds())) {
            return false;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp != null && classComp.hasLearnedClass(classId)) {
            return false; // Already learned
        }

        return checkPrerequisites(entityRef, classDef, store) == null;
    }

    /**
     * Preview whether an ability can be manually learned and whether the entity can
     * afford any configured learn costs.
     */
    @Nonnull
    public ClassAbilityLearnResult previewAbilityLearn(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String abilityId,
            @Nonnull Store<EntityStore> store) {
        return validateAbilityLearn(entityRef, classId, abilityId, store, true,
                resolveCharacterCurrencyAccessContext(entityRef, store));
    }

    /**
     * Manually learn a class ability, optionally charging configured learn costs.
     * This is intended for future trainer-style interactions.
     */
    @Nonnull
    public ClassAbilityLearnResult learnAbility(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String abilityId,
            boolean chargeLearnCosts,
            @Nonnull Store<EntityStore> store) {
        return learnAbility(entityRef, classId, abilityId, chargeLearnCosts, store,
                resolveCharacterCurrencyAccessContext(entityRef, store));
    }

    /**
     * Manually learn a class ability using an explicit character currency access
     * context for physical-item currencies.
     */
    @Nonnull
    public ClassAbilityLearnResult learnAbility(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String abilityId,
            boolean chargeLearnCosts,
            @Nonnull Store<EntityStore> store,
            @Nonnull CurrencyAccessContext characterCurrencyAccessContext) {
        ClassAbilityLearnResult validation = validateAbilityLearn(entityRef, classId, abilityId, store,
                chargeLearnCosts, characterCurrencyAccessContext);
        if (!validation.isSuccess()) {
            return validation;
        }

        classId = validation.getClassId();

        ClassDefinition.AbilityUnlock unlock = validation.getAbilityUnlock();
        if (unlock == null) {
            return ClassAbilityLearnResult.failure("ERROR: Ability unlock is not configured", classId, abilityId);
        }

        if (chargeLearnCosts && unlock.hasLearnCosts()) {
            CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
            List<ClassDefinition.AbilityLearnCost> learnCosts = sanitizeLearnCosts(unlock.getLearnCosts());
            for (ClassDefinition.AbilityLearnCost learnCost : learnCosts) {
                if (learnCost == null) {
                    continue;
                }
                CurrencyScope scope = learnCost.getCurrencyScope();
                String ownerId = resolveCurrencyOwnerId(scope, entityRef, store);
                if (ownerId == null || ownerId.isBlank()) {
                    return ClassAbilityLearnResult.failure(
                            "ERROR: Could not resolve currency owner for scope " + scope.name(),
                            classId,
                            abilityId,
                            unlock);
                }
                CurrencyAccessContext accessContext = scope == CurrencyScope.Character
                        ? characterCurrencyAccessContext
                        : CurrencyAccessContext.empty();
                CurrencyAmountDefinition amountDefinition = learnCost.toCurrencyAmountDefinition();
                CurrencyActionResult spendResult = currencyManager.spend(scope, ownerId, amountDefinition, accessContext);
                if (!spendResult.isSuccess()) {
                    return ClassAbilityLearnResult.failure(spendResult.getMessage(), classId, abilityId, unlock);
                }
            }
        }

        String resolvedSystemId = resolveAbilityLevelSystemId(getClassDefinition(classId));
        if (!unlockAbility(entityRef, classId, resolvedSystemId, abilityId, store)) {
            return ClassAbilityLearnResult.failure("ERROR: Ability is already learned", classId, abilityId, unlock);
        }

        return ClassAbilityLearnResult.success("SUCCESS: Learned ability " + abilityId, classId, abilityId, unlock);
    }

    /**
     * Ensure the entity has at least one class learned.
     *
     * @return true if a class was learned during this call
     */
    public boolean ensureStartingClass(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        pruneUnknownClasses(entityRef, store);

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (hasKnownLearnedClasses(classComp)) {
            return false;
        }

        getOrCreateClassComponent(entityRef, store);

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

    @Nonnull
    private ClassAbilityLearnResult validateAbilityLearn(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String abilityId,
            @Nonnull Store<EntityStore> store,
            boolean validateLearnCosts,
            @Nonnull CurrencyAccessContext characterCurrencyAccessContext) {
        pruneUnknownClasses(entityRef, store);

        String resolvedClassId = resolveClassId(classId);
        ClassDefinition classDef = getClassDefinition(resolvedClassId == null ? classId : resolvedClassId);
        if (classDef == null || resolvedClassId == null) {
            return ClassAbilityLearnResult.failure("ERROR: Unknown class: " + classId, classId, abilityId);
        }
        classId = resolvedClassId;
        if (!classDef.isEnabled()) {
            return ClassAbilityLearnResult.failure("ERROR: Class " + classId + " is not available", classId,
                    abilityId);
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return ClassAbilityLearnResult.failure("ERROR: Must learn class before learning its abilities", classId,
                    abilityId);
        }

        ClassDefinition.AbilityUnlock unlock = classDef.getAbilityUnlock(abilityId);
        if (unlock == null) {
            return ClassAbilityLearnResult.failure("ERROR: Ability " + abilityId + " is not configured for class "
                    + classId, classId, abilityId);
        }

        int currentLevel = resolveCurrentLevel(entityRef, classDef);
        if (currentLevel < unlock.getUnlockLevel()) {
            return ClassAbilityLearnResult.failure(
                    "ERROR: Requires level " + unlock.getUnlockLevel() + " to learn " + abilityId,
                    classId,
                    abilityId,
                    unlock);
        }

        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp != null && abilityComp.hasAbility(abilityId)) {
            return ClassAbilityLearnResult.failure("ERROR: Ability is already learned", classId, abilityId, unlock);
        }

        if (validateLearnCosts && unlock.hasLearnCosts()) {
            CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
            for (ClassDefinition.AbilityLearnCost learnCost : sanitizeLearnCosts(unlock.getLearnCosts())) {
                if (learnCost == null) {
                    continue;
                }
                CurrencyScope scope = learnCost.getCurrencyScope();
                String ownerId = resolveCurrencyOwnerId(scope, entityRef, store);
                if (ownerId == null || ownerId.isBlank()) {
                    return ClassAbilityLearnResult.failure(
                            "ERROR: Could not resolve currency owner for scope " + scope.name(),
                            classId,
                            abilityId,
                            unlock);
                }

                CurrencyAccessContext accessContext = scope == CurrencyScope.Character
                        ? characterCurrencyAccessContext
                        : CurrencyAccessContext.empty();
                if (!currencyManager.canAfford(scope, ownerId, learnCost.toCurrencyAmountDefinition(), accessContext)) {
                    return ClassAbilityLearnResult.failure(
                            "ERROR: Cannot afford learn cost for ability " + abilityId,
                            classId,
                            abilityId,
                            unlock);
                }
            }
        }

        return ClassAbilityLearnResult.success("SUCCESS: Ability can be learned", classId, abilityId, unlock);
    }

    private boolean unlockAbility(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String resolvedSystemId,
            @Nonnull String abilityId,
            @Nonnull Store<EntityStore> store) {
        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp == null) {
            abilityComp = store.addComponent(entityRef, ClassAbilityComponent.getComponentType());
        }

        if (abilityComp.hasAbility(abilityId)) {
            return false;
        }

        abilityComp.unlockAbility(abilityId, classId, 1);
        ClassAbilityUnlockedEvent.dispatch(entityRef, classId, abilityId, 1, resolvedSystemId, false);
        return true;
    }

    private boolean shouldAutoLearnAbilitiesOnLevelUp() {
        RpgModConfig config = resolveConfig();
        return config == null || config.shouldAutoLearnClassAbilitiesOnLevelUp();
    }

    private int resolveCurrentLevel(@Nonnull Ref<EntityStore> entityRef, @Nonnull ClassDefinition classDef) {
        String systemId = resolveAbilityLevelSystemId(classDef);
        if (systemId.isEmpty()) {
            return 1;
        }

        int currentLevel = levelProgressionSystem.getLevel(entityRef, systemId);
        return currentLevel <= 0 ? 1 : currentLevel;
    }

    @Nonnull
    private String resolveAbilityLevelSystemId(@Nullable ClassDefinition classDef) {
        if (classDef == null) {
            return "";
        }

        String systemId = classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId();
        return systemId == null ? "" : systemId;
    }

    @Nonnull
    private List<ClassDefinition.AbilityLearnCost> sanitizeLearnCosts(
            @Nullable List<ClassDefinition.AbilityLearnCost> learnCosts) {
        List<ClassDefinition.AbilityLearnCost> resolved = new ArrayList<>();
        if (learnCosts == null || learnCosts.isEmpty()) {
            return resolved;
        }

        for (ClassDefinition.AbilityLearnCost learnCost : learnCosts) {
            if (learnCost == null || learnCost.isFree()) {
                continue;
            }
            resolved.add(learnCost);
        }
        return resolved;
    }

    @Nonnull
    private CurrencyAccessContext resolveCharacterCurrencyAccessContext(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null || player.getInventory() == null) {
            return CurrencyAccessContext.empty();
        }
        return CurrencyAccessContext.fromInventory(player.getInventory());
    }

    @Nullable
    private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return scope == CurrencyScope.Global ? "global" : null;
        }

        return switch (scope) {
            case Character -> Realmweavers.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
            case Account -> playerRef.getUuid().toString();
            case Guild -> {
                Guild guild = Realmweavers.get().getGuildManager().getGuildForMember(playerRef.getUuid());
                yield guild == null ? null : guild.getId().toString();
            }
            case Global -> "global";
            case Custom -> null;
        };
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

    public boolean hasKnownLearnedClasses(@Nullable ClassComponent classComp) {
        return getPrimaryKnownClassId(classComp) != null;
    }

    @Nullable
    public String getPrimaryKnownClassId(@Nullable ClassComponent classComp) {
        if (classComp == null) {
            return null;
        }

        for (String classId : classComp.getLearnedClassIds()) {
            if (isKnownEnabledClassId(classId)) {
                return classId;
            }
        }

        return null;
    }

    @Nonnull
    public Set<String> pruneUnknownClasses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || classComp.getLearnedClassIds().isEmpty()) {
            return java.util.Set.of();
        }

        Set<String> removedClassIds = new LinkedHashSet<>();
        for (String classId : classComp.getLearnedClassIds()) {
            if (isKnownEnabledClassId(classId)) {
                continue;
            }
            removedClassIds.add(classId);
        }

        if (removedClassIds.isEmpty()) {
            return java.util.Set.of();
        }

        for (String classId : removedClassIds) {
            removeClassState(entityRef, classComp, classId, store);
        }

        RpgLogging.debugDeveloper("[ClassSystem] Pruned unknown/disabled classes for %s: %s",
                entityRef.getIndex(), removedClassIds);
        return removedClassIds;
    }

    public boolean isKnownEnabledClassId(@Nullable String classId) {
        ClassDefinition classDefinition = getClassDefinition(classId);
        return classDefinition != null && classDefinition.isEnabled();
    }

    private void removeClassState(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ClassComponent classComp,
            @Nonnull String classId,
            @Nonnull Store<EntityStore> store) {
        classComp.unlearnClass(classId);

        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp != null) {
            abilityComp.removeAbilitiesForClass(classId);
        }

        if (statSystem != null) {
            statSystem.recalculateClassStatBonuses(entityRef, store);
            statSystem.recalculateTalentStatBonuses(entityRef, store);
        }
    }
}
