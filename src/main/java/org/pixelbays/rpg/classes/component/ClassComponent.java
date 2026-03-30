package org.pixelbays.rpg.classes.component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

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
    // Nested map codec: treeId -> (nodeId -> rank)
    private static final MapCodec<Integer, LinkedHashMap<String, Integer>> NODE_RANK_MAP_CODEC =
            new MapCodec<>(Codec.INTEGER, LinkedHashMap::new, false);
    private static final MapCodec<Map<String, Integer>, LinkedHashMap<String, Map<String, Integer>>> TREE_ALLOC_CODEC =
            new MapCodec<>(NODE_RANK_MAP_CODEC, LinkedHashMap::new, false);

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
            .append(new KeyedCodec<>("AllocatedNodes", TREE_ALLOC_CODEC, false, true),
                    (data, value) -> data.allocatedNodes = value,
                    data -> data.allocatedNodes)
            .add()
            .append(new KeyedCodec<>("SpentTalentPoints", Codec.INTEGER, false, true),
                    (data, value) -> data.spentTalentPoints = value,
                    data -> data.spentTalentPoints)
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
        return findLearnedClassKey(classId) != null;
    }
    
    /**
     * Get class data for a specific class (returns null if not learned)
     */
    @Nullable
    public ClassData getClassData(String classId) {
        String resolvedClassId = findLearnedClassKey(classId);
        return resolvedClassId == null ? null : learnedClasses.get(resolvedClassId);
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
        String resolvedClassId = findLearnedClassKey(classId);
        if (resolvedClassId == null) {
            return;
        }

        if (!(learnedClasses instanceof LinkedHashMap)) {
            return;
        }

        ClassData data = learnedClasses.remove(resolvedClassId);
        LinkedHashMap<String, ClassData> reordered = new LinkedHashMap<>();
        reordered.put(resolvedClassId, data);
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
        String resolvedClassId = findLearnedClassKey(classId);
        return resolvedClassId == null ? null : learnedClasses.remove(resolvedClassId);
    }
    
    /**
     * Get all learned classes data
     */
    public Map<String, ClassData> getAllLearnedClasses() {
        return learnedClasses;
    }

    @Nullable
    private String findLearnedClassKey(@Nullable String classId) {
        if (classId == null || classId.isEmpty()) {
            return null;
        }

        if (learnedClasses.containsKey(classId)) {
            return classId;
        }

        for (String learnedClassId : learnedClasses.keySet()) {
            if (learnedClassId != null && learnedClassId.equalsIgnoreCase(classId)) {
                return learnedClassId;
            }
        }

        return null;
    }

    /**
     * Get the component type for registration
     */
    public static ComponentType<EntityStore, ClassComponent> getComponentType() {
        return Realmweavers.get().getClassComponentType();
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
        // treeId -> (nodeId -> allocatedRank). Persisted.
        private Map<String, Map<String, Integer>> allocatedNodes;
        private int spentTalentPoints;    // Total talent points spent in this class (used to refund on reset)
        
        // Default constructor for codec
        public ClassData() {
            this.classId = "";
            this.learnedTime = System.currentTimeMillis();
            this.allocatedNodes = new LinkedHashMap<>();
            this.spentTalentPoints = 0;
        }
        
        public ClassData(String classId) {
            this.classId = classId;
            this.learnedTime = System.currentTimeMillis();
            this.allocatedNodes = new LinkedHashMap<>();
            this.spentTalentPoints = 0;
        }
        
        public String getId() {
            return classId;
        }
        
        public long getLearnedTime() {
            return learnedTime;
        }

        /** Get the rank of a specific node (0 = not allocated). */
        public int getNodeRank(String treeId, String nodeId) {
            Map<String, Integer> treeNodes = allocatedNodes.get(treeId);
            if (treeNodes == null) return 0;
            return treeNodes.getOrDefault(nodeId, 0);
        }

        /** Set the rank of a node (0 to remove). Returns old rank. */
        public int setNodeRank(String treeId, String nodeId, int rank) {
            if (rank <= 0) {
                Map<String, Integer> treeNodes = allocatedNodes.get(treeId);
                if (treeNodes == null) return 0;
                int old = treeNodes.getOrDefault(nodeId, 0);
                treeNodes.remove(nodeId);
                if (treeNodes.isEmpty()) allocatedNodes.remove(treeId);
                return old;
            }
            allocatedNodes.computeIfAbsent(treeId, k -> new LinkedHashMap<>()).put(nodeId, rank);
            return 0;
        }

        /** Get all allocated nodes for a tree (may be null/empty). */
        @Nullable
        public Map<String, Integer> getTreeAllocations(String treeId) {
            return allocatedNodes.get(treeId);
        }

        /** Get total points invested in a specific tree. */
        public int getTreePointsSpent(String treeId) {
            Map<String, Integer> treeNodes = allocatedNodes.get(treeId);
            if (treeNodes == null) return 0;
            int total = 0;
            for (int rank : treeNodes.values()) total += rank;
            return total;
        }

        /** Get all tree allocations (treeId -> nodeId -> rank). */
        public Map<String, Map<String, Integer>> getAllAllocatedNodes() {
            return allocatedNodes;
        }

        public int getSpentTalentPoints() {
            return spentTalentPoints;
        }

        public void setSpentTalentPoints(int spentTalentPoints) {
            this.spentTalentPoints = spentTalentPoints;
        }

        public void addSpentTalentPoints(int amount) {
            this.spentTalentPoints += amount;
        }

        /** Remove all talent allocations from this class data (for reset). */
        public void clearTalentAllocations() {
            allocatedNodes.clear();
            spentTalentPoints = 0;
        }

        public ClassData copy() {
            ClassData cloned = new ClassData(this.classId);
            cloned.learnedTime = this.learnedTime;
            cloned.spentTalentPoints = this.spentTalentPoints;
            cloned.allocatedNodes = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Integer>> treeEntry : this.allocatedNodes.entrySet()) {
                cloned.allocatedNodes.put(treeEntry.getKey(), new LinkedHashMap<>(treeEntry.getValue()));
            }
            return cloned;
        }
    }
}
