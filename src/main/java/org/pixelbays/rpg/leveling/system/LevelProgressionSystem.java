package org.pixelbays.rpg.leveling.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * System that handles level progression logic.
 * Manages exp gain, level-ups, and reward distribution.
 * 
 * This system should be registered with Hytale's ECS and updated each tick.
 */
@SuppressWarnings("null")
public class LevelProgressionSystem {

    // Loaded level system configurations from asset pack
    private final Map<String, LevelSystemConfig> levelSystemConfigs;
    private StatSystem statSystem;

    public LevelProgressionSystem(@Nonnull EventRegistry eventRegistry) {
        this.levelSystemConfigs = new HashMap<>();
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    /**
     * Load a level system configuration from asset pack
     */
    public void registerLevelSystem(LevelSystemConfig config) {
        if (config == null || config.getSystemId() == null || config.getSystemId().isEmpty()) {
            System.err.println("[LevelSystem] Ignored null/invalid level system config");
            return;
        }
        levelSystemConfigs.put(config.getSystemId(), config);
        System.out.println("[LevelSystem] Registered level system: " + config.getSystemId() +
                " (" + config.getDisplayName() + ")");
    }

    /**
     * Load level system configurations from JSON files in the asset pack
     * Path: Server/Entity/Stats/*.json
     * 
     * This method scans all registered asset packs for level system configurations
     * and loads them.
     * Configs are loaded using Gson JSON parsing and support inheritance from
     * parent configs.
     */
    public void loadLevelSystemsFromAssets() {
        System.out.println("[LevelSystem] Loading level system configurations from asset packs...");

        levelSystemConfigs.clear();

        try {
            // Use AssetLoader to load all level system configs from asset packs
            Map<String, LevelSystemConfig> loadedConfigs = org.pixelbays.rpg.global.util.AssetLoader
                    .loadLevelSystemConfigs();

            // Register each loaded config
            for (LevelSystemConfig config : loadedConfigs.values()) {
                registerLevelSystem(config);
            }

            System.out.println("[LevelSystem] Successfully loaded " + levelSystemConfigs.size() +
                    " level system configurations from asset packs");

        } catch (Exception e) {
            System.err.println("[LevelSystem] Failed to load level systems from asset packs: " + e.getMessage());
            System.out.println("[LevelSystem] Falling back to test configs");
        }

        // If no configs were loaded, use test data as fallback
        if (levelSystemConfigs.isEmpty()) {
            System.out
                    .println("[LevelSystem] No level system configs found in asset packs - using test configs instead");
        }

        System.out.println(
                "[LevelSystem] Level progression system initialized with " + levelSystemConfigs.size() + " systems");
    }

    /**
     * Create test configurations for testing (fallback when asset loading fails)
     */

    /**
     * Grant experience to an entity in a specific level system
     * 
     * @param entityRef The entity reference gaining exp
     * @param systemId  The level system ID (e.g., "character_level")
     * @param amount    Amount of exp to grant
     * @param source    Source of the exp (for events/logging)
     * @param store     Optional store for triggering effects (can be null)
     * @param world     Optional world for triggering effects (can be null)
     */
    public void grantExperience(@Nonnull Ref<EntityStore> entityRef, String systemId, float amount, String source,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        if (amount <= 0f) {
            return;
        }

        // Get level system config
        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null) {
            System.err.println("[LevelSystem] Unknown level system: " + systemId);
            return;
        }

        if (!config.isEnabled()) {
            return; // System disabled
        }

        // Get or create level progression component
        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);
        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        ensureExpToNextLevel(levelData, config);

        // Check prerequisites
        if (!checkPrerequisites(levelComp, config)) {
            System.out.println("[LevelSystem] Prerequisites not met for " + systemId);
            return; // Prerequisites not met
        }

        // Check if at max level
        if (config.getMaxLevel() > 0 && levelData.getCurrentLevel() >= config.getMaxLevel()) {
            return; // Already at max level
        }

