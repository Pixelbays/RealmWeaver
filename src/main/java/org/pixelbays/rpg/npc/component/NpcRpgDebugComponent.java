package org.pixelbays.rpg.npc.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone"})
public class NpcRpgDebugComponent implements Component<EntityStore>, Cloneable {

    public static final BuilderCodec<NpcRpgDebugComponent> CODEC = BuilderCodec
            .builder(NpcRpgDebugComponent.class, NpcRpgDebugComponent::new)
            .append(new KeyedCodec<>("LastAbilityId", Codec.STRING),
                    (component, value) -> component.lastAbilityId = value,
                    component -> component.lastAbilityId)
            .add()
            .append(new KeyedCodec<>("AbilitySecondsRemaining", Codec.FLOAT),
                    (component, value) -> component.abilitySecondsRemaining = value,
                    component -> component.abilitySecondsRemaining)
            .add()
            .build();

    private String lastAbilityId;
    private float abilitySecondsRemaining;

    public NpcRpgDebugComponent() {
        this.lastAbilityId = "";
        this.abilitySecondsRemaining = 0f;
    }

    public void setLastAbility(@Nullable String abilityId, float durationSeconds) {
        this.lastAbilityId = abilityId == null ? "" : abilityId;
        this.abilitySecondsRemaining = Math.max(0f, durationSeconds);
    }

    public String getLastAbilityId() {
        return lastAbilityId;
    }

    public float getAbilitySecondsRemaining() {
        return abilitySecondsRemaining;
    }

    public void tick(float dt) {
        if (abilitySecondsRemaining > 0f) {
            abilitySecondsRemaining = Math.max(0f, abilitySecondsRemaining - dt);
            if (abilitySecondsRemaining == 0f) {
                lastAbilityId = "";
            }
        }
    }

    public boolean hasRecentAbility() {
        return abilitySecondsRemaining > 0f && lastAbilityId != null && !lastAbilityId.isEmpty();
    }

    public static ComponentType<EntityStore, NpcRpgDebugComponent> getComponentType() {
        return Realmweavers.get().getNpcRpgDebugComponentType();
    }

    @Nonnull
    @Override
    @SuppressWarnings({"all", "clone", "CloneDoesntDeclareCloneNotSupportedException"})
    public Component<EntityStore> clone() {
        NpcRpgDebugComponent cloned = new NpcRpgDebugComponent();
        cloned.lastAbilityId = this.lastAbilityId;
        cloned.abilitySecondsRemaining = this.abilitySecondsRemaining;
        return cloned;
    }
}
