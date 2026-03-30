# Event and Integration Map

## Why this page exists

The Realmweaver codebase is split between ECS state, manager classes, asset-driven content, packet filters, and custom events.

That means a lot of important behavior is not visible from a single class. This page maps the main runtime handoffs so contributors can answer questions like:

- what actually happens after XP is granted?
- which systems respond when stats change?
- where do party, guild, and ability events come from?
- which events are active extension seams versus internal implementation details?

## Integration layers at a glance

The mod currently uses five main integration layers:

1. asset load/reload hooks
2. Hytale global events like player ready/connect/disconnect/chat
3. custom Realmweaver events such as `GiveXPEvent`, `LevelUpEvent`, `ClassStatBonusesRecalculatedEvent`, and `AbilityTriggeredEvent`
4. ECS ticking systems and death-drop systems
5. UI callbacks and packet filters that route player input into managers and systems

## Composition-root registrations

`ExamplePlugin` is where the major event graph is assembled.

Notable registrations and integrations include:

- `LoadedAssetsEvent` / `RemovedAssetsEvent` for `RpgModConfig` reload handling
- `GiveXPEvent` handled by `GiveXPHandler`
- `GiveCurrencyEvent` handled by `GiveCurrencyHandler`
- `LevelUpEvent` handled by `LevelUpHandler`
- `PlayerReadyEvent` handled by `RpgCameraController`
- `InventoryHandlingSystem.register(...)` wiring stat-change events into inventory resizing
- `LevelProgressionSystem`, `RestedXpSystem`, `StatSystem`, `TalentSystem`, and other core gameplay services created during plugin startup
- `LevelMilestone.register(...)` wiring class milestone rewards to level-up events
- `EquipmentRestrictions.register(...)` and `RandomizedEquipmentManager.register(...)` wiring inventory and level signals into gear behavior

This is why the plugin file is the best place to start when tracking cross-module behavior.

## Core progression event flow

## XP grant flow

The XP pipeline is one of the clearest examples of the project’s event-driven architecture.

Typical sources of XP events:

- admin/test commands like level or class XP commands
- `XpDeathDropSystem` when a valid kill awards XP
- any future system that dispatches `GiveXPEvent`

Runtime sequence:

1. A caller dispatches `GiveXPEvent`.
2. `GiveXPHandler` resolves the target and calls into `LevelProgressionSystem`.
3. `LevelProgressionSystem` grants XP to a specific level system id.
4. If thresholds are crossed, `LevelUpEvent` is dispatched.
5. `LevelProgressionSystem` also applies level rewards and may dispatch `LevelRewardsAppliedEvent`.
6. If the level track unlocks class abilities, it dispatches `ClassAbilityUnlockedEvent`.
7. Stat growth and flat reward bonuses ultimately feed into `StatSystem` methods.

That makes XP a multi-system trigger rather than a single subsystem concern.

## Level-up listeners and follow-on effects

`LevelUpEvent` currently matters beyond simple level numbers.

Known listeners or downstream consumers include:

- `LevelMilestone`, which grants class milestone rewards like skill points when the class is tied to the same level system
- `LevelUpHandler`, which is registered centrally and likely handles user-facing follow-up behavior
- `RandomizedEquipmentManager`, which listens globally for level ups to re-evaluate item progression behavior

In practice, a single level-up may affect:

- available talent points
- class milestone rewards
- unlocked abilities
- inventory sizing if growth changes relevant stats
- gear eligibility or randomization refreshes

## Stat recalculation flow

`StatSystem` is the main stat integration hub.

On `PlayerConnectEvent`, it performs startup reconciliation for the entity:

- ensure starting class exists
- recalculate class stat bonuses
- recalculate talent stat bonuses
- recalculate race stat bonuses
- reapply any pending stat growth from level systems

During runtime it can dispatch:

- `ClassStatBonusesRecalculatedEvent`
- `RaceStatBonusesRecalculatedEvent`
- `StatIncreasesAppliedEvent`
- `StatGrowthAppliedEvent`