        // Fire experience gain event (allows modification/cancellation)
        // TODO: Re-enable when event compilation issue is resolved
        /*
         * ExperienceGainEvent expEvent = new ExperienceGainEvent(entityRef, systemId,
         * amount, source);
         * fireEvent(expEvent);
         * 
         * if (expEvent.isCancelled()) {
         * return;
         * }
         * 
         * // Apply modified exp amount
         * float expToGrant = expEvent.getExpAmount();
         */

        // Apply XP rate gain stat (e.g., boosts/rested XP)
        float expToGrant = statSystem != null && store != null
                ? statSystem.applyExpRateGain(entityRef, amount, store)
                : amount;

        // Store current level for event
        int previousLevel = levelData.getCurrentLevel();

        // Add experience (this may trigger level-ups internally)
        int levelsGained = levelData.addExperience(expToGrant);

        // If leveled up, process each level individually
        if (levelsGained > 0) {
            processLevelUps(entityRef, systemId, previousLevel, levelData.getCurrentLevel(), levelData, config, store,
                    world);
        }

        // Update exp required for next level
        updateExpToNextLevel(levelData, config);
    }

    /**
     * Process multiple level-ups, firing individual events for each level
     */
    private void processLevelUps(@Nonnull Ref<EntityStore> entityRef, String systemId, int startLevel, int endLevel,
            LevelProgressionComponent.LevelSystemData levelData, LevelSystemConfig config,
            @Nullable Store<EntityStore> store, @Nullable World world) {

        // Fire separate event for each level gained
        for (int level = startLevel + 1; level <= endLevel; level++) {
            final int currentLevel = level;

            // TODO: Re-enable when event compilation issue is resolved
            /*
             * LevelUpEvent levelUpEvent = new LevelUpEvent(entityRef, systemId,
             * currentLevel - 1, currentLevel, levelData);
             * fireEvent(levelUpEvent);
             */
            applyLevelRewards(entityRef, systemId, currentLevel, levelData, config, store, world);

            if (store != null) {
                unlockClassAbilitiesForLevel(entityRef, systemId, currentLevel, store, true);
            }
        }

        refreshClassStatsIfNeeded(entityRef, systemId, store);

        System.out.println("[LevelSystem] Entity leveled up " + systemId + ": " +
                startLevel + " -> " + endLevel);
    }

    /**
     * Apply level-up rewards
     */
    private void applyLevelRewards(@Nonnull Ref<EntityStore> entityRef, String systemId, int newLevel,
            LevelProgressionComponent.LevelSystemData levelData, LevelSystemConfig config,
            @Nullable Store<EntityStore> store, @Nullable World world) {

        LevelSystemConfig.LevelRewardConfig rewards = config.getRewardsForLevel(newLevel);
        if (rewards == null) {
            return;
        }

        // Grant stat points
        if (rewards.getStatPoints() > 0) {
            levelData.addStatPoints(rewards.getStatPoints());
        }

        // Grant skill points
        if (rewards.getSkillPoints() > 0) {
            levelData.addSkillPoints(rewards.getSkillPoints());
        }

        // Apply stat increases
        if (store != null && statSystem != null
                && rewards.getStatIncreases() != null && !rewards.getStatIncreases().isEmpty()) {
            statSystem.applyStatIncreases(entityRef, rewards.getStatIncreases(), store);
        }

        // Apply stat growth from config
        if (store != null && statSystem != null && config.getStatGrowth() != null) {
            statSystem.applyStatGrowth(entityRef, newLevel, config.getStatGrowth(), store);
        }

        // Trigger level up effects (sound, particles, notification)
        if (rewards.getLevelUpEffects() != null && !rewards.getLevelUpEffects().isEmpty()) {
            triggerLevelUpEffects(entityRef, systemId, newLevel, rewards.getLevelUpEffects(), store, world);
        }

        // TODO: Handle other rewards (abilities, quests, currency, items, interactions)
        // These will be implemented as other systems are added
    }

    /**
     * Update the exp required for next level based on config
     */
    private void updateExpToNextLevel(LevelProgressionComponent.LevelSystemData levelData, LevelSystemConfig config) {
        int nextLevel = levelData.getCurrentLevel() + 1;

        // Check if at max level
        if (config.getMaxLevel() > 0 && levelData.getCurrentLevel() >= config.getMaxLevel()) {
            levelData.setExpToNextLevel(0); // No more levels
            return;
        }

        float expRequired = config.calculateExpForLevel(nextLevel);
        levelData.setExpToNextLevel(expRequired);
    }

    /**
     * Check if entity meets prerequisites for this level system
     */
    private boolean checkPrerequisites(LevelProgressionComponent levelComp, LevelSystemConfig config) {
        if (config.getPrerequisites() == null || config.getPrerequisites().isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Integer> prereq : config.getPrerequisites().entrySet()) {
            String requiredSystem = prereq.getKey();
            int requiredLevel = prereq.getValue();

            LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(requiredSystem);
            if (systemData == null || systemData.getCurrentLevel() < requiredLevel) {
                return false; // Prerequisite not met
            }
        }

        return true;
    }

    /**
     * Initialize a new level system for an entity
     */
    public void initializeLevelSystem(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null) {
            System.err.println("[LevelSystem] Cannot initialize unknown system: " + systemId);
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);

        // Only initialize if the system doesn't already exist
        if (levelComp.hasSystem(systemId)) {
            System.out.println("[LevelSystem] System " + systemId + " already exists, skipping initialization");
            return;
        }

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        // Set starting level
        levelData.setCurrentLevel(config.getStartingLevel());
        levelData.setCurrentExp(0);

        // Calculate exp to next level
        updateExpToNextLevel(levelData, config);

        System.out.println("[LevelSystem] Initialized " + systemId + " for entity at level " +
                config.getStartingLevel());
    }

    /**
     * Get level system configuration
     */
    public LevelSystemConfig getConfig(String systemId) {
        return levelSystemConfigs.get(systemId);
    }

    /**
     * Check if entity can access a level system (prerequisites met)
     */
    public boolean canAccessSystem(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null || !config.isEnabled()) {
            return false;
        }

        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null) {
            return true; // No component yet, can initialize
        }

        return checkPrerequisites(levelComp, config);
    }

    // ========== PUBLIC API METHODS FOR OTHER MODS ==========

    // === Query Methods ===

    /**
     * Get the current level for a specific level system
     * 
     * @return Current level, or 0 if system doesn't exist
     */
    public int getLevel(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        return systemData != null ? systemData.getCurrentLevel() : 0;
    }

    /**
     * Get the current experience for a specific level system
     * 
     * @return Current experience, or 0 if system doesn't exist
     */
    public float getExperience(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0f;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        return systemData != null ? systemData.getCurrentExp() : 0f;
    }

    /**
     * Get the experience required for the next level
     * 
     * @return Experience needed, or 0 if at max level or system doesn't exist
     */
    public float getExpToNextLevel(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0f;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        return systemData != null ? systemData.getExpToNextLevel() : 0f;
    }

    /**
     * Get the progress towards the next level as a percentage (0.0 to 1.0)
     * 
     * @return Progress percentage, or 0 if at max level or system doesn't exist
     */
    public float getExpProgress(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0f;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        if (systemData == null)
            return 0f;

        float expToNext = systemData.getExpToNextLevel();
        if (expToNext <= 0)
            return 0f; // At max level

        return systemData.getCurrentExp() / expToNext;
    }

    /**
     * Get available stat points for a level system
     * 
     * @return Available stat points, or 0 if system doesn't exist
     */
    public int getStatPoints(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        return systemData != null ? systemData.getAvailableStatPoints() : 0;
    }

    /**
     * Get available skill points for a level system
     * 
     * @return Available skill points, or 0 if system doesn't exist
     */
    public int getSkillPoints(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0;

        LevelProgressionComponent.LevelSystemData systemData = levelComp.getSystem(systemId);
        return systemData != null ? systemData.getAvailableSkillPoints() : 0;
    }

    /**
     * Check if entity has a specific level system initialized
     */
    public boolean hasLevelSystem(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        return levelComp != null && levelComp.hasSystem(systemId);
    }

    /**
     * Get all level system IDs that this entity has
     */
    public java.util.Set<String> getAllLevelSystems(@Nonnull Ref<EntityStore> entityRef) {
        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return java.util.Collections.emptySet();

        return levelComp.getAllSystems().keySet();
    }

    // === Direct Manipulation Methods ===

    /**
     * Set the level directly (bypasses normal progression, no rewards granted)
     * Useful for admin commands, cheats, or special game events
     * 
     * @param level The level to set (will be clamped to valid range)
     */
    public void setLevel(@Nonnull Ref<EntityStore> entityRef, String systemId, int level,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null) {
            System.err.println("[LevelSystem] Cannot set level for unknown system: " + systemId);
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);
        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        // Clamp level to valid range
        int maxLevel = getEffectiveMaxLevel(config);
        int clampedLevel = Math.max(1, Math.min(level, maxLevel));

        levelData.setCurrentLevel(clampedLevel);
        levelData.setCurrentExp(0);
        updateExpToNextLevel(levelData, config);

        if (store != null) {
            unlockClassAbilitiesForLevel(entityRef, systemId, clampedLevel, store, false);
        }

        refreshClassStatsIfNeeded(entityRef, systemId, store);

        System.out.println("[LevelSystem] Set " + systemId + " level to " + clampedLevel);
    }

    private void unlockClassAbilitiesForLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String systemId,
            int level,
            @Nonnull Store<EntityStore> store,
            boolean exactLevel) {
        var classSystem = ExamplePlugin.get().getClassManagementSystem();
        java.util.List<String> classIds = classSystem.getClassesForLevelSystem(systemId);
        if (classIds.isEmpty()) {
            return;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null) {
            return;
        }

        for (String classId : classIds) {
            if (!classComp.hasLearnedClass(classId)) {
                continue;
            }

            ClassDefinition classDef = classSystem.getClassDefinition(classId);
            if (classDef == null) {
                continue;
            }

            java.util.List<ClassDefinition.AbilityUnlock> unlocks = exactLevel
                    ? classDef.getAbilityUnlocksExactLevel(level)
                    : classDef.getAbilityUnlocksAtLevel(level);

            for (ClassDefinition.AbilityUnlock unlock : unlocks) {
                String abilityId = unlock.getAbilityId();
                if (abilityId == null || abilityId.isEmpty()) {
                    continue;
                }

                if (!classComp.hasUnlockedSpell(abilityId)) {
                    int rank = unlock.getMaxRank() > 0 ? 1 : 1;
                    classComp.unlockSpell(abilityId, rank);
                }
            }
        }
    }

    /**
     * Add multiple levels at once (grants rewards for each level)
     * 
     * @param levelsToAdd Number of levels to add (can be negative to remove)
     */
    public void addLevels(@Nonnull Ref<EntityStore> entityRef, String systemId, int levelsToAdd,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        if (levelsToAdd == 0) {
            return;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null) {
            System.err.println("[LevelSystem] Cannot add levels for unknown system: " + systemId);
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);
        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        int currentLevel = levelData.getCurrentLevel();
        int newLevel = currentLevel + levelsToAdd;

        // Clamp to valid range
        int maxLevel = getEffectiveMaxLevel(config);
        newLevel = Math.max(1, Math.min(newLevel, maxLevel));

        if (newLevel > currentLevel) {
            // Leveling up - grant rewards
            processLevelUps(entityRef, systemId, currentLevel, newLevel, levelData, config, store, world);
        }

        levelData.setCurrentLevel(newLevel);
        levelData.setCurrentExp(0);
        updateExpToNextLevel(levelData, config);

        refreshClassStatsIfNeeded(entityRef, systemId, store);
    }

    /**
     * Set experience directly (may trigger level-ups if enough exp)
     */
    public void setExperience(@Nonnull Ref<EntityStore> entityRef, String systemId, float experience,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null) {
            System.err.println("[LevelSystem] Cannot set experience for unknown system: " + systemId);
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);
        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        int currentLevel = levelData.getCurrentLevel();
        float clampedExp = Math.max(0, experience);
        levelData.setCurrentExp(clampedExp);

        // Manually check for level ups
        int newLevel = currentLevel;
        float remainingExp = clampedExp;
        int maxLevel = getEffectiveMaxLevel(config);

        while (remainingExp >= levelData.getExpToNextLevel() && levelData.getExpToNextLevel() > 0
                && newLevel < maxLevel) {
            remainingExp -= levelData.getExpToNextLevel();
            newLevel++;
            // Update exp requirement for next level
            if (newLevel < maxLevel) {
                levelData.setExpToNextLevel(config.calculateExpForLevel(newLevel + 1));
            }
        }

        if (newLevel > currentLevel) {
            levelData.setCurrentLevel(newLevel);
            levelData.setCurrentExp(remainingExp);
            processLevelUps(entityRef, systemId, currentLevel, newLevel, levelData, config, store, world);
        }

        updateExpToNextLevel(levelData, config);

        refreshClassStatsIfNeeded(entityRef, systemId, store);
    }

    /**
     * Remove experience (for death penalties, etc.)
     * 
     * @param amount Amount of experience to remove
     * @return Actual amount removed (may be less if not enough exp)
     */
    public float removeExperience(@Nonnull Ref<EntityStore> entityRef, String systemId, float amount) {
        if (systemId == null || systemId.isEmpty()) {
            return 0f;
        }

        if (amount <= 0f) {
            return 0f;
        }

        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return 0f;

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getSystem(systemId);
        if (levelData == null)
            return 0f;

        float currentExp = levelData.getCurrentExp();
        float amountToRemove = Math.min(amount, currentExp);

        levelData.setCurrentExp(currentExp - amountToRemove);
        return amountToRemove;
    }

    /**
     * Spend stat points (returns true if successful)
     */
    public boolean spendStatPoints(@Nonnull Ref<EntityStore> entityRef, String systemId, int amount) {
        if (systemId == null || systemId.isEmpty()) {
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return false;

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getSystem(systemId);
        if (levelData == null || levelData.getAvailableStatPoints() < amount)
            return false;

        levelData.setAvailableStatPoints(levelData.getAvailableStatPoints() - amount);
        return true;
    }

    /**
     * Spend skill points (returns true if successful)
     */
    public boolean spendSkillPoints(@Nonnull Ref<EntityStore> entityRef, String systemId, int amount) {
        if (systemId == null || systemId.isEmpty()) {
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null)
            return false;

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getSystem(systemId);
        if (levelData == null || levelData.getAvailableSkillPoints() < amount)
            return false;

        levelData.setAvailableSkillPoints(levelData.getAvailableSkillPoints() - amount);
        return true;
    }

    // === Bulk Operations ===

    /**
     * Grant experience to multiple entities at once (e.g., party members)
     */
    public void grantExperienceToGroup(@Nonnull ObjectList<Ref<EntityStore>> entityRefs, String systemId,
            float amount, String source,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> entityRef : entityRefs) {
            if (entityRef.isValid()) {
                grantExperience(entityRef, systemId, amount, source, store, world);
            }
        }
    }

    /**
     * Grant experience to all entities within a radius (e.g., area rewards)
     * Note: position parameter should match the type used by
     * TransformComponent.getPosition()
     */
    public void grantExperienceInRadius(@Nonnull com.hypixel.hytale.math.vector.Vector3d position, double radius,
            String systemId, float amount, String source,
            @Nonnull Store<EntityStore> store, @Nonnull World world) {
        if (systemId == null || systemId.isEmpty()) {
            return;
        }

        // Get spatial resource for players
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = store
                .getResource(EntityModule.get().getPlayerSpatialResourceType());

        ObjectList<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(position, radius, nearbyPlayers);

        for (Ref<EntityStore> playerRef : nearbyPlayers) {
            if (playerRef.isValid()) {
                grantExperience(playerRef, systemId, amount, source, store, world);
            }
        }
    }

    // === Utility Methods ===

    /**
     * Calculate the experience required for a specific level
     * 
     * @return Experience required, or -1 if system not found
     */
    public float calculateExpForLevel(String systemId, int level) {
        if (systemId == null || systemId.isEmpty()) {
            return -1f;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null)
            return -1f;

        return config.calculateExpForLevel(level);
    }

    /**
     * Calculate what level a given amount of experience corresponds to
     * 
     * @return Level that would be reached with this exp
     */
    public int getLevelFromExp(String systemId, float totalExp) {
        if (systemId == null || systemId.isEmpty()) {
            return 0;
        }

        LevelSystemConfig config = levelSystemConfigs.get(systemId);
        if (config == null)
            return 0;

        int level = config.getStartingLevel();
        int maxLevel = getEffectiveMaxLevel(config);

        float expAccumulated = 0f;
        while (level < maxLevel) {
            float expForNextLevel = config.calculateExpForLevel(level + 1);
            if (expAccumulated + expForNextLevel > totalExp) {
                break;
            }
            expAccumulated += expForNextLevel;
            level++;
        }

        return level;
    }

    /**
     * Get all registered level system IDs
     */
    public java.util.Set<String> getRegisteredSystems() {
        return levelSystemConfigs.keySet();
    }

    // === Helper methods (adapted to Hytale's ECS) ===

    @Nonnull
    private LevelProgressionComponent getOrCreateComponent(@Nonnull Ref<EntityStore> entityRef) {
        LevelProgressionComponent component = entityRef.getStore().getComponent(entityRef,
                LevelProgressionComponent.getComponentType());
        if (component == null) {
            component = entityRef.getStore().addComponent(entityRef, LevelProgressionComponent.getComponentType());
        }
        return component;
    }

    private void ensureExpToNextLevel(@Nonnull LevelProgressionComponent.LevelSystemData levelData,
            @Nonnull LevelSystemConfig config) {
        if (levelData.getExpToNextLevel() <= 0 && (config.getMaxLevel() <= 0
                || levelData.getCurrentLevel() < config.getMaxLevel())) {
            updateExpToNextLevel(levelData, config);
        }
    }

    private int getEffectiveMaxLevel(@Nonnull LevelSystemConfig config) {
        return config.getMaxLevel() > 0 ? config.getMaxLevel() : 999;
    }

    private void refreshClassStatsIfNeeded(@Nonnull Ref<EntityStore> entityRef,
            String systemId,
            @Nullable Store<EntityStore> store) {
        if (store == null || statSystem == null || systemId == null || systemId.isEmpty()) {
            return;
        }

        var classSystem = ExamplePlugin.get().getClassManagementSystem();
        if (classSystem.getClassesForLevelSystem(systemId).isEmpty()) {
            return;
        }

        statSystem.recalculateClassStatBonuses(entityRef, store);
    }

    @Nullable
    private LevelProgressionComponent getComponent(@Nonnull Ref<EntityStore> entityRef) {
        return entityRef.getStore().getComponent(entityRef, LevelProgressionComponent.getComponentType());
    }

    /**
     * Trigger level up effects (sound, particles, notification)
     */
    private void triggerLevelUpEffects(@Nonnull Ref<EntityStore> entityRef, String systemId, int newLevel,
            @Nonnull LevelSystemConfig.LevelUpEffects effects,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (store == null || world == null) {
            return; // Can't trigger effects without store/world
        }

        // Check if entity is a player
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return; // Only trigger effects for players
        }

        // Get player component
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Play sound effect
        if (effects.getSoundId() != null) {
            playSoundEffect(entityRef, effects.getSoundId(), store, world);
        }

        // Spawn particle effect
        if (effects.getParticleEffect() != null) {
            spawnParticleEffect(entityRef, effects.getParticleEffect(), store, world);
        }

        // Send notification
        if (effects.getNotification() != null) {
            sendLevelUpNotification(playerRef, systemId, newLevel, effects.getNotification());
        }

        // Show event title popup
        if (effects.getEventTitle() != null) {
            showLevelUpEventTitle(playerRef, systemId, newLevel, effects.getEventTitle());
        }
    }

    /**
     * Play sound effect at entity location
     */
    private void playSoundEffect(@Nonnull Ref<EntityStore> entityRef, String soundId,
            @Nonnull Store<EntityStore> store, @Nonnull World world) {
        try {
            // Get sound index from asset map
            int soundIndex = SoundEvent.getAssetMap().getIndex(soundId);
            if (soundIndex == 0 || soundIndex == Integer.MIN_VALUE) {
                System.err.println("[LevelSystem] Sound asset not found in game files: '" + soundId + "'");
                System.err.println(
                        "[LevelSystem] Tip: Check assets/Common/Sounds/ or assets/Server/Audio/ for valid sound IDs");
                return;
            }

            // Get entity transform for position
            TransformComponent transform = store.getComponent(entityRef,
                    EntityModule.get().getTransformComponentType());
            if (transform == null) {
                System.err.println("[LevelSystem] No transform component for sound playback");
                return;
            }

            // Play sound in world context - using 3D positional sound
            world.execute(() -> {
                try {
                    SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.UI,
                            transform.getPosition(), store);
                    System.out.println("[LevelSystem] Played sound: " + soundId + " (index: " + soundIndex + ")");
                } catch (Exception e) {
                    System.err.println("[LevelSystem] Failed to play sound in world.execute: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("[LevelSystem] Failed to play sound " + soundId + ": " + e.getMessage());
            System.err.println("[LevelSystem] " + e);
        }
    }

    /**
     * Spawn particle effect attached to entity (follows the player)
     */
    private void spawnParticleEffect(@Nonnull Ref<EntityStore> entityRef, String particleEffect,
            @Nonnull Store<EntityStore> store, @Nonnull World world) {
        try {
            // Get entity's network ID
            NetworkId networkId = store.getComponent(entityRef, NetworkId.getComponentType());
            if (networkId == null) {
                System.err.println("[LevelSystem] No network ID component for particle effect");
                return;
            }
            int entityNetworkId = networkId.getId();

            // Get entity transform for position (for spatial query)
            TransformComponent transform = store.getComponent(entityRef,
                    EntityModule.get().getTransformComponentType());
            if (transform == null) {
                System.err.println("[LevelSystem] No transform component for particle effect");
                return;
            }

            // Create ModelParticle that attaches to the entity
            ModelParticle modelParticle = new ModelParticle(
                    particleEffect, // systemId
                    EntityPart.Self, // targetEntityPart - attach to entity itself
                    null, // targetNodeName - no specific node
                    null, // color - use default
                    1.0f, // scale
                    null, // positionOffset - no offset
                    null, // rotationOffset - no rotation
                    false // detachedFromModel - FALSE means it follows the entity!
            );

            // Convert to protocol packet format
            com.hypixel.hytale.protocol.ModelParticle[] modelParticlesProtocol = new com.hypixel.hytale.protocol.ModelParticle[] {
                    modelParticle.toPacket() };

            // Create packet to spawn particles attached to the entity
            SpawnModelParticles packet = new SpawnModelParticles(entityNetworkId, modelParticlesProtocol);

            // Spawn particle effect in world context
            world.execute(() -> {
                try {
                    // Get all nearby players to send the packet to
                    SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = store
                            .getResource(EntityModule.get().getPlayerSpatialResourceType());
                    ObjectList<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
                    playerSpatialResource.getSpatialStructure().collect(transform.getPosition(), 75.0, nearbyPlayers);

                    // Send packet to all nearby players
                    for (Ref<EntityStore> playerRef : nearbyPlayers) {
                        if (playerRef.isValid()) {
                            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
                            if (playerRefComponent != null) {
                                playerRefComponent.getPacketHandler().writeNoCache(packet);
                            }
                        }
                    }

                    System.out.println("[LevelSystem] Spawned attached particle effect: " + particleEffect);
                } catch (Exception e) {
                    System.err.println("[LevelSystem] Failed to spawn particle in world.execute: " + e.getMessage());
                    System.err.println("[LevelSystem] " + e);
                }
            });
        } catch (Exception e) {
            System.err.println("[LevelSystem] Failed to spawn particle " + particleEffect + ": " + e.getMessage());
            System.err.println("[LevelSystem] " + e);
        }
    }

    /**
     * Send level up notification banner to player
     */
    private void sendLevelUpNotification(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull LevelSystemConfig.NotificationConfig notifConfig) {
        try {
            // Get packet handler
            PacketHandler packetHandler = playerRef.getPacketHandler();

            // Replace placeholders in primary message
            String primaryText = notifConfig.getPrimaryMessage()
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{system}", systemId);
            Message primaryMessage = Message.raw(primaryText);

            // Replace placeholders in secondary message
            String secondaryText = null;
            Message secondaryMessage = null;
            if (notifConfig.getSecondaryMessage() != null) {
                secondaryText = notifConfig.getSecondaryMessage()
                        .replace("{level}", String.valueOf(newLevel))
                        .replace("{system}", systemId);
                secondaryMessage = Message.raw(secondaryText);
            }

            // Create item icon if specified
            ItemWithAllMetadata icon = null;
            if (notifConfig.getIconItemId() != null) {
                try {
                    ItemStack itemStack = new ItemStack(notifConfig.getIconItemId(), 1);
                    icon = (ItemWithAllMetadata) itemStack.toPacket();
                } catch (Exception e) {
                    System.err.println("[LevelSystem] Failed to create icon item " + notifConfig.getIconItemId() + ": "
                            + e.getMessage());
                }
            }

            // Send notification with default style
            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    null, // icon string (we're using item instead)
                    icon,
                    NotificationStyle.Default);

            // Build debug log with replaced text
            String debugMessage = primaryText;
            if (secondaryMessage != null) {
                debugMessage += " - " + secondaryText;
            }
            System.out.println("[LevelSystem] Sent notification: " + debugMessage);
        } catch (Exception e) {
            System.err.println("[LevelSystem] Failed to send notification: " + e.getMessage());
            System.err.println("[LevelSystem] " + e);
        }
    }

    /**
     * Show event title popup to player
     */
    private void showLevelUpEventTitle(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull LevelSystemConfig.EventTitleConfig titleConfig) {
        try {
            String primaryText = titleConfig.getPrimaryMessage()
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{system}", systemId);
            String secondaryText = titleConfig.getSecondaryMessage() == null ? ""
                    : titleConfig.getSecondaryMessage()
                            .replace("{level}", String.valueOf(newLevel))
                            .replace("{system}", systemId);

            Message primary = Message.raw(primaryText);
            Message secondary = Message.raw(secondaryText);

            EventTitleUtil.showEventTitleToPlayer(playerRef, primary, secondary, titleConfig.isMajor());
        } catch (Exception e) {
            System.err.println("[LevelSystem] Failed to show event title: " + e.getMessage());
            System.err.println("[LevelSystem] " + e);
        }
    }
}
