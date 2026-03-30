# Interaction Authoring Reference

This page documents the RPG-specific custom interaction types added on top of Hytale's base interaction runtime.

## Why this matters

Realmweaver already uses interaction chains for abilities, lockpicking, bank UI, mail UI, targeting helpers, and dice-roll style branching.

The mod now also supports interaction-driven:

- prerequisite checks
- achievement unlocks
- achievement progress grants
- expansion unlocks
- class unlocks
- race unlocks
- system mail delivery

That means NPCs, ability chains, interactables, dialogue trees, and authored content can now gate or grant progression directly through JSON interaction chains instead of requiring one-off Java hooks.

## Registered custom interaction types

The plugin currently registers these notable Realmweaver interaction types:

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

## `PrerequisiteCheck`

Purpose: fail or continue a chain based on optional player progression/social requirements.

Supported requirement fields:

- `RequiredClassIds`
- `RequiredClassLevel`
- `RequiredRaceIds`
- `RequiredLevel`
- `RequiredLevelSystemId`
- `RequiredAchievementIds`
- `RequireParty`
- `RequiredPartyCount`
- `RequireRaid`
- `RequiredRaidCount`
- `SendFailureMessage`

Behavior notes:

- every requirement family is optional
- if a family is omitted, it is ignored
- if any supplied family fails, the interaction fails
- when `SendFailureMessage` is `true`, the player gets a translated requirement message
- this is designed to work with normal interaction-chain `Next` and `Failed` routing

### Example

```json
{
  "Type": "PrerequisiteCheck",
  "RequiredClassIds": ["Warrior", "Paladin"],
  "RequiredClassLevel": 10,
  "RequiredAchievementIds": ["ChoosePath"],
  "RequireParty": true,
  "RequiredPartyCount": 3,
  "SendFailureMessage": true,
  "Next": "RPG_Challenge_Start",
  "Failed": "RPG_Challenge_Deny"
}
```

## `UnlockAchievement`

Purpose: directly unlock one or more achievements for the interacting player.

Fields:

- `AchievementIds`

Behavior notes:

- respects the achievement definition's account-wide vs character-scoped storage
- reuses the achievement system's reward and notification pipeline
- already-unlocked achievements are treated as a successful no-op

### Example

```json
{
  "Type": "UnlockAchievement",
  "AchievementIds": ["FirstSteps", "ChoosePath"]
}
```

## `GrantAchievementProgress`

Purpose: add progress to a specific achievement criterion.

Fields:

- `AchievementId`
- `CriterionId`
- `Amount`

Behavior notes:

- if the target achievement has exactly one criterion, `CriterionId` can be omitted
- if the progress grant satisfies the achievement, the unlock flow runs immediately
- this is the interaction to use for NPC dialogue, interactables, or scripted events that should move an achievement forward without relying on normal gameplay event capture

### Example

```json
{
  "Type": "GrantAchievementProgress",
  "AchievementId": "FirstSteps",
  "CriterionId": "ReachLevel2",
  "Amount": 1
}
```

## `UnlockExpansion`

Purpose: grant one or more expansion entitlements to the player.

Fields:

- `ExpansionIds`

Behavior notes:

- writes through the normal `ExpansionManager` persistence path
- meant for admin flows, rewards, account-service content, or special progression grants

### Example

```json
{
  "Type": "UnlockExpansion",
  "ExpansionIds": ["founders_pack", "season_one"]
}
```

## `UnlockClass`

Purpose: teach one or more classes to the interacting entity.

Fields:

- `ClassIds`

Behavior notes:

- uses `ClassManagementSystem.learnClass(...)`
- therefore still respects normal class validation such as prerequisites, expansion gates, and class limits
- if the entity already knows the class, the interaction still counts as success

### Example

```json
{
  "Type": "UnlockClass",
  "ClassIds": ["Paladin"]
}
```

## `UnlockRace`

Purpose: set the race for the interacting entity.

Fields:

- `RaceId`

Behavior notes:

- uses the normal `RaceSystem`
- applies race stats and race ability unlocks through the existing runtime
- useful for authored race-change shrines, NPC services, or scripted onboarding chains

### Example

```json
{
  "Type": "UnlockRace",
  "RaceId": "Elf"
}
```

## `SendMail`

Purpose: queue system mail to the interacting player.

Supported fields:

- `MailId`
- `SenderName`
- `Subject`
- `Body`

Behavior notes:

- when `MailId` is supplied, the interaction clones the authored `Server/MailData` template
- otherwise it sends a simple inline system mail using the provided sender/subject/body fields
- inline mode currently focuses on message text, while template mode supports the richer authored mail payload shape

### Template example

```json
{
  "Type": "SendMail",
  "MailId": "WelcomePackage"
}
```

### Inline example

```json
{
  "Type": "SendMail",
  "SenderName": "Archivist Elowen",
  "Subject": "Your first assignment",
  "Body": "Meet the quartermaster in the lower courtyard."
}
```

## Recommended authoring patterns

### Gate, then grant

Common progression chain shape:

1. `PrerequisiteCheck`
2. success branch into one or more reward/grant interactions
3. failure branch into dialogue or message content

### Prefer chain composition over giant one-off interactions

Instead of making one monolithic interaction do everything, prefer authored chains such as:

- `PrerequisiteCheck` -> `UnlockClass`
- `PrerequisiteCheck` -> `UnlockRace` -> `UnlockAchievement`
- `PrerequisiteCheck` -> `GrantAchievementProgress` -> `SendMail`

### Use mail templates for reusable rewards

If the same reward mail is sent from multiple sources, prefer `MailId` templates in `Server/MailData` rather than repeating inline message text everywhere.

## Where these interactions fit best

These new interaction types are especially useful for:

- NPC conversation outcomes
- challenge gates and entrance checks
- account-service style interactables
- onboarding/tutorial chains
- class trainers and race-change services
- story triggers that should grant achievements or mail rewards

## Related pages

- [Ability Trigger Flow](Ability-Trigger-Flow.md)
- [Content and Asset Reference](Content-and-Asset-Reference.md)
- [UI, Input, and Content Pipeline](UI-Input-and-Content-Pipeline.md)
- [Social and Economy Systems](Social-and-Economy-Systems.md)
- [Progression and Character Systems](Progression-and-Character-Systems.md)