# UI Page Reference

## Overview

The Realmweaver UI layer is built around Hytale `CustomUIPage` controllers paired with `.ui` assets under `src/main/resources/Common/UI/Custom/Pages`.

The common runtime pattern is:

1. `build(...)` appends a page asset and binds UI events.
2. `handleDataEvent(...)` extracts raw event payload values.
3. The page bounces gameplay mutations onto `World.execute(...)`.
4. A page-specific `appendView(...)` or rebuild step pushes updated labels, fields, grids, or anchors back to the client.
5. Stateful pages clean up listeners in `onDismiss(...)`.

That pattern is consistent with the repo rule that live gameplay mutations should happen on the world thread.

## Page inventory

| UI asset | Java controller | Purpose |
| --- | --- | --- |
| `Pages/CharacterSelectPage.ui` | `CharacterSelectPage` | Character roster browsing, creation, selection, deletion, recovery |
| `Pages/PartyPage.ui` | `PartyPage` | Party roster and leader actions |
| `Pages/GroupFinderPage.ui` | `GroupFinderPage` | Listing groups, applying to listings, reviewing applicants |
| `Pages/GuildPage.ui` | `GuildPage` | Guild overview, invites, applications, role creation, permission editing |
| `Pages/BankPage.ui` | `BankPage` | Entry page for personal/account/guild/profession banks |
| `Pages/BankSelectionPage.ui` | `BankSelectionPage` | Chooses among multiple bank definitions when more than one type is available |
| `Pages/BankStoragePage.ui` | `BankStoragePage` | Live item-grid storage UI with tabs and currency deposit/withdraw actions |
| `Pages/CurrencyPage.ui` | `CurrencyPage` | Shows scoped balances and conversion rules, exposes normalize actions |
| `Pages/MailPage.ui` | `MailPage` | Inbox browser, detail pane, and simple compose flow |
| `Pages/RpgInventoryPage.ui` | `RpgInventoryPage` | Custom inventory grids and manual item movement UX |
| `Pages/LockpickingPage.ui` | `LockpickingPage` | Real-time lockpicking minigame page |
| `TalentTreePage` controller expects `Pages/TalentTreePage.ui`; companion assets present are `TalentTreePage_Detail_Pane.ui` and `TalentTreePage_Tool_Tips.ui` | `TalentTreePage` | Dynamic talent-tree tabs, node graph, and detail panel |

## Common controller conventions

### Event payload extraction

Most pages manually parse string, int, or bool values from raw UI event data instead of relying on a higher-level serializer. This is why many pages define small helpers like `extractString(...)`, `extractInt(...)`, or `extractBoolean(...)`.

### World-thread handoff

Interactive pages almost always do this before touching gameplay state:

- resolve `World` from `store.getExternalData().getWorld()`
- call `world.execute(...)`
- perform manager/system mutations inside that callback

This keeps UI callbacks aligned with the rest of the ECS/runtime model.

### View refresh model

Most simple pages follow this loop:

- apply the requested action
- create a new `UICommandBuilder`
- call `appendView(...)`
- call `sendUpdate(...)`

More advanced pages like `TalentTreePage` and `BankStoragePage` do partial or full rebuilds because the visible widget tree depends on runtime state.

## Per-page notes

## Character select

`CharacterSelectPage` is the front end for the `CharacterManager` flow already documented separately in `Character-Select-Flow.md`.

Key responsibilities:

- roster browsing and focus switching
- create/delete/recover/select actions
- preview metadata rendering
- acting as the bridge between the player lobby state and `CharacterManager`

It is one of the most gameplay-critical pages because it controls the handoff into active character play.

## Party page

`PartyPage` is a thin manager-backed page.

Primary actions:

- invite by name
- kick member
- promote member to assistant
- leave current party
- open `GroupFinderPage`

Rendering model:

- shows a high-level party summary
- renders a plain-text roster list with role, NPC, and offline flags
- converts `PartyActionResult` messages into localized page status text

This page does not own party state itself; it is a direct view over `PartyManager.getPartyUiSnapshot(...)`.

## Group finder page

`GroupFinderPage` extends party UX into discovery and recruiting.

It supports:

- creating or updating the current party listing
- removing the listing
- applying to another listing
- withdrawing the current application
- inviting or rejecting applicants when the viewer owns the listing
- navigating back into `PartyPage`

The page renders four distinct text areas:

- your own listing
- visible public listings
- your current application
- applications received by your listing

This makes it a social workflow page rather than a pure data editor.

## Guild page

`GuildPage` exposes the guild-management feature set currently surfaced to players.

Primary actions:

- invite a player
- apply to a guild by name or tag
- create a guild role
- assign a role to a member
- set a permission toggle on a role

Important detail:

