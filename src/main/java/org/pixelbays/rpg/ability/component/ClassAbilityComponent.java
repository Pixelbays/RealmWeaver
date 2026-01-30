package org.pixelbays.rpg.ability.component;

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
 * Component that stores unlocked abilities for each class.
 * Tracks which abilities are available and when they were unlocked.
 */
@SuppressWarnings({ "PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all" })
public class ClassAbilityComponent implements Component<EntityStore>, Cloneable {

    // Codec for AbilityData serialization
    public static final BuilderCodec<AbilityData> Ability_DATA_CODEC = BuilderCodec
            .builder(AbilityData.class, AbilityData::new)
            .append(new KeyedCodec<>("AbilityId", Codec.STRING),
                    (data, value) -> data.abilityId = value,
                    data -> data.abilityId)
            .add()
            .append(new KeyedCodec<>("ClassId", Codec.STRING),
                    (data, value) -> data.classId = value,
                    data -> data.classId)
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
    public static final BuilderCodec<ClassAbilityComponent> CODEC = BuilderCodec
            .builder(ClassAbilityComponent.class, ClassAbilityComponent::new)
            .append(new KeyedCodec<>("UnlockedAbilities", new MapCodec<>(Ability_DATA_CODEC, HashMap::new, false)),
                    (component, value) -> component.unlockedAbilities = value,
                    component -> component.unlockedAbilities)
            .add()
            .append(new KeyedCodec<>("ToggleStates", new MapCodec<>(Codec.BOOLEAN, HashMap::new, false)),
                    (component, value) -> component.toggleStates = value,
                    component -> component.toggleStates)
            .add()
            .build();

    // Map of abilityId -> ability data
    private Map<String, AbilityData> unlockedAbilities;
    
    // Map of abilityId -> toggle state (true = active, false = inactive)
    private Map<String, Boolean> toggleStates;

    public ClassAbilityComponent() {
        this.unlockedAbilities = new HashMap<>();
        this.toggleStates = new HashMap<>();
    }

    /**
     * Check if an ability is unlocked
     */
    public boolean hasAbility(String abilityId) {
        return unlockedAbilities.containsKey(abilityId);
    }

    /**
     * Get ability data (returns null if not unlocked)
     */
    @Nullable
    public AbilityData getAbilityData(String abilityId) {
        return unlockedAbilities.get(abilityId);
    }

    /**
     * Get all unlocked ability IDs
     */
    public Set<String> getUnlockedAbilityIds() {
        return new HashSet<>(unlockedAbilities.keySet());
    }

    /**
     * Get all abilities for a specific class
     */
    public Set<String> getAbilitiesForClass(String classId) {
        Set<String> classAbilities = new HashSet<>();
        for (AbilityData data : unlockedAbilities.values()) {
            if (classId.equals(data.classId)) {
                classAbilities.add(data.getAbilityId());
            }
        }
        return classAbilities;
    }

    /**
     * Unlock an ability
     */
    public AbilityData unlockAbility(String abilityId, String classId) {
        AbilityData data = new AbilityData(abilityId, classId);
        unlockedAbilities.put(abilityId, data);
        return data;
    }

    /**
     * Unlock an ability with explicit rank.
     */
    public AbilityData unlockAbility(String abilityId, String classId, int rank) {
        AbilityData data = new AbilityData(abilityId, classId);
        data.setRank(Math.max(1, rank));
        unlockedAbilities.put(abilityId, data);
        return data;
    }

    /**
     * Remove an ability (e.g., when unlearning a class)
     */
    @Nullable
    public AbilityData removeAbility(String abilityId) {
        toggleStates.remove(abilityId);
        return unlockedAbilities.remove(abilityId);
    }

    /**
     * Remove all abilities for a class
     */
    public void removeAbilitiesForClass(String classId) {
        unlockedAbilities.entrySet().removeIf(entry -> {
            boolean remove = classId.equals(entry.getValue().classId);
            if (remove) {
                toggleStates.remove(entry.getKey());
            }
            return remove;
        });
    }

    /**
     * Get ability rank (0 if not unlocked)
     */
    public int getAbilityRank(String abilityId) {
        AbilityData data = unlockedAbilities.get(abilityId);
        return data != null ? data.getRank() : 0;
    }

    /**
     * Increase ability rank
     */
    public boolean increaseRank(String abilityId) {
        AbilityData data = unlockedAbilities.get(abilityId);
        if (data != null) {
            data.setRank(data.getRank() + 1);
            return true;
        }
        return false;
    }

    /**
     * Get all unlocked abilities
     */
    public Map<String, AbilityData> getAllAbilities() {
        return unlockedAbilities;
    }

    /**
     * Check if a toggle ability is currently active
     */
    public boolean isToggleActive(String abilityId) {
        return toggleStates.getOrDefault(abilityId, false);
    }

    /**
     * Set toggle state for an ability
     */
    public void setToggleState(String abilityId, boolean active) {
        toggleStates.put(abilityId, active);
    }

    /**
     * Toggle the state of a toggle ability
     * @return the new state (true = now active, false = now inactive)
     */
    public boolean toggleAbility(String abilityId) {
        boolean currentState = toggleStates.getOrDefault(abilityId, false);
        boolean newState = !currentState;
        toggleStates.put(abilityId, newState);
        return newState;
    }

    /**
     * Get all active toggle abilities
     */
    public Set<String> getActiveToggles() {
        Set<String> activeToggles = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : toggleStates.entrySet()) {
            if (entry.getValue()) {
                activeToggles.add(entry.getKey());
            }
        }
        return activeToggles;
    }

    /**
     * Get the component type for registration
     */
    @SuppressWarnings("unchecked")
    public static ComponentType<EntityStore, ClassAbilityComponent> getComponentType() {
        return (ComponentType<EntityStore, ClassAbilityComponent>) (ComponentType<?, ?>) ExamplePlugin.get()
                .getClassAbilityComponentType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public ClassAbilityComponent clone() {
        try {
            ClassAbilityComponent cloned = (ClassAbilityComponent) super.clone();
            cloned.unlockedAbilities = new HashMap<>();
            for (Map.Entry<String, AbilityData> entry : this.unlockedAbilities.entrySet()) {
                AbilityData source = entry.getValue();
                AbilityData copy = new AbilityData(source.abilityId, source.classId);
                copy.unlockedTime = source.unlockedTime;
                copy.rank = source.rank;
                cloned.unlockedAbilities.put(entry.getKey(), copy);
            }
            cloned.toggleStates = new HashMap<>(this.toggleStates);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Data for a single unlocked ability
     */
    public static class AbilityData {
        private String abilityId;
        private String classId; // Which class this ability belongs to
        private long unlockedTime; // Timestamp when unlocked
        private int rank; // Ability rank (for upgradeable abilities)

        // Default constructor for codec
        public AbilityData() {
            this.abilityId = "";
            this.classId = "";
            this.unlockedTime = System.currentTimeMillis();
            this.rank = 1;
        }

        public AbilityData(String abilityId, String classId) {
            this.abilityId = abilityId;
            this.classId = classId;
            this.unlockedTime = System.currentTimeMillis();
            this.rank = 1;
        }

        public String getAbilityId() {
            return abilityId;
        }

        public String getClassId() {
            return classId;
        }

        public long getUnlockedTime() {
            return unlockedTime;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

    }
}
