# Ability Trigger Flow

This page describes the current high-level path from player input to ability execution.

## Main pieces involved

### Runtime classes

- `AbilityInputFilter`
- control-specific input handlers
- `AbilityBindingComponent`
- `ClassAbilitySystem`
- `ClassAbilityComponent`
- `HotbarAbilityIconManager`
- Hytale interaction manager/runtime

### Data/config inputs

- `AbilitySettings` in `RpgModConfig`
- `ClassAbilityDefinition` assets
- root interaction assets
- player class/ability state

## Step 1: inbound packet interception

The plugin conditionally registers `AbilityInputFilter` when the ability module is enabled.

This filter listens for `SyncInteractionChains` packets.

It does not blindly execute abilities. It first decides which control path should handle the packet.

## Step 2: control-type routing

The filter resolves the effective control type for the player.

Current routing paths include:

- weapons-based control
- hotbar control
- explicit ability-slot control
- click-to-move pre-processing for movement input

The current implementation primarily falls back to global config while leaving room for class-specific override logic.

## Step 3: handler-specific interpretation

The selected input handler interprets the packet and decides whether:

- a hotbar binding should be used
- a weapon interaction should map to an ability
- a numbered ability slot should fire
- the packet should be ignored and allowed through

## Step 4: unlocked ability and binding state

The ability stack depends on entity components such as:

- `ClassAbilityComponent` for unlocked abilities
- `AbilityBindingComponent` for hotbar-slot mapping
- `AbilityTriggerBlockComponent` for temporary blocking
- `AbilityEmpowerComponent` for one-shot empowerment multipliers

These components are what turn raw input into RPG-aware execution.

## Step 5: trigger request enters `ClassAbilitySystem`

`ClassAbilitySystem.triggerAbility(...)` is the core execution path.

It performs, in order:

1. trigger-block check
2. ability asset lookup
3. enabled-state check
4. global cooldown check
5. root interaction id lookup
6. interaction manager lookup
7. interaction-context creation
8. empowerment consumption and metadata injection
9. root interaction queueing
10. sound/particle/animation side effects
11. success/failure event dispatch

## Step 6: root interaction execution

The actual gameplay effect is ultimately expressed as a Hytale interaction chain.

That means the authored ability asset points into root interactions rather than hard-coding effect logic directly in the ability system.

This is one of the most important architectural traits of the mod.

The same interaction runtime is now also used outside direct combat execution for authored progression/service chains.

Examples now supported by custom interaction types include:

- prerequisite validation before letting a chain continue
- direct achievement unlocks or progress grants
- class/race/expansion unlock flows
- system mail follow-up after a successful interaction branch

## Step 7: side effects

After queueing the main chain, the runtime may also trigger:

- animation chains
- 3D sound events
- model-particle packets to nearby players

These are derived from the authored ability asset.

## Step 8: UI sync and player-facing management

Outside the direct trigger path, the mod also provides player utility flows around abilities:

- `/bindability` to assign learned abilities to hotbar slots
- `/synchotbar` to rebuild hotbar icons
- unlock/debug commands for development/admin use

These systems sit beside the trigger flow and make the control model usable in practice.

## Authoring relationship

The trigger flow only works when these authored pieces line up correctly:

- the ability asset exists and is enabled
- the ability is unlocked for the player
- the input binding/control model makes sense
- the root interaction asset exists
- any sound/animation/particle references are valid

If one of those pieces is wrong, the runtime can fail before actual gameplay logic executes.

## Current strengths

- very data-driven
- easy to extend with new ability assets
- decouples input interpretation from ability execution
- uses standard Hytale interaction runtime rather than inventing a separate effect engine
- supports multiple control styles

## Current pressure points

- class-specific control-type override is still marked as future-facing in the input filter
- interaction vars and authored chain complexity can become hard to debug
- command and UI docs around bindings are still lighter than the underlying runtime sophistication

## Related pages

- [Ability Definition Schema](Ability-Definition-Schema.md)
- [Interaction Authoring Reference](Interaction-Authoring-Reference.md)
- [Commands Reference](Commands-Reference.md)
- [UI, Input, and Content Pipeline](UI-Input-and-Content-Pipeline.md)
- [Class Definition Schema](Class-Definition-Schema.md)
