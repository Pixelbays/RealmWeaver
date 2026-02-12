package org.pixelbays.plugin;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.ability.command.BindAbilityCommand;
import org.pixelbays.rpg.ability.command.SyncHotbarCommand;
import org.pixelbays.rpg.ability.command.UnlockAbilityCommand;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.AbilityEmpowerComponent;
import org.pixelbays.rpg.ability.component.AbilityTriggerBlockComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.event.AbilityTriggerFailedEvent;
import org.pixelbays.rpg.ability.event.AbilityTriggeredEvent;
import org.pixelbays.rpg.ability.event.BlockAbilityTriggerEvent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;
import org.pixelbays.rpg.ability.config.AbilityCategory;
import org.pixelbays.rpg.ability.config.AbilityQuality;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.interaction.EmpowerAbilityInteraction;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.ability.system.HotbarAbilityIconManager;
import org.pixelbays.rpg.camera.RpgCameraController;
import org.pixelbays.rpg.classes.command.ClassCommand;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.event.ActiveClassChangedEvent;
import org.pixelbays.rpg.classes.event.ClassLearnedEvent;
import org.pixelbays.rpg.classes.event.ClassUnlearnedEvent;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.event.ClassStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.RaceStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.StatGrowthAppliedEvent;
import org.pixelbays.rpg.global.event.StatIncreasesAppliedEvent;
import org.pixelbays.rpg.global.input.AbilityInputFilter;
import org.pixelbays.rpg.global.interaction.DiceRollInteraction;
import org.pixelbays.rpg.global.interaction.ForceTargetInteraction;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.system.XpDeathDropSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.command.GuildCommand;
import org.pixelbays.rpg.guild.config.GuildData;
import org.pixelbays.rpg.guild.event.GuildApplicationAcceptedEvent;
import org.pixelbays.rpg.guild.event.GuildApplicationDeniedEvent;
import org.pixelbays.rpg.guild.event.GuildApplicationSubmittedEvent;
import org.pixelbays.rpg.guild.event.GuildCreatedEvent;
import org.pixelbays.rpg.guild.event.GuildDisbandedEvent;
import org.pixelbays.rpg.guild.event.GuildInviteSentEvent;
import org.pixelbays.rpg.guild.event.GuildJoinedEvent;
import org.pixelbays.rpg.guild.event.GuildJoinPolicyChangedEvent;
import org.pixelbays.rpg.guild.event.GuildLeaderChangedEvent;
import org.pixelbays.rpg.guild.event.GuildLeftEvent;
import org.pixelbays.rpg.guild.event.GuildMemberKickedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleAssignedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleCreatedEvent;
import org.pixelbays.rpg.guild.event.GuildRolePermissionChangedEvent;
import org.pixelbays.rpg.item.system.EquipmentRestrictions;
import org.pixelbays.rpg.inventory.input.InventoryOpenFilter;
import org.pixelbays.rpg.inventory.system.InventoryHandlingSystem;
import org.pixelbays.rpg.leveling.command.LevelTestCommand;
import org.pixelbays.rpg.leveling.command.ResetLevelCommand;
import org.pixelbays.rpg.leveling.command.TestLevelCommand;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;
import org.pixelbays.rpg.leveling.config.ExpItemDropContainer;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;
import org.pixelbays.rpg.leveling.event.LevelRewardsAppliedEvent;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;
import org.pixelbays.rpg.leveling.handlers.GiveXPHandler;
import org.pixelbays.rpg.leveling.handlers.LevelUpHandler;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.leveling.system.RestedXpSystem;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.command.PartyCommand;
import org.pixelbays.rpg.party.config.PartyData;
import org.pixelbays.rpg.party.event.PartyAssistantPromotedEvent;
import org.pixelbays.rpg.party.event.PartyCreatedEvent;
import org.pixelbays.rpg.party.event.PartyDisbandedEvent;
import org.pixelbays.rpg.party.event.PartyInviteSentEvent;
import org.pixelbays.rpg.party.event.PartyJoinedEvent;
import org.pixelbays.rpg.party.event.PartyLeaderChangedEvent;
import org.pixelbays.rpg.party.event.PartyLeftEvent;
import org.pixelbays.rpg.party.event.PartyMemberKickedEvent;
import org.pixelbays.rpg.race.command.RaceCommand;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.event.RaceAbilityUnlockedEvent;
import org.pixelbays.rpg.race.event.RaceChangedEvent;
import org.pixelbays.rpg.race.system.RaceManagementSystem;
import org.pixelbays.rpg.race.system.RaceSystem;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * RPG Mod Plugin - Adds MMO/RPG progression systems to Hytale
 */
