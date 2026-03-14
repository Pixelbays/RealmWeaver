package org.pixelbays.rpg.classes.talent.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.Message;

final class TalentCommandUtil {

    private static final Pattern UNKNOWN_CLASS = Pattern.compile("ERROR: Unknown class: (.+)");
    private static final Pattern NOT_LEARNED = Pattern.compile("ERROR: You have not learned class (.+)");
    private static final Pattern UNKNOWN_TREE = Pattern.compile("ERROR: Unknown talent tree: (.+)");
    private static final Pattern UNKNOWN_NODE = Pattern.compile("ERROR: Unknown talent node: (.+)");
    private static final Pattern CLASS_DATA_MISSING = Pattern.compile("ERROR: Class data not found for (.+)");
    private static final Pattern REQUIRES_LEVEL = Pattern.compile("ERROR: Requires level (\\d+) \\(you are (\\d+)\\)");
    private static final Pattern NODE_MAX_RANK = Pattern.compile("ERROR: Node (.+) is already at max rank \\((\\d+)\\)");
    private static final Pattern PREREQUISITE_MISSING = Pattern.compile("ERROR: Prerequisite node not allocated: (.+)");
    private static final Pattern NOT_ENOUGH_POINTS = Pattern.compile("ERROR: Not enough skill points \\(you have (\\d+)\\)");
    private static final Pattern NO_ALLOCATED_RANKS = Pattern.compile("ERROR: Node (.+) has no allocated ranks");
    private static final Pattern REFUND_BLOCKED = Pattern.compile("ERROR: Cannot refund (.+) because (.+) depends on it");
    private static final Pattern NO_POINTS_TO_RESET = Pattern.compile("INFO: No talent points to reset for (.+)");
    private static final Pattern TREE_MAX_POINTS = Pattern.compile("ERROR: Tree (.+) is at max points \\((\\d+)\\)");
    private static final Pattern EXCLUSIVE_SPEC = Pattern.compile(
            "ERROR: You are spec'd into (.+)\\. Reset talents to change spec \\(Exclusive mode\\)\\.");

    private TalentCommandUtil() {
    }

    @Nonnull
    static Message translateResult(String result) {
        if (result == null || result.isBlank()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        Matcher matcher = UNKNOWN_CLASS.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.error.unknownClass").param("classId", matcher.group(1));
        }

        matcher = NOT_LEARNED.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.error.notLearned").param("class", matcher.group(1));
        }

        matcher = UNKNOWN_TREE.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.unknownTree").param("treeId", matcher.group(1));
        }

        matcher = UNKNOWN_NODE.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.unknownNode").param("nodeId", matcher.group(1));
        }

        matcher = CLASS_DATA_MISSING.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.classDataMissing").param("classId", matcher.group(1));
        }

        matcher = REQUIRES_LEVEL.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.requiresLevel")
                    .param("required", matcher.group(1))
                    .param("current", matcher.group(2));
        }

        matcher = NODE_MAX_RANK.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.nodeMaxRank")
                    .param("nodeId", matcher.group(1))
                    .param("maxRank", matcher.group(2));
        }

        matcher = PREREQUISITE_MISSING.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.prerequisiteMissing")
                    .param("nodeId", matcher.group(1));
        }

        if ("ERROR: Class has no level system configured".equals(result)) {
            return Message.translation("pixelbays.rpg.class.talent.error.noLevelSystem");
        }

        matcher = NOT_ENOUGH_POINTS.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.notEnoughPoints")
                    .param("available", matcher.group(1));
        }

        if ("ERROR: Partial refunds are not allowed in Paid reset mode. Use /class talent reset instead.".equals(result)) {
            return Message.translation("pixelbays.rpg.class.talent.error.partialRefundsPaidMode");
        }

        matcher = NO_ALLOCATED_RANKS.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.nodeNoRanks")
                    .param("nodeId", matcher.group(1));
        }

        matcher = REFUND_BLOCKED.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.refundBlocked")
                    .param("nodeId", matcher.group(1))
                    .param("blockingNodeId", matcher.group(2));
        }

        matcher = NO_POINTS_TO_RESET.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.noneToReset")
                    .param("classId", matcher.group(1));
        }

        matcher = TREE_MAX_POINTS.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.treeMaxPoints")
                    .param("treeId", matcher.group(1))
                    .param("maxPoints", matcher.group(2));
        }

        matcher = EXCLUSIVE_SPEC.matcher(result);
        if (matcher.matches()) {
            return Message.translation("pixelbays.rpg.class.talent.error.exclusiveSpecBlocked")
                    .param("treeId", matcher.group(1));
        }

        return Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", result);
    }
}
