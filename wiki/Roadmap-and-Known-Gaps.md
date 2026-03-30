# Roadmap and Known Gaps

This page blends the current codebase state with the active requirements notes.

## Reading this page correctly

Some systems in the requirements document are already partially or fully implemented in code.
Others are still clearly aspirational.

This page separates **current pressure points** from **future systems**.

## Current pressure points called out by the repo

### Character select / creation polish

The requirements notes explicitly call for:

- polishing the character select screen
- class selection UI work
- race creator UI work
- fixing weird bits in the flow
- testing UI behavior
- showing the server name in more places

This lines up with what the code already supports: the foundation exists, but the presentation layer still needs iteration.

### Lobby and login presentation

The requirements notes also call out:

- using an instance template for the lobby world
- tying into machinima/login effects
- VFX codec setup

The character system already has config slots for preview cameras, backgrounds, spawn points, and login/logout VFX, so this looks like an area where the runtime foundation is ahead of the tooling/content polish.

### Banking asset-editor cleanup

The notes specifically mention:

- the asset editor needing a codec to fetch created banks
- removing unnecessary `defaults`
- tweaking small bits
- testing the UI

That suggests the bank runtime is fairly far along, while asset-authoring ergonomics still need cleanup.

### Inventory remains unstable

The requirements notes openly state the inventory area is very broken right now.

That matches the current codebase shape:

- inventory config exists
- an inventory page exists
- packet interception exists
- but the feature is not yet at the same confidence level as class/party/guild/currency systems

### Gathering / crafting professions are not finished

The notes call out:

- gathering nodes
- prefab-backed gathering possibilities
- respawn timing rules
- profession setup and crafting flows

The repo already has some profession-adjacent assets, but not yet a clearly complete profession framework.

## Systems that look like the next major implementation waves

### Instances and encounter content

This is one of the most important future gaps.

The notes repeatedly call out:

- instance templates
- instanced systems
- zoning/dungeons
- more seamless instancing

The repo has prefabs and some related content structure, but not yet a clearly complete encounter/instance gameplay layer.

### Threat / aggro system

The requirements call for a dedicated threat table framework.

This is not yet reflected as a finished standalone runtime module in the current code scan.

### Advanced quest framework

Quest chains, repeatables, daily reset logic, and richer quest assets are still requirements-stage work.

### Broader guild progression

Guild membership and role systems already exist, but deeper guild progression/banks/perks/leveling are still future-facing compared with the current implementation.

### Buff/debuff + resistance framework expansion

The current repo already touches stats/effects through abilities and stat systems, but the more MMO-style stacking, dispel, immunity, and diminishing-returns framework from the requirements is still a future layer.

### Reputation / achievements / combat logging

These are still roadmap-grade systems rather than mature modules in the current repo.

### Advanced loot system

There is some loot and randomized equipment work, but not yet the full personal loot / lockout / roll-resolution system described in the requirements.

### Full crafting profession system

Still a roadmap item even though there are related assets and profession concepts in the repo.

### Mount / pet collection framework

Pieces may exist in the wider Hytale ecosystem, but this repo does not yet present a fully realized MMO collection framework.

### PvP frameworks

Arena/battleground/honor systems remain future work.

### World event systems

Seasonal, boss, and multi-phase world-event orchestration is still roadmap territory.

## Suggested implementation/documentation order

To keep the project coherent, the next best sequence likely is:

1. **finish character-select polish**
   - highest player-facing value
   - already has working foundations
2. **stabilize inventory**
   - blocks multiple other systems
3. **finish gathering/profession core loop**
   - unlocks crafting economy depth
4. **build instance/lobby template flow**
   - enables dungeons and encounter progression
5. **expand loot + encounter framework together**
   - avoids rework from building them in isolation
6. **add quest/reputation/achievement layers**
   - better once progression and content loops are more settled

## Contributor caution list

When implementing roadmap work, preserve these existing strengths:

- keep systems data-driven
- avoid bypassing `RpgModConfig`
- reuse manager + persistence patterns where appropriate
- mutate live entity state on the world thread
- favor reusable assets over one-off hard-coded feature logic

## Documentation gaps still worth filling next

This first wiki pass does **not** yet fully document:

- command syntax and subcommands
- asset schema field-by-field reference
- event map for progression/social systems
- persistence file examples for all manager-backed subsystems
- UI interaction contracts for each page
- NPC ability/content authoring flow

Those should be the next documentation wave after the initial architecture map.
