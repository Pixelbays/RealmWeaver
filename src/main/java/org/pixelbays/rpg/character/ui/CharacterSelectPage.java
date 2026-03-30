package org.pixelbays.rpg.character.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.character.CharacterActionResult;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.config.CharacterProfileData;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings;
import org.pixelbays.rpg.character.token.CharacterTokenDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class CharacterSelectPage extends CustomUIPage {

    private static final int HARD_MAX_VISIBLE_SLOTS = 50;
    private static final String SLOT_ENTRY_ASSET = "Common/CharacterRosterEntry.ui";

    private static final String BRAND_TITLE_LABEL = "#BrandTitleLabel";
    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String HERO_NAME_LABEL = "#HeroNameLabel";
    private static final String HERO_META_LABEL = "#HeroMetaLabel";
    private static final String DETAILS_LABEL = "#DetailsLabel";
    private static final String SETTINGS_LABEL = "#SettingsLabel";
    private static final String OVERFLOW_LABEL = "#OverflowLabel";
    private static final String SLOT_LIST = "#SlotList";
    private static final String CHARACTER_ID_FIELD = "#CharacterIdField";

    private static final String REFRESH_BUTTON = "#RefreshButton";
    private static final String SELECT_BUTTON = "#SelectButton";
    private static final String DELETE_BUTTON = "#DeleteButton";
    private static final String RECOVER_BUTTON = "#RecoverButton";
    private static final String CREATE_BUTTON = "#CreateButton";
    private static final String LEAVE_SERVER_BUTTON = "#LeaveServerButton";
    private static final String ROTATE_LEFT_BUTTON = "#RotateLeftButton";
    private static final String ROTATE_RIGHT_BUTTON = "#RotateRightButton";
    private static final String ROSTER_ACTION_ROW = "#RosterActionRow";

    private final CharacterManager characterManager;

    public CharacterSelectPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.characterManager = ExamplePlugin.get().getCharacterManager();
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        characterManager.resetCharacterAppearancePreviewCamera(playerRef);
        characterManager.ensureCharacterSelectionUiOpen(ref, store, playerRef);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/CharacterSelectPage.ui");
        bindStaticEvents(eventBuilder);
        CharacterProfileData selectedProfile = appendView(ref, store, commandBuilder, eventBuilder, null, null, true);
        syncPreview(ref, store, selectedProfile);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String characterId = extractString(rawData, "CharacterId");
        if (characterId == null) {
            characterId = extractString(rawData, "@CharacterId");
        }
        final String resolvedCharacterId = characterId;

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, resolvedCharacterId));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String characterId) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef currentPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || currentPlayerRef == null) {
            return;
        }

        List<CharacterProfileData> profiles = characterManager.getSelectableProfilesFor(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        CharacterActionResult result = null;
        CharacterProfileData focusedProfile = resolveProfile(profiles, characterId);

        switch (action) {
            case "Refresh" -> {
                characterManager.reopenCharacterSelectPage(ref, store, currentPlayerRef);
                return;
            }
            case "Focus" -> focusedProfile = resolveProfile(profiles, characterId);
            case "Select" -> result = characterManager.selectCharacter(ref, store, currentPlayerRef, characterId);
            case "Delete" -> result = characterManager.deleteCharacter(currentPlayerRef, characterId);
            case "OpenRecovery" -> {
                characterManager.openDeletedCharacterRecoveryPage(ref, store, currentPlayerRef);
                return;
            }
            case "LeaveServer" -> {
                currentPlayerRef.getPacketHandler().disconnect(Message.raw("Left the server."));
                return;
            }
            case "RotateLeft" -> characterManager.rotateCharacterPreviewLeft(ref, store);
            case "RotateRight" -> characterManager.rotateCharacterPreviewRight(ref, store);
            case "CreateNew" -> {
                characterManager.openCharacterCreator(ref, store, currentPlayerRef);
                return;
            }
            default -> {
            }
        }

        if (result != null && result.getProfile() != null && !result.getProfile().isSoftDeleted()) {
            focusedProfile = result.getProfile();
        }

        if ("Select".equals(action) && result != null && result.isSuccess()) {
            return;
        }

        if ("Delete".equals(action) && result != null && result.isSuccess()) {
            characterManager.reopenCharacterSelectPage(ref, store, currentPlayerRef);
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        CharacterProfileData selectedProfile = appendView(ref,
            store,
            commandBuilder,
                null,
                result == null ? null : characterManager.mapMessage(result.getMessage()),
                focusedProfile,
                false);
        syncPreview(ref, store, selectedProfile);
        sendUpdate(commandBuilder);
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                REFRESH_BUTTON,
                new EventData().append("Action", "Refresh"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                SELECT_BUTTON,
                new EventData().append("Action", "Select")
                        .append("@CharacterId", CHARACTER_ID_FIELD + ".Value"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                DELETE_BUTTON,
                new EventData().append("Action", "Delete")
                        .append("@CharacterId", CHARACTER_ID_FIELD + ".Value"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                RECOVER_BUTTON,
                new EventData().append("Action", "OpenRecovery"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                LEAVE_SERVER_BUTTON,
                new EventData().append("Action", "LeaveServer"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                ROTATE_LEFT_BUTTON,
                new EventData().append("Action", "RotateLeft"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                ROTATE_RIGHT_BUTTON,
                new EventData().append("Action", "RotateRight"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                CREATE_BUTTON,
                new EventData().append("Action", "CreateNew"));
    }

    @Nullable
        private CharacterProfileData appendView(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UIEventBuilder eventBuilder,
            @Nullable Message statusMessage,
            @Nullable CharacterProfileData focusedProfile,
            boolean rebuildRows) {
        PlayerRef currentPlayerRef = this.playerRef;
        CharacterModSettings settings = characterManager.getSettings();
        List<CharacterProfileData> profiles = characterManager.getSelectableProfilesFor(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        List<CharacterProfileData> deletedProfiles = characterManager.getDeletedProfilesFor(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        String activeCharacterId = characterManager.getActiveCharacterId(currentPlayerRef.getUuid());
        String lastSelectedCharacterId = characterManager.getLastSelectedCharacterId(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        int currentSlotCapacity = characterManager.getCurrentCharacterSlotCapacity(ref, store, currentPlayerRef);
        int visibleSlotCount = resolveVisibleSlotCount(profiles.size(), currentSlotCapacity);
        int defaultSlotCount = settings.getDefaultCharacterSlots();
        int unlockedSlotCount = characterManager.getUnlockedExtraCharacterSlots(currentPlayerRef.getUuid(), currentPlayerRef.getUsername());
        int grantedSlotCount = characterManager.getGrantedCharacterSlotCount(ref, store, currentPlayerRef);

        CharacterProfileData selected = resolveSelectedProfile(profiles, focusedProfile, activeCharacterId, lastSelectedCharacterId);
        boolean deleteVisible = settings.isAllowCharacterDeletion() && selected != null;
        boolean deletedRecoveryVisible = settings.usesSoftDeleteRecovery() && !deletedProfiles.isEmpty();

        commandBuilder.set(BRAND_TITLE_LABEL + ".Text", "Realmweavers: " + characterManager.resolveServerName());
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        if (rebuildRows) {
            commandBuilder.clear(SLOT_LIST);
        }
        for (int index = 0; index < visibleSlotCount; index++) {
            CharacterProfileData slotProfile = index < profiles.size() ? profiles.get(index) : null;
            String selector = SLOT_LIST + "[" + index + "]";
            if (rebuildRows) {
                appendSlotEntry(commandBuilder, index);
            }
            boolean isSelected = slotProfile != null
                    && selected != null
                    && selected.getCharacterId().equalsIgnoreCase(slotProfile.getCharacterId());

            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", isSelected);
            commandBuilder.set(selector + " #TitleLabel.Text", formatSlotTitle(slotProfile, index));
            commandBuilder.set(selector + " #SubtitleLabel.Text", formatSlotSubtitle(slotProfile));
            commandBuilder.set(selector + " #MetaLabel.Text", formatSlotMeta(slotProfile, activeCharacterId, index));

            if (rebuildRows && eventBuilder != null && slotProfile != null) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        new EventData().append("Action", "Focus").append("CharacterId", slotProfile.getCharacterId()),
                        false);
            }
        }

        commandBuilder.set(CHARACTER_ID_FIELD + ".Value", selected == null ? "" : selected.getCharacterId());
        commandBuilder.set(SELECT_BUTTON + ".Visible", selected != null);
        commandBuilder.set(DELETE_BUTTON + ".Visible", deleteVisible);
        commandBuilder.set(RECOVER_BUTTON + ".Visible", deletedRecoveryVisible);
        commandBuilder.set(CREATE_BUTTON + ".Visible", settings.isAllowCharacterCreation());
        commandBuilder.set(ROSTER_ACTION_ROW + ".Visible", deleteVisible || deletedRecoveryVisible);

        if (selected != null) {
            String race = selected.getRaceId().isBlank() ? "Unchosen Race" : selected.getRaceId();
            String classId = characterManager.resolveDisplayedClassName(selected);
            if (classId.isBlank()) {
                classId = "Unchosen Class";
            }
            int displayLevel = characterManager.resolveDisplayedLevel(selected);
            commandBuilder.set(HERO_NAME_LABEL + ".Text", selected.getCharacterName());
            commandBuilder.set(HERO_META_LABEL + ".Text",
                    "Level " + displayLevel + " • " + race + " • " + classId + (selected.isHardcore() ? " • Hardcore" : ""));
            commandBuilder.set(DETAILS_LABEL + ".Text",
                    "Character Id: " + selected.getCharacterId()
                            + "\nStatus: Ready to enter world"
                            + "\nProfile Type: " + (selected.isHardcore() ? "Hardcore" : "Standard")
                            + "\nLobby Background: " + characterManager.resolveBackgroundId(selected)
                            + "\nPreview Camera: " + characterManager.resolvePreviewCameraId(selected)
                            + "\nLast Played: " + selected.getLastPlayedEpochMs());
        } else if (!deletedProfiles.isEmpty()) {
            commandBuilder.set(HERO_NAME_LABEL + ".Text", "No Active Character Selected");
            commandBuilder.set(HERO_META_LABEL + ".Text", "Recover a deleted character or create a new one.");
            commandBuilder.set(DETAILS_LABEL + ".Text", "Deleted characters are stored in the recovery list.");
        } else {
            commandBuilder.set(HERO_NAME_LABEL + ".Text", "No Character Selected");
            commandBuilder.set(HERO_META_LABEL + ".Text", "Create a new adventurer or select an existing one.");
            commandBuilder.set(DETAILS_LABEL + ".Text", "");
        }

        Map<CharacterTokenDefinition, Long> tokenBalances = characterManager.getVisibleAccountTokenBalances(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        String tokenSummary = tokenBalances.isEmpty()
                ? "No account-bound character tokens found."
                : formatTokenSummary(tokenBalances);
        commandBuilder.set(SETTINGS_LABEL + ".Text",
                "Account: " + currentPlayerRef.getUsername()
                        + "\nActive Character: " + (activeCharacterId.isBlank() ? "none" : activeCharacterId)
                        + "\nSelected Character: " + (selected == null ? "none" : selected.getCharacterId())
                        + "\nDeleted Characters: " + deletedProfiles.size()
                + "\nSlots: " + profiles.size() + "/" + currentSlotCapacity
                + " (Base " + defaultSlotCount
                + ", Unlocks " + unlockedSlotCount
                + ", Grants " + grantedSlotCount
                + ", Hard Max " + settings.getMaxCharacterSlots() + ")"
                        + "\n\nCharacter Tokens:\n" + tokenSummary);

        if (profiles.size() > currentSlotCapacity) {
            commandBuilder.set(OVERFLOW_LABEL + ".Text",
                "This account currently has more characters than its active slot grants. Existing characters remain selectable.");
        } else if (profiles.size() > visibleSlotCount) {
            commandBuilder.set(OVERFLOW_LABEL + ".Text",
                    "Only the first " + visibleSlotCount + " roster slots are shown in this view.");
        } else if (!deletedProfiles.isEmpty()) {
            commandBuilder.set(OVERFLOW_LABEL + ".Text",
                    deletedProfiles.size() + " deleted character(s) are available in the recovery list.");
        } else {
            commandBuilder.set(OVERFLOW_LABEL + ".Text", "");
        }

        return selected;
    }

    private int resolveVisibleSlotCount(int profileCount, int currentSlotCapacity) {
        return Math.max(1, Math.min(HARD_MAX_VISIBLE_SLOTS, Math.max(profileCount, currentSlotCapacity)));
    }

    @Nonnull
    private String appendSlotEntry(@Nonnull UICommandBuilder commandBuilder, int slotIndex) {
        commandBuilder.append(SLOT_LIST, SLOT_ENTRY_ASSET);
        return SLOT_LIST + "[" + slotIndex + "]";
    }

    @Nullable
    private CharacterProfileData resolveSelectedProfile(@Nonnull List<CharacterProfileData> profiles,
            @Nullable CharacterProfileData focusedProfile,
            @Nonnull String activeCharacterId,
            @Nonnull String lastSelectedCharacterId) {
        CharacterProfileData selected = focusedProfile == null ? null : resolveProfile(profiles, focusedProfile.getCharacterId());
        if (selected == null && !activeCharacterId.isBlank()) {
            selected = resolveProfile(profiles, activeCharacterId);
        }
        if (selected == null && !lastSelectedCharacterId.isBlank()) {
            selected = resolveProfile(profiles, lastSelectedCharacterId);
        }
        if (selected == null && !profiles.isEmpty()) {
            selected = profiles.getFirst();
        }
        return selected;
    }

    @Nonnull
    private String formatSlotTitle(@Nullable CharacterProfileData profile, int slotIndex) {
        if (profile == null) {
            return "Empty Slot " + (slotIndex + 1);
        }
        return profile.getCharacterName() + (profile.isHardcore() ? " [Hardcore]" : "");
    }

    @Nonnull
    private String formatSlotSubtitle(@Nullable CharacterProfileData profile) {
        if (profile == null) {
            return "Choose create below to forge a new character.";
        }

        String race = profile.getRaceId().isBlank() ? "No Race" : profile.getRaceId();
        String displayedClass = characterManager.resolveDisplayedClassName(profile);
        if (displayedClass.isBlank()) {
            displayedClass = "No Class";
        }
        return "Level " + characterManager.resolveDisplayedLevel(profile) + " • " + race + " • " + displayedClass;
    }

    @Nonnull
    private String formatSlotMeta(@Nullable CharacterProfileData profile,
            @Nonnull String activeCharacterId,
            int slotIndex) {
        if (profile == null) {
            return "Slot " + (slotIndex + 1);
        }

        StringBuilder builder = new StringBuilder();
        if (!activeCharacterId.isBlank() && activeCharacterId.equalsIgnoreCase(profile.getCharacterId())) {
            builder.append("Active Character");
        }
        if (profile.isHardcore()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append("Hardcore");
        }
        if (builder.length() > 0) {
            builder.append(" • ");
        }
        builder.append("Slot ").append(slotIndex + 1);
        return builder.toString();
    }

    private void syncPreview(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable CharacterProfileData selectedProfile) {
        if (selectedProfile != null) {
            characterManager.applyCharacterPreview(ref, store, playerRef,
                    selectedProfile.getRaceId(),
                    selectedProfile.getAppearance());
            characterManager.resetCharacterPreviewRotation(ref, store);
            return;
        }
        characterManager.resetCharacterPreviewRotation(ref, store);
        characterManager.applyCharacterAppearancePreviewCamera(ref, store, playerRef);
    }

    @Nonnull
    private String formatTokenSummary(@Nonnull Map<CharacterTokenDefinition, Long> tokenBalances) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<CharacterTokenDefinition, Long> entry : tokenBalances.entrySet()) {
            CharacterTokenDefinition definition = entry.getKey();
            if (definition == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(definition.getDisplayName())
                    .append(": ")
                    .append(entry.getValue());
        }
        return builder.toString();
    }

    @Nullable
    private CharacterProfileData resolveProfile(@Nonnull List<CharacterProfileData> profiles, @Nullable String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return null;
        }

        return profiles.stream()
                .filter(profile -> profile != null && characterId.equalsIgnoreCase(profile.getCharacterId()))
                .findFirst()
                .orElse(null);
    }
    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
    }

    @Nullable
    private static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue stringValue) {
            return stringValue.value;
        }
        if (value instanceof IntParamValue intValue) {
            return Integer.toString(intValue.value);
        }
        if (value instanceof LongParamValue longValue) {
            return Long.toString(longValue.value);
        }
        if (value instanceof DoubleParamValue doubleValue) {
            return Double.toString(doubleValue.value);
        }
        if (value instanceof BoolParamValue boolValue) {
            return Boolean.toString(boolValue.value);
        }
        return null;
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