These are important because they are not just informational; other systems actively listen to them.

## Inventory-size integration

`InventoryHandlingSystem` is the clearest concrete listener for stat recalculation events.

It registers handlers for:

- `PlayerReadyEvent`
- `StatIncreasesAppliedEvent`
- `StatGrowthAppliedEvent`
- `ClassStatBonusesRecalculatedEvent`
- `RaceStatBonusesRecalculatedEvent`

Its job is to rebuild inventory capacity when the configured inventory model depends on stats such as strength.

This creates a clean chain:

1. class, race, talent, or level growth changes stats
2. `StatSystem` dispatches a stat-related event
3. `InventoryHandlingSystem` re-reads config and stats
4. player inventory capacity is expanded if needed

That is one of the best examples in the mod of a feature integrating through events instead of direct hard-coded coupling.

## Class and talent integration flow

## Class lifecycle events

`ClassManagementSystem` dispatches several events that act as extension hooks:

- `ClassLearnedEvent`
- `ClassUnlearnedEvent`
- `ActiveClassChangedEvent`
- `ClassAbilityUnlockedEvent`

These cover the major lifecycle transitions for class ownership and active-class switching.

Important implications:

- class changes are not only about UI state; they alter available abilities, stat calculations, and equipment validity
- event consumers can respond without needing to patch class-management code directly

## Talent integration

Talent spending is not shown as a large event surface by itself, but it does feed into `StatSystem.recalculateTalentStatBonuses(...)` and into class ability availability.

From a system perspective, talent allocation behaves like a second-order modifier layer on top of class progression:

- class data stores node ranks
- talent pages mutate allocation on the world thread
- `StatSystem` converts ranked nodes into additive and multiplicative stat modifiers
- ability or passive grants can also come from talent nodes

## Level milestone integration

`LevelMilestone` listens to `LevelUpEvent` and checks:

- which classes the entity has learned
- which level system each class uses
- whether a milestone exists for each newly crossed level

Current implemented milestone behavior:

- grants skill points into the relevant `LevelProgressionComponent.LevelSystemData`

Current TODO milestone hooks called out in code:

- item rewards
- interaction-chain rewards

That means the class milestone framework is live, but some reward types are still placeholders.

## Race integration flow

`RaceSystem` dispatches:

- `RaceChangedEvent`
- `RaceAbilityUnlockedEvent`

`StatSystem` then handles race-derived stat modifier reconciliation and dispatches `RaceStatBonusesRecalculatedEvent`.

The combined race flow looks like this:

1. race selection/change mutates `RaceComponent`
2. `RaceSystem` emits race lifecycle events
3. race ability unlocks may be dispatched from race-level logic
4. `StatSystem` recalculates race stat modifiers
5. listeners like `InventoryHandlingSystem` can react if those modifiers matter for secondary systems

## Ability trigger flow

The low-level ability runtime is already documented in `Ability-Trigger-Flow.md`, but the event surface is worth summarizing here.

`ClassAbilitySystem` dispatches:

- `BlockAbilityTriggerEvent` when an attempted cast is blocked
- `AbilityTriggeredEvent` when the cast proceeds
- `AbilityTriggerFailedEvent` when runtime validation fails

This event family is useful for:

- analytics or debugging hooks
- future combat log systems
- UI feedback extensions
- anti-spam or cooldown telemetry

Because abilities are packet/input driven, these events are the cleanest seam for observing runtime outcome without reaching into input filters.

## Currency and death-drop flow

Two important cross-system producers are the death-drop systems:

- `XpDeathDropSystem` can dispatch `GiveXPEvent`
- `CurrencyDeathDropSystem` can dispatch `GiveCurrencyEvent`

That means kill rewards are intentionally routed back into the same generic progression/economy event pipeline instead of using special-case reward paths.

The currency side then follows this broad chain:

