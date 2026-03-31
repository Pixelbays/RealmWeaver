package org.pixelbays.rpg.hud;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class HudModuleSupport {

    private HudModuleSupport() {
    }

    static float clampRatio(float ratio) {
        if (ratio < 0f) {
            return 0f;
        }
        if (ratio > 1f) {
            return 1f;
        }
        return ratio;
    }

    @Nonnull
    static String escapeUiString(@Nonnull String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ");
    }

    static boolean safeEquals(@Nullable String a, @Nullable String b) {
        return Objects.equals(a, b);
    }
}