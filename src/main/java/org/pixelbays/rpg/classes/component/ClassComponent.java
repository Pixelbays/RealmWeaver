package org.pixelbays.rpg.classes.component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone"})
public class ClassComponent implements Component<EntityStore>, Cloneable {
    
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
            .build();
    
        // Component codec for persistence
    public static final BuilderCodec<ClassComponent> CODEC = BuilderCodec
            .builder(ClassComponent.class, ClassComponent::new)
            .append(new KeyedCodec<>("LearnedClasses", new MapCodec<>(CLASS_DATA_CODEC, LinkedHashMap::new, false)),
                    (component, value) -> component.learnedClasses = value, 
                    component -> component.learnedClasses)
            .add()
            .build();
    
    // Map of classId -> class data
    private Map<String, ClassData> learnedClasses;

    public ClassComponent() {
        this.learnedClasses = new LinkedHashMap<>();
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
        return new LinkedHashSet<>(learnedClasses.keySet());
    }

    /**
     * Get the primary learned class id (first learned).
     */
    @Nullable
    public String getPrimaryClassId() {
        if (learnedClasses.isEmpty()) {
            return null;
        }
        return learnedClasses.keySet().iterator().next();
    }

    /**
     * Move a learned class to the primary position.
     */
    public void prioritizeClass(String classId) {
        if (classId == null || classId.isEmpty() || !learnedClasses.containsKey(classId)) {
            return;
        }

        if (!(learnedClasses instanceof LinkedHashMap)) {
            return;
        }

        ClassData data = learnedClasses.remove(classId);
        LinkedHashMap<String, ClassData> reordered = new LinkedHashMap<>();
        reordered.put(classId, data);
        reordered.putAll(learnedClasses);
        learnedClasses.clear();
        learnedClasses.putAll(reordered);
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
        return learnedClasses.remove(classId);
    }
    
    /**
     * Get all learned classes data
     */
    public Map<String, ClassData> getAllLearnedClasses() {
        return learnedClasses;
    }

    /**
     * Get the component type for registration
     */
    public static ComponentType<EntityStore, ClassComponent> getComponentType() {
        return ExamplePlugin.get().getClassComponentType();
    }
    
    @Nonnull
    @Override
    @SuppressWarnings({"all", "clone", "CloneDoesntDeclareCloneNotSupportedException"})
    public Component<EntityStore> clone() {
        ClassComponent cloned = new ClassComponent();
        cloned.learnedClasses = new LinkedHashMap<>();
        for (Map.Entry<String, ClassData> entry : this.learnedClasses.entrySet()) {
            cloned.learnedClasses.put(entry.getKey(), entry.getValue().copy());
        }
        return cloned;
    }
    
    /**
     * Data for a single learned class
     */
    public static class ClassData {
        private String classId;
        private long learnedTime;         // Timestamp when class was learned
        
        // Default constructor for codec
        public ClassData() {
            this.classId = "";
            this.learnedTime = System.currentTimeMillis();
        }
        
        public ClassData(String classId) {
            this.classId = classId;
            this.learnedTime = System.currentTimeMillis();
        }
        
        public String getId() {
            return classId;
        }
        
        public long getLearnedTime() {
            return learnedTime;
        }
        
        public ClassData copy() {
            ClassData cloned = new ClassData(this.classId);
            cloned.learnedTime = this.learnedTime;
            return cloned;
        }
    }
}
