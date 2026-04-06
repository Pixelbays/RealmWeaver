package org.pixelbays.rpg.hud;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

public final class HotbarHudModule implements PlayerHudModule {

    private static final String SELECTOR_USABLE_HOST = "#HotbarUsableHost";
    private static final String SELECTOR_SPELL_HOST = "#HotbarSpellHost";
    private static final int USABLE_ROOT_WIDTH = 1000;
    private static final int SPELL_ROOT_WIDTH = 320;
    private static final int USABLE_SLOT_WIDTH = 86;
    private static final int USABLE_SLOT_HEIGHT = 108;
    private static final int SPELL_SLOT_WIDTH = 92;
    private static final int SPELL_SLOT_HEIGHT = 116;
    private static final int KEYCAP_WIDTH = 18;
    private static final int KEYCAP_HEIGHT = 18;
    private static final String ITEM_GRID_STYLE = "ItemGridStyle("
            + "SlotSpacing: 0, "
            + "SlotSize: 74, "
            + "SlotIconSize: 64, "
            + "SlotBackground: \"Common/BlockSelectorSlotBackground.png\", "
            + "DefaultItemIcon: \"Common/UnknownItemIcon.png\", "
            + "DurabilityBar: \"Common/ProgressBarFill.png\", "
            + "DurabilityBarBackground: \"Common/ProgressBar.png\", "
            + "DurabilityBarAnchor: (Bottom: 10, Left: 13, Width: 48, Height: 2), "
            + "DurabilityBarColorStart: #a63232, "
            + "DurabilityBarColorEnd: #63b649"
            + ")";

    private final PlayerHud hud;

    private String lastSignature = "";
    private List<SlotViewData> lastUsableSlots = Collections.emptyList();
    private List<SlotViewData> lastSpellSlots = Collections.emptyList();

    HotbarHudModule(@Nonnull PlayerHud hud) {
        this.hud = hud;
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd) {
        rebuild(cmd, lastUsableSlots, lastSpellSlots);
    }

    public void prime(@Nonnull List<SlotViewData> usableSlots, @Nonnull List<SlotViewData> spellSlots) {
        lastUsableSlots = List.copyOf(usableSlots);
        lastSpellSlots = List.copyOf(spellSlots);
        lastSignature = buildSignature(lastUsableSlots, lastSpellSlots);
    }

    public void update(@Nonnull List<SlotViewData> usableSlots, @Nonnull List<SlotViewData> spellSlots) {
        String signature = buildSignature(usableSlots, spellSlots);
        if (signature.equals(lastSignature)) {
            return;
        }

        lastSignature = signature;
        lastUsableSlots = List.copyOf(usableSlots);
        lastSpellSlots = List.copyOf(spellSlots);

        UICommandBuilder cmd = new UICommandBuilder();
        rebuild(cmd, lastUsableSlots, lastSpellSlots);
        hud.applyModuleUpdate(cmd);
    }

    private void rebuild(
            @Nonnull UICommandBuilder cmd,
            @Nonnull List<SlotViewData> usableSlots,
            @Nonnull List<SlotViewData> spellSlots) {
        cmd.clear(SELECTOR_USABLE_HOST);
        cmd.clear(SELECTOR_SPELL_HOST);
        cmd.setObject(SELECTOR_USABLE_HOST + ".Anchor", createUsableHostAnchor(usableSlots.size()));

        if (!usableSlots.isEmpty()) {
            cmd.appendInline(SELECTOR_USABLE_HOST, buildMarkup(usableSlots, false));
            applySlots(cmd, usableSlots, false);
        }

        if (!spellSlots.isEmpty()) {
            cmd.appendInline(SELECTOR_SPELL_HOST, buildMarkup(spellSlots, true));
            applySlots(cmd, spellSlots, true);
        }
    }

