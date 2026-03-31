package org.pixelbays.rpg.hud;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class PartyMembersHudModule implements PlayerHudModule {

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

    private static final String SELECTOR_PARTY_ROOT_ANCHOR = "#PartyRoot.Anchor";

    private final PlayerHud hud;

    private String lastLayoutSignature = "";
    private List<PartyMemberHudData> lastLayout = Collections.emptyList();
    private int[] lastFillWidths = new int[0];
    private String[] lastValueTexts = new String[0];

    PartyMembersHudModule(@Nonnull PlayerHud hud) {
        this.hud = hud;
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd) {
        if (!lastLayout.isEmpty()) {
            rebuildPartyMembers(cmd, lastLayout);
            applyPartyRootHeight(cmd, computePartyRootHeight(lastLayout));
        }
    }

    public void prime(@Nonnull List<PartyMemberHudData> members) {
        if (members.isEmpty()) {
            lastLayoutSignature = "";
            lastLayout = Collections.emptyList();
            lastFillWidths = new int[0];
            lastValueTexts = new String[0];
            return;
        }

        lastLayoutSignature = buildLayoutSignature(members);
        lastLayout = List.copyOf(members);
        lastFillWidths = buildFillSnapshot(lastLayout);
        lastValueTexts = buildValueSnapshot(lastLayout);
    }

    public void update(@Nonnull List<PartyMemberHudData> members) {
        if (members.isEmpty()) {
            if (lastLayoutSignature.isEmpty()) {
                return;
            }

            lastLayoutSignature = "";
            lastLayout = Collections.emptyList();
            lastFillWidths = new int[0];
            lastValueTexts = new String[0];

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.clear("#PartyMembers");
            applyPartyRootHeight(cmd, 0);
            hud.applyModuleUpdate(cmd);
            return;
        }

        String layoutSignature = buildLayoutSignature(members);
        int totalBars = countPartyBars(members);
        boolean layoutChanged = !layoutSignature.equals(lastLayoutSignature);
        if (layoutChanged) {
            lastLayoutSignature = layoutSignature;
            lastLayout = List.copyOf(members);
            lastFillWidths = buildFillSnapshot(lastLayout);
            lastValueTexts = buildValueSnapshot(lastLayout);

            UICommandBuilder cmd = new UICommandBuilder();
            rebuildPartyMembers(cmd, lastLayout);
            applyPartyRootHeight(cmd, computePartyRootHeight(lastLayout));
            hud.applyModuleUpdate(cmd);
            return;
        }

        if (lastFillWidths.length != totalBars || lastValueTexts.length != totalBars) {
            lastFillWidths = new int[totalBars];
            lastValueTexts = new String[totalBars];
            for (int i = 0; i < totalBars; i++) {
                lastFillWidths[i] = -1;
                lastValueTexts[i] = null;
            }
        }

        UICommandBuilder cmd = null;
        int flattenedBarIndex = 0;
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            PartyMemberHudData member = members.get(memberIndex);
            for (int barIndex = 0; barIndex < member.bars.size(); barIndex++) {
                PartyStatBarData bar = member.bars.get(barIndex);
                int fillWidth = Math.round(PARTY_BAR_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio));

                if (fillWidth != lastFillWidths[flattenedBarIndex]) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastFillWidths[flattenedBarIndex] = fillWidth;
                    applyPartyBarFillWidth(cmd, memberIndex, barIndex, fillWidth);
                }

                if (!HudModuleSupport.safeEquals(bar.valueText, lastValueTexts[flattenedBarIndex])) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastValueTexts[flattenedBarIndex] = bar.valueText;
                    cmd.set(partyBarValueSelector(memberIndex, barIndex), bar.valueText);
                }

                flattenedBarIndex++;
            }
        }

        if (cmd != null) {
            hud.applyModuleUpdate(cmd);
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

    private static void applyPartyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(PARTY_ROOT_TOP));
        rootAnchor.setLeft(Value.of(PARTY_ROOT_LEFT));
        rootAnchor.setWidth(Value.of(PARTY_ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(0, height)));
        cmd.setObject(SELECTOR_PARTY_ROOT_ANCHOR, rootAnchor);
    }

    private static int computePartyRootHeight(@Nonnull List<PartyMemberHudData> members) {
        int total = 0;
        for (PartyMemberHudData member : members) {
            total += computePartyEntryHeight(member.bars.size()) + PARTY_ENTRY_GAP;
        }
        return total;
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

    private static void rebuildPartyMembers(@Nonnull UICommandBuilder cmd, @Nonnull List<PartyMemberHudData> members) {
        cmd.clear("#PartyMembers");

        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            PartyMemberHudData member = members.get(memberIndex);
            int entryHeight = computePartyEntryHeight(member.bars.size());

            StringBuilder ui = new StringBuilder(2048);
            ui.append("Group #PartyEntry").append(memberIndex).append(" {\n");
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

            ui.append("    Group #PartyAccent").append(memberIndex).append(" {\n");
            ui.append("      Background: PatchStyle(Color: ").append(member.accentColor).append("(0.95));\n");
            ui.append("      Anchor: (Width: 8, Height: 8, Right: 6);\n");
            ui.append("    }\n\n");

            ui.append("    Label #PartyName").append(memberIndex).append(" {\n");
            ui.append("      Text: \"").append(HudModuleSupport.escapeUiString(member.displayName)).append("\";\n");
            ui.append("      Anchor: (Width: 120, Height: ").append(PARTY_HEADER_HEIGHT).append(");\n");
            ui.append("      Style: (FontSize: 12, RenderBold: true);\n");
            ui.append("    }\n\n");

            ui.append("    Group { FlexWeight: 1; }\n\n");

            ui.append("    Label #PartyClass").append(memberIndex).append(" {\n");
            ui.append("      Text: \"").append(HudModuleSupport.escapeUiString(member.classLabel)).append("\";\n");
            ui.append("      Anchor: (Width: 92, Height: ").append(PARTY_HEADER_HEIGHT).append(");\n");
            ui.append("      Style: (FontSize: 10, RenderBold: true);\n");
            ui.append("    }\n");
            ui.append("  }\n\n");

            for (int barIndex = 0; barIndex < member.bars.size(); barIndex++) {
                PartyStatBarData bar = member.bars.get(barIndex);
                ui.append("  Group #PartyBarBlock").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("    LayoutMode: Top;\n");
                ui.append("    Anchor: (Width: ").append(PARTY_ENTRY_INNER_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_LABEL_HEIGHT + PARTY_BAR_LABEL_GAP + PARTY_BAR_TRACK_HEIGHT)
                        .append(", Bottom: ").append(PARTY_BAR_GAP).append(");\n\n");

                ui.append("    Group #PartyBarHeader").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("      LayoutMode: Left;\n");
                ui.append("      Anchor: (Width: ").append(PARTY_ENTRY_INNER_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_LABEL_HEIGHT).append(", Bottom: ").append(PARTY_BAR_LABEL_GAP).append(");\n\n");

                ui.append("      Label #PartyBarLabel").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("        Text: \"").append(HudModuleSupport.escapeUiString(bar.label)).append("\";\n");
                ui.append("        Anchor: (Width: ").append(PARTY_BAR_LABEL_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_LABEL_HEIGHT).append(");\n");
                ui.append("        Style: (FontSize: 9, RenderBold: true);\n");
                ui.append("      }\n\n");

                ui.append("      Group { FlexWeight: 1; }\n\n");

                ui.append("      Label #PartyBarValue").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("        Text: \"").append(HudModuleSupport.escapeUiString(bar.valueText)).append("\";\n");
                ui.append("        Anchor: (Width: ").append(PARTY_BAR_VALUE_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_LABEL_HEIGHT).append(");\n");
                ui.append("        Style: (FontSize: 9, RenderBold: true);\n");
                ui.append("      }\n");
                ui.append("    }\n\n");

                ui.append("    Group #PartyBarTrack").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("      Anchor: (Width: ").append(PARTY_BAR_TRACK_WIDTH).append(", Height: ")
                        .append(PARTY_BAR_TRACK_HEIGHT).append(");\n");
                ui.append("      Background: PatchStyle(Color: #000000(0.52));\n");
                ui.append("      OutlineColor: #000000(0.65);\n");
                ui.append("      OutlineSize: 1;\n\n");

                ui.append("      Group #PartyBarFill").append(memberIndex).append('_').append(barIndex).append(" {\n");
                ui.append("        Background: PatchStyle(Color: ").append(bar.fillColor).append("(0.90));\n");
                ui.append("        Anchor: (Left: 0, Top: 0, Width: ")
                        .append(Math.round(PARTY_BAR_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio)))
                        .append(", Height: ").append(PARTY_BAR_TRACK_HEIGHT).append(");\n");
                ui.append("      }\n");
                ui.append("    }\n");
                ui.append("  }\n\n");
            }

            ui.append("}\n");
            cmd.appendInline("#PartyMembers", ui.toString());
        }
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

    @Nonnull
    private static int[] buildFillSnapshot(@Nonnull List<PartyMemberHudData> members) {
        int[] fillWidths = new int[countPartyBars(members)];
        int index = 0;
        for (PartyMemberHudData member : members) {
            for (PartyStatBarData bar : member.bars) {
                fillWidths[index++] = Math.round(PARTY_BAR_TRACK_WIDTH * HudModuleSupport.clampRatio(bar.ratio));
            }
        }
        return fillWidths;
    }

    @Nonnull
    private static String[] buildValueSnapshot(@Nonnull List<PartyMemberHudData> members) {
        String[] values = new String[countPartyBars(members)];
        int index = 0;
        for (PartyMemberHudData member : members) {
            for (PartyStatBarData bar : member.bars) {
                values[index++] = bar.valueText;
            }
        }
        return values;
    }

    @Nonnull
    private static String buildLayoutSignature(@Nonnull List<PartyMemberHudData> members) {
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
}