# Achievement System

This page documents the current achievement framework added to Realmweaver.

## Purpose

The achievement system provides:

- persistent per-character achievement progress
- persistent account-wide achievement progress
- data-driven achievement definitions under `Server/Achievements`
- automatic progress tracking from existing gameplay events
- configurable feedback effects from `AchievementSettings`

The implementation is split between asset definitions, a persistent ECS component, character-roster persistence, and a runtime manager/system layer.

## Main runtime pieces

### `AchievementDefinition`

Defined in [src/main/java/org/pixelbays/rpg/achievement/config/AchievementDefinition.java](../src/main/java/org/pixelbays/rpg/achievement/config/AchievementDefinition.java).

This is the authored asset type loaded from `Server/Achievements/**`.

Each achievement can currently define:

- `DisplayName`
- `Description`
- `NameTranslationKey`
- `DescriptionTranslationKey`
- `Category`
- `Points`
- `Criteria`
- `Reward`
- `AccountWide`
- `Hidden`
- `IconId`

### `AchievementComponent`

Defined in [src/main/java/org/pixelbays/rpg/achievement/component/AchievementComponent.java](../src/main/java/org/pixelbays/rpg/achievement/component/AchievementComponent.java).

This component stores runtime and persistent state:

- unlocked achievements
- per-achievement criterion progress
- total achievement points
- displayed title reward

The same component shape is used for:

- character-specific progress on the live entity
- character-specific progress inside saved character profiles
- account-wide progress inside `CharacterRosterData`

### `AchievementSystem`

Defined in [src/main/java/org/pixelbays/rpg/achievement/system/AchievementSystem.java](../src/main/java/org/pixelbays/rpg/achievement/system/AchievementSystem.java).

This system currently listens to:

- `PlayerReadyEvent`
- `LevelUpEvent`
- `ClassLearnedEvent`
- `RaceChangedEvent`
- `ClassAbilityUnlockedEvent`

On each relevant trigger it:

1. loads the authored achievement assets
2. resolves account and character achievement state
3. evaluates current criterion values from live ECS/player state
4. updates criterion progress
5. unlocks completed achievements
6. grants configured rewards/effects
7. persists account or character progress when needed

## Authored asset location

Current achievement assets live under:

```text
src/main/resources/Server/Achievements/
├── Ability/
├── Class/
├── Progression/
└── Race/
```

Sample authored assets currently include:

- `FirstSteps`
- `ChoosePath`
- `LineageAwakened`
- `FirstPower`

## Supported criterion types

The current implementation supports these criterion types:

- `ReachLevel`
- `LearnClass`
- `ChangeRace`
- `UnlockAbility`

### `ReachLevel`

Tracks a specific level-system id, or the highest observed level if no system id is supplied.

Useful fields:

- `SystemId`
- `TargetValue`

### `LearnClass`

Tracks whether a specific class was learned, or how many classes have been learned if no class id is supplied.

Useful fields:

- `ClassId`
- `TargetValue`

### `ChangeRace`

Tracks whether a race has been selected or whether the current race matches a specific race id.

Useful fields:

- `RaceId`
- `TargetValue`

### `UnlockAbility`

Tracks either a specific ability rank or total unlocked ability count.

Useful fields:

- `AbilityId`
- `TargetValue`

## Reward model

Each achievement supports a `Reward` object.

Current reward fields include:

- `DisplayedTitle`
- `DisplayedTitleTranslationKey`
- `CurrencyRewards`
- `Notification`
- `EventTitle`

### Current runtime reward behavior

When an achievement unlocks, the runtime can:

- add achievement points
- set a displayed title on the achievement state
- grant configured currency rewards
- send notification/event-title/chat feedback

## `AchievementSettings` in mod config

The top-level mod config now contains `AchievementSettings`.

This section is defined by [src/main/java/org/pixelbays/rpg/achievement/config/settings/AchievementModSettings.java](../src/main/java/org/pixelbays/rpg/achievement/config/settings/AchievementModSettings.java).

Current fields:

- `Enabled`
- `ToastNotificationsEnabled`
- `TitleNotificationsEnabled`
- `PersistProgressImmediately`
- `UnlockEffects`
- `ProgressGainedEffects`

## Global feedback effects for achievements

A key addition is that achievement settings now support effect payloads using the same shape as `LevelUpEffects`.

That means both `UnlockEffects` and `ProgressGainedEffects` can author:

- `SoundId`
- `ParticleEffect`
- `Notification`
- `EventTitle`
- `ChatMessage`
- `KillFeedPopup`

This makes achievement feedback configurable from the main mod settings without forcing every asset author to duplicate the same feedback setup.

### `UnlockEffects`

Triggered when an achievement becomes complete and is newly unlocked.

Use this for:

- toast/banner popups
- title splashes
- unlock sounds
- unlock particles
- celebratory chat text
- kill-feed style popups

### `ProgressGainedEffects`

Triggered when a tracked criterion value increases but the achievement is not yet complete.

Use this for lighter-weight feedback such as:

- progress chat messages
- subtle notifications
- progress-only particles or sounds

## Effect placeholders

Configured achievement effects support these placeholders:

- `{name}` — localized or display-name achievement name
- `{points}` — achievement point value
- `{id}` — achievement asset id
- `{current}` — current criterion value for the latest progressed criterion
- `{target}` — target value for the latest progressed criterion
- `{criterion}` — latest progressed criterion id

## Persistence model

The current persistence split is:

- character-specific progress is saved with each `CharacterProfileData`
- account-wide progress is saved on `CharacterRosterData`

That means `AccountWide = true` achievements survive character swaps, while character-bound achievements remain tied to the selected profile.

## Practical authoring example

A simple achievement asset can look like:

```json
{
  "DisplayName": "First Steps",
  "Category": "Progression",
  "Points": 10,
  "Criteria": [
    {
      "Id": "reach_base_level",
      "Type": "ReachLevel",
      "TargetValue": 2,
      "SystemId": "Base_Character_Level"
    }
  ],
  "AccountWide": false
}
```

Then the global `AchievementSettings` can decide what feedback should happen on unlock or progress gain.

## Current limitations

The current framework is intentionally first-pass.

Notable current limits:

- no dedicated achievement UI page yet
- no achievement command surface yet
- no broad library of criterion types yet
- no per-achievement override layer for the new global progress/unlock effects yet
- progress evaluation is event-driven from a small current set of gameplay events

## Recommended next documentation links

See also:

- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Config Schema Reference](Config-Schema-Reference.md)
- [Event and Integration Map](Event-and-Integration-Map.md)
