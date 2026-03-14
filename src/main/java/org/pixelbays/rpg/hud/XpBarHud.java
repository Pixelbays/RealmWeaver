package org.pixelbays.rpg.hud;

import java.util.ArrayList;
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

    private List<String> lastResourceStatIds = Collections.emptyList();
    private int[] lastResourceFillWidths = null;

    private String lastPartyLayoutSignature = "";
    private int[] lastPartyFillWidths = new int[0];
    private String[] lastPartyValueTexts = new String[0];

    public XpBarHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append("Hud/XpBarHud.ui");
    }

    public static final class ResourceBarData {
        @Nonnull
        public final String statId;
        public final float ratio;

        public ResourceBarData(@Nonnull String statId, float ratio) {
            this.statId = Objects.requireNonNull(statId, "statId");
            this.ratio = ratio;
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
            if (lastResourceStatIds.isEmpty()) {
                return;
            }

            lastResourceStatIds = Collections.emptyList();
            lastResourceFillWidths = null;

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear("#ResourceBars");
            applyRootHeight(cmd, ROOT_BASE_HEIGHT);
            update(false, cmd);
            return;
        }

        List<String> statIds = new ArrayList<>(bars.size());
        for (ResourceBarData bar : bars) {
            statIds.add(bar.statId);
        }

        boolean idsChanged = !statIds.equals(lastResourceStatIds);
        if (idsChanged) {
            lastResourceStatIds = List.copyOf(statIds);
            lastResourceFillWidths = new int[bars.size()];
            for (int i = 0; i < lastResourceFillWidths.length; i++) {
                lastResourceFillWidths[i] = -1;
            }

            UICommandBuilder cmd = new UICommandBuilder();
            rebuildResourceBars(cmd, lastResourceStatIds);
            applyRootHeight(cmd, computeRootHeight(lastResourceStatIds.size()));
            update(false, cmd);
        }

        UICommandBuilder cmd = null;
        for (int i = 0; i < bars.size(); i++) {
            ResourceBarData bar = bars.get(i);
            float ratio = bar.ratio;
            if (ratio < 0f) {
                ratio = 0f;
            } else if (ratio > 1f) {
                ratio = 1f;
            }

            int fillWidth = Math.round(RESOURCE_TRACK_WIDTH * ratio);
            if (lastResourceFillWidths != null && i < lastResourceFillWidths.length && fillWidth == lastResourceFillWidths[i]) {
                continue;
            }

            if (cmd == null) {
                cmd = new UICommandBuilder();
            }

            lastResourceFillWidths[i] = fillWidth;
            applyResourceFillWidth(cmd, i, fillWidth);
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
        float clamped = ratio;
        if (clamped < 0f) {
            clamped = 0f;
        } else if (clamped > 1f) {
            clamped = 1f;
        }

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
        applyFillWidth(cmd, fillWidth);
        cmd.set(SELECTOR_TEXT,
                labelPrefix + " Lv " + level + " — XP: " + current + "/" + next + " (" + remaining + " to next)");
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

    private static int computeRootHeight(int resourceCount) {
        if (resourceCount <= 0) {
            return ROOT_BASE_HEIGHT;
        }

        int perBar = RESOURCE_BAR_HEIGHT + RESOURCE_BAR_GAP;
        return ROOT_BASE_HEIGHT + (resourceCount * perBar);
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

    private static void applyPartyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(PARTY_ROOT_TOP));
        rootAnchor.setLeft(Value.of(PARTY_ROOT_LEFT));
        rootAnchor.setWidth(Value.of(PARTY_ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(0, height)));
        cmd.setObject(SELECTOR_PARTY_ROOT_ANCHOR, rootAnchor);
    }

    private static void rebuildResourceBars(@Nonnull UICommandBuilder cmd, @Nonnull List<String> statIds) {
        cmd.clear("#ResourceBars");

        for (int i = 0; i < statIds.size(); i++) {
            String statId = statIds.get(i);

            String barId = "ResBar" + i;
            String labelId = "ResLabel" + i;
            String trackId = "ResTrack" + i;
            String fillId = "ResFill" + i;

            StringBuilder ui = new StringBuilder(512);
            ui.append("Group #").append(barId).append(" {\n");
            ui.append("  LayoutMode: Left;\n");
            ui.append("  Anchor: (Width: 296, Height: ").append(RESOURCE_BAR_HEIGHT).append(", Bottom: ").append(RESOURCE_BAR_GAP).append(");\n");
            ui.append("\n");
            ui.append("  Label #").append(labelId).append(" {\n");
            ui.append("    Text: \"").append(escapeUiString(statId)).append("\";\n");
            ui.append("    Anchor: (Width: ").append(RESOURCE_LABEL_WIDTH).append(", Height: ").append(RESOURCE_BAR_HEIGHT).append(", Right: ").append(RESOURCE_LABEL_GAP).append(");\n");
            ui.append("    Style: (FontSize: 10, RenderBold: true);\n");
            ui.append("  }\n");
            ui.append("\n");
            ui.append("  Group #").append(trackId).append(" {\n");
            ui.append("    Anchor: (Width: ").append(RESOURCE_TRACK_WIDTH).append(", Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
            ui.append("    Background: PatchStyle(Color: #000000(0.35));\n");
            ui.append("    OutlineColor: #000000(0.5);\n");
            ui.append("    OutlineSize: 1;\n");
            ui.append("\n");
            ui.append("    Group #").append(fillId).append(" {\n");
            ui.append("      Background: PatchStyle(Color: #ffffff(0.85));\n");
            ui.append("      Anchor: (Left: 0, Top: 0, Width: 0, Height: ").append(RESOURCE_BAR_HEIGHT).append(");\n");
            ui.append("    }\n");
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
