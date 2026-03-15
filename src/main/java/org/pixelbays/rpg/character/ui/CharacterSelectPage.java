package org.pixelbays.rpg.character.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.character.CharacterActionResult;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.config.CharacterRosterData.CharacterProfileData;

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

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String HERO_NAME_LABEL = "#HeroNameLabel";
    private static final String HERO_META_LABEL = "#HeroMetaLabel";
    private static final String DETAILS_LABEL = "#DetailsLabel";
    private static final String SETTINGS_LABEL = "#SettingsLabel";
    private static final String OVERFLOW_LABEL = "#OverflowLabel";

    private static final String CHARACTER_ID_FIELD = "#CharacterIdField";
    private static final String CREATE_NAME_FIELD = "#CreateNameField";
    private static final String CREATE_RACE_FIELD = "#CreateRaceField";
    private static final String CREATE_CLASS_FIELD = "#CreateClassField";

    private static final String SLOT_BUTTON_ONE = "#SlotButton1";
    private static final String SLOT_BUTTON_TWO = "#SlotButton2";
    private static final String SLOT_BUTTON_THREE = "#SlotButton3";
    private static final String SLOT_BUTTON_FOUR = "#SlotButton4";
    private static final String SLOT_BUTTON_FIVE = "#SlotButton5";
    private static final String SLOT_BUTTON_SIX = "#SlotButton6";

    private static final String REFRESH_BUTTON = "#RefreshButton";
    private static final String SELECT_BUTTON = "#SelectButton";
    private static final String DELETE_BUTTON = "#DeleteButton";
    private static final String RECOVER_BUTTON = "#RecoverButton";
    private static final String CREATE_BUTTON = "#CreateButton";

    private static final String[] SLOT_BUTTONS = {
            SLOT_BUTTON_ONE,
            SLOT_BUTTON_TWO,
            SLOT_BUTTON_THREE,
            SLOT_BUTTON_FOUR,
            SLOT_BUTTON_FIVE,
            SLOT_BUTTON_SIX
    };

    private final CharacterManager characterManager;

    public CharacterSelectPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.characterManager = ExamplePlugin.get().getCharacterManager();
    }

    @Override
    @SuppressWarnings("unused")
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/CharacterSelectPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String characterId = extractString(rawData, "@CharacterId");
        String createName = extractString(rawData, "@CreateName");
        String createRace = extractString(rawData, "@CreateRace");
        String createClass = extractString(rawData, "@CreateClass");
        int slotIndex = extractInt(rawData, "SlotIndex", -1);

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, characterId, createName, createRace, createClass, slotIndex));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String characterId,
            @Nullable String createName,
            @Nullable String createRace,
            @Nullable String createClass,
            int slotIndex) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef currentPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || currentPlayerRef == null) {
            return;
        }

        List<CharacterProfileData> profiles = characterManager.getProfilesFor(currentPlayerRef.getUuid(), currentPlayerRef.getUsername());
        CharacterActionResult result = null;
        CharacterProfileData focusedProfile = resolveProfile(profiles, characterId);

        switch (action) {
            case "Refresh" -> {
            }
            case "Focus" -> focusedProfile = resolveProfile(profiles, slotIndex);
            case "Select" -> result = characterManager.selectCharacter(ref, store, currentPlayerRef, characterId);
            case "Delete" -> result = characterManager.deleteCharacter(currentPlayerRef, characterId);
            case "Recover" -> result = characterManager.recoverCharacter(currentPlayerRef, characterId);
            case "Create" -> result = characterManager.createCharacter(ref, store, currentPlayerRef, createName, createRace,
                    createClass);
            default -> {
            }
        }

        if (result != null && result.getProfile() != null) {
            focusedProfile = result.getProfile();
        }

        if ("Select".equals(action) && result != null && result.isSuccess()) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder,
                result == null ? null : characterManager.mapMessage(result.getMessage()),
            focusedProfile);
        sendUpdate(commandBuilder);
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        for (int index = 0; index < SLOT_BUTTONS.length; index++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                SLOT_BUTTONS[index],
                new EventData().append("Action", "Focus")
                    .append("SlotIndex", Integer.toString(index)));
        }

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
                new EventData().append("Action", "Recover")
                        .append("@CharacterId", CHARACTER_ID_FIELD + ".Value"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                CREATE_BUTTON,
                new EventData().append("Action", "Create")
                        .append("@CreateName", CREATE_NAME_FIELD + ".Value")
                        .append("@CreateRace", CREATE_RACE_FIELD + ".Value")
                        .append("@CreateClass", CREATE_CLASS_FIELD + ".Value"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nullable Message statusMessage,
            @Nullable CharacterProfileData focusedProfile) {
        PlayerRef currentPlayerRef = this.playerRef;
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        List<CharacterProfileData> profiles = characterManager.getProfilesFor(currentPlayerRef.getUuid(), currentPlayerRef.getUsername());
        String activeCharacterId = characterManager.getActiveCharacterId(currentPlayerRef.getUuid());
        String lastSelectedCharacterId = characterManager.getLastSelectedCharacterId(currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());

        CharacterProfileData selected = focusedProfile;
        if (selected == null && !activeCharacterId.isBlank()) {
            selected = resolveProfile(profiles, activeCharacterId);
        }
        if (selected == null && !lastSelectedCharacterId.isBlank()) {
            selected = resolveProfile(profiles, lastSelectedCharacterId);
        }
        if (selected == null && !profiles.isEmpty()) {
            selected = profiles.get(0);
        }

        for (int index = 0; index < SLOT_BUTTONS.length; index++) {
            CharacterProfileData slotProfile = index < profiles.size() ? profiles.get(index) : null;
            commandBuilder.set(SLOT_BUTTONS[index] + ".Text", formatSlotText(slotProfile, selected, activeCharacterId, index));
        }

        commandBuilder.set(CHARACTER_ID_FIELD + ".Value", selected == null ? "" : selected.getCharacterId());

        if (selected != null) {
            String race = selected.getRaceId().isBlank() ? "Unchosen Race" : selected.getRaceId();
            String classId = selected.getPrimaryClassId().isBlank() ? "Unchosen Class" : selected.getPrimaryClassId();
            int displayLevel = characterManager.resolveDisplayedLevel(selected);
            commandBuilder.set(HERO_NAME_LABEL + ".Text", selected.getCharacterName());
            commandBuilder.set(HERO_META_LABEL + ".Text",
                "Level " + displayLevel + " • " + race + " • " + classId);
            commandBuilder.set(DETAILS_LABEL + ".Text",
                    "Character Id: " + selected.getCharacterId()
                            + "\nStatus: " + (selected.isSoftDeleted() ? "Marked for recovery" : "Ready to enter world")
                            + "\nLobby Background: " + characterManager.resolveBackgroundId(selected)
                            + "\nPreview Camera: " + characterManager.resolvePreviewCameraId(selected)
                            + "\nLast Played: " + selected.getLastPlayedEpochMs());
        } else {
            commandBuilder.set(HERO_NAME_LABEL + ".Text", "No Character Selected");
            commandBuilder.set(HERO_META_LABEL + ".Text", "Create a new adventurer or select an existing one.");
            commandBuilder.set(DETAILS_LABEL + ".Text", "");
        }

        commandBuilder.set(SETTINGS_LABEL + ".Text",
                "Account: " + currentPlayerRef.getUsername()
                        + "\nActive Character: " + (activeCharacterId.isBlank() ? "none" : activeCharacterId)
                        + "\nSelected Character: " + (selected == null ? "none" : selected.getCharacterId())
                        + "\nVisible Slots: " + Math.min(profiles.size(), SLOT_BUTTONS.length) + "/" + profiles.size());

        if (profiles.size() > SLOT_BUTTONS.length) {
            commandBuilder.set(OVERFLOW_LABEL + ".Text",
                    "Additional characters exist beyond the visible roster slots. Use refresh after changing slot limits.");
        } else {
            commandBuilder.set(OVERFLOW_LABEL + ".Text", "");
        }
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

    @Nullable
    private CharacterProfileData resolveProfile(@Nonnull List<CharacterProfileData> profiles, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= profiles.size()) {
            return null;
        }
        return profiles.get(slotIndex);
    }

    @Nonnull
    private String formatSlotText(@Nullable CharacterProfileData profile,
            @Nullable CharacterProfileData selected,
            @Nonnull String activeCharacterId,
            int slotIndex) {
        if (profile == null) {
            return "Empty Slot\nChoose create below to forge a new character.\nSlot " + (slotIndex + 1);
        }

        StringBuilder builder = new StringBuilder();
        int displayLevel = characterManager.resolveDisplayedLevel(profile);
        builder.append(profile.getCharacterName())
                .append("\nLevel ")
            .append(displayLevel)
                .append(" • ")
                .append(profile.getRaceId().isBlank() ? "No Race" : profile.getRaceId())
                .append("\n")
                .append(profile.getPrimaryClassId().isBlank() ? "No Class" : profile.getPrimaryClassId());

        List<String> badges = new java.util.ArrayList<>();
        if (profile.isSoftDeleted()) {
            badges.add("Deleted");
        }
        if (!activeCharacterId.isBlank() && activeCharacterId.equalsIgnoreCase(profile.getCharacterId())) {
            badges.add("Active");
        }
        if (selected != null && selected.getCharacterId().equalsIgnoreCase(profile.getCharacterId())) {
            badges.add("Selected");
        }
        if (!badges.isEmpty()) {
            builder.append("\n[").append(String.join(" • ", badges)).append("]");
        }
        return builder.toString();
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

    private static int extractInt(@Nonnull String rawData, @Nonnull String key, int fallback) {
        String value = extractString(rawData, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
