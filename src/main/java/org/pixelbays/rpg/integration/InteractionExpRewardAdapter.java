package org.pixelbays.rpg.integration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.pixelbays.rpg.system.LevelProgressionSystem;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Integration adapter for Hytale's Interaction System.
 * This allows interactions to grant experience through the exp_rewards field.
 * 
 * Example usage in an interaction JSON:
 * 
 * {
 *   "type": "kill_entity",
 *   "entity": "hytale:zombie",
 *   "exp_rewards": {
 *     "character_level": 100,
 *     "class_warrior": 50,
 *     "profession_combat": 25
 *   }
 * }
 */
public class InteractionExpRewardAdapter {
    
    private final LevelProgressionSystem levelSystem;
    
    public InteractionExpRewardAdapter(LevelProgressionSystem levelSystem) {
        this.levelSystem = levelSystem;
    }
    
    /**
     * Process exp rewards from an interaction.
     * Call this from your interaction handler after the interaction succeeds.
     * 
     * @param entityRef The entity reference receiving exp
     * @param expRewards Map of system_id -> exp_amount
     * @param source Source identifier for the exp (e.g., "kill_zombie", "quest_complete")
     */
    public void processExpRewards(@Nonnull Ref<EntityStore> entityRef, Map<String, Float> expRewards, String source) {
        if (expRewards == null || expRewards.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Float> reward : expRewards.entrySet()) {
            String systemId = reward.getKey();
            float expAmount = reward.getValue();
            
            // Pass null for store and world since we don't have them here
            // Effects won't trigger, but exp will still be granted
            levelSystem.grantExperience(entityRef, systemId, expAmount, source, null, null);
        }
    }
    
    /**
     * Example: Integration with entity death/kill events
     */
    public void onEntityKilled(@Nonnull Ref<EntityStore> killerRef, @Nonnull Ref<EntityStore> victimRef, String victimType) {
        // Load victim's exp reward configuration
        // This could be stored in the entity's definition or a separate config
        Map<String, Float> expRewards = loadExpRewardsForEntity(victimType);
        
        if (expRewards != null && !expRewards.isEmpty()) {
            processExpRewards(killerRef, expRewards, "kill_" + victimType);
        }
    }
    
    /**
     * Example: Integration with quest completion
     */
    public void onQuestCompleted(@Nonnull Ref<EntityStore> playerRef, String questId, Map<String, Float> questExpRewards) {
        processExpRewards(playerRef, questExpRewards, "quest_" + questId);
    }
    
    /**
     * Example: Integration with crafting
     */
    public void onItemCrafted(@Nonnull Ref<EntityStore> playerRef, String itemId, String professionId) {
        // Calculate exp based on item difficulty
        float expAmount = calculateCraftingExp(itemId);
        
        Map<String, Float> expRewards = new java.util.HashMap<>();
        expRewards.put(professionId, expAmount);
        expRewards.put("character_level", expAmount * 0.1f); // 10% to character level
        
        processExpRewards(playerRef, expRewards, "craft_" + itemId);
    }
    
    /**
     * Example: Integration with quest completion
     */
    public void onQuestCompleted(@Nonnull Ref<EntityStore> entityRef, String questId) {
        Map<String, Float> expRewards = new java.util.HashMap<>();
        expRewards.put("character_level", 500.0f);
        
        processExpRewards(entityRef, expRewards, "quest:" + questId);
    }
    
    /**
     * Example: Integration with item crafting
     */
    public void onItemCrafted(@Nonnull Ref<EntityStore> playerRef, String itemId) {
        float craftExp = calculateCraftingExp(itemId);
        
        Map<String, Float> expRewards = new java.util.HashMap<>();
        expRewards.put("character_level", craftExp * 0.5f);
        expRewards.put("profession_crafting", craftExp);
        
        processExpRewards(playerRef, expRewards, "craft_" + itemId);
    }
    
    /**
     * Example: Integration with resource gathering
     */
    public void onResourceGathered(@Nonnull Ref<EntityStore> playerRef, String resourceId, int amount) {
        // Determine profession (mining, woodcutting, etc.)
        String professionId = determineProfessionForResource(resourceId);
        
        float baseExp = 10f;
        float totalExp = baseExp * amount;
        
        Map<String, Float> expRewards = new java.util.HashMap<>();
        expRewards.put(professionId, totalExp);
        
        processExpRewards(playerRef, expRewards, "gather_" + resourceId);
    }
    
    // === Helper methods (implement based on your system) ===
    
    private Map<String, Float> loadExpRewardsForEntity(String entityType) {
        // TODO: Load from entity configuration or dedicated exp reward configs
        // Example structure: Server/Entity/ExpRewards/Zombie.json
        return new java.util.HashMap<>();
    }
    
    private float calculateCraftingExp(String itemId) {
        // TODO: Calculate based on item rarity, recipe complexity, etc.
        return 50f;
    }
    
    private String determineProfessionForResource(String resourceId) {
        // TODO: Map resources to professions
        // Example: "hytale:oak_log" -> "profession_woodcutting"
        return "profession_gathering";
    }
}