1. `GiveCurrencyEvent` is dispatched
2. `GiveCurrencyHandler` resolves the recipient and scope
3. `CurrencyManager` mutates the appropriate wallet/item-backed store
4. currency-aware UI or gameplay systems reflect the updated balance on next refresh

## Social-system event families

## Party domain events

`PartyManager` dispatches a fairly complete party event family, including:

- `PartyCreatedEvent`
- `PartyInviteSentEvent`
- `PartyJoinedEvent`
- `PartyLeftEvent`
- `PartyDisbandedEvent`
- `PartyMemberKickedEvent`
- `PartyAssistantPromotedEvent`
- `PartyLeaderChangedEvent`

These currently act mainly as domain signals and extension seams. The page/UI layer mostly talks directly to `PartyManager`, but other modules can subscribe to party lifecycle without modifying party code.

## Guild domain events

`GuildManager` does the same for guild operations, dispatching events such as:

- `GuildCreatedEvent`
- `GuildInviteSentEvent`
- `GuildJoinedEvent`
- `GuildApplicationSubmittedEvent`
- `GuildApplicationAcceptedEvent`
- `GuildApplicationDeniedEvent`
- `GuildLeftEvent`
- `GuildDisbandedEvent`
- `GuildMemberKickedEvent`
- `GuildLeaderChangedEvent`
- `GuildJoinPolicyChangedEvent`
- `GuildRoleCreatedEvent`
- `GuildRolePermissionChangedEvent`
- `GuildRoleAssignedEvent`

This is a strong sign that the social systems were designed with future integrations in mind, even if the current wiki-visible listeners are modest.

## Inventory and equipment integration

Two item/inventory features are wired mainly through event listeners rather than direct calls:

- `EquipmentRestrictions`
- `RandomizedEquipmentManager`

Known listeners include:

- `LivingEntityInventoryChangeEvent`
- `PlayerReadyEvent`
- `PlayerDisconnectEvent` for randomized equipment lifecycle
- `LevelUpEvent` for randomized equipment progression refresh

This suggests a general pattern in the repo:

- progression systems emit state changes
- equipment systems react by validating or regenerating item behavior

## Chat and async integration

`ChatManager` is one of the main async integration points.

It is registered through an async player-chat event path rather than a synchronous gameplay path, then resolves:

- channel selection
- inline `!channel` overrides
- party/guild target routing
- formatter application before the event completes

This is important architecturally because chat is one of the few clearly async-first subsystems in the mod.

## UI callback integration

Most custom pages are not pure presentation. They are integration adapters.

Common pattern:

1. the UI emits raw event data
2. the page parses strings/ints/bools
3. the page jumps onto `World.execute(...)`
4. a manager or system performs the mutation
5. the page rebuilds or updates visible state

This means UI pages are part of the runtime event graph even when they are not dispatching formal custom events.

## What is tightly coupled vs loosely coupled

### Tightly coupled areas

- `ExamplePlugin` startup wiring
- page controllers talking directly to their managers
- packet filters feeding specific gameplay systems
- level progression directly coordinating reward/unlock logic

### Loosely coupled areas

- stat-change propagation into inventory sizing
- party and guild lifecycle signals
- XP and currency reward dispatch from unrelated systems
- class/race/ability lifecycle observation through custom events

## Contributor guidance

When adding a new system, use this rough rule:

- if the behavior is a direct internal implementation detail, keep it in the owning manager/system
- if other modules might need to react later, dispatch a focused custom event
- if the behavior touches live entity state from UI or async code, bounce onto `World.execute(...)`

For this repo, the best event seams already exist around:

- XP and currency grants
- level-ups and level rewards
- class and race lifecycle changes
- stat recalculation
- party and guild lifecycle transitions
- ability runtime outcomes

## Gaps and opportunities

The current event model is useful, but some seams are still underused.

Likely future improvements:

- more explicit talent allocation events
- more listeners for party/guild domain events
- a shared combat-log or notification layer subscribing to ability, level, and social events
- richer UI auto-refresh subscriptions instead of mostly manual page refresh actions
- milestone reward expansion beyond skill points
