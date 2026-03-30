# Systems Overview

This page gives a fast map of the feature modules currently present in the repo.

## Feature matrix

| Module | Main Java areas | Main assets | Summary |
| --- | --- | --- | --- |
| Characters | `rpg/character` | `Server/CharacterData`, config section | Account roster, character creation/selection, lobby flow, soft-delete recovery |
| Races | `rpg/race` | `Server/Races` | Race definitions, race stats, race ability unlocks, hybrid support scaffolding |
| Leveling | `rpg/leveling` | `Server/Entity/levels`, `Server/Entity/ExpCurves` | Reusable level systems, XP gain, rewards, stat growth, hardcore/rested hooks |
| Classes | `rpg/classes` | `Server/Classes` | Learn/unlearn/switch classes, prerequisites, level routing, milestones |
| Talents | `rpg/classes/talent` | embedded in class assets + talent settings | Tree allocation, refunds, resets, node-granted abilities |
| Abilities | `rpg/ability` | `Server/Abilities`, `Server/Item/RootInteractions`, `Server/Item/Interactions` | Ability definitions, input binding, GCD, interactions, particles/sounds |
| Custom interactions | `rpg/global/interaction` | `Server/Item/RootInteractions`, `Server/Item/Interactions`, `Server/MailData` | Combat helpers plus prerequisite checks, unlock/grant flows, and mail-triggering interaction steps |
| Stats | `rpg/global/system/StatSystem` | `Server/Entity/Stats` | Class/race/talent/level-driven stat recalculation |
| Parties | `rpg/party` | `Server/PartyData` | Party/raid membership, invites, assistants, NPC members, XP sharing |
| Group Finder | `rpg/party/finder` | config-driven | Group listing/search UI on top of party data |
| Guilds | `rpg/guild` | `Server/GuildData` | Roles, permissions, applications, invites, join policy |
| Chat | `rpg/chat` | translations/config | Async routed party/guild chat channels |
| Currency | `rpg/economy/currency` | `Server/Currencies/Types`, `Server/CurrencyData` | Scope-aware wallet/item currency system with conversion rules |
| Banks | `rpg/economy/banks` | `Server/Banks/Types`, `Server/BankData` | Bank scopes, tabs, costs, storage UI |
| Auction House | `rpg/economy/auctions` | `Server/AuctionData` | Listing creation, fees, rate limits, sold/expired states |
| Mail | `rpg/mail` | `Server/MailData` | Delayed delivery, attachments, COD, inbox queries, system mail |
| Expansions | `rpg/expansion` | `Server/ExpansionData` | Expansion entitlement checks used by other systems |
| Inventory | `rpg/inventory` | UI + config | Alternate inventory handling layer, still marked unstable by requirements |
| Items | `rpg/item` | `Server/Item/...` | Randomized equipment definitions and equipment restrictions |
| Lockpicking | `rpg/lockpicking` | config + page asset | Packet-driven lockpicking minigame with session component |
| NPC RPG hooks | `rpg/npc` | NPC core component builders | Adds RPG setup/debug/cast support to NPC pipeline |
| Camera / movement | `rpg/camera`, `rpg/movement` | config sections | Early camera-style targeting support and MOBA-style helpers |
| HUD | `rpg/hud` | custom HUD asset | XP bar overlay kept in sync with player progression |

## Root command surface

The plugin currently exposes these top-level command families when their modules are enabled:

- `race`
- `class`
- `party`
- `guild`
- `character`
- `bank`
- `currency`
- `mail`
- `chat`
- ability binding/sync/unlock commands
- leveling test/reset commands
- NPC RPG debug command

A later wiki pass should document each subcommand and permissions/usage expectations.

## Shared cross-cutting systems

### `RpgModConfig`

This is the global feature toggle and tuning asset. Almost every module depends on it directly or indirectly.

### `StatSystem`

This is the glue layer between progression sources and final player stats.

It is recalculated from multiple domains:

- class bonuses
- race bonuses
- talent bonuses
- level rewards/growth

### `CharacterManager`

This is the gateway from account identity into the rest of the RPG stack.

It decides which active character profile is attached to a player and therefore influences:

- selected class state
- race state
- progression state
- bank/currency/mail owner resolution
- login/logout/select flows

### `ExpansionManager`

Used as an entitlement gate by systems such as classes.

## Implemented vs emerging systems

### Mature modules

These already look like real gameplay systems rather than placeholders:

- characters
- leveling
- classes
- abilities
- parties
- guilds
- currencies
- banks
- mail
- custom progression interactions

### Functional but still evolving

- talents
- auction house
- lockpicking
- NPC RPG hooks
- inventory overrides
- randomized equipment

### Planned / partially scaffolded

- professions/gathering loop
- instances and encounter framework
- advanced loot framework
- broader camera manager tooling
- polished character creation/select presentation

## Practical contributor advice

If you want to extend the mod safely:

1. Find the manager/system package for the feature.
2. Check whether the feature is entity-state-driven or manager-driven.
3. Check `RpgModConfig` for enablement/tuning switches.
4. Check `ExamplePlugin` for registration order and live reload behavior.
5. Check `Server/...` JSON for how the feature expects its content to be authored.
