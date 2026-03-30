# Ability Definition Schema

This page documents the main shape of `ClassAbilityDefinition` assets under `Server/Abilities/Abilities`.

## Purpose

Ability assets define the authored side of the combat/utility ability system.

They are not just metadata. They connect UI, input, interaction chains, VFX, SFX, animation, and unlock requirements into one asset.

## Top-level fields

### Identity and presentation

- `Icon`
- `Categories`
- `TranslationProperties`
- `Tooltip`
- `Enabled`

These fields control how the ability appears in UI/editor contexts and whether it can be used at runtime.

### Leveling and rank metadata

- `AbilityLevel`
- `HasRanks`
- `MaxRank`
- `AbilityType`

These fields define the intended progression tier and rank behavior of the ability.

### Audio and animation

- `SoundEventId`
- `AbilitySoundSetId`
- `UsePlayerAnimations`
- `PlayerAnimationsId`

These fields let the runtime play a sound event, item-sound set, and player animation set when the ability is executed.

### Particle and visual data

- `Particles`
- `FirstPersonParticles`
- `PullbackConfig`

These fields define the model-particle payloads and first-person handling around the ability.

### Interaction-chain wiring

- `Interactions`
- `InteractionConfig`
- `InteractionVars`

This is one of the most important parts of the schema.

`Interactions` is a map from `InteractionType` to root interaction asset id.
`InteractionConfig` controls the interaction system behavior.
`InteractionVars` provides named inline interaction override payloads.

This is the bridge between authored RPG ability data and Hytale's native interaction system.

### Input and hotbar behavior

- `InputBinding`
- `HotbarKeyOverrides`

These fields influence how the ability is meant to be triggered from player controls.

### Unlock and requirement fields

- `PrerequisiteAbilities`
- `StatRequirements`

These fields are used to express progression gating.

### Cooldown fields

- `GlobalCooldown`
- `GlobalCooldownCategories`

These fields let an ability participate in the mod's GCD/category cooldown model.

## Important enum areas

### `AbilityInputBinding`

The asset uses an input-binding enum to determine which interaction slot or control lane the ability belongs to.

The exact enum values should be documented in a later field-by-field pass, but current runtime usage clearly supports multiple routed ability slots.

### `AbilityType`

The asset also stores an ability-type enum, used to distinguish behavior categories such as passive/toggle/global-style abilities.

## Runtime implications

An ability asset is consumed by `ClassAbilitySystem`, which uses it to:

1. validate whether the ability exists and is enabled
2. apply GCD logic
3. resolve the main root interaction chain
4. construct the interaction context
5. consume empowerment metadata if present
6. queue the chain through the interaction manager
7. trigger animation, sound, and particle side effects

## Packet/UI implications

The asset is also packetized through `toPacket()` and exposed into client-facing ability data.

That means authored fields affect not just server-side execution but also:

- icon display
- translated names/descriptions
- category grouping
- client-visible interaction mappings
- effect previews

## Authoring patterns

### Simple instant ability

Use:

- one clear root interaction
- one input binding
- sound/particle payloads as needed
- no ranking if the ability is binary

### Passive or toggle ability

Use:

- appropriate `AbilityType`
- authored metadata that matches passive/toggle handling
- no misleading active-cast presentation fields unless needed for UI

### Multi-stage combat ability

Use:

- clear root interaction mapping
- any needed `InteractionVars`
- GCD category participation
- animation/sound/particle payloads tuned together

## Known cautions

- `Interactions` and `InteractionVars` can become hard to reason about without naming discipline.
- input binding should match the actual player-control strategy in config and packet handlers.
- cooldown category usage should be coordinated across the whole class kit.
- translation and tooltip fields should be kept in sync with the actual gameplay effect.

## Example content in repo

- `Server/Abilities/Abilities/Fireball.json`
- `Server/Abilities/Abilities/ClassAbilities/Warrior/Charge.json`
- `Server/Abilities/Abilities/ClassAbilities/Paladin/LayOnHands.json`
- `Server/Abilities/Abilities/RaceAbilities/Human/Adaptability.json`

## Related pages

- [Commands Reference](Commands-Reference.md)
- [Content and Asset Reference](Content-and-Asset-Reference.md)
- [Class Definition Schema](Class-Definition-Schema.md)
- [Ability Trigger Flow](Ability-Trigger-Flow.md)
