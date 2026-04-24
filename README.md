# Realmweaver

Realmweaver is a data-driven MMO/RPG framework mod for Hytale. It combines character identity, progression, abilities, social tooling, economy systems, UI flows, and content authoring hooks into one cohesive server-side package.

> **⚠️ Early access warning**  
> Hytale is still evolving, and so is Realmweaver. Systems are already substantial, but some feature areas are still maturing or awaiting polish.

# Alpha Disclaimer

This mod is currently in alpha and will remain so until version 0.1.0 is released.

A large portion of the core features are already implemented or are in progress, but there are still additional systems planned and plenty of bugs to fix before reaching a full release. Progress is steady, and version 1.0 is closer than ever.

If you encounter any bugs or have feature requests, please submit them. Feedback is greatly appreciated and helps shape the direction of the project.

This has been a long-term passion project, with many hours invested even before launch. I’m excited to continue improving it and bringing it to its full potential.

## What Realmweaver is

Realmweaver is not a single mechanic. It is a modular RPG stack built around Hytale's ECS architecture, JSON assets, and custom UI flow.

Current implemented or active feature areas include:

- character roster and character select flow (disabled on public builds for now)
- race definitions and race-based stat or ability hooks (proper cosmetic changes coming soon)
- reusable leveling systems and progression tracks
- classes, class switching, class prerequisites, and talent trees
- data-driven abilities with hotbar bindings, global cooldowns, and interaction chains
- party, group finder, guild, and channel chat systems
- currencies, banks, auction house, and mail
- lockpicking UI and packet-driven gameplay input
- XP HUD updates and NPC RPG support hooks
- Here are some early clips
https://www.youtube.com/playlist?list=PLgjeVEYfxVrj5NQ-wwFIBY06_Y9bYcpOs


## Why use Realmweaver

### 1. It gives you an MMO foundation instead of a single feature
Realmweaver already covers the core loops most RPG or MMO-style Hytale servers need: identity, progression, social play, and economy.

### 2. It is data-driven first
Classes, races, abilities, currencies, banks, and much of the gameplay tuning live in assets under in your assetpack! That makes it easier to extend content without rewriting core logic.

### 3. It is modular and live-configurable
Major systems are toggled through and settings are in the RpgModConfig/default.json file. 
Plugin reconciles dynamic registrations of modules when config is updated, allowing for you to turn on and off features you don't need.

### 4. It supports real multiplayer server design
Realmweaver is built around parties, guilds, shared progression flows, account-vs-character ownership, and service systems like mail and banking.

### 5. It is built for expansion
The project already contains entitlement hooks, class and race gating, reusable level systems, interaction-driven unlocks, and early scaffolding for future encounter or profession systems.

## What's possible with Realmweaver

Realmweaver can support servers and experiences such as:

- class-based adventure realms with race identity and talent builds
- multiplayer co-op progression servers with parties, raids, and guild play
- social sandbox realms with mail, banking, and player economy tools
- progression-heavy survival servers with layered class, race, and stat systems
- live-service content models with unlockable expansions, new classes, and new ability packs
- NPC-driven service hubs for training, class unlocks, progression rewards, or lockpicking gameplay

Because the systems are asset-driven, Realmweaver is suited to both handcrafted content and long-term live content pipelines.

## Asset pack ecosystem

Realmweaver Core is an framework, itself adds nothing without a assetpack, which is all built by you! (or the example packs, or others!)

Recommended naming format:

**Realmweaver Asset Pack Name**

This makes it clear that Realmweaver is the systems backbone, while each asset pack defines the world theme, presentation, and content flavor.

Example starter packs:

- **Realmweaver MMAnywhere** — a basic framework example for a traditional MMO-style realm
- **Realmweaver Space Forged** — a space-themed variant showing how the same framework can support a very different world fantasy

This gives Realmweaver room to grow as both a single mod and a broader family of assetpacks for the core framework.

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
Much of the gameplay surface is built around the hytale assets editor and reusable interaction chains, so designers can author new content with less Java.


## Current maturity snapshot

Broadly mature systems:

- characters
- leveling
- classes
- talent trees
- abilities
- lockpicking


Functional but still evolving:
- Races (Cosmetics WIP)
- parties
- guilds
- currencies
- inventory overrides for class restricted items
- randomized equipment
- character creation and selection (heavy WIP)

Planned or earlier-stage areas:
- NPC using abilities
- NPC RPG hooks
- banks
- mail
- auction house
- professions and gathering loops
- instance and encounter framework
- broader camera or movement tooling
- instanced content, worlds, zones

A better road map comming soon

# Helping out!
Feel free to build, help, and submit issues!

## Build and run

## Project architecture

The composition root is
[src/main/java/org/pixelbays/plugin/ExamplePlugin.java](src/main/java/org/pixelbays/plugin/ExamplePlugin.java). 

It registers:

- asset stores
- ECS components
- gameplay systems
- managers and commands
- packet filters and custom interactions
- HUD and UI wiring

### Requirements

1. Hytale installed through the official launcher
2. Java 25
3. Windows environment

### Build the plugin

Use the workspace task `build`.
or in cmd `./gradlew build`

### Run the Dev server

Update your `asset_dir_pack_name` path in `gradle.properties` (default is currently star forged)
Use the workspace task `runserver`.
or in cmd `./gradlew runserver`.

### Dev server notes

The Gradle setup resolves `HytaleServer.jar` from the local Hytale installation and supports running the server with the included asset pack. `processResources` updates the plugin manifest version and `IncludesAssetPack` values during builds.

## Learn more

Start with the wiki: 
proper guides coming soon

- [wiki/Home.md](https://github.com/Pixelbays/RealmWeaver/wiki)

## Positioning summary

Realmweaver is best described as a Hytale MMO/RPG framework that already ships with meaningful progression, social, and economy foundations. It is suited to creators who want to build a realm with identity, persistence, and room to grow.

It can also be presented as the base framework for a family of themed asset packs, such as **Realmweaver MMAnywhere** and **Realmweaver Space Forged**.




## AI Usage

There is usage of AI in this project as a tool to enhance creativity and productivity, always with human review and testing, and limited to:

    Code snippets.
    Text content, descriptions, and documentation.
