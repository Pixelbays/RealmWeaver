package org.pixelbays.rpg.ability.component;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component that stores player ability bindings for hotbar slots and other
 * controls.
 * Maps slot identifiers to ability IDs from ClassAbilityDefinition.
 */
public class AbilityBindingComponent implements Component<EntityStore> {

    public static final BuilderCodec<AbilityBindingComponent> CODEC = BuilderCodec.builder(
            AbilityBindingComponent.class,
            AbilityBindingComponent::new)
            .append(
                    new KeyedCodec<>("HotbarBindings",
                            new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (component, value) -> component.setHotbarBindingsFromStrings(value),
                    component -> component.getHotbarBindingsAsStrings())
            .add()
            .append(
                    new KeyedCodec<>("WeaponBindings",
                            new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (component, value) -> component.weaponBindings = value,
                    component -> component.weaponBindings)
            .add()
            .append(
                    new KeyedCodec<>("AbilitySlotBindings",
                            new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (component, value) -> component.setAbilitySlotBindingsFromStrings(value),
                    component -> component.getAbilitySlotBindingsAsStrings())
            .add()
            .build();

    private static ComponentType<EntityStore, AbilityBindingComponent> COMPONENT_TYPE;

    // Hotbar slot -> ability ID (slot 7 = "fireball")
    private Map<Integer, String> hotbarBindings;

    // Weapon interaction -> ability ID ("Primary" = "slash", "Secondary" = "block")
    private Map<String, String> weaponBindings;

    // Ability slot number -> ability ID (1 = "dash", 2 = "heal", 3 = "ultimate")
    private Map<Integer, String> abilitySlotBindings;

    public AbilityBindingComponent() {
        this.hotbarBindings = new HashMap<>();
        this.weaponBindings = new HashMap<>();
        this.abilitySlotBindings = new HashMap<>();
    }

    @Nonnull
    public static ComponentType<EntityStore, AbilityBindingComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, AbilityBindingComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }

    @Override
    @SuppressWarnings({ "all", "clone" })
    public Component<EntityStore> clone() {
        AbilityBindingComponent cloned = new AbilityBindingComponent();
        cloned.hotbarBindings = new HashMap<>(this.hotbarBindings);
        cloned.weaponBindings = new HashMap<>(this.weaponBindings);
        cloned.abilitySlotBindings = new HashMap<>(this.abilitySlotBindings);
        return cloned;
    }

    // === Hotbar Bindings ===

    /**
     * Bind an ability to a hotbar slot.
     * 
     * @param slot      The hotbar slot (0-8)
     * @param abilityId The ability ID, or null to unbind
     */
    public void setHotbarBinding(int slot, @Nullable String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) {
            hotbarBindings.remove(slot);
        } else {
            hotbarBindings.put(slot, abilityId);
        }
    }

    /**
     * Get the ability bound to a hotbar slot.
     * 
     * @param slot The hotbar slot (0-8)
     * @return The ability ID, or null if none bound
     */
    @Nullable
    public String getHotbarBinding(int slot) {
        return hotbarBindings.get(slot);
    }

    /**
     * Get all hotbar bindings.
     */
    @Nonnull
    public Map<Integer, String> getHotbarBindings() {
        return hotbarBindings;
    }

    /**
     * Clear all hotbar bindings.
     */
    public void clearHotbarBindings() {
        hotbarBindings.clear();
    }

    // === Weapon Bindings ===

    /**
     * Bind an ability to a weapon interaction.
     * 
     * @param interactionType "Primary" or "Secondary"
     * @param abilityId       The ability ID, or null to unbind
     */
    public void setWeaponBinding(@Nonnull String interactionType, @Nullable String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) {
            weaponBindings.remove(interactionType);
        } else {
            weaponBindings.put(interactionType, abilityId);
        }
    }

    /**
     * Get the ability bound to a weapon interaction.
     * 
     * @param interactionType "Primary" or "Secondary"
     * @return The ability ID, or null if none bound
     */
    @Nullable
    public String getWeaponBinding(@Nonnull String interactionType) {
        return weaponBindings.get(interactionType);
    }

    /**
     * Get all weapon bindings.
     */
    @Nonnull
    public Map<String, String> getWeaponBindings() {
        return weaponBindings;
    }

    /**
     * Clear all weapon bindings.
     */
    public void clearWeaponBindings() {
        weaponBindings.clear();
    }

    // === Ability Slot Bindings ===

    /**
     * Bind an ability to an ability slot.
     * 
     * @param slot      The ability slot (1, 2, or 3)
     * @param abilityId The ability ID, or null to unbind
     */
    public void setAbilitySlotBinding(int slot, @Nullable String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) {
            abilitySlotBindings.remove(slot);
        } else {
            abilitySlotBindings.put(slot, abilityId);
        }
    }

    /**
     * Get the ability bound to an ability slot.
     * 
     * @param slot The ability slot (1, 2, or 3)
     * @return The ability ID, or null if none bound
     */
    @Nullable
    public String getAbilitySlotBinding(int slot) {
        return abilitySlotBindings.get(slot);
    }

    /**
     * Get all ability slot bindings.
     */
    @Nonnull
    public Map<Integer, String> getAbilitySlotBindings() {
        return abilitySlotBindings;
    }

    /**
     * Clear all ability slot bindings.
     */
    public void clearAbilitySlotBindings() {
        abilitySlotBindings.clear();
    }

    /**
     * Clear all bindings.
     */
    public void clearAllBindings() {
        clearHotbarBindings();
        clearWeaponBindings();
        clearAbilitySlotBindings();
    }

    // Codec helper methods for integer key conversion
    private void setHotbarBindingsFromStrings(Map<String, String> stringMap) {
        this.hotbarBindings = new HashMap<>();
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            try {
                this.hotbarBindings.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }
    }

    private Map<String, String> getHotbarBindingsAsStrings() {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : this.hotbarBindings.entrySet()) {
            stringMap.put(entry.getKey().toString(), entry.getValue());
        }
        return stringMap;
    }

    private void setAbilitySlotBindingsFromStrings(Map<String, String> stringMap) {
        this.abilitySlotBindings = new HashMap<>();
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            try {
                this.abilitySlotBindings.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }
    }

    private Map<String, String> getAbilitySlotBindingsAsStrings() {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : this.abilitySlotBindings.entrySet()) {
            stringMap.put(entry.getKey().toString(), entry.getValue());
        }
        return stringMap;
    }
}
