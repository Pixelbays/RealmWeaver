package org.pixelbays.rpg.ability.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;

import com.hypixel.hytale.assetstore.AssetRegistry;

/**
 * System that manages class abilities - registration, lookup, and
 * interaction binding.
 * Unlock logic is handled elsewhere.
 */
public class ClassAbilitySystem {

    // Loaded ability definitions from asset pack
    private final Map<String, ClassAbilityDefinition> abilityDefinitions;

    public ClassAbilitySystem(@Nonnull ClassManagementSystem classManagementSystem) {
        this.abilityDefinitions = new HashMap<>();
    }

    /**
     * Register an ability definition from asset file
     */
    public void registerAbility(ClassAbilityDefinition abilityDefinition) {
        abilityDefinitions.put(abilityDefinition.getAbilityId(), abilityDefinition);
        System.out.println("[AbilitySystem] Registered ability: " + abilityDefinition.getAbilityId() +
                " (" + abilityDefinition.getDisplayName() + ")");
    }

    /**
     * Load ability definitions from asset registry
     */
    public void loadAbilityDefinitionsFromAssets() {
        System.out.println("[AbilitySystem] Loading class abilities from asset registry...");

        abilityDefinitions.clear();

        try {
            var store = AssetRegistry.getAssetStore(ClassAbilityDefinition.class);
            if (store != null) {
                for (ClassAbilityDefinition abilityDef : store.getAssetMap().getAssetMap().values()) {
                    registerAbility(abilityDef);
                }
                System.out.println(
                        "[AbilitySystem] Loaded " + abilityDefinitions.size() + " abilities from asset registry");
            }
        } catch (Exception e) {
            System.err.println("[AbilitySystem] Failed to load abilities from asset registry: " + e.getMessage());
        }

    }

    /**
     * Get ability definition
     */
    public ClassAbilityDefinition getAbilityDefinition(String abilityId) {
        return abilityDefinitions.get(abilityId);
    }

    /**
     * Get all registered ability IDs
     */
    public java.util.Set<String> getRegisteredAbilities() {
        return abilityDefinitions.keySet();
    }

}
