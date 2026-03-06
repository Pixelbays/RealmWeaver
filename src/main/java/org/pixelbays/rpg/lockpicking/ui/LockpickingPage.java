package org.pixelbays.rpg.lockpicking.ui;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.lockpicking.component.LockpickingSessionComponent;
import org.pixelbays.rpg.lockpicking.system.LockpickingSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class LockpickingPage extends CustomUIPage {

    private static final String SET_PIN_BUTTON = "#SetPinButton";
    private static final String CANCEL_BUTTON = "#CancelButton";

    public LockpickingPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/LockpickingPage.ui");
        bindEvents(eventBuilder);
        LockpickingSessionComponent session = store.getComponent(ref, LockpickingSessionComponent.getComponentType());
        if (session != null) {
            appendSessionData(commandBuilder, session);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        store.getExternalData().getWorld().execute(() -> handleAction(ref, store, action));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String action) {
        if ("SetPin".equals(action)) {
            LockpickingSystem.get().handlePinAttempt(ref, store);
            return;
        }

        if ("Cancel".equals(action)) {
            LockpickingSystem.get().cancelSession(ref, store);
        }
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, SET_PIN_BUTTON,
                new EventData().append("Action", "SetPin"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, CANCEL_BUTTON,
                new EventData().append("Action", "Cancel"));
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        store.getExternalData().getWorld().execute(() -> LockpickingSystem.get().cancelSession(ref, store));
    }

    public static void sendUpdate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull LockpickingSessionComponent session) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendSessionData(commandBuilder, session);
        // Send commands only — event bindings are registered once in build() and must not
        // be re-sent every tick, as that causes click events to be dropped mid-registration.
        player.getPageManager().updateCustomPage(
                new CustomPage(
                        LockpickingPage.class.getName(),
                        false,
                        false,
                        CustomPageLifetime.CanDismiss,
                        commandBuilder.getCommands(),
                        UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY));
    }

    private static void appendSessionData(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull LockpickingSessionComponent session) {
        final int TRACK_WIDTH = 520;
        final int MAX_PINS = 5;

        int pinCount = session.getPinCount();
        int currentPin = session.getCurrentPin();

        // --- Difficulty label ---
        commandBuilder.set("#DifficultyLabel.Text", session.getDifficultyTierId());

        // --- Pin indicators ---
        for (int i = 0; i < MAX_PINS; i++) {
            String outlineColor;
            String barColor;
            if (i >= pinCount) {
                // Unused slot
                outlineColor = "#1a1b2a";
                barColor = "#181a28";
            } else if (i < currentPin) {
                // Set / done
                outlineColor = "#3a7a4a";
                barColor = "#3a7a4a";
            } else if (i == currentPin) {
                // Active
                outlineColor = "#4a7aaa";
                barColor = "#3a5a9a";
            } else {
                // Upcoming
                outlineColor = "#2a2d45";
                barColor = "#2e3050";
            }
            commandBuilder.set("#Pin" + i + ".OutlineColor", outlineColor);
            commandBuilder.set("#Pin" + i + ".OutlineSize", 1.5f);
            commandBuilder.setObject("#Pin" + i + "Bar.Background", new PatchStyle().setColor(Value.of(barColor)));
        }

        // --- Needle & sweet-spot track ---
        int needleLeft = Math.round(session.getNeedlePosition() * TRACK_WIDTH) - 1;
        needleLeft = Math.max(0, Math.min(TRACK_WIDTH - 3, needleLeft));

        float sweetStart = session.getSweetSpotCenter() - session.getSweetSpotSize() * 0.5f;
        int sweetLeft = Math.round(sweetStart * TRACK_WIDTH);
        int sweetWidth = Math.round(session.getSweetSpotSize() * TRACK_WIDTH);
        sweetLeft = Math.max(0, Math.min(TRACK_WIDTH - 2, sweetLeft));
        sweetWidth = Math.max(2, Math.min(TRACK_WIDTH - sweetLeft, sweetWidth));

        Anchor sweetAnchor = new Anchor();
        sweetAnchor.setLeft(Value.of(sweetLeft));
        sweetAnchor.setWidth(Value.of(sweetWidth));
        sweetAnchor.setTop(Value.of(2));
        sweetAnchor.setBottom(Value.of(2));
        commandBuilder.setObject("#SweetSpot.Anchor", sweetAnchor);

        Anchor needleAnchor = new Anchor();
        needleAnchor.setLeft(Value.of(needleLeft));
        needleAnchor.setWidth(Value.of(3));
        needleAnchor.setTop(Value.of(0));
        needleAnchor.setBottom(Value.of(0));
        commandBuilder.setObject("#Needle.Anchor", needleAnchor);

        // --- Info row ---
        commandBuilder.set("#PinLabel.Text",
                String.format(Locale.US, "Pin %d/%d", Math.min(currentPin + 1, Math.max(pinCount, 1)), pinCount));
        commandBuilder.set("#MistakesLabel.Text",
                String.format(Locale.US, "Mistakes: %d/%d", session.getMistakes(), session.getMaxMistakes()));
        commandBuilder.set("#TimeLabel.Text",
                String.format(Locale.US, "%.1fs", Math.max(0f, session.getTimeRemainingSeconds())));
        commandBuilder.set("#LockpicksLabel.Text",
                String.format(Locale.US, "Picks: %d", session.getLockpickCount()));

        // --- Timer fill (width-based, track is 520px wide) ---
        final int TIMER_TRACK_WIDTH = 520;
        float totalTime = session.getTotalTimeLimitSeconds();
        float timerValue = totalTime > 0f ? Math.max(0f, Math.min(1f, session.getTimeRemainingSeconds() / totalTime)) : 0f;
        int fillWidth = Math.round(timerValue * TIMER_TRACK_WIDTH);
        Anchor timerAnchor = new Anchor();
        timerAnchor.setLeft(Value.of(0));
        timerAnchor.setTop(Value.of(0));
        timerAnchor.setBottom(Value.of(0));
        timerAnchor.setWidth(Value.of(fillWidth));
        commandBuilder.setObject("#TimerFill.Anchor", timerAnchor);
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
}
