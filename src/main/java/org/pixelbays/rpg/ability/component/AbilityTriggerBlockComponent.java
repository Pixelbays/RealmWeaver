package org.pixelbays.rpg.ability.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component that blocks all ability triggers for an entity.
 */
public class AbilityTriggerBlockComponent implements Component<EntityStore> {

    public static final BuilderCodec<AbilityTriggerBlockComponent> CODEC = BuilderCodec
            .builder(AbilityTriggerBlockComponent.class, AbilityTriggerBlockComponent::new)
            .append(new KeyedCodec<>("Blocked", Codec.BOOLEAN),
                    (component, value) -> component.blocked = value,
                    component -> component.blocked)
            .add()
            .append(new KeyedCodec<>("Reason", Codec.STRING),
                    (component, value) -> component.reason = value,
                    component -> component.reason)
            .add()
            .build();

    private boolean blocked;
    private String reason;

    public AbilityTriggerBlockComponent() {
        this.blocked = false;
        this.reason = "";
    }

    public boolean isBlocked() {
        return blocked;
    }

    @Nullable
    public String getReason() {
        return reason == null || reason.isEmpty() ? null : reason;
    }

    public void setBlocked(boolean blocked, @Nullable String reason) {
        this.blocked = blocked;
        this.reason = reason == null ? "" : reason;
    }

    @Override
    @SuppressWarnings({ "all", "clone" })
    public Component<EntityStore> clone() {
        AbilityTriggerBlockComponent cloned = new AbilityTriggerBlockComponent();
        cloned.blocked = this.blocked;
        cloned.reason = this.reason;
        return cloned;
    }

    @Nonnull
    public static ComponentType<EntityStore, AbilityTriggerBlockComponent> getComponentType() {
        return Realmweavers.get().getAbilityTriggerBlockComponentType();
    }
}
