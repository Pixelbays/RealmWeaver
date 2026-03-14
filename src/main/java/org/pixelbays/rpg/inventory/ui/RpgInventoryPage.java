package org.pixelbays.rpg.inventory.ui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class RpgInventoryPage extends CustomUIPage {

    private static final String STORAGE_GRID_SELECTOR = "#StorageGrid";
    private static final String HOTBAR_GRID_SELECTOR = "#HotbarGrid";
    private static final String ARMOR_GRID_SELECTOR = "#ArmorGrid";
    private static final String UTILITY_GRID_SELECTOR = "#UtilityGrid";

    private final List<EventRegistration> containerRegistrations = new ArrayList<>();
    private boolean listenersRegistered;
    private String pendingSourceGrid;
    private Integer pendingSourceSlot;

    public RpgInventoryPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/RpgInventoryPage.ui");

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        populateGrids(commandBuilder, player.getInventory());
        bindSlotEvents(eventBuilder);
        registerInventoryListeners(player.getInventory());
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String grid = extractString(rawData, "Grid");
        String event = extractString(rawData, "Event");
        Integer slotIndex = extractInt(rawData, "SlotIndex");
        Integer sourceSlotId = extractInt(rawData, "SourceSlotId");
        Integer sourceGridIndex = extractInt(rawData, "SourceItemGridIndex");
        Integer sourceSectionId = extractInt(rawData, "SourceInventorySectionId");
        Integer itemGridIndex = extractInt(rawData, "ItemGridIndex");
        Integer targetSlotId = extractInt(rawData, "TargetSlotId");
        Integer targetSectionId = extractInt(rawData, "TargetInventorySectionId");
        Integer itemQuantity = extractInt(rawData, "ItemStackQuantity");
        String itemId = extractString(rawData, "ItemStackId");
        Integer mouseButton = extractInt(rawData, "PressedMouseButton");

        RpgLogging.debugDeveloper(
                "[InventoryUI] Slot event: grid=%s event=%s slot=%s sourceSlot=%s sourceGrid=%s sourceSection=%s itemGrid=%s targetSlot=%s targetSection=%s qty=%s item=%s button=%s raw=%s",
                grid,
                event,
                slotIndex,
                sourceSlotId,
                sourceGridIndex,
                sourceSectionId,
                itemGridIndex,
                targetSlotId,
                targetSectionId,
                itemQuantity,
                itemId,
                mouseButton,
                rawData);

        if (grid == null || slotIndex == null || event == null) {
            return;
        }

        if (!"SlotClicking".equals(event)) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> handleSlotClick(ref, store, grid, slotIndex, itemQuantity, mouseButton));
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        for (EventRegistration registration : containerRegistrations) {
            registration.unregister();
        }
        containerRegistrations.clear();
        listenersRegistered = false;
    }

    private void bindSlotEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                STORAGE_GRID_SELECTOR,
                new EventData().append("Grid", "Storage").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotDoubleClicking, STORAGE_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, STORAGE_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, STORAGE_GRID_SELECTOR);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                HOTBAR_GRID_SELECTOR,
                new EventData().append("Grid", "Hotbar").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotDoubleClicking, HOTBAR_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, HOTBAR_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, HOTBAR_GRID_SELECTOR);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                ARMOR_GRID_SELECTOR,
                new EventData().append("Grid", "Armor").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotDoubleClicking, ARMOR_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, ARMOR_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, ARMOR_GRID_SELECTOR);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                UTILITY_GRID_SELECTOR,
                new EventData().append("Grid", "Utility").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotDoubleClicking, UTILITY_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, UTILITY_GRID_SELECTOR);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, UTILITY_GRID_SELECTOR);
    }

    private void handleSlotClick(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String grid,
            int slotIndex,
            Integer itemQuantity,
            Integer mouseButton) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if ("Hotbar".equals(grid)) {
            player.getInventory().setActiveSlot(Inventory.HOTBAR_SECTION_ID, (byte) slotIndex);
        }

        if (mouseButton == null || mouseButton != 1) {
            return;
        }

        if (pendingSourceGrid == null || pendingSourceSlot == null) {
            pendingSourceGrid = grid;
            pendingSourceSlot = slotIndex;
            return;
        }

        Integer sourceSectionId = gridToSectionId(pendingSourceGrid);
        Integer targetSectionId = gridToSectionId(grid);
        if (sourceSectionId == null || targetSectionId == null) {
            pendingSourceGrid = null;
            pendingSourceSlot = null;
            return;
        }

        int quantity = itemQuantity != null && itemQuantity > 0 ? itemQuantity : 1;
        player.getInventory().moveItem(sourceSectionId, pendingSourceSlot, quantity, targetSectionId, slotIndex);
        pendingSourceGrid = null;
        pendingSourceSlot = null;
    }

    private static Integer gridToSectionId(@Nonnull String grid) {
        return switch (grid) {
            case "Storage" -> Inventory.STORAGE_SECTION_ID;
            case "Hotbar" -> Inventory.HOTBAR_SECTION_ID;
            case "Armor" -> Inventory.ARMOR_SECTION_ID;
            case "Utility" -> Inventory.UTILITY_SECTION_ID;
            default -> null;
        };
    }

    private static Integer extractInt(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int start = colonIndex + 1;
        while (start < rawData.length() && rawData.charAt(start) == ' ') {
            start++;
        }

        int end = start;
        while (end < rawData.length() && (rawData.charAt(end) == '-' || Character.isDigit(rawData.charAt(end)))) {
            end++;
        }

        if (end == start) {
            return null;
        }

        try {
            return Integer.parseInt(rawData.substring(start, end));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = rawData.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return rawData.substring(firstQuote + 1, secondQuote);
    }

    private void registerInventoryListeners(@Nonnull Inventory inventory) {
        if (listenersRegistered) {
            return;
        }

        listenersRegistered = true;
        registerContainerListener(inventory.getStorage());
        registerContainerListener(inventory.getHotbar());
        registerContainerListener(inventory.getArmor());
        registerContainerListener(inventory.getUtility());
    }

    private void registerContainerListener(ItemContainer container) {
        if (container == null) {
            return;
        }

        containerRegistrations.add(container.registerChangeEvent(event -> sendGridUpdate()));
    }

    private void sendGridUpdate() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        populateGrids(commandBuilder, player.getInventory());
        UIEventBuilder eventBuilder = new UIEventBuilder();
        bindSlotEvents(eventBuilder);
        sendUpdateWithEvents(commandBuilder, eventBuilder);
    }

    private void sendUpdateWithEvents(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.getPageManager().updateCustomPage(
                new CustomPage(
                        getClass().getName(),
                        false,
                        false,
                        getLifetime(),
                        commandBuilder.getCommands(),
                        eventBuilder.getEvents()));
    }

    private void populateGrids(@Nonnull UICommandBuilder commandBuilder, @Nonnull Inventory inventory) {
        populateGrid(commandBuilder, STORAGE_GRID_SELECTOR, inventory.getStorage());
        populateGrid(commandBuilder, HOTBAR_GRID_SELECTOR, inventory.getHotbar());
        populateGrid(commandBuilder, ARMOR_GRID_SELECTOR, inventory.getArmor());
        populateGrid(commandBuilder, UTILITY_GRID_SELECTOR, inventory.getUtility());
    }

    private void populateGrid(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull String selector,
            ItemContainer container) {
        List<ItemGridSlot> slots = new ArrayList<>();
        if (container != null) {
            int capacity = container.getCapacity();
            slots = new ArrayList<>(capacity);
            for (short i = 0; i < capacity; i++) {
                ItemStack stack = container.getItemStack(i);
                ItemGridSlot slot = stack == null || stack.isEmpty()
                        ? new ItemGridSlot()
                        : new ItemGridSlot(stack);
                slot.setActivatable(true);
                slots.add(slot);
            }
        }

        commandBuilder.set(selector + ".Slots", slots);
    }
}
