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

    public RaceManagementSystem() {
        // Races are loaded via Hytale's asset system
    }

    /**
     * Get race definition from asset store
     */
    public RaceDefinition getRaceDefinition(String raceId) {
        return RaceDefinition.getAssetMap().getAsset(raceId);
    }

    /**
     * Get all race definitions from asset store
     */
    public Map<String, RaceDefinition> getRaceDefinitions() {
        return RaceDefinition.getAssetMap().getAssetMap();
    }
}
