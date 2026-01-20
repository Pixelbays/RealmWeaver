package org.pixelbays.rpg.event.levels;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.pixelbays.rpg.component.LevelProgressionComponent;

import javax.annotation.Nonnull;

/**
 * Event fired when an entity levels up in a specific level system.
 * Fired once per level gained (if entity gains 3 levels, this fires 3 times).
 */
public class LevelUpEvent {
    
    private final Ref<EntityStore> entityRef;      // Reference to the entity
    private final String systemId;                 // Which leveling system leveled up
    private final int previousLevel;
    private final int newLevel;
    private final LevelProgressionComponent.LevelSystemData levelData;
    
    public LevelUpEvent(@Nonnull Ref<EntityStore> entityRef, String systemId, int previousLevel, int newLevel, 
                        LevelProgressionComponent.LevelSystemData levelData) {
        this.entityRef = entityRef;
        this.systemId = systemId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.levelData = levelData;
    }
    
    @Nonnull
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }
    
    public String getSystemId() {
        return systemId;
    }
    
    public int getPreviousLevel() {
        return previousLevel;
    }
    
    public int getNewLevel() {
        return newLevel;
    }
    
    public LevelProgressionComponent.LevelSystemData getLevelData() {
        return levelData;
    }
    
    public int getLevelsGained() {
        return newLevel - previousLevel;
    }
}
