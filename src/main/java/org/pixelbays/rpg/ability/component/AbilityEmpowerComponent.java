package org.pixelbays.rpg.ability.component;

import java.util.HashMap;
import java.util.Map;

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
 * Tracks temporary empowerment effects for abilities.
 */
@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all"})
public class AbilityEmpowerComponent implements Component<EntityStore>, Cloneable {

    public static final BuilderCodec<EmpowerData> EMPOWER_DATA_CODEC = BuilderCodec
            .builder(EmpowerData.class, EmpowerData::new)
            .append(new KeyedCodec<>("AbilityId", Codec.STRING),
                    (data, value) -> data.abilityId = value,
                    data -> data.abilityId)
            .add()
            .append(new KeyedCodec<>("RemainingCasts", Codec.INTEGER),
                    (data, value) -> data.remainingCasts = value,
                    data -> data.remainingCasts)
            .add()
            .append(new KeyedCodec<>("Multiplier", Codec.FLOAT),
                    (data, value) -> data.multiplier = value,
                    data -> data.multiplier)
            .add()
            .build();

    public static final BuilderCodec<AbilityEmpowerComponent> CODEC = BuilderCodec
            .builder(AbilityEmpowerComponent.class, AbilityEmpowerComponent::new)
            .append(new KeyedCodec<>("Empowerments", new MapCodec<>(EMPOWER_DATA_CODEC, HashMap::new, false)),
                    (component, value) -> component.empowerments = value,
                    component -> component.empowerments)
            .add()
            .build();

    private Map<String, EmpowerData> empowerments;

    public AbilityEmpowerComponent() {
        this.empowerments = new HashMap<>();
    }

    /**
     * Add empowerment for an ability.
     */
    public void addEmpowerment(@Nonnull String abilityId, int casts, float multiplier) {
        if (casts <= 0) {
            return;
        }

        EmpowerData data = empowerments.get(abilityId);
        if (data == null) {
            data = new EmpowerData(abilityId, casts, multiplier);
            empowerments.put(abilityId, data);
            return;
        }

        data.remainingCasts += casts;
        data.multiplier = Math.max(data.multiplier, multiplier);
    }

    /**
     * Consume one empowered cast for an ability.
     * @return the multiplier if empowered, or 1.0f if not empowered.
     */
    public float consumeEmpowerment(@Nonnull String abilityId) {
        EmpowerData data = empowerments.get(abilityId);
        if (data == null || data.remainingCasts <= 0) {
            return 1.0f;
        }

        data.remainingCasts -= 1;
        float multiplier = data.multiplier;
        if (data.remainingCasts <= 0) {
            empowerments.remove(abilityId);
        }

        return multiplier;
    }

    /**
     * Check if an ability has empowerment available.
     */
    public boolean hasEmpowerment(@Nonnull String abilityId) {
        EmpowerData data = empowerments.get(abilityId);
        return data != null && data.remainingCasts > 0;
    }

    /**
     * Get empowerment data for an ability.
     */
    @Nullable
    public EmpowerData getEmpowerment(@Nonnull String abilityId) {
        return empowerments.get(abilityId);
    }

    /**
     * Get all empowerment data.
     */
    @Nonnull
    public Map<String, EmpowerData> getAllEmpowerments() {
        return empowerments;
    }

    @SuppressWarnings("unchecked")
    public static ComponentType<EntityStore, AbilityEmpowerComponent> getComponentType() {
        return (ComponentType<EntityStore, AbilityEmpowerComponent>) (ComponentType<?, ?>) ExamplePlugin.get()
                .getAbilityEmpowerComponentType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public AbilityEmpowerComponent clone() {
        try {
            AbilityEmpowerComponent cloned = (AbilityEmpowerComponent) super.clone();
            cloned.empowerments = new HashMap<>();
            for (Map.Entry<String, EmpowerData> entry : this.empowerments.entrySet()) {
                EmpowerData source = entry.getValue();
                EmpowerData copy = new EmpowerData(source.abilityId, source.remainingCasts, source.multiplier);
                cloned.empowerments.put(entry.getKey(), copy);
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public static class EmpowerData {
        private String abilityId;
        private int remainingCasts;
        private float multiplier;

        public EmpowerData() {
            this.abilityId = "";
            this.remainingCasts = 0;
            this.multiplier = 1.0f;
        }

        public EmpowerData(String abilityId, int remainingCasts, float multiplier) {
            this.abilityId = abilityId;
            this.remainingCasts = Math.max(0, remainingCasts);
            this.multiplier = Math.max(1.0f, multiplier);
        }

        public String getAbilityId() {
            return abilityId;
        }

        public int getRemainingCasts() {
            return remainingCasts;
        }

        public float getMultiplier() {
            return multiplier;
        }
    }
}
