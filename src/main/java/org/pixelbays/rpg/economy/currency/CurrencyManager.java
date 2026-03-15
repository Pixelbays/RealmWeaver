package org.pixelbays.rpg.economy.currency;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;
import org.pixelbays.rpg.global.config.RpgModConfig;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

@SuppressWarnings("null")
public class CurrencyManager {

    private final Map<String, CurrencyWallet> walletsById = new ConcurrentHashMap<>();
    private final CurrencyPersistence persistence = new CurrencyPersistence();

    public void loadFromAssets() {
        clear();

        RpgModConfig config = resolveConfig();
        if (config != null && !config.isCurrencyModuleEnabled()) {
            return;
        }

        for (CurrencyWalletData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }
            CurrencyWallet wallet = data.toWallet();
            walletsById.put(wallet.getId(), wallet);
        }
    }

    public void clear() {
        walletsById.clear();
    }

    @Nullable
    public CurrencyWallet getWallet(@Nonnull CurrencyScope scope, @Nonnull String ownerId) {
        return walletsById.get(createWalletId(scope, ownerId));
    }

    @Nonnull
    public CurrencyWallet getOrCreateWallet(@Nonnull CurrencyScope scope, @Nonnull String ownerId) {
        String walletId = createWalletId(scope, ownerId);
        CurrencyWallet existing = walletsById.get(walletId);
        if (existing != null) {
            return existing;
        }

        long now = System.currentTimeMillis();
        CurrencyWallet created = new CurrencyWallet(walletId, scope, ownerId, new LinkedHashMap<>(), now, now);
        for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getByScope(scope)) {
            if (!definition.usesWalletBalances()) {
                continue;
            }
            long startingBalance = clampToDefinition(definition, definition.getStartingBalance());
            if (startingBalance != 0L) {
                created.setBalance(definition.getId(), startingBalance);
            }
        }
        walletsById.put(walletId, created);
        saveWalletIfEnabled(created);
        return created;
    }

    public long getBalance(@Nonnull CurrencyScope scope, @Nonnull String ownerId, @Nonnull String currencyId) {
        return getBalance(scope, ownerId, currencyId, CurrencyAccessContext.empty());
    }

    public long getBalance(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull String currencyId,
            @Nonnull CurrencyAccessContext accessContext) {
        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(currencyId);
        if (definition == null || !definition.isEnabled() || !definition.supportsScope(scope)) {
            return 0L;
        }

        long balance = 0L;
        if (definition.usesWalletBalances()) {
            CurrencyWallet wallet = getOrCreateWallet(scope, ownerId);
            normalizeWallet(scope, wallet);
            balance += wallet.getBalance(definition.getId());
        }
        if (definition.usesPhysicalItems()) {
            balance += countPhysicalCurrency(definition, accessContext);
        }
        return balance;
    }

    public boolean canAfford(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull CurrencyAmountDefinition cost,
            @Nonnull CurrencyAccessContext accessContext) {
        if (cost.isFree()) {
            return true;
        }
        return getBalance(scope, ownerId, cost.getCurrencyId(), accessContext) >= cost.getAmount();
    }

    @Nonnull
    public CurrencyActionResult spend(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull CurrencyAmountDefinition cost,
            @Nonnull CurrencyAccessContext accessContext) {
        if (cost.isFree()) {
            return CurrencyActionResult.success("Spent currency.", cost.getCurrencyId(),
                    getBalance(scope, ownerId, cost.getCurrencyId(), accessContext));
        }

        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(cost.getCurrencyId());
        if (definition == null) {
            return CurrencyActionResult.failure("Unknown currency type: " + cost.getCurrencyId());
        }
        if (!definition.isEnabled()) {
            return CurrencyActionResult.failure("Currency type is disabled: " + cost.getCurrencyId());
        }
        if (!definition.supportsScope(scope)) {
            return CurrencyActionResult.failure("Currency type does not support scope: " + scope.name());
        }
        if (!canAfford(scope, ownerId, cost, accessContext)) {
            return CurrencyActionResult.failure("Insufficient currency: " + cost.getCurrencyId());
        }

        long remaining = cost.getAmount();
        CurrencyWallet wallet = null;
        boolean walletChanged = false;

        if (definition.usesWalletBalances()) {
            wallet = getOrCreateWallet(scope, ownerId);
            normalizeWallet(scope, wallet);
            long current = wallet.getBalance(definition.getId());
            long fromWallet = Math.min(current, remaining);
            if (fromWallet > 0L) {
                wallet.setBalance(definition.getId(), current - fromWallet);
                remaining -= fromWallet;
                walletChanged = true;
            }
        }

        if (remaining > 0L && definition.usesPhysicalItems()) {
            remaining = removePhysicalCurrency(definition, accessContext, remaining);
        }

        if (walletChanged && wallet != null) {
            normalizeWallet(scope, wallet);
            saveWalletIfEnabled(wallet);
        }

        if (remaining > 0L) {
            return CurrencyActionResult.failure("Insufficient currency: " + cost.getCurrencyId());
        }

        return CurrencyActionResult.success("Spent currency.", definition.getId(),
                getBalance(scope, ownerId, definition.getId(), accessContext));
    }

    @Nonnull
    public CurrencyActionResult addBalance(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull String currencyId,
            long amount) {
        if (amount < 0L) {
            return CurrencyActionResult.failure("Amount must be zero or greater.");
        }
        return mutateWalletBalance(scope, ownerId, currencyId, amount, MutationType.ADD);
    }

    @Nonnull
    public CurrencyActionResult removeBalance(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull String currencyId,
            long amount) {
        if (amount < 0L) {
            return CurrencyActionResult.failure("Amount must be zero or greater.");
        }
        return mutateWalletBalance(scope, ownerId, currencyId, amount, MutationType.REMOVE);
    }

    @Nonnull
    public CurrencyActionResult setBalance(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull String currencyId,
            long amount) {
        return mutateWalletBalance(scope, ownerId, currencyId, amount, MutationType.SET);
    }

    @Nonnull
    private CurrencyActionResult mutateWalletBalance(@Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull String currencyId,
            long amount,
            @Nonnull MutationType mutationType) {
        if (ownerId.isBlank()) {
            return CurrencyActionResult.failure("Owner id cannot be empty.");
        }

        CurrencyTypeDefinition definition = CurrencyTypeRegistry.get(currencyId);
        if (definition == null) {
            return CurrencyActionResult.failure("Unknown currency type: " + currencyId);
        }
        if (!definition.isEnabled()) {
            return CurrencyActionResult.failure("Currency type is disabled: " + currencyId);
        }
        if (!definition.supportsScope(scope)) {
            return CurrencyActionResult.failure("Currency type does not support scope: " + scope.name());
        }
        if (!definition.usesWalletBalances() || definition.getStorageMode() == CurrencyTypeDefinition.CurrencyStorageMode.PhysicalItem) {
            return CurrencyActionResult.failure("Currency type cannot be modified directly: " + currencyId);
        }

        CurrencyWallet wallet = getOrCreateWallet(scope, ownerId);
        normalizeWallet(scope, wallet);
        long current = wallet.getBalance(definition.getId());
        long updated = switch (mutationType) {
            case ADD -> current + amount;
            case REMOVE -> current - amount;
            case SET -> amount;
        };

        long clamped = clampToDefinition(definition, updated);
        if (mutationType == MutationType.REMOVE && clamped > updated) {
            return CurrencyActionResult.failure("Insufficient currency: " + currencyId);
        }
        if (!definition.isAllowNegative() && clamped < 0L) {
            return CurrencyActionResult.failure("Insufficient currency: " + currencyId);
        }

        wallet.setBalance(definition.getId(), clamped);
    normalizeWallet(scope, wallet);
        saveWalletIfEnabled(wallet);

        String message = switch (mutationType) {
            case ADD -> "Added currency.";
            case REMOVE -> "Removed currency.";
            case SET -> "Set currency.";
        };
        return CurrencyActionResult.success(message, definition.getId(), wallet.getBalance(definition.getId()));
    }

    public boolean normalizeWallet(@Nonnull CurrencyScope scope, @Nonnull String ownerId) {
        CurrencyWallet wallet = getOrCreateWallet(scope, ownerId);
        return normalizeWallet(scope, wallet);
    }

    private boolean normalizeWallet(@Nonnull CurrencyScope scope, @Nonnull CurrencyWallet wallet) {
        boolean changed = false;
        int iterations = 0;

        while (iterations++ < 32) {
            boolean convertedThisPass = false;
            for (CurrencyTypeDefinition sourceDefinition : CurrencyTypeRegistry.getByScope(scope)) {
                if (!sourceDefinition.usesWalletBalances() || !sourceDefinition.hasAutoConversions()) {
                    continue;
                }

                long sourceBalance = wallet.getBalance(sourceDefinition.getId());
                if (sourceBalance <= 0L) {
                    continue;
                }

                for (CurrencyTypeDefinition.CurrencyConversionDefinition conversion : sourceDefinition.getConversions()) {
                    if (conversion == null || !conversion.isValid() || !conversion.isAutoConvert()) {
                        continue;
                    }

                    CurrencyTypeDefinition targetDefinition = CurrencyTypeRegistry.get(conversion.getTargetCurrencyId());
                    if (targetDefinition == null || !targetDefinition.isEnabled() || !targetDefinition.supportsScope(scope)) {
                        continue;
                    }
                    if (!targetDefinition.usesWalletBalances()) {
                        continue;
                    }

                    long availableConversions = sourceBalance / conversion.getSourceAmount();
                    if (availableConversions <= 0L) {
                        continue;
                    }

                    long currentTarget = wallet.getBalance(targetDefinition.getId());
                    long targetMax = targetDefinition.getMaxBalance();
                    long maxConversionsByTarget = conversion.getTargetAmount() <= 0L
                            ? 0L
                            : Math.max(0L, (targetMax - currentTarget) / conversion.getTargetAmount());
                    if (maxConversionsByTarget <= 0L) {
                        continue;
                    }

                    long conversionsToApply = Math.min(availableConversions, maxConversionsByTarget);
                    if (conversionsToApply <= 0L) {
                        continue;
                    }

                    long sourceDelta = conversionsToApply * conversion.getSourceAmount();
                    long targetDelta = conversionsToApply * conversion.getTargetAmount();
                    wallet.setBalance(sourceDefinition.getId(), sourceBalance - sourceDelta);
                    wallet.setBalance(targetDefinition.getId(), clampToDefinition(targetDefinition, currentTarget + targetDelta));
                    sourceBalance -= sourceDelta;
                    convertedThisPass = true;
                    changed = true;
                }
            }

            if (!convertedThisPass) {
                break;
            }
        }

        if (changed) {
            saveWalletIfEnabled(wallet);
        }
        return changed;
    }

    private long clampToDefinition(@Nonnull CurrencyTypeDefinition definition, long amount) {
        long min = definition.getMinBalance();
        long max = definition.getMaxBalance();
        if (!definition.isAllowNegative() && min < 0L) {
            min = 0L;
        }
        if (amount < min) {
            return min;
        }
        if (amount > max) {
            return max;
        }
        return amount;
    }

    private long countPhysicalCurrency(@Nonnull CurrencyTypeDefinition definition,
            @Nonnull CurrencyAccessContext accessContext) {
        long total = 0L;
        for (ItemContainer container : accessContext.getItemContainers()) {
            if (container == null) {
                continue;
            }
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                if (matchesCurrencyItem(definition, stack)) {
                    total += stack.getQuantity();
                }
            }
        }
        return total;
    }

    private long removePhysicalCurrency(@Nonnull CurrencyTypeDefinition definition,
            @Nonnull CurrencyAccessContext accessContext,
            long amountToRemove) {
        long remaining = amountToRemove;
        for (ItemContainer container : accessContext.getItemContainers()) {
            if (container == null) {
                continue;
            }

            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity && remaining > 0L; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || stack.isEmpty() || !matchesCurrencyItem(definition, stack)) {
                    continue;
                }

                long removeAmount = Math.min((long) stack.getQuantity(), remaining);
                int updatedQuantity = (int) (stack.getQuantity() - removeAmount);
                container.setItemStackForSlot(slot,
                        updatedQuantity <= 0 ? ItemStack.EMPTY : stack.withQuantity(updatedQuantity));
                remaining -= removeAmount;
            }
        }
        return remaining;
    }

    private boolean matchesCurrencyItem(@Nonnull CurrencyTypeDefinition definition, @Nonnull ItemStack stack) {
        for (String acceptedId : definition.getAllAcceptedItemIds()) {
            if (acceptedId.equalsIgnoreCase(stack.getItemId())) {
                return true;
            }
        }

        if (matchesAcceptedTag(definition, stack)) {
            return true;
        }

        if (definition.getAcceptedItemCategories().isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        String[] categories = item == null ? null : item.getCategories();
        if (categories == null || categories.length == 0) {
            return false;
        }

        for (String category : categories) {
            for (String acceptedCategory : definition.getAcceptedItemCategories()) {
                if (acceptedCategory != null && acceptedCategory.equalsIgnoreCase(category)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAcceptedTag(@Nonnull CurrencyTypeDefinition definition, @Nonnull ItemStack stack) {
        if (definition.getAcceptedItemTags().isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        if (item == null || item.getData() == null || item.getData().getExpandedTagIndexes() == null) {
            return false;
        }

        for (String acceptedTag : definition.getAcceptedItemTags()) {
            if (acceptedTag == null || acceptedTag.isBlank()) {
                continue;
            }

            int tagIndex = AssetRegistry.getTagIndex(acceptedTag);
            if (tagIndex == Integer.MIN_VALUE) {
                continue;
            }

            if (item.getData().getExpandedTagIndexes().contains(tagIndex)) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    private String createWalletId(@Nonnull CurrencyScope scope, @Nonnull String ownerId) {
        return sanitizeId(scope.name()) + "__" + sanitizeId(ownerId);
    }

    @Nonnull
    private String sanitizeId(@Nonnull String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9:_-]+", "_");
        return normalized.isBlank() ? "wallet" : normalized;
    }

    private void saveWalletIfEnabled(@Nonnull CurrencyWallet wallet) {
        if (!isPersistenceEnabled()) {
            return;
        }

        persistence.saveWallet(wallet);
    }

    private boolean isPersistenceEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || (config.isCurrencyModuleEnabled() && config.getCurrencySettings().isPersistenceEnabled());
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        return RpgModConfig.getAssetMap() == null ? null : RpgModConfig.getAssetMap().getAsset("default");
    }

    private enum MutationType {
        ADD,
        REMOVE,
        SET
    }

}
