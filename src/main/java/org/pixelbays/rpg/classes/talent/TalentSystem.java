package org.pixelbays.rpg.classes.talent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.config.settings.TalentModSettings;
import org.pixelbays.rpg.classes.config.settings.TalentModSettings.TalentResetMode;
import org.pixelbays.rpg.classes.config.settings.TalentModSettings.TalentSpecMode;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Manages talent tree point allocation, validation, and stat/ability effects.
 *
 * <p>Points come from LevelMilestone.SkillPoints (stored in
 * LevelProgressionComponent.LevelSystemData.availableSkillPoints).
 * When a node is allocated, one skill point is deducted per rank invested.
 * On reset, points are refunded according to TalentSettings.ResetMode.</p>
 */
@SuppressWarnings("null")
public class TalentSystem {

    private final ClassManagementSystem classManagementSystem;
    private final LevelProgressionSystem levelProgressionSystem;
    private StatSystem statSystem;

    public TalentSystem(@Nonnull ClassManagementSystem classManagementSystem,
            @Nonnull LevelProgressionSystem levelProgressionSystem) {
        this.classManagementSystem = classManagementSystem;
        this.levelProgressionSystem = levelProgressionSystem;
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    // ── Point Availability ──────────────────────────────────────────────────

    /**
     * Returns the number of unspent skill points available for the class's
     * level system.
     */
    public int getAvailablePoints(@Nonnull Ref<EntityStore> entityRef, @Nonnull String classId) {
        String resolvedClassId = classManagementSystem.resolveClassId(classId);
        if (resolvedClassId != null) {
            classId = resolvedClassId;
        }
        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef == null) return 0;
        String systemId = resolveSystemId(classDef);
        if (systemId == null) return 0;
        return levelProgressionSystem.getSkillPoints(entityRef, systemId);
    }

    // ── Allocation ───────────────────────────────────────────────────────────

    /**
     * Attempt to allocate one rank into a talent node.
     *
     * @return null on success, or an error message string
     */
    @Nullable
    public String allocateTalentPoint(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String treeId,
            @Nonnull String nodeId,
            @Nonnull String configId,
            @Nonnull Store<EntityStore> store) {
        String resolvedClassId = classManagementSystem.resolveClassId(classId);
        if (resolvedClassId != null) {
            classId = resolvedClassId;
        }

        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef == null) {
            return "ERROR: Unknown class: " + classId;
        }

