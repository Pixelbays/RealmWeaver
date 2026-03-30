package org.pixelbays.rpg.character.token;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

@SuppressWarnings({ "deprecation", "null" })
public class CharacterTokenDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, CharacterTokenDefinition>> {

    public static final AssetBuilderCodec<String, CharacterTokenDefinition> CODEC = AssetBuilderCodec.builder(
            CharacterTokenDefinition.class,
            CharacterTokenDefinition::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                    (i, s) -> i.displayName = s, i -> i.displayName)
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING, false, true),
                    (i, s) -> i.description = s, i -> i.description)
            .add()
            .append(new KeyedCodec<>("SortOrder", Codec.INTEGER, false, true),
                    (i, s) -> i.sortOrder = s, i -> i.sortOrder)
            .add()
            .append(new KeyedCodec<>("VisibleInCharacterSelect", Codec.BOOLEAN, false, true),
                    (i, s) -> i.visibleInCharacterSelect = s, i -> i.visibleInCharacterSelect)
            .add()
            .append(new KeyedCodec<>("StartingBalance", Codec.LONG, false, true),
                    (i, s) -> i.startingBalance = s, i -> i.startingBalance)
            .add()
            .append(new KeyedCodec<>("MaxBalance", Codec.LONG, false, true),
                    (i, s) -> i.maxBalance = s, i -> i.maxBalance)
            .add()
            .build();

    private static DefaultAssetMap<String, CharacterTokenDefinition> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private boolean enabled;
    private String displayName;
    private String description;
    private int sortOrder;
    private boolean visibleInCharacterSelect;
    private long startingBalance;
    private long maxBalance;

    public CharacterTokenDefinition() {
        this.id = "";
        this.enabled = true;
        this.displayName = "";
        this.description = "";
        this.sortOrder = 0;
        this.visibleInCharacterSelect = true;
        this.startingBalance = 0L;
        this.maxBalance = Long.MAX_VALUE;
    }

    @Override
    public String getId() {
        return id;
    }

    public static DefaultAssetMap<String, CharacterTokenDefinition> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(CharacterTokenDefinition.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, CharacterTokenDefinition>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? getId() : displayName;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isVisibleInCharacterSelect() {
        return visibleInCharacterSelect;
    }

    public long getStartingBalance() {
        return Math.max(0L, startingBalance);
    }

    public long getMaxBalance() {
        return maxBalance <= 0L ? 0L : maxBalance;
    }
}