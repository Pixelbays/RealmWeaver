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
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerSecondaryNameplateFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final double DEFAULT_PLAYER_HEIGHT = 1.8d;
    private static final double SECONDARY_LINE_HEIGHT_OFFSET = 0.15d;

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType = PlayerSecondaryNameplateComponent
                .getComponentType();
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        if (markerType == null || transformType == null) {
            return;
        }

        PlayerSecondaryNameplateComponent marker = archetypeChunk.getComponent(index, markerType);
        TransformComponent hologramTransform = archetypeChunk.getComponent(index, transformType);
        if (marker == null || hologramTransform == null) {
            return;
        }

        Ref<EntityStore> ownerRef = store.getExternalData().getRefFromUUID(marker.getOwnerPlayerUuid());
        if (ownerRef == null || !ownerRef.isValid()) {
            return;
        }

        TransformComponent ownerTransform = store.getComponent(ownerRef, transformType);
        if (ownerTransform == null) {
            return;
        }

        Vector3d nextPosition = new Vector3d(ownerTransform.getPosition());
        nextPosition.y += resolvePlayerHeight(store, ownerRef) + SECONDARY_LINE_HEIGHT_OFFSET;
        hologramTransform.setPosition(nextPosition);
        hologramTransform.setRotation(ownerTransform.getRotation());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType = PlayerSecondaryNameplateComponent
                .getComponentType();
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();

        Query<EntityStore> query = null;
        if (markerType != null) {
            query = markerType;
        }
        if (transformType != null) {
            query = query == null ? transformType : Query.and(query, transformType);
        }
        return query == null ? Query.any() : query;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }

    private double resolvePlayerHeight(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ownerRef) {

        BoundingBox boundingBox = store.getComponent(ownerRef, BoundingBox.getComponentType());
        if (boundingBox == null) {
            return DEFAULT_PLAYER_HEIGHT;
        }

        Box box = boundingBox.getBoundingBox();
        if (box == null) {
            return DEFAULT_PLAYER_HEIGHT;
        }

        return Math.max(DEFAULT_PLAYER_HEIGHT, box.height());
    }
}