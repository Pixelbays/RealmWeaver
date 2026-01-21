package org.pixelbays.rpg.global.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;

/**
 * Utility class for loading RPG assets from Hytale asset packs.
 * Handles loading of class definitions and level system configurations
 * from JSON files in the asset pack structure.
 */
public class AssetLoader {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Load all ClassDefinition files from Server/Classes/*.json in all asset packs
     * 
     * @return Map of classId to ClassDefinition
     */
    @Nonnull
    public static Map<String, ClassDefinition> loadClassDefinitions() {
        Map<String, ClassDefinition> classDefinitions = new HashMap<>();

        LOGGER.atInfo().log("Loading class definitions from asset packs...");

        // Get all registered asset packs from AssetModule
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path classesDir = pack.getRoot().resolve("Server/Classes");

            if (!Files.exists(classesDir)) {
                LOGGER.at(Level.FINE).log("No Classes directory found in asset pack: " + pack.getName());
                continue;
            }

            try (Stream<Path> paths = Files.list(classesDir)) {
                paths.filter(path -> path.toString().endsWith(".json"))
                        .forEach(jsonFile -> {
                            try {
                                String jsonContent = Files.readString(jsonFile);
                                ClassDefinition classDef = GSON.fromJson(jsonContent, ClassDefinition.class);

                                if (classDef == null || classDef.getId() == null
                                        || classDef.getId().isEmpty()) {
                                    LOGGER.atWarning().log("Invalid or empty class definition in file: " + jsonFile);
                                    return;
                                }

                                classDefinitions.put(classDef.getId(), classDef);
                                LOGGER.atInfo().log("Loaded class definition: " + classDef.getId() +
                                        " from " + pack.getName());

                            } catch (JsonSyntaxException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to parse class definition from " + jsonFile + ": " + e.getMessage());
                            } catch (IOException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to read class definition file " + jsonFile + ": " + e.getMessage());
                            }
                        });

            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).log("Failed to list files in " + classesDir + ": " + e.getMessage());
            }
        }

        LOGGER.atInfo().log("Loaded " + classDefinitions.size() + " class definitions");
        return classDefinitions;
    }

    /**
     * Load all LevelSystemConfig files from Server/Entity/levels (recursive) in all
     * asset packs.
     * Subfolders are supported for organization.
     * 
     * @return Map of systemId to LevelSystemConfig
     */
    @Nonnull
    public static Map<String, LevelSystemConfig> loadLevelSystemConfigs() {
        Map<String, LevelSystemConfig> levelConfigs = new HashMap<>();

        Map<String, ExpCurveDefinition> expCurves = loadExpCurveConfigs();

        LOGGER.atInfo().log("Loading level system configurations from asset packs...");

        // Get all registered asset packs from AssetModule
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path levelsDir = pack.getRoot().resolve("Server/Entity/levels");

            if (!Files.exists(levelsDir)) {
                LOGGER.at(Level.FINE).log("No Entity/levels directory found in asset pack: " + pack.getName());
                continue;
            }

            try (Stream<Path> paths = Files.walk(levelsDir)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                        .forEach(jsonFile -> {
                            try {
                                String jsonContent = Files.readString(jsonFile);
                                LevelSystemConfig config = GSON.fromJson(jsonContent, LevelSystemConfig.class);

                                if (config == null || config.getSystemId() == null || config.getSystemId().isEmpty()) {
                                    LOGGER.atWarning().log("Invalid or empty level system config in file: " + jsonFile);
                                    return;
                                }

                                levelConfigs.put(config.getSystemId(), config);
                                LOGGER.atInfo().log("Loaded level system config: " + config.getSystemId() +
                                        " from " + pack.getName());

                            } catch (JsonSyntaxException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to parse level system config from " + jsonFile + ": " + e.getMessage());
                            } catch (IOException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to read level system config file " + jsonFile + ": " + e.getMessage());
                            }
                        });

            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).log("Failed to walk files in " + levelsDir + ": " + e.getMessage());
            }
        }

        // Resolve exp curve references before inheritance
        resolveExpCurveReferences(levelConfigs, expCurves);

        // Process inheritance - merge parent configs into child configs

        LOGGER.atInfo().log("Loaded " + levelConfigs.size() + " level system configurations");
        return levelConfigs;
    }

    /**
     * Load all ExpCurve files from Server/Entity/ExpCurves (recursive) in all asset
     * packs.
     * Subfolders are supported for organization.
     */
    @Nonnull
    private static Map<String, ExpCurveDefinition> loadExpCurveConfigs() {
        Map<String, ExpCurveDefinition> expCurves = new HashMap<>();

        LOGGER.atInfo().log("Loading exp curve configurations from asset packs...");

        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path curvesDir = pack.getRoot().resolve("Server/Entity/ExpCurves");

            if (!Files.exists(curvesDir)) {
                LOGGER.at(Level.FINE).log("No Entity/ExpCurves directory found in asset pack: " + pack.getName());
                continue;
            }

            try (Stream<Path> paths = Files.walk(curvesDir)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                        .forEach(jsonFile -> {
                            try {
                                String jsonContent = Files.readString(jsonFile);
                                ExpCurveDefinition curve = GSON.fromJson(jsonContent, ExpCurveDefinition.class);

                                if (curve == null) {
                                    LOGGER.atWarning().log("Invalid exp curve in file: " + jsonFile);
                                    return;
                                }

                                String fileName = jsonFile.getFileName().toString();
                                String keyNoExt = stripJsonExtension(fileName);
                                String relativeKey = stripJsonExtension(curvesDir.relativize(jsonFile).toString())
                                        .replace("\\", "/");

                                expCurves.putIfAbsent(keyNoExt, curve);
                                expCurves.putIfAbsent(relativeKey, curve);

                                LOGGER.atInfo().log("Loaded exp curve: " + keyNoExt + " from " + pack.getName());

                            } catch (JsonSyntaxException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to parse exp curve from " + jsonFile + ": " + e.getMessage());
                            } catch (IOException e) {
                                LOGGER.at(Level.SEVERE).log(
                                        "Failed to read exp curve file " + jsonFile + ": " + e.getMessage());
                            }
                        });

            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).log("Failed to walk files in " + curvesDir + ": " + e.getMessage());
            }
        }

        LOGGER.atInfo().log("Loaded " + expCurves.size() + " exp curve configurations");
        return expCurves;
    }

    /**
     * Resolve expCurveRef fields into concrete expCurve configs when possible.
     */
    private static void resolveExpCurveReferences(
            Map<String, LevelSystemConfig> levelConfigs,
            Map<String, ExpCurveDefinition> expCurves) {
        for (LevelSystemConfig config : levelConfigs.values()) {
            String ref = config.getExpCurveRef();
            if (ref == null || ref.isEmpty()) {
                continue;
            }

            if (config.getExpCurve() != null) {
                continue;
            }

            ExpCurveDefinition curve = expCurves.get(ref);
            if (curve == null) {
                curve = expCurves.get(stripJsonExtension(ref));
            }

            if (curve != null) {
                config.setExpCurve(curve);
                if (config.getExpCurveType() == null || config.getExpCurveType().isEmpty()) {
                    config.setExpCurveType(curve.getType());
                }
            } else {
                LOGGER.atWarning()
                        .log("Exp curve reference not found: " + ref + " for level system " + config.getSystemId());
            }
        }
    }

    private static String stripJsonExtension(String value) {
        if (value == null) {
            return null;
        }
        if (value.toLowerCase().endsWith(".json")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    /**
     * Load a single ClassDefinition from a specific file path
     * 
     * @param filePath Path to the JSON file
     * @return ClassDefinition or null if failed to load
     */
    public static ClassDefinition loadClassDefinition(@Nonnull Path filePath) {
        try {
            String jsonContent = Files.readString(filePath);
            ClassDefinition classDef = GSON.fromJson(jsonContent, ClassDefinition.class);

            if (classDef == null || classDef.getId() == null || classDef.getId().isEmpty()) {
                LOGGER.atWarning().log("Invalid or empty class definition in file: " + filePath);
                return null;
            }

            return classDef;

        } catch (JsonSyntaxException e) {
            LOGGER.at(Level.SEVERE).log("Failed to parse class definition from " + filePath + ": " + e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to read class definition file " + filePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Load a single LevelSystemConfig from a specific file path
     * 
     * @param filePath Path to the JSON file
     * @return LevelSystemConfig or null if failed to load
     */
    public static LevelSystemConfig loadLevelSystemConfig(@Nonnull Path filePath) {
        try {
            String jsonContent = Files.readString(filePath);
            LevelSystemConfig config = GSON.fromJson(jsonContent, LevelSystemConfig.class);

            if (config == null || config.getSystemId() == null || config.getSystemId().isEmpty()) {
                LOGGER.atWarning().log("Invalid or empty level system config in file: " + filePath);
                return null;
            }

            return config;

        } catch (JsonSyntaxException e) {
            LOGGER.at(Level.SEVERE).log("Failed to parse level system config from " + filePath + ": " + e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to read level system config file " + filePath + ": " + e.getMessage());
            return null;
        }
    }
}
