package org.pixelbays.rpg.leveling.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an entity gains experience in a level system.
 * Fired BEFORE level-up calculations, allowing other systems to modify the exp amount.
 */
public class ExperienceGainEvent {
    
    private final Ref<EntityStore> entityRef;      // Reference to the entity
    private final String systemId;                 // Which leveling system gained exp
    private float expAmount;                       // Amount of exp gained (modifiable)
    private final String source;                   // Source of exp (e.g., "kill_entity", "quest_complete", "crafting")
    private boolean cancelled;
    
    public ExperienceGainEvent(@Nonnull Ref<EntityStore> entityRef, String systemId, float expAmount, String source) {
        this.entityRef = entityRef;
        this.systemId = systemId;
        this.expAmount = expAmount;
        this.source = source;
        this.cancelled = false;
    }
    
    @Nonnull
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }
    
    public String getSystemId() {
        return systemId;
    }
    
    public float getExpAmount() {
        return expAmount;
    }
    
    public void setExpAmount(float expAmount) {
        this.expAmount = expAmount;
    }
    
    public void multiplyExpAmount(float multiplier) {
        this.expAmount *= multiplier;
    }
    
    public String getSource() {
        return source;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
