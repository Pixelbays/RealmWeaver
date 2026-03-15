package org.pixelbays.rpg.economy.banks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.economy.banks.config.BankScope;

@SuppressWarnings("null")
public class BankAccount {

    private final String id;
    private final String bankTypeId;
    private final BankScope ownerScope;
    private final String ownerId;
    private String displayName;
    private final List<String> unlockedTabIds;
    private final Map<String, Integer> tabSlotCounts;
    private final Map<String, List<BankTabItem>> tabItems;
    private final long createdAt;
    private long updatedAt;

    public BankAccount(@Nonnull String id, @Nonnull String bankTypeId, @Nonnull BankScope ownerScope,
                    @Nonnull String ownerId, @Nonnull String displayName,
                    @Nonnull List<String> unlockedTabIds, @Nonnull Map<String, Integer> tabSlotCounts,
                    @Nonnull Map<String, List<BankTabItem>> tabItems,
                    long createdAt, long updatedAt) {
        this.id = id;
        this.bankTypeId = bankTypeId;
        this.ownerScope = ownerScope;
        this.ownerId = ownerId;
        this.displayName = displayName;
        this.unlockedTabIds = new ArrayList<>(unlockedTabIds);
        this.tabSlotCounts = new LinkedHashMap<>(tabSlotCounts);
        this.tabItems = copyTabItems(tabItems);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getBankTypeId() {
        return bankTypeId;
    }

    @Nonnull
    public BankScope getOwnerScope() {
        return ownerScope;
    }

    @Nonnull
    public String getOwnerId() {
        return ownerId;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nonnull String displayName) {
        this.displayName = displayName;
        touch();
    }

    @Nonnull
    public List<String> getUnlockedTabIds() {
        return Collections.unmodifiableList(unlockedTabIds);
    }

    @Nonnull
    public Map<String, Integer> getTabSlotCounts() {
        return Collections.unmodifiableMap(tabSlotCounts);
    }

    @Nonnull
    public Map<String, List<BankTabItem>> getAllTabItems() {
        return Collections.unmodifiableMap(copyTabItems(tabItems));
    }

    @Nonnull
    public List<BankTabItem> getTabItems(@Nonnull String tabId) {
        List<BankTabItem> items = tabItems.get(tabId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTabUnlocked(@Nonnull String tabId) {
        return unlockedTabIds.contains(tabId);
    }

    public void unlockTab(@Nonnull String tabId) {
        if (!unlockedTabIds.contains(tabId)) {
            unlockedTabIds.add(tabId);
            touch();
        }
    }

    public int getTabSlotCount(@Nonnull String tabId, int fallback) {
        return tabSlotCounts.getOrDefault(tabId, fallback);
    }

    public void setTabSlotCount(@Nonnull String tabId, int slotCount) {
        tabSlotCounts.put(tabId, slotCount);
        touch();
    }

    public void replaceTabItems(@Nonnull String tabId, @Nonnull List<BankTabItem> items) {
        List<BankTabItem> sanitized = new ArrayList<>();
        for (BankTabItem item : items) {
            if (item == null || item.getSlotIndex() < 0 || item.getItemStack().isEmpty()) {
                continue;
            }
            sanitized.add(new BankTabItem(item.getSlotIndex(), item.getItemStack()));
        }

        sanitized.sort((left, right) -> Integer.compare(left.getSlotIndex(), right.getSlotIndex()));
        if (sanitized.isEmpty()) {
            tabItems.remove(tabId);
        } else {
            tabItems.put(tabId, sanitized);
        }
        touch();
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    @Nonnull
    private static Map<String, List<BankTabItem>> copyTabItems(@Nonnull Map<String, List<BankTabItem>> source) {
        Map<String, List<BankTabItem>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<BankTabItem>> entry : source.entrySet()) {
            List<BankTabItem> items = new ArrayList<>();
            if (entry.getValue() != null) {
                for (BankTabItem item : entry.getValue()) {
                    if (item == null) {
                        continue;
                    }
                    items.add(new BankTabItem(item.getSlotIndex(), item.getItemStack()));
                }
            }
            copy.put(entry.getKey(), items);
        }
        return copy;
    }
}
