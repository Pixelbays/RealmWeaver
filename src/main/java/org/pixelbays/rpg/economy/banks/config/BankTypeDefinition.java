package org.pixelbays.rpg.economy.banks.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
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
public class BankTypeDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, BankTypeDefinition>> {

    public enum BankStorageMode {
        Items,
        Void,
        CurrencyOnly,
        ReagentsOnly,
        Mixed
    }

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.STRING, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    private static final FunctionCodec<BankTabDefinition[], List<BankTabDefinition>> TAB_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(BankTabDefinition.CODEC, BankTabDefinition[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(BankTabDefinition[]::new));

    private static final FunctionCodec<BankCostDefinition[], List<BankCostDefinition>> COST_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(BankCostDefinition.CODEC, BankCostDefinition[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(BankCostDefinition[]::new));

    public static final AssetBuilderCodec<String, BankTypeDefinition> CODEC = AssetBuilderCodec.builder(
            BankTypeDefinition.class,
            BankTypeDefinition::new,
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
            .appendInherited(new KeyedCodec<>("Icon", Codec.STRING),
                    (i, s) -> i.icon = (s == null || s.isEmpty()) ? null : s,
                    i -> i.icon,
                    (i, inherited) -> i.icon = inherited.icon)
            .addValidator(CommonAssetValidator.ICON_ITEM)
            .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))
            .add()
            .appendInherited(new KeyedCodec<>("Scope", new EnumCodec<>(BankScope.class), false, true),
                    (i, s) -> i.scope = s, i -> i.scope,
                    (i, inherited) -> i.scope = inherited.scope)
            .metadata(new UIEditorSectionStart("Ownership"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("StorageMode", new EnumCodec<>(BankStorageMode.class), false, true),
                    (i, s) -> i.storageMode = s, i -> i.storageMode,
                    (i, inherited) -> i.storageMode = inherited.storageMode)
            .add()
            .appendInherited(new KeyedCodec<>("RequiredPermission", Codec.STRING, false, true),
                    (i, s) -> i.requiredPermission = s, i -> i.requiredPermission,
                    (i, inherited) -> i.requiredPermission = inherited.requiredPermission)
            .add()
            .appendInherited(new KeyedCodec<>("AutoCreateOnFirstUse", Codec.BOOLEAN, false, true),
                    (i, s) -> i.autoCreateOnFirstUse = s, i -> i.autoCreateOnFirstUse,
                    (i, inherited) -> i.autoCreateOnFirstUse = inherited.autoCreateOnFirstUse)
            .add()
            .appendInherited(new KeyedCodec<>("RemoteAccessAllowed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.remoteAccessAllowed = s, i -> i.remoteAccessAllowed,
                    (i, inherited) -> i.remoteAccessAllowed = inherited.remoteAccessAllowed)
            .add()
            .appendInherited(new KeyedCodec<>("DepositEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.depositEnabled = s, i -> i.depositEnabled,
                    (i, inherited) -> i.depositEnabled = inherited.depositEnabled)
            .metadata(new UIEditorSectionStart("Access"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("WithdrawEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.withdrawEnabled = s, i -> i.withdrawEnabled,
                    (i, inherited) -> i.withdrawEnabled = inherited.withdrawEnabled)
            .add()
            .appendInherited(new KeyedCodec<>("SearchEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.searchEnabled = s, i -> i.searchEnabled,
                    (i, inherited) -> i.searchEnabled = inherited.searchEnabled)
            .add()
            .appendInherited(new KeyedCodec<>("SortEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.sortEnabled = s, i -> i.sortEnabled,
                    (i, inherited) -> i.sortEnabled = inherited.sortEnabled)
            .add()
            .appendInherited(new KeyedCodec<>("AutoStackEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.autoStackEnabled = s, i -> i.autoStackEnabled,
                    (i, inherited) -> i.autoStackEnabled = inherited.autoStackEnabled)
            .add()
            .appendInherited(new KeyedCodec<>("DefaultSlotCount", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultSlotCount = s, i -> i.defaultSlotCount,
                    (i, inherited) -> i.defaultSlotCount = inherited.defaultSlotCount)
            .metadata(new UIEditorSectionStart("Storage Layout"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("MaxSlotCountPerTab", Codec.INTEGER, false, true),
                    (i, s) -> i.maxSlotCountPerTab = s, i -> i.maxSlotCountPerTab,
                    (i, inherited) -> i.maxSlotCountPerTab = inherited.maxSlotCountPerTab)
            .add()
            .appendInherited(new KeyedCodec<>("DefaultTabCount", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultTabCount = s, i -> i.defaultTabCount,
                    (i, inherited) -> i.defaultTabCount = inherited.defaultTabCount)
            .add()
            .appendInherited(new KeyedCodec<>("MaxTabs", Codec.INTEGER, false, true),
                    (i, s) -> i.maxTabs = s, i -> i.maxTabs,
                    (i, inherited) -> i.maxTabs = inherited.maxTabs)
            .add()
            .appendInherited(new KeyedCodec<>("AllowedItemTags", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.allowedItemTags = s, i -> i.allowedItemTags,
                    (i, inherited) -> i.allowedItemTags = inherited.allowedItemTags)
            .add()
            .appendInherited(new KeyedCodec<>("BlockedItemTags", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.blockedItemTags = s, i -> i.blockedItemTags,
                    (i, inherited) -> i.blockedItemTags = inherited.blockedItemTags)
            .add()
            .appendInherited(new KeyedCodec<>("OpenCost", BankCostDefinition.CODEC, false, true),
                    (i, s) -> i.openCost = s, i -> i.openCost,
                    (i, inherited) -> i.openCost = inherited.openCost)
            .metadata(new UIEditorSectionStart("Costs"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .appendInherited(new KeyedCodec<>("AdditionalTabCosts", COST_LIST_CODEC, false, true),
                    (i, s) -> i.additionalTabCosts = s, i -> i.additionalTabCosts,
                    (i, inherited) -> i.additionalTabCosts = inherited.additionalTabCosts)
            .add()
            .appendInherited(new KeyedCodec<>("SlotUpgradeCosts", COST_LIST_CODEC, false, true),
                    (i, s) -> i.slotUpgradeCosts = s, i -> i.slotUpgradeCosts,
                    (i, inherited) -> i.slotUpgradeCosts = inherited.slotUpgradeCosts)
            .add()
            .appendInherited(new KeyedCodec<>("Tabs", TAB_LIST_CODEC, false, true),
                    (i, s) -> i.tabs = s, i -> i.tabs,
                    (i, inherited) -> i.tabs = inherited.tabs)
            .metadata(new UIEditorSectionStart("Tabs"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .build();

    public static final ContainedAssetCodec<String, BankTypeDefinition, ?> CHILD_ASSET_CODEC = new ContainedAssetCodec<>(
            BankTypeDefinition.class,
            CODEC);

    private static AssetStore<String, BankTypeDefinition, DefaultAssetMap<String, BankTypeDefinition>> assetStore;
    private static DefaultAssetMap<String, BankTypeDefinition> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private String parent;
    private String displayName;
    private String description;
    private boolean enabled;
    private boolean visibleInUi;
    private String icon;
    private BankScope scope;
    private BankStorageMode storageMode;
    private String requiredPermission;
    private boolean autoCreateOnFirstUse;
    private boolean remoteAccessAllowed;
    private boolean depositEnabled;
    private boolean withdrawEnabled;
    private boolean searchEnabled;
    private boolean sortEnabled;
    private boolean autoStackEnabled;
    private int defaultSlotCount;
    private int maxSlotCountPerTab;
    private int defaultTabCount;
    private int maxTabs;
    private List<String> allowedItemTags;
    private List<String> blockedItemTags;
    private BankCostDefinition openCost;
    private List<BankCostDefinition> additionalTabCosts;
    private List<BankCostDefinition> slotUpgradeCosts;
    private List<BankTabDefinition> tabs;

    public BankTypeDefinition() {
        this.id = "";
        this.parent = "";
        this.displayName = "";
        this.description = "";
        this.enabled = true;
        this.visibleInUi = true;
        this.icon = null;
        this.scope = BankScope.Player;
        this.storageMode = BankStorageMode.Items;
        this.requiredPermission = "";
        this.autoCreateOnFirstUse = true;
        this.remoteAccessAllowed = false;
        this.depositEnabled = true;
        this.withdrawEnabled = true;
        this.searchEnabled = true;
        this.sortEnabled = true;
        this.autoStackEnabled = true;
        this.defaultSlotCount = 24;
        this.maxSlotCountPerTab = 48;
        this.defaultTabCount = 1;
        this.maxTabs = 4;
        this.allowedItemTags = new ArrayList<>();
        this.blockedItemTags = new ArrayList<>();
        this.openCost = new BankCostDefinition();
        this.additionalTabCosts = new ArrayList<>();
        this.slotUpgradeCosts = new ArrayList<>();
        this.tabs = new ArrayList<>();
    }

    @Nullable
    public static AssetStore<String, BankTypeDefinition, DefaultAssetMap<String, BankTypeDefinition>> getAssetStore() {
        if (assetStore == null) {
            assetStore = AssetRegistry.getAssetStore(BankTypeDefinition.class);
        }
        return assetStore;
    }

    @Nullable
    public static DefaultAssetMap<String, BankTypeDefinition> getAssetMap() {
        if (assetMap == null) {
            AssetStore<String, BankTypeDefinition, DefaultAssetMap<String, BankTypeDefinition>> store = getAssetStore();
            if (store == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, BankTypeDefinition>) store.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
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

    @Nullable
    public String getIcon() {
        return icon;
    }

    @Nonnull
    public BankScope getScope() {
        return scope == null ? BankScope.Player : scope;
    }

    @Nonnull
    public BankStorageMode getStorageMode() {
        return storageMode == null ? BankStorageMode.Items : storageMode;
    }

    public String getRequiredPermission() {
        return requiredPermission == null ? "" : requiredPermission;
    }

    public boolean isAutoCreateOnFirstUse() {
        return autoCreateOnFirstUse;
    }

    public boolean isRemoteAccessAllowed() {
        return remoteAccessAllowed;
    }

    public boolean isDepositEnabled() {
        return depositEnabled;
    }

    public boolean isWithdrawEnabled() {
        return withdrawEnabled;
    }

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public boolean isSortEnabled() {
        return sortEnabled;
    }

    public boolean isAutoStackEnabled() {
        return autoStackEnabled;
    }

    public int getDefaultSlotCount() {
        return defaultSlotCount;
    }

    public int getMaxSlotCountPerTab() {
        return maxSlotCountPerTab;
    }

    public int getDefaultTabCount() {
        return defaultTabCount;
    }

    public int getMaxTabs() {
        return maxTabs;
    }

    @Nonnull
    public List<String> getAllowedItemTags() {
        return allowedItemTags == null ? Collections.emptyList() : Collections.unmodifiableList(allowedItemTags);
    }

    @Nonnull
    public List<String> getBlockedItemTags() {
        return blockedItemTags == null ? Collections.emptyList() : Collections.unmodifiableList(blockedItemTags);
    }

    @Nonnull
    public BankCostDefinition getOpenCost() {
        return openCost == null ? new BankCostDefinition() : openCost;
    }

    @Nonnull
    public List<BankCostDefinition> getAdditionalTabCosts() {
        return additionalTabCosts == null ? Collections.emptyList() : Collections.unmodifiableList(additionalTabCosts);
    }

    @Nonnull
    public List<BankCostDefinition> getSlotUpgradeCosts() {
        return slotUpgradeCosts == null ? Collections.emptyList() : Collections.unmodifiableList(slotUpgradeCosts);
    }

    @Nonnull
    public List<BankTabDefinition> getTabs() {
        return tabs == null ? Collections.emptyList() : Collections.unmodifiableList(tabs);
    }

    @Nonnull
    public List<BankTabDefinition> getResolvedTabs() {
        if (tabs != null && !tabs.isEmpty()) {
            return Collections.unmodifiableList(tabs);
        }

        BankTabDefinition fallback = new BankTabDefinition();
        fallback.id = "Main";
        fallback.displayName = getDisplayName();
        fallback.description = getDescription();
        fallback.slotCount = Math.max(1, defaultSlotCount);
        fallback.unlockByDefault = true;
        return List.of(fallback);
    }

    public int getUnlockedTabCount() {
        int unlocked = 0;
        for (BankTabDefinition tab : getResolvedTabs()) {
            if (tab.isUnlockByDefault()) {
                unlocked++;
            }
        }
        return Math.max(defaultTabCount, unlocked);
    }

    public static class BankCostDefinition {
        public static final BuilderCodec<BankCostDefinition> CODEC = BuilderCodec
                .builder(BankCostDefinition.class, BankCostDefinition::new)
                .append(new KeyedCodec<>("Tier", Codec.INTEGER, false, true),
                        (i, s) -> i.tier = s, i -> i.tier)
                .add()
            .append(new KeyedCodec<>("CurrencyScope", new EnumCodec<>(CurrencyScope.class), false, true),
                (i, s) -> i.currencyScope = s, i -> i.currencyScope)
            .add()
                .append(new KeyedCodec<>("CurrencyId", Codec.STRING, false, true),
                        (i, s) -> i.currencyId = s, i -> i.currencyId)
                .add()
                .append(new KeyedCodec<>("Amount", Codec.INTEGER, false, true),
                        (i, s) -> i.amount = s, i -> i.amount)
                .add()
                .append(new KeyedCodec<>("ItemTag", Codec.STRING, false, true),
                        (i, s) -> i.itemTag = s, i -> i.itemTag)
                .add()
                .append(new KeyedCodec<>("ItemCount", Codec.INTEGER, false, true),
                        (i, s) -> i.itemCount = s, i -> i.itemCount)
                .add()
                .append(new KeyedCodec<>("Note", Codec.STRING, false, true),
                        (i, s) -> i.note = s, i -> i.note)
                .add()
                .build();

        private int tier;
    private CurrencyScope currencyScope;
        private String currencyId;
        private int amount;
        private String itemTag;
        private int itemCount;
        private String note;

        public BankCostDefinition() {
            this.tier = 1;
            this.currencyScope = null;
            this.currencyId = "gold";
            this.amount = 0;
            this.itemTag = "";
            this.itemCount = 0;
            this.note = "";
        }

        public int getTier() {
            return tier;
        }

        @Nullable
        public CurrencyScope getCurrencyScope() {
            return currencyScope;
        }

        public String getCurrencyId() {
            return currencyId == null ? "" : currencyId;
        }

        public int getAmount() {
            return amount;
        }

        public String getItemTag() {
            return itemTag == null ? "" : itemTag;
        }

        public int getItemCount() {
            return itemCount;
        }

        public String getNote() {
            return note == null ? "" : note;
        }

        public boolean hasCurrencyCost() {
            return amount > 0 && currencyId != null && !currencyId.isBlank();
        }

        public boolean hasItemCost() {
            return itemCount > 0 && itemTag != null && !itemTag.isBlank();
        }

        public boolean isFree() {
            return amount <= 0 && itemCount <= 0;
        }
    }

    public static class BankTabDefinition {
        public static final BuilderCodec<BankTabDefinition> CODEC = BuilderCodec
                .builder(BankTabDefinition.class, BankTabDefinition::new)
                .append(new KeyedCodec<>("Id", Codec.STRING, false, true),
                        (i, s) -> i.id = s, i -> i.id)
                .add()
                .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                        (i, s) -> i.displayName = s, i -> i.displayName)
                .add()
                .append(new KeyedCodec<>("Description", Codec.STRING, false, true),
                        (i, s) -> i.description = s, i -> i.description)
                .add()
                .append(new KeyedCodec<>("Icon", Codec.STRING, false, true),
                        (i, s) -> i.icon = s, i -> i.icon)
                .add()
                .append(new KeyedCodec<>("SlotCount", Codec.INTEGER, false, true),
                        (i, s) -> i.slotCount = s, i -> i.slotCount)
                .add()
                .append(new KeyedCodec<>("UnlockByDefault", Codec.BOOLEAN, false, true),
                        (i, s) -> i.unlockByDefault = s, i -> i.unlockByDefault)
                .add()
                .append(new KeyedCodec<>("AllowedItemTags", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.allowedItemTags = s, i -> i.allowedItemTags)
                .add()
                .append(new KeyedCodec<>("BlockedItemTags", STRING_LIST_CODEC, false, true),
                        (i, s) -> i.blockedItemTags = s, i -> i.blockedItemTags)
                .add()
                .append(new KeyedCodec<>("UnlockCost", BankCostDefinition.CODEC, false, true),
                        (i, s) -> i.unlockCost = s, i -> i.unlockCost)
                .add()
                .build();

        private String id;
        private String displayName;
        private String description;
        private String icon;
        private int slotCount;
        private boolean unlockByDefault;
        private List<String> allowedItemTags;
        private List<String> blockedItemTags;
        private BankCostDefinition unlockCost;

        public BankTabDefinition() {
            this.id = "Tab";
            this.displayName = "Tab";
            this.description = "";
            this.icon = "";
            this.slotCount = 24;
            this.unlockByDefault = true;
            this.allowedItemTags = new ArrayList<>();
            this.blockedItemTags = new ArrayList<>();
            this.unlockCost = new BankCostDefinition();
        }

        public String getId() {
            return id == null || id.isBlank() ? "Tab" : id;
        }

        public String getDisplayName() {
            return displayName == null || displayName.isBlank() ? getId() : displayName;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public String getIcon() {
            return icon == null ? "" : icon;
        }

        public int getSlotCount() {
            return slotCount;
        }

        public boolean isUnlockByDefault() {
            return unlockByDefault;
        }

        @Nonnull
        public List<String> getAllowedItemTags() {
            return allowedItemTags == null ? Collections.emptyList() : Collections.unmodifiableList(allowedItemTags);
        }

        @Nonnull
        public List<String> getBlockedItemTags() {
            return blockedItemTags == null ? Collections.emptyList() : Collections.unmodifiableList(blockedItemTags);
        }

        @Nonnull
        public BankCostDefinition getUnlockCost() {
            return unlockCost == null ? new BankCostDefinition() : unlockCost;
        }
    }
}
