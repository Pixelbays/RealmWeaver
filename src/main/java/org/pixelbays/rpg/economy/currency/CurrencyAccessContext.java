package org.pixelbays.rpg.economy.currency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public class CurrencyAccessContext {

    private final List<ItemContainer> itemContainers;

    public CurrencyAccessContext(@Nonnull List<ItemContainer> itemContainers) {
        this.itemContainers = List.copyOf(itemContainers);
    }

    @Nonnull
    public static CurrencyAccessContext empty() {
        return new CurrencyAccessContext(List.of());
    }

    @Nonnull
    public static CurrencyAccessContext fromInventory(@Nonnull Inventory inventory) {
        List<ItemContainer> containers = new ArrayList<>();
        containers.add(inventory.getStorage());
        containers.add(inventory.getHotbar());
        containers.add(inventory.getBackpack());
        return new CurrencyAccessContext(containers);
    }

    @Nonnull
    public List<ItemContainer> getItemContainers() {
        return Collections.unmodifiableList(itemContainers);
    }
}
