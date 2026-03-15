package org.pixelbays.rpg.economy.currency.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorSectionStart;
import com.hypixel.hytale.codec.schema.metadata.ui.UIRebuildCaches;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class CurrencyTypeDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, CurrencyTypeDefinition>> {

    public enum CurrencyStorageMode {
        NumericWallet,
        PhysicalItem,
        ItemWallet,
        Hybrid
    }

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.STRING, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final FunctionCodec<CurrencyScope[], List<CurrencyScope>> SCOPE_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(new EnumCodec<>(CurrencyScope.class), CurrencyScope[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(CurrencyScope[]::new));

        private static final FunctionCodec<CurrencyConversionDefinition[], List<CurrencyConversionDefinition>> CONVERSION_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(CurrencyConversionDefinition.CODEC, CurrencyConversionDefinition[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(CurrencyConversionDefinition[]::new));

    public static final AssetBuilderCodec<String, CurrencyTypeDefinition> CODEC = AssetBuilderCodec.builder(
            CurrencyTypeDefinition.class,
            CurrencyTypeDefinition::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .appendInherited(new KeyedCodec<>("Parent", Codec.STRING, false, true),
                    (i, s) -> i.parent = s, i -> i.parent,
                    (i, inherited) -> i.parent = inherited.parent)
            .add()
            .appendInherited(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                    (i, s) -> i.displayName = s, i -> i.displayName,
                    (i, inherited) -> i.displayName = inherited.displayName)
            .add()
            .appendInherited(new KeyedCodec<>("Description", Codec.STRING, false, true),
                    (i, s) -> i.description = s, i -> i.description,
                    (i, inherited) -> i.description = inherited.description)
            .add()
            .appendInherited(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled,
                    (i, inherited) -> i.enabled = inherited.enabled)
            .add()
            .appendInherited(new KeyedCodec<>("VisibleInUi", Codec.BOOLEAN, false, true),
                    (i, s) -> i.visibleInUi = s, i -> i.visibleInUi,
                    (i, inherited) -> i.visibleInUi = inherited.visibleInUi)
            .add()
            .appendInherited(new KeyedCodec<>("SortOrder", Codec.INTEGER, false, true),
                    (i, s) -> i.sortOrder = s, i -> i.sortOrder,
                    (i, inherited) -> i.sortOrder = inherited.sortOrder)
            .add()
            .appendInherited(new KeyedCodec<>("Icon", Codec.STRING, false, true),
                    (i, s) -> i.icon = (s == null || s.isEmpty()) ? null : s,
                    i -> i.icon,
                    (i, inherited) -> i.icon = inherited.icon)
            .addValidator(CommonAssetValidator.ICON_ITEM)
            .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))
            .add()
            .appendInherited(new KeyedCodec<>("StorageMode", new EnumCodec<>(CurrencyStorageMode.class), false, true),
                    (i, s) -> i.storageMode = s, i -> i.storageMode,
                    (i, inherited) -> i.storageMode = inherited.storageMode)
            .metadata(new UIEditorSectionStart("Behavior"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("AllowedScopes", SCOPE_LIST_CODEC, false, true),
                    (i, s) -> i.allowedScopes = s, i -> i.allowedScopes,
                    (i, inherited) -> i.allowedScopes = inherited.allowedScopes)
            .add()
            .appendInherited(new KeyedCodec<>("StartingBalance", Codec.LONG, false, true),
                    (i, s) -> i.startingBalance = s, i -> i.startingBalance,
                    (i, inherited) -> i.startingBalance = inherited.startingBalance)
            .add()
            .appendInherited(new KeyedCodec<>("MinBalance", Codec.LONG, false, true),
                    (i, s) -> i.minBalance = s, i -> i.minBalance,
                    (i, inherited) -> i.minBalance = inherited.minBalance)
            .add()
            .appendInherited(new KeyedCodec<>("MaxBalance", Codec.LONG, false, true),
                    (i, s) -> i.maxBalance = s, i -> i.maxBalance,
                    (i, inherited) -> i.maxBalance = inherited.maxBalance)
            .add()
            .appendInherited(new KeyedCodec<>("AllowNegative", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowNegative = s, i -> i.allowNegative,
                    (i, inherited) -> i.allowNegative = inherited.allowNegative)
            .add()
                .appendInherited(new KeyedCodec<>("Conversions", CONVERSION_LIST_CODEC, false, true),
                    (i, s) -> i.conversions = s, i -> i.conversions,
                    (i, inherited) -> i.conversions = inherited.conversions)
                .metadata(new UIEditorSectionStart("Conversions"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add()
            .appendInherited(new KeyedCodec<>("PrimaryItemId", Codec.STRING, false, true),
                    (i, s) -> i.primaryItemId = s, i -> i.primaryItemId,
                    (i, inherited) -> i.primaryItemId = inherited.primaryItemId)
            .metadata(new UIEditorSectionStart("Item Matching"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("AlternateItemIds", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.alternateItemIds = s, i -> i.alternateItemIds,
                    (i, inherited) -> i.alternateItemIds = inherited.alternateItemIds)
            .add()
                .appendInherited(new KeyedCodec<>("AcceptedItemTags", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.acceptedItemTags = s, i -> i.acceptedItemTags,
                    (i, inherited) -> i.acceptedItemTags = inherited.acceptedItemTags)
                .add()
            .appendInherited(new KeyedCodec<>("AcceptedItemCategories", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.acceptedItemCategories = s, i -> i.acceptedItemCategories,
                    (i, inherited) -> i.acceptedItemCategories = inherited.acceptedItemCategories)
            .add()
            .build();

    private static AssetStore<String, CurrencyTypeDefinition, DefaultAssetMap<String, CurrencyTypeDefinition>> assetStore;
    private static DefaultAssetMap<String, CurrencyTypeDefinition> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private String parent;
    private String displayName;
    private String description;
    private boolean enabled;
    private boolean visibleInUi;
    private int sortOrder;
    private String icon;
    private CurrencyStorageMode storageMode;
    private List<CurrencyScope> allowedScopes;
    private long startingBalance;
    private long minBalance;
    private long maxBalance;
    private boolean allowNegative;
    private List<CurrencyConversionDefinition> conversions;
    private String primaryItemId;
    private List<String> alternateItemIds;
    private List<String> acceptedItemTags;
    private List<String> acceptedItemCategories;

    public CurrencyTypeDefinition() {
        this.id = "";
        this.parent = "";
        this.displayName = "";
        this.description = "";
        this.enabled = true;
        this.visibleInUi = true;
        this.sortOrder = 0;
        this.icon = null;
        this.storageMode = CurrencyStorageMode.NumericWallet;
        this.allowedScopes = new ArrayList<>(List.of(CurrencyScope.Character, CurrencyScope.Account, CurrencyScope.Guild));
        this.startingBalance = 0L;
        this.minBalance = 0L;
        this.maxBalance = Long.MAX_VALUE;
        this.allowNegative = false;
        this.conversions = new ArrayList<>();
        this.primaryItemId = "";
        this.alternateItemIds = new ArrayList<>();
        this.acceptedItemTags = new ArrayList<>();
        this.acceptedItemCategories = new ArrayList<>();
    }

    @Nullable
    public static AssetStore<String, CurrencyTypeDefinition, DefaultAssetMap<String, CurrencyTypeDefinition>> getAssetStore() {
        if (assetStore == null) {
            assetStore = AssetRegistry.getAssetStore(CurrencyTypeDefinition.class);
        }
        return assetStore;
    }

    @Nullable
    public static DefaultAssetMap<String, CurrencyTypeDefinition> getAssetMap() {
        if (assetMap == null) {
            AssetStore<String, CurrencyTypeDefinition, DefaultAssetMap<String, CurrencyTypeDefinition>> store = getAssetStore();
            if (store == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, CurrencyTypeDefinition>) store.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getParent() {
        return parent == null ? "" : parent;
    }

    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? id : displayName;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVisibleInUi() {
        return visibleInUi;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @Nonnull
    public CurrencyStorageMode getStorageMode() {
        return storageMode == null ? CurrencyStorageMode.NumericWallet : storageMode;
    }

    @Nonnull
    public List<CurrencyScope> getAllowedScopes() {
        return allowedScopes == null ? List.of() : Collections.unmodifiableList(allowedScopes);
    }

    public boolean supportsScope(@Nonnull CurrencyScope scope) {
        List<CurrencyScope> scopes = getAllowedScopes();
        return scopes.isEmpty() || scopes.contains(scope);
    }

    public long getStartingBalance() {
        return startingBalance;
    }

    public long getMinBalance() {
        return allowNegative ? minBalance : Math.max(0L, minBalance);
    }

    public long getMaxBalance() {
        return maxBalance <= 0L ? Long.MAX_VALUE : maxBalance;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    @Nonnull
    public List<CurrencyConversionDefinition> getConversions() {
        return conversions == null ? List.of() : Collections.unmodifiableList(conversions);
    }

    public boolean hasAutoConversions() {
        for (CurrencyConversionDefinition conversion : getConversions()) {
            if (conversion != null && conversion.isEnabled() && conversion.isAutoConvert()) {
                return true;
            }
        }
        return false;
    }

    public boolean usesWalletBalances() {
        return switch (getStorageMode()) {
            case NumericWallet, ItemWallet, Hybrid -> true;
            case PhysicalItem -> false;
        };
    }

    public boolean usesPhysicalItems() {
        return switch (getStorageMode()) {
            case PhysicalItem, Hybrid -> true;
            case NumericWallet, ItemWallet -> false;
        };
    }

    public String getPrimaryItemId() {
        return primaryItemId == null ? "" : primaryItemId;
    }

    @Nonnull
    public List<String> getAlternateItemIds() {
        return alternateItemIds == null ? List.of() : Collections.unmodifiableList(alternateItemIds);
    }

    @Nonnull
    public List<String> getAcceptedItemTags() {
        return acceptedItemTags == null ? List.of() : Collections.unmodifiableList(acceptedItemTags);
    }

    @Nonnull
    public List<String> getAcceptedItemCategories() {
        return acceptedItemCategories == null ? List.of() : Collections.unmodifiableList(acceptedItemCategories);
    }

    @Nonnull
    public List<String> getAllAcceptedItemIds() {
        List<String> ids = new ArrayList<>();
        if (!getPrimaryItemId().isBlank()) {
            ids.add(getPrimaryItemId());
        }
        for (String value : getAlternateItemIds()) {
            if (value != null && !value.isBlank() && !ids.contains(value)) {
                ids.add(value);
            }
        }
        return ids;
    }

    public static class CurrencyConversionDefinition {

        public static final BuilderCodec<CurrencyConversionDefinition> CODEC = BuilderCodec.builder(
            CurrencyConversionDefinition.class,
            CurrencyConversionDefinition::new)
                .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                        (i, s) -> i.enabled = s, i -> i.enabled)
                .add()
                .append(new KeyedCodec<>("AutoConvert", Codec.BOOLEAN, false, true),
                        (i, s) -> i.autoConvert = s, i -> i.autoConvert)
                .add()
                .append(new KeyedCodec<>("TargetCurrencyId", Codec.STRING, false, true),
                        (i, s) -> i.targetCurrencyId = s, i -> i.targetCurrencyId)
                .add()
                .append(new KeyedCodec<>("SourceAmount", Codec.LONG, false, true),
                        (i, s) -> i.sourceAmount = s, i -> i.sourceAmount)
                .add()
                .append(new KeyedCodec<>("TargetAmount", Codec.LONG, false, true),
                        (i, s) -> i.targetAmount = s, i -> i.targetAmount)
                .add()
                .append(new KeyedCodec<>("Note", Codec.STRING, false, true),
                        (i, s) -> i.note = s, i -> i.note)
                .add()
                .build();

        private boolean enabled;
        private boolean autoConvert;
        private String targetCurrencyId;
        private long sourceAmount;
        private long targetAmount;
        private String note;

        public CurrencyConversionDefinition() {
            this.enabled = true;
            this.autoConvert = true;
            this.targetCurrencyId = "";
            this.sourceAmount = 1L;
            this.targetAmount = 1L;
            this.note = "";
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isAutoConvert() {
            return autoConvert;
        }

        public String getTargetCurrencyId() {
            return targetCurrencyId == null ? "" : targetCurrencyId;
        }

        public long getSourceAmount() {
            return sourceAmount;
        }

        public long getTargetAmount() {
            return targetAmount;
        }

        public String getNote() {
            return note == null ? "" : note;
        }

        public boolean isValid() {
            return enabled && !getTargetCurrencyId().isBlank() && sourceAmount > 0L && targetAmount > 0L;
        }
    }
}
