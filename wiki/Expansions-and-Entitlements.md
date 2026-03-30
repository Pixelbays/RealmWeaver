# Expansions and Entitlements

The expansion system is currently small but strategically important.

It is the beginning of the mod's paid/unlockable entitlement framework.

## Current role in the codebase

`ExpansionManager` handles:

- loading saved unlock data per account
- reading expansion definitions from the general RPG config
- checking whether a player has access to a feature gated by an expansion id
- checking release windows
- checking explicit permission-based bypass/access
- processing purchases through the currency system
- resolving purchase owner scope across account, character, guild, or global
- persisting successful unlocks

## Where expansion definitions live

There is not currently a separate `Server/Expansions/...` authored content tree.

Instead, expansion definitions are stored in the `GeneralSettings.Expansions` list inside the main RPG config asset.

That means expansion metadata is treated more like a global service catalog than a standalone gameplay asset family.

## Access model

A player can gain access to an expansion in several ways.

### 1. Free access

If the expansion unlock price is free, access can be granted automatically once the release rules allow it.

### 2. Purchased access

If the expansion has a cost, the purchase path spends currency and records the unlock in persistence.

### 3. Permission-based access

Admins, testers, or special accounts can bypass normal purchase/release rules through permission nodes.

### 4. Release bypass access

A separate permission can allow access before the configured release time.

## Release gating

Each expansion definition can carry a release timestamp.

`ExpansionManager` uses that to decide whether:

- the expansion is live for normal users
- bypass permissions are needed before release

The manager also formats release timestamps for display.

## Currency integration

Expansion purchases are tightly integrated with the currency module.

### Purchase flow

1. resolve the expansion definition
2. reject disabled or unknown entries
3. reject if the player already owns it
4. reject if the currency module is disabled for a priced expansion
5. resolve the purchase scope
6. resolve the owner id for that scope
7. build a `CurrencyAccessContext` if needed
8. spend the unlock price
9. persist the unlock

### Supported purchase scopes

Observed handling exists for:

- `Character`
- `Account`
- `Guild`
- `Global`

`Custom` is explicitly rejected by the purchase flow.

## Current command surface

The expansion feature is currently exposed through the currency command tree:

- `/currency expansion list`
- `/currency expansion unlock`

So expansions are currently treated as a purchasable currency-backed service rather than a standalone root module.

## Current gameplay integration

The most visible current integration is class gating.

`ClassManagementSystem` checks expansion access before allowing a player to learn a class that requires one or more expansion ids.

This is a strong sign of intended future direction.

Likely future uses include:

- unlocking races
- unlocking additional character slots or services
- race change/name change services
- expansion-based level caps
- dungeon/instance access
- premium or seasonal account entitlements

## Persistence model

Expansion ownership is stored as account-linked unlock data through `ExpansionUnlockData` and its persistence layer.

That is important because the feature is fundamentally account-centric even if the purchase currency can come from another scope.

## Level-cap interaction

`ExpansionManager` already exposes `getAccessibleLevelCap(...)`.

That means the design already anticipates expansions raising the effective level cap beyond the base cap, even if not every surrounding progression feature is fully wired yet.

## Why this system matters

This is one of the clearest bridges between:

- progression systems
- monetization/service-style systems
- account entitlements
- future content release scheduling

It is small today, but it is likely to become a central policy layer as the mod grows.

## Recommended future documentation

A later pass should document:

- the full shape of `GeneralSettings.ExpansionDefinition`
- permission node naming conventions
- exact purchase/unlock command behavior
- how expansion ownership interacts with characters vs account scope
- whether expansion unlock data should eventually move into a more explicit authored asset family
