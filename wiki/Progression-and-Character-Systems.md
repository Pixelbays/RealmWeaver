# Progression and Character Systems

## Character roster and select flow

The mod already supports a multi-character account model.

### Main responsibilities of `CharacterManager`

- load/save account rosters
- open the character select page on player ready
- support character creation, selection, deletion, and recovery
- maintain the active character id per account
- migrate legacy player data into the roster system
- move players between lobby/select flow and gameplay world flow
- play configurable login/logout VFX

### Character system features already visible in config

The `CharacterSettings` section supports:

- max character slots
- enable/disable creation and deletion
- logout-to-character-select flow
- soft-delete recovery windows
- login/logout VFX
- legacy migration enablement
- optional unique character names
- race requirement on creation
- starter class requirement on creation
- shared lobby world selection
- race-specific spawn points
- race-specific lobby backgrounds
- race-specific preview camera presets

This is already a strong foundation for an MMO-style account/character separation.

## Race system

Race data lives under `Server/Races`.

### Current race asset characteristics

A race asset can define:

- display metadata
- hybrid relationships
- size bounds
- starting stats
- additive/multiplicative stat modifiers
- race ability unlocks
- cosmetic allow/deny lists
- inheritance rules for hybrids

### Runtime role

The race system contributes to:

- player identity in character creation/select
- stat recalculation
- race-specific ability unlocks
- class eligibility checks when class tags restrict races

## Leveling system

Leveling is one of the most reusable pieces of the mod.

### Core concept

The mod does **not** assume there is only one level track.

Instead, `LevelProgressionSystem` works with named level-system ids, such as:

- base character level
- class-specific level tracks
- profession tracks

### Level system asset capabilities

A `LevelSystemConfig` can define:

- display name / description
- max level
- starting level
- XP curve config
- prerequisites on other level systems
- default rewards
- per-level overrides/milestones
- stat growth
- enable/visible flags

### Reward model

Level rewards already support several reward channels:

- stat points
- skill points
- direct stat increases
- stat growth application
- currency rewards
- items
- unlocked abilities/quests placeholders
- sound/particle/notification feedback

### Secondary systems tied into leveling

The config already exposes hooks for:

- rested XP
- hardcore penalties
- XP multipliers

Not every one of those loops is equally mature yet, but the foundation is already in place.

## Class system

The class system is one of the deeper modules in the repo.

### `ClassManagementSystem` handles

- class lookup from assets
- learn/unlearn flow
- active class switching
- prerequisite enforcement
- class-count limits by tags
- race-based class restrictions
- expansion gating
- class ability unlocks by level
- class stat bonus recalculation triggers

### Class asset capabilities

A class definition can express:

- prerequisites by required class level
- required classes
- shared or independent level track usage
- starting class status
- resource stats
- base/per-level stat modifiers
- equipment restrictions
- ability unlocks
- level milestones
- talent trees
- switching rules
- relearn penalties
- tags like combat/profession/job

### Important design note

Class tags are not cosmetic. They drive real behavior such as:

- combat vs profession limits
- XP routing logic
- class counting rules
- downstream system categorization

## Talent system

Talents are currently embedded inside class definitions rather than stored as a totally separate asset family.

### Current talent functionality

- allocate points into nodes
- validate level requirements
- validate prerequisite nodes
- enforce spec mode rules
- spend skill points from the class's level system
- grant node-based abilities
- refund single points
- reset full trees
- recalculate stat bonuses after changes

### Current talent settings axis

`TalentSettings` already exposes at least:

- spec mode
- reset mode
- partial refund tuning
- paid-reset item hooks

### Maturity note

The runtime side is already substantial. The requirements notes still suggest more asset-editor work is needed, especially around currencies and UX.

## Ability system

Abilities are fully data-driven and use Hytale interaction chains for execution.

### Ability definition support

`ClassAbilityDefinition` exposes a wide authoring surface:

- icon and categories
- translation properties
- sounds and sound sets
- particles
- player animation sets
- interaction map / config
- inline interaction vars
- tooltip
- input binding
- hotbar overrides
- prerequisite abilities
- stat requirements
- ranks/max rank
- global cooldown settings
- ability type

### Runtime trigger path

`ClassAbilitySystem`:

1. checks trigger-block component state
2. resolves the ability asset
3. checks enablement
4. applies global cooldown logic
5. resolves the root interaction chain
6. creates interaction context
7. consumes empowerment if present
8. queues the interaction chain
9. triggers animation/sound/particle side effects

### Why this matters

This means most combat/content design should happen in assets, while Java stays responsible for validation, state, and orchestration.

The same pattern now extends further into progression content through custom interactions that can:

- check class/race/level/achievement/party/raid prerequisites
- unlock achievements directly
- grant achievement criterion progress
- teach classes
- switch races
- unlock expansions

That gives authored NPC or world interactions a much cleaner bridge into progression systems.

## Achievement system

The repo now includes a first-pass achievement framework.

### Current responsibilities

- authored achievements under `Server/Achievements`
- persistent per-character achievement progress
- persistent account-wide achievement progress
- criterion tracking from live gameplay events
- configurable unlock/progress feedback in `AchievementSettings`

### Current supported tracked actions

- reaching a level
- learning a class
- changing race
- unlocking a class ability

### Feedback authoring model

`AchievementSettings` now supports `UnlockEffects` and `ProgressGainedEffects` using the same payload shape as `LevelUpEffects`.

That means achievement feedback can be configured with:

- sound
- particles
- notifications
- event titles
- chat messages
- kill-feed popups

For the full breakdown, see [Achievement System](Achievement-System.md).

## Stats layer

`StatSystem` is the core aggregator between progression systems and actual combat/resource numbers.

Although this wiki pass does not fully document every stat source yet, the code shows it recalculates bonuses from at least:

- race
- class
- talents
- level rewards/growth

The resources folder also shows a fairly broad stat taxonomy already exists:

- primary stats
- secondary stats
- class resources
- system stats like rested XP and XP gain rate

## Practical authoring flow for progression content

A typical gameplay addition in this area looks like:

1. add/update a class or race asset
2. define or reuse a level-system asset
3. define ability assets
4. define root interactions / supporting item interactions
5. optionally add prerequisite and reward interactions for NPC/service flows
6. make sure config enables the relevant module
7. rely on existing systems to load and wire the content

For the new interaction-driven progression hooks, see [Interaction Authoring Reference](Interaction-Authoring-Reference.md).

## Current progression strengths

- highly data-driven
- supports multiple level tracks
- class and ability authoring surface is already rich
- character/account separation is strong enough for MMO workflows
- talents are integrated into real player progression rather than being UI-only placeholders

## Current progression pressure points

- character creation/select presentation still needs polish
- some ability/editor workflows still need better codec/editor support
- professions and gathering are not yet at the same maturity as combat progression
- requirements still call out more work around instance/lobby flow and reset/rested XP world integration
