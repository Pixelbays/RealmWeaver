package org.pixelbays.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.command.BindAbilityCommand;
import org.pixelbays.rpg.ability.command.SpellbookCommand;
import org.pixelbays.rpg.ability.command.SyncHotbarCommand;
import org.pixelbays.rpg.ability.command.UnlockAbilityCommand;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.AbilityEmpowerComponent;
import org.pixelbays.rpg.ability.component.AbilityTriggerBlockComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.AbilityCategory;
import org.pixelbays.rpg.ability.config.AbilityQuality;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.event.AbilityTriggerFailedEvent;
import org.pixelbays.rpg.ability.event.AbilityTriggeredEvent;
import org.pixelbays.rpg.ability.event.BlockAbilityTriggerEvent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;
import org.pixelbays.rpg.ability.interaction.EmpowerAbilityInteraction;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.ability.system.HotbarAbilityIconManager;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.achievement.config.AchievementDefinition;
import org.pixelbays.rpg.achievement.event.AchievementUnlockedEvent;
import org.pixelbays.rpg.achievement.system.AchievementSystem;
import org.pixelbays.rpg.camera.RpgCameraController;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.command.CharacterCommand;
import org.pixelbays.rpg.character.config.CharacterRosterData;
import org.pixelbays.rpg.character.input.CharacterLobbyInputFilter;
import org.pixelbays.rpg.character.token.CharacterTokenDefinition;
import org.pixelbays.rpg.chat.ChatManager;
import org.pixelbays.rpg.chat.GuildChatChannel;
import org.pixelbays.rpg.chat.PartyChatChannel;
import org.pixelbays.rpg.chat.command.ChatCommand;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;
import org.pixelbays.rpg.chat.config.settings.ChatModSettings;
import org.pixelbays.rpg.classes.command.ClassCommand;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.event.ActiveClassChangedEvent;
import org.pixelbays.rpg.classes.event.ClassLearnedEvent;
import org.pixelbays.rpg.classes.event.ClassUnlearnedEvent;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.classes.talent.TalentSystem;
import org.pixelbays.rpg.economy.auctions.AuctionHouseManager;
import org.pixelbays.rpg.economy.auctions.config.AuctionData;
import org.pixelbays.rpg.economy.banks.BankManager;
import org.pixelbays.rpg.economy.banks.command.BankCommand;
import org.pixelbays.rpg.economy.banks.config.BankData;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;
import org.pixelbays.rpg.economy.banks.interaction.OpenBankInteraction;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.CurrencyWalletData;
import org.pixelbays.rpg.economy.currency.command.CurrencyCommand;
import org.pixelbays.rpg.economy.currency.config.CurrencyItemDropContainer;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.event.GiveCurrencyEvent;
import org.pixelbays.rpg.economy.currency.handler.GiveCurrencyHandler;
import org.pixelbays.rpg.economy.currency.interaction.ModifyCurrencyInteraction;
import org.pixelbays.rpg.economy.currency.system.CurrencyDeathDropSystem;
import org.pixelbays.rpg.expansion.ExpansionManager;
import org.pixelbays.rpg.expansion.ExpansionUnlockData;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.event.ClassStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.RaceStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.StatGrowthAppliedEvent;
import org.pixelbays.rpg.global.event.StatIncreasesAppliedEvent;
import org.pixelbays.rpg.global.input.AbilityInputFilter;
import org.pixelbays.rpg.global.input.UiInputFilter;
import org.pixelbays.rpg.global.interaction.DiceRollInteraction;
import org.pixelbays.rpg.global.interaction.ForceTargetInteraction;
import org.pixelbays.rpg.global.interaction.GrantAchievementProgressInteraction;
import org.pixelbays.rpg.global.interaction.PrerequisiteCheckInteraction;
import org.pixelbays.rpg.global.interaction.SendMailInteraction;
import org.pixelbays.rpg.global.interaction.UnlockAchievementInteraction;
import org.pixelbays.rpg.global.interaction.UnlockClassInteraction;
import org.pixelbays.rpg.global.interaction.UnlockExpansionInteraction;
import org.pixelbays.rpg.global.interaction.UnlockRaceInteraction;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.system.XpDeathDropSystem;
import org.pixelbays.rpg.global.config.BuildFlags;
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
import org.pixelbays.rpg.guild.event.GuildJoinPolicyChangedEvent;
import org.pixelbays.rpg.guild.event.GuildJoinedEvent;
import org.pixelbays.rpg.guild.event.GuildLeaderChangedEvent;
import org.pixelbays.rpg.guild.event.GuildLeftEvent;
import org.pixelbays.rpg.guild.event.GuildMemberKickedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleAssignedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleCreatedEvent;
import org.pixelbays.rpg.guild.event.GuildRolePermissionChangedEvent;
import org.pixelbays.rpg.hud.PlayerHudService;
import org.pixelbays.rpg.hud.PlayerHudUpdateSystem;
import org.pixelbays.rpg.inventory.input.InventoryOpenFilter;
import org.pixelbays.rpg.inventory.system.InventoryHandlingSystem;
import org.pixelbays.rpg.item.config.RandomizedEquipmentDefinition;
import org.pixelbays.rpg.item.system.EquipmentRestrictions;
import org.pixelbays.rpg.item.system.RandomizedEquipmentManager;
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
import org.pixelbays.rpg.lockpicking.component.LockpickingSessionComponent;
import org.pixelbays.rpg.lockpicking.input.LockpickingInputFilter;
import org.pixelbays.rpg.lockpicking.interaction.LockpickInteraction;
import org.pixelbays.rpg.lockpicking.system.LockpickingSystem;
import org.pixelbays.rpg.mail.MailManager;
import org.pixelbays.rpg.mail.command.MailCommand;
import org.pixelbays.rpg.mail.config.MailData;
import org.pixelbays.rpg.mail.interaction.OpenMailInteraction;
import org.pixelbays.rpg.npc.command.NpcRpgDebugCommand;
import org.pixelbays.rpg.npc.component.NpcRpgDebugComponent;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent;
import org.pixelbays.rpg.npc.component.NpcThreatComponent;
import org.pixelbays.rpg.npc.corecomponents.builders.BuilderActionRpgCastAbility;
import org.pixelbays.rpg.npc.corecomponents.builders.BuilderActionRpgSetup;
import org.pixelbays.rpg.npc.system.NpcRpgDebugOverlaySystem;
import org.pixelbays.rpg.npc.system.NpcRpgSetupSystem;
import org.pixelbays.rpg.npc.system.NpcThreatDamageSystem;
import org.pixelbays.rpg.npc.system.NpcThreatMaintenanceSystem;
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
import org.pixelbays.rpg.party.finder.GroupFinderManager;
import org.pixelbays.rpg.race.command.RaceCommand;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.event.RaceAbilityUnlockedEvent;
import org.pixelbays.rpg.race.event.RaceChangedEvent;
import org.pixelbays.rpg.race.system.RaceManagementSystem;
import org.pixelbays.rpg.race.system.RaceSystem;
import org.pixelbays.rpg.world.WorldTravelManager;
import org.pixelbays.rpg.world.command.TravelRouteCommand;
import org.pixelbays.rpg.world.config.WorldRouteDefinition;
import org.pixelbays.rpg.world.interaction.TravelRouteInteraction;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * Realmweaver - Adds data-driven MMO/RPG progression systems to Hytale.
 */
