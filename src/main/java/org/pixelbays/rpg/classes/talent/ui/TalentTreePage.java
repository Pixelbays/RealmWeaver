package org.pixelbays.rpg.classes.talent.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.talent.TalentSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


@SuppressWarnings("null")
public class TalentTreePage extends CustomUIPage {

    private static final String CONFIG_ID = "default";
    private static final int MAX_TREE_TAB_SLOTS = 8;
    private static final int MAX_NODE_SLOTS = 48;
    private static final int MAX_CONNECTOR_SLOTS = 96;

    private static final int TREE_CANVAS_PADDING = 28;
    private static final int TREE_NODE_WIDTH = 136;
    private static final int TREE_NODE_BUTTON_HEIGHT = 62;
    private static final int TREE_NODE_HEIGHT = 82;
    private static final int TREE_COLUMN_SPACING = 156;
    private static final int TREE_ROW_SPACING = 104;
    private static final int CONNECTOR_THICKNESS = 4;
    private static final int TREE_TITLE_LEFT = 8;
    private static final int TREE_TITLE_TOP = 8;
    private static final int TREE_TITLE_WIDTH = 112;
    private static final int TREE_TITLE_HEIGHT = 34;
    private static final int TREE_STATE_TOP = 46;
    private static final int TREE_STATE_HEIGHT = 12;
    private static final int TREE_RANK_TOP = 64;
    private static final int TREE_RANK_HEIGHT = 16;

    private static final String CONNECTOR_ACTIVE_COLOR = "#d2a84a";
    private static final String CONNECTOR_INACTIVE_COLOR = "#4d5c72";

    private final TalentSystem talentSystem;

    /** The class whose talent trees are displayed. */
    private final String classId;

    /** Currently active tree tab (null before class definition loads). */
    @Nullable
    private String activeTreeId;

    /** Node selected for inspection / allocation. */
    @Nullable
    private String selectedNodeId;

    // ── Construction ─────────────────────────────────────────────────────────

    public TalentTreePage(@Nonnull PlayerRef playerRef, @Nonnull String classId) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.talentSystem = ExamplePlugin.get().getTalentSystem();
        this.classId = classId;

