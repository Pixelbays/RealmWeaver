# Content and Asset Reference

This page maps the major authored content folders and explains how they fit into the runtime.

## Core principle

Most gameplay content in Realmweaver is authored under `src/main/resources/Server` and loaded through asset stores registered in the plugin.

Java defines codecs and runtime behavior.
JSON defines content instances and tuning.

## Main content roots

### `Server/RpgModConfig`

Purpose: live module enablement and top-level tuning.

Current `Default.json` sections include:

- general settings
- class settings
- character settings
- talent settings
- leveling settings
- ability settings
- inventory settings
- item settings
- party settings
- guild settings
- camera settings
- bank settings
- currency settings
- auction house settings
- mail settings
- lockpicking settings

This is the operational control plane for the mod.

## Progression content

### `Server/Races`

Purpose: race definitions.

Observed content shape includes:

- race id / display metadata
- hybrid relationships
- min/max size
- starting stats
- stat modifiers
- ability unlocks
- cosmetic allow/deny lists
- inheritance rules

Examples in the repo:

- `Human.json`
- `Elf.json`
- `Hybrids/HalfElf.json`
- `Base/BaseRace.json`

### `Server/Classes`

Purpose: combat, profession, and job class definitions.

Observed organization:

- `Combat/`
- `Profession/`

Observed class fields include:

- class id
- parent
- display metadata
- prerequisite classes / class levels
- level system id or shared character-level usage
- starting-class flag
- resource stats
- stat modifiers
- equipment restrictions
- ability unlock tables
- level milestones
- talent trees
- switching rules
- relearn penalty
- tags

Examples in the repo:

- `Combat/Warrior.json`
- `Combat/Paladin.json`
- `Profession/WoodWorking.json`

### `Server/Entity/levels`

Purpose: reusable level-system definitions.

Observed organization:

- `Base/`
- `Classes/`
- `Professions/`

Observed fields include:

- display metadata
- max and starting levels
- XP curve details
- prerequisites
- default rewards
- per-level reward overrides
- stat growth
- enabled/visible state
- icon

Examples in the repo:

- `Base/Base_Character_Level.json`
- `Base/Base_Class_Level.json`
- `Base/Base_Profession_Level.json`
- `Classes/Warrior.json`
- `Classes/Paladin.json`
- `Professions/Woodworking.json`

### `Server/Entity/ExpCurves`

Purpose: reusable XP curve presets.

Examples in the repo:

- `Curve_Linear.json`
- `Curve_Normal.json`
- `Curve_Fast.json`
- `Curve_Slow.json`
- `Curve_MMO_Standard.json`

### `Server/Entity/Stats`

Purpose: stat taxonomy used by progression and combat systems.

Observed subfolders:

- `Primary/`
- `Secondary/`
- `Class/`
- `System/`
- `Fun/`

This is one of the key content foundations for class/race/level/talent math.

## Ability content

### `Server/Abilities/Categories`

Purpose: creative/editor grouping and organizational metadata for abilities.

### `Server/Abilities/Qualities`

Purpose: quality tiers/default ability quality data.

### `Server/Abilities/Abilities`

Purpose: concrete ability definitions.

Observed organization includes:

- class ability bases
- class-specific abilities
- race ability bases
- race-specific abilities

Examples in the repo:

- `Fireball.json`
- `ClassAbilities/Warrior/Charge.json`
- `ClassAbilities/Paladin/LayOnHands.json`
- `ClassAbilities/Paladin/DivineShield.json`
- `RaceAbilities/Human/Adaptability.json`
- `RaceAbilities/Elf/Trance.json`

### `Server/Item/RootInteractions/RPG`

Purpose: top-level interaction chains used by abilities and RPG actions.

This is now also the natural authoring home for progression/service chains such as:

- prerequisite gates
- achievement rewards
- class or race unlock services
- expansion unlock flows
- mail-triggering interactions

### `Server/Item/Interactions/RPG`

Purpose: supporting interaction fragments/effects used by those root interactions.

The RPG custom interaction registry now includes both combat-oriented and progression-oriented interaction types.

Notable custom ids include:

- `ForceTarget`
- `DiceRoll`
- `EmpowerAbility`
- `Lockpicking`
- `OpenBankUi`
- `OpenMailUi`
- `PrerequisiteCheck`
- `UnlockAchievement`
- `GrantAchievementProgress`
- `UnlockExpansion`
- `UnlockRace`
- `UnlockClass`
- `SendMail`

This split is central to how abilities actually execute in runtime.

It is now also how authored content can gate and grant non-combat progression.

## Economy content

### `Server/Currencies/Types`

Purpose: currency type definitions.

Observed fields and behavior areas include:

- storage mode
- allowed scopes
- starting balance
- min/max balance
- negative allowance
- conversion rules
- auction house allowlist behavior

Examples:

- `Base.json`
- `Gold.json`
- `Silver.json`
- `DungeonToken.json`

### `Server/Banks/Types`

Purpose: bank type definitions.

Observed examples:

- `Personal.json`
- `Account.json`
- `Guild.json`
- `Void.json`
- `Warbound.json`
- `Professions.json`
- `Base.json`

These drive default bank creation and UI tab behavior.

### Persistence-backed economy data roots

These are primarily runtime data rather than authored design content:

- `Server/CurrencyData`
- `Server/BankData`
- `Server/AuctionData`
- `Server/MailData`

## Social and roster content

### Runtime data roots

- `Server/PartyData`
- `Server/GuildData`
- `Server/CharacterData`
- `Server/ExpansionData`
- `Server/MailData`

These are persistence-oriented stores rather than hand-authored progression design assets.

`Server/MailData` now also doubles as a useful authored-template source for the `SendMail` interaction when you want reusable system-mail payloads.

## Misc gameplay content

### `Server/Drops`

Purpose: drop definitions.

### `Server/Prefabs`

Purpose: prefab-backed content such as gates or future encounter/lobby structures.

### `Server/Particles`

Purpose: server-side particle/VFX assets referenced by systems and level-up effects.

### `Server/Audio`

Purpose: sound events and related audio data.

## UI content

UI markup lives under:

```text
src/main/resources/Common/UI/Custom/Pages/
```

This is separate from `Server/...` because UI assets are part of the shared content layer rather than only server gameplay data.

## Authoring workflow summary

For most new systems, the content workflow is:

1. define the codec in Java if a new asset type is needed
2. register the asset store in the plugin
3. add JSON under the relevant `Server/...` folder
4. reference the asset from systems, commands, UI, or interactions
5. tune enablement and defaults through `RpgModConfig`

## Good documentation follow-ups from here

The next deeper pass should add field-by-field authoring guides for:

- classes
- races
- level systems
- abilities
- currencies
- bank types
- mail data model
- expansion definitions inside general settings
