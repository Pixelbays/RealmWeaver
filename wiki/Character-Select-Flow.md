# Character Select Flow

This page describes the current login, roster, and character-select flow.

## Main pieces involved

### Runtime classes

- `ExamplePlugin`
- `CharacterManager`
- `CharacterSelectPage`

### Data/config inputs

- `CharacterRosterData`
- `CharacterSettings` in `RpgModConfig`
- race and class definitions
- level-system definitions

## Entry point

On `PlayerReadyEvent`, the plugin forwards the player into `CharacterManager.handlePlayerReady(...)`.

## Current flow

### 1. Feature gate and persistence load

The character manager first checks whether the character module is enabled.

If enabled, it ensures roster persistence has been loaded.

### 2. Resolve account roster

The manager resolves the account-level roster using the player's account UUID and username.

If no roster exists, one is created in memory.

### 3. Optional legacy migration

If legacy migration is enabled and no profiles exist yet, the manager can capture the player's current live state into a newly created fallback character profile.

This is the bridge from older single-character assumptions into the newer roster model.

### 4. Clear active character tracking

The account's active-character pointer is cleared before entering the selection flow.

### 5. Enter character select

`enterCharacterSelect(...)` does three important things:

- cancels any pending logout-to-select timer
- moves the player to the lobby/select context
- opens the `CharacterSelectPage`

## UI page behavior

`CharacterSelectPage` is a custom page backed by `CharacterSelectPage.ui`.

### Main UI actions currently handled

- `Refresh`
- `Focus`
- `Select`
- `Delete`
- `Recover`
- `Create`

The page gathers raw UI event data, then bounces handling back onto the world thread before mutating gameplay state.

## Creating a character

When the user hits create:

1. name, race, and starter class are collected from UI fields
2. character-slot limits are checked
3. creation policy from `CharacterSettings` is checked
4. naming and starter requirements are validated
5. a new `CharacterProfileData` is created
6. base level progression is initialized
7. race progression and race abilities are applied
8. starter class data and starter class abilities are applied if configured
9. empty inventory/stat/binding snapshots are created
10. the roster is persisted

## Selecting a character

When the user selects a profile:

1. the target profile is looked up in the roster
2. deleted/recovery-pending profiles are rejected
3. any current active character snapshot is saved
4. the selected profile is applied back onto the live player
5. active-character tracking is updated
6. the roster's selected-character pointer is updated
7. the page is closed
8. configured login VFX are played
9. the player is moved into the gameplay world/spawn flow

## Delete and recover flow

The system supports both destructive deletion and soft-delete recovery, depending on config.

### Soft delete path

If soft-delete recovery is enabled:

- the profile is marked deleted
- deletion timestamp is recorded
- the profile remains recoverable until the configured recovery window expires

### Recovery path

Recovery enforces:

- recovery mode must be enabled
- the profile must currently be soft-deleted
- rate limiting may apply
- retention window must not have expired

## Logout back to select

The character manager also supports logging out from gameplay back to character select.

This flow can be immediate or delayed by a configured timer.

When it completes, the system:

- saves the active character snapshot
- plays logout VFX if configured
- returns the player to the select page

## Profile state captured in the roster

A profile can store at least:

- identity metadata
- race progression
- class progression
- class abilities
- ability bindings
- level progression
- stat snapshot
- inventory snapshot
- last played timestamp

This is what lets the mod support multiple characters per account cleanly.

## Presentation controls already supported by config

The character-select flow already understands:

- shared lobby world id
- default/race-specific lobby spawn ids
- default/race-specific background ids
- default/race-specific preview camera preset ids
- login/logout VFX

This is why the repo already feels architecturally ready for a polished MMO-style select screen even though the requirements notes still call for more polish.

## Why this flow matters

This is one of the core architectural seams in the whole mod.

It is where:

- account identity
- roster persistence
- race selection
- starting class setup
- level initialization
- spawn/lobby presentation
- paid service style features

all eventually meet.

## Likely next documentation pass

A stronger follow-up page should document:

- exact snapshot fields inside `CharacterProfileData`
- lobby/world movement details
- preview camera/background application path
- how name/race/class change services would plug into the current system

## Related pages

- [Progression and Character Systems](Progression-and-Character-Systems.md)
- [Config Schema Reference](Config-Schema-Reference.md)
- [Race Definition Schema](Race-Definition-Schema.md)
- [Class Definition Schema](Class-Definition-Schema.md)