- permission names are entered as text and normalized into the `GuildPermission` enum at runtime
- invalid names produce an error translation instead of mutating guild state

The page is a compact administrative console over `GuildManager`, not a full tree-driven UI.

## Bank entry and selection pages

### `BankPage`

`BankPage` is the top-level selector for bank scope.

It opens:

- the default personal bank
- the default account bank
- the default guild bank for the player’s guild
- the default profession bank for a typed profession id

When a valid bank is opened, it immediately transitions into `BankStoragePage` instead of staying on the selector.

### `BankSelectionPage`

`BankSelectionPage` is a small helper page used when a caller needs the player to choose from multiple bank types.

It:

- renders up to six options
- resolves bank definitions through `BankTypeRegistry`
- calls `BankUiOpener.openBankType(...)` when an option is clicked

This page is effectively a runtime chooser over bank asset definitions.

## Bank storage page

`BankStoragePage` is the most stateful page in the repo.

It manages:

- a live bank tab container
- the player storage and hotbar grids
- tab switching
- unlock-next-tab actions
- currency deposit/withdraw actions
- persistence of the active bank tab container on change and on dismiss

Notable implementation details:

- it registers live container listeners for both player inventory sections and the active bank container
- it sends full page updates when grids change
- it translates slot clicks into item transfers between the bank and player inventory
- it persists the current tab container whenever data changes

This page is closer to a custom inventory screen than a static form page.

## Currency page

`CurrencyPage` is a dashboard page.

It displays:

- conversion rules between visible currencies
- character-scope balances
- account-scope balances
- guild-scope balances

It also exposes normalize actions per scope so players can force auto-conversion behavior through the UI.

Unlike `BankStoragePage`, this page is read-heavy and action-light.

## Mail page

`MailPage` combines inbox browsing and a basic compose form.

Primary actions:

- refresh inbox view
- select a message by visible index
- mark selected message as read
- send mail to a named recipient

View structure:

- summary panel with totals, unread count, and in-transit count
- inbox list text block
- detail header/meta/body for the selected message
- compose fields for recipient, subject, and body

Current scope:

- compose flow sends empty attachment/COD definitions from the page itself
- the page is a functional baseline UI over `MailManager`, not yet a full attachment authoring interface

## RPG inventory page

`RpgInventoryPage` is the current custom inventory experiment.

It binds slot events for:

- storage
- hotbar
- armor
- utility

Behavior highlights:

- clicking a hotbar slot sets the active hotbar slot
- right-click style interactions are used to stage a source slot and then move an item stack into a target slot
- the page registers container listeners and pushes packet updates when inventory state changes

This is one of the pages most clearly marked as mid-iteration in the overall project roadmap.

## Talent tree page

`TalentTreePage` is the most dynamic layout page.

It builds runtime UI for:

- tree tabs
- a scrollable node graph
- connector lines between prerequisite nodes
- selected-node detail text
- allocate and reset actions

Important characteristics:

- node placement is calculated from asset positions in `ClassDefinition.TalentTree`
- the page rebuilds the visible graph after allocation, reset, or tab changes
- it uses companion UI assets for detail and tooltip content

One implementation detail worth noting: the controller appends `Pages/TalentTreePage.ui`, but the workspace-visible page assets currently include the detail and tooltip fragments rather than a standalone `TalentTreePage.ui` file. That is worth double-checking if the talent UI is ever refactored or rebuilt.

This page is a strong example of data-driven UI: class JSON directly shapes the rendered graph.

## Lockpicking page

`LockpickingPage` is a real-time status page driven by `LockpickingSessionComponent`.

It supports:

- set-pin action
- cancel action
- auto-cancel on dismiss

Its static `sendUpdate(...)` helper is important:

- the page can be updated externally by the lockpicking runtime
- command updates are sent without re-registering event bindings
- the visible track, sweet spot, timer fill, and pin indicators are recalculated from session state each update

This makes it closer to a HUD/minigame overlay than to a standard form page.

## UI architecture takeaways

- The mod favors text-heavy operational pages over decorative layouts.
- Most pages are manager-backed rather than ECS-system-backed.
- Pages are intentionally thin: they collect input, validate basics, then delegate to feature managers.
- The most advanced pages are the ones that mirror inventories or graphs: `BankStoragePage`, `TalentTreePage`, and `LockpickingPage`.
- UI files are only half the implementation; most behavior lives in Java page controllers.

## Gaps and likely future improvements

Based on the current implementations, likely future work areas include:

- richer attachment editing in mail UI
- better inventory drag/drop semantics and slot restrictions in the custom inventory page
- more visual guild/party/grouper roster widgets instead of plain text blocks
- stronger bank selection and profession-bank discovery UX
- more tooltip/detail support for currencies, abilities, and talent nodes
