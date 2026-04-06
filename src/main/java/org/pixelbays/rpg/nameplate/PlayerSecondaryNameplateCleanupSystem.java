package org.pixelbays.rpg.nameplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.nameplate.component.PlayerSecondaryNameplateComponent;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerSecondaryNameplateCleanupSystem extends DelayedEntitySystem<EntityStore> {

    private static final float DEFAULT_INTERVAL_SECONDS = 1.0f;

    public PlayerSecondaryNameplateCleanupSystem() {
        super(DEFAULT_INTERVAL_SECONDS);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType = PlayerSecondaryNameplateComponent
                .getComponentType();
        if (markerType == null) {
            return;
        }

        PlayerSecondaryNameplateComponent marker = archetypeChunk.getComponent(index, markerType);
        if (marker == null) {
            return;
        }

        Ref<EntityStore> ownerRef = store.getExternalData().getRefFromUUID(marker.getOwnerPlayerUuid());
        if (ownerRef == null || !ownerRef.isValid()) {
            commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
            return;
        }

        if (store.getComponent(ownerRef, Player.getComponentType()) == null
                || store.getComponent(ownerRef, PlayerRef.getComponentType()) == null) {
            commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType = PlayerSecondaryNameplateComponent
                .getComponentType();
        return markerType == null ? Query.any() : markerType;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }
}