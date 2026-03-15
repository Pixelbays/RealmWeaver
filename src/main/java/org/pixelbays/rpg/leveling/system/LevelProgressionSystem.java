package org.pixelbays.rpg.leveling.system;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.EventTitleConfig;
import org.pixelbays.rpg.leveling.config.LevelRewardConfig;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.config.LevelUpEffects;
import org.pixelbays.rpg.leveling.config.NotificationConfig;
import org.pixelbays.rpg.leveling.event.LevelRewardsAppliedEvent;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.KillFeedMessage;
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

    private StatSystem statSystem;

    public LevelProgressionSystem(@Nonnull EventRegistry eventRegistry) {
        // Level system configs are loaded via Hytale's asset system
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    /**
     * Note: Level system configurations are now loaded automatically via Hytale's asset system.
     * No manual loading is required - configs are accessed directly from the asset store.
     */

    /**
     * Grant experience to an entity in a specific level system
     * 
     * @param entityRef The entity reference gaining exp
     * @param systemId  The level system ID (e.g., "Base_Character_Level")
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

        // Get level system config (from asset store for latest changes)
        LevelSystemConfig config = getConfig(systemId);
        if (config == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Unknown level system: %s", systemId);
            return;
        }

        if (!config.isEnabled()) {
            return; // System disabled
        }

        if (source == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Grant exp source is null: systemId=%s amount=%s entity=%s",
                    systemId, amount, entityRef);
        }

        // If world is provided, wrap all component modifications in world.execute() for thread safety
        // This ensures that entity component modifications happen on the correct world thread
        if (world != null) {
            world.execute(() -> grantExperienceInternal(entityRef, systemId, amount, source, config, store, world));
        } else {
            // No world provided - assume we're already on the correct thread (e.g., from commands)
            grantExperienceInternal(entityRef, systemId, amount, source, config, store, null);
        }
    }

    /**
     * Internal implementation of experience granting with component modifications.
     * This should only be called from grantExperience() which handles thread safety.
     */
    private void grantExperienceInternal(@Nonnull Ref<EntityStore> entityRef, String systemId, float amount, 
            String source, @Nonnull LevelSystemConfig config, @Nullable Store<EntityStore> store, 
            @Nullable World world) {
        RpgLogging.debugDeveloper("[LevelSystem] Grant exp: systemId=%s amount=%s source=%s entity=%s",
            systemId, amount, source, entityRef);
        
        // Get or create level progression component
        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);
        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        ensureExpToNextLevel(levelData, config);

        // Check prerequisites
        if (!checkPrerequisites(levelComp, config)) {
            RpgLogging.debugDeveloper("[LevelSystem] Prerequisites not met for %s", systemId);
            return; // Prerequisites not met
        }

        // Check if at max level
        if (config.getMaxLevel() > 0 && levelData.getCurrentLevel() >= config.getMaxLevel()) {
            return; // Already at max level
        }

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

            applyLevelRewards(entityRef, systemId, currentLevel, levelData, config, store, world);

            if (store != null) {
                unlockClassAbilitiesForLevel(entityRef, systemId, currentLevel, store, true);
            }
        }

        refreshClassStatsIfNeeded(entityRef, systemId, store);

        LevelUpEvent.dispatch(entityRef, systemId, startLevel, endLevel);

        RpgLogging.debugDeveloper("[LevelSystem] Entity leveled up %s: %d -> %d", systemId,
                startLevel, endLevel);
    }

    /**
     * Apply level-up rewards
     */
    private void applyLevelRewards(@Nonnull Ref<EntityStore> entityRef, String systemId, int newLevel,
            LevelProgressionComponent.LevelSystemData levelData, LevelSystemConfig config,
            @Nullable Store<EntityStore> store, @Nullable World world) {

        RpgLogging.debugDeveloper("[LevelSystem] applyLevelRewards: systemId=%s, newLevel=%d", systemId, newLevel);
        RpgLogging.debugDeveloper("[LevelSystem] Config details: id=%s, displayName=%s, parent=%s",
                config.getId(), config.getDisplayName(), config.getParent());

        // Debug: Check if parent config exists
        if (config.getParent() != null && !config.getParent().isEmpty()) {
            LevelSystemConfig parentConfig = getConfig(config.getParent());
            if (parentConfig != null) {
                RpgLogging.debugDeveloper("[LevelSystem] Parent config found: %s", parentConfig.getId());
                if (parentConfig.getDefaultRewards() != null) {
                    RpgLogging.debugDeveloper("[LevelSystem] Parent has DefaultRewards: statPoints=%d, skillPoints=%d",
                            parentConfig.getDefaultRewards().getStatPoints(),
                            parentConfig.getDefaultRewards().getSkillPoints());
                } else {
                    RpgLogging.debugDeveloper("[LevelSystem] Parent has NULL DefaultRewards");
                }
            } else {
                RpgLogging.debugDeveloper("[LevelSystem] Parent config NOT FOUND: %s", config.getParent());
            }
        }

        // Debug: Check what reward levels are available
        if (config.getLevelRewards() != null) {
            RpgLogging.debugDeveloper("[LevelSystem] Config has %d reward entries: %s",
                    config.getLevelRewards().size(), config.getLevelRewards().keySet());
        } else {
            RpgLogging.debugDeveloper("[LevelSystem] Config has NULL rewards map");
        }

        // Debug: Check default rewards
        if (config.getDefaultRewards() != null) {
            RpgLogging.debugDeveloper("[LevelSystem] Config has DefaultRewards: statPoints=%d, skillPoints=%d",
                    config.getDefaultRewards().getStatPoints(), config.getDefaultRewards().getSkillPoints());
        } else {
            RpgLogging.debugDeveloper("[LevelSystem] Config has NULL DefaultRewards");
        }

        LevelRewardConfig rewards = config.getRewardsForLevel(newLevel);
        if (rewards == null) {
            RpgLogging.debugDeveloper("[LevelSystem] No rewards found for level %d in system %s", newLevel, systemId);
            RpgLogging.debugDeveloper("[LevelSystem] Checked config: %s (parent: %s)",
                    config.getId(), config.getParent());

            if (rewards == null) {
                return;
            }
        }

        RpgLogging.debugDeveloper("[LevelSystem] Rewards found: statPoints=%d, skillPoints=%d",
                rewards.getStatPoints(), rewards.getSkillPoints());

        // Apply direct stat increases from rewards
        if (statSystem != null && store != null && rewards.getStatIncreases() != null) {
            statSystem.applyStatIncreases(entityRef, rewards.getStatIncreases(), store);
        }

        // Apply stat growth for this level (flat, percentage, milestone)
        if (statSystem != null && store != null && config.getStatGrowth() != null) {
            statSystem.applyStatGrowth(entityRef, newLevel, config.getStatGrowth(), store);
        }

        // Grant stat points
        if (rewards.getStatPoints() > 0) {
            int beforeStatPoints = levelData.getAvailableStatPoints();
            levelData.addStatPoints(rewards.getStatPoints());
            RpgLogging.debugDeveloper("[LevelSystem] Granted %d stat points (before=%d, after=%d)",
                    rewards.getStatPoints(), beforeStatPoints, levelData.getAvailableStatPoints());
        }

        // Grant skill points
        if (rewards.getSkillPoints() > 0) {
            int beforeSkillPoints = levelData.getAvailableSkillPoints();
            levelData.addSkillPoints(rewards.getSkillPoints());
            RpgLogging.debugDeveloper("[LevelSystem] Granted %d skill points (before=%d, after=%d)",
                    rewards.getSkillPoints(), beforeSkillPoints, levelData.getAvailableSkillPoints());
        }

        // Grant currency rewards
        if (rewards.getCurrencyRewards() != null && !rewards.getCurrencyRewards().isEmpty() && store != null) {
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                CurrencyManager currencyManager = ExamplePlugin.get().getCurrencyManager();
                for (Map.Entry<String, Long> entry : rewards.getCurrencyRewards().entrySet()) {
                    String currencyId = entry.getKey();
                    Long amount = entry.getValue();
                    if (currencyId == null || currencyId.isBlank() || amount == null || amount <= 0L) {
                        continue;
                    }

                    var result = currencyManager.addBalance(CurrencyScope.Character, playerRef.getUuid().toString(),
                            currencyId, amount.longValue());
                    RpgLogging.debugDeveloper(
                            "[LevelSystem] Granted currency reward: level=%d system=%s currency=%s amount=%d success=%s",
                            newLevel, systemId, currencyId, amount, result.isSuccess());
                }
            } else {
                RpgLogging.debugDeveloper(
                        "[LevelSystem] Skipped currency rewards for non-player entity: level=%d system=%s",
                        newLevel, systemId);
            }
        }

        // Apply stat increases
        if (store != null && statSystem != null
                && rewards.getStatIncreases() != null && !rewards.getStatIncreases().isEmpty()) {
            RpgLogging.debugDeveloper("[LevelSystem] Applying stat increases: %s", rewards.getStatIncreases().keySet());
            statSystem.applyStatIncreases(entityRef, rewards.getStatIncreases(), store);
        } else {
            RpgLogging.debugDeveloper(
                    "[LevelSystem] No stat increases to apply (store=%s, statSystem=%s, statIncreases=%s)",
                    store != null, statSystem != null,
                    rewards.getStatIncreases() != null ? rewards.getStatIncreases().isEmpty() : "null");
        }

        // Apply stat growth from config
        if (store != null && statSystem != null && config.getStatGrowth() != null) {
            RpgLogging.debugDeveloper("[LevelSystem] Applying stat growth from config for level %d", newLevel);
            statSystem.applyStatGrowth(entityRef, newLevel, config.getStatGrowth(), store);
            levelData.setLastGrowthAppliedLevel(newLevel);
        } else {
            RpgLogging.debugDeveloper("[LevelSystem] No stat growth to apply (store=%s, statSystem=%s, statGrowth=%s)",
                    store != null, statSystem != null, config.getStatGrowth() != null);
        }

        // Trigger level up effects (sound, particles, notification)
        if (rewards.getLevelUpEffects() != null && !rewards.getLevelUpEffects().isEmpty()) {
            RpgLogging.debugDeveloper("[LevelSystem] Triggering level-up effects");
            triggerLevelUpEffects(entityRef, systemId, newLevel, rewards.getLevelUpEffects(), store, world);
        } else {
            RpgLogging.debugDeveloper("[LevelSystem] No level-up effects to trigger");
        }

        RpgLogging.debugDeveloper("[LevelSystem] applyLevelRewards complete for level %d", newLevel);

        LevelRewardsAppliedEvent.dispatch(entityRef, systemId, newLevel, rewards);

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

        LevelSystemConfig config = getConfig(systemId);
        if (config == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Cannot initialize unknown system: %s", systemId);
            return;
        }

        if (!config.isEnabled()) {
            return;
        }

        LevelProgressionComponent levelComp = getOrCreateComponent(entityRef);

        // Only initialize if the system doesn't already exist
        if (levelComp.hasSystem(systemId)) {
            RpgLogging.debugDeveloper("[LevelSystem] System %s already exists, skipping initialization", systemId);
            return;
        }

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);

        // Set starting level
        levelData.setCurrentLevel(config.getStartingLevel());
        levelData.setCurrentExp(0);

        // Calculate exp to next level
        updateExpToNextLevel(levelData, config);

        RpgLogging.debugDeveloper("[LevelSystem] Initialized %s for entity at level %s", systemId,
            config.getStartingLevel());
    }

    /**
     * Get level system configuration from asset store (always fetches latest)
     */
    public LevelSystemConfig getConfig(String systemId) {
        if (systemId == null || systemId.isEmpty()) {
            return null;
        }
        // Fetch from asset store to get latest changes
        return LevelSystemConfig.getAssetMap().getAsset(systemId);
    }

    /**
     * Check if entity can access a level system (prerequisites met)
     */
    public boolean canAccessSystem(@Nonnull Ref<EntityStore> entityRef, String systemId) {
        LevelSystemConfig config = getConfig(systemId);
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

        LevelSystemConfig config = getConfig(systemId);
        if (config == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Cannot set level for unknown system: %s", systemId);
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

        RpgLogging.debugDeveloper("[LevelSystem] Set %s level to %s", systemId, clampedLevel);
    }

    private void unlockClassAbilitiesForLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String systemId,
            int level,
            @Nonnull Store<EntityStore> store,
            boolean exactLevel) {
        if (!shouldAutoLearnClassAbilitiesOnLevelUp()) {
            return;
        }

        var classSystem = ExamplePlugin.get().getClassManagementSystem();
        java.util.List<String> classIds = classSystem.getClassesForLevelSystem(systemId);
        if (classIds.isEmpty()) {
            return;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null) {
            return;
        }

        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp == null) {
            abilityComp = store.addComponent(entityRef, ClassAbilityComponent.getComponentType());
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

                if (!abilityComp.hasAbility(abilityId)) {
                    int rank = unlock.getMaxRank() > 0 ? 1 : 1;
                    abilityComp.unlockAbility(abilityId, classId, rank);
                    ClassAbilityUnlockedEvent.dispatch(entityRef, classId, abilityId, rank, systemId, exactLevel);
                }
            }
        }
    }

    private boolean shouldAutoLearnClassAbilitiesOnLevelUp() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return true;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config == null && !assetMap.getAssetMap().isEmpty()) {
            config = assetMap.getAssetMap().values().iterator().next();
        }

        return config == null || config.shouldAutoLearnClassAbilitiesOnLevelUp();
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

        LevelSystemConfig config = getConfig(systemId);
        if (config == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Cannot add levels for unknown system: %s", systemId);
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

        LevelSystemConfig config = getConfig(systemId);
        if (config == null) {
            RpgLogging.debugDeveloper("[LevelSystem] Cannot set experience for unknown system: %s", systemId);
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

    /**
     * Refund skill points back to a level system (e.g., on talent reset).
     * Returns true if successful.
     */
    public boolean refundSkillPoints(@Nonnull Ref<EntityStore> entityRef, String systemId, int amount) {
        if (systemId == null || systemId.isEmpty() || amount <= 0) {
            return false;
        }

        LevelProgressionComponent levelComp = getComponent(entityRef);
        if (levelComp == null) return false;

        LevelProgressionComponent.LevelSystemData levelData = levelComp.getOrCreateSystem(systemId);
        if (levelData == null) return false;

        levelData.setAvailableSkillPoints(levelData.getAvailableSkillPoints() + amount);
        RpgLogging.debugDeveloper("[LevelSystem] Refunded %d skill points to %s system (now %d)",
                amount, systemId, levelData.getAvailableSkillPoints());
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

        LevelSystemConfig config = getConfig(systemId);
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

        LevelSystemConfig config = getConfig(systemId);
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
        return LevelSystemConfig.getAssetMap().getAssetMap().keySet();
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
            @Nonnull LevelUpEffects effects,
            @Nullable Store<EntityStore> store, @Nullable World world) {
        if (store == null) {
            return; // Can't trigger effects without store
        }

        World effectiveWorld = world != null ? world : store.getExternalData().getWorld();

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
        if (effectiveWorld != null) {
            // Play sound effect
            if (effects.getSoundId() != null) {
                playSoundEffect(entityRef, effects.getSoundId(), store, effectiveWorld);
            }

            // Spawn particle effect
            if (effects.getParticleEffect() != null) {
                spawnParticleEffect(entityRef, effects.getParticleEffect(), store, effectiveWorld);
            }
        }

        // Send notification
        if (effects.getNotification() != null) {
            sendLevelUpNotification(playerRef, systemId, newLevel, effects.getNotification());
        }

        // Show event title popup
        if (effects.getEventTitle() != null) {
            showLevelUpEventTitle(playerRef, systemId, newLevel, effects.getEventTitle());
        }

        // Send chat message
        if (effects.getChatMessage() != null) {
            sendLevelUpChatMessage(playerRef, systemId, newLevel, effects.getChatMessage());
        }

        // Send kill feed popup
        if (effects.getKillFeedPopup() != null) {
            sendLevelUpKillFeedPopup(playerRef, systemId, newLevel, effects.getKillFeedPopup());
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
                RpgLogging.debugDeveloper("[LevelSystem] Sound asset not found in game files: '%s'", soundId);
                RpgLogging.debugDeveloper(
                    "[LevelSystem] Tip: Check assets/Common/Sounds/ or assets/Server/Audio/ for valid sound IDs");
                return;
            }

            // Get entity transform for position
            TransformComponent transform = store.getComponent(entityRef,
                    EntityModule.get().getTransformComponentType());
            if (transform == null) {
                RpgLogging.debugDeveloper("[LevelSystem] No transform component for sound playback");
                return;
            }

            // Play sound in world context - using 3D positional sound
            world.execute(() -> {
                try {
                    SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.UI,
                            transform.getPosition(), store);
                    RpgLogging.debugDeveloper("[LevelSystem] Played sound: %s (index: %s)", soundId, soundIndex);
                } catch (Exception e) {
                    RpgLogging.debugDeveloper("[LevelSystem] Failed to play sound in world.execute: %s", e.getMessage());
                }
            });
        } catch (Exception e) {
            RpgLogging.debugDeveloper("[LevelSystem] Failed to play sound %s: %s", soundId, e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
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
                RpgLogging.debugDeveloper("[LevelSystem] No network ID component for particle effect");
                return;
            }
            int entityNetworkId = networkId.getId();

            // Get entity transform for position (for spatial query)
            TransformComponent transform = store.getComponent(entityRef,
                    EntityModule.get().getTransformComponentType());
            if (transform == null) {
                RpgLogging.debugDeveloper("[LevelSystem] No transform component for particle effect");
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

                    RpgLogging.debugDeveloper("[LevelSystem] Spawned attached particle effect: %s", particleEffect);
                } catch (Exception e) {
                    RpgLogging.debugDeveloper("[LevelSystem] Failed to spawn particle in world.execute: %s", e.getMessage());
                    RpgLogging.debugDeveloper("[LevelSystem] %s", e);
                }
            });
        } catch (Exception e) {
            RpgLogging.debugDeveloper("[LevelSystem] Failed to spawn particle %s: %s", particleEffect, e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
        }
    }

    /**
     * Send level up notification banner to player
     */
    private void sendLevelUpNotification(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull NotificationConfig notifConfig) {
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
                    RpgLogging.debugDeveloper("[LevelSystem] Failed to create icon item %s: %s",
                            notifConfig.getIconItemId(),
                            e.getMessage());
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
            RpgLogging.debugDeveloper("[LevelSystem] Sent notification: %s", debugMessage);
        } catch (Exception e) {
            RpgLogging.debugDeveloper("[LevelSystem] Failed to send notification: %s", e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
        }
    }

    /**
     * Show event title popup to player
     */
    private void showLevelUpEventTitle(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull EventTitleConfig titleConfig) {
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
            RpgLogging.debugDeveloper("[LevelSystem] Failed to show event title: %s", e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
        }
    }

    /**
     * Send chat message to player on level up
     */
    private void sendLevelUpChatMessage(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull String chatMessageTemplate) {
        try {
            // Replace placeholders in chat message
            String messageText = chatMessageTemplate
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{system}", systemId);

                // Send to player via server message API
                playerRef.sendMessage(Message.raw(messageText));

            RpgLogging.debugDeveloper("[LevelSystem] Sent chat message: %s", messageText);
        } catch (Exception e) {
            RpgLogging.debugDeveloper("[LevelSystem] Failed to send chat message: %s", e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
        }
    }

    /**
     * Send kill feed popup to player on level up
     */
    private void sendLevelUpKillFeedPopup(@Nonnull PlayerRef playerRef, String systemId, int newLevel,
            @Nonnull String killFeedMessageTemplate) {
        try {
            // Replace placeholders in kill feed message
            String messageText = killFeedMessageTemplate
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{system}", systemId);

            // Create formatted message for kill feed
            Message message = Message.raw(messageText);
            
            // Send kill feed message packet directly (same approach as DeathSystems.java)
            // killer = null (no killer message for level up)
            // decedent = our level up message (shows in kill feed)
            // icon = Icon_LevelUp (shows a level-up icon next to the message)
            KillFeedMessage killFeedPacket = new KillFeedMessage(
                    null, // killer message
                    message.getFormattedMessage(), // decedent/main message
                    "Icon_LevelUp"  // icon
            );

            // Send packet to player
            playerRef.getPacketHandler().write(killFeedPacket);

            RpgLogging.debugDeveloper("[LevelSystem] Sent kill feed popup: %s", messageText);
        } catch (Exception e) {
            RpgLogging.debugDeveloper("[LevelSystem] Failed to send kill feed popup: %s", e.getMessage());
            RpgLogging.debugDeveloper("[LevelSystem] %s", e);
        }
    }
}