    private static void applySlots(
            @Nonnull UICommandBuilder cmd,
            @Nonnull List<SlotViewData> slots,
            boolean spellPanel) {
        for (int i = 0; i < slots.size(); i++) {
            cmd.set(slotGridSelector(i, spellPanel) + ".Slots", List.of(slots.get(i).slot));
        }
    }

    @Nonnull
    private static String buildMarkup(@Nonnull List<SlotViewData> slots, boolean spellPanel) {
        StringBuilder ui = new StringBuilder(Math.max(512, slots.size() * 420));
        for (int i = 0; i < slots.size(); i++) {
            SlotViewData slot = slots.get(i);
            appendSlotMarkup(ui, i, slot, spellPanel);
        }
        return ui.toString();
    }

    private static void appendSlotMarkup(
            @Nonnull StringBuilder ui,
            int index,
            @Nonnull SlotViewData slot,
            boolean spellPanel) {
        String prefix = spellPanel ? "Spell" : "Usable";
        int groupWidth = spellPanel ? SPELL_SLOT_WIDTH : USABLE_SLOT_WIDTH;
        int frameLeft = spellPanel ? 7 : 4;
        int frameSize = spellPanel ? 78 : 78;
        int chipLeft = spellPanel ? 11 : 8;
        int chipTop = 4;
        String frameBackground = spellPanel
                ? (slot.active ? "#162033(0.90)" : "#101723(0.74)")
                : (slot.active ? "#182234(0.88)" : "#0F1622(0.62)");
        String frameOutline = slot.active ? "#E4B45A(0.98)" : (spellPanel ? "#526884(0.80)" : "#42546D(0.72)");
        String chipBackground = slot.active ? "#243047(0.96)" : "#111927(0.86)";
        String chipText = slot.active ? "#F4C96B" : "#B4C0D0";

        ui.append("Group #")
                .append(prefix)
                .append("Slot")
                .append(index)
                .append(" {\n")
                .append("  Anchor: (Width: ")
                .append(groupWidth)
                .append(", Height: ")
                .append(spellPanel ? SPELL_SLOT_HEIGHT : USABLE_SLOT_HEIGHT)
            .append(");\n")
                .append("\n")
                .append("  Group {\n")
                .append("    Anchor: (Left: ")
                .append(frameLeft)
                .append(", Top: 0, Width: ")
                .append(frameSize)
                .append(", Height: ")
                .append(frameSize)
                .append(");\n")
                .append("    Background: PatchStyle(Color: ")
                .append(frameBackground)
                .append(");\n")
                .append("    OutlineColor: ")
                .append(frameOutline)
                .append(";\n")
                .append("    OutlineSize: ")
                .append(slot.active ? "2" : "1")
                .append(";\n")
                .append("\n")
                .append("    ItemGrid #")
                .append(prefix)
                .append("Grid")
                .append(index)
                .append(" {\n")
                .append("      Anchor: (Left: 2, Top: 2, Width: 74, Height: 74);\n")
                .append("      SlotsPerRow: 1;\n")
                .append("      Style: ")
                .append(ITEM_GRID_STYLE.replace("SlotIconSize: 64", "SlotIconSize: " + (spellPanel ? 60 : 64)))
                .append(";\n")
                .append("      AreItemsDraggable: false;\n")
                .append("      Background: #202c3b(0);\n")
                .append("      Padding: 0;\n")
                .append("    }\n")
                .append("  }\n")
                .append("\n")
                .append("  Label #")
                .append(prefix)
                .append("Key")
                .append(index)
                .append(" {\n")
                .append("    Text: \"")
                .append(HudModuleSupport.escapeUiString(slot.keyLabel))
                .append("\";\n")
                .append("    Background: PatchStyle(Color: ")
                .append(chipBackground)
                .append(");\n")
                .append("    Style: (Alignment: Center, FontSize: 11, RenderBold: true, TextColor: ")
                .append(chipText)
                .append(", OutlineColor: #000000(0.25));\n")
                .append("    Padding: (Horizontal: 0, Vertical: 0);\n")
                .append("    Anchor: (Left: ")
                .append(chipLeft)
                .append(", Top: ")
                .append(chipTop)
                .append(", Width: ")
                .append(KEYCAP_WIDTH)
                .append(", Height: ")
                .append(KEYCAP_HEIGHT)
                .append(");\n")
                .append("  }\n")
                .append("}\n");
    }

