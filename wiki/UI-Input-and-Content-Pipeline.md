# UI, Input, and Content Pipeline

## UI page inventory

The repo already contains a healthy set of custom UI pages.

### Current page assets

- `BankPage.ui`
- `BankSelectionPage.ui`
- `BankStoragePage.ui`
- `CharacterSelectPage.ui`
- `CurrencyPage.ui`
- `GroupFinderPage.ui`
- `GuildPage.ui`
- `LockpickingPage.ui`
- `MailPage.ui`
- `PartyPage.ui`
- `RpgInventoryPage.ui`
- `TalentTreePage_Detail_Pane.ui`
- `TalentTreePage_Tool_Tips.ui`

### Current Java page classes

- character select page
- party page
- group finder page
- guild page
- bank pages
- currency page
- mail page
- inventory page
- lockpicking page
- talent tree page
- XP bar HUD

## Character presentation pipeline

The character module already points toward a more MMO-style login presentation.

### Config-driven presentation features

- lobby world id
- default and race-specific spawn point ids
- default and race-specific background ids
- default and race-specific preview camera preset ids
- login/logout VFX lists

### What this means

The code is already structured to support polished character-select staging even though the requirements notes still call for more polish.

## HUDs

The XP bar is maintained through a dedicated HUD service and update system.

### Current pattern

- `XpBarHudService` manages attach/update/remove
- `XpBarHudUpdateSystem` keeps the display synchronized
- player ready/disconnect hooks ensure lifecycle cleanup
- active class changes also trigger HUD refreshes

## Input interception

Some core RPG features are driven by inbound packet filters.

### Current inbound filters

- ability input
- inventory open handling
- lockpicking input

These are toggled dynamically based on the main config.

## Ability input path

The ability stack uses several layers:

1. inbound input interception
2. ability binding state on the player
3. ability lookup in asset stores
4. trigger blocking / GCD checks
5. root interaction execution
6. optional animation/sound/particle side effects
7. hotbar icon sync

This is a good example of the mod blending Hytale-native input with custom RPG runtime rules.

## Interaction authoring pipeline

The interaction layer is no longer just for combat abilities.

Custom interaction registration now supports authored progression/service flows such as:

- `PrerequisiteCheck`
- `UnlockAchievement`
- `GrantAchievementProgress`
- `UnlockExpansion`
- `UnlockRace`
- `UnlockClass`
- `SendMail`

That means a root interaction chain can now:

1. validate the player's progression or social state
2. branch on success/failure
3. grant a progression reward
4. queue follow-up mail or UI steps

This is an important shift because it lets NPCs and other interactables participate in progression systems using the same authored chain model already used by abilities.

## Lockpicking input path

Lockpicking is one of the more self-contained feature loops in the repo.

It combines:

- a persistent ECS session component
- a dedicated runtime system
- a packet filter for input
- a custom UI page
- a custom interaction type (`Lockpicking`)
- difficulty tuning in the global RPG config

That makes it a useful reference implementation for future minigame-style systems.

## Chat input path

Chat is handled differently from abilities and lockpicking.

Instead of raw packet interception, it uses an async global `PlayerChatEvent` handler.

That handler:

- parses channel routing
- resolves targets
- swaps in formatter output
- blocks invalid sends

## NPC integration

The plugin adds RPG-specific behavior into the NPC subsystem through:

- core component registration for `RpgSetup`
- core component registration for `RpgCastAbility`
- `NpcRpgSetupSystem`
- `NpcRpgDebugOverlaySystem`

This suggests the intended long-term direction is to let NPC behavior consume the same class/ability stack as player-facing systems.

## Content authoring pipeline

The content pipeline is strongly asset-driven.

### Examples of authored content already present

- race definitions
- combat/profession classes
- level systems and XP curves
- ability categories and qualities
- concrete ability definitions
- root interactions and supporting interaction fragments
- progression-gating and progression-grant interactions
- bank types
- currency types
- prefab content
- stat definitions
- sound events and particles

### Authoring pattern

1. define content in JSON under `Server/...`
2. load it through an asset store registered in `ExamplePlugin`
3. let a runtime manager/system consume it
4. optionally expose it through UI pages or commands

## Asset editor considerations already visible in the repo

The requirements notes and config/code structure both show that asset-editor UX matters a lot for this project.

Some current pressure points are:

- codec support for VFX and camera definitions
- talent currency/editor ergonomics
- bank editor cleanup (`defaults` note in the requirements doc)
- more polished character select UI behavior

## Good extension points for future work

If you need to add a new UI-heavy RPG subsystem, the existing project already gives you multiple patterns to copy:

- page + `.ui` asset pair
- HUD service + update system
- packet filter + session component
- interaction-driven action chain
- manager + persistence asset + UI page

For the new progression/service interaction types, see [Interaction Authoring Reference](Interaction-Authoring-Reference.md).
