package org.pixelbays.rpg.hud;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class XpBarHud extends CustomUIHud {

    public static final int BAR_WIDTH = 296;
    public static final int BAR_HEIGHT = 18;

    public static final int ROOT_TOP = 190;
    public static final int ROOT_LEFT = 20;
    public static final int ROOT_WIDTH = 320;
    public static final int ROOT_BASE_HEIGHT = 60;

    public static final int RESOURCE_BAR_HEIGHT = 12;
    public static final int RESOURCE_BAR_GAP = 4;
    public static final int RESOURCE_CHARGE_GAP = 3;
    public static final int RESOURCE_CHARGE_ASSET_SIZE = RESOURCE_BAR_HEIGHT;

    public static final int RESOURCE_LABEL_WIDTH = 72;
    public static final int RESOURCE_LABEL_GAP = 6;
    public static final int RESOURCE_TRACK_WIDTH = BAR_WIDTH - RESOURCE_LABEL_WIDTH - RESOURCE_LABEL_GAP;

    public static final int PARTY_ROOT_TOP = 36;
    public static final int PARTY_ROOT_LEFT = 20;
    public static final int PARTY_ROOT_WIDTH = 248;
    public static final int PARTY_ENTRY_WIDTH = 248;
    public static final int PARTY_ENTRY_INNER_WIDTH = 236;
    public static final int PARTY_ENTRY_PADDING = 6;
    public static final int PARTY_ENTRY_GAP = 6;
    public static final int PARTY_HEADER_HEIGHT = 16;
    public static final int PARTY_HEADER_GAP = 4;
    public static final int PARTY_BAR_LABEL_HEIGHT = 10;
    public static final int PARTY_BAR_TRACK_HEIGHT = 10;
    public static final int PARTY_BAR_LABEL_GAP = 2;
    public static final int PARTY_BAR_GAP = 4;
    public static final int PARTY_BAR_LABEL_WIDTH = 28;
    public static final int PARTY_BAR_VALUE_WIDTH = 84;
    public static final int PARTY_BAR_TRACK_WIDTH = PARTY_ENTRY_INNER_WIDTH - PARTY_BAR_LABEL_WIDTH - PARTY_BAR_VALUE_WIDTH - 8;

    private static final String SELECTOR_FILL_ANCHOR = "#XpFill.Anchor";
    private static final String SELECTOR_TEXT = "#XpText.Text";
    private static final String SELECTOR_ROOT_ANCHOR = "#Root.Anchor";
    private static final String SELECTOR_PARTY_ROOT_ANCHOR = "#PartyRoot.Anchor";

    private int lastFillWidth = -1;
    private String lastLabelPrefix = null;
    private int lastLevel = Integer.MIN_VALUE;
    private int lastCurrent = Integer.MIN_VALUE;
    private int lastNext = Integer.MIN_VALUE;
    private int lastRemaining = Integer.MIN_VALUE;
    private boolean lastMax = false;

    private String lastResourceLayoutSignature = "";
    private List<ResourceBarData> lastResourceLayout = Collections.emptyList();
    private int[] lastResourceStates = new int[0];

    private String lastPartyLayoutSignature = "";
    private int[] lastPartyFillWidths = new int[0];
    private String[] lastPartyValueTexts = new String[0];
    private boolean progressHidden = false;

    public XpBarHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append("Hud/XpBarHud.ui");
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

    public static final class PartyStatBarData {
        @Nonnull
        public final String statId;
        @Nonnull
        public final String label;
        @Nonnull
        public final String valueText;
        @Nonnull
        public final String fillColor;
        public final float ratio;

        public PartyStatBarData(
                @Nonnull String statId,
                @Nonnull String label,
                @Nonnull String valueText,
                @Nonnull String fillColor,
                float ratio) {
            this.statId = Objects.requireNonNull(statId, "statId");
            this.label = Objects.requireNonNull(label, "label");
            this.valueText = Objects.requireNonNull(valueText, "valueText");
            this.fillColor = Objects.requireNonNull(fillColor, "fillColor");
            this.ratio = ratio;
        }
    }

    public static final class PartyMemberHudData {
        @Nonnull
        public final String memberKey;
        @Nonnull
        public final String displayName;
        @Nonnull
        public final String classLabel;
        @Nonnull
        public final String accentColor;
        @Nonnull
        public final List<PartyStatBarData> bars;

        public PartyMemberHudData(
                @Nonnull String memberKey,
                @Nonnull String displayName,
                @Nonnull String classLabel,
                @Nonnull String accentColor,
                @Nonnull List<PartyStatBarData> bars) {
            this.memberKey = Objects.requireNonNull(memberKey, "memberKey");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.classLabel = Objects.requireNonNull(classLabel, "classLabel");
            this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
            this.bars = List.copyOf(Objects.requireNonNull(bars, "bars"));
        }
    }

    public void updateResources(@Nonnull List<ResourceBarData> bars) {
        if (bars.isEmpty()) {
            if (lastResourceLayoutSignature.isEmpty()) {
                return;
            }

            lastResourceLayoutSignature = "";
            lastResourceLayout = Collections.emptyList();
            lastResourceStates = new int[0];

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear("#ResourceBars");
            applyRootHeight(cmd, ROOT_BASE_HEIGHT);
            update(false, cmd);
            return;
        }

        String layoutSignature = buildResourceLayoutSignature(bars);
        boolean layoutChanged = !layoutSignature.equals(lastResourceLayoutSignature);
        if (layoutChanged) {
            lastResourceLayoutSignature = layoutSignature;
            lastResourceLayout = List.copyOf(bars);
            lastResourceStates = new int[bars.size()];
            for (int i = 0; i < lastResourceStates.length; i++) {
                lastResourceStates[i] = -1;
            }

            UICommandBuilder cmd = new UICommandBuilder();
            rebuildResourceBars(cmd, lastResourceLayout);
            applyRootHeight(cmd, computeRootHeight(lastResourceLayout));
            update(false, cmd);
        } else if (lastResourceStates.length != bars.size()) {
            lastResourceStates = new int[bars.size()];
            for (int i = 0; i < lastResourceStates.length; i++) {
                lastResourceStates[i] = -1;
            }
        }

        UICommandBuilder cmd = null;
        for (int i = 0; i < bars.size(); i++) {
            ResourceBarData bar = bars.get(i);
            switch (bar.displayMode) {
                case CHARGES -> {
                    int filledCharges = clampChargeCount(bar.currentCharges, bar.maxCharges);
                    if (i < lastResourceStates.length && filledCharges == lastResourceStates[i]) {
                        continue;
                    }

                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }

                    lastResourceStates[i] = filledCharges;
                    applyResourceChargeFillState(cmd, i, bar.maxCharges, filledCharges, usesChargeAsset(bar));
                }
                case BAR -> {
                    int fillWidth = Math.round(RESOURCE_TRACK_WIDTH * clampRatio(bar.ratio));
                    if (i < lastResourceStates.length && fillWidth == lastResourceStates[i]) {
                        continue;
                    }

                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }

                    lastResourceStates[i] = fillWidth;
                    applyResourceFillWidth(cmd, i, fillWidth);
                }
            }
        }

        if (cmd != null) {
            update(false, cmd);
        }
    }

    public void updatePartyMembers(@Nonnull List<PartyMemberHudData> members) {
        if (members.isEmpty()) {
            if (lastPartyLayoutSignature.isEmpty()) {
                return;
            }

            lastPartyLayoutSignature = "";
            lastPartyFillWidths = new int[0];
            lastPartyValueTexts = new String[0];

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear("#PartyMembers");
            applyPartyRootHeight(cmd, 0);
            update(false, cmd);
            return;
        }

        String layoutSignature = buildPartyLayoutSignature(members);
        int totalBars = countPartyBars(members);
        boolean layoutChanged = !layoutSignature.equals(lastPartyLayoutSignature);

        if (layoutChanged) {
            lastPartyLayoutSignature = layoutSignature;
            lastPartyFillWidths = new int[totalBars];
            lastPartyValueTexts = new String[totalBars];
            for (int i = 0; i < totalBars; i++) {
                lastPartyFillWidths[i] = -1;
                lastPartyValueTexts[i] = null;
            }

            UICommandBuilder cmd = new UICommandBuilder();
            rebuildPartyMembers(cmd, members);
            applyPartyRootHeight(cmd, computePartyRootHeight(members));
            update(false, cmd);
        } else if (lastPartyFillWidths.length != totalBars || lastPartyValueTexts.length != totalBars) {
            lastPartyFillWidths = new int[totalBars];
            lastPartyValueTexts = new String[totalBars];
            for (int i = 0; i < totalBars; i++) {
                lastPartyFillWidths[i] = -1;
                lastPartyValueTexts[i] = null;
            }
        }

        UICommandBuilder cmd = null;
        int flattenedBarIndex = 0;
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            PartyMemberHudData member = members.get(memberIndex);
            for (int barIndex = 0; barIndex < member.bars.size(); barIndex++) {
                PartyStatBarData bar = member.bars.get(barIndex);
                float ratio = clampRatio(bar.ratio);
                int fillWidth = Math.round(PARTY_BAR_TRACK_WIDTH * ratio);

                if (flattenedBarIndex >= lastPartyFillWidths.length || flattenedBarIndex >= lastPartyValueTexts.length) {
                    continue;
                }

                if (fillWidth != lastPartyFillWidths[flattenedBarIndex]) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastPartyFillWidths[flattenedBarIndex] = fillWidth;
                    applyPartyBarFillWidth(cmd, memberIndex, barIndex, fillWidth);
                }

                if (!safeEquals(bar.valueText, lastPartyValueTexts[flattenedBarIndex])) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastPartyValueTexts[flattenedBarIndex] = bar.valueText;
                    cmd.set(partyBarValueSelector(memberIndex, barIndex), bar.valueText);
                }

                flattenedBarIndex++;
            }
        }

        if (cmd != null) {
            update(false, cmd);
        }
    }

    public void updateMax(@Nonnull String labelPrefix, int level) {
        if (lastMax) {
            return;
        }

        lastMax = true;
        lastLabelPrefix = labelPrefix;
        lastLevel = level;
        lastCurrent = Integer.MIN_VALUE;
        lastNext = Integer.MIN_VALUE;
        lastRemaining = Integer.MIN_VALUE;

        UICommandBuilder cmd = new UICommandBuilder();
        ensureProgressVisible(cmd);
        applyFillWidth(cmd, BAR_WIDTH);
        cmd.set(SELECTOR_TEXT, labelPrefix + " Lv " + level + " — XP: MAX");
        update(false, cmd);
    }

    public void updateProgress(
            @Nonnull String labelPrefix,
            int level,
            float ratio,
            int current,
            int next,
            int remaining) {
        float clamped = clampRatio(ratio);
        int fillWidth = Math.round(BAR_WIDTH * clamped);

        if (!lastMax
                && safeEquals(labelPrefix, lastLabelPrefix)
                && level == lastLevel
                && fillWidth == lastFillWidth
                && current == lastCurrent
                && next == lastNext
                && remaining == lastRemaining) {
            return;
        }

        lastMax = false;
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
                labelPrefix + " Lv " + level + " — XP: " + current + "/" + next + " (" + remaining + " to next)");
        update(false, cmd);
    }

    public void hideProgress() {
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
        update(false, cmd);
    }

    private void applyFillWidth(@Nonnull UICommandBuilder cmd, int fillWidth) {
        int clamped = Math.max(0, Math.min(BAR_WIDTH, fillWidth));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setTop(Value.of(0));
        fillAnchor.setWidth(Value.of(clamped));
        fillAnchor.setHeight(Value.of(BAR_HEIGHT));

        cmd.setObject(SELECTOR_FILL_ANCHOR, fillAnchor);
    }

    private static int computeRootHeight(@Nonnull List<ResourceBarData> bars) {
        if (bars.isEmpty()) {
            return ROOT_BASE_HEIGHT;
        }

        int height = ROOT_BASE_HEIGHT;
        for (int i = 0; i < bars.size(); i++) {
            height += RESOURCE_BAR_HEIGHT + RESOURCE_BAR_GAP;
        }
        return height;
    }

    private static int computePartyRootHeight(@Nonnull List<PartyMemberHudData> members) {
        if (members.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (PartyMemberHudData member : members) {
            total += computePartyEntryHeight(member.bars.size()) + PARTY_ENTRY_GAP;
        }
        return total;
    }

    private static void applyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(ROOT_TOP));
        rootAnchor.setLeft(Value.of(ROOT_LEFT));
        rootAnchor.setWidth(Value.of(ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(ROOT_BASE_HEIGHT, height)));
        cmd.setObject(SELECTOR_ROOT_ANCHOR, rootAnchor);
    }

    private void ensureProgressVisible(@Nonnull UICommandBuilder cmd) {
        if (!progressHidden) {
            return;
        }

        progressHidden = false;
        applyRootHeight(cmd, computeRootHeight(lastResourceLayout));
    }

    private static void applyHiddenRoot(@Nonnull UICommandBuilder cmd) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(-10_000));
        rootAnchor.setLeft(Value.of(-10_000));
        rootAnchor.setWidth(Value.of(0));
        rootAnchor.setHeight(Value.of(0));
        cmd.setObject(SELECTOR_ROOT_ANCHOR, rootAnchor);
    }

    private static void applyPartyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(PARTY_ROOT_TOP));
        rootAnchor.setLeft(Value.of(PARTY_ROOT_LEFT));
        rootAnchor.setWidth(Value.of(PARTY_ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(0, height)));
        cmd.setObject(SELECTOR_PARTY_ROOT_ANCHOR, rootAnchor);
    }

    private static void rebuildResourceBars(@Nonnull UICommandBuilder cmd, @Nonnull List<ResourceBarData> bars) {
        cmd.clear("#ResourceBars");

        for (int i = 0; i < bars.size(); i++) {
            ResourceBarData bar = bars.get(i);
            boolean useChargeAsset = usesChargeAsset(bar);

            String barId = "ResBar" + i;
            String labelId = "ResLabel" + i;
            String trackId = "ResTrack" + i;
            String fillId = "ResFill" + i;
            int trackWidth = bar.displayMode == ResourceDisplayMode.CHARGES && useChargeAsset
                    ? computeChargeTrackWidth(bar.maxCharges, true)
                    : RESOURCE_TRACK_WIDTH;

            StringBuilder ui = new StringBuilder(1024);
            ui.append("Group #").append(barId).append(" {\n");
            ui.append("  LayoutMode: Left;\n");
            ui.append("  Anchor: (Width: 296, Height: ").append(RESOURCE_BAR_HEIGHT)
                    .append(", Bottom: ").append(RESOURCE_BAR_GAP).append(");\n\n");

            ui.append("  Label #").append(labelId).append(" {\n");
            ui.append("    Text: \"").append(escapeUiString(bar.label)).append("\";\n");
            ui.append("    Anchor: (Width: ").append(RESOURCE_LABEL_WIDTH)
                    .append(", Height: ").append(RESOURCE_BAR_HEIGHT)
                    .append(", Right: ").append(RESOURCE_LABEL_GAP).append(");\n");
            ui.append("    Style: (FontSize: 10, RenderBold: true);\n");
            ui.append("  }\n\n");

            ui.append("  Group #").append(trackId).append(" {\n");
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
                ui.append("    LayoutMode: Left;\n\n");
                for (int chargeIndex = 0; chargeIndex < safeMaxCharges; chargeIndex++) {
                    ui.append("    Group #ResChargeSlot").append(i).append('_').append(chargeIndex).append(" {\n");
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

                    ui.append("      Group #ResChargeFill").append(i).append('_').append(chargeIndex).append(" {\n");
                    if (useChargeAsset) {
                        ui.append("        Background: (TexturePath: \"")
                                .append(escapeUiString(bar.assetPath))
                                .append("\");\n");
                    } else {
                        ui.append("        Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.90));\n");
                    }
                    ui.append("        Anchor: (Left: 0, Top: 0, Width: 0, Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
                    ui.append("      }\n");
                    ui.append("    }\n");
                }
            } else {
                ui.append("\n");
                ui.append("    Group #").append(fillId).append(" {\n");
                ui.append("      Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.85));\n");
                ui.append("      Anchor: (Left: 0, Top: 0, Width: 0, Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
                ui.append("    }\n");
            }

            ui.append("  }\n");
            ui.append("}\n");

            cmd.appendInline("#ResourceBars", ui.toString());
        }
    }

    private static void rebuildPartyMembers(@Nonnull UICommandBuilder cmd, @Nonnull List<PartyMemberHudData> members) {
        cmd.clear("#PartyMembers");

        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            PartyMemberHudData member = members.get(memberIndex);
            int entryHeight = computePartyEntryHeight(member.bars.size());

            String entryId = "PartyEntry" + memberIndex;
            String nameId = "PartyName" + memberIndex;
            String classId = "PartyClass" + memberIndex;
            String accentId = "PartyAccent" + memberIndex;

            StringBuilder ui = new StringBuilder(2048);
            ui.append("Group #").append(entryId).append(" {\n");
            ui.append("  LayoutMode: Top;\n");
            ui.append("  Anchor: (Width: ").append(PARTY_ENTRY_WIDTH).append(", Height: ").append(entryHeight)
                    .append(", Bottom: ").append(PARTY_ENTRY_GAP).append(");\n");
            ui.append("  Padding: (Full: ").append(PARTY_ENTRY_PADDING).append(");\n");
            ui.append("  Background: PatchStyle(Color: #000000(0.32));\n");
            ui.append("  OutlineColor: ").append(member.accentColor).append("(0.95);\n");
            ui.append("  OutlineSize: 1;\n\n");

            ui.append("  Group #PartyHeader").append(memberIndex).append(" {\n");
            ui.append("    LayoutMode: Left;\n");
            ui.append("    Anchor: (Width: ").append(PARTY_ENTRY_INNER_WIDTH).append(", Height: ").append(PARTY_HEADER_HEIGHT)
                    .append(", Bottom: ").append(PARTY_HEADER_GAP).append(");\n\n");

            ui.append("    Group #").append(accentId).append(" {\n");
            ui.append("      Background: PatchStyle(Color: ").append(member.accentColor).append("(0.95));\n");
            ui.append("      Anchor: (Width: 8, Height: 8, Right: 6);\n");
            ui.append("    }\n\n");

            ui.append("    Label #").append(nameId).append(" {\n");
            ui.append("      Text: \"").append(escapeUiString(member.displayName)).append("\";\n");
            ui.append("      Anchor: (Width: 120, Height: ").append(PARTY_HEADER_HEIGHT).append(");\n");
            ui.append("      Style: (FontSize: 12, RenderBold: true);\n");
            ui.append("    }\n\n");

            ui.append("    Group { FlexWeight: 1; }\n\n");

            ui.append("    Label #").append(classId).append(" {\n");
            ui.append("      Text: \"").append(escapeUiString(member.classLabel)).append("\";\n");
            ui.append("      Anchor: (Width: 92, Height: ").append(PARTY_HEADER_HEIGHT).append(");\n");
            ui.append("      Style: (FontSize: 10, RenderBold: true);\n");
            ui.append("    }\n");
            ui.append("  }\n\n");

            for (int barIndex = 0; barIndex < member.bars.size(); barIndex++) {
                PartyStatBarData bar = member.bars.get(barIndex);
                String labelId = "PartyBarLabel" + memberIndex + "_" + barIndex;
                String valueId = "PartyBarValue" + memberIndex + "_" + barIndex;
                String trackId = "PartyBarTrack" + memberIndex + "_" + barIndex;
                String fillId = "PartyBarFill" + memberIndex + "_" + barIndex;

                ui.append("  Group #PartyBarBlock").append(memberIndex).append("_").append(barIndex).append(" {\n");
                ui.append("    LayoutMode: Top;\n");
                ui.append("    Anchor: (Width: ").append(PARTY_ENTRY_INNER_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_LABEL_HEIGHT + PARTY_BAR_LABEL_GAP + PARTY_BAR_TRACK_HEIGHT)
                        .append(", Bottom: ").append(PARTY_BAR_GAP).append(");\n\n");

                ui.append("    Group #PartyBarHeader").append(memberIndex).append("_").append(barIndex).append(" {\n");
                ui.append("      LayoutMode: Left;\n");
                ui.append("      Anchor: (Width: ").append(PARTY_ENTRY_INNER_WIDTH).append(", Height: ").append(PARTY_BAR_LABEL_HEIGHT)
                        .append(", Bottom: ").append(PARTY_BAR_LABEL_GAP).append(");\n\n");

                ui.append("      Label #").append(labelId).append(" {\n");
                ui.append("        Text: \"").append(escapeUiString(bar.label)).append("\";\n");
                ui.append("        Anchor: (Width: ").append(PARTY_BAR_LABEL_WIDTH).append(", Height: ").append(PARTY_BAR_LABEL_HEIGHT)
                        .append(");\n");
                ui.append("        Style: (FontSize: 9, RenderBold: true);\n");
                ui.append("      }\n\n");

                ui.append("      Group { FlexWeight: 1; }\n\n");

                ui.append("      Label #").append(valueId).append(" {\n");
                ui.append("        Text: \"").append(escapeUiString(bar.valueText)).append("\";\n");
                ui.append("        Anchor: (Width: ").append(PARTY_BAR_VALUE_WIDTH).append(", Height: ").append(PARTY_BAR_LABEL_HEIGHT)
                        .append(");\n");
                ui.append("        Style: (FontSize: 9, RenderBold: true);\n");
                ui.append("      }\n");
                ui.append("    }\n\n");

                ui.append("    Group #").append(trackId).append(" {\n");
                ui.append("      Anchor: (Width: ").append(PARTY_BAR_TRACK_WIDTH).append(", Height: ").append(PARTY_BAR_TRACK_HEIGHT)
                        .append(");\n");
                ui.append("      Background: PatchStyle(Color: #000000(0.52));\n");
                ui.append("      OutlineColor: #000000(0.65);\n");
                ui.append("      OutlineSize: 1;\n\n");

                ui.append("      Group #").append(fillId).append(" {\n");
                ui.append("        Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.90));\n");
                ui.append("        Anchor: (Left: 0, Top: 0, Width: 0, Height: ").append(PARTY_BAR_TRACK_HEIGHT).append(");\n");
                ui.append("      }\n");
                ui.append("    }\n");
                ui.append("  }\n\n");
            }

            ui.append("}\n");
            cmd.appendInline("#PartyMembers", ui.toString());
        }
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
    private static String resourceChargeFillSelector(int resourceIndex, int chargeIndex) {
        return "#ResChargeFill" + resourceIndex + "_" + chargeIndex + ".Anchor";
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

    private static void applyPartyBarFillWidth(@Nonnull UICommandBuilder cmd, int memberIndex, int barIndex, int fillWidth) {
        int clamped = Math.max(0, Math.min(PARTY_BAR_TRACK_WIDTH, fillWidth));

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setTop(Value.of(0));
        fillAnchor.setWidth(Value.of(clamped));
        fillAnchor.setHeight(Value.of(PARTY_BAR_TRACK_HEIGHT));

        cmd.setObject(partyBarFillSelector(memberIndex, barIndex), fillAnchor);
    }

    @Nonnull
    private static String partyBarFillSelector(int memberIndex, int barIndex) {
        return "#PartyBarFill" + memberIndex + "_" + barIndex + ".Anchor";
    }

    @Nonnull
    private static String partyBarValueSelector(int memberIndex, int barIndex) {
        return "#PartyBarValue" + memberIndex + "_" + barIndex + ".Text";
    }

    private static int computePartyEntryHeight(int barCount) {
        int safeBarCount = Math.max(1, barCount);
        return (PARTY_ENTRY_PADDING * 2)
                + PARTY_HEADER_HEIGHT
                + PARTY_HEADER_GAP
                + (safeBarCount * (PARTY_BAR_LABEL_HEIGHT + PARTY_BAR_LABEL_GAP + PARTY_BAR_TRACK_HEIGHT))
                + (Math.max(0, safeBarCount - 1) * PARTY_BAR_GAP);
    }

    private static int countPartyBars(@Nonnull List<PartyMemberHudData> members) {
        int total = 0;
        for (PartyMemberHudData member : members) {
            total += member.bars.size();
        }
        return total;
    }

    @Nonnull
    private static String buildResourceLayoutSignature(@Nonnull List<ResourceBarData> bars) {
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

    @Nonnull
    private static String buildPartyLayoutSignature(@Nonnull List<PartyMemberHudData> members) {
        StringBuilder signature = new StringBuilder(members.size() * 128);
        for (PartyMemberHudData member : members) {
            signature.append(member.memberKey)
                    .append('|')
                    .append(member.displayName)
                    .append('|')
                    .append(member.classLabel)
                    .append('|')
                    .append(member.accentColor);
            for (PartyStatBarData bar : member.bars) {
                signature.append('|')
                        .append(bar.statId)
                        .append(':')
                        .append(bar.label)
                        .append(':')
                        .append(bar.fillColor);
            }
            signature.append(';');
        }
        return signature.toString();
    }

    private static float clampRatio(float ratio) {
        if (ratio < 0f) {
            return 0f;
        }
        if (ratio > 1f) {
            return 1f;
        }
        return ratio;
    }

    private static String escapeUiString(@Nonnull String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean safeEquals(String a, String b) {
        return Objects.equals(a, b);
    }
}