    @Nonnull
    private static String buildSignature(
            @Nonnull List<SlotViewData> usableSlots,
            @Nonnull List<SlotViewData> spellSlots) {
        StringBuilder sig = new StringBuilder((usableSlots.size() + spellSlots.size()) * 40 + 8);
        sig.append("U:");
        for (SlotViewData slot : usableSlots) {
            sig.append(slot.signatureToken).append(';');
        }
        sig.append("|S:");
        for (SlotViewData slot : spellSlots) {
            sig.append(slot.signatureToken).append(';');
        }
        return sig.toString();
    }

    @Nonnull
    private static String slotGridSelector(int index, boolean spellPanel) {
        return spellPanel ? "#SpellGrid" + index : "#UsableGrid" + index;
    }

    @Nonnull
    private static Anchor createUsableHostAnchor(int slotCount) {
        int totalWidth = slotCount * USABLE_SLOT_WIDTH;

        Anchor anchor = new Anchor();
        anchor.setTop(Value.of(0));
        anchor.setWidth(Value.of(totalWidth));
        anchor.setHeight(Value.of(USABLE_SLOT_HEIGHT));
        return anchor;
    }

    void appendDebugUi(@Nonnull StringBuilder ui) {
        int usableWidth = lastUsableSlots.size() * USABLE_SLOT_WIDTH;
        int spellWidth = lastSpellSlots.size() * SPELL_SLOT_WIDTH;

        ui.append("Group #HotbarUsableRoot {\n");
        ui.append("  Anchor: (Left: 0, Right: 0, Bottom: 12, Height: ").append(USABLE_SLOT_HEIGHT).append(");\n");
        ui.append("  LayoutMode: CenterMiddle;\n\n");
        ui.append("  Group #HotbarUsableHost {\n");
        ui.append("    LayoutMode: Left;\n");
        ui.append("    Anchor: (Top: 0, Width: ").append(usableWidth)
            .append(", Height: ").append(USABLE_SLOT_HEIGHT).append(");\n");
        if (!lastUsableSlots.isEmpty()) {
            ui.append('\n');
            HudModuleSupport.appendIndentedBlock(ui, buildMarkup(lastUsableSlots, false), 4);
        }
        ui.append("  }\n");
        ui.append("}\n\n");

        ui.append("Group #HotbarSpellRoot {\n");
        ui.append("  Anchor: (Right: 24, Bottom: 18, Width: ").append(SPELL_ROOT_WIDTH)
                .append(", Height: ").append(SPELL_SLOT_HEIGHT).append(");\n");
        ui.append("  LayoutMode: Right;\n\n");
        ui.append("  Group #HotbarSpellHost {\n");
        ui.append("    LayoutMode: Right;\n");
        ui.append("    Anchor: (Right: 10, Width: ").append(spellWidth).append(", Height: ")
                .append(SPELL_SLOT_HEIGHT).append(");\n");
        if (!lastSpellSlots.isEmpty()) {
            ui.append('\n');
            HudModuleSupport.appendIndentedBlock(ui, buildMarkup(lastSpellSlots, true), 4);
        }
        ui.append("  }\n");
        ui.append("}\n\n");
    }

    static final class SlotViewData {
        @Nonnull
        private final ItemGridSlot slot;
        @Nonnull
        private final String keyLabel;
        @Nonnull
        private final String signatureToken;
        private final boolean active;

        SlotViewData(
                @Nonnull ItemGridSlot slot,
                @Nonnull String keyLabel,
                boolean active,
                @Nonnull String signatureToken) {
            this.slot = slot;
            this.keyLabel = keyLabel;
            this.active = active;
            this.signatureToken = signatureToken;
        }
    }
}