@SuppressWarnings("null")
public class ExamplePlugin extends JavaPlugin {

        private static ExamplePlugin instance;

        private LevelProgressionSystem levelSystem;
        private ClassManagementSystem classManagementSystem;
        private ClassAbilitySystem classAbilitySystem;
        private HotbarAbilityIconManager hotbarIconManager;
        private StatSystem statSystem;
        private RaceManagementSystem raceManagementSystem;
        private RaceSystem raceSystem;
        @SuppressWarnings("unused")
        private RestedXpSystem restedXpSystem;
        private EquipmentRestrictions equipmentRestrictions;
        private PartyManager partyManager;
        private GuildManager guildManager;

        private ComponentType<EntityStore, LevelProgressionComponent> levelProgressionComponentType;
        private ComponentType<EntityStore, ClassComponent> classComponentType;
        private ComponentType<EntityStore, ClassAbilityComponent> classAbilityComponentType;
        private ComponentType<EntityStore, AbilityEmpowerComponent> abilityEmpowerComponentType;
        private ComponentType<EntityStore, RaceComponent> raceComponentType;
        private ComponentType<EntityStore, AbilityBindingComponent> abilityBindingComponentType;
        private ComponentType<EntityStore, AbilityTriggerBlockComponent> abilityTriggerBlockComponentType;

        private PacketFilter abilityInputFilter;
        private PacketFilter inventoryOpenFilter;

        public ExamplePlugin(@Nonnull JavaPluginInit init) {
                super(init);
                instance = this;
                RpgLogging.debugDeveloper("Hello from %s version %s", this.getName(),
                                this.getManifest().getVersion().toString());
        }

