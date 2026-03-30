# Architecture

## Composition root

The entire mod is wired from `org.pixelbays.plugin.ExamplePlugin`.

The plugin performs four major jobs:

1. **Asset registration** for gameplay config/data types.
2. **ECS registration** for persistent entity components.
3. **System/manager initialization** for gameplay features.
4. **Dynamic registration reconciliation** for commands, packet filters, and persistence-backed managers.

That makes `ExamplePlugin` the first file to read when tracing any feature.

## Lifecycle model

### `setup()`

`setup()` builds the mod graph.

It registers asset stores for:

- `RpgModConfig`
- `PartyData`
- `GuildData`
- `AuctionData`
- `BankData`
- `BankTypeDefinition`
- `CurrencyWalletData`
- `ExpansionUnlockData`
- `MailData`
- `CurrencyTypeDefinition`
- `ExpCurveDefinition`
- `LevelSystemConfig`
- `ClassDefinition`
- `RandomizedEquipmentDefinition`
- `AbilityQuality`
- `AbilityCategory`
- `ClassAbilityDefinition`
- `RaceDefinition`
- `CharacterRosterData`

It then registers ECS components for:

- `LevelProgressionComponent`
- `ClassComponent`
- `ClassAbilityComponent`
- `AbilityEmpowerComponent`
- `RaceComponent`
- `AbilityBindingComponent`
- `AbilityTriggerBlockComponent`
- `LockpickingSessionComponent`
- `NpcRpgDebugComponent`
- `NpcRpgSetupComponent`

After that it instantiates and wires managers/systems such as:

- leveling
- classes
- abilities
- stats
- races
- talents
- parties/group finder
- guilds
- auctions
- banks
- currencies
- expansions
- mail
- character roster/select
- chat
- HUD updates
- lockpicking
- NPC setup/debug systems

### `start()`

`start()` mainly reloads persistence-backed managers through `reconcilePersistenceBackedManagers()`.

### Config reload path

`LoadedAssetsEvent` and `RemovedAssetsEvent` for `RpgModConfig` both route into a config-change handler.

That handler re-runs dynamic setup so the mod can respond to asset editor changes without needing a full plugin rewrite.

## Module toggle strategy

`Server/RpgModConfig/Default.json` is the runtime control plane.

Each major module has a settings section with an `Enabled` flag. The plugin uses that to decide whether to:

- register or unregister commands
- register or unregister packet filters
- load or clear persistence-backed managers

### Dynamically controlled commands

The plugin conditionally exposes command roots for:

- leveling
- classes
- parties
- guilds
- characters
- banks
- currency
- mail
- chat
- ability binding / hotbar sync / ability unlocking

### Dynamically controlled packet filters

The plugin conditionally enables inbound packet hooks for:

- ability input
- inventory opening
- lockpicking input

This is one of the most important patterns in the mod: **feature activation is config-driven, not hardwired**.

## Manager vs ECS split

The codebase uses a practical hybrid architecture.

### ECS is used for live entity state

Examples:

- progression on player entities
- learned classes and active class order
- unlocked abilities / bindings
- race data
- active lockpicking sessions
- NPC RPG debug/setup tags

### Managers are used for cross-entity feature orchestration

Examples:

- `CharacterManager`
- `PartyManager`
- `GuildManager`
- `BankManager`
- `CurrencyManager`
- `AuctionHouseManager`
- `MailManager`
- `ExpansionManager`
- `ChatManager`

These managers usually:

- hold in-memory maps
- load/save asset-backed persistence objects
- expose higher-level feature APIs
- integrate with commands and UI pages

## Persistence model

The repo is not currently structured around a separate SQL gameplay backend.

Instead, most systems persist through asset-like data objects under `src/main/resources/Server`-style paths such as:

- `PartyData`
- `GuildData`
- `AuctionData`
- `BankData`
- `CurrencyData`
- `MailData`
- `CharacterData`
- `ExpansionData`

