package org.pixelbays.rpg.economy.banks.ui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.banks.BankTypeRegistry;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class BankSelectionPage extends CustomUIPage {

    private static final int MAX_OPTIONS = 6;
    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String MENU_TITLE_LABEL = "#MenuTitleLabel";
    private static final String MENU_SUBTITLE_LABEL = "#MenuSubtitleLabel";

    private final List<String> bankTypeIds;
    private final String professionId;
    private final String customOwnerId;
    private final String title;

    public BankSelectionPage(@Nonnull PlayerRef playerRef,
            @Nonnull List<String> bankTypeIds,
            @Nullable String professionId,
            @Nullable String customOwnerId,
            @Nullable String title) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.bankTypeIds = List.copyOf(bankTypeIds);
        this.professionId = professionId;
        this.customOwnerId = customOwnerId;
        this.title = title;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/BankSelectionPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        String indexValue = extractString(rawData, "BankIndex");
        if (action == null || !"SelectBank".equals(action) || indexValue == null) {
            return;
        }

        int index;
        try {
            index = Integer.parseInt(indexValue);
        } catch (NumberFormatException ex) {
            return;
        }

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleSelection(ref, store, index));
    }

    private void handleSelection(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            int index) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || index < 0 || index >= bankTypeIds.size()) {
            return;
        }

        boolean opened = BankUiOpener.openBankType(
                ref,
                store,
                player,
                playerRef,
                bankTypeIds.get(index),
                professionId,
                customOwnerId,
                false);
        if (opened) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, "Unable to open selected bank.");
        sendUpdate(commandBuilder);
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        for (int i = 0; i < MAX_OPTIONS; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#OptionButton" + i,
                    new EventData().append("Action", "SelectBank").append("BankIndex", String.valueOf(i)));
        }
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder, @Nullable String statusText) {
        commandBuilder.set(MENU_TITLE_LABEL + ".Text",
                title == null || title.isBlank() ? "Available Bank Services" : title);
        commandBuilder.set(MENU_SUBTITLE_LABEL + ".Text", "Choose which bank service to open.");
        commandBuilder.set(STATUS_LABEL + ".Text", statusText == null ? "" : statusText);

        for (int i = 0; i < MAX_OPTIONS; i++) {
            String buttonSelector = "#OptionButton" + i;
            String titleSelector = "#OptionTitle" + i;
            String descSelector = "#OptionDescription" + i;
            if (i < bankTypeIds.size()) {
                BankTypeDefinition definition = BankTypeRegistry.get(bankTypeIds.get(i));
                if (definition != null) {
                    commandBuilder.set(buttonSelector + ".Text", definition.getDisplayName());
                    commandBuilder.set(titleSelector + ".Text", definition.getScope().name());
                    commandBuilder.set(descSelector + ".Text", descriptionFor(definition));
                    continue;
                }
                commandBuilder.set(buttonSelector + ".Text", bankTypeIds.get(i));
                commandBuilder.set(titleSelector + ".Text", "Unknown");
                commandBuilder.set(descSelector + ".Text", "This bank type could not be resolved.");
                continue;
            }

            commandBuilder.set(buttonSelector + ".Text", "");
            commandBuilder.set(titleSelector + ".Text", "");
            commandBuilder.set(descSelector + ".Text", "");
        }
    }

    @Nonnull
    private String descriptionFor(@Nonnull BankTypeDefinition definition) {
        String description = definition.getDescription();
        if (description == null || description.isBlank()) {
            return "Scope: " + definition.getScope().name();
        }
        return description;
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
}