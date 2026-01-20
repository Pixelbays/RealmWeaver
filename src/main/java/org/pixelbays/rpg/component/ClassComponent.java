package org.pixelbays.rpg.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component that stores class/job progression for an entity.
 * Supports multiple learned classes with one active class at a time.
 * Integrates with LevelProgressionSystem for class leveling.
 */
@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
public class ClassComponent implements Component<EntityStore> {
    
    // Codec for ClassData serialization
    public static final BuilderCodec<ClassData> CLASS_DATA_CODEC = BuilderCodec
            .builder(ClassData.class, ClassData::new)
            .append(new KeyedCodec<>("ClassId", Codec.STRING), 
                    (data, value) -> data.classId = value,
                    data -> data.classId)
            .add()
            .append(new KeyedCodec<>("LearnedTime", Codec.LONG), 
                    (data, value) -> data.learnedTime = value,
                    data -> data.learnedTime)
            .add()
            .append(new KeyedCodec<>("TotalExpEarned", Codec.FLOAT), 
                    (data, value) -> data.totalExpEarned = value,
                    data -> data.totalExpEarned)
            .add()
            .append(new KeyedCodec<>("IsActive", Codec.BOOLEAN), 
                    (data, value) -> data.isActive = value,
                    data -> data.isActive)
            .add()
            .build();
    
        // Codec for SpellUnlockData serialization
        public static final BuilderCodec<SpellUnlockData> SPELL_UNLOCK_CODEC = BuilderCodec
            .builder(SpellUnlockData.class, SpellUnlockData::new)
            .append(new KeyedCodec<>("AbilityId", Codec.STRING),
                (data, value) -> data.abilityId = value,
                data -> data.abilityId)
            .add()
            .append(new KeyedCodec<>("UnlockedTime", Codec.LONG),
                (data, value) -> data.unlockedTime = value,
                data -> data.unlockedTime)
            .add()
            .append(new KeyedCodec<>("Rank", Codec.INTEGER),
                (data, value) -> data.rank = value,
                data -> data.rank)
            .add()
            .build();

        // Component codec for persistence
    public static final BuilderCodec<ClassComponent> CODEC = BuilderCodec
            .builder(ClassComponent.class, ClassComponent::new)
            .append(new KeyedCodec<>("LearnedClasses", new MapCodec<>(CLASS_DATA_CODEC, HashMap::new, false)),
                    (component, value) -> component.learnedClasses = value, 
                    component -> component.learnedClasses)
            .add()
            .append(new KeyedCodec<>("UnlockedSpells", new MapCodec<>(SPELL_UNLOCK_CODEC, HashMap::new, false)),
                (component, value) -> component.unlockedSpells = value,
                component -> component.unlockedSpells)
            .add()
            .append(new KeyedCodec<>("ActiveClassId", Codec.STRING), 
                    (component, value) -> component.activeClassId = value,
                    component -> component.activeClassId)
            .add()
            .build();
    
    // Map of classId -> class data
    private Map<String, ClassData> learnedClasses;

    // Map of abilityId -> unlock data
    private Map<String, SpellUnlockData> unlockedSpells;
    
    // Currently active class ID (only one active at a time)
    private String activeClassId;
    
    public ClassComponent() {
        this.learnedClasses = new HashMap<>();
        this.unlockedSpells = new HashMap<>();
        this.activeClassId = "";
    }
    
    /**
     * Check if player has learned a specific class
     */
    public boolean hasLearnedClass(String classId) {
        return learnedClasses.containsKey(classId);
    }
    
    /**
     * Get class data for a specific class (returns null if not learned)
     */
    @Nullable
    public ClassData getClassData(String classId) {
        return learnedClasses.get(classId);
    }
    
    /**
     * Get all learned class IDs
     */
    public Set<String> getLearnedClassIds() {
        return new HashSet<>(learnedClasses.keySet());
    }
    
    /**
     * Get active class ID
     */
    public String getActiveClassId() {
        return activeClassId;
    }
    
    /**
     * Set active class (does not validate, system should validate)
     */
    public void setActiveClassId(String classId) {
        // Mark old active class as inactive
        if (!activeClassId.isEmpty() && learnedClasses.containsKey(activeClassId)) {
            learnedClasses.get(activeClassId).setActive(false);
        }
        
        this.activeClassId = classId;
        
        // Mark new active class as active
        if (!classId.isEmpty() && learnedClasses.containsKey(classId)) {
            learnedClasses.get(classId).setActive(true);
        }
    }
    
    /**
     * Get active class data
     */
    @Nullable
    public ClassData getActiveClassData() {
        return activeClassId.isEmpty() ? null : learnedClasses.get(activeClassId);
    }
    
