package org.pixelbays.rpg.economy.banks;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.banks.config.BankData;
import org.pixelbays.rpg.economy.banks.config.BankScope;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

@SuppressWarnings("null")
public class BankManager {

    private final Map<String, BankAccount> banksById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> ownerToBankIds = new ConcurrentHashMap<>();
    private final BankPersistence persistence = new BankPersistence();

    public void loadFromAssets() {
        clear();

        RpgModConfig config = resolveConfig();
        if (config != null && !config.isBankModuleEnabled()) {
            return;
        }

        for (BankData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }

            BankAccount bankAccount = data.toBankAccount();
            banksById.put(bankAccount.getId(), bankAccount);
            indexBank(bankAccount);
        }
    }

    public void clear() {
        banksById.clear();
        ownerToBankIds.clear();
    }

    @Nullable
    public BankAccount getBank(@Nullable String bankId) {
        return bankId == null ? null : banksById.get(bankId);
    }

    @Nonnull
    public List<BankAccount> getAllBanks() {
        return new ArrayList<>(banksById.values());
    }

    @Nonnull
    public List<BankAccount> getBanksForOwner(@Nonnull BankScope scope, @Nonnull String ownerId) {
        Set<String> bankIds = ownerToBankIds.get(createOwnerKey(scope, ownerId));
        if (bankIds == null || bankIds.isEmpty()) {
            return List.of();
        }

        List<BankAccount> banks = new ArrayList<>();
        for (String bankId : bankIds) {
            BankAccount bankAccount = banksById.get(bankId);
            if (bankAccount != null) {
                banks.add(bankAccount);
            }
        }
        return banks;
    }

    @Nullable
    public BankAccount getBankForOwnerAndType(@Nonnull BankScope scope, @Nonnull String ownerId, @Nonnull String bankTypeId) {
        for (BankAccount bankAccount : getBanksForOwner(scope, ownerId)) {
            if (bankTypeId.equalsIgnoreCase(bankAccount.getBankTypeId())) {
                return bankAccount;
            }
        }
        return null;
    }

    @Nonnull
    public BankActionResult getOrCreateBank(@Nonnull String bankTypeId, @Nonnull String ownerId) {
        BankTypeDefinition definition = BankTypeRegistry.get(bankTypeId);
        if (definition == null) {
            return BankActionResult.failure("Unknown bank type: " + bankTypeId);
        }
        return getOrCreateBank(definition, ownerId);
    }

    @Nonnull
    public BankActionResult getOrCreateBank(@Nonnull BankTypeDefinition definition, @Nonnull String ownerId) {
        if (!definition.isEnabled()) {
            return BankActionResult.failure("Bank type is disabled: " + definition.getId());
        }
        if (ownerId.isBlank()) {
            return BankActionResult.failure("Owner id cannot be empty.");
        }

        String bankId = createBankId(definition, ownerId);
        BankAccount existing = banksById.get(bankId);
        if (existing != null) {
            return BankActionResult.success("Loaded existing bank.", existing);
        }

        BankAccount bankAccount = createBankAccount(definition, ownerId);
        banksById.put(bankAccount.getId(), bankAccount);
        indexBank(bankAccount);
        saveBankIfEnabled(bankAccount);
        RpgLogging.debugDeveloper("Created bank %s for %s (%s)", bankAccount.getId(), ownerId, definition.getId());
        return BankActionResult.success("Created bank.", bankAccount);
    }

    @Nonnull
    public BankActionResult getOrCreateBank(@Nonnull BankTypeDefinition definition,
            @Nonnull String ownerId,
            @Nonnull UUID payerId,
            @Nonnull Inventory payerInventory) {
        if (!definition.isEnabled()) {
            return BankActionResult.failure("Bank type is disabled: " + definition.getId());
        }
        if (ownerId.isBlank()) {
            return BankActionResult.failure("Owner id cannot be empty.");
        }

        String bankId = createBankId(definition, ownerId);
        BankAccount existing = banksById.get(bankId);
        if (existing != null) {
            return BankActionResult.success("Loaded existing bank.", existing);
        }

        BankActionResult costResult = chargeCost(definition, definition.getOpenCost(), ownerId, payerId, payerInventory);
        if (!costResult.isSuccess()) {
            return costResult;
        }

        BankAccount bankAccount = createBankAccount(definition, ownerId);
        banksById.put(bankAccount.getId(), bankAccount);
        indexBank(bankAccount);
        saveBankIfEnabled(bankAccount);
        RpgLogging.debugDeveloper("Created bank %s for %s (%s)", bankAccount.getId(), ownerId, definition.getId());
        return BankActionResult.success("Created bank.", bankAccount);
    }

    @Nullable
    public BankTypeDefinition getDefinition(@Nonnull BankAccount bankAccount) {
        return BankTypeRegistry.get(bankAccount.getBankTypeId());
    }

    @Nonnull
    public List<BankTypeDefinition.BankTabDefinition> getAvailableTabs(@Nonnull BankAccount bankAccount) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        if (definition == null) {
            return List.of();
        }

        List<BankTypeDefinition.BankTabDefinition> available = new ArrayList<>();
        for (BankTypeDefinition.BankTabDefinition tab : definition.getResolvedTabs()) {
            if (bankAccount.isTabUnlocked(tab.getId())) {
                available.add(tab);
            }
        }

        if (available.isEmpty() && !definition.getResolvedTabs().isEmpty()) {
            available.add(definition.getResolvedTabs().get(0));
        }
        return available;
    }

    @Nullable
    public BankTypeDefinition.BankTabDefinition getTabDefinition(@Nonnull BankAccount bankAccount, @Nonnull String tabId) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        if (definition == null) {
            return null;
        }

        for (BankTypeDefinition.BankTabDefinition tab : definition.getResolvedTabs()) {
            if (tabId.equalsIgnoreCase(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    @Nullable
    public String resolveInitialTabId(@Nonnull BankAccount bankAccount) {
        List<BankTypeDefinition.BankTabDefinition> tabs = getAvailableTabs(bankAccount);
        return tabs.isEmpty() ? null : tabs.get(0).getId();
    }

    @Nonnull
    public SimpleItemContainer createTabContainer(@Nonnull BankAccount bankAccount, @Nonnull String tabId) {
        BankTypeDefinition.BankTabDefinition tabDefinition = getTabDefinition(bankAccount, tabId);
        int slotCount = tabDefinition == null
                ? bankAccount.getTabSlotCount(tabId, 1)
                : bankAccount.getTabSlotCount(tabId, tabDefinition.getSlotCount());
        short capacity = (short) Math.max(1, slotCount);
        SimpleItemContainer container = new SimpleItemContainer(capacity);

        for (BankTabItem item : bankAccount.getTabItems(tabId)) {
            if (item.getSlotIndex() < 0 || item.getSlotIndex() >= capacity || item.getItemStack().isEmpty()) {
                continue;
            }
            container.setItemStackForSlot((short) item.getSlotIndex(), item.getItemStack(), false);
        }

        return container;
    }

    public void saveTabContainer(@Nonnull BankAccount bankAccount,
            @Nonnull String tabId,
            @Nonnull ItemContainer container) {
        List<BankTabItem> items = new ArrayList<>();
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            items.add(new BankTabItem(slot, stack));
        }

        bankAccount.replaceTabItems(tabId, items);
        saveBank(bankAccount);
    }

    @Nonnull
    public BankActionResult getOrCreateConfiguredBank(@Nonnull BankScope scope, @Nonnull String ownerId) {
        String bankTypeId = resolveConfiguredBankTypeId(scope);
        if (bankTypeId == null || bankTypeId.isBlank()) {
            return BankActionResult.failure("No configured bank type for scope " + scope.name() + '.');
        }
        return getOrCreateBank(bankTypeId, ownerId);
    }

    @Nonnull
    public BankActionResult getOrCreateConfiguredBank(@Nonnull BankScope scope,
            @Nonnull String ownerId,
            @Nonnull UUID payerId,
            @Nonnull Inventory payerInventory) {
        String bankTypeId = resolveConfiguredBankTypeId(scope);
        if (bankTypeId == null || bankTypeId.isBlank()) {
            return BankActionResult.failure("No configured bank type for scope " + scope.name() + '.');
        }

        BankTypeDefinition definition = BankTypeRegistry.get(bankTypeId);
        if (definition == null) {
            return BankActionResult.failure("Unknown bank type: " + bankTypeId);
        }
        return getOrCreateBank(definition, ownerId, payerId, payerInventory);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultPersonalBank(@Nonnull UUID ownerId) {
        String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(ownerId);
        return getOrCreateConfiguredBank(BankScope.Character, characterOwnerId.isBlank() ? ownerId.toString() : characterOwnerId);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultPersonalBank(@Nonnull UUID ownerId, @Nonnull Inventory payerInventory) {
        String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(ownerId);
        return getOrCreateConfiguredBank(BankScope.Character,
                characterOwnerId.isBlank() ? ownerId.toString() : characterOwnerId,
                ownerId,
                payerInventory);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultAccountBank(@Nonnull UUID ownerId) {
        return getOrCreateConfiguredBank(BankScope.Account, ownerId.toString());
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultAccountBank(@Nonnull UUID ownerId, @Nonnull Inventory payerInventory) {
        return getOrCreateConfiguredBank(BankScope.Account, ownerId.toString(), ownerId, payerInventory);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultGuildBank(@Nonnull UUID guildId) {
        return getOrCreateConfiguredBank(BankScope.Guild, guildId.toString());
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultGuildBank(@Nonnull UUID guildId,
            @Nonnull UUID payerId,
            @Nonnull Inventory payerInventory) {
        return getOrCreateConfiguredBank(BankScope.Guild, guildId.toString(), payerId, payerInventory);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultVoidBank(@Nonnull String ownerId) {
        RpgModConfig config = resolveConfig();
        String bankTypeId = config == null ? "Void" : config.getDefaultVoidBankTypeId();
        return getOrCreateBank(bankTypeId, ownerId);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultWarboundBank(@Nonnull String warbandId) {
        RpgModConfig config = resolveConfig();
        String bankTypeId = config == null ? "Warbound" : config.getDefaultWarboundBankTypeId();
        return getOrCreateBank(bankTypeId, warbandId);
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultProfessionBank(@Nonnull UUID ownerId, @Nonnull String professionId) {
        RpgModConfig config = resolveConfig();
        String bankTypeId = config == null ? "Professions" : config.getDefaultProfessionBankTypeId();
        String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(ownerId);
        return getOrCreateBank(bankTypeId,
                createQualifiedOwnerId(characterOwnerId.isBlank() ? ownerId.toString() : characterOwnerId, professionId));
    }

    @Nonnull
    public BankActionResult getOrCreateDefaultProfessionBank(@Nonnull UUID ownerId,
            @Nonnull String professionId,
            @Nonnull Inventory payerInventory) {
        RpgModConfig config = resolveConfig();
        String bankTypeId = config == null ? "Professions" : config.getDefaultProfessionBankTypeId();
        BankTypeDefinition definition = BankTypeRegistry.get(bankTypeId);
        if (definition == null) {
            return BankActionResult.failure("Unknown bank type: " + bankTypeId);
        }
        String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(ownerId);
        return getOrCreateBank(definition,
            createQualifiedOwnerId(characterOwnerId.isBlank() ? ownerId.toString() : characterOwnerId, professionId), ownerId,
                payerInventory);
    }

    @Nullable
    public BankTypeDefinition.BankTabDefinition getNextLockedTab(@Nonnull BankAccount bankAccount) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        if (definition == null) {
            return null;
        }

        for (BankTypeDefinition.BankTabDefinition tab : definition.getResolvedTabs()) {
            if (!bankAccount.isTabUnlocked(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    @Nonnull
    public BankTypeDefinition.BankCostDefinition getNextTabUnlockCost(@Nonnull BankAccount bankAccount) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        BankTypeDefinition.BankTabDefinition nextLockedTab = getNextLockedTab(bankAccount);
        if (definition == null || nextLockedTab == null) {
            return new BankTypeDefinition.BankCostDefinition();
        }

        if (!nextLockedTab.getUnlockCost().isFree()) {
            return nextLockedTab.getUnlockCost();
        }

        int targetTier = bankAccount.getUnlockedTabIds().size() + 1;
        for (BankTypeDefinition.BankCostDefinition cost : definition.getAdditionalTabCosts()) {
            if (cost != null && cost.getTier() == targetTier) {
                return cost;
            }
        }

        List<BankTypeDefinition.BankCostDefinition> fallbackCosts = definition.getAdditionalTabCosts();
        int fallbackIndex = Math.max(0, bankAccount.getUnlockedTabIds().size() - 1);
        if (fallbackIndex < fallbackCosts.size()) {
            return fallbackCosts.get(fallbackIndex);
        }

        return new BankTypeDefinition.BankCostDefinition();
    }

    @Nonnull
    public BankActionResult unlockNextTab(@Nonnull BankAccount bankAccount,
            @Nonnull UUID payerId,
            @Nonnull Inventory payerInventory) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        if (definition == null) {
            return BankActionResult.failure("Unknown bank type: " + bankAccount.getBankTypeId());
        }

        BankTypeDefinition.BankTabDefinition nextLockedTab = getNextLockedTab(bankAccount);
        if (nextLockedTab == null) {
            return BankActionResult.failure("No locked bank tabs available.");
        }

        BankActionResult costResult = chargeCost(definition, getNextTabUnlockCost(bankAccount), bankAccount.getOwnerId(),
                payerId, payerInventory);
        if (!costResult.isSuccess()) {
            return costResult;
        }

        bankAccount.unlockTab(nextLockedTab.getId());
        bankAccount.setTabSlotCount(nextLockedTab.getId(), Math.max(1, nextLockedTab.getSlotCount()));
        saveBank(bankAccount);
        return BankActionResult.success("Unlocked bank tab.", bankAccount);
    }

    public boolean supportsCurrencyStorage(@Nonnull BankAccount bankAccount) {
        BankTypeDefinition definition = getDefinition(bankAccount);
        if (definition == null) {
            return false;
        }

        return switch (definition.getStorageMode()) {
            case CurrencyOnly, Mixed -> true;
            case Items, Void, ReagentsOnly -> false;
        };
    }

    @Nonnull
    public List<CurrencyTypeDefinition> getSupportedStoredCurrencies(@Nonnull BankAccount bankAccount) {
        if (!supportsCurrencyStorage(bankAccount)) {
            return List.of();
        }

        List<CurrencyTypeDefinition> supported = new ArrayList<>();
        for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getVisible()) {
            if (definition == null || !definition.supportsScope(CurrencyScope.Custom) || !definition.usesWalletBalances()) {
                continue;
            }
            supported.add(definition);
        }
        return supported;
    }

    public long getStoredCurrencyBalance(@Nonnull BankAccount bankAccount, @Nonnull String currencyId) {
        return getCurrencyManager().getBalance(CurrencyScope.Custom, bankAccount.getId(), currencyId,
                CurrencyAccessContext.empty());
    }

    public long getSourceCurrencyBalance(@Nonnull BankAccount bankAccount,
            @Nonnull UUID playerId,
            @Nonnull Inventory inventory,
            @Nonnull String currencyId) {
        CurrencyScope playerScope = resolveBankWalletPlayerScope(bankAccount);
        String playerOwnerId = resolveBankWalletPlayerOwnerId(bankAccount, playerId);
        if (playerOwnerId == null || playerOwnerId.isBlank()) {
            return 0L;
        }

        CurrencyAccessContext playerAccess = playerScope == CurrencyScope.Character
                ? CurrencyAccessContext.fromInventory(inventory)
                : CurrencyAccessContext.empty();
        return getCurrencyManager().getBalance(playerScope, playerOwnerId, currencyId, playerAccess);
    }

    @Nonnull
    public BankActionResult depositCurrency(@Nonnull BankAccount bankAccount,
            @Nonnull UUID playerId,
            @Nonnull Inventory inventory,
            @Nonnull String currencyId,
            long amount) {
        if (!supportsCurrencyStorage(bankAccount)) {
            return BankActionResult.failure("Bank does not support stored currency.");
        }
        if (amount <= 0L) {
            return BankActionResult.failure("Currency amount must be greater than zero.");
        }

        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(currencyId);
        if (definition == null) {
            return BankActionResult.failure("Unknown currency type: " + currencyId);
        }
        if (!definition.supportsScope(CurrencyScope.Custom) || !definition.usesWalletBalances()) {
            return BankActionResult.failure("Bank cannot store currency type: " + currencyId);
        }

        CurrencyScope playerScope = resolveBankWalletPlayerScope(bankAccount);
        String playerOwnerId = resolveBankWalletPlayerOwnerId(bankAccount, playerId);
        if (playerOwnerId == null || playerOwnerId.isBlank()) {
            return BankActionResult.failure("Currency source unavailable for bank scope.");
        }

        long storedBalance = getStoredCurrencyBalance(bankAccount, currencyId);
        if (storedBalance + amount > definition.getMaxBalance()) {
            return BankActionResult.failure("Bank currency storage is full for: " + currencyId);
        }

        CurrencyAccessContext playerAccess = playerScope == CurrencyScope.Character
                ? CurrencyAccessContext.fromInventory(inventory)
                : CurrencyAccessContext.empty();
        CurrencyActionResult spendResult = getCurrencyManager().spend(playerScope, playerOwnerId,
                new CurrencyAmountDefinition(currencyId, amount), playerAccess);
        if (!spendResult.isSuccess()) {
            return BankActionResult.failure(spendResult.getMessage());
        }

        CurrencyActionResult addResult = getCurrencyManager().addBalance(CurrencyScope.Custom, bankAccount.getId(),
                currencyId, amount);
        if (!addResult.isSuccess()) {
            getCurrencyManager().addBalance(playerScope, playerOwnerId, currencyId, amount);
            return BankActionResult.failure(addResult.getMessage());
        }

        return BankActionResult.success("Deposited bank currency.", bankAccount);
    }

    @Nonnull
    public BankActionResult withdrawCurrency(@Nonnull BankAccount bankAccount,
            @Nonnull UUID playerId,
            @Nonnull Inventory inventory,
            @Nonnull String currencyId,
            long amount) {
        if (!supportsCurrencyStorage(bankAccount)) {
            return BankActionResult.failure("Bank does not support stored currency.");
        }
        if (amount <= 0L) {
            return BankActionResult.failure("Currency amount must be greater than zero.");
        }

        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(currencyId);
        if (definition == null) {
            return BankActionResult.failure("Unknown currency type: " + currencyId);
        }
        if (!definition.supportsScope(CurrencyScope.Custom) || !definition.usesWalletBalances()) {
            return BankActionResult.failure("Bank cannot store currency type: " + currencyId);
        }

        CurrencyScope playerScope = resolveBankWalletPlayerScope(bankAccount);
        String playerOwnerId = resolveBankWalletPlayerOwnerId(bankAccount, playerId);
        if (playerOwnerId == null || playerOwnerId.isBlank()) {
            return BankActionResult.failure("Currency source unavailable for bank scope.");
        }

        long storedBalance = getStoredCurrencyBalance(bankAccount, currencyId);
        if (storedBalance < amount) {
            return BankActionResult.failure("Insufficient stored bank currency: " + currencyId);
        }

        CurrencyAccessContext playerAccess = playerScope == CurrencyScope.Character
                ? CurrencyAccessContext.fromInventory(inventory)
                : CurrencyAccessContext.empty();
        long playerBalance = getCurrencyManager().getBalance(playerScope, playerOwnerId, currencyId, playerAccess);
        if (playerBalance + amount > definition.getMaxBalance()) {
            return BankActionResult.failure("Currency destination is full for: " + currencyId);
        }

        CurrencyActionResult addResult = getCurrencyManager().addBalance(playerScope, playerOwnerId, currencyId, amount);
        if (!addResult.isSuccess()) {
            return BankActionResult.failure(addResult.getMessage());
        }

        CurrencyActionResult removeResult = getCurrencyManager().removeBalance(CurrencyScope.Custom, bankAccount.getId(),
                currencyId, amount);
        if (!removeResult.isSuccess()) {
            getCurrencyManager().removeBalance(playerScope, playerOwnerId, currencyId, amount);
            return BankActionResult.failure(removeResult.getMessage());
        }

        return BankActionResult.success("Withdrew bank currency.", bankAccount);
    }

    public void saveBank(@Nonnull BankAccount bankAccount) {
        banksById.put(bankAccount.getId(), bankAccount);
        indexBank(bankAccount);
        saveBankIfEnabled(bankAccount);
    }

    public void deleteBank(@Nonnull String bankId) {
        BankAccount removed = banksById.remove(bankId);
        if (removed == null) {
            return;
        }

        Set<String> indexedBankIds = ownerToBankIds.get(createOwnerKey(removed.getOwnerScope(), removed.getOwnerId()));
        if (indexedBankIds != null) {
            indexedBankIds.remove(bankId);
            if (indexedBankIds.isEmpty()) {
                ownerToBankIds.remove(createOwnerKey(removed.getOwnerScope(), removed.getOwnerId()));
            }
        }

        if (isPersistenceEnabled()) {
            persistence.deleteBank(bankId);
        }
    }

    @Nonnull
    public static String createQualifiedOwnerId(@Nonnull String ownerId, @Nonnull String qualifier) {
        return ownerId + "::" + qualifier;
    }

    @Nonnull
    public static String createOwnerKey(@Nonnull BankScope scope, @Nonnull String ownerId) {
        return scope.name() + ':' + ownerId;
    }

    @Nonnull
    private BankAccount createBankAccount(@Nonnull BankTypeDefinition definition, @Nonnull String ownerId) {
        long now = System.currentTimeMillis();
        List<String> unlockedTabs = new ArrayList<>();
        Map<String, Integer> slotCounts = new ConcurrentHashMap<>();

        List<BankTypeDefinition.BankTabDefinition> tabs = definition.getResolvedTabs();
        for (BankTypeDefinition.BankTabDefinition tab : tabs) {
            if (tab.isUnlockByDefault()) {
                unlockedTabs.add(tab.getId());
            }
            slotCounts.put(tab.getId(), tab.getSlotCount());
        }

        while (unlockedTabs.size() < definition.getDefaultTabCount() && unlockedTabs.size() < tabs.size()) {
            String tabId = tabs.get(unlockedTabs.size()).getId();
            if (!unlockedTabs.contains(tabId)) {
                unlockedTabs.add(tabId);
            }
        }

        return new BankAccount(
                createBankId(definition, ownerId),
                definition.getId(),
                definition.getScope(),
                ownerId,
                definition.getDisplayName(),
                unlockedTabs,
                slotCounts,
            Map.of(),
                now,
                now);
    }

    @Nonnull
    private BankActionResult chargeCost(@Nonnull BankTypeDefinition definition,
            @Nonnull BankTypeDefinition.BankCostDefinition cost,
            @Nonnull String bankOwnerId,
            @Nonnull UUID payerId,
            @Nonnull Inventory payerInventory) {
        if (cost.isFree()) {
            return BankActionResult.success("No cost.");
        }

        CurrencyAccessContext accessContext = CurrencyAccessContext.fromInventory(payerInventory);
        if (!canAffordCost(definition, cost, bankOwnerId, payerId, accessContext, payerInventory)) {
            return BankActionResult.failure("Insufficient bank cost: " + describeCost(cost));
        }

        if (cost.hasCurrencyCost()) {
            CurrencyScope costScope = resolveCurrencyScope(definition.getScope(), cost);
            String currencyOwnerId = resolveCurrencyOwnerId(costScope, bankOwnerId, payerId);
            CurrencyActionResult result = getCurrencyManager().spend(
                    costScope,
                    currencyOwnerId,
                    new CurrencyAmountDefinition(cost.getCurrencyId(), cost.getAmount()),
                    accessContext);
            if (!result.isSuccess()) {
                return BankActionResult.failure("Insufficient bank cost: " + describeCost(cost));
            }
        }

        if (cost.hasItemCost()) {
            removeTaggedItems(payerInventory, cost.getItemTag(), cost.getItemCount());
        }

        return BankActionResult.success("Paid bank cost.");
    }

    private boolean canAffordCost(@Nonnull BankTypeDefinition definition,
            @Nonnull BankTypeDefinition.BankCostDefinition cost,
            @Nonnull String bankOwnerId,
            @Nonnull UUID payerId,
            @Nonnull CurrencyAccessContext accessContext,
            @Nonnull Inventory payerInventory) {
        if (cost.hasCurrencyCost()) {
            CurrencyScope costScope = resolveCurrencyScope(definition.getScope(), cost);
            String currencyOwnerId = resolveCurrencyOwnerId(costScope, bankOwnerId, payerId);
            if (currencyOwnerId == null || currencyOwnerId.isBlank()) {
                return false;
            }

            if (!getCurrencyManager().canAfford(
                    costScope,
                    currencyOwnerId,
                    new CurrencyAmountDefinition(cost.getCurrencyId(), cost.getAmount()),
                    accessContext)) {
                return false;
            }
        }

        return !cost.hasItemCost() || countTaggedItems(payerInventory, cost.getItemTag()) >= cost.getItemCount();
    }

    @Nonnull
    private CurrencyScope resolveCurrencyScope(@Nonnull BankScope bankScope,
            @Nonnull BankTypeDefinition.BankCostDefinition cost) {
        if (cost.getCurrencyScope() != null) {
            return cost.getCurrencyScope();
        }

        return switch (bankScope) {
            case Account -> CurrencyScope.Account;
            case Guild -> CurrencyScope.Guild;
            case Global -> CurrencyScope.Global;
            case Character, Player, Warband, Profession, Custom -> CurrencyScope.Character;
        };
    }

    @Nonnull
    private CurrencyScope resolveBankWalletPlayerScope(@Nonnull BankAccount bankAccount) {
        return switch (bankAccount.getOwnerScope()) {
            case Account -> CurrencyScope.Account;
            case Guild -> CurrencyScope.Guild;
            case Global -> CurrencyScope.Global;
            case Character, Player, Warband, Profession, Custom -> CurrencyScope.Character;
        };
    }

    @Nullable
    private String resolveBankWalletPlayerOwnerId(@Nonnull BankAccount bankAccount, @Nonnull UUID playerId) {
        return switch (bankAccount.getOwnerScope()) {
            case Character -> {
                String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(playerId);
                yield characterOwnerId.isBlank() ? playerId.toString() : characterOwnerId;
            }
            case Player, Account -> playerId.toString();
            case Warband, Profession, Guild, Custom -> bankAccount.getOwnerId();
            case Global -> "global";
        };
    }

    @Nonnull
    private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope,
            @Nonnull String bankOwnerId,
            @Nonnull UUID payerId) {
        return switch (scope) {
            case Character -> {
                String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(payerId);
                yield characterOwnerId.isBlank() ? payerId.toString() : characterOwnerId;
            }
            case Account -> payerId.toString();
            case Guild, Custom -> bankOwnerId;
            case Global -> "global";
        };
    }

    @Nonnull
    public String describeCost(@Nonnull BankTypeDefinition.BankCostDefinition cost) {
        List<String> parts = new ArrayList<>();
        if (cost.hasCurrencyCost()) {
            parts.add(cost.getAmount() + " " + cost.getCurrencyId());
        }
        if (cost.hasItemCost()) {
            parts.add(cost.getItemCount() + "x #" + cost.getItemTag());
        }
        if (!cost.getNote().isBlank()) {
            parts.add(cost.getNote());
        }
        return parts.isEmpty() ? "Free" : String.join(" • ", parts);
    }

    private int countTaggedItems(@Nonnull Inventory inventory, @Nonnull String itemTag) {
        int total = 0;
        for (ItemContainer container : getInventoryContainers(inventory)) {
            if (container == null) {
                continue;
            }

            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || stack.isEmpty() || !matchesItemTag(stack, itemTag)) {
                    continue;
                }
                total += stack.getQuantity();
            }
        }
        return total;
    }

    private void removeTaggedItems(@Nonnull Inventory inventory, @Nonnull String itemTag, int amount) {
        int remaining = amount;
        for (ItemContainer container : getInventoryContainers(inventory)) {
            if (container == null) {
                continue;
            }

            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity && remaining > 0; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || stack.isEmpty() || !matchesItemTag(stack, itemTag)) {
                    continue;
                }

                int removeAmount = Math.min(stack.getQuantity(), remaining);
                int updatedQuantity = stack.getQuantity() - removeAmount;
                container.setItemStackForSlot(slot,
                        updatedQuantity <= 0 ? ItemStack.EMPTY : stack.withQuantity(updatedQuantity));
                remaining -= removeAmount;
            }
        }
    }

    private boolean matchesItemTag(@Nonnull ItemStack stack, @Nonnull String itemTag) {
        if (itemTag.isBlank()) {
            return false;
        }

        int tagIndex = AssetRegistry.getTagIndex(itemTag);
        if (tagIndex == Integer.MIN_VALUE) {
            return false;
        }

        Item item = stack.getItem();
        return item != null
                && item.getData() != null
                && item.getData().getExpandedTagIndexes() != null
                && item.getData().getExpandedTagIndexes().contains(tagIndex);
    }

    @Nonnull
    private List<ItemContainer> getInventoryContainers(@Nonnull Inventory inventory) {
        return List.of(inventory.getStorage(), inventory.getHotbar(), inventory.getBackpack());
    }

    @Nonnull
    private CurrencyManager getCurrencyManager() {
        return ExamplePlugin.get().getCurrencyManager();
    }

    @Nonnull
    private String createBankId(@Nonnull BankTypeDefinition definition, @Nonnull String ownerId) {
        return sanitizeId(definition.getId()) + "__" + sanitizeId(definition.getScope().name()) + "__" + sanitizeId(ownerId);
    }

    private void indexBank(@Nonnull BankAccount bankAccount) {
        ownerToBankIds.computeIfAbsent(createOwnerKey(bankAccount.getOwnerScope(), bankAccount.getOwnerId()),
                ignored -> new LinkedHashSet<>()).add(bankAccount.getId());
    }

    @Nonnull
    private String sanitizeId(@Nonnull String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9:_-]+", "_");
        return normalized.isBlank() ? "bank" : normalized;
    }

    @Nullable
    private String resolveConfiguredBankTypeId(@Nonnull BankScope scope) {
        RpgModConfig config = resolveConfig();
        if (config == null) {
            return switch (scope) {
                case Account -> "Account";
                case Guild -> "Guild";
                case Warband -> "Warbound";
                case Profession -> "Professions";
                case Character, Player, Custom, Global -> "Personal";
            };
        }

        return switch (scope) {
            case Account -> config.getDefaultAccountBankTypeId();
            case Guild -> config.getDefaultGuildBankTypeId();
            case Warband -> config.getDefaultWarboundBankTypeId();
            case Profession -> config.getDefaultProfessionBankTypeId();
            case Character, Player, Custom, Global -> config.getDefaultPersonalBankTypeId();
        };
    }

    private boolean isPersistenceEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isBankModuleEnabled();
    }

    private void saveBankIfEnabled(@Nonnull BankAccount bankAccount) {
        if (isPersistenceEnabled()) {
            persistence.saveBank(bankAccount);
        }
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        return RpgModConfig.getAssetMap() == null ? null : RpgModConfig.getAssetMap().getAsset("default");
    }
}
