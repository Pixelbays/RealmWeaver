package org.pixelbays.rpg.npc.corecomponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.npc.component.NpcRpgDebugComponent;
import org.pixelbays.rpg.npc.corecomponents.builders.BuilderActionRpgCastAbility;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

public class ActionRpgCastAbility extends ActionBase {

    private final String abilityId;
    @Nullable
    private final InteractionType interactionTypeOverride;
    private final boolean requireUnlocked;
    private final boolean logFailures;

    public ActionRpgCastAbility(@Nonnull BuilderActionRpgCastAbility builder, @Nonnull BuilderSupport support) {
        super(builder);
        this.abilityId = builder.getAbilityId(support);
        this.interactionTypeOverride = parseInteractionType(builder.getInteractionType(support));
        this.requireUnlocked = builder.isRequireUnlocked(support);
        this.logFailures = builder.isLogFailures(support);
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt,
            @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);

        ClassAbilitySystem abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        if (requireUnlocked && !abilitySystem.isAbilityUnlocked(ref, store, abilityId)) {
            if (logFailures) {
                RpgLogging.debugDeveloper("[NpcAbility] Ability not unlocked: %s", abilityId);
            }
            return true;
        }

        ClassAbilitySystem.TriggerResult result = interactionTypeOverride == null
                ? abilitySystem.triggerAbility(ref, store, abilityId)
                : abilitySystem.triggerAbility(ref, store, abilityId, interactionTypeOverride);

        NpcRpgDebugComponent debugComponent = store.getComponent(ref, NpcRpgDebugComponent.getComponentType());
        if (debugComponent != null) {
            debugComponent.setLastAbility(abilityId, 2.0f);
        }

        if (result.isFailure() && logFailures) {
            RpgLogging.debugDeveloper("[NpcAbility] Ability failed: %s (%s)", abilityId, result.getErrorMessage());
        }

        return true;
    }

    @Nullable
    private InteractionType parseInteractionType(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "PRIMARY" -> InteractionType.Primary;
            case "SECONDARY" -> InteractionType.Secondary;
            case "ABILITY1" -> InteractionType.Ability1;
            case "ABILITY2" -> InteractionType.Ability2;
            case "ABILITY3" -> InteractionType.Ability3;
            default -> null;
        };
    }
}
