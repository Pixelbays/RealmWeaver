package org.pixelbays.rpg.economy.banks.ui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.banks.BankAccount;
import org.pixelbays.rpg.economy.banks.BankActionResult;
import org.pixelbays.rpg.economy.banks.BankManager;
import org.pixelbays.rpg.economy.banks.command.BankCommandUtil;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "null", "rawtypes" })
public class BankStoragePage extends CustomUIPage {

    private static final String BANK_GRID_SELECTOR = "#BankGrid";
    private static final String PLAYER_STORAGE_SELECTOR = "#PlayerStorageGrid";
    private static final String PLAYER_HOTBAR_SELECTOR = "#PlayerHotbarGrid";

    private static final String[] TAB_BUTTON_SELECTORS = {
            "#TabButton0",
            "#TabButton1",
            "#TabButton2",
            "#TabButton3"
    };

        private static final String NEXT_TAB_COST_LABEL = "#NextTabCostLabel";
        private static final String UNLOCK_NEXT_TAB_BUTTON = "#UnlockNextTabButton";
        private static final String CURRENCY_SUMMARY_LABEL = "#CurrencySummaryLabel";
        private static final String CURRENCY_ID_FIELD = "#CurrencyIdField";
        private static final String CURRENCY_AMOUNT_FIELD = "#CurrencyAmountField";
        private static final String DEPOSIT_CURRENCY_BUTTON = "#DepositCurrencyButton";
        private static final String WITHDRAW_CURRENCY_BUTTON = "#WithdrawCurrencyButton";

    private final BankManager bankManager;
    private final String bankId;
    private String currentTabId;

    private final List<EventRegistration> containerRegistrations = new ArrayList<>();
    private SimpleItemContainer bankContainer;

    public BankStoragePage(@Nonnull PlayerRef playerRef, @Nonnull String bankId, @Nonnull String initialTabId) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.bankManager = ExamplePlugin.get().getBankManager();
        this.bankId = bankId;
        this.currentTabId = initialTabId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/BankStoragePage.ui");
        bindEvents(eventBuilder);
        BankAccount bankAccount = bankManager.getBank(bankId);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (bankAccount == null || player == null) {
            return;
        }

        ensureBankContainer(bankAccount);
        appendView(commandBuilder, bankAccount, player.getInventory(), null);
        registerInventoryListeners(player.getInventory());
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        String grid = extractString(rawData, "Grid");
        String event = extractString(rawData, "Event");
        Integer slotIndex = extractInt(rawData, "SlotIndex");
        Integer mouseButton = extractInt(rawData, "PressedMouseButton");
        Integer tabIndex = extractInt(rawData, "TabIndex");
        String currencyId = extractString(rawData, "@CurrencyId");
        String currencyAmount = extractString(rawData, "@CurrencyAmount");

        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if ("SelectTab".equals(action) && tabIndex != null) {
                handleTabSwitch(ref, store, tabIndex);
                return;
            }

            if ("UnlockNextTab".equals(action)) {
                handleUnlockNextTab(ref, store);
                return;
            }

            if ("DepositCurrency".equals(action) || "WithdrawCurrency".equals(action)) {
                handleCurrencyAction(ref, store, action, currencyId, currencyAmount);
                return;
            }

            if (!"SlotClicking".equals(event) || grid == null || slotIndex == null) {
                return;
            }

