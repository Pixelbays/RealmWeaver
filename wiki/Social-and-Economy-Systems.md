# Social and Economy Systems

## Party system

`PartyManager` is a mature in-memory social manager with optional persistence.

### Current features

- create parties and raids
- invite / join / decline flow
- leader and assistant roles
- leave / disband / kick flow
- NPC party members
- configurable max sizes
- invite expiry
- XP-sharing settings
- UI snapshot support

### Party config knobs already present

- party enabled
- raid enabled
- group finder enabled
- party/raid max size
- assistant counts
- invite expiry
- party XP enabled
- party XP granting mode
- range checks for XP sharing
- NPC allowance
- persistence enablement/interval

## Group finder

The group finder sits on top of the party system rather than replacing it.

It appears to provide:

- listing/snapshot generation
- finder UI page
- integration with the existing party manager

This is a good sign that social systems were built to compose rather than duplicate state.

## Guild system

`GuildManager` is already substantial.

### Current guild features

- create/disband guilds
- invite-based joining
- open/application/invite-only join policy
- application accept/deny flow
- leave / kick / leadership transfer
- custom roles
- role-based permissions
- join policy changes
- persistence-backed guild state

### Important guild model details

The guild module is not just a member list. It already contains:

- guild identity (name/tag)
- role definitions
- permission checks on actions
- membership-scope config
- join workflow configuration

That means it is already usable as a real gameplay/social backbone.

## Chat channels

`ChatManager` uses async chat-event routing.

### Current behavior

- supports named channels with aliases
- supports active channel selection per player
- supports one-off `!channel message` routing
- validates send permissions
- resolves explicit targets from the channel
- swaps in channel-specific formatter output

### Currently registered channels

- party
- guild

This is a light but clean abstraction that can grow later into more channel types.

## Currency system

`CurrencyManager` is broader than a simple integer wallet.

### Current capabilities

- scope-aware wallets (`Character`, `Account`, `Guild`, `Custom`, etc.)
- wallet-backed numeric currency
- physical-item currency support
- affordability checks
- spending across wallet + physical item representations
- direct balance mutation for numeric currencies
- auto-normalization / auto-conversion between currency tiers
- persistence-backed wallet storage

### Currency assets

Currency types live under `Server/Currencies/Types`.

A type can control things like:

- storage mode
- allowed scopes
- starting balances
- min/max balances
- negative allowance
- conversion rules
- auction house availability

## Bank system

The bank module is one of the more configurable economy subsystems.

### Current capabilities

- multiple bank scopes/types
- create or load bank accounts on demand
- per-owner indexing
- type-based tabs
- unlockable tabs
- storage containers for UI pages
- bank-opening costs
- default configured banks by scope
- ties into currency manager for fees and cost handling

### Configured default scopes already visible

- personal / character
- account
- guild
- void
- warbound
- professions

### Asset-driven bank types

Bank types live under `Server/Banks/Types` and can define:

- scope
- enabled state
- open costs
- tab layout
- slot counts
- unlock progression

## Auction house

`AuctionHouseManager` currently covers the core listing lifecycle.

### Current features

- create listing
- validate duration options
- charge listing fees
- enforce level requirement
- enforce listing caps and post rate limits
- allow bids, buyouts, or both
- validate supported sale currencies
- cancel listing
- mark listing expired
- mark listing sold
- persist listing state

### Auction config already supports

- bidding on/off
- buyouts on/off
- listing duration presets
- listing fees
- seller success fee percent
- delivery delay minutes for item/currency
- max listings per owner
- rate limiting
- minimum level to post
- seller cancel rules

This is already a strong service layer even if the full player-facing workflow still needs more UI polish and gameplay integration.

## Mail system

`MailManager` is surprisingly complete for a first pass economy/social subsystem.

### Current features

- normal player mail
- system mail
- broadcast mail to all known players
- item attachments
- currency attachments
- cash on delivery
- rate limiting
- delivery delay calculation
- expiry dates
- mailbox ownership modes
- inbox querying
- persistence-backed mail storage

### Design strength

Mail already understands the account/character ownership split, which matters a lot in an MMO-style multi-character game.

Mail is also now reachable through authored interaction chains via the `SendMail` interaction.

That makes it practical to build:

- NPC reward mail
- onboarding mail
- class/race service follow-up mail
- challenge-complete mail

using the same interaction-chain system already used by the rest of the mod.

## Expansion entitlements

The expansion module is small but strategically important.

It is already used to gate class access, and likely becomes the place to gate:

- new classes
- race changes
- paid services
- account unlocks
- future premium/seasonal systems

## Overall state of the social/economy stack

This side of the repo is further along than the requirements doc might suggest at first glance.

### Already solid

- parties
- guilds
- chat routing
- currencies
- banks
- mail

### Functional core but still expanding

- auction house
- expansion entitlements

### Likely next documentation steps

- document ownership-resolution rules across account vs character scope
- document UI pages for guild, bank, mail, and currency
- document command reference for party/guild/bank/mail/currency
- document persistence asset schemas in detail

See [Interaction Authoring Reference](Interaction-Authoring-Reference.md) for the new mail-triggering and prerequisite-driven interaction content layer.
