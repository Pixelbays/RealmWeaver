package org.pixelbays.rpg.system;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.config.ClassAbilityDefinition;
import org.pixelbays.rpg.config.ClassDefinition;

import com.hypixel.hytale.assetstore.AssetRegistry;

/**
 * System that manages class abilities - registration, lookup, and
 * interaction binding.
 * Unlock logic is handled elsewhere.
 */
public class ClassAbilitySystem {

    // Loaded ability definitions from asset pack
    private final Map<String, ClassAbilityDefinition> abilityDefinitions;

    // Reference to class management system
    private final ClassManagementSystem classManagementSystem;

    public ClassAbilitySystem(@Nonnull ClassManagementSystem classManagementSystem) {
        this.classManagementSystem = classManagementSystem;
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
                processAbilityInheritance();
                System.out.println(
                        "[AbilitySystem] Loaded " + abilityDefinitions.size() + " abilities from asset registry");
            }
        } catch (Exception e) {
            System.err.println("[AbilitySystem] Failed to load abilities from asset registry: " + e.getMessage());
        }

    }

    /**
     * Create test ability configurations
     */
    private void processAbilityInheritance() {
        for (ClassAbilityDefinition abilityDef : abilityDefinitions.values()) {
            String parentId = abilityDef.getParentAbilityId();
            if (parentId != null && !parentId.isEmpty()) {
                ClassAbilityDefinition parent = abilityDefinitions.get(parentId);
                if (parent != null) {
                    abilityDef.mergeFrom(parent);
                }
            }
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

    /**
     * Get all ability ids for a specific class.
     */
    public java.util.Set<String> getAbilitiesForClass(String classId) {
        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef != null) {
            java.util.Set<String> abilityIds = classDef.getAbilityIds();
            if (!abilityIds.isEmpty()) {
                return abilityIds;
            }
        }

        java.util.Set<String> classAbilities = new java.util.HashSet<>();
        for (ClassAbilityDefinition abilityDef : abilityDefinitions.values()) {
            classAbilities.add(abilityDef.getAbilityId());
        }

        return classAbilities;
    }

    /**
     * Resolve the owning class id for an ability using class definitions.
     */
    public String getOwningClassId(String abilityId) {
        for (ClassDefinition classDef : classManagementSystem.getAllClassDefinitions().values()) {
            if (classDef.getAbilityIds().contains(abilityId)) {
                return classDef.getId();
            }
        }
        return null;
    }

}