            handleSlotClick(ref, store, grid, slotIndex, mouseButton);
        });
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        persistCurrentContainer();
        unregisterListeners();
    }

    private void handleTabSwitch(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            int tabIndex) {
        BankAccount bankAccount = bankManager.getBank(bankId);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (bankAccount == null || player == null) {
            return;
        }

        List<BankTypeDefinition.BankTabDefinition> tabs = bankManager.getAvailableTabs(bankAccount);
        if (tabIndex < 0 || tabIndex >= tabs.size()) {
            return;
        }

        String tabId = tabs.get(tabIndex).getId();
        if (tabId.equalsIgnoreCase(currentTabId)) {
            return;
        }

        persistCurrentContainer();
        currentTabId = tabId;
        ensureBankContainer(bankAccount);
        sendFullUpdate(player, bankAccount);
    }

    private void handleSlotClick(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String grid,
            int slotIndex,
            @Nullable Integer mouseButton) {
        BankAccount bankAccount = bankManager.getBank(bankId);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (bankAccount == null || player == null) {
            return;
        }

        ensureBankContainer(bankAccount);
        if (bankContainer == null) {
            return;
        }

        boolean singleItem = mouseButton != null && mouseButton == 1;
        Inventory inventory = player.getInventory();

        switch (grid) {
            case "Bank" -> moveFromBank((short) slotIndex, singleItem, inventory);
            case "PlayerStorage" -> moveToBank(inventory.getStorage(), (short) slotIndex, singleItem);
            case "PlayerHotbar" -> moveToBank(inventory.getHotbar(), (short) slotIndex, singleItem);
            default -> {
                return;
            }
        }

        persistCurrentContainer();
        sendFullUpdate(player, bankAccount);
    }

    private void handleUnlockNextTab(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        BankAccount bankAccount = bankManager.getBank(bankId);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (bankAccount == null || player == null) {
            return;
        }

        BankTypeDefinition.BankTabDefinition nextLockedTab = bankManager.getNextLockedTab(bankAccount);
        BankActionResult result = bankManager.unlockNextTab(bankAccount, playerRef.getUuid(), player.getInventory());
        if (result.isSuccess() && nextLockedTab != null) {
            currentTabId = nextLockedTab.getId();
            ensureBankContainer(bankAccount);
        }

        sendFullUpdate(player, bankAccount, BankCommandUtil.managerResultMessage(result.getMessage()));
    }

    private void handleCurrencyAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String currencyId,
            @Nullable String rawAmount) {
        BankAccount bankAccount = bankManager.getBank(bankId);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (bankAccount == null || player == null) {
            return;
        }

        long amount = parseLong(rawAmount);
        if (currencyId == null) {
            currencyId = "";
        }

        BankActionResult result = "DepositCurrency".equals(action)
                ? bankManager.depositCurrency(bankAccount, playerRef.getUuid(), player.getInventory(), currencyId.trim(), amount)
                : bankManager.withdrawCurrency(bankAccount, playerRef.getUuid(), player.getInventory(), currencyId.trim(), amount);
        sendFullUpdate(player, bankAccount, BankCommandUtil.managerResultMessage(result.getMessage()));
    }

    private void moveFromBank(short slotIndex, boolean singleItem, @Nonnull Inventory inventory) {
        if (bankContainer == null || slotIndex < 0 || slotIndex >= bankContainer.getCapacity()) {
            return;
        }

        if (singleItem) {
            bankContainer.moveItemStackFromSlot(slotIndex, 1, false, true,
                    inventory.getStorage(), inventory.getHotbar(), inventory.getBackpack());
        } else {
            bankContainer.moveItemStackFromSlot(slotIndex, false, true,
                    inventory.getStorage(), inventory.getHotbar(), inventory.getBackpack());
        }
    }

    private void moveToBank(@Nullable ItemContainer sourceContainer, short slotIndex, boolean singleItem) {
        if (bankContainer == null || sourceContainer == null || slotIndex < 0 || slotIndex >= sourceContainer.getCapacity()) {
            return;
        }

        if (singleItem) {
            sourceContainer.moveItemStackFromSlot(slotIndex, 1, bankContainer, false, true);
        } else {
            sourceContainer.moveItemStackFromSlot(slotIndex, bankContainer, false, true);
        }
    }

    private void ensureBankContainer(@Nonnull BankAccount bankAccount) {
        String resolvedTabId = currentTabId;
        if (resolvedTabId == null || bankManager.getTabDefinition(bankAccount, resolvedTabId) == null) {
            resolvedTabId = bankManager.resolveInitialTabId(bankAccount);
            currentTabId = resolvedTabId;
        }

        if (resolvedTabId == null) {
            bankContainer = null;
            return;
        }

        unregisterBankListener();
        bankContainer = bankManager.createTabContainer(bankAccount, resolvedTabId);
        registerBankListener();
    }

    private void registerInventoryListeners(@Nonnull Inventory inventory) {
        if (containerRegistrations.size() > 1) {
            return;
        }

        containerRegistrations.add(inventory.getStorage().registerChangeEvent(event -> sendGridUpdate()));
        containerRegistrations.add(inventory.getHotbar().registerChangeEvent(event -> sendGridUpdate()));
    }

    private void registerBankListener() {
        if (bankContainer != null) {
            containerRegistrations.add(bankContainer.registerChangeEvent(event -> {
                persistCurrentContainer();
                sendGridUpdate();
            }));
        }
    }

    private void unregisterBankListener() {
        if (containerRegistrations.size() < 3) {
            return;
        }

        EventRegistration bankRegistration = containerRegistrations.remove(containerRegistrations.size() - 1);
        bankRegistration.unregister();
    }

    private void unregisterListeners() {
        for (EventRegistration registration : containerRegistrations) {
            registration.unregister();
        }
        containerRegistrations.clear();
    }

    private void persistCurrentContainer() {
        BankAccount bankAccount = bankManager.getBank(bankId);
        if (bankAccount == null || bankContainer == null || currentTabId == null) {
            return;
        }

        bankManager.saveTabContainer(bankAccount, currentTabId, bankContainer);
    }

    private void sendGridUpdate() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        BankAccount bankAccount = bankManager.getBank(bankId);
        if (player == null || bankAccount == null) {
            return;
        }

        sendFullUpdate(player, bankAccount);
    }

    private void sendFullUpdate(@Nonnull Player player, @Nonnull BankAccount bankAccount) {
        sendFullUpdate(player, bankAccount, null);
    }

    private void sendFullUpdate(@Nonnull Player player,
            @Nonnull BankAccount bankAccount,
            @Nullable Message statusMessage) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, bankAccount, player.getInventory(), statusMessage);
        UIEventBuilder eventBuilder = new UIEventBuilder();
        bindEvents(eventBuilder);
        player.getPageManager().updateCustomPage(
                new CustomPage(
                        getClass().getName(),
                        false,
                        false,
                        getLifetime(),
                        commandBuilder.getCommands(),
                        eventBuilder.getEvents()));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull BankAccount bankAccount,
            @Nonnull Inventory inventory,
            @Nullable Message statusMessage) {
        BankTypeDefinition definition = bankManager.getDefinition(bankAccount);
        if (definition == null || currentTabId == null) {
            commandBuilder.set("#TitleLabel.Text", bankAccount.getDisplayName());
            commandBuilder.set("#StatusLabel.Text", "No bank tabs available.");
            return;
        }

        BankTypeDefinition.BankTabDefinition currentTab = bankManager.getTabDefinition(bankAccount, currentTabId);
        String tabName = currentTab == null ? currentTabId : currentTab.getDisplayName();

        commandBuilder.set("#TitleLabel.Text", bankAccount.getDisplayName());
    if (statusMessage != null) {
        commandBuilder.setObject("#StatusLabel.Text", toLocalizableString(statusMessage));
    } else {
        commandBuilder.set("#StatusLabel.Text",
            definition.getDisplayName() + " • " + tabName + " • Right-click moves one item.");
    }
        commandBuilder.set("#BankSummaryLabel.Text",
                bankAccount.getOwnerScope().name() + " • " + bankAccount.getBankTypeId() + " • tabs="
                        + bankManager.getAvailableTabs(bankAccount).size());

        populateTabButtons(commandBuilder, bankAccount, currentTabId);
        populateUnlockControls(commandBuilder, bankAccount);
        populateCurrencyControls(commandBuilder, bankAccount, inventory);
        populateGrid(commandBuilder, BANK_GRID_SELECTOR, bankContainer);
        populateGrid(commandBuilder, PLAYER_STORAGE_SELECTOR, inventory.getStorage());
        populateGrid(commandBuilder, PLAYER_HOTBAR_SELECTOR, inventory.getHotbar());
    }

        private void populateCurrencyControls(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull BankAccount bankAccount,
            @Nonnull Inventory inventory) {
        if (!bankManager.supportsCurrencyStorage(bankAccount)) {
            commandBuilder.set(CURRENCY_SUMMARY_LABEL + ".Text",
                translateRaw("pixelbays.rpg.bank.storage.currency.unavailable"));
            commandBuilder.set(DEPOSIT_CURRENCY_BUTTON + ".Text",
                translateRaw("pixelbays.rpg.bank.storage.currency.depositButton"));
            commandBuilder.set(WITHDRAW_CURRENCY_BUTTON + ".Text",
                translateRaw("pixelbays.rpg.bank.storage.currency.withdrawButton"));
            return;
        }

        List<CurrencyTypeDefinition> supported = bankManager.getSupportedStoredCurrencies(bankAccount);
        if (supported.isEmpty()) {
            commandBuilder.set(CURRENCY_SUMMARY_LABEL + ".Text",
                translateRaw("pixelbays.rpg.bank.storage.currency.none"));
        } else {
            StringBuilder builder = new StringBuilder();
            for (CurrencyTypeDefinition definition : supported) {
            if (builder.length() > 0) {
                builder.append('\n');
            }

            long stored = bankManager.getStoredCurrencyBalance(bankAccount, definition.getId());
            long carried = bankManager.getSourceCurrencyBalance(bankAccount, playerRef.getUuid(), inventory, definition.getId());
            builder.append(definition.getDisplayName())
                .append(": bank=")
                .append(stored)
                .append(" | available=")
                .append(carried)
                .append(" [")
                .append(definition.getId())
                .append(']');
            }
            commandBuilder.set(CURRENCY_SUMMARY_LABEL + ".Text", builder.toString());
        }

        commandBuilder.set(DEPOSIT_CURRENCY_BUTTON + ".Text",
            translateRaw("pixelbays.rpg.bank.storage.currency.depositButton"));
        commandBuilder.set(WITHDRAW_CURRENCY_BUTTON + ".Text",
            translateRaw("pixelbays.rpg.bank.storage.currency.withdrawButton"));
        }

    private void populateUnlockControls(@Nonnull UICommandBuilder commandBuilder,
        @Nonnull BankAccount bankAccount) {
    BankTypeDefinition.BankTabDefinition nextLockedTab = bankManager.getNextLockedTab(bankAccount);
    if (nextLockedTab == null) {
        commandBuilder.set(NEXT_TAB_COST_LABEL + ".Text", translateRaw("pixelbays.rpg.bank.storage.unlock.none"));
        commandBuilder.set(UNLOCK_NEXT_TAB_BUTTON + ".Text",
            translateRaw("pixelbays.rpg.bank.storage.unlock.unavailableButton"));
        return;
    }

    BankTypeDefinition.BankCostDefinition cost = bankManager.getNextTabUnlockCost(bankAccount);
    String costText = cost.isFree()
        ? translateRaw("pixelbays.rpg.bank.storage.unlock.freeCost")
        : bankManager.describeCost(cost);
    commandBuilder.set(NEXT_TAB_COST_LABEL + ".Text",
        translateRaw("pixelbays.rpg.bank.storage.unlock.cost", "tab", nextLockedTab.getDisplayName(), "cost",
            costText));
    commandBuilder.set(UNLOCK_NEXT_TAB_BUTTON + ".Text",
        translateRaw("pixelbays.rpg.bank.storage.unlock.button", "tab", nextLockedTab.getDisplayName()));
    }

    private void populateTabButtons(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull BankAccount bankAccount,
            @Nonnull String selectedTabId) {
        List<BankTypeDefinition.BankTabDefinition> tabs = bankManager.getAvailableTabs(bankAccount);
        for (int i = 0; i < TAB_BUTTON_SELECTORS.length; i++) {
            String selector = TAB_BUTTON_SELECTORS[i];
            if (i < tabs.size()) {
                BankTypeDefinition.BankTabDefinition tab = tabs.get(i);
                String label = tab.getDisplayName();
                if (tab.getId().equalsIgnoreCase(selectedTabId)) {
                    label = "[" + label + "]";
                }
                commandBuilder.set(selector + ".Text", label);
            } else {
                commandBuilder.set(selector + ".Text", "-");
            }
        }
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                BANK_GRID_SELECTOR,
                new EventData().append("Grid", "Bank").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                PLAYER_STORAGE_SELECTOR,
                new EventData().append("Grid", "PlayerStorage").append("Event", "SlotClicking"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                PLAYER_HOTBAR_SELECTOR,
                new EventData().append("Grid", "PlayerHotbar").append("Event", "SlotClicking"));

        for (int i = 0; i < TAB_BUTTON_SELECTORS.length; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    TAB_BUTTON_SELECTORS[i],
                    new EventData().append("Action", "SelectTab").append("TabIndex", String.valueOf(i)));
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            UNLOCK_NEXT_TAB_BUTTON,
            new EventData().append("Action", "UnlockNextTab"));

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            DEPOSIT_CURRENCY_BUTTON,
            new EventData().append("Action", "DepositCurrency")
                .append("@CurrencyId", CURRENCY_ID_FIELD + ".Value")
                .append("@CurrencyAmount", CURRENCY_AMOUNT_FIELD + ".Value"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            WITHDRAW_CURRENCY_BUTTON,
            new EventData().append("Action", "WithdrawCurrency")
                .append("@CurrencyId", CURRENCY_ID_FIELD + ".Value")
                .append("@CurrencyAmount", CURRENCY_AMOUNT_FIELD + ".Value"));
    }

    private void populateGrid(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull String selector,
            @Nullable ItemContainer container) {
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

    @Nonnull
    private String translateRaw(@Nonnull String messageId, String... kvPairs) {
        Message message = Message.translation(messageId);
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            message = message.param(kvPairs[i], kvPairs[i + 1]);
        }

        var formatted = message.getFormattedMessage();
        return formatted.rawText != null ? formatted.rawText : "";
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();
        if (formatted.messageId != null) {
            java.util.Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new java.util.HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    params.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }
        return LocalizableString.fromString(formatted.rawText != null ? formatted.rawText : "");
    }

    @Nullable
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

    @Nullable
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

    private static long parseLong(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
