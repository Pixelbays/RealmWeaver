package org.pixelbays.rpg.classes.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.command.ClassCommandUtil;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.FirstJoinClassSelectionManager;

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
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class FirstJoinClassSelectionPage extends CustomUIPage {

    private static final String CLASS_ENTRY_ASSET = "Common/CharacterRosterEntry.ui";

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String CLASS_LIST = "#ClassList";
    private static final String SELECTED_CLASS_ID_FIELD = "#SelectedClassIdField";
    private static final String HERO_NAME_LABEL = "#HeroNameLabel";
    private static final String HERO_META_LABEL = "#HeroMetaLabel";
    private static final String DETAILS_LABEL = "#DetailsLabel";
    private static final String CONFIRM_BUTTON = "#ConfirmButton";

    @Nullable
    private final FirstJoinClassSelectionManager selectionManager;

    private String selectedClassId = "";
    @Nullable
    private Message statusMessage;

    public FirstJoinClassSelectionPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CantClose);
        this.selectionManager = Realmweavers.get().getFirstJoinClassSelectionManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/FirstJoinClassSelectionPage.ui");
        bindStaticEvents(eventBuilder);
        appendView(ref, store, commandBuilder, eventBuilder, true);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String classId = extractString(rawData, "ClassId");
        if (classId == null) {
            classId = extractString(rawData, "SelectedClassId");
        }
        if (classId == null) {
            classId = extractString(rawData, "@SelectedClassId");
        }
        final String resolvedClassId = classId;

        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, resolvedClassId));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String classId) {
        if (!ref.isValid() || selectionManager == null) {
            return;
        }

        switch (action) {
            case "Focus" -> {
                selectedClassId = classId == null ? "" : classId.trim();
                statusMessage = null;
            }
            case "Confirm" -> {
                String requestedClassId = classId == null || classId.isBlank() ? selectedClassId : classId.trim();
                String result = selectionManager.chooseStartingClass(ref, store, playerRef, requestedClassId);
                if (result != null && result.startsWith("SUCCESS")) {
                    return;
                }
                statusMessage = ClassCommandUtil.managerResultMessage(result);
            }
            default -> {
            }
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(ref, store, commandBuilder, null, false);
        sendUpdate(commandBuilder);
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                CONFIRM_BUTTON,
                new EventData().append("Action", "Confirm")
                        .append("@SelectedClassId", SELECTED_CLASS_ID_FIELD + ".Value"));
    }

    private void appendView(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UIEventBuilder eventBuilder,
            boolean rebuildRows) {
        List<ClassDefinition> startingClasses = selectionManager == null
                ? List.of()
                : selectionManager.getSelectableStartingClasses(ref, store);

        if (selectedClassId.isBlank() || startingClasses.stream().noneMatch(def -> def.getId().equalsIgnoreCase(selectedClassId))) {
            selectedClassId = startingClasses.isEmpty() ? "" : startingClasses.getFirst().getId();
        }

        if (statusMessage != null) {
            String resolvedStatusText = toResolvedText(statusMessage);
            if (resolvedStatusText != null) {
                commandBuilder.set(STATUS_LABEL + ".Text", resolvedStatusText);
            } else {
                commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
            }
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        if (rebuildRows) {
            commandBuilder.clear(CLASS_LIST);
        }

        for (int index = 0; index < startingClasses.size(); index++) {
            ClassDefinition classDefinition = startingClasses.get(index);
            String selector = CLASS_LIST + "[" + index + "]";
            if (rebuildRows) {
                commandBuilder.append(CLASS_LIST, CLASS_ENTRY_ASSET);
            }

            boolean selected = classDefinition.getId().equalsIgnoreCase(selectedClassId);
            commandBuilder.set(selector + ".Visible", true);
            commandBuilder.set(selector + " #SelectedBar.Visible", selected);
            commandBuilder.set(selector + " #TitleLabel.Text", displayName(classDefinition));
            commandBuilder.set(selector + " #SubtitleLabel.Text", formatRoles(classDefinition));
            commandBuilder.set(selector + " #MetaLabel.Text", classDefinition.getId());

            if (rebuildRows && eventBuilder != null) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        new EventData().append("Action", "Focus").append("ClassId", classDefinition.getId()),
                        false);
            }
        }

        commandBuilder.set(SELECTED_CLASS_ID_FIELD + ".Value", selectedClassId);
        commandBuilder.set(CONFIRM_BUTTON + ".Visible", !selectedClassId.isBlank());

        ClassDefinition selectedClass = startingClasses.stream()
                .filter(def -> def.getId().equalsIgnoreCase(selectedClassId))
                .findFirst()
                .orElse(null);

        if (selectedClass == null) {
            commandBuilder.set(HERO_NAME_LABEL + ".Text", "");
            commandBuilder.set(HERO_META_LABEL + ".Text", "");
            commandBuilder.setObject(DETAILS_LABEL + ".Text",
                    LocalizableString.fromMessageId("pixelbays.rpg.class.onboarding.noDescription", null));
            return;
        }

        commandBuilder.set(HERO_NAME_LABEL + ".Text", displayName(selectedClass));
        commandBuilder.set(HERO_META_LABEL + ".Text", formatRoles(selectedClass));
        commandBuilder.set(DETAILS_LABEL + ".Text", description(selectedClass));
    }

    @Nonnull
    private String displayName(@Nonnull ClassDefinition classDefinition) {
        String displayName = classDefinition.getDisplayName();
        return displayName == null || displayName.isBlank() ? classDefinition.getId() : displayName;
    }

    @Nonnull
    private String description(@Nonnull ClassDefinition classDefinition) {
        String description = classDefinition.getDescription();
        if (description == null || description.isBlank()) {
            return "No description configured for this class yet.";
        }
        return description;
    }

    @Nonnull
    private String formatRoles(@Nonnull ClassDefinition classDefinition) {
        List<String> roles = classDefinition.getRoles();
        if (roles == null || roles.isEmpty()) {
            return classDefinition.getId();
        }
        return String.join(" • ", roles);
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
    private static String toResolvedText(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();
        if (formatted.rawText == null || formatted.rawText.isBlank()) {
            return null;
        }
        return formatted.rawText;
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