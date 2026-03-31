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
    private static final String SELECTOR_TEXT = "#XpText.Text";
    private static final String SELECTOR_ROOT_ANCHOR = "#Root.Anchor";

    private final PlayerHud hud;

    private int lastFillWidth = -1;
    private String lastLabelPrefix = null;
    private int lastLevel = Integer.MIN_VALUE;
    private int lastCurrent = Integer.MIN_VALUE;
    private int lastNext = Integer.MIN_VALUE;
    private int lastRemaining = Integer.MIN_VALUE;
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

    public void primeMax(@Nonnull String labelPrefix, int level) {
        progressHidden = false;
        lastMax = true;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = BAR_WIDTH;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;
    }

    public void primeProgress(@Nonnull String labelPrefix, int level, float ratio, int current, int next, int remaining) {
        progressHidden = false;
        lastMax = false;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastFillWidth = Math.round(BAR_WIDTH * HudModuleSupport.clampRatio(ratio));
        lastCurrent = current;
        lastNext = next;
        lastRemaining = remaining;
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

    public void updateMax(@Nonnull String labelPrefix, int level) {
        if (lastMax && HudModuleSupport.safeEquals(labelPrefix, lastLabelPrefix) && level == lastLevel) {
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

        UICommandBuilder cmd = new UICommandBuilder();
        ensureProgressVisible(cmd);
        applyFillWidth(cmd, BAR_WIDTH);
        cmd.set(SELECTOR_TEXT, labelPrefix + " Lv " + level + " - XP: MAX");
        hud.applyModuleUpdate(cmd);
    }

    public void updateProgress(@Nonnull String labelPrefix, int level, float ratio, int current, int next, int remaining) {
        int fillWidth = Math.round(BAR_WIDTH * HudModuleSupport.clampRatio(ratio));

        if (!lastMax
                && HudModuleSupport.safeEquals(labelPrefix, lastLabelPrefix)
                && level == lastLevel
                && fillWidth == lastFillWidth
                && current == lastCurrent
                && next == lastNext
                && remaining == lastRemaining) {
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

        UICommandBuilder cmd = new UICommandBuilder();
        ensureProgressVisible(cmd);
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

    private static void applyFillWidth(@Nonnull UICommandBuilder cmd, int fillWidth) {
        int clamped = Math.max(0, Math.min(BAR_WIDTH, fillWidth));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setTop(Value.of(0));
        fillAnchor.setWidth(Value.of(clamped));
        fillAnchor.setHeight(Value.of(BAR_HEIGHT));
        cmd.setObject(SELECTOR_FILL_ANCHOR, fillAnchor);
    }
}