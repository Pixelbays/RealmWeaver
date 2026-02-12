package org.pixelbays.rpg.global.interaction;

import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

public final class DiceRollInteractionMeta {
    private DiceRollInteractionMeta() {
    }

    public static final MetaKey<Integer> DICE_ROLL_RESULT =
            Interaction.CONTEXT_META_REGISTRY.registerMetaObject(data -> 0);
}
