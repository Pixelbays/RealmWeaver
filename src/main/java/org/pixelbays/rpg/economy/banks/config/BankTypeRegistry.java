package org.pixelbays.rpg.economy.banks.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;

public final class BankTypeRegistry {

    private BankTypeRegistry() {
    }

    @Nullable
    public static BankTypeDefinition get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        DefaultAssetMap<String, BankTypeDefinition> assetMap = BankTypeDefinition.getAssetMap();
        return assetMap == null ? null : assetMap.getAsset(id);
    }

    @Nonnull
    public static List<BankTypeDefinition> getAll() {
        DefaultAssetMap<String, BankTypeDefinition> assetMap = BankTypeDefinition.getAssetMap();
        if (assetMap == null) {
            return List.of();
        }

        List<BankTypeDefinition> definitions = new ArrayList<>(assetMap.getAssetMap().values());
        definitions.sort(Comparator.comparing(BankTypeDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    @Nonnull
    public static List<BankTypeDefinition> getEnabled() {
        List<BankTypeDefinition> enabled = new ArrayList<>();
        for (BankTypeDefinition definition : getAll()) {
            if (definition != null && definition.isEnabled()) {
                enabled.add(definition);
            }
        }
        return enabled;
    }

    @Nullable
    public static BankTypeDefinition getFirstByScope(@Nonnull BankScope scope) {
        for (BankTypeDefinition definition : getEnabled()) {
            if (definition.getScope() == scope) {
                return definition;
            }
        }
        return null;
    }

    @Nonnull
    public static List<BankTypeDefinition> getByScope(@Nonnull BankScope scope) {
        List<BankTypeDefinition> matching = new ArrayList<>();
        for (BankTypeDefinition definition : getEnabled()) {
            if (definition.getScope() == scope) {
                matching.add(definition);
            }
        }
        return matching;
    }
}
