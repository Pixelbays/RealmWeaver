package org.pixelbays.rpg.system;

import com.hypixel.hytale.assetstore.AssetRegistry;
import org.pixelbays.rpg.config.RaceDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System that manages race definitions and hybrid validation.
 * Asset loading is handled via AssetRegistry.
 */
public class RaceManagementSystem {

    private static final String BASE_RACE_ID = "base_race";

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
                processRaceInheritance();
                System.out.println("[RaceSystem] Loaded " + raceDefinitions.size() + " races from asset registry");
            }
        } catch (Exception e) {
            System.err.println("[RaceSystem] Failed to load races from asset registry: " + e.getMessage());
        }
    }

    private void processRaceInheritance() {
        RaceDefinition baseRace = raceDefinitions.get(BASE_RACE_ID);
        for (RaceDefinition raceDef : raceDefinitions.values()) {
            if (baseRace != null && !BASE_RACE_ID.equalsIgnoreCase(raceDef.getRaceId())) {
                List<String> parentsForBaseCheck = raceDef.getParentRaces();
                boolean hasExplicitBaseParent = parentsForBaseCheck != null && parentsForBaseCheck.contains(BASE_RACE_ID);
                if (!hasExplicitBaseParent) {
                    raceDef.mergeFrom(baseRace);
                }
            }
            List<String> parents = raceDef.getParentRaces();
            if (parents == null || parents.isEmpty()) {
                continue;
            }
            for (String parentId : parents) {
                if (BASE_RACE_ID.equalsIgnoreCase(parentId)) {
                    continue;
                }
                RaceDefinition parent = raceDefinitions.get(parentId);
                if (parent != null) {
                    raceDef.mergeFrom(parent);
                }
            }
        }
    }

    public RaceDefinition getRaceDefinition(String raceId) {
        return raceDefinitions.get(raceId);
    }

    public Map<String, RaceDefinition> getRaceDefinitions() {
        return raceDefinitions;
    }
}