    /**
     * Learn a new class
     */
    public ClassData learnClass(String classId) {
        ClassData data = new ClassData(classId);
        learnedClasses.put(classId, data);
        return data;
    }
    
    /**
     * Unlearn a class (returns the class data before removal)
     */
    @Nullable
    public ClassData unlearnClass(String classId) {
        // If this was the active class, clear active
        if (activeClassId.equals(classId)) {
            activeClassId = "";
        }
        return learnedClasses.remove(classId);
    }
    
    /**
     * Get all learned classes data
     */
    public Map<String, ClassData> getAllLearnedClasses() {
        return learnedClasses;
    }

    /**
     * Check if a spell/ability is unlocked.
     */
    public boolean hasUnlockedSpell(String abilityId) {
        return unlockedSpells.containsKey(abilityId);
    }

    /**
     * Get unlock data for a spell/ability (returns null if not unlocked).
     */
    @Nullable
    public SpellUnlockData getUnlockedSpell(String abilityId) {
        return unlockedSpells.get(abilityId);
    }

    /**
     * Unlock a spell/ability.
     */
    public SpellUnlockData unlockSpell(String abilityId, int rank) {
        SpellUnlockData data = new SpellUnlockData(abilityId, rank);
        unlockedSpells.put(abilityId, data);
        return data;
    }

    /**
     * Get all unlocked spells.
     */
    public Map<String, SpellUnlockData> getAllUnlockedSpells() {
        return unlockedSpells;
    }
    
    /**
     * Get the component type for registration
     */
    public static ComponentType<EntityStore, ClassComponent> getComponentType() {
        return ExamplePlugin.get().getClassComponentType();
    }
    
    @Nonnull
    @Override
    @SuppressWarnings({"all", "CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException", "PMD.CloneMethodMustImplementCloneable"})
    public Component<EntityStore> clone() {
        ClassComponent cloned = new ClassComponent();
        cloned.activeClassId = this.activeClassId;
        for (Map.Entry<String, ClassData> entry : this.learnedClasses.entrySet()) {
            cloned.learnedClasses.put(entry.getKey(), entry.getValue().copy());
        }
        for (Map.Entry<String, SpellUnlockData> entry : this.unlockedSpells.entrySet()) {
            cloned.unlockedSpells.put(entry.getKey(), entry.getValue().copy());
        }
        return cloned;
    }
    
    /**
     * Data for a single learned class
     */
    public static class ClassData {
        private String classId;
        private long learnedTime;         // Timestamp when class was learned
        private float totalExpEarned;     // Total exp earned (for penalty calculation)
        private boolean isActive;         // Is this the currently active class?
        
        // Default constructor for codec
        public ClassData() {
            this.classId = "";
            this.learnedTime = System.currentTimeMillis();
            this.totalExpEarned = 0f;
            this.isActive = false;
        }
        
        public ClassData(String classId) {
            this.classId = classId;
            this.learnedTime = System.currentTimeMillis();
            this.totalExpEarned = 0f;
            this.isActive = false;
        }
        
        public String getId() {
            return classId;
        }
        
        public long getLearnedTime() {
            return learnedTime;
        }
        
        public float getTotalExpEarned() {
            return totalExpEarned;
        }
        
        public void addTotalExp(float amount) {
            this.totalExpEarned += amount;
        }
        
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            this.isActive = active;
        }
        
        public ClassData copy() {
            ClassData cloned = new ClassData(this.classId);
            cloned.learnedTime = this.learnedTime;
            cloned.totalExpEarned = this.totalExpEarned;
            cloned.isActive = this.isActive;
            return cloned;
        }
    }

    /**
     * Data for a single unlocked spell/ability
     */
    public static class SpellUnlockData {
        private String abilityId;
        private long unlockedTime;
        private int rank;

        public SpellUnlockData() {
            this.abilityId = "";
            this.unlockedTime = System.currentTimeMillis();
            this.rank = 1;
        }

        public SpellUnlockData(String abilityId, int rank) {
            this.abilityId = abilityId;
            this.unlockedTime = System.currentTimeMillis();
            this.rank = Math.max(1, rank);
        }

        public String getAbilityId() {
            return abilityId;
        }

        public long getUnlockedTime() {
            return unlockedTime;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = Math.max(1, rank);
        }

        public SpellUnlockData copy() {
            SpellUnlockData cloned = new SpellUnlockData(this.abilityId, this.rank);
            cloned.unlockedTime = this.unlockedTime;
            return cloned;
        }
    }
}
