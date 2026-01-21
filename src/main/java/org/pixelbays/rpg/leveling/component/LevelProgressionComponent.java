package org.pixelbays.rpg.leveling.component;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component that stores all level progression data for an entity.
 * Supports multiple simultaneous leveling systems (character level, class
 * levels, profession levels, etc.)
 */
@SuppressWarnings("all")
public class LevelProgressionComponent implements Component<EntityStore>, Cloneable {

    // Codec for NBT serialization (required for persistence)
    public static final BuilderCodec<LevelSystemData> LEVEL_SYSTEM_DATA_CODEC = BuilderCodec
            .builder(LevelSystemData.class, LevelSystemData::new)
            .append(new KeyedCodec<>("SystemId", Codec.STRING), (data, value) -> data.systemId = value,
                    data -> data.systemId)
            .add()
            .append(new KeyedCodec<>("CurrentLevel", Codec.INTEGER), (data, value) -> data.currentLevel = value,
                    data -> data.currentLevel)
            .add()
            .append(new KeyedCodec<>("CurrentExp", Codec.FLOAT), (data, value) -> data.currentExp = value,
                    data -> data.currentExp)
            .add()
            .append(new KeyedCodec<>("ExpToNextLevel", Codec.FLOAT), (data, value) -> data.expToNextLevel = value,
                    data -> data.expToNextLevel)
            .add()
            .append(new KeyedCodec<>("TotalLevelsGained", Codec.INTEGER),
                    (data, value) -> data.totalLevelsGained = value, data -> data.totalLevelsGained)
            .add()
            .append(new KeyedCodec<>("AvailableStatPoints", Codec.INTEGER),
                    (data, value) -> data.availableStatPoints = value, data -> data.availableStatPoints)
            .add()
            .append(new KeyedCodec<>("AvailableSkillPoints", Codec.INTEGER),
                    (data, value) -> data.availableSkillPoints = value, data -> data.availableSkillPoints)
            .add()
            .build();

    public static final BuilderCodec<LevelProgressionComponent> CODEC = BuilderCodec
            .builder(LevelProgressionComponent.class, LevelProgressionComponent::new)
            .append(new KeyedCodec<>("LevelSystems", new MapCodec<>(LEVEL_SYSTEM_DATA_CODEC, HashMap::new, false)),
                    (component, value) -> component.levelSystems = value, component -> component.levelSystems)
            .add()
            .build();

    /**
     * Map of level system ID -> level data
     * Examples: "character_level", "class_warrior", "profession_woodworking"
     */
    private Map<String, LevelSystemData> levelSystems;

    public LevelProgressionComponent() {
        this.levelSystems = new HashMap<>();
    }

    /**
     * Get level data for a specific system, or create it if it doesn't exist
     */
    public LevelSystemData getOrCreateSystem(String systemId) {
        return levelSystems.computeIfAbsent(systemId, k -> new LevelSystemData(systemId));
    }

    /**
     * Check if entity has a specific level system
     */
    public boolean hasSystem(String systemId) {
        return levelSystems.containsKey(systemId);
    }

    /**
     * Get level data for a specific system (returns null if doesn't exist)
     */
    public LevelSystemData getSystem(String systemId) {
        return levelSystems.get(systemId);
    }

    /**
     * Get all level systems
     */
    public Map<String, LevelSystemData> getAllSystems() {
        return levelSystems;
    }

    /**
     * Get the component type for registration
     */
    public static ComponentType<EntityStore, LevelProgressionComponent> getComponentType() {
        return ExamplePlugin.get().getLevelProgressionComponentType();
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        try {
            LevelProgressionComponent cloned = (LevelProgressionComponent) super.clone();
            cloned.levelSystems = new HashMap<>();
            for (Map.Entry<String, LevelSystemData> entry : this.levelSystems.entrySet()) {
                cloned.levelSystems.put(entry.getKey(), entry.getValue().clone());
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Data for a single leveling system
     */
    public static class LevelSystemData implements Cloneable {
        private String systemId; // Changed from final to allow codec deserialization
        private int currentLevel;
        private float currentExp;
        private float expToNextLevel;
        private int totalLevelsGained; // Track total levels for multi-level-up events

        // Reward tracking
        private int availableStatPoints;
        private int availableSkillPoints;

        // Default constructor for codec
        public LevelSystemData() {
            this.systemId = "";
            this.currentLevel = 1;
            this.currentExp = 0;
            this.expToNextLevel = 100;
            this.totalLevelsGained = 0;
            this.availableStatPoints = 0;
            this.availableSkillPoints = 0;
        }

        public LevelSystemData(String systemId) {
            this.systemId = systemId;
            this.currentLevel = 1;
            this.currentExp = 0;
            this.expToNextLevel = 100; // Default, will be overridden by config
            this.totalLevelsGained = 0;
            this.availableStatPoints = 0;
            this.availableSkillPoints = 0;
        }

        // Getters and setters
        public String getSystemId() {
            return systemId;
        }

        public int getCurrentLevel() {
            return currentLevel;
        }

        public void setCurrentLevel(int currentLevel) {
            this.currentLevel = currentLevel;
        }

        public float getCurrentExp() {
            return currentExp;
        }

        public void setCurrentExp(float currentExp) {
            this.currentExp = currentExp;
        }

        public float getExpToNextLevel() {
            return expToNextLevel;
        }

        public void setExpToNextLevel(float expToNextLevel) {
            this.expToNextLevel = expToNextLevel;
        }

        public int getTotalLevelsGained() {
            return totalLevelsGained;
        }

        public void setTotalLevelsGained(int totalLevelsGained) {
            this.totalLevelsGained = totalLevelsGained;
        }

        public void incrementLevelsGained() {
            this.totalLevelsGained++;
        }

        public int getAvailableStatPoints() {
            return availableStatPoints;
        }

        public void setAvailableStatPoints(int availableStatPoints) {
            this.availableStatPoints = availableStatPoints;
        }

        public void addStatPoints(int points) {
            this.availableStatPoints += points;
        }

        public int getAvailableSkillPoints() {
            return availableSkillPoints;
        }

        public void setAvailableSkillPoints(int availableSkillPoints) {
            this.availableSkillPoints = availableSkillPoints;
        }

        public void addSkillPoints(int points) {
            this.availableSkillPoints += points;
        }

        /**
         * Add experience and return number of levels gained
         */
        public int addExperience(float amount) {
            this.currentExp += amount;
            int levelsGained = 0;

            // Check for level ups (can be multiple)
            while (this.currentExp >= this.expToNextLevel && this.expToNextLevel > 0) {
                this.currentExp -= this.expToNextLevel;
                this.currentLevel++;
                levelsGained++;
                this.totalLevelsGained++;
                // Note: expToNextLevel will be updated by the system after processing
            }

            return levelsGained;
        }

        /**
         * Get progress to next level as percentage (0.0 to 1.0)
         */
        public float getLevelProgress() {
            if (expToNextLevel <= 0) {
                return 1.0f; // Max level
            }
            return Math.min(1.0f, currentExp / expToNextLevel);
        }

        /**
         * Clone this level system data
         */
        @Override
        public LevelSystemData clone() {
            try {
                LevelSystemData cloned = (LevelSystemData) super.clone();
                cloned.systemId = this.systemId;
                cloned.currentLevel = this.currentLevel;
                cloned.currentExp = this.currentExp;
                cloned.expToNextLevel = this.expToNextLevel;
                cloned.totalLevelsGained = this.totalLevelsGained;
                cloned.availableStatPoints = this.availableStatPoints;
                cloned.availableSkillPoints = this.availableSkillPoints;
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }
}
