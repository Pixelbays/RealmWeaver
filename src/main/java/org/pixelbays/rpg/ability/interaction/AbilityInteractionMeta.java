package org.pixelbays.rpg.ability.interaction;

import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

/**
 * Custom meta keys for ability interactions.
 */
public final class AbilityInteractionMeta {
    private AbilityInteractionMeta() {
    }

    /**
     * Empower multiplier for the current ability cast.
     * Defaults to 1.0f (no empowerment).
     */
    public static final MetaKey<Float> EMPOWER_MULTIPLIER =
            Interaction.CONTEXT_META_REGISTRY.registerMetaObject(data -> 1.0f);
}
