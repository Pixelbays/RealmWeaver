package org.pixelbays.rpg.hud;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class PartyMembersHudModule implements PlayerHudModule {

    private static final String PARTY_ENTRY_ASSET_PATH = "Hud/PartyMemberEntry.ui";
    private static final String SELECTOR_PARTY_HOST = "#PartyMembers";
    private static final String SELECTOR_PARTY_HOST_ANCHOR = SELECTOR_PARTY_HOST + ".Anchor";
    private static final int MAX_VISIBLE_BARS = 3;
    private static final int MAX_VISIBLE_EFFECTS = 6;

    public static final int PARTY_ROOT_CENTER_Y = 360;
    public static final int PARTY_ROOT_LEFT = 20;
    public static final int PARTY_ROOT_WIDTH = 248;
    public static final int PARTY_ENTRY_WIDTH = 248;
    public static final int PARTY_ENTRY_INNER_WIDTH = 236;
    public static final int PARTY_ENTRY_PADDING = 6;
    public static final int PARTY_ENTRY_GAP = 6;
    public static final int PARTY_HEADER_HEIGHT = 16;
    public static final int PARTY_HEADER_GAP = 4;
        public static final int PARTY_EFFECT_ICON_TOP = 18;
        public static final int PARTY_EFFECT_ICON_SIZE = 14;
        public static final int PARTY_EFFECT_ICON_GAP = 4;
    public static final int PARTY_BAR_LABEL_HEIGHT = 10;
    public static final int PARTY_BAR_TRACK_HEIGHT = 10;
    public static final int PARTY_BAR_LABEL_GAP = 2;
    public static final int PARTY_BAR_GAP = 4;
    public static final int PARTY_BAR_LABEL_WIDTH = 28;
    public static final int PARTY_BAR_VALUE_WIDTH = 84;
    public static final int PARTY_BAR_TRACK_WIDTH = PARTY_ENTRY_INNER_WIDTH - PARTY_BAR_LABEL_WIDTH - PARTY_BAR_VALUE_WIDTH - 8;
        private static final int PARTY_BAR_BLOCK_HEIGHT = PARTY_BAR_LABEL_HEIGHT + PARTY_BAR_LABEL_GAP + PARTY_BAR_TRACK_HEIGHT;
        private static final int PARTY_BAR_TOP = 38;
        private static final int PARTY_ENTRY_HEIGHT = (PARTY_ENTRY_PADDING * 2)
            + PARTY_BAR_TOP
            + PARTY_BAR_BLOCK_HEIGHT
            + (Math.max(0, MAX_VISIBLE_BARS - 1) * (PARTY_BAR_BLOCK_HEIGHT + PARTY_BAR_GAP));

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
            applyPartyRootHeight(cmd, computePartyRootHeight(lastLayout));
            rebuildPartyMembers(cmd, lastLayout);
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
        int totalBars = countRenderedBars(members);
        boolean layoutChanged = !layoutSignature.equals(lastLayoutSignature);
        if (layoutChanged) {
            lastLayoutSignature = layoutSignature;
            lastLayout = List.copyOf(members);
            lastFillWidths = buildFillSnapshot(lastLayout);
            lastValueTexts = buildValueSnapshot(lastLayout);

            UICommandBuilder cmd = new UICommandBuilder();
            applyPartyRootHeight(cmd, computePartyRootHeight(lastLayout));
            rebuildPartyMembers(cmd, lastLayout);
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
            for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
                PartyStatBarData bar = barIndex < member.bars.size() ? member.bars.get(barIndex) : null;
                int fillWidth = bar == null ? 0 : computePartyBarFillWidth(bar.ratio);
                String valueText = bar == null ? "" : bar.valueText;

                if (fillWidth != lastFillWidths[flattenedBarIndex]) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastFillWidths[flattenedBarIndex] = fillWidth;
                    applyPartyBarFillWidth(cmd, memberIndex, barIndex, fillWidth);
                }

                if (!HudModuleSupport.safeEquals(valueText, lastValueTexts[flattenedBarIndex])) {
                    if (cmd == null) {
                        cmd = new UICommandBuilder();
                    }
                    lastValueTexts[flattenedBarIndex] = valueText;
                    cmd.set(partyBarValueSelector(memberIndex, barIndex), valueText);
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

    public static final class PartyEffectHudData {
        @Nonnull
        public final String effectKey;
        @Nonnull
        public final String iconPath;
        public final boolean debuff;

        public PartyEffectHudData(@Nonnull String effectKey, @Nonnull String iconPath, boolean debuff) {
            this.effectKey = Objects.requireNonNull(effectKey, "effectKey");
            this.iconPath = Objects.requireNonNull(iconPath, "iconPath");
            this.debuff = debuff;
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
        public final List<PartyEffectHudData> effects;
        @Nonnull
        public final List<PartyStatBarData> bars;

        public PartyMemberHudData(
                @Nonnull String memberKey,
                @Nonnull String displayName,
                @Nonnull String classLabel,
                @Nonnull String accentColor,
                @Nonnull List<PartyEffectHudData> effects,
                @Nonnull List<PartyStatBarData> bars) {
            this.memberKey = Objects.requireNonNull(memberKey, "memberKey");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.classLabel = Objects.requireNonNull(classLabel, "classLabel");
            this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
            this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
            this.bars = List.copyOf(Objects.requireNonNull(bars, "bars"));
        }
    }

    void appendDebugUi(@Nonnull StringBuilder ui) {
        int rootHeight = computePartyRootHeight(lastLayout);

        ui.append("Group #PartyMembers {\n");
        ui.append("  LayoutMode: Top;\n");
        ui.append("  Anchor: (Top: ").append(computePartyRootTop(rootHeight))
                .append(", Left: ").append(PARTY_ROOT_LEFT)
                .append(", Width: ").append(PARTY_ROOT_WIDTH)
                .append(", Height: ").append(Math.max(0, rootHeight)).append(");\n");
        if (!lastLayout.isEmpty()) {
            ui.append('\n');
            HudModuleSupport.appendIndentedBlock(ui, buildPartyMembersMarkup(lastLayout), 2);
        }
        ui.append("}\n\n");
    }

    private static void applyPartyRootHeight(@Nonnull UICommandBuilder cmd, int height) {
        Anchor rootAnchor = new Anchor();
        rootAnchor.setTop(Value.of(computePartyRootTop(height)));
        rootAnchor.setLeft(Value.of(PARTY_ROOT_LEFT));
        rootAnchor.setWidth(Value.of(PARTY_ROOT_WIDTH));
        rootAnchor.setHeight(Value.of(Math.max(0, height)));
        cmd.setObject(SELECTOR_PARTY_HOST_ANCHOR, rootAnchor);
    }

    private static int computePartyRootTop(int height) {
        return Math.max(0, PARTY_ROOT_CENTER_Y - (Math.max(0, height) / 2));
    }

    private static int computePartyRootHeight(@Nonnull List<PartyMemberHudData> members) {
        int total = 0;
        for (int i = 0; i < members.size(); i++) {
            total += PARTY_ENTRY_HEIGHT + PARTY_ENTRY_GAP;
        }
        return total;
    }

    private static int countRenderedBars(@Nonnull List<PartyMemberHudData> members) {
        return members.size() * MAX_VISIBLE_BARS;
    }

    private static void rebuildPartyMembers(@Nonnull UICommandBuilder cmd, @Nonnull List<PartyMemberHudData> members) {
        cmd.clear(SELECTOR_PARTY_HOST);

        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            cmd.append(SELECTOR_PARTY_HOST, PARTY_ENTRY_ASSET_PATH);
            applyPartyMemberLayout(cmd, memberIndex, members.get(memberIndex));
        }
    }

    @Nonnull
    private static String buildPartyMembersMarkup(@Nonnull List<PartyMemberHudData> members) {
        StringBuilder ui = new StringBuilder(Math.max(2048, members.size() * 2048));
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            appendPartyMemberMarkup(ui, memberIndex, members.get(memberIndex));
        }
        return ui.toString();
    }

    private static void applyPartyMemberLayout(
            @Nonnull UICommandBuilder cmd,
            int memberIndex,
            @Nonnull PartyMemberHudData member) {
        String selector = partyMemberSelector(memberIndex);
        int renderedEffectCount = Math.min(MAX_VISIBLE_EFFECTS, member.effects.size());
        int renderedBarCount = Math.min(MAX_VISIBLE_BARS, member.bars.size());

        cmd.set(selector + " #MemberAccent.Background", member.accentColor);
        cmd.set(selector + " #MemberName.Text", member.displayName);
        cmd.set(selector + " #MemberClass.Text", member.classLabel);

        for (int effectIndex = 0; effectIndex < MAX_VISIBLE_EFFECTS; effectIndex++) {
            String effectSelector = partyEffectSlotSelector(memberIndex, effectIndex);
            if (effectIndex >= renderedEffectCount) {
                cmd.set(effectSelector + ".Visible", false);
                continue;
            }

            PartyEffectHudData effect = member.effects.get(effectIndex);
            cmd.set(effectSelector + ".Visible", true);
            cmd.setObject(effectSelector + ".Background", createPartyEffectSlotBackground(effect.debuff));
            cmd.set(effectSelector + ".OutlineColor", partyEffectOutlineColor(effect.debuff));
            cmd.setObject(partyEffectIconSelector(memberIndex, effectIndex), createPartyEffectIconBackground(effect.iconPath, effect.debuff));
        }

        for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
            String blockSelector = partyBarBlockSelector(memberIndex, barIndex);
            if (barIndex >= renderedBarCount) {
                cmd.set(blockSelector + ".Visible", false);
                cmd.set(partyBarLabelSelector(memberIndex, barIndex), "");
                cmd.set(partyBarValueSelector(memberIndex, barIndex), "");
                applyPartyBarFillWidth(cmd, memberIndex, barIndex, 0);
                continue;
            }

            PartyStatBarData bar = member.bars.get(barIndex);
            cmd.set(blockSelector + ".Visible", true);
            cmd.set(partyBarLabelSelector(memberIndex, barIndex), bar.label);
            cmd.set(partyBarValueSelector(memberIndex, barIndex), bar.valueText);
            cmd.set(partyBarFillBackgroundSelector(memberIndex, barIndex), bar.fillColor);
            applyPartyBarFillWidth(cmd, memberIndex, barIndex, computePartyBarFillWidth(bar.ratio));
        }
    }

    private static void appendPartyMemberMarkup(
            @Nonnull StringBuilder ui,
            int memberIndex,
            @Nonnull PartyMemberHudData member) {
        int renderedEffectCount = Math.min(MAX_VISIBLE_EFFECTS, member.effects.size());
        int renderedBarCount = Math.min(MAX_VISIBLE_BARS, member.bars.size());

        ui.append("Group {\n");
        ui.append("  Anchor: (Width: ").append(PARTY_ENTRY_WIDTH).append(", Height: ").append(PARTY_ENTRY_HEIGHT)
            .append(", Bottom: ").append(PARTY_ENTRY_GAP).append(");\n");
        ui.append("  Padding: (Full: ").append(PARTY_ENTRY_PADDING).append(");\n");
        ui.append("  Background: PatchStyle(Color: #000000(0.32));\n");
        ui.append("  OutlineColor: #2F4156(0.85);\n");
        ui.append("  OutlineSize: 1;\n\n");

        ui.append("  Group #MemberAccent {\n");
        ui.append("    Anchor: (Left: 0, Top: 4, Width: 8, Height: 8);\n");
        ui.append("    Background: ").append(member.accentColor).append(";\n");
        ui.append("  }\n\n");

        ui.append("  Label #MemberName {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(member.displayName)).append("\";\n");
        ui.append("    Anchor: (Left: 14, Top: 0, Width: 122, Height: 16);\n");
        ui.append("    Style: (FontSize: 12, RenderBold: true);\n");
        ui.append("  }\n\n");

        ui.append("  Label #MemberClass {\n");
        ui.append("    Text: \"").append(HudModuleSupport.escapeUiString(member.classLabel)).append("\";\n");
        ui.append("    Anchor: (Left: 144, Top: 0, Width: 92, Height: 16);\n");
        ui.append("    Style: (FontSize: 10, RenderBold: true);\n");
        ui.append("  }\n\n");

        for (int effectIndex = 0; effectIndex < MAX_VISIBLE_EFFECTS; effectIndex++) {
            boolean visible = effectIndex < renderedEffectCount;
            PartyEffectHudData effect = visible ? member.effects.get(effectIndex) : null;

            ui.append("  Group #Effect").append(effectIndex).append(" {\n");
            if (!visible) {
                ui.append("    Visible: false;\n");
            }
            ui.append("    Anchor: (Left: ").append(partyEffectLeft(effectIndex))
                    .append(", Top: ").append(PARTY_EFFECT_ICON_TOP)
                    .append(", Width: ").append(PARTY_EFFECT_ICON_SIZE)
                    .append(", Height: ").append(PARTY_EFFECT_ICON_SIZE).append(");\n");
            ui.append("    Background: PatchStyle(Color: ")
                    .append(effect == null ? partyEffectBackgroundColor(false) : partyEffectBackgroundColor(effect.debuff))
                    .append(");\n");
            ui.append("    OutlineColor: ")
                    .append(effect == null ? partyEffectOutlineColor(false) : partyEffectOutlineColor(effect.debuff))
                    .append(";\n");
            ui.append("    OutlineSize: 1;\n\n");

            ui.append("    Group #Effect").append(effectIndex).append("Icon {\n");
            ui.append("      Anchor: (Full: 1);\n");
            if (effect != null && !effect.iconPath.isBlank()) {
                ui.append("      Background: PatchStyle(TexturePath: \"")
                        .append(HudModuleSupport.escapeUiString(effect.iconPath))
                        .append("\");\n");
            } else if (effect != null) {
                ui.append("      Background: PatchStyle(Color: ")
                        .append(partyEffectFallbackColor(effect.debuff))
                        .append(");\n");
            } else {
                ui.append("      Background: PatchStyle(Color: #ffffff(0));\n");
            }
            ui.append("    }\n");
            ui.append("  }\n\n");
        }

        for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
            boolean visible = barIndex < renderedBarCount;
            PartyStatBarData bar = visible ? member.bars.get(barIndex) : null;
            int top = PARTY_BAR_TOP + (barIndex * (PARTY_BAR_BLOCK_HEIGHT + PARTY_BAR_GAP));
            String labelText = visible && bar != null ? HudModuleSupport.escapeUiString(bar.label) : "";
            String valueText = visible && bar != null ? HudModuleSupport.escapeUiString(bar.valueText) : "";
            int fillWidth = visible && bar != null ? computePartyBarFillWidth(bar.ratio) : 0;
            String fillColor = visible && bar != null ? bar.fillColor : "#4FD36F";

            ui.append("  Group #Bar").append(barIndex).append(" {\n");
            if (!visible) {
                ui.append("    Visible: false;\n");
            }
            ui.append("    Anchor: (Top: ").append(top).append(", Width: 236, Height: 22);\n\n");

            ui.append("    Label #Bar").append(barIndex).append("Label {\n");
            ui.append("      Text: \"").append(labelText).append("\";\n");
            ui.append("      Anchor: (Left: 0, Top: 0, Width: 28, Height: 10);\n");
            ui.append("      Style: (FontSize: 9, RenderBold: true);\n");
            ui.append("    }\n\n");

            ui.append("    Label #Bar").append(barIndex).append("Value {\n");
            ui.append("      Text: \"").append(valueText).append("\";\n");
            ui.append("      Anchor: (Left: 152, Top: 0, Width: 84, Height: 10);\n");
            ui.append("      Style: (FontSize: 9, RenderBold: true);\n");
            ui.append("    }\n\n");

            ui.append("    Group #Bar").append(barIndex).append("Track {\n");
            ui.append("      Anchor: (Left: 36, Top: 12, Width: ").append(PARTY_BAR_TRACK_WIDTH).append(", Height: ")
                .append(PARTY_BAR_TRACK_HEIGHT).append(");\n");
            ui.append("      Background: PatchStyle(Color: #000000(0.52));\n");
            ui.append("      OutlineColor: #000000(0.65);\n");
            ui.append("      OutlineSize: 1;\n\n");

            ui.append("      Group #Bar").append(barIndex).append("Fill {\n");
            ui.append("        Anchor: (Left: 0, Top: 0, Width: ")
                .append(fillWidth)
                .append(", Height: ").append(PARTY_BAR_TRACK_HEIGHT).append(");\n");
            ui.append("        Background: ").append(fillColor).append(";\n");
            ui.append("      }\n");
            ui.append("    }\n");
            ui.append("  }\n\n");
        }

        ui.append("}\n");
    }

    private static int computePartyBarFillWidth(float ratio) {
        return Math.round(PARTY_BAR_TRACK_WIDTH * HudModuleSupport.clampRatio(ratio));
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
        return partyMemberSelector(memberIndex) + " #Bar" + barIndex + "Fill.Anchor";
    }

    @Nonnull
    private static String partyBarFillBackgroundSelector(int memberIndex, int barIndex) {
        return partyMemberSelector(memberIndex) + " #Bar" + barIndex + "Fill.Background";
    }

    @Nonnull
    private static String partyBarValueSelector(int memberIndex, int barIndex) {
        return partyMemberSelector(memberIndex) + " #Bar" + barIndex + "Value.Text";
    }

    @Nonnull
    private static String partyBarLabelSelector(int memberIndex, int barIndex) {
        return partyMemberSelector(memberIndex) + " #Bar" + barIndex + "Label.Text";
    }

    @Nonnull
    private static String partyBarBlockSelector(int memberIndex, int barIndex) {
        return partyMemberSelector(memberIndex) + " #Bar" + barIndex;
    }

    @Nonnull
    private static String partyEffectSlotSelector(int memberIndex, int effectIndex) {
        return partyMemberSelector(memberIndex) + " #Effect" + effectIndex;
    }

    @Nonnull
    private static String partyEffectIconSelector(int memberIndex, int effectIndex) {
        return partyEffectSlotSelector(memberIndex, effectIndex) + " #Effect" + effectIndex + "Icon.Background";
    }

    @Nonnull
    private static PatchStyle createPartyEffectSlotBackground(boolean debuff) {
        return new PatchStyle().setColor(Value.of(partyEffectBackgroundColor(debuff)));
    }

    @Nonnull
    private static PatchStyle createPartyEffectIconBackground(@Nonnull String iconPath, boolean debuff) {
        if (iconPath.isBlank()) {
            return new PatchStyle().setColor(Value.of(partyEffectFallbackColor(debuff)));
        }

        return new PatchStyle().setTexturePath(Value.of(iconPath));
    }

    private static int partyEffectLeft(int effectIndex) {
        return 14 + (effectIndex * (PARTY_EFFECT_ICON_SIZE + PARTY_EFFECT_ICON_GAP));
    }

    @Nonnull
    private static String partyEffectBackgroundColor(boolean debuff) {
        return debuff ? "#4A1F28(0.78)" : "#183A26(0.78)";
    }

    @Nonnull
    private static String partyEffectFallbackColor(boolean debuff) {
        return debuff ? "#F08E93(0.92)" : "#7FE1A2(0.92)";
    }

    @Nonnull
    private static String partyEffectOutlineColor(boolean debuff) {
        return debuff ? "#E2777E(0.9)" : "#6ED88A(0.9)";
    }

    @Nonnull
    private static String partyMemberSelector(int memberIndex) {
        return SELECTOR_PARTY_HOST + "[" + memberIndex + "]";
    }

    @Nonnull
    private static int[] buildFillSnapshot(@Nonnull List<PartyMemberHudData> members) {
        int[] fillWidths = new int[countRenderedBars(members)];
        int index = 0;
        for (PartyMemberHudData member : members) {
            for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
                if (barIndex < member.bars.size()) {
                    fillWidths[index++] = computePartyBarFillWidth(member.bars.get(barIndex).ratio);
                } else {
                    fillWidths[index++] = 0;
                }
            }
        }
        return fillWidths;
    }

    @Nonnull
    private static String[] buildValueSnapshot(@Nonnull List<PartyMemberHudData> members) {
        String[] values = new String[countRenderedBars(members)];
        int index = 0;
        for (PartyMemberHudData member : members) {
            for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
                values[index++] = barIndex < member.bars.size() ? member.bars.get(barIndex).valueText : "";
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
            for (int effectIndex = 0; effectIndex < MAX_VISIBLE_EFFECTS; effectIndex++) {
                if (effectIndex < member.effects.size()) {
                    PartyEffectHudData effect = member.effects.get(effectIndex);
                    signature.append('|')
                            .append(effect.effectKey)
                            .append(':')
                            .append(effect.iconPath)
                            .append(':')
                            .append(effect.debuff ? 'D' : 'B');
                } else {
                    signature.append('|').append('_');
                }
            }
            for (int barIndex = 0; barIndex < MAX_VISIBLE_BARS; barIndex++) {
                if (barIndex < member.bars.size()) {
                    PartyStatBarData bar = member.bars.get(barIndex);
                    signature.append('|')
                            .append(bar.statId)
                            .append(':')
                            .append(bar.label)
                            .append(':')
                            .append(bar.fillColor);
                } else {
                    signature.append('|').append('-');
                }
            }
            signature.append(';');
        }
        return signature.toString();
    }
}