        // Default to first defined tree
        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        if (classDef != null && classDef.getTalentTrees() != null && !classDef.getTalentTrees().isEmpty()) {
            activeTreeId = classDef.getTalentTrees().get(0).getTreeId();
        }
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/TalentTreePage.ui");
        buildDynamicContent(ref, cmd, events, store, null);
    }

    // ── Event Handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) return;

        switch (action) {
            case "Tab" -> {
                String treeId = extractString(rawData, "TreeId");
                if (treeId != null) {
                    activeTreeId = treeId;
                    selectedNodeId = null;
                }
                // Full rebuild to regenerate tree tab styles and node list
                store.getExternalData().getWorld().execute(this::rebuild);
            }

            case "Select" -> {
                selectedNodeId = extractString(rawData, "NodeId");
                store.getExternalData().getWorld().execute(() -> {
                    ClassComponent classComp = store.getComponent(ref, ClassComponent.getComponentType());
                    UICommandBuilder cmd = new UICommandBuilder();
                    updateDetailPanel(cmd, classComp, null);
                    sendUpdate(cmd);
                });
            }

            case "Allocate" -> store.getExternalData().getWorld().execute(() -> {
                if (selectedNodeId == null || activeTreeId == null) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#StatusLabel.Text", "Select a node first.");
                    sendUpdate(cmd);
                    return;
                }
                String error = talentSystem.allocateTalentPoint(
                        ref, classId, activeTreeId, selectedNodeId, CONFIG_ID, store);
                if (error != null) {
                    // Display error; no ranks changed so no need to rebuild the node list
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#StatusLabel.Text", error);
                    sendUpdate(cmd);
                } else {
                    // Rank allocated — rebuild so points counter and node ranks refresh
                    rebuild();
                }
            });

            case "Reset" -> store.getExternalData().getWorld().execute(() -> {
                String result = talentSystem.resetTalents(ref, classId, CONFIG_ID, true, store);
                if (result == null) {
                    // Success — rebuild entire page to reflect cleared ranks
                    selectedNodeId = null;
                    rebuild();
                } else {
                    // Error or info (nothing changed), just show in status
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#StatusLabel.Text", result);
                    sendUpdate(cmd);
                }
            });

            default -> { /* unknown action, ignore */ }
        }
    }

    // ── Dynamic Content ───────────────────────────────────────────────────────

    /**
     * Produces all dynamic UI content. Called from {@link #build} on initial open
     * and via {@link #rebuild} on tab switches, allocation, and reset.
     */
    private void buildDynamicContent(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store,
            @Nullable String statusMsg) {

        ClassComponent classComp = store.getComponent(ref, ClassComponent.getComponentType());

        updateHeaderLabels(ref, cmd, classComp);
        buildTreeTabs(cmd, events, classComp);
        buildTreeGrid(cmd, events, classComp);
        updateDetailPanel(cmd, classComp, statusMsg);

        // Static action buttons — must be registered every build/rebuild
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AllocateButton",
                new EventData().append("Action", "Allocate"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetButton",
                new EventData().append("Action", "Reset"));
    }

    private void updateHeaderLabels(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nullable ClassComponent classComp) {

        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        String displayName = (classDef != null
                && classDef.getDisplayName() != null
                && !classDef.getDisplayName().isEmpty())
                        ? classDef.getDisplayName()
                        : classId;
        cmd.set("#ClassName.Text", displayName);

        int avail = talentSystem.getAvailablePoints(ref, classId);
        ClassComponent.ClassData classData = classComp != null ? classComp.getClassData(classId) : null;
        int spent = classData != null ? classData.getSpentTalentPoints() : 0;
        cmd.set("#PointsLabel.Text", avail + " pt available  |  " + spent + " spent");
    }

    private void buildTreeTabs(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events,
            @Nullable ClassComponent classComp) {

        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        resetTreeTabSlots(cmd);
        if (classDef == null || classDef.getTalentTrees() == null) return;

        ClassComponent.ClassData classData = classComp != null ? classComp.getClassData(classId) : null;

        int tabIndex = 0;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (tabIndex >= MAX_TREE_TAB_SLOTS) break;

            String tabSelector = treeTabSelector(tabIndex);
            boolean isActive = tree.getTreeId().equals(activeTreeId);
            int treeSpent = classData != null ? classData.getTreePointsSpent(tree.getTreeId()) : 0;
            String prefix = isActive ? "> " : "";
            String label = prefix + tree.getDisplayName() + " (" + treeSpent + ")";

            cmd.set(tabSelector + ".Text", label);
            cmd.setObject(tabSelector + ".Anchor", createHorizontalAnchor(140, 36, 6));

            events.addEventBinding(CustomUIEventBindingType.Activating, tabSelector,
                    new EventData().append("Action", "Tab").append("TreeId", tree.getTreeId()));
            tabIndex++;
        }
    }

    private void buildTreeGrid(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events,
            @Nullable ClassComponent classComp) {

        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        resetConnectorSlots(cmd);
        resetNodeSlots(cmd);
        cmd.set("#TreeName.Text", "");
        cmd.set("#TreeDescription.Text", "");
        cmd.setObject("#TreeCanvas.Anchor", createSizeAnchor(0, 0));
        if (classDef == null || classDef.getTalentTrees() == null || activeTreeId == null) return;

        ClassComponent.ClassData classData = classComp != null ? classComp.getClassData(classId) : null;

        ClassDefinition.TalentTree activeTree = findActiveTree(classDef, activeTreeId);
        if (activeTree == null) return;

        cmd.set("#TreeName.Text", activeTree.getDisplayName() != null ? activeTree.getDisplayName() : activeTreeId);
        cmd.set("#TreeDescription.Text", activeTree.getDescription() != null ? activeTree.getDescription() : "");

        ClassDefinition.TalentNode[] nodes = activeTree.getNodes();
        if (nodes == null || nodes.length == 0) return;

        Map<String, ClassDefinition.TalentNode> nodesById = new HashMap<>();
        int minX = 0;
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        for (ClassDefinition.TalentNode node : nodes) {
            nodesById.put(node.getNodeId(), node);
            minX = Math.min(minX, node.getPositionX());
            minY = Math.min(minY, node.getPositionY());
            maxX = Math.max(maxX, node.getPositionX());
            maxY = Math.max(maxY, node.getPositionY());
        }

        int gridWidth = maxX - minX;
        int gridHeight = maxY - minY;
        int canvasWidth = TREE_CANVAS_PADDING * 2 + gridWidth * TREE_COLUMN_SPACING + TREE_NODE_WIDTH;
        int canvasHeight = TREE_CANVAS_PADDING * 2 + gridHeight * TREE_ROW_SPACING + TREE_NODE_HEIGHT;
        cmd.setObject("#TreeCanvas.Anchor", createSizeAnchor(canvasWidth, canvasHeight));

        int connectorIndex = 0;
        for (ClassDefinition.TalentNode node : nodes) {
            List<String> prereqs = node.getRequiredNodes();
            if (prereqs == null || prereqs.isEmpty()) continue;

            for (String prereqId : prereqs) {
                ClassDefinition.TalentNode parent = nodesById.get(prereqId);
                if (parent == null) continue;

                boolean activePath = classData != null && classData.getNodeRank(activeTreeId, prereqId) > 0;
                connectorIndex = drawConnectorPath(cmd, connectorIndex, parent, node,
                    activePath ? CONNECTOR_ACTIVE_COLOR : CONNECTOR_INACTIVE_COLOR,
                    minX, minY);
                if (connectorIndex >= MAX_CONNECTOR_SLOTS) break;
            }

            if (connectorIndex >= MAX_CONNECTOR_SLOTS) break;
        }

        int nodeIndex = 0;
        for (ClassDefinition.TalentNode node : nodes) {
            if (nodeIndex >= MAX_NODE_SLOTS) break;

            int rank = classData != null ? classData.getNodeRank(activeTreeId, node.getNodeId()) : 0;
            boolean isSelected = node.getNodeId().equals(selectedNodeId);
            boolean isMaxed = rank > 0 && rank >= node.getMaxRank();
            boolean isAllocated = rank > 0;

            String rankText = rank + "/" + node.getMaxRank();
            String nodeSelector = nodeSlotSelector(nodeIndex);

            cmd.set(nodeTitleSelector(nodeIndex) + ".Text", node.getDisplayName());
            cmd.set(nodeRankSelector(nodeIndex) + ".Text", rankText);
            cmd.set(nodeStateSelector(nodeIndex) + ".Text", buildNodeStateLabel(node, isSelected, isAllocated, isMaxed));
            cmd.setObject(nodeSelector + ".Anchor", createNodeAnchor(node.getPositionX(), node.getPositionY(), minX, minY));
            cmd.setObject(nodeButtonSelector(nodeIndex) + ".Anchor", createNodeButtonAnchor());
            cmd.setObject(nodeTitleSelector(nodeIndex) + ".Anchor", createNodeTitleAnchor());
            cmd.setObject(nodeStateSelector(nodeIndex) + ".Anchor", createNodeStateAnchor());
            cmd.setObject(nodeRankSelector(nodeIndex) + ".Anchor", createNodeRankAnchor());

            events.addEventBinding(CustomUIEventBindingType.Activating, nodeButtonSelector(nodeIndex),
                    new EventData().append("Action", "Select").append("NodeId", node.getNodeId()));
            nodeIndex++;
        }
    }

    private void updateDetailPanel(@Nonnull UICommandBuilder cmd,
            @Nullable ClassComponent classComp, @Nullable String statusMsg) {

        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        ClassComponent.ClassData classData = classComp != null ? classComp.getClassData(classId) : null;

        if (selectedNodeId == null || classDef == null || activeTreeId == null) {
            cmd.setObject("#NodeName.Text", LocalizableString.fromMessageId("pixelbays.rpg.talent.ui.selectNode", null));
            cmd.set("#NodeRankLabel.Text", "");
            cmd.set("#NodeDesc.Text", "");
            cmd.set("#NodeReqLabel.Text", "");
            cmd.set("#NodeStatsLabel.Text", "");
        } else {
            ClassDefinition.TalentNode node = findNodeInActiveTree(classDef, selectedNodeId);
            if (node != null) {
                int rank = classData != null ? classData.getNodeRank(activeTreeId, selectedNodeId) : 0;
                cmd.set("#NodeName.Text", node.getDisplayName());
                cmd.set("#NodeRankLabel.Text",
                        "Rank: " + rank + " / " + node.getMaxRank()
                        + "   |   Req. Level: " + node.getRequiredLevel());
                cmd.set("#NodeDesc.Text",
                        node.getDescription() != null ? node.getDescription() : "");

                List<String> prereqs = node.getRequiredNodes();
                if (prereqs != null && !prereqs.isEmpty()) {
                    cmd.set("#NodeReqLabel.Text", "Requires: " + String.join(", ", prereqs));
                } else {
                    cmd.set("#NodeReqLabel.Text", "");
                }

                String abilityText = (node.getGrantsAbilityId() != null && !node.getGrantsAbilityId().isEmpty())
                        ? "Grants: " + node.getGrantsAbilityId() + "\n" : "";
                cmd.set("#NodeStatsLabel.Text", abilityText + buildStatSummary(node));
            } else {
                cmd.set("#NodeName.Text", selectedNodeId);
                cmd.set("#NodeRankLabel.Text", "");
                cmd.set("#NodeDesc.Text", "");
                cmd.set("#NodeReqLabel.Text", "");
                cmd.set("#NodeStatsLabel.Text", "");
            }
        }

        cmd.set("#StatusLabel.Text", statusMsg != null ? statusMsg : "");
    }

    // ── Static Helpers ────────────────────────────────────────────────────────

    @Nullable
    private ClassDefinition.TalentNode findNodeInActiveTree(@Nonnull ClassDefinition classDef,
            @Nonnull String nodeId) {
        if (classDef.getTalentTrees() == null || activeTreeId == null) return null;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (!tree.getTreeId().equals(activeTreeId)) continue;
            if (tree.getNodes() == null) return null;
            for (ClassDefinition.TalentNode n : tree.getNodes()) {
                if (nodeId.equals(n.getNodeId())) return n;
            }
        }
        return null;
    }

    @Nullable
    private static ClassDefinition.TalentTree findActiveTree(@Nonnull ClassDefinition classDef,
            @Nonnull String treeId) {
        if (classDef.getTalentTrees() == null) return null;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (treeId.equals(tree.getTreeId())) {
                return tree;
            }
        }
        return null;
    }

    @Nonnull
    private static String buildStatSummary(@Nonnull ClassDefinition.TalentNode node) {
        ClassDefinition.StatModifiers mods = node.getStatModifiers();
        if (mods == null || mods.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        if (mods.getAdditiveModifiers() != null) {
            mods.getAdditiveModifiers().forEach((stat, val) -> {
                if (sb.length() > 0) sb.append('\n');
                sb.append(val > 0 ? "+" : "").append(val).append(" ").append(stat).append(" per rank");
            });
        }
        if (mods.getMultiplicativeModifiers() != null) {
            mods.getMultiplicativeModifiers().forEach((stat, val) -> {
                if (sb.length() > 0) sb.append('\n');
                float pct = val * 100f;
                sb.append(pct > 0 ? "+" : "").append(String.format("%.1f%%", pct))
                        .append(" ").append(stat).append(" per rank");
            });
        }
        return sb.toString();
    }

    private static void resetTreeTabSlots(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < MAX_TREE_TAB_SLOTS; i++) {
            String selector = treeTabSelector(i);
            cmd.set(selector + ".Text", "");
            cmd.setObject(selector + ".Anchor", createHorizontalAnchor(0, 0, 0));
        }
    }

    private static void resetNodeSlots(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < MAX_NODE_SLOTS; i++) {
            String selector = nodeSlotSelector(i);
            cmd.set(nodeTitleSelector(i) + ".Text", "");
            cmd.set(nodeRankSelector(i) + ".Text", "");
            cmd.set(nodeStateSelector(i) + ".Text", "");
            cmd.setObject(selector + ".Anchor", createRectAnchor(0, 0, 0, 0));
            cmd.setObject(nodeButtonSelector(i) + ".Anchor", createRectAnchor(0, 0, 0, 0));
            cmd.setObject(nodeTitleSelector(i) + ".Anchor", createRectAnchor(0, 0, 0, 0));
            cmd.setObject(nodeStateSelector(i) + ".Anchor", createRectAnchor(0, 0, 0, 0));
            cmd.setObject(nodeRankSelector(i) + ".Anchor", createRectAnchor(0, 0, 0, 0));
        }
    }

    private static void resetConnectorSlots(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < MAX_CONNECTOR_SLOTS; i++) {
            String selector = connectorSelector(i);
            cmd.setObject(selector + ".Anchor", createSizeAnchor(0, 0));
            cmd.set(selector + ".Background", CONNECTOR_INACTIVE_COLOR);
        }
    }

    @Nonnull
    private static String treeTabSelector(int index) {
        return "#TreeTab" + index;
    }

    @Nonnull
    private static String nodeSlotSelector(int index) {
        return "#NodeSlot" + index;
    }

    @Nonnull
    private static String nodeButtonSelector(int index) {
        return nodeSlotSelector(index) + " #NodeButton";
    }

    @Nonnull
    private static String nodeTitleSelector(int index) {
        return nodeSlotSelector(index) + " #NodeTitle";
    }

    @Nonnull
    private static String nodeRankSelector(int index) {
        return nodeSlotSelector(index) + " #NodeRank";
    }

    @Nonnull
    private static String nodeStateSelector(int index) {
        return nodeSlotSelector(index) + " #NodeState";
    }

    @Nonnull
    private static String connectorSelector(int index) {
        return "#Connector" + index;
    }

    @Nonnull
    private static Anchor createHorizontalAnchor(int width, int height, int right) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        anchor.setRight(Value.of(right));
        return anchor;
    }

    @Nonnull
    private static Anchor createSizeAnchor(int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }

    @Nonnull
    private static Anchor createNodeAnchor(int gridX, int gridY, int minGridX, int minGridY) {
        return createRectAnchor(
                TREE_CANVAS_PADDING + (gridX - minGridX) * TREE_COLUMN_SPACING,
                TREE_CANVAS_PADDING + (gridY - minGridY) * TREE_ROW_SPACING,
                TREE_NODE_WIDTH,
                TREE_NODE_HEIGHT);
    }

    @Nonnull
    private static Anchor createNodeButtonAnchor() {
        return createRectAnchor(0, 0, TREE_NODE_WIDTH, TREE_NODE_BUTTON_HEIGHT);
    }

    @Nonnull
    private static Anchor createNodeTitleAnchor() {
        return createRectAnchor(TREE_TITLE_LEFT, TREE_TITLE_TOP, TREE_TITLE_WIDTH, TREE_TITLE_HEIGHT);
    }

    @Nonnull
    private static Anchor createNodeStateAnchor() {
        return createRectAnchor(0, TREE_STATE_TOP, TREE_NODE_WIDTH, TREE_STATE_HEIGHT);
    }

    @Nonnull
    private static Anchor createNodeRankAnchor() {
        return createRectAnchor(0, TREE_RANK_TOP, TREE_NODE_WIDTH, TREE_RANK_HEIGHT);
    }

    @Nonnull
    private static Anchor createRectAnchor(int left, int top, int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }

    private static int drawConnectorPath(@Nonnull UICommandBuilder cmd, int connectorIndex,
            @Nonnull ClassDefinition.TalentNode start, @Nonnull ClassDefinition.TalentNode end,
            @Nonnull String color, int minGridX, int minGridY) {
        if (connectorIndex >= MAX_CONNECTOR_SLOTS) return connectorIndex;

        int startCenterX = TREE_CANVAS_PADDING + (start.getPositionX() - minGridX) * TREE_COLUMN_SPACING + TREE_NODE_WIDTH / 2;
        int startCenterY = TREE_CANVAS_PADDING + (start.getPositionY() - minGridY) * TREE_ROW_SPACING + TREE_NODE_BUTTON_HEIGHT / 2;
        int endCenterX = TREE_CANVAS_PADDING + (end.getPositionX() - minGridX) * TREE_COLUMN_SPACING + TREE_NODE_WIDTH / 2;
        int endCenterY = TREE_CANVAS_PADDING + (end.getPositionY() - minGridY) * TREE_ROW_SPACING + TREE_NODE_BUTTON_HEIGHT / 2;

        if (startCenterY == endCenterY || startCenterX == endCenterX) {
            addConnectorSegment(cmd, connectorIndex++, startCenterX, startCenterY, endCenterX, endCenterY, color);
            return connectorIndex;
        }

        addConnectorSegment(cmd, connectorIndex++, startCenterX, startCenterY, endCenterX, startCenterY, color);
        if (connectorIndex >= MAX_CONNECTOR_SLOTS) return connectorIndex;

        addConnectorSegment(cmd, connectorIndex++, endCenterX, startCenterY, endCenterX, endCenterY, color);
        return connectorIndex;
    }

    private static void addConnectorSegment(@Nonnull UICommandBuilder cmd, int index,
            int startX, int startY, int endX, int endY, @Nonnull String color) {
        if (index >= MAX_CONNECTOR_SLOTS) return;

        int left = Math.min(startX, endX);
        int top = Math.min(startY, endY);
        int width = Math.max(CONNECTOR_THICKNESS, Math.abs(endX - startX));
        int height = Math.max(CONNECTOR_THICKNESS, Math.abs(endY - startY));

        if (startY == endY) {
            top -= CONNECTOR_THICKNESS / 2;
        } else {
            left -= CONNECTOR_THICKNESS / 2;
        }

        String selector = connectorSelector(index);
        cmd.setObject(selector + ".Anchor", createRectAnchor(left, top, width, height));
        cmd.set(selector + ".Background", color);
    }

    @Nonnull
    private static String buildNodeStateLabel(@Nonnull ClassDefinition.TalentNode node,
            boolean isSelected, boolean isAllocated, boolean isMaxed) {
        if (isSelected) {
            return "Selected";
        }
        if (isMaxed) {
            return "Maxed";
        }
        if (isAllocated) {
            return "Invested";
        }
        return "Lv " + node.getRequiredLevel();
    }

    /** Minimal JSON string extractor for {@link #handleDataEvent} rawData parsing. */
    @Nullable
    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) return null;

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) return null;

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) return null;

        int endQuote = firstQuote + 1;
        while (endQuote < rawData.length()) {
            char c = rawData.charAt(endQuote);
            switch (c) {
                case '\\' -> endQuote += 2; // skip escaped character
                case '"'  -> { return rawData.substring(firstQuote + 1, endQuote); }
                default   -> endQuote++;
            }
        }
        return null; // closing quote not found
    }
}
