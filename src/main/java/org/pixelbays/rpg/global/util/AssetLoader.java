package org.pixelbays.rpg.global.util;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.system.RpgLogging;
import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;

import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;

/**
 * Utility class for loading RPG assets using Hytale's asset system.
 * 
 * This class provides convenient access methods to retrieve assets that have
 * been
 * loaded by the Hytale asset system during initialization.
 * 
 * All assets are loaded automatically via AssetRegistry when the plugin
 * initializes.
 * Assets support inheritance, validation, and are cached by the system.
 * 
 * @see com.hypixel.hytale.assetstore.AssetRegistry
 */
public class AssetLoader {

    /**
     * Load all ClassDefinition assets from the asset system.
     * 
     * ClassDefinitions are automatically loaded from Server/Classes/*.json
     * by the AssetRegistry during plugin initialization.
     * 
     * @return Map of classId to ClassDefinition
     */
    @Nonnull
    public static Map<String, ClassDefinition> loadClassDefinitions() {
        RpgLogging.debugDeveloper("Loading class definitions from asset system...");

        try {
            AssetStore<String, ClassDefinition, DefaultAssetMap<String, ClassDefinition>> assetStore = ClassDefinition
                    .getAssetStore();

            if (assetStore == null) {
                RpgLogging.debugDeveloper("ClassDefinition AssetStore not registered!");
                return new HashMap<>();
            }

            DefaultAssetMap<String, ClassDefinition> assetMap = assetStore.getAssetMap();
            if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
                RpgLogging.debugDeveloper("No class definitions loaded by asset system");
                return new HashMap<>();
            }

            Map<String, ClassDefinition> classDefinitions = new HashMap<>(assetMap.getAssetMap());
            RpgLogging.debugDeveloper("Loaded %d class definitions from asset system", classDefinitions.size());

            return classDefinitions;
        } catch (Exception e) {
            RpgLogging.debugDeveloper("Failed to load class definitions from asset system: %s", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Load all LevelSystemConfig assets from the asset system.
     * 
     * LevelSystemConfigs are automatically loaded from Server/Entity/levels/*.json
     * by the AssetRegistry during plugin initialization.
     * Parent inheritance is handled automatically by the CODEC system.
     * 
     * @return Map of systemId to LevelSystemConfig
     */
    @Nonnull
    public static Map<String, LevelSystemConfig> loadLevelSystemConfigs() {
        RpgLogging.debugDeveloper("Loading level system configurations from asset system...");

        try {
            AssetStore<String, LevelSystemConfig, DefaultAssetMap<String, LevelSystemConfig>> assetStore = LevelSystemConfig
                    .getAssetStore();

            if (assetStore == null) {
                RpgLogging.debugDeveloper("LevelSystemConfig AssetStore not registered!");
                return new HashMap<>();
            }

            DefaultAssetMap<String, LevelSystemConfig> assetMap = assetStore.getAssetMap();
            if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
                RpgLogging.debugDeveloper("No level system configs loaded by asset system");
                return new HashMap<>();
            }

            Map<String, LevelSystemConfig> levelConfigs = new HashMap<>(assetMap.getAssetMap());
            RpgLogging.debugDeveloper("Loaded %d level system configurations from asset system", levelConfigs.size());

            return levelConfigs;
        } catch (Exception e) {
            RpgLogging.debugDeveloper("Failed to load level system configs from asset system: %s", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Load all ExpCurveDefinition assets from the asset system.
     * 
     * ExpCurveDefinitions are automatically loaded from
     * Server/Entity/ExpCurves/*.json
     * by the AssetRegistry during plugin initialization.
     * 
     * @return Map of curveId to ExpCurveDefinition
     */
    @Nonnull
    public static Map<String, ExpCurveDefinition> loadExpCurveConfigs() {
        RpgLogging.debugDeveloper("Loading exp curve configurations from asset system...");

        try {
            // ExpCurveDefinition uses wildcard type for asset map
            var assetStore = ExpCurveDefinition.getAssetStore();

            if (assetStore == null) {
                RpgLogging.debugDeveloper("ExpCurveDefinition AssetStore not registered!");
                return new HashMap<>();
            }

            DefaultAssetMap<String, ExpCurveDefinition> assetMap = (DefaultAssetMap<String, ExpCurveDefinition>) assetStore
                    .getAssetMap();

            if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
                RpgLogging.debugDeveloper("No exp curves loaded by asset system");
                return new HashMap<>();
            }

            Map<String, ExpCurveDefinition> expCurves = new HashMap<>(assetMap.getAssetMap());
            RpgLogging.debugDeveloper("Loaded %d exp curve configurations from asset system", expCurves.size());

            return expCurves;
        } catch (Exception e) {
            RpgLogging.debugDeveloper("Failed to load exp curves from asset system: %s", e.getMessage());
            return new HashMap<>();
        }
    }
}
