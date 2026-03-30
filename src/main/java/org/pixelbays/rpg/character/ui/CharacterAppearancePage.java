package org.pixelbays.rpg.character.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterActionResult;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog.Category;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog.Cosmetic;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog.Option;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog.ParsedSelection;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceData;
import org.pixelbays.rpg.character.config.CharacterProfileData;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings.BarberPricingMode;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings.BarberShopSettings;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.expansion.ExpansionManager;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class CharacterAppearancePage extends InteractiveCustomUIPage<CharacterAppearancePage.PageEventData> {

    private static final String BRAND_TITLE_LABEL = "#BrandTitleLabel";
    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String MODE_LABEL = "#ModeLabel";
    private static final String COST_LABEL = "#CostLabel";
    private static final String SUMMARY_LABEL = "#SummaryLabel";
    private static final String CATEGORY_HEADING = "#CategoryHeadingLabel";
    private static final String NAME_FIELD = "#NameField";
    private static final String SEARCH_FIELD = "#SearchField";
    private static final String CATEGORY_RAIL = "#CategoryRail";
    private static final String COSMETIC_GRID = "#CosmeticGrid";
    private static final String COLOR_SECTION = "#ColorSection";
    private static final String COLOR_LIST = "#ColorList";
    private static final String VARIANT_SECTION = "#VariantSection";
    private static final String VARIANT_LIST = "#VariantList";
    private static final String RACE_SELECTION_LABEL = "#RaceSelectionLabel";
    private static final String CLASS_SELECTION_LABEL = "#ClassSelectionLabel";
    private static final String HARDCORE_SECTION = "#HardcoreSection";
    private static final String HARDCORE_CHECK = "#HardcoreCheck";
    private static final String RACE_LIST = "#RaceList";
    private static final String CLASS_LIST = "#ClassList";
    private static final String RANDOMIZE_BUTTON = "#RandomizeButton";
    private static final String ROTATE_LEFT_BUTTON = "#RotateLeftButton";
    private static final String ROTATE_RIGHT_BUTTON = "#RotateRightButton";
    private static final String CONFIRM_BUTTON = "#ConfirmButton";
    private static final String BACK_BUTTON = "#BackButton";
    private static final String CREATE_CONFIRM_OVERLAY = "#CreateConfirmOverlay";
    private static final String CREATE_CONFIRM_MESSAGE = "#CreateConfirmMessage";
    private static final String CREATE_CONFIRM_ACCEPT_BUTTON = "#CreateConfirmAcceptButton";
    private static final String CREATE_CONFIRM_CANCEL_BUTTON = "#CreateConfirmCancelButton";

    private final CharacterManager characterManager;
    private final Mode mode;

    private boolean initialized;
    private String draftName = "";
    private String draftRaceId = "";
    private String draftClassId = "";
    private String searchQuery = "";
    private String statusText = "";
    private String restoreRaceId = "";
    private boolean confirmDialogOpen;
    private boolean restoreAppearanceOnDismiss = true;
    private boolean draftHardcore;
    private Category selectedCategory = Category.BODY_CHARACTERISTIC;
    private String selectedCosmeticAssetId = "";
    private String selectedColorId = "";
    private String selectedVariantId = "";
    private CharacterAppearanceData draftAppearance = new CharacterAppearanceData();
    private CharacterAppearanceData persistedAppearance = new CharacterAppearanceData();
    private PlayerSkin restoreSkin = new PlayerSkin();

    public CharacterAppearancePage(@Nonnull PlayerRef playerRef, @Nonnull Mode mode) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.characterManager = Realmweavers.get().getCharacterManager();
        this.mode = mode;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        ensureInitialized(ref, store);
        commandBuilder.append("Pages/CharacterAppearancePage.ui");
        bindStaticEvents(eventBuilder);
        appendView(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData data) {
        ensureInitialized(ref, store);
        boolean shouldRefresh = true;
        if (data.name != null) {
            draftName = data.name.trim();
            invalidateCreateConfirmation();
        }

        if (data.searchQuery != null) {
            searchQuery = data.searchQuery.trim();
            invalidateCreateConfirmation();
        }

        if (data.hardcore != null) {
            draftHardcore = characterManager.isHardcoreCharacterCreationEnabled() && data.hardcore;
            invalidateCreateConfirmation();
        }

        String type = data.type == null ? "" : data.type;
        RpgLogging.debugDeveloper("[CharacterAppearancePage] dataEvent type=%s category=%s option=%s name=%s search=%s",
            type,
            data.categoryId,
            data.optionId,
            data.name,
            data.searchQuery);
        switch (type) {
            case "SelectCategory" -> {
                Category category = CharacterAppearanceCatalog.findCategory(data.categoryId);
                if (category != null) {
                    selectedCategory = category;
                    selectedCosmeticAssetId = "";
                    selectedColorId = "";
                    selectedVariantId = "";
                    invalidateCreateConfirmation();
                }
            }
            case "SelectRace" -> {
                String selectedRaceId = data.optionId == null ? "" : data.optionId.trim();
                if (mode == Mode.CREATE && isSelectableRaceId(selectedRaceId)
                        && !selectedRaceId.equalsIgnoreCase(draftRaceId)) {
                    RpgLogging.debugDeveloper(
                            "[CharacterAppearancePage] selectRace old=%s new=%s currentOption=%s",
                            draftRaceId,
                            selectedRaceId,
                            draftAppearance.getValue(selectedCategory.fieldName()));
                    invalidateCreateConfirmation();
                    draftRaceId = selectedRaceId;
                    draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
                    characterManager.applyAppearanceToPlayer(ref, store, draftRaceId, draftAppearance);
                }
            }
            case "SelectClass" -> {
                String selectedClassId = data.optionId == null ? "" : data.optionId.trim();
                if (mode == Mode.CREATE && isSelectableClassId(selectedClassId)) {
                    invalidateCreateConfirmation();
                    draftClassId = selectedClassId;
                }
            }
            case "SelectOption" -> {
                invalidateCreateConfirmation();
                RpgLogging.debugDeveloper(
                        "[CharacterAppearancePage] selectOption category=%s old=%s new=%s race=%s",
                        selectedCategory.fieldName(),
                        draftAppearance.getValue(selectedCategory.fieldName()),
                        data.optionId,
                        draftRaceId);
                draftAppearance.setValue(selectedCategory.fieldName(), data.optionId);
                draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
                syncDraftPreview(ref, store);
            }
            case "SelectCosmetic" -> {
                invalidateCreateConfirmation();
                applyCosmeticSelection(ref, store, data.cosmeticId);
            }
            case "SelectColor" -> {
                invalidateCreateConfirmation();
                applyColorSelection(ref, store, data.colorId);
            }
            case "SelectVariant" -> {
                invalidateCreateConfirmation();
                applyVariantSelection(ref, store, data.variantId);
            }
            case "ClearOption" -> {
                invalidateCreateConfirmation();
                draftAppearance.setValue(selectedCategory.fieldName(), "");
                draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
                selectedCosmeticAssetId = "";
                selectedColorId = "";
                selectedVariantId = "";
                syncDraftPreview(ref, store);
            }
            case "Randomize" -> {
                invalidateCreateConfirmation();
                draftAppearance = characterManager.createDefaultAppearance(draftRaceId);
                selectedCosmeticAssetId = "";
                selectedColorId = "";
                selectedVariantId = "";
                syncDraftPreview(ref, store);
            }
            case "RotateLeft" -> characterManager.rotateCharacterPreviewLeft(ref, store);
            case "RotateRight" -> characterManager.rotateCharacterPreviewRight(ref, store);
            case "CancelCreateConfirm" -> {
                confirmDialogOpen = false;
                statusText = "";
            }
            case "ConfirmCreateAccept" -> shouldRefresh = handleConfirmedCreate(ref, store);
            case "Back" -> shouldRefresh = handleBack(ref, store);
            case "Confirm" -> shouldRefresh = handleConfirm(ref, store);
            default -> {
            }
        }

        if (shouldRefresh) {
            refresh();
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        characterManager.resetCharacterAppearancePreviewCamera(playerRef);
        if (restoreAppearanceOnDismiss) {
            characterManager.applyAppearanceToPlayer(ref, store, restoreRaceId,
                    CharacterAppearanceData.fromPlayerSkin(new PlayerSkin(restoreSkin)));
            characterManager.ensureCharacterSelectionUiOpen(ref, store, playerRef);
        }
    }

    private void ensureInitialized(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (initialized) {
            return;
        }

        PlayerSkinComponent playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (playerSkinComponent != null) {
            restoreSkin = new PlayerSkin(playerSkinComponent.getPlayerSkin());
        }

        CharacterProfileData activeProfile = characterManager.getActiveProfile(playerRef.getUuid(), playerRef.getUsername());
        if (activeProfile != null) {
            restoreRaceId = activeProfile.getRaceId();
        }

        if (mode == Mode.BARBER && activeProfile != null) {
            draftName = activeProfile.getCharacterName();
            draftRaceId = activeProfile.getRaceId();
            draftClassId = activeProfile.getPrimaryClassId();
            persistedAppearance = activeProfile.getAppearance().copy();
            draftAppearance = persistedAppearance.copy();
        } else {
            if (mode == Mode.CREATE) {
                draftRaceId = resolveInitialRaceId(draftRaceId);
                draftClassId = resolveInitialClassId(draftClassId);
            }
            draftAppearance = characterManager.createDefaultAppearance(draftRaceId);
            persistedAppearance = CharacterAppearanceData.fromPlayerSkin(restoreSkin);
        }

        syncDraftPreview(ref, store);
        characterManager.resetCharacterPreviewRotation(ref, store);

        initialized = true;
    }

    private boolean handleBack(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (confirmDialogOpen) {
            confirmDialogOpen = false;
            statusText = "";
            return true;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return false;
        }
        if (mode == Mode.CREATE) {
            characterManager.enterCharacterSelect(ref, store, playerRef);
            return false;
        }
        player.getPageManager().setPage(ref, store, Page.None);
        return false;
    }

    private boolean handleConfirm(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        CharacterActionResult result;
        if (mode == Mode.CREATE) {
            confirmDialogOpen = true;
            statusText = "";
            return true;
        }

        result = characterManager.applyBarberAppearance(ref, store, playerRef, draftAppearance);
        statusText = result.getMessage();
        if (result.isSuccess()) {
            persistedAppearance = draftAppearance.copy();
            restoreSkin = persistedAppearance.toPlayerSkin();
            playerRef.sendMessage(characterManager.mapMessage(result.getMessage()));
        }
        return true;
    }

    private boolean handleConfirmedCreate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        CharacterActionResult result = characterManager.createCharacter(ref, store, playerRef, draftName, draftRaceId,
            draftClassId, draftAppearance, draftHardcore);
        confirmDialogOpen = false;
        statusText = result.getMessage();
        if (result.isSuccess() && result.getProfile() != null) {
            restoreAppearanceOnDismiss = false;
            playerRef.sendMessage(characterManager.mapMessage(result.getMessage()));
            CharacterActionResult selectResult = characterManager.selectCharacter(ref, store, playerRef,
                    result.getProfile().getCharacterId());
            if (selectResult.isSuccess()) {
                return false;
            }
            restoreAppearanceOnDismiss = true;
            statusText = selectResult.getMessage();
        }
        return true;
    }

    private void refresh() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid() && ref.getStore() != null) {
            syncDraftPreview(ref, ref.getStore());
        }
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        appendView(commandBuilder, eventBuilder);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void syncDraftPreview(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        RpgLogging.debugDeveloper(
                "[CharacterAppearancePage] syncDraftPreview race=%s category=%s appliedValue=%s",
                draftRaceId,
                selectedCategory.fieldName(),
                draftAppearance.getValue(selectedCategory.fieldName()));
        characterManager.applyCharacterPreview(ref, store, playerRef, draftRaceId, draftAppearance);
    }

    private void applyCosmeticSelection(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isBlank()) {
            return;
        }
        Cosmetic cosmetic = findCosmetic(
                characterManager.getAppearanceService().getAllowedCosmetics(
                        characterManager.getAppearanceService().resolveRace(draftRaceId),
                        selectedCategory,
                        searchQuery),
                cosmeticId);
        if (cosmetic == null) {
            return;
        }

        String optionId = cosmetic.resolveOptionId(selectedColorId, selectedVariantId);
        draftAppearance.setValue(selectedCategory.fieldName(), optionId);
        draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
        syncDraftPreview(ref, store);
    }

    private void applyColorSelection(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String colorId) {
        if (selectedCosmeticAssetId.isBlank() || colorId == null || colorId.isBlank()) {
            return;
        }
        Cosmetic cosmetic = findCosmetic(
                characterManager.getAppearanceService().getAllowedCosmetics(
                        characterManager.getAppearanceService().resolveRace(draftRaceId),
                        selectedCategory,
                        searchQuery),
                selectedCosmeticAssetId);
        if (cosmetic == null) {
            return;
        }

        String optionId = cosmetic.resolveOptionId(colorId, selectedVariantId);
        draftAppearance.setValue(selectedCategory.fieldName(), optionId);
        draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
        syncDraftPreview(ref, store);
    }

    private void applyVariantSelection(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String variantId) {
        if (selectedCosmeticAssetId.isBlank() || variantId == null || variantId.isBlank()) {
            return;
        }
        Cosmetic cosmetic = findCosmetic(
                characterManager.getAppearanceService().getAllowedCosmetics(
                        characterManager.getAppearanceService().resolveRace(draftRaceId),
                        selectedCategory,
                        searchQuery),
                selectedCosmeticAssetId);
        if (cosmetic == null) {
            return;
        }

        String optionId = cosmetic.resolveOptionId(selectedColorId, variantId);
        draftAppearance.setValue(selectedCategory.fieldName(), optionId);
        draftAppearance = characterManager.sanitizeAppearance(draftRaceId, draftAppearance);
        syncDraftPreview(ref, store);
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder) {
        RaceDefinition raceDefinition = characterManager.getAppearanceService().resolveRace(draftRaceId);
        List<Cosmetic> cosmetics = characterManager.getAppearanceService().getAllowedCosmetics(
                raceDefinition,
                selectedCategory,
                searchQuery);
        syncSelectedCosmeticState(cosmetics);
        Cosmetic selectedCosmetic = findCosmetic(cosmetics, selectedCosmeticAssetId);
        List<ChoiceEntry> raceEntries = buildRaceEntries();
        List<ChoiceEntry> classEntries = buildClassEntries();
        String raceSummary = resolveSelectedChoiceSummaryValue(raceEntries, draftRaceId);
        String classSummary = resolveSelectedChoiceSummaryValue(classEntries, draftClassId);

        commandBuilder.set(BRAND_TITLE_LABEL + ".Text",
            "Realmweavers: " + characterManager.resolveServerName());
        commandBuilder.set(MODE_LABEL + ".Text",
            mode == Mode.CREATE ? "Create New Character" : "Barber Shop");
        commandBuilder.set(NAME_FIELD + ".Value", draftName);
        commandBuilder.set(SEARCH_FIELD + ".Value", searchQuery);
        commandBuilder.set(STATUS_LABEL + ".Text", statusText);
        commandBuilder.set(CATEGORY_HEADING + ".Text", humanizeCategory(selectedCategory));
        commandBuilder.set(SUMMARY_LABEL + ".Text", buildSummary(raceSummary, classSummary));
        commandBuilder.set(COST_LABEL + ".Text", buildCostLabel());
        commandBuilder.set(CREATE_CONFIRM_OVERLAY + ".Visible", confirmDialogOpen);
        commandBuilder.set(CREATE_CONFIRM_MESSAGE + ".Text",
            "Create " + (draftName.isBlank() ? "this character" : draftName)
            + (draftHardcore ? " as a hardcore character and enter the world?" : " and enter the world?"));
        commandBuilder.set(RACE_SELECTION_LABEL + ".Text",
                buildSelectedChoiceLabel(raceEntries, draftRaceId, mode == Mode.CREATE ? "Select a race." : "-"));
        commandBuilder.set(CLASS_SELECTION_LABEL + ".Text",
                buildSelectedChoiceLabel(classEntries, draftClassId,
                        mode == Mode.CREATE ? "Select a class." : "-"));
        commandBuilder.set(HARDCORE_SECTION + ".Visible", mode == Mode.CREATE && characterManager.isHardcoreCharacterCreationEnabled());
        commandBuilder.set(HARDCORE_CHECK + ".Value", draftHardcore);
        commandBuilder.set(CONFIRM_BUTTON + ".Text",
            mode == Mode.CREATE ? "Create Character" : "Apply Changes");

        commandBuilder.clear(CATEGORY_RAIL);
        for (int index = 0; index < CharacterAppearanceCatalog.getOrderedCategories().size(); index++) {
            Category category = CharacterAppearanceCatalog.getOrderedCategories().get(index);
            String selector = CATEGORY_RAIL + "[" + index + "]";
            commandBuilder.append(CATEGORY_RAIL, "Common/AppearanceCategoryRailButton.ui");
            commandBuilder.set(selector + " #GlyphLabel.Text", railGlyph(category));
            commandBuilder.set(selector + " #TitleLabel.Text", railLabel(category));
            commandBuilder.set(selector + " #SelectedBar.Visible", category == selectedCategory);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData().append("Type", "SelectCategory").append("CategoryId", category.fieldName()),
                    false);
        }

        appendChoiceList(commandBuilder, eventBuilder, RACE_LIST, raceEntries, draftRaceId, "SelectRace");
        appendChoiceList(commandBuilder, eventBuilder, CLASS_LIST, classEntries, draftClassId, "SelectClass");

        appendCosmeticGrid(commandBuilder, eventBuilder, cosmetics);
        appendColorChoices(commandBuilder, eventBuilder, selectedCosmetic);
        appendVariantChoices(commandBuilder, eventBuilder, selectedCosmetic);
    }

    private void appendCosmeticGrid(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull List<Cosmetic> cosmetics) {
        commandBuilder.clear(COSMETIC_GRID);

        int itemIndex = 0;
        if (!selectedCategory.required()) {
            String selector = appendTile(commandBuilder, itemIndex++);
            boolean selected = draftAppearance.getValue(selectedCategory.fieldName()).isBlank();
            commandBuilder.set(selector + " #TitleLabel.Text", "None");
            commandBuilder.set(selector + " #SubtitleLabel.Text", "Clear this slot");
            commandBuilder.set(selector + " #SelectedBadge.Visible", selected);
            commandBuilder.set(selector + " #PreviewFrame.Visible", true);
            commandBuilder.set(selector + " #PreviewLabel.Text", "NO");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData().append("Type", "ClearOption"),
                    false);
        }

        if (cosmetics.isEmpty()) {
            String selector = appendTile(commandBuilder, itemIndex);
            commandBuilder.set(selector + " #TitleLabel.Text", "No Matches");
            commandBuilder.set(selector + " #SubtitleLabel.Text", "Try a different search");
            commandBuilder.set(selector + " #SelectedBadge.Visible", false);
            commandBuilder.set(selector + " #PreviewFrame.Visible", true);
            commandBuilder.set(selector + " #PreviewLabel.Text", "--");
            return;
        }

        for (Cosmetic cosmetic : cosmetics) {
            String selector = appendTile(commandBuilder, itemIndex++);
            boolean selected = cosmetic.assetId().equalsIgnoreCase(selectedCosmeticAssetId);
            commandBuilder.set(selector + " #TitleLabel.Text", cosmetic.label());
            commandBuilder.set(selector + " #SubtitleLabel.Text", buildCosmeticSubtitle(cosmetic));
            commandBuilder.set(selector + " #SelectedBadge.Visible", selected);
            commandBuilder.set(selector + " #PreviewFrame.Visible", true);
            commandBuilder.set(selector + " #PreviewLabel.Text", buildCosmeticPreviewToken(cosmetic));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData().append("Type", "SelectCosmetic").append("CosmeticId", cosmetic.assetId()),
                    false);
        }
    }

    @Nonnull
    private String appendTile(@Nonnull UICommandBuilder commandBuilder, int itemIndex) {
        int row = itemIndex / 3;
        int column = itemIndex % 3;
        String rowSelector = COSMETIC_GRID + "[" + row + "]";
        if (column == 0) {
            commandBuilder.append(COSMETIC_GRID, "Common/AppearanceTileRow.ui");
        }
        commandBuilder.append(rowSelector + " #Cells", "Common/AppearanceCosmeticTile.ui");
        return rowSelector + " #Cells[" + column + "]";
    }

    private void appendColorChoices(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nullable Cosmetic selectedCosmetic) {
        boolean visible = selectedCosmetic != null && selectedCosmetic.supportsColorSelection();
        commandBuilder.set(COLOR_SECTION + ".Visible", visible);
        commandBuilder.clear(COLOR_LIST);
        if (!visible || selectedCosmetic == null) {
            return;
        }

        for (int index = 0; index < selectedCosmetic.colors().size(); index++) {
            CharacterAppearanceCatalog.TextureChoice color = selectedCosmetic.colors().get(index);
            String selector = appendChoiceChip(commandBuilder, COLOR_LIST, index);
            commandBuilder.set(selector + " #Label.Text", color.label());
            commandBuilder.set(selector + " #SelectedBadge.Visible", color.id().equalsIgnoreCase(selectedColorId));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData().append("Type", "SelectColor").append("ColorId", color.id()),
                    false);
        }
    }

    private void appendVariantChoices(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nullable Cosmetic selectedCosmetic) {
        boolean visible = selectedCosmetic != null && selectedCosmetic.supportsVariantSelection();
        commandBuilder.set(VARIANT_SECTION + ".Visible", visible);
        commandBuilder.clear(VARIANT_LIST);
        if (!visible || selectedCosmetic == null) {
            return;
        }

        for (int index = 0; index < selectedCosmetic.variants().size(); index++) {
            CharacterAppearanceCatalog.VariantChoice variant = selectedCosmetic.variants().get(index);
            String selector = appendChoiceChip(commandBuilder, VARIANT_LIST, index);
            commandBuilder.set(selector + " #Label.Text", variant.label());
            commandBuilder.set(selector + " #SelectedBadge.Visible", variant.id().equalsIgnoreCase(selectedVariantId));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    selector + " #Button",
                    new EventData().append("Type", "SelectVariant").append("VariantId", variant.id()),
                    false);
        }
    }

    @Nonnull
    private String appendChoiceChip(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull String listSelector,
            int itemIndex) {
        int row = itemIndex / 3;
        int column = itemIndex % 3;
        String rowSelector = listSelector + "[" + row + "]";
        if (column == 0) {
            commandBuilder.append(listSelector, "Common/AppearanceChoiceRow.ui");
        }
        commandBuilder.append(rowSelector + " #Cells", "Common/AppearanceChoiceChip.ui");
        return rowSelector + " #Cells[" + column + "]";
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,
                NAME_FIELD,
                new EventData().append("Type", "UpdateFields").append("@Name", NAME_FIELD + ".Value"),
                false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,
                SEARCH_FIELD,
                new EventData().append("Type", "UpdateSearch").append("@SearchQuery", SEARCH_FIELD + ".Value"),
                false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,
            HARDCORE_CHECK,
            new EventData().append("Type", "UpdateHardcore").append("@Hardcore", HARDCORE_CHECK + ".Value"),
            false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
            ROTATE_LEFT_BUTTON,
            new EventData().append("Type", "RotateLeft"),
            false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
            ROTATE_RIGHT_BUTTON,
            new EventData().append("Type", "RotateRight"),
            false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                RANDOMIZE_BUTTON,
                new EventData().append("Type", "Randomize"),
                false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                CONFIRM_BUTTON,
                new EventData()
                        .append("Type", "Confirm")
                        .append("@Name", NAME_FIELD + ".Value"),
                false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                BACK_BUTTON,
                new EventData().append("Type", "Back"),
                false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
            CREATE_CONFIRM_ACCEPT_BUTTON,
            new EventData()
                .append("Type", "ConfirmCreateAccept")
                .append("@Name", NAME_FIELD + ".Value"),
            false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
            CREATE_CONFIRM_CANCEL_BUTTON,
            new EventData().append("Type", "CancelCreateConfirm"),
            false);
    }

    @Nonnull
    private String buildSummary(@Nonnull String raceLabel, @Nonnull String classLabel) {
        return "Name: " + (draftName.isBlank() ? "-" : draftName)
                + "\nRace: " + raceLabel
                + "\nClass: " + classLabel
                + (mode == Mode.CREATE && characterManager.isHardcoreCharacterCreationEnabled()
                    ? "\nHardcore: " + (draftHardcore ? "Enabled" : "Disabled")
                    : "")
                + "\nChanged Slots: " + draftAppearance.countDifferences(persistedAppearance);
    }

    private void invalidateCreateConfirmation() {
        if (mode == Mode.CREATE) {
            confirmDialogOpen = false;
        }
    }

    private void syncSelectedCosmeticState(@Nonnull List<Cosmetic> cosmetics) {
        ParsedSelection parsedSelection = CharacterAppearanceCatalog.parseOptionId(
                draftAppearance.getValue(selectedCategory.fieldName()));
        Cosmetic selectedCosmetic = findCosmetic(cosmetics, parsedSelection.assetId());
        if (selectedCosmetic == null) {
            selectedCosmeticAssetId = "";
            selectedColorId = "";
            selectedVariantId = "";
            return;
        }

        String resolvedOptionId = selectedCosmetic.resolveOptionId(
                parsedSelection.textureId(),
                parsedSelection.variantId());
        ParsedSelection resolvedSelection = CharacterAppearanceCatalog.parseOptionId(resolvedOptionId);
        selectedCosmeticAssetId = selectedCosmetic.assetId();
        selectedColorId = resolvedSelection.textureId();
        selectedVariantId = resolvedSelection.variantId();
    }

    @Nullable
    private Cosmetic findCosmetic(@Nonnull List<Cosmetic> cosmetics, @Nullable String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        for (Cosmetic cosmetic : cosmetics) {
            if (cosmetic.assetId().equalsIgnoreCase(assetId)) {
                return cosmetic;
            }
        }
        return null;
    }

    @Nonnull
    private String buildCosmeticSubtitle(@Nonnull Cosmetic cosmetic) {
        List<String> descriptors = new ArrayList<>();
        if (cosmetic.supportsColorSelection()) {
            descriptors.add(cosmetic.colors().size() + " colors");
        } else if (!cosmetic.colors().isEmpty()) {
            descriptors.add(cosmetic.colors().getFirst().label());
        }
        if (cosmetic.supportsVariantSelection()) {
            descriptors.add(cosmetic.variants().size() + " options");
        }
        if (descriptors.isEmpty()) {
            return cosmetic.isDefaultAsset() ? "Default cosmetic" : "Cosmetic";
        }
        return String.join(" • ", descriptors);
    }

    @Nonnull
    private String buildCosmeticPreviewToken(@Nonnull Cosmetic cosmetic) {
        String source = cosmetic.label().isBlank() ? cosmetic.assetId() : cosmetic.label();
        String[] tokens = source.trim().split("\\s+");
        StringBuilder builder = new StringBuilder(2);
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (builder.length() >= 2) {
                break;
            }
        }
        if (builder.isEmpty() && !source.isBlank()) {
            builder.append(Character.toUpperCase(source.charAt(0)));
        }
        return builder.isEmpty() ? "--" : builder.toString();
    }

    @Nonnull
    private String railGlyph(@Nonnull Category category) {
        return switch (category) {
            case BODY_CHARACTERISTIC -> "BD";
            case UNDERWEAR -> "UW";
            case FACE -> "FC";
            case EYES -> "EY";
            case EARS -> "ER";
            case MOUTH -> "MT";
            case FACIAL_HAIR -> "FH";
            case HAIRCUT -> "HR";
            case EYEBROWS -> "EB";
            case PANTS -> "PA";
            case OVERPANTS -> "OP";
            case UNDERTOP -> "UT";
            case OVERTOP -> "OT";
            case SHOES -> "SH";
            case HEAD_ACCESSORY -> "HA";
            case FACE_ACCESSORY -> "FA";
            case EAR_ACCESSORY -> "EA";
            case SKIN_FEATURE -> "SF";
            case GLOVES -> "GL";
            case CAPE -> "CP";
        };
    }

    @Nonnull
    private String railLabel(@Nonnull Category category) {
        return switch (category) {
            case BODY_CHARACTERISTIC -> "Body";
            case UNDERWEAR -> "Under";
            case FACE -> "Face";
            case EYES -> "Eyes";
            case EARS -> "Ears";
            case MOUTH -> "Mouth";
            case FACIAL_HAIR -> "Beard";
            case HAIRCUT -> "Hair";
            case EYEBROWS -> "Brow";
            case PANTS -> "Pants";
            case OVERPANTS -> "Layer";
            case UNDERTOP -> "Top";
            case OVERTOP -> "Outer";
            case SHOES -> "Shoes";
            case HEAD_ACCESSORY -> "Head";
            case FACE_ACCESSORY -> "Face";
            case EAR_ACCESSORY -> "Ear";
            case SKIN_FEATURE -> "Skin";
            case GLOVES -> "Hand";
            case CAPE -> "Cape";
        };
    }

    private void appendChoiceList(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull String listSelector,
            @Nonnull List<ChoiceEntry> entries,
            @Nonnull String selectedId,
            @Nonnull String eventType) {
        commandBuilder.clear(listSelector);
        for (int index = 0; index < entries.size(); index++) {
            ChoiceEntry entry = entries.get(index);
            String selector = listSelector + "[" + index + "]";
            commandBuilder.append(listSelector, "Common/TextButton.ui");

            boolean selected = entry.id().equalsIgnoreCase(selectedId);
            StringBuilder text = new StringBuilder();
            if (selected) {
                text.append("[x] ");
            }
            text.append(entry.label());
            if (!entry.detail().isBlank()) {
                text.append(" (").append(entry.detail()).append(')');
            }
            commandBuilder.set(selector + " #Button.Text", text.toString());

            if (entry.selectable()) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        new EventData().append("Type", eventType).append("OptionId", entry.id()),
                        false);
            }
        }
    }

    @Nonnull
    private List<ChoiceEntry> buildRaceEntries() {
        ExpansionManager expansionManager = Realmweavers.get().getExpansionManager();
        List<ChoiceEntry> entries = new ArrayList<>();

        if (mode == Mode.CREATE && !characterManager.getSettings().isRequireRaceOnCreation()) {
            entries.add(new ChoiceEntry("", "No race", true, ""));
        }

        List<RaceDefinition> definitions = new ArrayList<>(Realmweavers.get().getRaceManagementSystem().getRaceDefinitions().values());
        definitions.sort(Comparator.comparing(this::resolveRaceDisplayName, String.CASE_INSENSITIVE_ORDER));

        for (RaceDefinition definition : definitions) {
            if (!isRaceVisibleToPlayers(definition)) {
                continue;
            }

            boolean hasExpansionAccess = expansionManager.hasAccess(playerRef, definition.getRequiredExpansionIds());
            boolean selectable = mode == Mode.CREATE && hasExpansionAccess;
            String detail = "";
            if (!hasExpansionAccess) {
                detail = "Locked: " + expansionManager.describeRequirements(definition.getRequiredExpansionIds());
            }

            entries.add(new ChoiceEntry(definition.getRaceId(), resolveRaceDisplayName(definition), selectable, detail));
        }

        return entries;
    }

    private boolean isRaceVisibleToPlayers(@Nullable RaceDefinition definition) {
        if (definition == null || !definition.isVisible() || !definition.isEnabled()) {
            return false;
        }

        String raceId = definition.getRaceId();
        if (raceId == null || raceId.isBlank()) {
            return false;
        }

        String normalizedRaceId = raceId.trim();
        return !normalizedRaceId.equalsIgnoreCase("base")
                && !normalizedRaceId.equalsIgnoreCase("template");
    }

    @Nonnull
    private List<ChoiceEntry> buildClassEntries() {
        ExpansionManager expansionManager = Realmweavers.get().getExpansionManager();
        List<ChoiceEntry> entries = new ArrayList<>();

        if (mode == Mode.CREATE && !characterManager.getSettings().isRequireStarterClassOnCreation()) {
            entries.add(new ChoiceEntry("", "No starting class", true, ""));
        }

        List<ClassDefinition> definitions = new ArrayList<>(Realmweavers.get().getClassManagementSystem().getAllClassDefinitions().values());
        definitions.sort(Comparator.comparing(this::resolveClassDisplayName, String.CASE_INSENSITIVE_ORDER));

        for (ClassDefinition definition : definitions) {
            if (definition == null || !definition.isVisible() || !definition.isEnabled()) {
                continue;
            }

            boolean hasExpansionAccess = expansionManager.hasAccess(playerRef, definition.getRequiredExpansionIds());
            boolean selectable = mode == Mode.CREATE
                    && definition.isStartingClass()
                    && hasExpansionAccess;

            String detail = "";
            if (!definition.isStartingClass()) {
                detail = "Locked";
            } else if (!hasExpansionAccess) {
                detail = "Locked: " + expansionManager.describeRequirements(definition.getRequiredExpansionIds());
            }

            entries.add(new ChoiceEntry(definition.getId(), resolveClassDisplayName(definition), selectable, detail));
        }

        return entries;
    }

    @Nonnull
    private String resolveInitialRaceId(@Nonnull String preferredRaceId) {
        if (isSelectableRaceId(preferredRaceId)) {
            return preferredRaceId;
        }
        if (!characterManager.getSettings().isRequireRaceOnCreation()) {
            return "";
        }
        return resolveFirstSelectableId(buildRaceEntries());
    }

    @Nonnull
    private String resolveInitialClassId(@Nonnull String preferredClassId) {
        if (isSelectableClassId(preferredClassId)) {
            return preferredClassId;
        }
        if (!characterManager.getSettings().isRequireStarterClassOnCreation()) {
            return "";
        }
        return resolveFirstSelectableId(buildClassEntries());
    }

    private boolean isSelectableRaceId(@Nonnull String raceId) {
        for (ChoiceEntry entry : buildRaceEntries()) {
            if (entry.selectable() && entry.id().equalsIgnoreCase(raceId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSelectableClassId(@Nonnull String classId) {
        for (ChoiceEntry entry : buildClassEntries()) {
            if (entry.selectable() && entry.id().equalsIgnoreCase(classId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private String resolveFirstSelectableId(@Nonnull List<ChoiceEntry> entries) {
        for (ChoiceEntry entry : entries) {
            if (entry.selectable()) {
                return entry.id();
            }
        }
        return "";
    }

    @Nonnull
    private String buildSelectedChoiceLabel(@Nonnull List<ChoiceEntry> entries,
            @Nonnull String selectedId,
            @Nonnull String emptyValue) {
        for (ChoiceEntry entry : entries) {
            if (entry.id().equalsIgnoreCase(selectedId)) {
                if (entry.detail().isBlank()) {
                    return entry.label();
                }
                return entry.label() + " (" + entry.detail() + ")";
            }
        }
        return emptyValue;
    }

    @Nonnull
    private String resolveSelectedChoiceSummaryValue(@Nonnull List<ChoiceEntry> entries, @Nonnull String selectedId) {
        for (ChoiceEntry entry : entries) {
            if (entry.id().equalsIgnoreCase(selectedId)) {
                return entry.label();
            }
        }
        return "-";
    }

    @Nonnull
    private String resolveRaceDisplayName(@Nonnull RaceDefinition definition) {
        return definition.getDisplayName() == null || definition.getDisplayName().isBlank()
                ? definition.getRaceId()
                : definition.getDisplayName();
    }

    @Nonnull
    private String resolveClassDisplayName(@Nonnull ClassDefinition definition) {
        return definition.getDisplayName() == null || definition.getDisplayName().isBlank()
                ? definition.getId()
                : definition.getDisplayName();
    }

    @Nonnull
    private String buildCostLabel() {
        if (mode != Mode.BARBER) {
            return "Creation Cost: Free";
        }
        BarberShopSettings settings = Realmweavers.get().getCharacterManager().getSettings().getBarberShopSettings();
        int changes = draftAppearance.countDifferences(persistedAppearance);
        if (!settings.isEnabled() || changes <= 0) {
            return "Barber Cost: Free";
        }

        long amount;
        String currencyId;
        if (settings.getPricingMode() == BarberPricingMode.PerChangedSlot) {
            amount = settings.getPerChangedSlotCost().getAmount() * changes;
            currencyId = settings.getPerChangedSlotCost().getCurrencyId();
        } else {
            amount = settings.getFlatCost().getAmount();
            currencyId = settings.getFlatCost().getCurrencyId();
        }
        return amount <= 0L
                ? "Barber Cost: Free"
                : "Barber Cost: " + amount + " " + (currencyId == null || currencyId.isBlank() ? "currency" : currencyId);
    }

    @Nonnull
    private String humanizeCategory(@Nonnull Category category) {
        return switch (category) {
            case BODY_CHARACTERISTIC -> "Body";
            case UNDERWEAR -> "Underwear";
            case FACE -> "Face";
            case EYES -> "Eyes";
            case EARS -> "Ears";
            case MOUTH -> "Mouth";
            case FACIAL_HAIR -> "Facial Hair";
            case HAIRCUT -> "Haircut";
            case EYEBROWS -> "Eyebrows";
            case PANTS -> "Pants";
            case OVERPANTS -> "Overpants";
            case UNDERTOP -> "Undertop";
            case OVERTOP -> "Overtop";
            case SHOES -> "Shoes";
            case HEAD_ACCESSORY -> "Head Accessory";
            case FACE_ACCESSORY -> "Face Accessory";
            case EAR_ACCESSORY -> "Ear Accessory";
            case SKIN_FEATURE -> "Skin Feature";
            case GLOVES -> "Gloves";
            case CAPE -> "Cape";
        };
    }

    public enum Mode {
        CREATE,
        BARBER
    }

    private record ChoiceEntry(String id, String label, boolean selectable, String detail) {
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec
                .builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Type", Codec.STRING), (i, s) -> i.type = s, i -> i.type)
                .add()
                .append(new KeyedCodec<>("CategoryId", Codec.STRING), (i, s) -> i.categoryId = s, i -> i.categoryId)
                .add()
                .append(new KeyedCodec<>("OptionId", Codec.STRING), (i, s) -> i.optionId = s, i -> i.optionId)
                .add()
                .append(new KeyedCodec<>("CosmeticId", Codec.STRING), (i, s) -> i.cosmeticId = s, i -> i.cosmeticId)
                .add()
                .append(new KeyedCodec<>("ColorId", Codec.STRING), (i, s) -> i.colorId = s, i -> i.colorId)
                .add()
                .append(new KeyedCodec<>("VariantId", Codec.STRING), (i, s) -> i.variantId = s, i -> i.variantId)
                .add()
                .append(new KeyedCodec<>("@SearchQuery", Codec.STRING), (i, s) -> i.searchQuery = s, i -> i.searchQuery)
                .add()
                .append(new KeyedCodec<>("@Name", Codec.STRING), (i, s) -> i.name = s, i -> i.name)
                .add()
                .append(new KeyedCodec<>("@Hardcore", Codec.BOOLEAN), (i, s) -> i.hardcore = s, i -> i.hardcore)
                .add()
                .build();

        private String type;
        private String categoryId;
        private String optionId;
        private String cosmeticId;
        private String colorId;
        private String variantId;
        private String searchQuery;
        private String name;
        private Boolean hardcore;
    }
}
