package org.pixelbays.rpg.character.token;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;

public final class CharacterTokenRegistry {

    private CharacterTokenRegistry() {
    }

    @Nullable
    public static CharacterTokenDefinition get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        DefaultAssetMap<String, CharacterTokenDefinition> assetMap = CharacterTokenDefinition.getAssetMap();
        return assetMap == null ? null : assetMap.getAsset(id);
    }

    @Nonnull
    public static List<CharacterTokenDefinition> getAll() {
        DefaultAssetMap<String, CharacterTokenDefinition> assetMap = CharacterTokenDefinition.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }

        List<CharacterTokenDefinition> definitions = new ArrayList<>(assetMap.getAssetMap().values());
        definitions.sort(Comparator
                .comparingInt(CharacterTokenDefinition::getSortOrder)
                .thenComparing(CharacterTokenDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    @Nonnull
    public static List<CharacterTokenDefinition> getEnabled() {
        List<CharacterTokenDefinition> enabled = new ArrayList<>();
        for (CharacterTokenDefinition definition : getAll()) {
            if (definition != null && definition.isEnabled()) {
                enabled.add(definition);
            }
        }
        return enabled;
    }

    @Nonnull
    public static List<CharacterTokenDefinition> getVisibleInCharacterSelect() {
        List<CharacterTokenDefinition> visible = new ArrayList<>();
        for (CharacterTokenDefinition definition : getEnabled()) {
            if (definition.isVisibleInCharacterSelect()) {
                visible.add(definition);
            }
        }
        return visible;
    }
}