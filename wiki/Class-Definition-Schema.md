# Class Definition Schema

This page documents the main shape of `ClassDefinition` assets under `Server/Classes`.

## Purpose

A class asset defines how a combat class, profession, or job behaves.

It is one of the richest authored asset types in the repo.

## Top-level fields

### Identity and presentation

- `Parent`
- `DisplayName`
- `Description`
- `Icon`
- `Enabled`
- `Visible`
- `RequiredExpansionIds`

Use these for inheritance, UI display, and availability gating.

### Unlock prerequisites

- `Prerequisites`
- `RequiredClasses`
- `ExclusiveWith`

These fields control what must already be learned or leveled before the class can be acquired.

### Leveling integration

- `LevelSystemId`
- `UsesCharacterLevel`
- `IsStartingClass`
- `IsHeroClass`
- `HeroStartingLevel`

These fields decide whether the class has its own level track, shares the main character level, starts at character creation, or starts above baseline.

### Resource and stat model

- `ResourceStats`
- `BaseStatModifiers`
- `PerLevelModifiers`

Use these to define the class's resource priorities and its passive stat math.

### Equipment policy

- `EquipmentRestrictions`

Nested structure:

- `RestrictionMode`
- `AllowedWeaponTypes`
- `AllowedArmorTypes`
- `RequiredItems`

`RestrictionMode` values:

- `None`
- `Soft`
- `Hard`

### Ability unlocks

- `AbilityUnlocks`

Each unlock entry can define:

- `AbilityId`
- `UnlockLevel`
- `MaxRank`
- `ItemRewards`
- `LearnCosts`

`LearnCosts` entries can define:

- `CurrencyId`
- `Amount`
- `CurrencyScope`

This supports trainer-style or milestone-style ability acquisition.

### Level milestones

- `LevelMilestones`

Each milestone can define:

- `Level`
- `SkillPoints`
- `ItemRewards`
- `InteractionChain`

This is used for non-standard rewards that are not simply part of the regular ability unlock list.

### Talent trees

- `TalentTrees`

Each tree can define:

- `TreeId`
- `DisplayName`
- `Description`
- `Icon`
- `MaxPoints`
- `PrerequisiteRankMode`
- `Nodes`

`PrerequisiteRankMode` values:

- `OnePoint`
- `FullRank`

Each `TalentNode` can define:

- `NodeId`
- `DisplayName`
- `Description`
- `Icon`
- `MaxRank`
- `RequiredLevel`
- `RequiredNodes`
- `GrantsAbilityId`
- `StatModifiers`
- `PositionX`
- `PositionY`

`PositionX` is validated to remain in a narrow layout range, which is a clue that the talent UI is grid-oriented.

### Switching rules

- `SwitchingRules`

Nested structure:

- `CanSwitch`
- `CanSwitchInCombat`
- `SwitchCooldown`

These fields are used by `ClassManagementSystem` when activating classes.

### Misc policy

- `RelearnExpPenalty`
- `AbilityControlTypeOverride`

These fields support respec/relearn penalties and control-model overrides.

## Runtime implications

The class asset drives all of these systems:

- class learning and activation
- class level routing
- race/class compatibility checks
- stat recalculation
- ability unlock timing
- talent tree behavior
- equipment restriction handling
- expansion-based class gating

## Example patterns

### Starting combat class

Use:

- `IsStartingClass = true`
- `UsesCharacterLevel = true` or a dedicated starting level system
- light prerequisite set

### Advanced prestige/hero class

Use:

- `RequiredClasses`
- `Prerequisites`
- `RequiredExpansionIds`
- `IsHeroClass = true`
- `HeroStartingLevel > 1`

### Profession/job class

Use tags and class settings so the runtime counts it correctly against profession/job limits.

## Known authoring cautions

- `LevelSystemId` and `UsesCharacterLevel` must make sense together.
- `RequiredClasses` and `Prerequisites` can easily create dead-end progression if not planned carefully.
- `ExclusiveWith` should be used sparingly because it directly constrains build combinations.
- talent nodes should be laid out with UI readability in mind, not just runtime correctness.

## Example files in repo

- `Server/Classes/Combat/Warrior.json`
- `Server/Classes/Combat/Paladin.json`
- `Server/Classes/Profession/WoodWorking.json`

## Related pages

- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Level System Schema](Level-System-Schema.md)
- [Commands Reference](Commands-Reference.md)
