# Economy Schema Reference

This page documents the main shapes of `CurrencyTypeDefinition` and `BankTypeDefinition` assets.

## Currency type schema

Currency assets live under `Server/Currencies/Types`.

### Core fields

- `Parent`
- `DisplayName`
- `Description`
- `Enabled`
- `VisibleInUi`
- `SortOrder`
- `AuctionHouseAllowed`
- `Icon`

### Behavior fields

- `StorageMode`
- `AllowedScopes`
- `StartingBalance`
- `MinBalance`
- `MaxBalance`
- `AllowNegative`

Observed `StorageMode` values:

- `NumericWallet`
- `PhysicalItem`
- `ItemWallet`
- `Hybrid`

### Conversion fields

- `Conversions`

Each conversion entry can define:

- `Enabled`
- `AutoConvert`
- `TargetCurrencyId`
- `SourceAmount`
- `TargetAmount`
- `Note`

This is how wallet normalization and tier roll-up behavior are expressed.

### Physical-item matching fields

- `PrimaryItemId`
- `AlternateItemIds`
- `AcceptedItemTags`
- `AcceptedItemCategories`

These fields matter when a currency is represented as an item or hybridized between wallet and item form.

### Runtime behavior implications

Currency definitions decide whether the runtime:

- stores balances numerically
- scans physical inventory items
- does both
- allows auto-conversion
- allows auction-house use
- supports a given ownership scope

## Bank type schema

Bank assets live under `Server/Banks/Types`.

### Core fields

- `Parent`
- `DisplayName`
- `Description`
- `Enabled`
- `VisibleInUi`
- `Icon`

### Ownership and mode fields

- `Scope`
- `StorageMode`
- `RequiredPermission`
- `AutoCreateOnFirstUse`
- `RemoteAccessAllowed`

Observed bank `StorageMode` values:

- `Items`
- `Void`
- `CurrencyOnly`
- `ReagentsOnly`
- `Mixed`

### Access behavior fields

- `DepositEnabled`
- `WithdrawEnabled`
- `SearchEnabled`
- `SortEnabled`
- `AutoStackEnabled`

### Layout fields

- `DefaultSlotCount`
- `MaxSlotCountPerTab`
- `DefaultTabCount`
- `MaxTabs`
- `AllowedItemTags`
- `BlockedItemTags`

### Cost fields

- `OpenCost`
- `AdditionalTabCosts`
- `SlotUpgradeCosts`

A `BankCostDefinition` can include:

- `Tier`
- `CurrencyScope`
- `CurrencyId`
- `Amount`
- `ItemTag`
- `ItemCount`
- `Note`

This allows banks and bank upgrades to charge either currency, tagged items, or both.

### Tab fields

- `Tabs`

Each `BankTabDefinition` can define:

- `Id`
- `DisplayName`
- `Description`
- `Icon`
- `SlotCount`
- `UnlockByDefault`
- `AllowedItemTags`
- `BlockedItemTags`
- `UnlockCost`

This makes tab behavior highly authorable.

## Runtime implications

Together, currency and bank schemas drive:

- wallet/balance persistence
- item-based currency handling
- player/account/guild/character bank ownership
- bank creation costs
- tab unlock progression
- storage filtering by tags
- UI availability and ordering

## Example files in repo

### Currency

- `Server/Currencies/Types/Base.json`
- `Server/Currencies/Types/Gold.json`
- `Server/Currencies/Types/Silver.json`
- `Server/Currencies/Types/DungeonToken.json`

### Banks

- `Server/Banks/Types/Base.json`
- `Server/Banks/Types/Personal.json`
- `Server/Banks/Types/Account.json`
- `Server/Banks/Types/Guild.json`
- `Server/Banks/Types/Void.json`
- `Server/Banks/Types/Warbound.json`
- `Server/Banks/Types/Professions.json`

## Related pages

- [Social and Economy Systems](Social-and-Economy-Systems.md)
- [Content and Asset Reference](Content-and-Asset-Reference.md)
- [Expansions and Entitlements](Expansions-and-Entitlements.md)
