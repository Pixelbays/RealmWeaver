package org.pixelbays.rpg.economy.currency.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;

public final class CurrencyTypeRegistry {

    private CurrencyTypeRegistry() {
    }

    @Nullable
    public static CurrencyTypeDefinition get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        DefaultAssetMap<String, CurrencyTypeDefinition> assetMap = CurrencyTypeDefinition.getAssetMap();
        return assetMap == null ? null : assetMap.getAsset(id);
    }

    @Nonnull
    public static List<CurrencyTypeDefinition> getAll() {
        DefaultAssetMap<String, CurrencyTypeDefinition> assetMap = CurrencyTypeDefinition.getAssetMap();
        if (assetMap == null) {
            return List.of();
        }

        List<CurrencyTypeDefinition> definitions = new ArrayList<>(assetMap.getAssetMap().values());
        definitions.sort(Comparator
                .comparingInt(CurrencyTypeDefinition::getSortOrder)
                .thenComparing(CurrencyTypeDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    @Nonnull
    public static List<CurrencyTypeDefinition> getEnabled() {
        List<CurrencyTypeDefinition> enabled = new ArrayList<>();
        for (CurrencyTypeDefinition definition : getAll()) {
            if (definition != null && definition.isEnabled()) {
                enabled.add(definition);
            }
        }
        return enabled;
    }

    @Nonnull
    public static List<CurrencyTypeDefinition> getVisible() {
        List<CurrencyTypeDefinition> visible = new ArrayList<>();
        for (CurrencyTypeDefinition definition : getEnabled()) {
            if (definition.isVisibleInUi()) {
                visible.add(definition);
            }
        }
        return visible;
    }

    @Nonnull
    public static List<CurrencyTypeDefinition> getByScope(@Nonnull CurrencyScope scope) {
        List<CurrencyTypeDefinition> matching = new ArrayList<>();
        for (CurrencyTypeDefinition definition : getEnabled()) {
            if (definition.supportsScope(scope)) {
                matching.add(definition);
            }
        }
        return matching;
    }
}