@SuppressWarnings("null")
public class Realmweavers extends JavaPlugin {

        private static Realmweavers instance;

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
        private RandomizedEquipmentManager randomizedEquipmentManager;
        private PartyManager partyManager;
        private GroupFinderManager groupFinderManager;
        private GuildManager guildManager;
        private AuctionHouseManager auctionHouseManager;
        private BankManager bankManager;
        private CurrencyManager currencyManager;
        private ExpansionManager expansionManager;
        private MailManager mailManager;
        private CharacterManager characterManager;
        private TalentSystem talentSystem;
        private AchievementSystem achievementSystem;
        private ChatManager chatManager;
        private PlayerHudService playerHudService;
        private WorldTravelManager worldTravelManager;

        private ComponentType<EntityStore, AchievementComponent> achievementComponentType;
        private ComponentType<EntityStore, LevelProgressionComponent> levelProgressionComponentType;
        private ComponentType<EntityStore, ClassComponent> classComponentType;
        private ComponentType<EntityStore, ClassAbilityComponent> classAbilityComponentType;
        private ComponentType<EntityStore, AbilityEmpowerComponent> abilityEmpowerComponentType;
        private ComponentType<EntityStore, RaceComponent> raceComponentType;
        private ComponentType<EntityStore, AbilityBindingComponent> abilityBindingComponentType;
        private ComponentType<EntityStore, AbilityTriggerBlockComponent> abilityTriggerBlockComponentType;
        private ComponentType<EntityStore, NpcRpgDebugComponent> npcRpgDebugComponentType;
        private ComponentType<EntityStore, NpcRpgSetupComponent> npcRpgSetupComponentType;
        private ComponentType<EntityStore, NpcThreatComponent> npcThreatComponentType;
        private ComponentType<EntityStore, LockpickingSessionComponent> lockpickingSessionComponentType;

        private PacketFilter uiInputFilter;
        private PacketFilter abilityInputFilter;
        private PacketFilter characterLobbyInputFilter;
        private PacketFilter inventoryOpenFilter;
        private PacketFilter lockpickingInputFilter;
        private LockpickingSystem lockpickingSystem;
        private CommandRegistration raceCommandRegistration;
        private CommandRegistration npcRpgDebugCommandRegistration;
        private CommandRegistration testLevelCommandRegistration;
        private CommandRegistration levelTestCommandRegistration;
        private CommandRegistration resetLevelCommandRegistration;
        private CommandRegistration classCommandRegistration;
        private CommandRegistration partyCommandRegistration;
        private CommandRegistration guildCommandRegistration;
        private CommandRegistration characterCommandRegistration;
        private CommandRegistration bankCommandRegistration;
        private CommandRegistration currencyCommandRegistration;
        private CommandRegistration mailCommandRegistration;
        private CommandRegistration chatCommandRegistration;
        private CommandRegistration bindAbilityCommandRegistration;
        private CommandRegistration spellbookCommandRegistration;
        private CommandRegistration syncHotbarCommandRegistration;
        private CommandRegistration unlockAbilityCommandRegistration;
        private CommandRegistration travelRouteCommandRegistration;

        public Realmweavers(@Nonnull JavaPluginInit init) {
                super(init);
                instance = this;
                RpgLogging.debugDeveloper("Hello from %s version %s", this.getName(),
                                this.getManifest().getVersion().toString());
        }

        @Override
        protected void setup() {
                RpgLogging.debugDeveloper("Setting up Realmweaver plugin %s", this.getName());

                registerAssetStores();
                registerComponents();
                registerConfigReloadHooks();

                RpgModConfig config = resolveModConfig();
                ModuleFlags moduleFlags = resolveModuleFlags(config);
                logModuleEnablement(moduleFlags);

                registerCoreEventHandlers();
                initializeProgressionSystems();
                registerHudSystems();
                initializeGameplayManagers();
                registerPlayerLifecycleHandlers();
                registerGameplaySystems();
                registerNpcSystems();
                registerCustomCodecs();
                registerOptionalSystems(moduleFlags);
                registerAlwaysAvailableCommands();
                reconcileDynamicRegistrations(config);

                RpgLogging.debugDeveloper("Realmweaver setup complete!");
        }