This keeps the current implementation consistent with Hytale's asset/editor pipeline.

## Data-driven content model

Gameplay content lives primarily in JSON.

### Important asset buckets

```text
Server/Abilities/
Server/Banks/
Server/Classes/
Server/Currencies/
Server/Drops/
Server/Entity/ExpCurves/
Server/Entity/levels/
Server/Races/
Server/RpgModConfig/
Server/Prefabs/
```

### Content ownership pattern

- Java defines codecs, systems, managers, and runtime rules.
- JSON defines content instances, tuning values, unlock tables, rewards, and most gameplay metadata.

## UI architecture

The mod uses paired Java + `.ui` assets.

Java pages/huds live under package `ui` folders, while markup lives under:

```text
src/main/resources/Common/UI/Custom/Pages/
```

Current UI/page assets include:

- `CharacterSelectPage.ui`
- `PartyPage.ui`
- `GroupFinderPage.ui`
- `GuildPage.ui`
- `BankPage.ui`
- `BankSelectionPage.ui`
- `BankStoragePage.ui`
- `CurrencyPage.ui`
- `MailPage.ui`
- `LockpickingPage.ui`
- `RpgInventoryPage.ui`
- talent detail/tooltip panes

The XP bar is a HUD rather than a page.

## Input architecture

The mod uses a mix of:

- commands
- Hytale interaction chains
- packet filters
- custom UI pages

Examples:

- abilities are triggered through packet interception + ability binding + root interactions
- inventory behavior can be intercepted by an inbound filter
- lockpicking has both a live session component and a packet-driven UI/input loop
- chat uses an async event pipeline rather than a packet filter

## Thread-safety pattern

Live entity mutation must happen on the world thread.

You can see that pattern clearly in systems like leveling and character handling, where methods either:

- accept a `World` and call `world.execute(...)`, or
- assume they are already running on the correct thread

This is a critical rule for extending the mod safely.

## Folder map

```text
src/main/java/org/pixelbays/plugin/          plugin entrypoint
src/main/java/org/pixelbays/rpg/ability/     abilities, bindings, GCD, input
src/main/java/org/pixelbays/rpg/character/   roster/select/login flow
src/main/java/org/pixelbays/rpg/classes/     class defs, switching, talents
src/main/java/org/pixelbays/rpg/economy/     auctions, banks, currency
src/main/java/org/pixelbays/rpg/global/      config, stats, shared interactions
src/main/java/org/pixelbays/rpg/guild/       guilds, roles, permissions
src/main/java/org/pixelbays/rpg/hud/         XP HUD
src/main/java/org/pixelbays/rpg/inventory/   custom inventory handling
src/main/java/org/pixelbays/rpg/item/        randomized equipment and restrictions
src/main/java/org/pixelbays/rpg/leveling/    XP, level systems, rewards
src/main/java/org/pixelbays/rpg/lockpicking/ lock minigame
src/main/java/org/pixelbays/rpg/mail/        mailbox subsystem
src/main/java/org/pixelbays/rpg/npc/         NPC RPG integration
src/main/java/org/pixelbays/rpg/party/       party + raid + finder
src/main/java/org/pixelbays/rpg/race/        race assets and runtime logic
```

## Architectural strengths

- Strong asset-driven bias.
- Good separation between persistent social/economy managers and entity-bound progression state.
- Runtime module toggling is already built in.
- UI/content is organized around Hytale-native assets instead of external overlays.
- Feature packages are clear enough to document and extend independently.

## Architectural pressure points

- `ExamplePlugin` is becoming very large, so system registration knowledge is centralized and dense.
- Some modules are much more mature than others, creating uneven contributor expectations.
- Inventory/profession/instance work is still incomplete, so downstream docs must clearly distinguish shipped vs planned behavior.
- Multiple gameplay surfaces depend on the main config asset, making config hygiene extremely important.
