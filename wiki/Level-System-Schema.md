# Level System Schema

This page documents the main shape of `LevelSystemConfig` assets under `Server/Entity/levels`.

## Purpose

A level-system asset defines one reusable progression track.

The codebase already uses this concept for:

- base character progression
- class progression
- profession progression

## Top-level fields

### Identity and presentation

- `DisplayName`
- `Description`
- `Parent`
- `Enabled`
- `Visible`
- `Icon`

`Parent` allows inheritance between level-system configs.

### Level range

- `MaxLevel`
- `StartingLevel`

These define the bounds and starting point for the track.

### Experience curve

- `ExpCurveRef`

Internally the config also caches curve-related fields such as `ExpCurveType`, `ExpCurve`, and `ExpTable`, but authored content should primarily rely on the configured curve reference and the linked curve asset.

### Cross-system prerequisites

- `Prerequisites`

This is a map of other level-system ids to required minimum levels.

That makes the overall progression model capable of expressing gated secondary tracks.

### Reward model

- `DefaultRewards`
- `LevelRewards`

`DefaultRewards` are applied broadly.
`LevelRewards` override or add milestone-specific behavior at exact levels.

Observed reward capabilities in the live content include:

- stat points
- skill points
- stat increases
- item rewards
- unlocked abilities / quests placeholders
- interaction chains
- level-up sound/particle/notification payloads

### Stat growth

- `StatGrowth`

This is the long-term stat scaling model for the progression track.

Observed subareas in authored content include:

- flat growth
- percentage growth
- milestone growth

## Runtime implications

`LevelProgressionSystem` uses these assets for:

- XP accumulation
- level cap checks
- reward application
- stat growth application
- class-ability unlock timing
- prerequisite checks across systems

## Authoring patterns

### Base character level

Use:

- large max level
- broad default rewards
- global milestone unlocks
- stat growth that sets the baseline for the whole character

### Class-specific level track

Use:

- class-themed milestone rewards
- class-specific stat growth
- separate cap from base character level if desired

### Profession track

Use:

- lower direct combat stat growth
- profession-specific milestone rewards
- recipe/gathering-related progression hooks as those systems mature

## Known cautions

- a system that has `UsesCharacterLevel` in a class definition should not accidentally depend on a conflicting class-specific level track
- cross-system prerequisites can create confusing progression walls if not documented in content
- reward duplication between `DefaultRewards` and exact `LevelRewards` should be intentional

## Example files in repo

- `Server/Entity/levels/Base/Base_Character_Level.json`
- `Server/Entity/levels/Base/Base_Class_Level.json`
- `Server/Entity/levels/Base/Base_Profession_Level.json`
- `Server/Entity/levels/Classes/Warrior.json`
- `Server/Entity/levels/Classes/Paladin.json`
- `Server/Entity/levels/Professions/Woodworking.json`

## Related pages

- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Class Definition Schema](Class-Definition-Schema.md)
