# Realmweaver Wiki

This wiki is a working deep-dive of the current Realmweaver codebase and asset pack.
It is based on the actual Java systems under `src/main/java`, the data assets under `src/main/resources/Server`, and the current MMO/RPG requirements notes.

## What this mod already is

Realmweaver is not a single feature. It is a collection of live-toggleable MMO/RPG subsystems built on top of Hytale's server ECS and asset pipeline.

The current implementation already includes:

- character roster and character select flow
- race definitions and race-based ability/stat hooks
- leveling with reusable level-system assets
- class learning, switching, prerequisites, and per-class level tracks
- talent trees backed by class definitions
- ability unlocks, hotbar bindings, global cooldowns, and interaction-driven casts
- party, group finder, guild, and channel chat systems
- currencies, banks, auction house, and mail
- lockpicking UI + packet/input handling
- XP HUD updates, death-drop integrations, and some NPC RPG hooks

## Big picture architecture

The real composition root is `org.pixelbays.plugin.ExamplePlugin`.

That plugin:

1. registers asset stores
2. registers ECS components
3. wires systems and managers together
4. toggles commands and packet filters based on `Server/RpgModConfig/Default.json`
5. reloads persistence-backed managers when config changes

The mod is therefore **data-driven first** and **manager/system driven second**.

## Current status snapshot

| Area | Status | Notes |
| --- | --- | --- |
| Character roster / select | Implemented | Includes lobby world, preview camera/background selection, soft-delete recovery, legacy migration |
| Races | Implemented | Data-driven race assets and unlock hooks are in place |
| Leveling | Implemented | Reusable level systems, rewards, stat growth, rested XP/hardcore hooks |
| Classes | Implemented | Learn/unlearn/switch, prerequisites, expansion gating, class milestones |
| Talents | Implemented / maturing | Allocation, reset, refund, granted abilities; more editor-side UX still needed |
| Abilities | Implemented | Root interactions, GCD, sound/particle/animation support, hotbar sync |
| Parties / raids | Implemented | Invites, leadership, NPC members, XP-sharing settings, UI |
| Guilds | Implemented | Roles, permissions, invites/applications, join policy, UI |
| Chat | Implemented | Async channel routing for party/guild chat |
| Currency | Implemented | Wallet + physical item support, scope-aware balances, auto-conversion |
| Banks | Implemented | Multiple bank scopes/types/tabs, UI, currency-aware costs |
| Auction house | Implemented core | Listing creation/cancel/sold/expired flow exists; broader gameplay integration still evolving |
| Mail | Implemented core | Mailboxes, attachments, COD, delivery delay, broadcasts |
| Inventory overhaul | Partial | Config exists, custom page exists, requirement notes still mark this area as broken/incomplete |
| Gathering / professions | Early | Some profession class/level assets exist, but framework is not yet complete |
| Instances / encounters | Planned | Mentioned heavily in requirements, but not yet a finished gameplay framework |

## Start here

- [Architecture](Architecture.md)
- [Systems Overview](Systems-Overview.md)
- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Achievement System](Achievement-System.md)
- [Social and Economy Systems](Social-and-Economy-Systems.md)
- [Commands Reference](Commands-Reference.md)
- [Content and Asset Reference](Content-and-Asset-Reference.md)
- [Expansions and Entitlements](Expansions-and-Entitlements.md)
- [Config Schema Reference](Config-Schema-Reference.md)
- [Class Definition Schema](Class-Definition-Schema.md)
- [Race Definition Schema](Race-Definition-Schema.md)
- [Level System Schema](Level-System-Schema.md)
- [Economy Schema Reference](Economy-Schema-Reference.md)
- [Ability Definition Schema](Ability-Definition-Schema.md)
- [Interaction Authoring Reference](Interaction-Authoring-Reference.md)
- [Character Select Flow](Character-Select-Flow.md)
- [Ability Trigger Flow](Ability-Trigger-Flow.md)
- [Event and Integration Map](Event-and-Integration-Map.md)
- [UI Page Reference](UI-Page-Reference.md)
- [UI, Input, and Content Pipeline](UI-Input-and-Content-Pipeline.md)
- [Roadmap and Known Gaps](Roadmap-and-Known-Gaps.md)

## Important repo paths

```text
src/main/java/org/pixelbays/plugin/ExamplePlugin.java   -> plugin composition root
src/main/java/org/pixelbays/rpg/                        -> gameplay packages
src/main/resources/Server/                              -> gameplay data assets
src/main/resources/Common/UI/Custom/Pages/              -> custom UI markup
src/main/resources/Server/RpgModConfig/Default.json     -> live module config
src/main/resources/Server/Languages/en-US/pixelbays.lang-> translations
wiki/                                                   -> this wiki
```

## Design principles seen in the codebase

- Prefer JSON assets over hard-coded gameplay constants.
- Keep feature enablement under the main RPG config.
- Store persistent feature state in asset-backed data blobs rather than a separate DB layer.
- Use ECS components for player/entity state.
- Use manager classes for feature orchestration and persistence.
- Use packet filters and interaction chains for low-level gameplay input.
- Bounce gameplay mutations onto the world thread when touching live entity state.

## Suggested next documentation passes

This first pass maps the codebase. The next pass should add:

- per-command reference pages
- asset schema reference pages for classes, races, abilities, banks, currencies, and mail
- flow diagrams for character login/select/spawn
- system-to-system event maps
- contributor guides for adding new Realmweaver modules

## Second pass additions

The wiki now also includes:

- a command-surface reference
- a content-folder and asset-authoring reference
- an expansion/entitlement deep dive

## Third pass additions

The wiki now also includes first-pass schema references for:

- the master RPG config
- class assets
- race assets
- level-system assets
- bank and currency assets

## Fourth pass additions

The wiki now also includes:

- an ability asset schema page
- a character-select/runtime flow page
- an ability trigger/input/runtime flow page

## Fifth pass additions

The wiki now also includes:

- a cross-system event and integration map
- a page-by-page UI controller reference
