package org.pixelbays.rpg.race.system;

import java.util.HashMap;
import java.util.Map;

import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.assetstore.AssetRegistry;

/**
 * System that manages race definitions and hybrid validation.
 * Asset loading is handled via AssetRegistry.
 */
public class RaceManagementSystem {


    private final Map<String, RaceDefinition> raceDefinitions;

    public RaceManagementSystem() {
        this.raceDefinitions = new HashMap<>();
    }

    public void registerRace(RaceDefinition raceDefinition) {
        raceDefinitions.put(raceDefinition.getRaceId(), raceDefinition);
        System.out.println("[RaceSystem] Registered race: " + raceDefinition.getRaceId() + " (" + raceDefinition.getDisplayName() + ")");
    }

    public void loadRaceDefinitionsFromAssets() {
        System.out.println("[RaceSystem] Loading race definitions from asset registry...");
        raceDefinitions.clear();

        try {
            var store = AssetRegistry.getAssetStore(RaceDefinition.class);
            if (store != null) {
                for (RaceDefinition raceDef : store.getAssetMap().getAssetMap().values()) {
                    registerRace(raceDef);
                }
                System.out.println("[RaceSystem] Loaded " + raceDefinitions.size() + " races from asset registry");
            }
        } catch (Exception e) {
            System.err.println("[RaceSystem] Failed to load races from asset registry: " + e.getMessage());
        }
    }


    public RaceDefinition getRaceDefinition(String raceId) {
        return raceDefinitions.get(raceId);
    }

    public Map<String, RaceDefinition> getRaceDefinitions() {
        return raceDefinitions;
    }
}
