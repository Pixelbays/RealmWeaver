package org.pixelbays.rpg.expansion;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;
import org.pixelbays.rpg.guild.Guild;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class ExpansionManager {

    private static final DateTimeFormatter RELEASE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneOffset.UTC);

    private final Map<String, ExpansionUnlockData> unlocksByAccountId = new ConcurrentHashMap<>();
    private final ExpansionPersistence persistence = new ExpansionPersistence();

    public void loadFromAssets() {
        clear();
        for (ExpansionUnlockData data : persistence.loadAll()) {
            if (data == null || data.getAccountId().isBlank()) {
                continue;
            }
            unlocksByAccountId.put(data.getAccountId(), data);
        }
    }

    public void clear() {
        unlocksByAccountId.clear();
    }

    @Nonnull
    public List<GeneralModSettings.ExpansionDefinition> getDefinitions() {
        RpgModConfig config = resolveConfig();
        if (config == null) {
            return new ArrayList<>();
        }

        List<GeneralModSettings.ExpansionDefinition> definitions = new ArrayList<>(config.getGeneralSettings().getExpansions());
        definitions.sort(Comparator
                .comparingInt(GeneralModSettings.ExpansionDefinition::getSortOrder)
                .thenComparing(GeneralModSettings.ExpansionDefinition::getDisplayNameOrId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    @Nullable
    public GeneralModSettings.ExpansionDefinition getDefinition(@Nullable String expansionId) {
        RpgModConfig config = resolveConfig();
        if (config == null) {
            return null;
        }
        return config.getGeneralSettings().getExpansion(expansionId);
    }

    public boolean hasAccess(@Nullable PlayerRef playerRef, @Nullable String expansionId) {
        if (expansionId == null || expansionId.isBlank()) {
            return true;
        }
        if (playerRef == null) {
            return false;
        }

        GeneralModSettings.ExpansionDefinition definition = getDefinition(expansionId);
        if (definition == null || !definition.isEnabled()) {
            return false;
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return false;
        }

        if (hasAccessPermission(playerUuid, definition)) {
            return true;
        }

        if (!isReleased(definition) && !hasReleaseBypassPermission(playerUuid, definition)) {
            return false;
        }

        if (definition.getUnlockPrice().isFree()) {
            return true;
        }

        return isUnlocked(playerUuid, definition.getId());
    }

    public boolean hasAccess(@Nullable PlayerRef playerRef, @Nonnull List<String> expansionIds) {
        if (expansionIds.isEmpty()) {
            return true;
        }
        if (playerRef == null) {
            return false;
        }
        for (String expansionId : expansionIds) {
            if (!hasAccess(playerRef, expansionId)) {
                return false;
            }
        }
        return true;
    }

    public int getAccessibleLevelCap(@Nullable PlayerRef playerRef, int baseLevelCap) {
        if (baseLevelCap <= 0 || playerRef == null) {
            return baseLevelCap;
        }

        int effectiveLevelCap = baseLevelCap;
        for (GeneralModSettings.ExpansionDefinition definition : getDefinitions()) {
            if (definition == null || !definition.isEnabled()) {
                continue;
            }

            int expansionLevelCap = definition.getLevelCap();
            if (expansionLevelCap <= effectiveLevelCap) {
                continue;
            }

            if (hasAccess(playerRef, definition.getId())) {
                effectiveLevelCap = expansionLevelCap;
            }
        }

        return effectiveLevelCap;
    }

    public boolean isUnlocked(@Nullable UUID playerUuid, @Nullable String expansionId) {
        if (playerUuid == null || expansionId == null || expansionId.isBlank()) {
            return false;
        }
        ExpansionUnlockData data = unlocksByAccountId.get(playerUuid.toString());
        return data != null && data.hasUnlocked(expansionId);
    }

    public boolean isReleased(@Nullable GeneralModSettings.ExpansionDefinition definition) {
        return definition != null && definition.getReleaseTimeEpochMs() <= System.currentTimeMillis();
    }

    public boolean hasAccessPermission(@Nullable UUID playerUuid, @Nullable GeneralModSettings.ExpansionDefinition definition) {
        return hasPermission(playerUuid, definition == null ? null : definition.getAccessPermission());
    }

    public boolean hasReleaseBypassPermission(@Nullable UUID playerUuid,
            @Nullable GeneralModSettings.ExpansionDefinition definition) {
        if (hasAccessPermission(playerUuid, definition)) {
            return true;
        }
        return hasPermission(playerUuid, definition == null ? null : definition.getReleaseBypassPermission());
    }

    @Nonnull
    public String describeRequirements(@Nonnull List<String> expansionIds) {
        List<String> names = new ArrayList<>();
        for (String expansionId : expansionIds) {
            if (expansionId == null || expansionId.isBlank()) {
                continue;
            }
            GeneralModSettings.ExpansionDefinition definition = getDefinition(expansionId);
            names.add(definition == null ? expansionId : definition.getDisplayNameOrId());
        }
        return names.isEmpty() ? "Unknown Expansion" : String.join(", ", names);
    }

    @Nonnull
    public String formatReleaseTime(@Nullable GeneralModSettings.ExpansionDefinition definition) {
        if (definition == null || definition.getReleaseTimeEpochMs() <= 0L) {
            return "";
        }
        return RELEASE_TIME_FORMATTER.format(Instant.ofEpochMilli(definition.getReleaseTimeEpochMs()));
    }

    @Nonnull
    public ExpansionPurchaseResult purchase(@Nonnull com.hypixel.hytale.server.core.entity.entities.Player player,
            @Nonnull PlayerRef playerRef,
            @Nullable String expansionId) {
        GeneralModSettings.ExpansionDefinition definition = getDefinition(expansionId);
        if (definition == null) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.UnknownExpansion, null);
        }
        if (!definition.isEnabled()) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.Disabled, definition);
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null || playerRef == null) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.OwnerUnavailable, definition);
        }

        if (hasAccessPermission(playerUuid, definition)) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.AccessGrantedByPermission, definition);
        }
        if (isUnlocked(playerUuid, definition.getId())) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.AlreadyOwned, definition);
        }
        if (definition.getUnlockPrice().isFree()) {
            return ExpansionPurchaseResult.of(
                    isReleased(definition) || hasReleaseBypassPermission(playerUuid, definition)
                            ? ExpansionPurchaseResult.Status.FreeAccess
                            : ExpansionPurchaseResult.Status.FreeNotLive,
                    definition);
        }

        RpgModConfig config = resolveConfig();
        if (config != null && !config.isCurrencyModuleEnabled()) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.CurrencyModuleDisabled, definition);
        }

        CurrencyScope purchaseScope = definition.getPurchaseCurrencyScope();
        if (purchaseScope == CurrencyScope.Custom) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.InvalidPurchaseScope, definition);
        }

        String ownerId = resolveCurrencyOwnerId(purchaseScope, playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.OwnerUnavailable, definition);
        }

        CurrencyAccessContext accessContext = purchaseScope == CurrencyScope.Character
                ? CurrencyAccessContext.fromInventory(player.getInventory())
                : CurrencyAccessContext.empty();

        CurrencyManager currencyManager = ExamplePlugin.get().getCurrencyManager();
        CurrencyActionResult spendResult = currencyManager.spend(
                purchaseScope,
                ownerId,
                definition.getUnlockPrice(),
                accessContext);
        if (!spendResult.isSuccess()) {
            return ExpansionPurchaseResult.currencyFailure(definition, spendResult);
        }

        unlock(playerUuid, definition.getId());
        return ExpansionPurchaseResult.of(ExpansionPurchaseResult.Status.Success, definition);
    }

    private void unlock(@Nonnull UUID playerUuid, @Nonnull String expansionId) {
        long now = System.currentTimeMillis();
        ExpansionUnlockData data = unlocksByAccountId.computeIfAbsent(playerUuid.toString(), this::createUnlockData);
        data.unlock(expansionId, now);
        persistence.save(data);
    }

    @Nonnull
    private ExpansionUnlockData createUnlockData(@Nonnull String accountId) {
        ExpansionUnlockData data = new ExpansionUnlockData();
        data.setId(sanitizeId(accountId));
        data.setAccountId(accountId);
        long now = System.currentTimeMillis();
        data.touch(now);
        return data;
    }

    @Nullable
    private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope, @Nonnull PlayerRef playerRef) {
        return switch (scope) {
            case Character -> ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
            case Account -> String.valueOf(playerRef.getUuid());
            case Guild -> resolveGuildOwnerId(playerRef.getUuid());
            case Global -> "global";
            case Custom -> null;
        };
    }

    @Nullable
    private String resolveGuildOwnerId(@Nullable UUID memberId) {
        if (memberId == null) {
            return null;
        }
        Guild guild = ExamplePlugin.get().getGuildManager().getGuildForMember(memberId);
        return guild == null ? null : String.valueOf(guild.getId());
    }

    private boolean hasPermission(@Nullable UUID playerUuid, @Nullable String permission) {
        return playerUuid != null && permission != null && !permission.isBlank()
                && PermissionsModule.get().hasPermission(playerUuid, permission);
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        return RpgModConfig.getAssetMap() == null ? null : RpgModConfig.getAssetMap().getAsset("default");
    }

    @Nonnull
    private String sanitizeId(@Nonnull String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9:_-]+", "_");
        return normalized.isBlank() ? "expansion" : normalized;
    }
}
