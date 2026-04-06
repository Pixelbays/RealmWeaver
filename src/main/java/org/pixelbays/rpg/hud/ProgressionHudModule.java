package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class ProgressionHudModule implements PlayerHudModule {

    public static final int BAR_WIDTH = 296;
    public static final int BAR_HEIGHT = 18;

    public static final int ROOT_TOP = 190;
    public static final int ROOT_LEFT = 20;
    public static final int ROOT_WIDTH = 320;
    public static final int ROOT_BASE_HEIGHT = 60;

    private static final String SELECTOR_FILL_ANCHOR = "#XpFill.Anchor";
    private static final String SELECTOR_FILL_BACKGROUND = "#XpFill.Background";
    private static final String SELECTOR_TEXT = "#XpText.Text";
    private static final String SELECTOR_ROOT_ANCHOR = "#Root.Anchor";

    private final PlayerHud hud;

    private int lastFillWidth = -1;
    private String lastLabelPrefix = null;
    private int lastLevel = Integer.MIN_VALUE;
    private int lastCurrent = Integer.MIN_VALUE;
    private int lastNext = Integer.MIN_VALUE;
    private int lastRemaining = Integer.MIN_VALUE;
    @Nonnull
    private String lastFillColor = PlayerHudServiceSupport.DEFAULT_PROGRESSION_FILL_COLOR;
    private boolean lastMax = false;
    private boolean progressHidden = false;

    ProgressionHudModule(@Nonnull PlayerHud hud) {
        this.hud = hud;
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd) {
        if (progressHidden) {
            applyHiddenRoot(cmd);
            applyFillWidth(cmd, 0);
            cmd.set(SELECTOR_TEXT, "");
            return;
        }

        applyRootHeight(cmd, hud.getResourceBarsModule().computeRootHeight());
        applyFillColor(cmd, lastFillColor);

        if (lastMax) {
            applyFillWidth(cmd, BAR_WIDTH);
            cmd.set(SELECTOR_TEXT, lastLabelPrefix + " Lv " + lastLevel + " - XP: MAX");
            return;
        }

        if (lastLabelPrefix == null) {
            applyFillWidth(cmd, 0);
            cmd.set(SELECTOR_TEXT, "");
            return;
        }

        applyFillWidth(cmd, lastFillWidth);
        cmd.set(SELECTOR_TEXT,
                lastLabelPrefix + " Lv " + lastLevel + " - XP: " + lastCurrent + "/" + lastNext + " ("
                        + lastRemaining + " to next)");
    }

    public void primeMax(@Nonnull String labelPrefix, int level, @Nonnull String fillColor) {
        progressHidden = false;
        lastMax = true;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = BAR_WIDTH;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;
        lastFillColor = fillColor;
    }

    public void primeProgress(
            @Nonnull String labelPrefix,
            int level,
            float ratio,
            int current,
            int next,
            int remaining,
            @Nonnull String fillColor) {
        progressHidden = false;
        lastMax = false;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = Math.round(BAR_WIDTH * HudModuleSupport.clampRatio(ratio));
        lastCurrent = current;
        lastNext = next;
        lastRemaining = remaining;
        lastFillColor = fillColor;
    }

    public void primeHide() {
        progressHidden = true;
        lastMax = false;
        lastLabelPrefix = null;
        lastLevel = Integer.MIN_VALUE;
        lastFillWidth = -1;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;
    }

    public void updateMax(@Nonnull String labelPrefix, int level, @Nonnull String fillColor) {
        if (lastMax
                && HudModuleSupport.safeEquals(labelPrefix, lastLabelPrefix)
                && level == lastLevel
                && HudModuleSupport.safeEquals(fillColor, lastFillColor)) {
            return;
        }

        lastMax = true;
        progressHidden = false;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = BAR_WIDTH;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;
        lastFillColor = fillColor;

        UICommandBuilder cmd = new UICommandBuilder();
        ensureProgressVisible(cmd);
        applyFillColor(cmd, fillColor);
        applyFillWidth(cmd, BAR_WIDTH);
        cmd.set(SELECTOR_TEXT, labelPrefix + " Lv " + level + " - XP: MAX");
        hud.applyModuleUpdate(cmd);
    }

    public void updateProgress(
            @Nonnull String labelPrefix,
            int level,
            float ratio,
            int current,
            int next,
            int remaining,
            @Nonnull String fillColor) {
        int fillWidth = Math.round(BAR_WIDTH * HudModuleSupport.clampRatio(ratio));

        if (!lastMax
                && HudModuleSupport.safeEquals(labelPrefix, lastLabelPrefix)
                && level == lastLevel
                && fillWidth == lastFillWidth
                && current == lastCurrent
                && next == lastNext
                && remaining == lastRemaining
                && HudModuleSupport.safeEquals(fillColor, lastFillColor)) {
            return;
        }

        lastMax = false;
        progressHidden = false;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = fillWidth;
        lastCurrent = current;
        lastNext = next;
        lastRemaining = remaining;
        lastFillColor = fillColor;

        UICommandBuilder cmd = new UICommandBuilder();
        ensureProgressVisible(cmd);
        applyFillColor(cmd, fillColor);
        applyFillWidth(cmd, fillWidth);
        cmd.set(SELECTOR_TEXT,
                labelPrefix + " Lv " + level + " - XP: " + current + "/" + next + " (" + remaining + " to next)");
        hud.applyModuleUpdate(cmd);
    }

    public void hide() {
        if (progressHidden) {
            return;
        }

        progressHidden = true;
        lastMax = false;
        lastLabelPrefix = null;
        lastLevel = Integer.MIN_VALUE;
        lastFillWidth = -1;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;

        UICommandBuilder cmd = new UICommandBuilder();
        applyHiddenRoot(cmd);
        applyFillWidth(cmd, 0);
        cmd.set(SELECTOR_TEXT, "");
        hud.applyModuleUpdate(cmd);
    }

    void syncRootHeight(@Nonnull UICommandBuilder cmd) {
        if (progressHidden) {
            return;
        }

        applyRootHeight(cmd, hud.getResourceBarsModule().computeRootHeight());
    }

    void appendDebugUi(@Nonnull StringBuilder ui) {
        int rootHeight = progressHidden ? 0 : hud.getResourceBarsModule().computeRootHeight();
        int fillWidth = progressHidden ? 0 : Math.max(0, Math.min(BAR_WIDTH, lastMax ? BAR_WIDTH : Math.max(0, lastFillWidth)));

        ui.append("Group #Root {\n");
        if (progressHidden) {
            ui.append("  Anchor: (Top: -10000, Left: -10000, Width: 0, Height: 0);\n");
        } else {
            ui.append("  Anchor: (Top: ").append(ROOT_TOP)
                    .append(", Left: ").append(ROOT_LEFT)
                    .append(", Width: ").append(ROOT_WIDTH)
                    .append(", Height: ").append(Math.max(ROOT_BASE_HEIGHT, rootHeight)).append(");\n");
        }
        ui.append("  LayoutMode: Top;\n");
        ui.append("  Padding: (Horizontal: 12, Vertical: 8);\n");
        ui.append("  Background: #000000(0.2);\n\n");

        ui.append("  Label #XpText {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(buildCurrentText())).append("\";\n");
        ui.append("    Style: (...$C.@DefaultLabelStyle, FontSize: 12, RenderBold: true);\n");
        ui.append("    Anchor: (Height: 16, Bottom: 6);\n");
        ui.append("  }\n\n");

        ui.append("  Group #XpBar {\n");
        ui.append("    Anchor: (Width: ").append(BAR_WIDTH).append(", Height: ").append(BAR_HEIGHT).append(");\n");
        ui.append("    Background: PatchStyle(Color: #000000(0.35));\n");
        ui.append("    OutlineColor: #000000(0.5);\n");
        ui.append("    OutlineSize: 1;\n\n");

        ui.append("    Group #XpFill {\n");
        ui.append("      Background: PatchStyle(Color: ").append(lastFillColor).append(");\n");
        ui.append("      Anchor: (Left: 0, Top: 0, Width: ").append(fillWidth)
                .append(", Height: ").append(BAR_HEIGHT).append(");\n");
        ui.append("    }\n");
        ui.append("  }\n");
        ui.append("}\n");
    }

    private void ensureProgressVisible(@Nonnull UICommandBuilder cmd) {
        if (!progressHidden) {
            return;
        }

        progressHidden = false;
        applyRootHeight(cmd, hud.getResourceBarsModule().computeRootHeight());
    }

    private static void applyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(ROOT_TOP));
        rootAnchor.setLeft(Value.of(ROOT_LEFT));
        rootAnchor.setWidth(Value.of(ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(ROOT_BASE_HEIGHT, height)));
        cmd.setObject(SELECTOR_ROOT_ANCHOR, rootAnchor);
    }

    private static void applyHiddenRoot(@Nonnull UICommandBuilder cmd) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(-10_000));
        rootAnchor.setLeft(Value.of(-10_000));
        rootAnchor.setWidth(Value.of(0));
        rootAnchor.setHeight(Value.of(0));
        cmd.setObject(SELECTOR_ROOT_ANCHOR, rootAnchor);
    }

    private static void applyFillColor(@Nonnull UICommandBuilder cmd, @Nonnull String fillColor) {
        cmd.set(SELECTOR_FILL_BACKGROUND, fillColor);
    }

    private static void applyFillWidth(@Nonnull UICommandBuilder cmd, int fillWidth) {
        int clamped = Math.max(0, Math.min(BAR_WIDTH, fillWidth));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setTop(Value.of(0));
        fillAnchor.setWidth(Value.of(clamped));
        fillAnchor.setHeight(Value.of(BAR_HEIGHT));
        cmd.setObject(SELECTOR_FILL_ANCHOR, fillAnchor);
    }

    @Nonnull
    private String buildCurrentText() {
        if (progressHidden || lastLabelPrefix == null) {
            return "";
        }

        if (lastMax) {
            return lastLabelPrefix + " Lv " + lastLevel + " - XP: MAX";
        }

        return lastLabelPrefix + " Lv " + lastLevel + " - XP: " + lastCurrent + "/" + lastNext + " ("
                + lastRemaining + " to next)";
    }
}