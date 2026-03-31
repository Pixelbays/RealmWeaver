package org.pixelbays.rpg.hud;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.system.RNGSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class DiceOverlayHudServiceModule implements PlayerHudServiceModule {

    private static final long DICE_RESULT_HOLD_MILLIS = 900L;

    private final ConcurrentHashMap<UUID, ActiveDiceRoll> activeDiceRollByPlayerId = new ConcurrentHashMap<>();

    @Override
    public void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getDiceOverlayModule().prime(resolveDiceRoll(context.getPlayerRef().getUuid()));
    }

    @Override
    public void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getDiceOverlayModule().update(resolveDiceRoll(context.getPlayerRef().getUuid()));
    }

    @Override
    public void remove(@Nonnull UUID playerId) {
        activeDiceRollByPlayerId.remove(playerId);
    }

    public void showDiceRoll(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull DiceRollRequest request) {
        if (!ref.isValid()) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        ActiveDiceRoll activeRoll = ActiveDiceRoll.create(request, System.currentTimeMillis());
        if (activeRoll == null) {
            return;
        }

        activeDiceRollByPlayerId.put(playerRef.getUuid(), activeRoll);
    }

    @Nullable
    private DiceOverlayHudModule.DiceRollViewData resolveDiceRoll(@Nonnull UUID playerId) {
        ActiveDiceRoll activeRoll = activeDiceRollByPlayerId.get(playerId);
        if (activeRoll == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (now >= activeRoll.hideAtMillis) {
            activeDiceRollByPlayerId.remove(playerId, activeRoll);
            return null;
        }

        boolean rolling = now < activeRoll.revealAtMillis;
        int[] displayedDice = rolling ? activeRoll.rollAnimatedValues() : activeRoll.finalDiceValues.clone();
        int displayedTotal = sum(displayedDice) + activeRoll.modifier;
        String notation = activeRoll.notation + (rolling ? "..." : "");
        String summary = Integer.toString(displayedTotal);
        if (activeRoll.modifier != 0) {
            summary += " (" + formatSigned(activeRoll.modifier) + ")";
        }

        return new DiceOverlayHudModule.DiceRollViewData(activeRoll.mode, notation, summary, displayedDice, rolling);
    }

    private static int sum(@Nonnull int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    @Nonnull
    private static String formatSigned(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    public static final class DiceRollRequest {
        @Nonnull
        private final DiceOverlayHudModule.DiceOverlayMode mode;
        private final float rollTimeSeconds;
        @Nonnull
        private final String notation;
        @Nonnull
        private final int[] finalDiceValues;
        @Nonnull
        private final int[] diceMinimums;
        @Nonnull
        private final int[] diceMaximums;
        private final int modifier;
        private final int finalTotal;

        public DiceRollRequest(
                @Nonnull DiceOverlayHudModule.DiceOverlayMode mode,
                float rollTimeSeconds,
                @Nonnull String notation,
                @Nonnull int[] finalDiceValues,
                @Nonnull int[] diceMinimums,
                @Nonnull int[] diceMaximums,
                int modifier,
                int finalTotal) {
            this.mode = mode;
            this.rollTimeSeconds = rollTimeSeconds;
            this.notation = notation;
            this.finalDiceValues = finalDiceValues.clone();
            this.diceMinimums = diceMinimums.clone();
            this.diceMaximums = diceMaximums.clone();
            this.modifier = modifier;
            this.finalTotal = finalTotal;
        }
    }

    private static final class ActiveDiceRoll {
        @Nonnull
        private final DiceOverlayHudModule.DiceOverlayMode mode;
        @Nonnull
        private final String notation;
        @Nonnull
        private final int[] finalDiceValues;
        @Nonnull
        private final int[] diceMinimums;
        @Nonnull
        private final int[] diceMaximums;
        private final int modifier;
        @SuppressWarnings("unused")
        private final int finalTotal;
        private final long revealAtMillis;
        private final long hideAtMillis;

        @Nullable
        private static ActiveDiceRoll create(@Nonnull DiceRollRequest request, long nowMillis) {
            int resolvedCount = Math.min(
                    request.finalDiceValues.length,
                    Math.min(request.diceMinimums.length, request.diceMaximums.length));
            if (resolvedCount <= 0) {
                return null;
            }

            int[] finalDiceValues = Arrays.copyOf(request.finalDiceValues, resolvedCount);
            int[] diceMinimums = Arrays.copyOf(request.diceMinimums, resolvedCount);
            int[] diceMaximums = Arrays.copyOf(request.diceMaximums, resolvedCount);
            long revealAtMillis = nowMillis + Math.max(200L, Math.round(Math.max(0.2f, request.rollTimeSeconds) * 1000f));

            return new ActiveDiceRoll(
                    request.mode,
                    request.notation.isBlank() ? Integer.toString(request.finalTotal) : request.notation,
                    finalDiceValues,
                    diceMinimums,
                    diceMaximums,
                    request.modifier,
                    request.finalTotal,
                    revealAtMillis,
                    revealAtMillis + DICE_RESULT_HOLD_MILLIS);
        }

        private ActiveDiceRoll(
                @Nonnull DiceOverlayHudModule.DiceOverlayMode mode,
                @Nonnull String notation,
                @Nonnull int[] finalDiceValues,
                @Nonnull int[] diceMinimums,
                @Nonnull int[] diceMaximums,
                int modifier,
                int finalTotal,
                long revealAtMillis,
                long hideAtMillis) {
            this.mode = mode;
            this.notation = notation;
            this.finalDiceValues = finalDiceValues;
            this.diceMinimums = diceMinimums;
            this.diceMaximums = diceMaximums;
            this.modifier = modifier;
            this.finalTotal = finalTotal;
            this.revealAtMillis = revealAtMillis;
            this.hideAtMillis = hideAtMillis;
        }

        @Nonnull
        private int[] rollAnimatedValues() {
            int[] values = new int[this.finalDiceValues.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = RNGSystem.roll(this.diceMinimums[i], this.diceMaximums[i]);
            }
            return values;
        }
    }
}