package org.pixelbays.rpg.hud;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class ResourceBarsHudModule implements PlayerHudModule {

    private static final String SELECTOR_RESOURCE_HOST = "#ResourceBars";
    public static final int RESOURCE_ROOT_TOP = 10;
    public static final int RESOURCE_ROOT_LEFT = 750;
    public static final int RESOURCE_ROOT_WIDTH = ProgressionHudModule.BAR_WIDTH;
    public static final int RESOURCE_ROOT_BOTTOM = 150;

    public static final int RESOURCE_BAR_HEIGHT = 12;
    public static final int RESOURCE_BAR_GAP = 4;
    public static final int RESOURCE_CHARGE_GAP = 3;
    public static final int RESOURCE_CHARGE_ASSET_SIZE = RESOURCE_BAR_HEIGHT;

    public static final int RESOURCE_LABEL_WIDTH = 72;
    public static final int RESOURCE_LABEL_GAP = 6;
    public static final int RESOURCE_TRACK_WIDTH = ProgressionHudModule.BAR_WIDTH - RESOURCE_LABEL_WIDTH - RESOURCE_LABEL_GAP;

    private final PlayerHud hud;

    private String lastLayoutSignature = "";
    private List<ResourceBarData> lastLayout = Collections.emptyList();
    private int[] lastStates = new int[0];

    ResourceBarsHudModule(@Nonnull PlayerHud hud) {
        this.hud = hud;
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd) {
        if (!lastLayout.isEmpty()) {
            rebuildResourceBars(cmd, lastLayout);
        }
    }

    public void prime(@Nonnull List<ResourceBarData> bars) {
        if (bars.isEmpty()) {
            lastLayoutSignature = "";
            lastLayout = Collections.emptyList();
            lastStates = new int[0];
            return;
        }

        lastLayoutSignature = buildLayoutSignature(bars);
        lastLayout = List.copyOf(bars);
        lastStates = buildStateSnapshot(lastLayout);
    }

    public void update(@Nonnull List<ResourceBarData> bars) {
        if (bars.isEmpty()) {
            if (lastLayoutSignature.isEmpty()) {
                return;
            }

            lastLayoutSignature = "";
            lastLayout = Collections.emptyList();
            lastStates = new int[0];

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear(SELECTOR_RESOURCE_HOST);
            hud.getProgressionModule().syncRootHeight(cmd);
            hud.applyModuleUpdate(cmd);
            return;
        }

        String layoutSignature = buildLayoutSignature(bars);
        boolean layoutChanged = !layoutSignature.equals(lastLayoutSignature);
        if (layoutChanged) {
            lastLayoutSignature = layoutSignature;
            lastLayout = List.copyOf(bars);
            lastStates = buildStateSnapshot(lastLayout);

            UICommandBuilder cmd = new UICommandBuilder();
            rebuildResourceBars(cmd, lastLayout);
            hud.getProgressionModule().syncRootHeight(cmd);
            hud.applyModuleUpdate(cmd);
            return;
        }

        if (lastStates.length != bars.size()) {
            lastStates = new int[bars.size()];
            for (int i = 0; i < lastStates.length; i++) {
                lastStates[i] = -1;
            }
        }

        UICommandBuilder cmd = null;
        for (int i = 0; i < bars.size(); i++) {
            ResourceBarData bar = bars.get(i);
            switch (bar.displayMode) {
                case CHARGES -> {
                    int filledCharges = clampChargeCount(bar.currentCharges, bar.maxCharges);
                    if (i < lastStates.length && filledCharges == lastStates[i]) {
                        continue;
                    }

                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }

                    lastStates[i] = filledCharges;
                    applyResourceChargeFillState(cmd, i, bar.maxCharges, filledCharges, usesChargeAsset(bar));
                }
                case BAR -> {
                    int fillWidth = Math.round(RESOURCE_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio));
                    if (i < lastStates.length && fillWidth == lastStates[i]) {
                        continue;
                    }

                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }

                    lastStates[i] = fillWidth;
                    applyResourceFillWidth(cmd, i, fillWidth);
                }
            }
        }

        if (cmd != null) {
            hud.applyModuleUpdate(cmd);
        }
    }

    int computeRootHeight() {
        if (lastLayout.isEmpty()) {
            return ProgressionHudModule.ROOT_BASE_HEIGHT;
        }

        return ProgressionHudModule.ROOT_BASE_HEIGHT + (lastLayout.size() * (RESOURCE_BAR_HEIGHT + RESOURCE_BAR_GAP));
    }

    void appendDebugUi(@Nonnull StringBuilder ui) {
        ui.append("Group #ResourceBarsRoot {\n");
        ui.append("  Anchor: (Top: ").append(RESOURCE_ROOT_TOP)
                .append(", Left: ").append(RESOURCE_ROOT_LEFT)
                .append(", Width: ").append(RESOURCE_ROOT_WIDTH)
                .append(", Bottom: ").append(RESOURCE_ROOT_BOTTOM)
                .append(");\n\n");
        ui.append("  Group #ResourceBars {\n");
        ui.append("    LayoutMode: Bottom;\n");
        ui.append("    Anchor: (Full: 0);\n");
        if (!lastLayout.isEmpty()) {
            ui.append('\n');
            HudModuleSupport.appendIndentedBlock(ui, buildResourceBarsMarkup(lastLayout), 4);
        }
        ui.append("  }\n");
        ui.append("}\n\n");
    }

    public enum ResourceDisplayMode {
        BAR,
        CHARGES
    }

    public static final class ResourceBarData {
        @Nonnull
        public final String statId;
        @Nonnull
        public final String label;
        @Nonnull
        public final ResourceDisplayMode displayMode;
        @Nonnull
        public final String fillColor;
        @Nonnull
        public final String assetPath;
        public final float ratio;
        public final int currentCharges;
        public final int maxCharges;

        public ResourceBarData(
                @Nonnull String statId,
                @Nonnull String label,
                @Nonnull ResourceDisplayMode displayMode,
                @Nonnull String fillColor,
                @Nonnull String assetPath,
                float ratio,
                int currentCharges,
                int maxCharges) {
            this.statId = Objects.requireNonNull(statId, "statId");
            this.label = Objects.requireNonNull(label, "label");
            this.displayMode = Objects.requireNonNull(displayMode, "displayMode");
            this.fillColor = Objects.requireNonNull(fillColor, "fillColor");
            this.assetPath = Objects.requireNonNull(assetPath, "assetPath");
            this.ratio = ratio;
            this.currentCharges = currentCharges;
            this.maxCharges = maxCharges;
        }
    }

    private static void rebuildResourceBars(@Nonnull UICommandBuilder cmd, @Nonnull List<ResourceBarData> bars) {
        cmd.clear(SELECTOR_RESOURCE_HOST);

        for (int i = 0; i < bars.size(); i++) {
            StringBuilder ui = new StringBuilder(1024);
            appendResourceBarMarkup(ui, i, bars.get(i));
            cmd.appendInline(SELECTOR_RESOURCE_HOST, ui.toString());
        }
    }

    @Nonnull
    private static String buildResourceBarsMarkup(@Nonnull List<ResourceBarData> bars) {
        StringBuilder ui = new StringBuilder(Math.max(1024, bars.size() * 1024));
        for (int i = 0; i < bars.size(); i++) {
            appendResourceBarMarkup(ui, i, bars.get(i));
        }
        return ui.toString();
    }

    private static void appendResourceBarMarkup(@Nonnull StringBuilder ui, int index, @Nonnull ResourceBarData bar) {
        boolean useChargeAsset = usesChargeAsset(bar);
        int initialFillWidth = Math.round(RESOURCE_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio));
        int trackWidth = bar.displayMode == ResourceDisplayMode.CHARGES && useChargeAsset
                ? computeChargeTrackWidth(bar.maxCharges, true)
                : RESOURCE_TRACK_WIDTH;

        ui.append("Group #ResBar").append(index).append(" {\n");
        ui.append("  LayoutMode: Left;\n");
        ui.append("  Anchor: (Width: 296, Height: ").append(RESOURCE_BAR_HEIGHT)
                .append(", Bottom: ").append(RESOURCE_BAR_GAP).append(");\n\n");

        ui.append("  Label #ResLabel").append(index).append(" {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(bar.label)).append("\";\n");
        ui.append("    Anchor: (Width: ").append(RESOURCE_LABEL_WIDTH)
                .append(", Height: ").append(RESOURCE_BAR_HEIGHT)
                .append(", Right: ").append(RESOURCE_LABEL_GAP).append(");\n");
        ui.append("    Style: (FontSize: 10, RenderBold: true);\n");
        ui.append("  }\n\n");

        ui.append("  Group #ResTrack").append(index).append(" {\n");
        ui.append("    Anchor: (Width: ").append(trackWidth)
                .append(", Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
        if (!useChargeAsset) {
            ui.append("    Background: PatchStyle(Color: #000000(0.35));\n");
            ui.append("    OutlineColor: #000000(0.5);\n");
            ui.append("    OutlineSize: 1;\n");
        }

        if (bar.displayMode == ResourceDisplayMode.CHARGES) {
            int safeMaxCharges = Math.max(1, bar.maxCharges);
            int chargeWidth = computeChargeSegmentWidth(safeMaxCharges, useChargeAsset);
            int filledCharges = clampChargeCount(bar.currentCharges, bar.maxCharges);
            ui.append("    LayoutMode: Left;\n\n");
            for (int chargeIndex = 0; chargeIndex < safeMaxCharges; chargeIndex++) {
                ui.append("    Group #").append(resourceChargeSlotId(index, chargeIndex)).append(" {\n");
                ui.append("      Anchor: (Width: ").append(chargeWidth)
                        .append(", Height: ").append(RESOURCE_BAR_HEIGHT);
                if (chargeIndex < safeMaxCharges - 1) {
                    ui.append(", Right: ").append(RESOURCE_CHARGE_GAP);
                }
                ui.append(");\n");
                if (!useChargeAsset) {
                    ui.append("      Background: PatchStyle(Color: #000000(0.45));\n");
                    ui.append("      OutlineColor: #000000(0.65);\n");
                    ui.append("      OutlineSize: 1;\n\n");
                } else {
                    ui.append("\n");
                }

                ui.append("      Group #").append(resourceChargeFillId(index, chargeIndex)).append(" {\n");
                if (useChargeAsset) {
                    ui.append("        Background: (TexturePath: \"")
                            .append(HudModuleSupport.escapeUiString(bar.assetPath))
                            .append("\");\n");
                } else {
                    ui.append("        Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.90));\n");
                }
                ui.append("        Anchor: (Left: 0, Top: 0, Width: ")
                        .append(chargeIndex < filledCharges ? chargeWidth : 0)
                        .append(", Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
                ui.append("      }\n");
                ui.append("    }\n");
            }
        } else {
            ui.append("\n");
            ui.append("    Group #ResFill").append(index).append(" {\n");
            ui.append("      Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.85));\n");
            ui.append("      Anchor: (Left: 0, Top: 0, Width: ").append(initialFillWidth)
                    .append(", Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
            ui.append("    }\n");
        }

        ui.append("  }\n");
        ui.append("}\n");
    }

    private static void applyResourceFillWidth(@Nonnull UICommandBuilder cmd, int index, int fillWidth) {
        int clamped = Math.max(0, Math.min(RESOURCE_TRACK_WIDTH, fillWidth));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setTop(Value.of(0));
        fillAnchor.setWidth(Value.of(clamped));
        fillAnchor.setHeight(Value.of(RESOURCE_BAR_HEIGHT));
        cmd.setObject("#ResFill" + index + ".Anchor", fillAnchor);
    }

    private static void applyResourceChargeFillState(
            @Nonnull UICommandBuilder cmd,
            int resourceIndex,
            int maxCharges,
            int filledCharges,
            boolean useChargeAsset) {
        int safeMaxCharges = Math.max(1, maxCharges);
        int safeFilledCharges = clampChargeCount(filledCharges, safeMaxCharges);
        int chargeWidth = computeChargeSegmentWidth(safeMaxCharges, useChargeAsset);

        for (int chargeIndex = 0; chargeIndex < safeMaxCharges; chargeIndex++) {
            Anchor fillAnchor = new Anchor();
            fillAnchor.setLeft(Value.of(0));
            fillAnchor.setTop(Value.of(0));
            fillAnchor.setWidth(Value.of(chargeIndex < safeFilledCharges ? chargeWidth : 0));
            fillAnchor.setHeight(Value.of(RESOURCE_BAR_HEIGHT));
            cmd.setObject(resourceChargeFillSelector(resourceIndex, chargeIndex), fillAnchor);
        }
    }

    @Nonnull
    private static String resourceChargeSlotId(int resourceIndex, int chargeIndex) {
        return "ResChargeSlot" + resourceIndex + "C" + chargeIndex;
    }

    @Nonnull
    private static String resourceChargeFillId(int resourceIndex, int chargeIndex) {
        return "ResChargeFill" + resourceIndex + "C" + chargeIndex;
    }

    @Nonnull
    private static String resourceChargeFillSelector(int resourceIndex, int chargeIndex) {
        return "#" + resourceChargeFillId(resourceIndex, chargeIndex) + ".Anchor";
    }

    private static int computeChargeSegmentWidth(int maxCharges, boolean useChargeAsset) {
        int safeMaxCharges = Math.max(1, maxCharges);
        if (useChargeAsset) {
            return RESOURCE_CHARGE_ASSET_SIZE;
        }
        int totalGapWidth = Math.max(0, safeMaxCharges - 1) * RESOURCE_CHARGE_GAP;
        return Math.max(1, (RESOURCE_TRACK_WIDTH - totalGapWidth) / safeMaxCharges);
    }

    private static int computeChargeTrackWidth(int maxCharges, boolean useChargeAsset) {
        int safeMaxCharges = Math.max(1, maxCharges);
        return (safeMaxCharges * computeChargeSegmentWidth(safeMaxCharges, useChargeAsset))
                + (Math.max(0, safeMaxCharges - 1) * RESOURCE_CHARGE_GAP);
    }

    private static int clampChargeCount(int currentCharges, int maxCharges) {
        return Math.max(0, Math.min(Math.max(1, maxCharges), currentCharges));
    }

    @Nonnull
    private static int[] buildStateSnapshot(@Nonnull List<ResourceBarData> bars) {
        int[] states = new int[bars.size()];
        for (int i = 0; i < bars.size(); i++) {
            ResourceBarData bar = bars.get(i);
            states[i] = bar.displayMode == ResourceDisplayMode.CHARGES
                    ? clampChargeCount(bar.currentCharges, bar.maxCharges)
                    : Math.round(RESOURCE_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio));
        }
        return states;
    }

    @Nonnull
    private static String buildLayoutSignature(@Nonnull List<ResourceBarData> bars) {
        StringBuilder signature = new StringBuilder(bars.size() * 64);
        for (ResourceBarData bar : bars) {
            signature.append(bar.statId)
                    .append('|')
                    .append(bar.label)
                    .append('|')
                    .append(bar.displayMode)
                    .append('|')
                    .append(bar.maxCharges)
                    .append('|')
                    .append(bar.fillColor)
                    .append('|')
                    .append(bar.assetPath)
                    .append(';');
        }
        return signature.toString();
    }

    private static boolean usesChargeAsset(@Nonnull ResourceBarData bar) {
        return bar.displayMode == ResourceDisplayMode.CHARGES && !bar.assetPath.isBlank();
    }
}