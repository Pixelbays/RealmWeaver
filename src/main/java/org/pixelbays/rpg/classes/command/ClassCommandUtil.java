package org.pixelbays.rpg.classes.command;

import com.hypixel.hytale.server.core.Message;

public final class ClassCommandUtil {

    private ClassCommandUtil() {
    }

    public static Message managerResultMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Message.translation("pixelbays.rpg.common.unknownError");
        }

        String normalized = message;
        if (normalized.startsWith("ERROR: ")) {
            normalized = normalized.substring("ERROR: ".length());
        } else if (normalized.startsWith("SUCCESS: ")) {
            normalized = normalized.substring("SUCCESS: ".length());
        }

        if (normalized.startsWith("Unknown class: ")) {
            return Message.translation("pixelbays.rpg.class.error.unknownClass")
                    .param("classId", normalized.substring("Unknown class: ".length()));
        }

        if (normalized.startsWith("Class ") && normalized.endsWith(" is not available")) {
            return Message.translation("pixelbays.rpg.class.error.notAvailable")
                    .param("classId", normalized.substring("Class ".length(), normalized.length() - " is not available".length()));
        }

        if (normalized.startsWith("Already learned class ")) {
            return Message.translation("pixelbays.rpg.class.error.alreadyLearned")
                    .param("classId", normalized.substring("Already learned class ".length()));
        }

        if (normalized.startsWith("Cannot learn ") && normalized.endsWith(" is learned")) {
            int whileIndex = normalized.indexOf(" while ");
            if (whileIndex > 0) {
                String className = normalized.substring("Cannot learn ".length(), whileIndex);
                String blockingClass = normalized.substring(whileIndex + " while ".length(), normalized.length() - " is learned".length());
                return Message.translation("pixelbays.rpg.class.error.exclusiveLearn")
                        .param("class", className)
                        .param("blockingClass", blockingClass);
            }
        }

        if (normalized.startsWith("Learned ")) {
            return Message.translation("pixelbays.rpg.class.success.learned")
                    .param("class", normalized.substring("Learned ".length()));
        }

        if (normalized.startsWith("Class ") && normalized.endsWith(" is not learned")) {
            return Message.translation("pixelbays.rpg.class.error.notLearnedById")
                    .param("classId", normalized.substring("Class ".length(), normalized.length() - " is not learned".length()));
        }

        if (normalized.startsWith("Unlearned ")) {
            return Message.translation("pixelbays.rpg.class.success.unlearned")
                    .param("class", normalized.substring("Unlearned ".length()));
        }

        if ("Must learn class before activating".equals(normalized)) {
            return Message.translation("pixelbays.rpg.class.error.mustLearnBeforeActivating");
        }

        if ("Cannot switch to this class".equals(normalized)) {
            return Message.translation("pixelbays.rpg.class.error.cannotSwitch");
        }

        if ("Cannot switch to this class while in combat".equals(normalized)) {
            return Message.translation("pixelbays.rpg.class.error.cannotSwitchCombat");
        }

        if (normalized.startsWith("Activated ")) {
            return Message.translation("pixelbays.rpg.class.success.activated")
                    .param("class", normalized.substring("Activated ".length()));
        }

        if ("Maximum combat classes reached".equals(normalized)) {
            return Message.translation("pixelbays.rpg.class.error.maxCombat");
        }

        if ("Maximum profession classes reached".equals(normalized)) {
            return Message.translation("pixelbays.rpg.class.error.maxProfession");
        }

        if (normalized.startsWith("You must select a race before learning ")) {
            return Message.translation("pixelbays.rpg.class.error.raceRequired")
                    .param("class", normalized.substring("You must select a race before learning ".length()));
        }

        if (normalized.startsWith("Your race cannot learn ")) {
            return Message.translation("pixelbays.rpg.class.error.raceCannotLearn")
                    .param("class", normalized.substring("Your race cannot learn ".length()));
        }

        if (normalized.startsWith("Requires unknown class ")) {
            return Message.translation("pixelbays.rpg.class.error.requiresUnknownClass")
                    .param("classId", normalized.substring("Requires unknown class ".length()));
        }

        if (normalized.startsWith("Requires ") && normalized.endsWith(" class")) {
            return Message.translation("pixelbays.rpg.class.error.requiresClass")
                    .param("class", normalized.substring("Requires ".length(), normalized.length() - " class".length()));
        }

        if (normalized.startsWith("Requires ") && normalized.contains(" level ")) {
            int levelIndex = normalized.lastIndexOf(" level ");
            if (levelIndex > "Requires ".length()) {
                String className = normalized.substring("Requires ".length(), levelIndex);
                String level = normalized.substring(levelIndex + " level ".length());
                return Message.translation("pixelbays.rpg.class.error.requiresLevel")
                        .param("class", className)
                        .param("level", level);
            }
        }

        return Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", normalized);
    }
}