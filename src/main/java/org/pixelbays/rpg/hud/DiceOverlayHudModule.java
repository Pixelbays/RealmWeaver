package org.pixelbays.rpg.hud;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class DiceOverlayHudModule implements PlayerHudModule {

    private static final String SELECTOR_DICE_SMALL_HOST = "#DiceOverlaySmallHost";
    private static final String SELECTOR_DICE_LARGE_HOST = "#DiceOverlayLargeHost";

    private final PlayerHud hud;

    private String lastSignature = "";
    private DiceRollViewData lastViewData = null;

    DiceOverlayHudModule(@Nonnull PlayerHud hud) {
        this.hud = hud;
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd) {
        if (lastViewData != null && !lastSignature.isEmpty()) {
            cmd.appendInline(
                    lastViewData.mode == DiceOverlayMode.LARGE ? SELECTOR_DICE_LARGE_HOST : SELECTOR_DICE_SMALL_HOST,
                    buildDiceOverlayMarkup(lastViewData));
        }
    }

    public void prime(@Nullable DiceRollViewData data) {
        if (data == null || data.diceValues.length == 0) {
            lastSignature = "";
            lastViewData = null;
            return;
        }

        lastSignature = buildSignature(data);
        lastViewData = data;
    }

    public void update(@Nullable DiceRollViewData data) {
        if (data == null || data.diceValues.length == 0) {
            if (lastSignature.isEmpty()) {
                return;
            }

            lastSignature = "";
            lastViewData = null;
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear(SELECTOR_DICE_SMALL_HOST);
            cmd.clear(SELECTOR_DICE_LARGE_HOST);
            hud.applyModuleUpdate(cmd);
            return;
        }

        String signature = buildSignature(data);
        if (HudModuleSupport.safeEquals(signature, lastSignature)) {
            return;
        }

        lastSignature = signature;
        lastViewData = data;

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.clear(SELECTOR_DICE_SMALL_HOST);
        cmd.clear(SELECTOR_DICE_LARGE_HOST);
        cmd.appendInline(
                data.mode == DiceOverlayMode.LARGE ? SELECTOR_DICE_LARGE_HOST : SELECTOR_DICE_SMALL_HOST,
                buildDiceOverlayMarkup(data));
        hud.applyModuleUpdate(cmd);
    }

    public enum DiceOverlayMode {
        SMALL,
        LARGE
    }

    public static final class DiceRollViewData {
        @Nonnull
        public final DiceOverlayMode mode;
        @Nonnull
        public final String notation;
        @Nonnull
        public final String summary;
        @Nonnull
        public final int[] diceValues;
        public final boolean rolling;

        public DiceRollViewData(
                @Nonnull DiceOverlayMode mode,
                @Nonnull String notation,
                @Nonnull String summary,
                @Nonnull int[] diceValues,
                boolean rolling) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.notation = Objects.requireNonNull(notation, "notation");
            this.summary = Objects.requireNonNull(summary, "summary");
            this.diceValues = Objects.requireNonNull(diceValues, "diceValues").clone();
            this.rolling = rolling;
        }
    }

    @Nonnull
    private static String buildSignature(@Nonnull DiceRollViewData data) {
        StringBuilder signature = new StringBuilder(64 + (data.diceValues.length * 4));
        signature.append(data.mode)
                .append('|')
                .append(data.notation)
                .append('|')
                .append(data.summary)
                .append('|')
                .append(data.rolling);
        for (int value : data.diceValues) {
            signature.append('|').append(value);
        }
        return signature.toString();
    }

    @Nonnull
    private static String buildDiceOverlayMarkup(@Nonnull DiceRollViewData data) {
        boolean large = data.mode == DiceOverlayMode.LARGE;
        int cardWidth = large ? 420 : 220;
        int cardPadding = large ? 16 : 10;
        int titleHeight = large ? 22 : 12;
        int titleBottom = large ? 8 : 4;
        int totalHeight = large ? 56 : 26;
        int totalBottom = large ? 12 : 6;
        int dieSize = large ? 52 : 28;
        int dieGap = large ? 8 : 6;
        int dieRowWidth = computeDiceRowWidth(data.diceValues.length, dieSize, dieGap);
        int cardHeight = (cardPadding * 2) + titleHeight + titleBottom + totalHeight + totalBottom + dieSize;
        String idPrefix = large ? "LargeDice" : "SmallDice";
        String backgroundColor = large ? "#16120D(0.88)" : "#101722(0.84)";
        String outlineColor = large ? "#D9B468(0.95)" : "#7FA7D6(0.90)";
        String dieBackgroundColor = data.rolling ? "#E8BF63(0.18)" : "#F2E2B7(0.18)";
        String dieOutlineColor = data.rolling ? "#D9B468(0.88)" : "#E6D3A6(0.80)";

        StringBuilder ui = new StringBuilder(2048 + (data.diceValues.length * 220));
        ui.append("Group #").append(idPrefix).append("Card {\n");
        ui.append("  LayoutMode: Top;\n");
        ui.append("  Anchor: (Width: ").append(cardWidth).append(", Height: ").append(cardHeight).append(");\n");
        ui.append("  Padding: (Full: ").append(cardPadding).append(");\n");
        ui.append("  Background: PatchStyle(Color: ").append(backgroundColor).append(");\n");
        ui.append("  OutlineColor: ").append(outlineColor).append(";\n");
        ui.append("  OutlineSize: 1;\n\n");

        ui.append("  Label #").append(idPrefix).append("Notation {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(data.notation)).append("\";\n");
        ui.append("    Anchor: (Height: ").append(titleHeight).append(", Bottom: ").append(titleBottom).append(");\n");
        ui.append("    Style: (FontSize: ").append(large ? 18 : 10)
                .append(", RenderBold: true, HorizontalAlignment: Center, LetterSpacing: ")
                .append(large ? "1.2" : "0.6").append(");\n");
        ui.append("  }\n\n");

        ui.append("  Label #").append(idPrefix).append("Summary {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(data.summary)).append("\";\n");
        ui.append("    Anchor: (Height: ").append(totalHeight).append(", Bottom: ").append(totalBottom).append(");\n");
        ui.append("    Style: (FontSize: ").append(large ? 42 : 18)
                .append(", RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center);\n");
        ui.append("  }\n\n");

        ui.append("  Group {\n");
        ui.append("    LayoutMode: Center;\n");
        ui.append("    Anchor: (Height: ").append(dieSize).append(");\n\n");

        ui.append("    Group #").append(idPrefix).append("Row {\n");
        ui.append("      LayoutMode: Left;\n");
        ui.append("      Anchor: (Width: ").append(dieRowWidth).append(", Height: ").append(dieSize).append(");\n\n");

        for (int i = 0; i < data.diceValues.length; i++) {
            ui.append("      Group #").append(idPrefix).append("Die").append(i).append(" {\n");
            ui.append("        Anchor: (Width: ").append(dieSize).append(", Height: ").append(dieSize);
            if (i < data.diceValues.length - 1) {
                ui.append(", Right: ").append(dieGap);
            }
            ui.append(");\n");
            ui.append("        Background: PatchStyle(Color: ").append(dieBackgroundColor).append(");\n");
            ui.append("        OutlineColor: ").append(dieOutlineColor).append(";\n");
            ui.append("        OutlineSize: 1;\n\n");
            ui.append("        Group {\n");
            ui.append("          LayoutMode: CenterMiddle;\n");
            ui.append("          Anchor: (Width: ").append(dieSize).append(", Height: ").append(dieSize).append(");\n\n");
            ui.append("          Label {\n");
            ui.append("            Text: \"").append(data.diceValues[i]).append("\";\n");
            ui.append("            Anchor: (Width: ").append(dieSize).append(", Height: ").append(dieSize).append(");\n");
            ui.append("            Style: (FontSize: ").append(large ? 26 : 15)
                    .append(", RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center);\n");
            ui.append("          }\n");
            ui.append("        }\n");
            ui.append("      }\n");
        }

        ui.append("    }\n");
        ui.append("  }\n");
        ui.append("}\n");
        return ui.toString();
    }

    private static int computeDiceRowWidth(int diceCount, int dieSize, int dieGap) {
        int safeDiceCount = Math.max(1, diceCount);
        return (safeDiceCount * dieSize) + (Math.max(0, safeDiceCount - 1) * dieGap);
    }
}