        // Verify class is learned
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: You have not learned class " + classId;
        }

        // Find tree and node
        ClassDefinition.TalentTree tree = findTree(classDef, treeId);
        if (tree == null) {
            return "ERROR: Unknown talent tree: " + treeId;
        }
        ClassDefinition.TalentNode node = findNode(tree, nodeId);
        if (node == null) {
            return "ERROR: Unknown talent node: " + nodeId;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        if (classData == null) {
            return "ERROR: Class data not found for " + classId;
        }

        // Check level requirement
        String systemId = resolveSystemId(classDef);
        int currentLevel = systemId != null ? levelProgressionSystem.getLevel(entityRef, systemId) : 1;
        if (currentLevel < node.getRequiredLevel()) {
            return String.format("ERROR: Requires level %d (you are %d)", node.getRequiredLevel(), currentLevel);
        }

        // Check current rank
        int currentRank = classData.getNodeRank(treeId, nodeId);
        if (currentRank >= node.getMaxRank()) {
            return String.format("ERROR: Node %s is already at max rank (%d)", nodeId, node.getMaxRank());
        }

        // Check prerequisite nodes
        List<String> prereqs = node.getRequiredNodes();
        if (prereqs != null) {
            for (String prereqNodeId : prereqs) {
                ClassDefinition.TalentNode prereqNode = findNodeInClass(classDef, prereqNodeId);
                if (prereqNode == null) {
                    return "ERROR: Unknown prerequisite node: " + prereqNodeId;
                }

                int prereqRank = findNodeRankInClass(classDef, classData, prereqNodeId);
                int requiredRank = tree.getPrerequisiteRankMode() == ClassDefinition.TalentPrerequisiteRankMode.FullRank
                        ? prereqNode.getMaxRank()
                        : 1;
                if (prereqRank < requiredRank) {
                    return String.format("ERROR: Prerequisite node %s requires %d/%d ranks",
                            prereqNodeId, requiredRank, prereqNode.getMaxRank());
                }
            }
        }

        // Check spec mode restrictions
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset(configId);
        TalentModSettings talentSettings = config != null ? config.getTalentSettings() : new TalentModSettings();

        String specError = validateSpecMode(classDef, classData, treeId, tree, talentSettings.getSpecMode());
        if (specError != null) {
            return specError;
        }

        // Spend a skill point
        if (systemId == null) {
            return "ERROR: Class has no level system configured";
        }
        boolean spent = levelProgressionSystem.spendSkillPoints(entityRef, systemId, 1);
        if (!spent) {
            return "ERROR: Not enough skill points (you have " +
                    levelProgressionSystem.getSkillPoints(entityRef, systemId) + ")";
        }

        // Allocate
        classData.setNodeRank(treeId, nodeId, currentRank + 1);
        classData.addSpentTalentPoints(1);

        // Grant ability if at rank 1 and node grants an ability
        if (currentRank == 0 && node.getGrantsAbilityId() != null && !node.getGrantsAbilityId().isEmpty()) {
            ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
            if (abilityComp != null) {
                abilityComp.unlockAbility(node.getGrantsAbilityId(), classId, 1);
            }
        }

        // Refresh talent stat bonuses
        if (statSystem != null) {
            statSystem.recalculateTalentStatBonuses(entityRef, store);
        }

        RpgLogging.debugDeveloper("[TalentSystem] %s allocated node %s/%s rank %d (class=%s)",
                entityRef.getIndex(), treeId, nodeId, currentRank + 1, classId);
        return null;
    }

    // ── Refund (Partial) ─────────────────────────────────────────────────────

    /**
     * Refund the highest rank in a specific node.
     * Only allowed when TalentSettings.ResetMode is Partial.
     *
     * @return null on success, or an error message string
     */
    @Nullable
    public String refundTalentPoint(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String treeId,
            @Nonnull String nodeId,
            @Nonnull String configId,
            @Nonnull Store<EntityStore> store) {
        String resolvedClassId = classManagementSystem.resolveClassId(classId);
        if (resolvedClassId != null) {
            classId = resolvedClassId;
        }

        RpgModConfig config = RpgModConfig.getAssetMap().getAsset(configId);
        TalentModSettings talentSettings = config != null ? config.getTalentSettings() : new TalentModSettings();

        if (talentSettings.getResetMode() == TalentResetMode.Paid) {
            return "ERROR: Partial refunds are not allowed in Paid reset mode. Use /class talent reset instead.";
        }

        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef == null) return "ERROR: Unknown class: " + classId;

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: You have not learned class " + classId;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        if (classData == null) return "ERROR: Class data not found for " + classId;

        int currentRank = classData.getNodeRank(treeId, nodeId);
        if (currentRank <= 0) {
            return "ERROR: Node " + nodeId + " has no allocated ranks";
        }

        // Check that no other node depends on this one
        ClassDefinition.TalentTree tree = findTree(classDef, treeId);
        if (tree != null) {
            String blockingNode = findDependentAllocatedNode(tree, classData, nodeId);
            if (blockingNode != null) {
                return "ERROR: Cannot refund " + nodeId + " because " + blockingNode + " depends on it";
            }
        }

        // Calculate refund amount (based on partialRefundPercent if Partial mode)
        int refundAmount;
        if (talentSettings.getResetMode() == TalentResetMode.Partial) {
            refundAmount = Math.max(0, Math.round(talentSettings.getPartialRefundPercent()));
        } else {
            // Free mode: full refund
            refundAmount = 1;
        }

        // Reduce rank
        classData.setNodeRank(treeId, nodeId, currentRank - 1);
        classData.addSpentTalentPoints(-1);

        // Remove ability if rank drops to 0
        ClassDefinition.TalentNode node = findNode(tree, nodeId);
        if (currentRank == 1 && node != null && node.getGrantsAbilityId() != null && !node.getGrantsAbilityId().isEmpty()) {
            ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
            if (abilityComp != null) {
                abilityComp.removeAbility(node.getGrantsAbilityId());
            }
        }

        // Refund skill point
        String systemId = resolveSystemId(classDef);
        if (systemId != null && refundAmount > 0) {
            levelProgressionSystem.refundSkillPoints(entityRef, systemId, refundAmount);
        }

        if (statSystem != null) {
            statSystem.recalculateTalentStatBonuses(entityRef, store);
        }

        RpgLogging.debugDeveloper("[TalentSystem] %s refunded node %s/%s (class=%s, refunded=%d pt)",
                entityRef.getIndex(), treeId, nodeId, classId, refundAmount);
        return null;
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    /**
     * Reset all talent allocations for a class.
     * Behavior depends on TalentSettings.ResetMode:
     * - Free: instant full refund of all spent points
    * - Paid: requires configured currency and spends it during the reset
     * - Partial: refunds PartialRefundPercent of spent points
     *
    * @param checkItemCost if true, validate/consume reset cost for Paid mode
     * @return null on success, or an error message string
     */
    @Nullable
    public String resetTalents(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull String configId,
            boolean checkItemCost,
            @Nonnull Store<EntityStore> store) {
        String resolvedClassId = classManagementSystem.resolveClassId(classId);
        if (resolvedClassId != null) {
            classId = resolvedClassId;
        }

        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef == null) return "ERROR: Unknown class: " + classId;

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            return "ERROR: You have not learned class " + classId;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        if (classData == null) return "ERROR: Class data not found for " + classId;

        int spent = classData.getSpentTalentPoints();
        if (spent <= 0) {
            return "INFO: No talent points to reset for " + classId;
        }

        RpgModConfig config = RpgModConfig.getAssetMap().getAsset(configId);
        TalentModSettings talentSettings = config != null ? config.getTalentSettings() : new TalentModSettings();

        // Check Paid mode cost
        if (talentSettings.getResetMode() == TalentResetMode.Paid && checkItemCost) {
            CurrencyAmountDefinition resetCost = talentSettings.getResetCost();
            if (!resetCost.isFree()) {
                CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
                String ownerId = resolveCurrencyOwnerId(CurrencyScope.Character, entityRef, store);
                if (ownerId == null || ownerId.isBlank()) {
                    return "ERROR: Scope is unavailable right now: Character";
                }

                CurrencyActionResult result = currencyManager.spend(
                        CurrencyScope.Character,
                        ownerId,
                        resetCost,
                        resolveCharacterCurrencyAccessContext(entityRef, store));
                if (!result.isSuccess()) {
                    return "ERROR: " + result.getMessage();
                }
            }
        }

        // Calculate refund
        int refundAmount;
        if (talentSettings.getResetMode() == TalentResetMode.Partial) {
            refundAmount = Math.max(0, Math.round(spent * talentSettings.getPartialRefundPercent()));
        } else {
            // Free or Paid: full refund
            refundAmount = spent;
        }

        // Remove all ability grants from talent nodes
        revokeAllTalentAbilities(entityRef, classDef, classData, store);

        // Clear allocations
        classData.clearTalentAllocations();

        // Refund skill points
        String systemId = resolveSystemId(classDef);
        if (systemId != null && refundAmount > 0) {
            levelProgressionSystem.refundSkillPoints(entityRef, systemId, refundAmount);
        }

        if (statSystem != null) {
            statSystem.recalculateTalentStatBonuses(entityRef, store);
        }

        RpgLogging.debugDeveloper("[TalentSystem] %s reset talents for class=%s (spent=%d, refunded=%d)",
                entityRef.getIndex(), classId, spent, refundAmount);
        return null;
    }

    @Nonnull
    private CurrencyAccessContext resolveCharacterCurrencyAccessContext(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null || player.getInventory() == null) {
            return CurrencyAccessContext.empty();
        }
        return CurrencyAccessContext.fromInventory(player.getInventory());
    }

    @Nullable
    private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return scope == CurrencyScope.Global ? "global" : null;
        }

        return switch (scope) {
            case Character -> Realmweavers.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
            case Account -> playerRef.getUuid().toString();
            case Guild -> {
                Guild guild = Realmweavers.get().getGuildManager().getGuildForMember(playerRef.getUuid());
                yield guild == null ? null : guild.getId().toString();
            }
            case Global -> "global";
            case Custom -> null;
        };
    }

    // ── Info ─────────────────────────────────────────────────────────────────

    /**
     * Build a human-readable summary of talent allocations for a class.
     */
    @Nonnull
    public List<String> getTalentSummary(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            @Nonnull Store<EntityStore> store) {
        List<String> lines = new ArrayList<>();

        String resolvedClassId = classManagementSystem.resolveClassId(classId);
        if (resolvedClassId != null) {
            classId = resolvedClassId;
        }

        ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
        if (classDef == null) {
            lines.add("Unknown class: " + classId);
            return lines;
        }

        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null || !classComp.hasLearnedClass(classId)) {
            lines.add("Class not learned.");
            return lines;
        }

        ClassComponent.ClassData classData = classComp.getClassData(classId);
        if (classData == null) {
            lines.add("No class data found.");
            return lines;
        }

        String systemId = resolveSystemId(classDef);
        int availPts = systemId != null ? levelProgressionSystem.getSkillPoints(entityRef, systemId) : 0;
        lines.add(String.format("Talent tree for %s | Available points: %d | Spent: %d",
                classId, availPts, classData.getSpentTalentPoints()));

        List<ClassDefinition.TalentTree> trees = classDef.getTalentTrees();
        if (trees == null || trees.isEmpty()) {
            lines.add("  (No talent trees defined)");
            return lines;
        }

        for (ClassDefinition.TalentTree tree : trees) {
            int treeSpent = classData.getTreePointsSpent(tree.getTreeId());
            lines.add(String.format("  [%s] %s (%d pts spent, max %d)",
                    tree.getTreeId(), tree.getDisplayName(), treeSpent, tree.getMaxPoints()));
            if (tree.getNodes() == null) continue;
            for (ClassDefinition.TalentNode node : tree.getNodes()) {
                int rank = classData.getNodeRank(tree.getTreeId(), node.getNodeId());
                if (rank > 0 || node.getMaxRank() > 0) {
                    lines.add(String.format("    - %s [%s]: %d/%d",
                            node.getDisplayName(), node.getNodeId(), rank, node.getMaxRank()));
                }
            }
        }
        return lines;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Nullable
    private ClassDefinition.TalentTree findTree(@Nonnull ClassDefinition classDef, @Nonnull String treeId) {
        if (classDef.getTalentTrees() == null) return null;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (treeId.equals(tree.getTreeId())) return tree;
        }
        return null;
    }

    @Nullable
    private ClassDefinition.TalentNode findNode(@Nullable ClassDefinition.TalentTree tree, @Nonnull String nodeId) {
        if (tree == null || tree.getNodes() == null) return null;
        for (ClassDefinition.TalentNode node : tree.getNodes()) {
            if (nodeId.equals(node.getNodeId())) return node;
        }
        return null;
    }

    private int findNodeRankInClass(@Nonnull ClassDefinition classDef,
            @Nonnull ClassComponent.ClassData classData,
            @Nonnull String nodeId) {
        if (classDef.getTalentTrees() == null) return 0;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (tree.getNodes() == null) continue;
            for (ClassDefinition.TalentNode node : tree.getNodes()) {
                if (nodeId.equals(node.getNodeId())) {
                    return classData.getNodeRank(tree.getTreeId(), nodeId);
                }
            }
        }
        return 0;
    }

    @Nullable
    private ClassDefinition.TalentNode findNodeInClass(@Nonnull ClassDefinition classDef,
            @Nonnull String nodeId) {
        if (classDef.getTalentTrees() == null) return null;
        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            ClassDefinition.TalentNode node = findNode(tree, nodeId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Find a node in the tree whose RequiredNodes list contains the given nodeId,
     * and that has at least one rank allocated. Returns the dependent node id or null.
     */
    @Nullable
    private String findDependentAllocatedNode(@Nonnull ClassDefinition.TalentTree tree,
            @Nonnull ClassComponent.ClassData classData,
            @Nonnull String nodeId) {
        if (tree.getNodes() == null) return null;
        for (ClassDefinition.TalentNode node : tree.getNodes()) {
            List<String> reqs = node.getRequiredNodes();
            if (reqs != null && reqs.contains(nodeId)) {
                if (classData.getNodeRank(tree.getTreeId(), node.getNodeId()) > 0) {
                    return node.getNodeId();
                }
            }
        }
        return null;
    }

    /**
     * Validates that allocating a point to the given tree doesn't violate the spec mode.
     */
    @Nullable
    private String validateSpecMode(@Nonnull ClassDefinition classDef,
            @Nonnull ClassComponent.ClassData classData,
            @Nonnull String treeId,
            @Nonnull ClassDefinition.TalentTree tree,
            @Nonnull TalentSpecMode specMode) {

        switch (specMode) {
            case SoftCap:
                if (tree.getMaxPoints() > 0) {
                    int treeSpent = classData.getTreePointsSpent(treeId);
                    if (treeSpent >= tree.getMaxPoints()) {
                        return String.format("ERROR: Tree %s is at max points (%d)", treeId, tree.getMaxPoints());
                    }
                }
                break;

            case Exclusive:
                // If any other tree already has points, block this one unless it's the same
                if (classDef.getTalentTrees() == null) break;
                for (ClassDefinition.TalentTree otherTree : classDef.getTalentTrees()) {
                    if (otherTree.getTreeId().equals(treeId)) continue;
                    if (classData.getTreePointsSpent(otherTree.getTreeId()) > 0) {
                        return "ERROR: You are spec'd into " + otherTree.getTreeId() +
                                ". Reset talents to change spec (Exclusive mode).";
                    }
                }
                break;

            case Free:
            default:
                break;
        }
        return null;
    }

    private void revokeAllTalentAbilities(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ClassDefinition classDef,
            @Nonnull ClassComponent.ClassData classData,
            @Nonnull Store<EntityStore> store) {
        if (classDef.getTalentTrees() == null) return;
        ClassAbilityComponent abilityComp = store.getComponent(entityRef, ClassAbilityComponent.getComponentType());
        if (abilityComp == null) return;

        for (ClassDefinition.TalentTree tree : classDef.getTalentTrees()) {
            if (tree.getNodes() == null) continue;
            for (ClassDefinition.TalentNode node : tree.getNodes()) {
                int rank = classData.getNodeRank(tree.getTreeId(), node.getNodeId());
                if (rank > 0 && node.getGrantsAbilityId() != null && !node.getGrantsAbilityId().isEmpty()) {
                    abilityComp.removeAbility(node.getGrantsAbilityId());
                }
            }
        }
    }

    @Nullable
    private String resolveSystemId(@Nonnull ClassDefinition classDef) {
        if (classDef.usesCharacterLevel()) {
            return "Base_Character_Level";
        }
        String systemId = classDef.getLevelSystemId();
        return (systemId == null || systemId.isEmpty()) ? null : systemId;
    }
}