        private void registerAssetStores() {

                // Register asset stores for classes and class abilities
                AssetRegistry.register(
                                HytaleAssetStore.builder(RpgModConfig.class, new DefaultAssetMap<>())
                                        .setPath("RpgModConfig")
                                                .setCodec(RpgModConfig.CODEC)
                                                .setKeyFunction(RpgModConfig::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(WorldRouteDefinition.class, new DefaultAssetMap<>())
                                                .setPath("WorldRoutes")
                                                .setCodec(WorldRouteDefinition.CODEC)
                                                .setKeyFunction(WorldRouteDefinition::getId)
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
                                HytaleAssetStore.builder(AuctionData.class, new DefaultAssetMap<>())
                                                .setPath("AuctionData")
                                                .setCodec(AuctionData.CODEC)
                                                .setKeyFunction(AuctionData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(BankData.class, new DefaultAssetMap<>())
                                                .setPath("BankData")
                                                .setCodec(BankData.CODEC)
                                                .setKeyFunction(BankData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(BankTypeDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Banks/Types")
                                                .setCodec(BankTypeDefinition.CODEC)
                                                .setKeyFunction(BankTypeDefinition::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(CurrencyWalletData.class, new DefaultAssetMap<>())
                                                .setPath("CurrencyData")
                                                .setCodec(CurrencyWalletData.CODEC)
                                                .setKeyFunction(CurrencyWalletData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(ExpansionUnlockData.class, new DefaultAssetMap<>())
                                                .setPath("ExpansionData")
                                                .setCodec(ExpansionUnlockData.CODEC)
                                                .setKeyFunction(ExpansionUnlockData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(MailData.class, new DefaultAssetMap<>())
                                                .setPath("MailData")
                                                .setCodec(MailData.CODEC)
                                                .setKeyFunction(MailData::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(CurrencyTypeDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Currencies/Types")
                                                .setCodec(CurrencyTypeDefinition.CODEC)
                                                .setKeyFunction(CurrencyTypeDefinition::getId)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(AchievementDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Achievements")
                                                .setCodec(AchievementDefinition.CODEC)
                                                .setKeyFunction(AchievementDefinition::getId)
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
                                                .loadsAfter(EntityStatType.class)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(RandomizedEquipmentDefinition.class, new DefaultAssetMap<>())
                                                .setPath("Item/RandomizedEquipment")
                                                .setCodec(RandomizedEquipmentDefinition.CODEC)
                                                .setKeyFunction(RandomizedEquipmentDefinition::getId)
                                                .loadsAfter(EntityStatType.class)
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
                AssetRegistry.register(
                                HytaleAssetStore.builder(CharacterRosterData.class, new DefaultAssetMap<>())
                                                .setPath("CharacterData")
                                                .setCodec(CharacterRosterData.CODEC)
                                                .setKeyFunction(CharacterRosterData::getId)
                                                .loadsAfter(EntityStatType.class)
                                                .build());
                AssetRegistry.register(
                                HytaleAssetStore.builder(CharacterTokenDefinition.class, new DefaultAssetMap<>())
                                                .setPath("CharacterTokens")
                                                .setCodec(CharacterTokenDefinition.CODEC)
                                                .setKeyFunction(CharacterTokenDefinition::getId)
                                                .build());
        }

        private void registerComponents() {

                // Register components with ECS (with Codec for persistence)
                this.levelProgressionComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                LevelProgressionComponent.class,
                                                "LevelProgression",
                                                LevelProgressionComponent.CODEC);

                this.achievementComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                AchievementComponent.class,
                                                "AchievementProgress",
                                                AchievementComponent.CODEC);

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

                this.lockpickingSessionComponentType = this.getEntityStoreRegistry()
                                .registerComponent(
                                                LockpickingSessionComponent.class,
                                                "LockpickingSession",
                                                LockpickingSessionComponent.CODEC);

                if (BuildFlags.NPC_MODULE) {
                        this.npcRpgDebugComponentType = this.getEntityStoreRegistry()
                                        .registerComponent(
                                                        NpcRpgDebugComponent.class,
                                                        "NpcRpgDebug",
                                                        NpcRpgDebugComponent.CODEC);

                        this.npcRpgSetupComponentType = this.getEntityStoreRegistry()
                                        .registerComponent(
                                                        NpcRpgSetupComponent.class,
                                                        "NpcRpgSetup",
                                                        NpcRpgSetupComponent.CODEC);

                        this.npcThreatComponentType = this.getEntityStoreRegistry()
                                        .registerComponent(
                                                        NpcThreatComponent.class,
                                                        "NpcThreat",
                                                        NpcThreatComponent.CODEC);
                }

                RpgLogging.debugDeveloper(
                                "Registered LevelProgressionComponent, ClassComponent, ClassAbilityComponent, AbilityEmpowerComponent, RaceComponent, AbilityBindingComponent, and AbilityTriggerBlockComponent");
        }

        private void registerConfigReloadHooks() {

                this.getEventRegistry().register(LoadedAssetsEvent.class, RpgModConfig.class, this::onRpgModConfigLoaded);
                this.getEventRegistry().register(RemovedAssetsEvent.class, RpgModConfig.class, this::onRpgModConfigRemoved);
        }

        private void logModuleEnablement(@Nonnull ModuleFlags moduleFlags) {
                RpgLogging.debugDeveloper(
                                "Module enablement: class=%s character=%s achievement=%s talent=%s leveling=%s ability=%s inventory=%s party=%s guild=%s chat=%s camera=%s bank=%s currency=%s auction=%s mail=%s lockpicking=%s",
                                moduleFlags.classModuleEnabled(),
                                moduleFlags.characterModuleEnabled(),
                                moduleFlags.achievementModuleEnabled(),
                                moduleFlags.talentModuleEnabled(),
                                moduleFlags.levelingModuleEnabled(),
                                moduleFlags.abilityModuleEnabled(),
                                moduleFlags.inventoryModuleEnabled(),
                                moduleFlags.partyModuleEnabled(),
                                moduleFlags.guildModuleEnabled(),
                                moduleFlags.chatModuleEnabled(),
                                moduleFlags.cameraModuleEnabled(),
                                moduleFlags.bankModuleEnabled(),
                                moduleFlags.currencyModuleEnabled(),
                                moduleFlags.auctionHouseModuleEnabled(),
                                moduleFlags.mailModuleEnabled(),
                                moduleFlags.lockpickingModuleEnabled());
        }

        private void registerCoreEventHandlers() {

                this.getEventRegistry().register(GiveXPEvent.class, new GiveXPHandler());
                this.getEventRegistry().register(GiveCurrencyEvent.class, new GiveCurrencyHandler());
                this.getEventRegistry().register(LevelUpEvent.class, new LevelUpHandler());
                if (BuildFlags.CAMERA_MODULE) {
                        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, new RpgCameraController());
                }
                if (BuildFlags.INVENTORY_MODULE) {
                        new InventoryHandlingSystem().register(this.getEventRegistry());
                }
                registerRpgEventHooks();
        }

        private void initializeProgressionSystems() {

                // Initialize level progression system
                this.levelSystem = new LevelProgressionSystem(this.getEventRegistry());
                if (BuildFlags.RESTED_XP) {
                        this.restedXpSystem = new RestedXpSystem(this.getEventRegistry());
                }

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
                this.talentSystem = new TalentSystem(this.classManagementSystem, this.levelSystem);
                this.talentSystem.setStatSystem(this.statSystem);
        }

        private void registerHudSystems() {

                this.playerHudService = new PlayerHudService(this.levelSystem);
                this.getEntityStoreRegistry().registerSystem(new PlayerHudUpdateSystem(0.2f, this.playerHudService));
                this.getEventRegistry().registerGlobal(PlayerReadyEvent.class,
                                event -> this.playerHudService.ensureAndUpdate(event.getPlayerRef(), event.getPlayerRef().getStore()));
                this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                                event -> this.playerHudService.remove(event.getPlayerRef().getUuid()));
                this.getEventRegistry().register(ActiveClassChangedEvent.class,
                                event -> this.playerHudService.ensureAndUpdate(event.entityRef(), event.entityRef().getStore()));
        }

        private void initializeGameplayManagers() {
                this.partyManager = new PartyManager();
                this.groupFinderManager = new GroupFinderManager(this.partyManager);
                this.guildManager = new GuildManager();
                this.auctionHouseManager = new AuctionHouseManager();
                this.bankManager = new BankManager();
                this.currencyManager = new CurrencyManager();
                this.expansionManager = new ExpansionManager();
                this.mailManager = new MailManager();
                this.characterManager = new CharacterManager();
                this.worldTravelManager = new WorldTravelManager();
                this.achievementSystem = new AchievementSystem(this.characterManager, this.currencyManager);
                this.achievementSystem.register();

                this.chatManager = new ChatManager();
                reconcileChatChannels(resolveModConfig());
        }

        private void registerPlayerLifecycleHandlers() {

                this.getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, this.chatManager.asAsyncHandler());
                this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                                event -> this.chatManager.clearActiveChannel(event.getPlayerRef().getUuid()));
                this.getEventRegistry().registerGlobal(PlayerReadyEvent.class,
                                event -> this.characterManager.handlePlayerReady(event));
                this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                                event -> this.characterManager.handlePlayerDisconnect(event));
        }

        private void registerGameplaySystems() {

                // Register passive ability ticking system (for passive and toggle abilities)
                this.getEntityStoreRegistry().registerSystem(
                                new org.pixelbays.rpg.ability.system.PassiveAbilityTickingSystem(
                                                this.classComponentType,
                                                this.classAbilityComponentType,
                                                this.classAbilitySystem));
                RpgLogging.debugDeveloper("Registered PassiveAbilityTickingSystem for passive and toggle abilities");

                this.lockpickingSystem = new LockpickingSystem(this.lockpickingSessionComponentType);
                this.getEntityStoreRegistry().registerSystem(this.lockpickingSystem);

                // Initialize remaining race systems
                this.raceSystem = new RaceSystem(this.raceManagementSystem);
                this.raceSystem.setStatSystem(this.statSystem);
                this.classManagementSystem.setRaceSystem(this.raceSystem);

                // Register NPC death XP drop system
                this.getEntityStoreRegistry().registerSystem(new XpDeathDropSystem());
                this.getEntityStoreRegistry().registerSystem(new CurrencyDeathDropSystem());

                // Register hardcore death handler
                this.getEntityStoreRegistry().registerSystem(new org.pixelbays.rpg.leveling.handlers.HardcoreHandler());
        }

        private void registerNpcSystems() {

                if (!BuildFlags.NPC_MODULE) {
                        return;
                }

                NPCPlugin npcPlugin = NPCPlugin.get();
                if (npcPlugin == null) {
                        RpgLogging.debugDeveloper("NPCPlugin unavailable; skipping NPC RPG registration.");
                } else {
                        npcPlugin.registerCoreComponentType("RpgSetup", BuilderActionRpgSetup::new);
                        npcPlugin.registerCoreComponentType("RpgCastAbility", BuilderActionRpgCastAbility::new);
                        this.getEntityStoreRegistry().registerSystem(
                                        new NpcRpgSetupSystem(this.npcRpgSetupComponentType));
                        this.getEntityStoreRegistry().registerSystem(
                                        new NpcThreatDamageSystem(this.npcThreatComponentType));
                        this.getEntityStoreRegistry().registerSystem(
                                        new NpcThreatMaintenanceSystem(this.npcThreatComponentType));
                        this.getEntityStoreRegistry().registerSystem(
                                        new NpcRpgDebugOverlaySystem(NPCEntity.getComponentType(), this.npcRpgDebugComponentType));
                }
        }

        private void registerCustomCodecs() {

                // Register custom interactions for RPG abilities
                Interaction.CODEC.register("ForceTarget", ForceTargetInteraction.class,
                                ForceTargetInteraction.FORCE_TARGET_CODEC);
                Interaction.CODEC.register("DiceRoll", DiceRollInteraction.class,
                                DiceRollInteraction.DICE_ROLL_CODEC);
                Interaction.CODEC.register("EmpowerAbility", EmpowerAbilityInteraction.class,
                                EmpowerAbilityInteraction.CODEC);
                Interaction.CODEC.register("Lockpicking", LockpickInteraction.class,
                                LockpickInteraction.LOCKPICK_CODEC);
                Interaction.CODEC.register("OpenBankUi", OpenBankInteraction.class,
                                OpenBankInteraction.CODEC);
                Interaction.CODEC.register("OpenMailUi", OpenMailInteraction.class,
                                OpenMailInteraction.CODEC);
                Interaction.CODEC.register("ModifyCurrency", ModifyCurrencyInteraction.class,
                                ModifyCurrencyInteraction.CODEC);
                Interaction.CODEC.register("PrerequisiteCheck", PrerequisiteCheckInteraction.class,
                                PrerequisiteCheckInteraction.PREREQUISITE_CHECK_CODEC);
                Interaction.CODEC.register("UnlockAchievement", UnlockAchievementInteraction.class,
                                UnlockAchievementInteraction.CODEC);
                Interaction.CODEC.register("GrantAchievementProgress", GrantAchievementProgressInteraction.class,
                                GrantAchievementProgressInteraction.CODEC);
                Interaction.CODEC.register("UnlockExpansion", UnlockExpansionInteraction.class,
                                UnlockExpansionInteraction.CODEC);
                Interaction.CODEC.register("UnlockRace", UnlockRaceInteraction.class,
                                UnlockRaceInteraction.CODEC);
                Interaction.CODEC.register("UnlockClass", UnlockClassInteraction.class,
                                UnlockClassInteraction.CODEC);
                Interaction.CODEC.register("SendMail", SendMailInteraction.class,
                                SendMailInteraction.CODEC);
                Interaction.CODEC.register("TravelRoute", TravelRouteInteraction.class,
                                TravelRouteInteraction.CODEC);

                // Register custom item drop containers
                ItemDropContainer.CODEC.register("Exp", ExpItemDropContainer.class, ExpItemDropContainer.CODEC);
                ItemDropContainer.CODEC.register("Currency", CurrencyItemDropContainer.class, CurrencyItemDropContainer.CODEC);
        }

        private void registerOptionalSystems(@Nonnull ModuleFlags moduleFlags) {

                // Register equipment restriction system
                if (moduleFlags.classModuleEnabled()) {
                        this.equipmentRestrictions = new EquipmentRestrictions(this.classManagementSystem);
                        this.getEntityStoreRegistry().registerSystem(this.equipmentRestrictions.createInventoryChangeSystem());
                }

                this.randomizedEquipmentManager = new RandomizedEquipmentManager();
                this.randomizedEquipmentManager.register(this.getEventRegistry());
                this.getEntityStoreRegistry().registerSystem(this.randomizedEquipmentManager.createInventoryChangeSystem());

                // Register class level milestone rewards listener
                if (moduleFlags.classModuleEnabled() && moduleFlags.levelingModuleEnabled()) {
                        new org.pixelbays.rpg.classes.integration.LevelMilestone()
                                        .register(this.getEventRegistry());
                }

                RpgLogging.debugDeveloper("Initialized Class/Job System with test data");
        }

        private void registerAlwaysAvailableCommands() {

                this.raceCommandRegistration = refreshCommand(this.raceCommandRegistration, true, RaceCommand::new);
                if (BuildFlags.NPC_MODULE) {
                        this.npcRpgDebugCommandRegistration = refreshCommand(this.npcRpgDebugCommandRegistration, true,
                                        NpcRpgDebugCommand::new);
                }
        }
        
        @Override
        protected void start() {
                reconcilePersistenceBackedManagers(resolveModConfig());
        }

        @Override
        protected void shutdown() {
                if (this.lockpickingSystem != null) {
                        this.lockpickingSystem.shutdown();
                }
                // Deregister packet filter
                if (this.uiInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.uiInputFilter);
                        RpgLogging.debugDeveloper("Deregistered UI input packet filter");
                }
                if (this.abilityInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.abilityInputFilter);
                        RpgLogging.debugDeveloper("Deregistered ability input packet filter");
                }
                if (this.characterLobbyInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.characterLobbyInputFilter);
                        RpgLogging.debugDeveloper("Deregistered character lobby input packet filter");
                }
                if (this.inventoryOpenFilter != null) {
                        PacketAdapters.deregisterInbound(this.inventoryOpenFilter);
                        RpgLogging.debugDeveloper("Deregistered inventory open packet filter");
                }
                if (this.lockpickingInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.lockpickingInputFilter);
                        RpgLogging.debugDeveloper("Deregistered lockpicking input packet filter");
                }
                unregisterCommand(this.raceCommandRegistration);
                unregisterCommand(this.npcRpgDebugCommandRegistration);
                unregisterCommand(this.testLevelCommandRegistration);
                unregisterCommand(this.levelTestCommandRegistration);
                unregisterCommand(this.resetLevelCommandRegistration);
                unregisterCommand(this.classCommandRegistration);
                unregisterCommand(this.partyCommandRegistration);
                unregisterCommand(this.guildCommandRegistration);
                unregisterCommand(this.characterCommandRegistration);
                unregisterCommand(this.bankCommandRegistration);
                unregisterCommand(this.currencyCommandRegistration);
                unregisterCommand(this.mailCommandRegistration);
                unregisterCommand(this.chatCommandRegistration);
                unregisterCommand(this.bindAbilityCommandRegistration);
                unregisterCommand(this.spellbookCommandRegistration);
                unregisterCommand(this.syncHotbarCommandRegistration);
                unregisterCommand(this.unlockAbilityCommandRegistration);
        }

