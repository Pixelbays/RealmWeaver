# Commands Reference

This page documents the currently visible command surface from the codebase.

## Important note

Many commands are **conditionally registered**.

The plugin only exposes command groups when their module is enabled in `Server/RpgModConfig/Default.json`.

That means a command existing in source does not always mean it will be available in a running server.

## Always-registered roots

### `/race`

Purpose: browse and choose a race.

Observed subcommands:

- `/race list`
- `/race select <raceId>`

### `/npcrpgdebug`

Purpose: NPC RPG debugging.

This is registered directly by the plugin. It is mainly a development/admin tool.

## Leveling commands

Registered when the leveling module is enabled.

Observed roots:

- `/testlevel`
- `/leveltest`
- `/resetlevel`

These look like development/testing utilities for the leveling stack rather than polished player-facing MMO commands.

## Class commands

Registered when the class module is enabled.

### `/class`

Observed subcommands:

- `/class list`
- `/class info <className>`
- `/class learn <className>`
- `/class unlearn <className>`
- `/class switch <className>`
- `/class debug <className>`

When the ability module is also enabled:

- `/class abilities [className]`
- `/class useability <abilityId>`

When the leveling module is also enabled:

- `/class progress [className]`
- `/class setlevel <className> <level>`
- `/class levelup <className>`

When the talent module is also enabled:

- `/class talent info [className]`
- `/class talent allocate <className> <treeId> <nodeId>`
- `/class talent refund <className> <treeId> <nodeId>`
- `/class talent reset <className>`
- plus additional talent helper/debug commands present under the talent command package

## Party commands

Registered when the party module is enabled.

### `/party`

Observed subcommands:

- `/party create [party|raid]`
- `/party decline`
- `/party invite <player>`
- `/party join <player>`
- `/party leave`
- `/party kick <player>`
- `/party promote <player> [assistant|leader]`
- `/party disband`
- `/party info`
- `/party ui`
- `/party finder ...`

The presence of `PartyFinderCommand` indicates the group finder is exposed through the party command tree.

## Guild commands

Registered when the guild module is enabled.

### `/guild`

Observed subcommands:

- `/guild create <name> <tag>`
- `/guild invite <player>`
- `/guild join <name|tag>`
- `/guild apply <name|tag>`
- `/guild accept <player>`
- `/guild deny <player>`
- `/guild leave`
- `/guild kick <player>`
- `/guild transfer <player>`
- `/guild joinpolicy <invite|open|application>`
- `/guild role ...`
- `/guild disband`
- `/guild info`
- `/guild ui`

### `/guild role`

Observed supporting role commands include:

- create role
- list roles
- assign role
- set permissions on a role

This is already a fairly complete admin surface for guild role management.

## Character commands

Registered when the character module is enabled.

### `/character`

Observed subcommands:

- `/character ui`
- `/character logout`

This reflects the current character system design: most character interaction is UI-driven rather than command-heavy.

## Bank commands

Registered when the bank module is enabled.

### `/bank`

Observed subcommands:

- `/bank info`
- `/bank ui`

## Currency commands

Registered when the currency module is enabled.

### `/currency`

Observed subcommands:

- `/currency info`
- `/currency ui`
- `/currency normalize`
- `/currency add`
- `/currency remove`
- `/currency set`
- `/currency expansion ...`

### `/currency expansion`

Observed subcommands:

- `/currency expansion list`
- `/currency expansion unlock`

This is the current player/admin entry point for the expansion entitlement system.

## Mail commands

Registered when the mail module is enabled.

### `/mail`

Observed subcommands:

- `/mail ui`

The code currently points heavily toward UI-first mail interaction rather than a rich chat-command workflow.

## Chat commands

Registered when either party chat or guild chat is effectively available.

### `/chat`

Observed behaviors:

- show current active channel when run without a channel argument
- set active channel by id or alias
- clear active channel with values like `off`, `clear`, `none`, or `global`

Known channel ids in the current plugin setup:

- `party`
- `guild`

The chat manager also supports one-off prefixed messages in chat such as:

- `!party hello`
- `!guild hello`

## Ability utility commands

Registered when the ability module is enabled.

### `/bindability`

Purpose: bind learned abilities to configured hotbar ability slots.

Observed behaviors:

- `/bindability list`
- `/bindability <slot> <abilityId>`
- `/bindability <slot> clear`

Notes:

- user-facing slots are 1-indexed
- binding is restricted to configured ability hotbar slots
- the ability must exist and already be unlocked for the player

### `/synchotbar`

Purpose: refresh hotbar ability icons from the player binding component.

### `/unlockability`

Purpose: developer/admin unlock helper for abilities.

This command is registered by the plugin, though this pass does not yet fully document its exact syntax.

## How command registration works

`ExamplePlugin` uses a `refreshCommand(...)` pattern when config changes.

That means:

- commands are unregistered and re-registered as module flags change
- the command surface is part of the live module system
- documentation should always mention the relevant enablement section in `RpgModConfig`

## Suggested next pass for this page

A stronger third-pass reference should add:

- permissions/admin expectations per command
- exact argument names and validation rules per subcommand
- output examples
- which commands are safe for players vs intended for debugging/admin use
