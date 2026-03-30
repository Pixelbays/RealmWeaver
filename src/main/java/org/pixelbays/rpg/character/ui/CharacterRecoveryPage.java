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
public class CharacterRecoveryPage extends CustomUIPage {

    private static final String ENTRY_ASSET = "Common/CharacterRosterEntry.ui";

    private static final String BRAND_TITLE_LABEL = "#BrandTitleLabel";
    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String SUMMARY_LABEL = "#SummaryLabel";
    private static final String DETAILS_LABEL = "#DetailsLabel";
    private static final String DELETED_LIST = "#DeletedList";
    private static final String CHARACTER_ID_FIELD = "#CharacterIdField";

    private static final String RECOVER_BUTTON = "#RecoverButton";
    private static final String BACK_BUTTON = "#BackButton";
    private static final String LEAVE_SERVER_BUTTON = "#LeaveServerButton";

    private final CharacterManager characterManager;

    public CharacterRecoveryPage(@Nonnull PlayerRef playerRef) {
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
        commandBuilder.append("Pages/CharacterRecoveryPage.ui");
        bindStaticEvents(eventBuilder);
        CharacterProfileData selectedProfile = appendView(commandBuilder, eventBuilder, null, null, true);
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

        List<CharacterProfileData> deletedProfiles = characterManager.getDeletedProfilesFor(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        CharacterActionResult result = null;
        CharacterProfileData focusedProfile = resolveProfile(deletedProfiles, characterId);

        switch (action) {
            case "Focus" -> focusedProfile = resolveProfile(deletedProfiles, characterId);
            case "Recover" -> result = characterManager.recoverCharacter(currentPlayerRef, characterId);
            case "Back" -> {
                characterManager.reopenCharacterSelectPage(ref, store, currentPlayerRef);
                return;
            }
            case "LeaveServer" -> {
                currentPlayerRef.getPacketHandler().disconnect(Message.raw("Left the server."));
                return;
            }
            default -> {
            }
        }

        if ("Recover".equals(action) && result != null && result.isSuccess()) {
            characterManager.openDeletedCharacterRecoveryPage(ref, store, currentPlayerRef);
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        CharacterProfileData selectedProfile = appendView(commandBuilder,
                null,
                result == null ? null : characterManager.mapMessage(result.getMessage()),
                result != null && result.getProfile() != null && result.getProfile().isSoftDeleted() ? result.getProfile() : focusedProfile,
                false);
        syncPreview(ref, store, selectedProfile);
        sendUpdate(commandBuilder);
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                RECOVER_BUTTON,
                new EventData().append("Action", "Recover")
                        .append("@CharacterId", CHARACTER_ID_FIELD + ".Value"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                BACK_BUTTON,
                new EventData().append("Action", "Back"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                LEAVE_SERVER_BUTTON,
                new EventData().append("Action", "LeaveServer"));
    }

        @Nullable
        private CharacterProfileData appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nullable UIEventBuilder eventBuilder,
            @Nullable Message statusMessage,
            @Nullable CharacterProfileData focusedProfile,
            boolean rebuildRows) {
        PlayerRef currentPlayerRef = this.playerRef;
        CharacterModSettings settings = characterManager.getSettings();
        List<CharacterProfileData> deletedProfiles = characterManager.getDeletedProfilesFor(
                currentPlayerRef.getUuid(),
                currentPlayerRef.getUsername());
        CharacterProfileData selected = focusedProfile == null ? null : resolveProfile(deletedProfiles, focusedProfile.getCharacterId());
        if (selected == null && !deletedProfiles.isEmpty()) {
            selected = deletedProfiles.getFirst();
        }

        commandBuilder.set(BRAND_TITLE_LABEL + ".Text", "Realmweavers: " + characterManager.resolveServerName());
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        if (rebuildRows) {
            commandBuilder.clear(DELETED_LIST);
        }
        for (int index = 0; index < deletedProfiles.size(); index++) {
            CharacterProfileData profile = deletedProfiles.get(index);
            String selector = DELETED_LIST + "[" + index + "]";
            if (rebuildRows) {
                appendEntry(commandBuilder, index);
            }
            boolean isSelected = selected != null && selected.getCharacterId().equalsIgnoreCase(profile.getCharacterId());
            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", isSelected);
            commandBuilder.set(selector + " #TitleLabel.Text", profile.getCharacterName() + (profile.isHardcore() ? " [Hardcore]" : ""));
            commandBuilder.set(selector + " #SubtitleLabel.Text", buildSubtitle(profile));
            commandBuilder.set(selector + " #MetaLabel.Text", "Deleted At: " + profile.getDeletedAtEpochMs());
            if (rebuildRows && eventBuilder != null) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        new EventData().append("Action", "Focus").append("CharacterId", profile.getCharacterId()),
                        false);
            }
        }

        commandBuilder.set(CHARACTER_ID_FIELD + ".Value", selected == null ? "" : selected.getCharacterId());
        commandBuilder.set(RECOVER_BUTTON + ".Visible", selected != null);
        commandBuilder.set(SUMMARY_LABEL + ".Text",
                "Deleted Characters: " + deletedProfiles.size()
                        + "\nRetention Hours: " + settings.getDeletedCharacterRetentionHours()
                        + "\nRecoveries Per Window: " + settings.getMaxRecoveriesPerWindow()
                        + "\nRecovery Window (Hours): " + settings.getRecoveryWindowHours());

        if (selected != null) {
            commandBuilder.set(DETAILS_LABEL + ".Text",
                    "Character Id: " + selected.getCharacterId()
                            + "\nProfile Type: " + (selected.isHardcore() ? "Hardcore" : "Standard")
                            + "\nLast Played: " + selected.getLastPlayedEpochMs()
                            + "\nDeleted At: " + selected.getDeletedAtEpochMs()
                            + "\nPreview Camera: " + characterManager.resolvePreviewCameraId(selected));
        } else {
            commandBuilder.set(DETAILS_LABEL + ".Text", "No deleted characters are pending recovery.");
        }

        return selected;
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
    private String appendEntry(@Nonnull UICommandBuilder commandBuilder, int index) {
        commandBuilder.append(DELETED_LIST, ENTRY_ASSET);
        return DELETED_LIST + "[" + index + "]";
    }

    @Nonnull
    private String buildSubtitle(@Nonnull CharacterProfileData profile) {
        String race = profile.getRaceId().isBlank() ? "No Race" : profile.getRaceId();
        String classId = characterManager.resolveDisplayedClassName(profile);
        if (classId.isBlank()) {
            classId = "No Class";
        }
        return "Level " + characterManager.resolveDisplayedLevel(profile) + " • " + race + " • " + classId;
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