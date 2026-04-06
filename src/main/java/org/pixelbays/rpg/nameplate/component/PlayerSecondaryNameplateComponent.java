package org.pixelbays.rpg.nameplate.component;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerSecondaryNameplateComponent implements Component<EntityStore> {

    public static final BuilderCodec<PlayerSecondaryNameplateComponent> CODEC = BuilderCodec
            .builder(PlayerSecondaryNameplateComponent.class, PlayerSecondaryNameplateComponent::new)
            .append(new KeyedCodec<>("OwnerPlayerUuid", Codec.UUID_BINARY),
                    (component, value) -> component.ownerPlayerUuid = value,
                    component -> component.ownerPlayerUuid)
            .add()
            .build();

    private static ComponentType<EntityStore, PlayerSecondaryNameplateComponent> componentType;

    @Nonnull
    private UUID ownerPlayerUuid;

    public PlayerSecondaryNameplateComponent() {
        this.ownerPlayerUuid = new UUID(0L, 0L);
    }

    public PlayerSecondaryNameplateComponent(@Nonnull UUID ownerPlayerUuid) {
        this.ownerPlayerUuid = ownerPlayerUuid;
    }

    @Nonnull
    public static ComponentType<EntityStore, PlayerSecondaryNameplateComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(
            @Nonnull ComponentType<EntityStore, PlayerSecondaryNameplateComponent> componentType) {
        PlayerSecondaryNameplateComponent.componentType = componentType;
    }

    @Nonnull
    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerSecondaryNameplateComponent(ownerPlayerUuid);
    }
}