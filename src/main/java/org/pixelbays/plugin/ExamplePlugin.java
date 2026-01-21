package org.pixelbays.plugin;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.classes.command.ClassCommand;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.interaction.ForceTargetInteraction;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.leveling.command.ResetLevelCommand;
import org.pixelbays.rpg.leveling.command.TestLevelCommand;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.GenerateSchemaEvent;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * RPG Mod Plugin - Adds MMO/RPG progression systems to Hytale
 */
@SuppressWarnings("null")
public class ExamplePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ExamplePlugin instance;

    private LevelProgressionSystem levelSystem;
    private ClassManagementSystem classManagementSystem;
    private ClassAbilitySystem classAbilitySystem;
    private StatSystem statSystem;
    private RaceManagementSystem raceManagementSystem;

    private ComponentType<EntityStore, LevelProgressionComponent> levelProgressionComponentType;
    private ComponentType<EntityStore, ClassComponent> classComponentType;
    private ComponentType<EntityStore, ClassAbilityComponent> classAbilityComponentType;

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up RPG MOD plugin " + this.getName());

        // Register schema generation for RPG assets
        this.getEventRegistry().register(GenerateSchemaEvent.class, ExamplePlugin::onSchemaGenerate);

        // Register asset stores for classes and class abilities
        AssetRegistry.register(
                HytaleAssetStore.builder(ClassDefinition.class, new DefaultAssetMap<>())
                        .setPath("Classes")
                        .setCodec(ClassDefinition.CODEC)
                        .setKeyFunction(ClassDefinition::getId)
                        .build());
        AssetRegistry.register(
                HytaleAssetStore.builder(ClassAbilityDefinition.class, new DefaultAssetMap<>())
                        .setPath("RPGAbilities")
                        .setCodec(ClassAbilityDefinition.CODEC)
                        .setKeyFunction(ClassAbilityDefinition::getId)
                        .build());
        AssetRegistry.register(
                HytaleAssetStore.builder(RaceDefinition.class, new DefaultAssetMap<>())
                        .setPath("Races")
                        .setCodec(RaceDefinition.CODEC)
                        .setKeyFunction(RaceDefinition::getId)
                        .build());
        AssetRegistry.register(
                HytaleAssetStore.builder(LevelSystemConfig.class, new DefaultAssetMap<>())
                        .setPath("Entity/levels")
                        .setCodec(LevelSystemConfig.CODEC)
                        .setKeyFunction(LevelSystemConfig::getId)
                        .build());
        AssetRegistry.register(
                HytaleAssetStore.builder(ExpCurveDefinition.class, new DefaultAssetMap<>())
                        .setPath("Entity/ExpCurves")
                        .setCodec(ExpCurveDefinition.EXP_CURVE_CODEC)
                        .setKeyFunction(ExpCurveDefinition::getId)
                        .build());

        // Register components with ECS (with Codec for persistence)
        this.levelProgressionComponentType = this.getEntityStoreRegistry()
                .registerComponent(
                        LevelProgressionComponent.class,
                        "LevelProgression",
                        LevelProgressionComponent.CODEC);

        this.classComponentType = this.getEntityStoreRegistry()
                .registerComponent(
                        ClassComponent.class,
                        "Class",
                        ClassComponent.CODEC);

        this.classAbilityComponentType = this.getEntityStoreRegistry()
                .registerComponent(
                        ClassAbilityComponent.class,
                        "ClassAbility",
                        ClassAbilityComponent.CODEC);

        LOGGER.atInfo().log("Registered LevelProgressionComponent, ClassComponent, and ClassAbilityComponent");

        // Initialize level progression system
        this.levelSystem = new LevelProgressionSystem(this.getEventRegistry());

        // Initialize class/job systems
        this.classManagementSystem = new ClassManagementSystem(this.levelSystem);
        this.classAbilitySystem = new ClassAbilitySystem(this.classManagementSystem);
        this.statSystem = new StatSystem(this.classManagementSystem, this.levelSystem);
        this.classManagementSystem.setStatSystem(this.statSystem);
        this.levelSystem.setStatSystem(this.statSystem);

        // Initialize race systems
        this.raceManagementSystem = new RaceManagementSystem();

        // Load assets after the asset registry has finished loading
        this.getEventRegistry().register(LoadAssetEvent.class, this::onAssetsLoaded);
        this.getEventRegistry().register(LoadedAssetsEvent.class, ClassDefinition.class, this::onClassAssetsReload);
        this.getEventRegistry().register(RemovedAssetsEvent.class, ClassDefinition.class, this::onClassAssetsRemoved);
        this.getEventRegistry().register(LoadedAssetsEvent.class, ClassAbilityDefinition.class,
                this::onAbilityAssetsReload);
        this.getEventRegistry().register(RemovedAssetsEvent.class, ClassAbilityDefinition.class,
                this::onAbilityAssetsRemoved);
        this.getEventRegistry().register(LoadedAssetsEvent.class, RaceDefinition.class, this::onRaceAssetsReload);
        this.getEventRegistry().register(RemovedAssetsEvent.class, RaceDefinition.class, this::onRaceAssetsRemoved);
        this.getEventRegistry().register(LoadedAssetsEvent.class, LevelSystemConfig.class, this::onLevelAssetsReload);
        this.getEventRegistry().register(RemovedAssetsEvent.class, LevelSystemConfig.class, this::onLevelAssetsRemoved);
        this.getEventRegistry().register(LoadedAssetsEvent.class, ExpCurveDefinition.class,
                this::onExpCurveAssetsReload);
        this.getEventRegistry().register(RemovedAssetsEvent.class, ExpCurveDefinition.class,
                this::onExpCurveAssetsRemoved);

        // Register custom interactions for RPG abilities
        Interaction.CODEC.register("ForceTarget", ForceTargetInteraction.class, ForceTargetInteraction.FORCE_TARGET_CODEC);

        LOGGER.atInfo().log("Initialized Class/Job System with test data");

        // Register commands
        this.getCommandRegistry()
                .registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        this.getCommandRegistry().registerCommand(new TestLevelCommand());
        this.getCommandRegistry().registerCommand(new ResetLevelCommand());
        this.getCommandRegistry().registerCommand(new ClassCommand());

        LOGGER.atInfo().log("RPG MOD setup complete!");
    }

    @SuppressWarnings("unused")
    private void onAssetsLoaded(@Nonnull LoadAssetEvent event) {
        this.levelSystem.loadLevelSystemsFromAssets();
        this.classManagementSystem.loadClassDefinitionsFromAssets();
        this.classAbilitySystem.loadAbilityDefinitionsFromAssets();
        this.raceManagementSystem.loadRaceDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onClassAssetsReload(@Nonnull LoadedAssetsEvent<String, ClassDefinition, ?> event) {
        this.classManagementSystem.loadClassDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onClassAssetsRemoved(@Nonnull RemovedAssetsEvent<String, ClassDefinition, ?> event) {
        this.classManagementSystem.loadClassDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onAbilityAssetsReload(@Nonnull LoadedAssetsEvent<String, ClassAbilityDefinition, ?> event) {
        this.classAbilitySystem.loadAbilityDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onAbilityAssetsRemoved(@Nonnull RemovedAssetsEvent<String, ClassAbilityDefinition, ?> event) {
        this.classAbilitySystem.loadAbilityDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onRaceAssetsReload(@Nonnull LoadedAssetsEvent<String, RaceDefinition, ?> event) {
        this.raceManagementSystem.loadRaceDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onRaceAssetsRemoved(@Nonnull RemovedAssetsEvent<String, RaceDefinition, ?> event) {
        this.raceManagementSystem.loadRaceDefinitionsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onLevelAssetsReload(@Nonnull LoadedAssetsEvent<String, LevelSystemConfig, ?> event) {
        this.levelSystem.loadLevelSystemsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onLevelAssetsRemoved(@Nonnull RemovedAssetsEvent<String, LevelSystemConfig, ?> event) {
        this.levelSystem.loadLevelSystemsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onExpCurveAssetsReload(@Nonnull LoadedAssetsEvent<String, ExpCurveDefinition, ?> event) {
        this.levelSystem.loadLevelSystemsFromAssets();
    }

    @SuppressWarnings("unused")
    private void onExpCurveAssetsRemoved(@Nonnull RemovedAssetsEvent<String, ExpCurveDefinition, ?> event) {
        this.levelSystem.loadLevelSystemsFromAssets();
    }

    private static void onSchemaGenerate(@Nonnull GenerateSchemaEvent event) {
        addLevelSchemas(event);
        addClassSchemas(event);
        addRaceSchemas(event);
    }

    private static void addLevelSchemas(@Nonnull GenerateSchemaEvent event) {
        ObjectSchema levelSchema = new ObjectSchema();
        levelSchema.setTitle("RPG Level System Config");
        levelSchema.setId("RPG_LevelSystemConfig.json");
        Schema.HytaleMetadata levelMeta = levelSchema.getHytale();
        if (levelMeta != null) {
            levelMeta.setPath("Entity/levels");
            levelMeta.setExtension(".json");
        }
        levelSchema.setAdditionalProperties(true);
        event.addSchema("RPG_LevelSystemConfig.json", levelSchema);
        event.addSchemaLink("RPG_LevelSystemConfig",
                java.util.List.of("Entity/levels/*.json", "Entity/levels/**/*.json"), null);

        ObjectSchema curveSchema = new ObjectSchema();
        curveSchema.setTitle("RPG Exp Curve Config");
        curveSchema.setId("RPG_ExpCurveConfig.json");
        Schema.HytaleMetadata curveMeta = curveSchema.getHytale();
        if (curveMeta != null) {
            curveMeta.setPath("Entity/ExpCurves");
            curveMeta.setExtension(".json");
        }
        curveSchema.setAdditionalProperties(true);
        event.addSchema("RPG_ExpCurveConfig.json", curveSchema);
        event.addSchemaLink("RPG_ExpCurveConfig",
                java.util.List.of("Entity/ExpCurves/*.json", "Entity/ExpCurves/**/*.json"), null);
    }

    private static void addClassSchemas(@Nonnull GenerateSchemaEvent event) {
        ObjectSchema classSchema = new ObjectSchema();
        classSchema.setTitle("RPG Class Definition");
        classSchema.setId("RPG_ClassDefinition.json");
        Schema.HytaleMetadata classMeta = classSchema.getHytale();
        if (classMeta != null) {
            classMeta.setPath("Classes");
            classMeta.setExtension(".json");
        }
        classSchema.setAdditionalProperties(true);
        event.addSchema("RPG_ClassDefinition.json", classSchema);
        event.addSchemaLink("RPG_ClassDefinition", java.util.List.of("Classes/*.json", "Classes/**/*.json"), null);

        ObjectSchema abilitySchema = new ObjectSchema();
        abilitySchema.setTitle("RPG Class Ability Definition");
        abilitySchema.setId("RPG_ClassAbilityDefinition.json");
        Schema.HytaleMetadata abilityMeta = abilitySchema.getHytale();
        if (abilityMeta != null) {
            abilityMeta.setPath("RPGAbilities");
            abilityMeta.setExtension(".json");
        }
        abilitySchema.setAdditionalProperties(true);
        event.addSchema("RPG_ClassAbilityDefinition.json", abilitySchema);
        event.addSchemaLink("RPG_ClassAbilityDefinition", java.util.List.of(
                "RPGAbilities/*.json",
                "RPGAbilities/**/*.json"), null);
    }

    private static void addRaceSchemas(@Nonnull GenerateSchemaEvent event) {
        ObjectSchema raceSchema = new ObjectSchema();
        raceSchema.setTitle("RPG Race Definition");
        raceSchema.setId("RPG_RaceDefinition.json");
        Schema.HytaleMetadata raceMeta = raceSchema.getHytale();
        if (raceMeta != null) {
            raceMeta.setPath("Races");
            raceMeta.setExtension(".json");
        }
        raceSchema.setAdditionalProperties(true);
        event.addSchema("RPG_RaceDefinition.json", raceSchema);
        event.addSchemaLink("RPG_RaceDefinition", java.util.List.of("Races/*.json", "Races/**/*.json"), null);

    }

    @Nonnull
    public static ExamplePlugin get() {
        return instance;
    }

    @Nonnull
    public LevelProgressionSystem getLevelProgressionSystem() {
        return levelSystem;
    }

    @Nonnull
    public ClassManagementSystem getClassManagementSystem() {
        return classManagementSystem;
    }

    @Nonnull
    public ClassAbilitySystem getClassAbilitySystem() {
        return classAbilitySystem;
    }

    @Nonnull
    public StatSystem getStatSystem() {
        return statSystem;
    }

    @Nonnull
    public ComponentType<EntityStore, LevelProgressionComponent> getLevelProgressionComponentType() {
        return levelProgressionComponentType;
    }

    @Nonnull
    public ComponentType<EntityStore, ClassComponent> getClassComponentType() {
        return classComponentType;
    }

    @Nonnull
    public ComponentType<EntityStore, ClassAbilityComponent> getClassAbilityComponentType() {
        return classAbilityComponentType;
    }
}