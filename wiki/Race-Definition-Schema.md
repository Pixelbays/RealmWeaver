# Race Definition Schema

This page documents the main shape of `RaceDefinition` assets under `Server/Races`.

## Purpose

A race asset defines racial identity, stat behavior, unlockable racial abilities, cosmetic rules, and hybrid inheritance rules.

## Top-level fields

### Identity and presentation

- `DisplayName`
- `Description`
- `IconId`
- `Enabled`
- `Visible`
- `RequiredExpansionIds`

These fields control whether the race is visible/selectable and whether it is gated behind an expansion.

### Hero-race support

- `IsHeroRace`
- `HeroStartingLevel`

This mirrors the hero-class concept and allows races to influence initial progression level.

### Hybrid model

- `IsHybrid`
- `ParentRaces`
- `CompatibleHybridRaces`
- `IncompatibleHybridRaces`
- `InheritanceRules`

`InheritanceRules` contains:

- `StatBlendMode`
- `AbilityMerge`
- `CosmeticMerge`
- `WeightA`
- `WeightB`

Observed enum values:

- `StatBlendMode`: `Average`, `Weighted`, `Override`
- `AbilityMerge`: `Union`, `Override`
- `CosmeticMerge`: `WhiteList_Union`, `WhiteList_Intersect`, `Override`

This is the current schema basis for hybrid-race logic.

### Size and body-scale controls

- `MinSize`
- `MaxSize`

These define allowed/randomized size range behavior for the race.

### Starting stats and stat modifiers

- `StartingStats`
- `StatModifiers`

`StartingStats` is a direct stat-value map.
`StatModifiers` is the more reusable additive/multiplicative modifier structure.

### Racial ability support

- `AbilityUnlocks`
- `AbilityIds`

`AbilityUnlocks` entries can define:

- `AbilityId`
- `AbilityType`
- `UnlockType`
- `RequiredLevel`

Observed enum values:

- `AbilityType`: `Race`, `Class`
- `UnlockType`: `Starting`, `Level`, `Quest`, `Item`, `Other`

The `AbilityIds` list acts as a simpler direct list alongside the richer unlock structure.

### Cosmetic allow/deny rules

- `AllowedCosmeticCategories`
- `AllowedCosmeticIds`
- `NotAllowedCosmeticCategories`
- `NotAllowedCosmeticIds`

These fields are important for race-specific appearance constraints.

## Runtime implications

Race assets currently influence:

- character creation/select flow
- stat recalculation
- race ability unlocks
- class eligibility checks when class tags restrict by race
- future hybrid-race logic

## Authoring patterns

### Simple base race

Use:

- `IsHybrid = false`
- direct `StartingStats`
- one or more starting racial ability unlocks
- clean cosmetic allowlist

### Hybrid-support race

Use:

- compatibility lists
- inheritance rules tuned for the intended blend behavior
- clear parent-race semantics

### Hero race

Use:

- `IsHeroRace = true`
- `HeroStartingLevel` above 1
- optionally expansion gating

## Known cautions

- `AbilityIds` and `AbilityUnlocks` should not drift apart conceptually.
- hybrid compatibility lists should be planned as a network, not authored one race at a time blindly.
- cosmetic rules can become hard to debug if both allow and deny lists are heavily populated.

## Example files in repo

- `Server/Races/Human.json`
- `Server/Races/Elf.json`
- `Server/Races/Base/BaseRace.json`
- `Server/Races/Hybrids/HalfElf.json`

## Related pages

- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Expansions and Entitlements](Expansions-and-Entitlements.md)
