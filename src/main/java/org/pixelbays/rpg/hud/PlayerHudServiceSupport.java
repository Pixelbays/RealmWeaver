package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class PlayerHudServiceSupport {

    static final String BASE_SYSTEM_ID = "Base_Character_Level";

    private PlayerHudServiceSupport() {
    }

    @Nullable
    static String resolveActiveClassId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(ref, ClassComponent.getComponentType());
        return getClassManagementSystem().getPrimaryKnownClassId(classComp);
    }

    @Nonnull
    static String resolveActiveClassSystemId(@Nonnull String activeClassId) {
        if (activeClassId.isEmpty()) {
            return BASE_SYSTEM_ID;
        }

        ClassDefinition classDef = getClassDefinition(activeClassId);
        if (classDef == null) {
            return BASE_SYSTEM_ID;
        }

        if (classDef.usesCharacterLevel()) {
            return BASE_SYSTEM_ID;
        }

        String systemId = classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            return BASE_SYSTEM_ID;
        }

        return systemId;
    }

    @Nonnull
    static String resolveStatColor(@Nonnull String statId) {
        String normalized = statId.replace("_", "").replace(" ", "").toLowerCase();
        return switch (normalized) {
            case "health" -> "#4FD36F";
            case "mana" -> "#4F9DFF";
            case "stamina" -> "#E1C74C";
            case "rage" -> "#D65A4F";
            case "heat" -> "#F18A3C";
            case "oxygen" -> "#5BD3D8";
            case "ammo" -> "#E3B75D";
            case "signatureenergy" -> "#B86BFF";
            default -> "#C8CCD4";
        };
    }

    @Nonnull
    static String shortStatLabel(@Nonnull String statId) {
        String normalized = statId.replace("_", "").replace(" ", "").toLowerCase();
        return switch (normalized) {
            case "health" -> "HP";
            case "mana" -> "MP";
            case "stamina" -> "STA";
            case "rage" -> "RAGE";
            case "heat" -> "HEAT";
            case "oxygen" -> "O2";
            case "ammo" -> "AMMO";
            case "signatureenergy" -> "SIG";
            default -> humanizeIdentifier(statId).toUpperCase();
        };
    }

    @Nonnull
    static String humanizeIdentifier(@Nonnull String value) {
        String normalized = value.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder(normalized.length() + 8);
        char previous = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (i > 0 && Character.isUpperCase(current) && Character.isLowerCase(previous) && previous != ' ') {
                result.append(' ');
            }
            result.append(current);
            previous = current;
        }
        return result.toString();
    }

    @Nonnull
    static String formatCompactValue(float value) {
        float abs = Math.abs(value);
        if (abs >= 1_000_000f) {
            return formatCompactUnit(value / 1_000_000f, "M");
        }
        if (abs >= 1_000f) {
            return formatCompactUnit(value / 1_000f, "K");
        }
        return Integer.toString(Math.max(0, Math.round(value)));
    }

    @Nonnull
    static String formatCompactUnit(float value, @Nonnull String suffix) {
        float rounded = Math.round(value * 10f) / 10f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.05f) {
            return Integer.toString(Math.round(rounded)) + suffix;
        }
        return rounded + suffix;
    }

    @Nullable
    static ClassDefinition getClassDefinition(@Nonnull String classId) {
        var map = ClassDefinition.getAssetMap();
        if (map == null) {
            return null;
        }

        ClassDefinition def = map.getAsset(classId);
        if (def != null) {
            return def;
        }

        return map.getAsset(classId.toLowerCase());
    }

    @Nonnull
    static ClassManagementSystem getClassManagementSystem() {
        return Realmweavers.get().getClassManagementSystem();
    }
}