        @Override
        protected void setup() {
                RpgLogging.debugDeveloper("Setting up RPG MOD plugin %s", this.getName());

                // Register asset stores for classes and class abilities
                AssetRegistry.register(
                                HytaleAssetStore.builder(RpgModConfig.class, new DefaultAssetMap<>())
                                        .setPath("RpgModConfig")
                                                .setCodec(RpgModConfig.CODEC)
                                                .setKeyFunction(RpgModConfig::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(PartyData.class, new DefaultAssetMap<>())
                                                .setPath("PartyData")
                                                .setCodec(PartyData.CODEC)
                                                .setKeyFunction(PartyData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(GuildData.class, new DefaultAssetMap<>())
                                                .setPath("GuildData")
                                                .setCodec(GuildData.CODEC)
                                                .setKeyFunction(GuildData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(ExpCurveDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Entity/ExpCurves")
                                                .setCodec(ExpCurveDefinition.EXP_CURVE_CODEC)
                                                .setKeyFunction(ExpCurveDefinition::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(LevelSystemConfig.class, new DefaultAssetMap<>())
                                                .setPath("Entity/levels")
                                                .setCodec(LevelSystemConfig.CODEC)
                                                .setKeyFunction(LevelSystemConfig::getId)
                                                .loadsAfter(ExpCurveDefinition.class) // Ensure exp curves load first
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(ClassDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Classes")
                                                .setCodec(ClassDefinition.CODEC)
                                                .setKeyFunction(ClassDefinition::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(AbilityQuality.class,
                                                new IndexedLookupTableAssetMap<>(AbilityQuality[]::new))
                                                .setPath("Abilities/Qualities")
                                                .setCodec(AbilityQuality.CODEC)
                                                .setKeyFunction(AbilityQuality::getId)
                                                .setReplaceOnRemove(key -> AbilityQuality.DEFAULT_ABILITY_QUALITY)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(AbilityCategory.class, new DefaultAssetMap<>())
                                                .setPath("Abilities/Categories")
                                                .setCodec(AbilityCategory.CODEC)
                                                .setKeyFunction(AbilityCategory::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(ClassAbilityDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Abilities/Abilities")
                                                .setCodec(ClassAbilityDefinition.CODEC)
                                                .setKeyFunction(ClassAbilityDefinition::getId)
                                                .loadsAfter(AbilityQuality.class) // Ensure qualities load first
                                                .loadsAfter(AbilityCategory.class) // Ensure categories load first
                                                .loadsAfter(RootInteraction.class) // Ensure categories load first
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(RaceDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Races")
                                                .setCodec(RaceDefinition.CODEC)
                                                .setKeyFunction(RaceDefinition::getId)
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

                this.abilityEmpowerComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                AbilityEmpowerComponent.class,
                                                "AbilityEmpower",
                                                AbilityEmpowerComponent.CODEC);

                this.raceComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                RaceComponent.class,
                                                "Race",
                                                RaceComponent.CODEC);

                this.abilityBindingComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                AbilityBindingComponent.class,
                                                "AbilityBinding",
                                                AbilityBindingComponent.CODEC);
                AbilityBindingComponent.setComponentType(this.abilityBindingComponentType);

                this.abilityTriggerBlockComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                AbilityTriggerBlockComponent.class,
                                                "AbilityTriggerBlock",
                                                AbilityTriggerBlockComponent.CODEC);

                RpgLogging.debugDeveloper(
                                "Registered LevelProgressionComponent, ClassComponent, ClassAbilityComponent, AbilityEmpowerComponent, RaceComponent, AbilityBindingComponent, and AbilityTriggerBlockComponent");

                this.getEventRegistry().register(GiveXPEvent.class, new GiveXPHandler());
                this.getEventRegistry().register(LevelUpEvent.class, new LevelUpHandler());
                this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, new RpgCameraController());
                new InventoryHandlingSystem().register(this.getEventRegistry());
                registerRpgEventHooks();
                // Initialize level progression system
                this.levelSystem = new LevelProgressionSystem(this.getEventRegistry());
                this.restedXpSystem = new RestedXpSystem(this.getEventRegistry());

                // Initialize race systems (before class/stat systems since they depend on it)
                this.raceManagementSystem = new RaceManagementSystem();

                // Initialize class/job systems
                this.classManagementSystem = new ClassManagementSystem(this.levelSystem);
                this.classAbilitySystem = new ClassAbilitySystem(this.classManagementSystem);
                this.hotbarIconManager = new HotbarAbilityIconManager();
                this.statSystem = new StatSystem(this.classManagementSystem, this.raceManagementSystem,
                                this.levelSystem,
                                this.getEventRegistry());
                this.classManagementSystem.setStatSystem(this.statSystem);
                this.levelSystem.setStatSystem(this.statSystem);

                this.partyManager = new PartyManager();
                this.guildManager = new GuildManager();

                // Register passive ability ticking system (for passive and toggle abilities)
                this.getEntityStoreRegistry().registerSystem(
                                new org.pixelbays.rpg.ability.system.PassiveAbilityTickingSystem(
                                                this.classComponentType,
                                                this.classAbilityComponentType,
                                                this.classAbilitySystem));
                RpgLogging.debugDeveloper("Registered PassiveAbilityTickingSystem for passive and toggle abilities");

                // Initialize remaining race systems
                this.raceSystem = new RaceSystem(this.raceManagementSystem);
                this.raceSystem.setStatSystem(this.statSystem);
                this.classManagementSystem.setRaceSystem(this.raceSystem);

                // Register NPC death XP drop system
                this.getEntityStoreRegistry().registerSystem(new XpDeathDropSystem());


                // Register hardcore death handler
                this.getEntityStoreRegistry().registerSystem(new org.pixelbays.rpg.leveling.handlers.HardcoreHandler());

                // Register custom interactions for RPG abilities
                Interaction.CODEC.register("ForceTarget", ForceTargetInteraction.class,
                                ForceTargetInteraction.FORCE_TARGET_CODEC);
                Interaction.CODEC.register("DiceRoll", DiceRollInteraction.class,
                                DiceRollInteraction.DICE_ROLL_CODEC);
                Interaction.CODEC.register("EmpowerAbility", EmpowerAbilityInteraction.class,
                                EmpowerAbilityInteraction.CODEC);

                // Register custom item drop containers
                ItemDropContainer.CODEC.register("Exp", ExpItemDropContainer.class, ExpItemDropContainer.CODEC);

                // Register equipment restriction system
                this.equipmentRestrictions = new EquipmentRestrictions(this.classManagementSystem);
                this.equipmentRestrictions.register(this.getEventRegistry());

                // Register class level milestone rewards listener
                new org.pixelbays.rpg.classes.integration.LevelMilestone()
                                .register(this.getEventRegistry());

                RpgLogging.debugDeveloper("Initialized Class/Job System with test data");

                // Register commands
                this.getCommandRegistry().registerCommand(new TestLevelCommand());
                this.getCommandRegistry().registerCommand(new LevelTestCommand());
                this.getCommandRegistry().registerCommand(new ResetLevelCommand());
                this.getCommandRegistry().registerCommand(new ClassCommand());
                this.getCommandRegistry().registerCommand(new RaceCommand());
                this.getCommandRegistry().registerCommand(new PartyCommand());
                this.getCommandRegistry().registerCommand(new GuildCommand());
                this.getCommandRegistry().registerCommand(new BindAbilityCommand());
                this.getCommandRegistry().registerCommand(new SyncHotbarCommand());
                this.getCommandRegistry().registerCommand(new UnlockAbilityCommand());

                this.partyManager.loadFromAssets();
                this.guildManager.loadFromAssets();

                // Register ability input packet filter
                AbilityInputFilter abilityFilter = new AbilityInputFilter(this);
                this.abilityInputFilter = PacketAdapters.registerInbound(abilityFilter);
                RpgLogging.debugDeveloper("Registered ability input packet filter");

                InventoryOpenFilter inventoryFilter = new InventoryOpenFilter();
                this.inventoryOpenFilter = PacketAdapters.registerInbound(inventoryFilter);
                RpgModConfig inventoryConfig = org.pixelbays.rpg.inventory.input.InventoryOpenFilter
                                .resolveConfigForLog();
                RpgLogging.debugDeveloper(
                                "Registered inventory open packet filter (handling=%s)",
                                inventoryConfig != null ? inventoryConfig.getInventoryHandling() : "unknown");

                RpgLogging.debugDeveloper("RPG MOD setup complete!");
        }

        @Override
        protected void shutdown() {
                // Deregister packet filter
                if (this.abilityInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.abilityInputFilter);
                        RpgLogging.debugDeveloper("Deregistered ability input packet filter");
                }
                if (this.inventoryOpenFilter != null) {
                        PacketAdapters.deregisterInbound(this.inventoryOpenFilter);
                        RpgLogging.debugDeveloper("Deregistered inventory open packet filter");
                }
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
        public HotbarAbilityIconManager getHotbarIconManager() {
                return hotbarIconManager;
        }

        @Nonnull
        public StatSystem getStatSystem() {
                return statSystem;
        }

        @Nonnull
        public RaceManagementSystem getRaceManagementSystem() {
                return raceManagementSystem;
        }

        @Nonnull
        public RaceSystem getRaceSystem() {
                return raceSystem;
        }

        @Nonnull
        public PartyManager getPartyManager() {
                return partyManager;
        }

        @Nonnull
        public GuildManager getGuildManager() {
                return guildManager;
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

        @Nonnull
        public ComponentType<EntityStore, AbilityEmpowerComponent> getAbilityEmpowerComponentType() {
                return abilityEmpowerComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, RaceComponent> getRaceComponentType() {
                return raceComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, AbilityBindingComponent> getAbilityBindingComponentType() {
                return abilityBindingComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, AbilityTriggerBlockComponent> getAbilityTriggerBlockComponentType() {
                return abilityTriggerBlockComponentType;
        }

        private void registerRpgEventHooks() {
                registerNoop(ActiveClassChangedEvent.class);
                registerNoop(ClassLearnedEvent.class);
                registerNoop(ClassUnlearnedEvent.class);
                registerNoop(RaceChangedEvent.class);
                registerNoop(RaceAbilityUnlockedEvent.class);
                registerNoop(AbilityTriggeredEvent.class);
                registerNoop(AbilityTriggerFailedEvent.class);
                registerNoop(BlockAbilityTriggerEvent.class);
                registerNoop(ClassAbilityUnlockedEvent.class);
                registerNoop(StatIncreasesAppliedEvent.class);
                registerNoop(StatGrowthAppliedEvent.class);
                registerNoop(ClassStatBonusesRecalculatedEvent.class);
                registerNoop(RaceStatBonusesRecalculatedEvent.class);
                registerNoop(LevelRewardsAppliedEvent.class);
                registerNoop(PartyCreatedEvent.class);
                registerNoop(PartyInviteSentEvent.class);
                registerNoop(PartyJoinedEvent.class);
                registerNoop(PartyLeftEvent.class);
                registerNoop(PartyDisbandedEvent.class);
                registerNoop(PartyMemberKickedEvent.class);
                registerNoop(PartyLeaderChangedEvent.class);
                registerNoop(PartyAssistantPromotedEvent.class);
                registerNoop(GuildCreatedEvent.class);
                registerNoop(GuildInviteSentEvent.class);
                registerNoop(GuildJoinedEvent.class);
                registerNoop(GuildLeftEvent.class);
                registerNoop(GuildDisbandedEvent.class);
                registerNoop(GuildMemberKickedEvent.class);
                registerNoop(GuildLeaderChangedEvent.class);
                registerNoop(GuildJoinPolicyChangedEvent.class);
                registerNoop(GuildRoleCreatedEvent.class);
                registerNoop(GuildRolePermissionChangedEvent.class);
                registerNoop(GuildRoleAssignedEvent.class);
                registerNoop(GuildApplicationSubmittedEvent.class);
                registerNoop(GuildApplicationAcceptedEvent.class);
                registerNoop(GuildApplicationDeniedEvent.class);
        }

        private <T extends com.hypixel.hytale.event.IBaseEvent<Void>> void registerNoop(Class<T> eventType) {
                this.getEventRegistry().register(eventType, event -> {
                });
        }

}