# Realmweaver

Realmweaver is a data-driven MMO/RPG framework mod for Hytale. It combines character identity, progression, abilities, social tooling, economy systems, UI flows, and content authoring hooks into one cohesive server-side package.

> **⚠️ Early access warning**  
> Hytale is still evolving, and so is Realmweaver. Systems are already substantial, but some feature areas are still maturing or awaiting polish.

## What Realmweaver is

Realmweaver is not a single mechanic. It is a modular RPG stack built around Hytale's ECS architecture, JSON assets, and custom UI flow.

Current implemented or active feature areas include:

- character roster and character select flow
- race definitions and race-based stat or ability hooks
- reusable leveling systems and progression tracks
- classes, class switching, class prerequisites, and talent trees
- data-driven abilities with hotbar bindings, global cooldowns, and interaction chains
- party, group finder, guild, and channel chat systems
- currencies, banks, auction house, and mail
- lockpicking UI and packet-driven gameplay input
- XP HUD updates and NPC RPG support hooks

## Why use Realmweaver

### 1. It gives you an MMO foundation instead of a single feature
Realmweaver already covers the core loops most RPG or MMO-style Hytale servers need: identity, progression, social play, and economy.

### 2. It is data-driven first
Classes, races, abilities, currencies, banks, and much of the gameplay tuning live in assets under [src/main/resources/Server](src/main/resources/Server). That makes it easier to extend content without rewriting core logic.

### 3. It is modular and live-configurable
Major systems are toggled through [src/main/resources/Server/RpgModConfig/Default.json](src/main/resources/Server/RpgModConfig/Default.json), and the plugin reconciles dynamic registrations when config assets reload.

### 4. It supports real multiplayer server design
Realmweaver is built around parties, guilds, shared progression flows, account-vs-character ownership, and service systems like mail and banking.

### 5. It is built for expansion
The project already contains entitlement hooks, class and race gating, reusable level systems, interaction-driven unlocks, and early scaffolding for future encounter or profession systems.

## What's possible with Realmweaver

Realmweaver can support servers and experiences such as:

- class-based adventure realms with race identity and talent builds
- multiplayer co-op progression servers with parties, raids, and guild play
- social sandbox realms with persistent mail, banking, and player economy tools
- progression-heavy survival servers with layered class, race, and stat systems
- live-service content models with unlockable expansions, new classes, and new ability packs
- NPC-driven service hubs for training, class unlocks, progression rewards, or lockpicking gameplay

Because the systems are asset-driven, Realmweaver is suited to both handcrafted content and long-term live content pipelines.

## Asset pack ecosystem

Realmweaver can also be positioned as a core framework with themed asset packs built on top of it.

Recommended naming format:

**Realmweaver: Asset Pack Name**

This makes it clear that Realmweaver is the systems backbone, while each asset pack defines the world theme, presentation, and content flavor.

Example starter packs:

- **Realmweaver: MMAnywhere** — a basic framework example for a traditional MMO-style realm
- **Realmweaver: Space Forged** — a space-themed variant showing how the same framework can support a very different world fantasy

This gives Realmweaver room to grow as both a single mod and a broader product family.

## Core design pillars

### Character identity
Players can move through a multi-character account model with creation, selection, deletion recovery, race-specific setup, and character-centric progression.

### Progression depth
The mod supports multiple level tracks, class milestones, talent trees, race hooks, and authored ability unlocks.

### Social persistence
Parties, guilds, routed chat, and shared-service systems help turn a collection of mechanics into an actual multiplayer world framework.

### Economy infrastructure
Currencies, banks, auction house flow, and mail provide the service layer needed for a deeper server economy.

### Content authoring flexibility
Much of the gameplay surface is built around JSON assets and reusable interaction chains, so designers can author new content with less Java churn.

### Asset pack scalability
The same framework can support multiple branded content packs, making it practical to launch one foundation and then ship very different realm experiences on top of it.

## Project architecture

The composition root is [src/main/java/org/pixelbays/plugin/ExamplePlugin.java](src/main/java/org/pixelbays/plugin/ExamplePlugin.java). It registers:

- asset stores
- ECS components
- gameplay systems
- managers and commands
- packet filters and custom interactions
- HUD and UI wiring

Important paths:

- [src/main/java/org/pixelbays/plugin/ExamplePlugin.java](src/main/java/org/pixelbays/plugin/ExamplePlugin.java)
- [src/main/java/org/pixelbays/rpg](src/main/java/org/pixelbays/rpg)
- [src/main/resources/Server](src/main/resources/Server)
- [src/main/resources/Common/UI/Custom/Pages](src/main/resources/Common/UI/Custom/Pages)
- [wiki/Home.md](wiki/Home.md)
- [docs/Realmweaver-Marketing-Guide.md](docs/Realmweaver-Marketing-Guide.md)

## Current maturity snapshot

Broadly mature systems:

- characters
- leveling
- classes
- talent trees

- abilities
- parties
- guilds
- currencies
- banks
- mail

Functional but still evolving:

- auction house
- lockpicking
- NPC RPG hooks
- inventory overrides
- randomized equipment
- NPC using abilities

Planned or earlier-stage areas:

- professions and gathering loops
- instance and encounter framework
- broader camera or movement tooling
- more presentation polish for character creation and selection
- instanced content, worlds, zones

## Build and run

### Requirements

1. Hytale installed through the official launcher
2. Java 25
3. Windows environment

### Build the plugin

Use the workspace task `build plugin`.

### Build and deploy to your local Hytale Mods folder

Use the workspace task `build and deploy`.

### Dev server notes

The Gradle setup resolves `HytaleServer.jar` from the local Hytale installation and supports running the server with the included asset pack. `processResources` updates the plugin manifest version and `IncludesAssetPack` values during builds.

## Learn more

Start with these pages:

- [wiki/Home.md](wiki/Home.md)
- [wiki/Systems-Overview.md](wiki/Systems-Overview.md)
- [wiki/Progression-and-Character-Systems.md](wiki/Progression-and-Character-Systems.md)
- [wiki/Social-and-Economy-Systems.md](wiki/Social-and-Economy-Systems.md)
- [wiki/UI-Input-and-Content-Pipeline.md](wiki/UI-Input-and-Content-Pipeline.md)
- [docs/Realmweaver-Marketing-Guide.md](docs/Realmweaver-Marketing-Guide.md)

## Positioning summary

Realmweaver is best described as a Hytale MMO/RPG framework that already ships with meaningful progression, social, and economy foundations. It is suited to creators who want to build a realm with identity, persistence, and room to grow.

It can also be presented as the base framework for a family of themed asset packs, such as **Realmweaver: MMAnywhere** and **Realmweaver: Space Forged**.




## AI Usage

There is usage of AI in this project as a tool to enhance creativity and productivity, always with human review and testing, and limited to:

    Code snippets.
    Text content, descriptions, and documentation.
