# Config Schema Reference

This page documents the top-level shape of `Server/RpgModConfig/Default.json`.

## Purpose

`RpgModConfig` is the master runtime config asset for the mod.

It controls:

- module enablement
- gameplay tuning
- UI behavior
- social/economy defaults
- service-style features like expansions

The plugin treats this file as the live control plane.

## Top-level sections

The current schema contains these sections:

- `GeneralSettings`
- `ClassSettings`
- `CharacterSettings`
- `AchievementSettings`
- `TalentSettings`
- `LevelingSettings`
- `AbilitySettings`
- `InventorySettings`
- `ItemSettings`
- `PartySettings`
- `GuildSettings`
- `CameraSettings`
- `BankSettings`
- `CurrencySettings`
- `AuctionHouseSettings`
- `MailSettings`
- `LockpickingSettings`

## `GeneralSettings`

High-level server-wide RPG settings.

Current observed responsibilities include:

- debugging mode
- player logging flags
- anti-grind options
- race requirement at start
- global mob scaling
- server name / website / discord info
- expansion definitions
- advantage roll modifier tables

This section is also where expansion definitions currently live.

## `ClassSettings`

Controls the class framework.

Observed behavior areas:

- module enablement
- class mode
- active class mode
- class tags
- XP routing
- XP tag splits
- max combat and profession class counts
- class switching rules
- require class at start
- ability learning mode

This section is one of the key policy layers for the class module.

## `CharacterSettings`

Controls the account/character roster and select flow.

Observed behavior areas:

- module enablement
- max character slots
- creation/deletion toggles
- logout-to-character-select
- deletion/recovery tuning
- login/logout VFX
- legacy migration
- unique-name policy
- race/class requirements on creation
- lobby world selection
- spawn point/background/camera preset selection

This section is central to the MMO-style multi-character flow.

## `AchievementSettings`

Controls the achievement framework and its global feedback behavior.

Observed behavior areas:

- module enablement
- toast notification enablement
- title notification enablement
- immediate persistence of progress
- global unlock feedback effects
- global progress-gained feedback effects

The feedback effect payloads currently reuse the same shape as `LevelUpEffects`, so the config can author:

- `SoundId`
- `ParticleEffect`
- `Notification`
- `EventTitle`
- `ChatMessage`
- `KillFeedPopup`

See [Achievement System](Achievement-System.md) for the full runtime behavior.

## `TalentSettings`

Controls talent-tree policy.

Observed behavior areas:

- module enablement
- spec mode
- reset mode and partial refund logic
- paid reset hooks

## `LevelingSettings`

Controls global leveling-side modifiers rather than the authored per-level-system assets.

Observed behavior areas:

- module enablement
- rested XP
- hardcore penalties
- currency loss on death
- base XP multiplier
- default player profile count

## `AbilitySettings`

Controls global behavior for the ability stack.

Observed behavior areas:

- module enablement
- base global cooldown
- hotbar ability slots

## `InventorySettings`

Controls the custom inventory handling layer.

Observed behavior areas:

- module enablement
- inventory handling mode
- default inventory size
- stat-driven slot expansion
- extra slot toggles for equipment categories

This module exists, but the broader inventory system is still marked unstable in project notes.

## `ItemSettings`

Controls item-layer RPG behavior.

Observed behavior areas:

- module enablement
- level scaling tag
- scale-per-level percentage

## `PartySettings`

Controls the party/raid/group-finder stack.

Observed behavior areas:

- module enablement
- group finder enablement
- party/raid enablement
- size limits
- assistant counts
- invite expiry
- XP sharing mode/range
- NPC allowance
- persistence enablement/interval

## `GuildSettings`

Controls guild policy.

Observed behavior areas:

- module enablement
- guild enablement
- max members
- invite expiry
- naming/tag rules
- default join policy
- membership scope
- persistence enablement/interval

## `CameraSettings`

Controls current camera/targeting style policy.

Observed behavior areas:

- module enablement
- targeting style
- camera style

## `BankSettings`

Controls bank-system defaults.

Observed behavior areas:

- module enablement
- default bank type ids by scope
- allow asset-defined bank types

## `CurrencySettings`

Controls the currency module.

Observed behavior areas:

- module enablement
- persistence enablement
- allow asset-defined currency types

## `AuctionHouseSettings`

Controls auction house policy.

Observed behavior areas:

- module enablement
- bidding and buyout toggles
- listing durations and fees
- success fee percent
- mail delays
- max active listings
- rate limiting
- minimum level to post
- seller cancel rules
- persistence

## `MailSettings`

Controls the mail subsystem.

Observed behavior areas:

- module enablement
- ownership mode
- attachment toggles
- COD toggle
- delay reductions
- subject/body/attachment limits
- expiry days
- persistence

## `LockpickingSettings`

Controls lockpicking difficulty and rules.

Observed behavior areas:

- module enablement
- lockpick item tag
- named difficulty tiers
- pin counts, timers, sweet-spot sizes, speed scaling, mistake limits

## Operational guidance

When documenting or debugging any module, always check:

1. whether its section exists in `RpgModConfig`
2. whether `Enabled` is true
3. whether the plugin dynamically gates commands/packet filters/managers for that section

## Related pages

- [Architecture](Architecture.md)
- [Content and Asset Reference](Content-and-Asset-Reference.md)
- [Commands Reference](Commands-Reference.md)