        @Nonnull
        public static Realmweavers get() {
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

        @Nullable
        public PlayerHudService getPlayerHudService() {
                return playerHudService;
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
        public GroupFinderManager getGroupFinderManager() {
                return groupFinderManager;
        }

        @Nonnull
        public GuildManager getGuildManager() {
                return guildManager;
        }

        @Nonnull
        public BankManager getBankManager() {
                return bankManager;
        }

        @Nonnull
        public AuctionHouseManager getAuctionHouseManager() {
                return auctionHouseManager;
        }

        @Nonnull
        public CurrencyManager getCurrencyManager() {
                return currencyManager;
        }

        @Nonnull
        public ExpansionManager getExpansionManager() {
                return expansionManager;
        }

        @Nonnull
        public MailManager getMailManager() {
                return mailManager;
        }

        @Nonnull
        public CharacterManager getCharacterManager() {
                return characterManager;
        }

        @Nonnull
        public WorldTravelManager getWorldTravelManager() {
                return worldTravelManager;
        }

        @Nonnull
        public ChatManager getChatManager() {
                return chatManager;
        }

        @Nonnull
        public AchievementSystem getAchievementSystem() {
                return achievementSystem;
        }

        @Nonnull
        public TalentSystem getTalentSystem() {
                return talentSystem;
        }

        @Nonnull
        public ComponentType<EntityStore, AchievementComponent> getAchievementComponentType() {
                return achievementComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, LevelProgressionComponent> getLevelProgressionComponentType() {
                return levelProgressionComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, LockpickingSessionComponent> getLockpickingSessionComponentType() {
                return lockpickingSessionComponentType;
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

        public ComponentType<EntityStore, NpcRpgSetupComponent> getNpcRpgSetupComponentType() {
                return npcRpgSetupComponentType;
        }

        public ComponentType<EntityStore, NpcRpgDebugComponent> getNpcRpgDebugComponentType() {
                return npcRpgDebugComponentType;
        }

        public ComponentType<EntityStore, NpcThreatComponent> getNpcThreatComponentType() {
                return npcThreatComponentType;
        }

        @Nonnull
        public ComponentType<EntityStore, AbilityBindingComponent> getAbilityBindingComponentType() {
                return abilityBindingComponentType;
        }

        @Nonnull
        public LockpickingSystem getLockpickingSystem() {
                return lockpickingSystem;
        }

        public boolean isLockpickingModuleEnabled() {
                RpgModConfig config = resolveModConfig();
                return isModuleEnabled(config, config != null ? config.getLockpickingSettings().isEnabled() : null);
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
                registerNoop(AchievementUnlockedEvent.class);
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

        @Nullable
        private RpgModConfig resolveModConfig() {
                return RpgModConfig.getAssetMap().getAsset("default");
        }

        private boolean isModuleEnabled(@Nullable RpgModConfig config, @Nullable Boolean enabled) {
                if (config == null || enabled == null) {
                        return true;
                }
                return enabled;
        }

        private void onRpgModConfigChanged() {
                RpgModConfig config = resolveModConfig();
                reconcileSavedAbilityBindings(config);
                reconcileDynamicRegistrations(config);
        }

        @SuppressWarnings("unused")
        private void onRpgModConfigLoaded(
                        @Nonnull LoadedAssetsEvent<String, RpgModConfig, DefaultAssetMap<String, RpgModConfig>> ignoredEvent) {
                onRpgModConfigChanged();
        }

        @SuppressWarnings("unused")
        private void onRpgModConfigRemoved(
                        @Nonnull RemovedAssetsEvent<String, RpgModConfig, DefaultAssetMap<String, RpgModConfig>> ignoredEvent) {
                onRpgModConfigChanged();
        }

        private void reconcileDynamicRegistrations(@Nullable RpgModConfig config) {
                ModuleFlags moduleFlags = resolveModuleFlags(config);

                reconcileChatChannels(config);

                this.testLevelCommandRegistration = refreshCommand(
                                this.testLevelCommandRegistration,
                                moduleFlags.levelingModuleEnabled(),
                                TestLevelCommand::new);
                this.levelTestCommandRegistration = refreshCommand(
                                this.levelTestCommandRegistration,
                                moduleFlags.levelingModuleEnabled(),
                                LevelTestCommand::new);
                this.resetLevelCommandRegistration = refreshCommand(
                                this.resetLevelCommandRegistration,
                                moduleFlags.levelingModuleEnabled(),
                                ResetLevelCommand::new);
                this.classCommandRegistration = refreshCommand(
                                this.classCommandRegistration,
                                moduleFlags.classModuleEnabled(),
                                ClassCommand::new);
                this.partyCommandRegistration = refreshCommand(
                                this.partyCommandRegistration,
                                moduleFlags.partyModuleEnabled(),
                                PartyCommand::new);
                this.guildCommandRegistration = refreshCommand(
                                this.guildCommandRegistration,
                                moduleFlags.guildModuleEnabled(),
                                GuildCommand::new);
                this.characterCommandRegistration = refreshCommand(
                                this.characterCommandRegistration,
                                moduleFlags.characterModuleEnabled(),
                                CharacterCommand::new);
                this.bankCommandRegistration = refreshCommand(
                                this.bankCommandRegistration,
                                moduleFlags.bankModuleEnabled(),
                                BankCommand::new);
                this.currencyCommandRegistration = refreshCommand(
                                this.currencyCommandRegistration,
                                moduleFlags.currencyModuleEnabled(),
                                CurrencyCommand::new);
                this.mailCommandRegistration = refreshCommand(
                                this.mailCommandRegistration,
                                moduleFlags.mailModuleEnabled(),
                                MailCommand::new);
                this.chatCommandRegistration = refreshCommand(
                                this.chatCommandRegistration,
                                moduleFlags.chatModuleEnabled(),
                                ChatCommand::new);
                this.bindAbilityCommandRegistration = refreshCommand(
                                this.bindAbilityCommandRegistration,
                                moduleFlags.abilityModuleEnabled(),
                                BindAbilityCommand::new);
                this.spellbookCommandRegistration = refreshCommand(
                                this.spellbookCommandRegistration,
                                moduleFlags.abilityModuleEnabled(),
                                SpellbookCommand::new);
                this.syncHotbarCommandRegistration = refreshCommand(
                                this.syncHotbarCommandRegistration,
                                moduleFlags.abilityModuleEnabled(),
                                SyncHotbarCommand::new);
                this.unlockAbilityCommandRegistration = refreshCommand(
                                this.unlockAbilityCommandRegistration,
                                moduleFlags.abilityModuleEnabled(),
                                UnlockAbilityCommand::new);
                this.travelRouteCommandRegistration = refreshCommand(
                                this.travelRouteCommandRegistration,
                                BuildFlags.WORLD_MODULE && config != null && config.getWorldSettings().isEnabled() && config.getWorldSettings().isAllowTravelCommands(),
                                TravelRouteCommand::new);

                if (this.getState() != PluginState.SETUP) {
                        reconcilePersistenceBackedManagers(moduleFlags);
                }

                reconcileInboundFilters(moduleFlags);
        }

        private void reconcileSavedAbilityBindings(@Nullable RpgModConfig config) {
                if (config == null || this.characterManager == null) {
                        return;
                }

                this.characterManager.sanitizeAbilityBindingsForCurrentConfig();
        }

        private void reconcilePersistenceBackedManagers(@Nullable RpgModConfig config) {
                reconcilePersistenceBackedManagers(resolveModuleFlags(config));
        }

        private void reconcilePersistenceBackedManagers(@Nonnull ModuleFlags moduleFlags) {

                this.expansionManager.loadFromAssets();

                if (moduleFlags.partyModuleEnabled()) {
                        this.partyManager.loadFromAssets();
                }
                if (moduleFlags.guildModuleEnabled()) {
                        this.guildManager.loadFromAssets();
                }
                if (moduleFlags.characterModuleEnabled()) {
                        this.characterManager.loadFromAssets();
                } else {
                        this.characterManager.clear();
                }
                if (moduleFlags.auctionHouseModuleEnabled()) {
                        this.auctionHouseManager.loadFromAssets();
                } else {
                        this.auctionHouseManager.clear();
                }
                if (moduleFlags.bankModuleEnabled()) {
                        this.bankManager.loadFromAssets();
                } else {
                        this.bankManager.clear();
                }
                if (moduleFlags.currencyModuleEnabled()) {
                        this.currencyManager.loadFromAssets();
                } else {
                        this.currencyManager.clear();
                }
                if (moduleFlags.mailModuleEnabled()) {
                        this.mailManager.loadFromAssets();
                } else {
                        this.mailManager.clear();
                }
        }

        private void reconcileInboundFilters(@Nonnull ModuleFlags moduleFlags) {
                if (moduleFlags.characterModuleEnabled()) {
                        if (this.characterLobbyInputFilter == null) {
                                this.characterLobbyInputFilter = PacketAdapters
                                                .registerInbound(new CharacterLobbyInputFilter());
                                RpgLogging.debugDeveloper("Registered character lobby input packet filter");
                        }
                } else if (this.characterLobbyInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.characterLobbyInputFilter);
                        this.characterLobbyInputFilter = null;
                        RpgLogging.debugDeveloper("Deregistered character lobby input packet filter");
                }

                if (this.uiInputFilter == null) {
                        this.uiInputFilter = PacketAdapters.registerInbound(new UiInputFilter());
                        RpgLogging.debugDeveloper("Registered UI input packet filter");
                }

                if (moduleFlags.abilityModuleEnabled()) {
                        if (this.abilityInputFilter == null) {
                                this.abilityInputFilter = PacketAdapters.registerInbound(new AbilityInputFilter(this));
                                RpgLogging.debugDeveloper("Registered ability input packet filter");
                        }
                } else if (this.abilityInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.abilityInputFilter);
                        this.abilityInputFilter = null;
                        RpgLogging.debugDeveloper("Deregistered ability input packet filter");
                }

                if (moduleFlags.inventoryModuleEnabled()) {
                        if (this.inventoryOpenFilter == null) {
                                this.inventoryOpenFilter = PacketAdapters.registerInbound(new InventoryOpenFilter());
                                RpgModConfig inventoryConfig = InventoryOpenFilter.resolveConfigForLog();
                                RpgLogging.debugDeveloper(
                                                "Registered inventory open packet filter (handling=%s)",
                                                inventoryConfig != null ? inventoryConfig.getInventoryHandling() : "unknown");
                        }
                } else if (this.inventoryOpenFilter != null) {
                        PacketAdapters.deregisterInbound(this.inventoryOpenFilter);
                        this.inventoryOpenFilter = null;
                        RpgLogging.debugDeveloper("Deregistered inventory open packet filter");
                }

                if (moduleFlags.lockpickingModuleEnabled()) {
                        if (this.lockpickingInputFilter == null) {
                                this.lockpickingInputFilter = PacketAdapters.registerInbound(new LockpickingInputFilter());
                                RpgLogging.debugDeveloper("Registered lockpicking input packet filter");
                        }
                } else if (this.lockpickingInputFilter != null) {
                        PacketAdapters.deregisterInbound(this.lockpickingInputFilter);
                        this.lockpickingInputFilter = null;
                        RpgLogging.debugDeveloper("Deregistered lockpicking input packet filter");
                }
        }

        private CommandRegistration refreshCommand(@Nullable CommandRegistration registration, boolean enabled,
                        @Nonnull Supplier<? extends AbstractCommand> supplier) {
                if (registration != null) {
                        registration.unregister();
                }
                if (!enabled) {
                        return null;
                }
                return this.getCommandRegistry().registerCommand(supplier.get());
        }

        private void unregisterCommand(@Nullable CommandRegistration registration) {
                if (registration != null) {
                        registration.unregister();
                }
        }

        private void reconcileChatChannels(@Nullable RpgModConfig config) {
                if (this.chatManager == null) {
                        return;
                }

                ChatModSettings chatSettings = config != null ? config.getChatSettings() : new ChatModSettings();
                if (!chatSettings.isEnabled()) {
                        this.chatManager.replaceChannels(List.of());
                        return;
                }

                List<org.pixelbays.rpg.chat.ChatChannel> channels = new ArrayList<>();
                for (ChatChannelDefinition definition : chatSettings.getChannels()) {
                        org.pixelbays.rpg.chat.ChatChannel channel = createConfiguredChatChannel(definition);
                        if (channel != null) {
                                channels.add(channel);
                        }
                }

                try {
                        this.chatManager.replaceChannels(channels);
                } catch (RuntimeException ex) {
                        RpgLogging.error(ex, "Failed to rebuild chat channels from RPGModConfig");
                }
        }

        @Nullable
        private org.pixelbays.rpg.chat.ChatChannel createConfiguredChatChannel(@Nullable ChatChannelDefinition definition) {
                if (definition == null || !definition.isEnabled()) {
                        return null;
                }

                return switch (definition.getType()) {
                        case Party -> new PartyChatChannel(this.partyManager, definition);
                        case Guild -> new GuildChatChannel(this.guildManager, definition);
                };
        }

        private ModuleFlags resolveModuleFlags(@Nullable RpgModConfig config) {
                return new ModuleFlags(
                                BuildFlags.CLASS_MODULE         && isModuleEnabled(config, config != null ? config.getClassSettings().isEnabled() : null),
                                BuildFlags.CHARACTER_MODULE     && isModuleEnabled(config, config != null ? config.getCharacterSettings().isEnabled() : null),
                                BuildFlags.ACHIEVEMENT_MODULE   && isModuleEnabled(config, config != null ? config.getAchievementSettings().isEnabled() : null),
                                BuildFlags.TALENT_MODULE        && isModuleEnabled(config, config != null ? config.getTalentSettings().isEnabled() : null),
                                BuildFlags.LEVELING_MODULE      && isModuleEnabled(config, config != null ? config.getLevelingSettings().isEnabled() : null),
                                BuildFlags.ABILITY_MODULE       && isModuleEnabled(config, config != null ? config.getAbilitySettings().isEnabled() : null),
                                BuildFlags.INVENTORY_MODULE     && isModuleEnabled(config, config != null ? config.getInventorySettings().isEnabled() : null),
                                BuildFlags.PARTY_MODULE         && isModuleEnabled(config, config != null ? config.getPartySettings().isEnabled() : null),
                                BuildFlags.GUILD_MODULE         && isModuleEnabled(config, config != null ? config.getGuildSettings().isEnabled() : null),
                                BuildFlags.CHAT_MODULE          && isModuleEnabled(config, config != null ? config.isChatModuleEnabled() : null),
                                BuildFlags.CAMERA_MODULE        && isModuleEnabled(config, config != null ? config.getCameraSettings().isEnabled() : null),
                                BuildFlags.BANK_MODULE          && isModuleEnabled(config, config != null ? config.getBankSettings().isEnabled() : null),
                                BuildFlags.CURRENCY_MODULE      && isModuleEnabled(config, config != null ? config.getCurrencySettings().isEnabled() : null),
                                BuildFlags.AUCTION_HOUSE_MODULE && isModuleEnabled(config, config != null ? config.getAuctionHouseSettings().isEnabled() : null),
                                BuildFlags.MAIL_MODULE          && isModuleEnabled(config, config != null ? config.getMailSettings().isEnabled() : null),
                                BuildFlags.LOCKPICKING_MODULE   && isModuleEnabled(config, config != null ? config.getLockpickingSettings().isEnabled() : null));
        }

        private record ModuleFlags(
                        boolean classModuleEnabled,
                        boolean characterModuleEnabled,
                        boolean achievementModuleEnabled,
                        boolean talentModuleEnabled,
                        boolean levelingModuleEnabled,
                        boolean abilityModuleEnabled,
                        boolean inventoryModuleEnabled,
                        boolean partyModuleEnabled,
                        boolean guildModuleEnabled,
                        boolean chatModuleEnabled,
                        boolean cameraModuleEnabled,
                        boolean bankModuleEnabled,
                        boolean currencyModuleEnabled,
                        boolean auctionHouseModuleEnabled,
                        boolean mailModuleEnabled,
                        boolean lockpickingModuleEnabled) {
        }

}