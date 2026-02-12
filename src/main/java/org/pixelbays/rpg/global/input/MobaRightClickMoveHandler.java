package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.camera.RpgCameraController;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.MobaMountController;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.SelectedHitEntity;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class MobaRightClickMoveHandler {

    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        RpgModConfig config = RpgCameraController.resolveConfig();
        if (!MobaMountController.shouldEnableMobaMount(config)) {
            return false;
        }

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType != InteractionType.Secondary || !chain.initial) {
                continue;
            }

            InteractionSyncData interactionData = getPrimaryInteractionData(chain);

            Ref<EntityStore> playerEntityRef = playerRef.getReference();
            if (playerEntityRef == null || !playerEntityRef.isValid()) {
                return false;
            }

            Store<EntityStore> store = playerEntityRef.getStore();
            var world = store.getExternalData().getWorld();
            world.execute(() -> handlePrimaryClickOnWorldThread(store, playerRef, chain, interactionData));
            return true; // do not consume; avoid interaction desync
        }

        return false;
    }

    @Nullable
    private static InteractionSyncData getPrimaryInteractionData(@Nonnull SyncInteractionChain chain) {
        InteractionSyncData[] interactionData = chain.interactionData;
        if (interactionData == null || interactionData.length == 0) {
            return null;
        }

        for (InteractionSyncData data : interactionData) {
            if (data != null) {
                return data;
            }
        }

        return null;
    }

    @Nullable
    private static Vector3d resolveTargetPosition(
            @Nonnull PlayerRef playerRef,
            @Nonnull SyncInteractionChain chain,
            @Nullable InteractionSyncData data) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return null;
        }

        Store<EntityStore> store = playerEntityRef.getStore();

        // Prefer chain hitLocation if provided (works even when interactionData is missing)
        var chainData = chain.data;
        if (chainData != null) {
            Vector3f hitLocation = chainData.hitLocation;
            if (hitLocation != null) {
                return new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z);
            }

            BlockPosition blockPosition = chainData.blockPosition;
            if (blockPosition != null) {
                return new Vector3d(blockPosition.x + 0.5, blockPosition.y + 0.5, blockPosition.z + 0.5);
            }
        }

        if (data != null) {
            SelectedHitEntity[] hitEntities = data.hitEntities;
            if (hitEntities != null && hitEntities.length > 0) {
                int networkId = hitEntities[0].networkId;
                Ref<EntityStore> hitRef = store.getExternalData().getRefFromNetworkId(networkId);
                if (hitRef != null && hitRef.isValid()) {
                    TransformComponent targetTransform = store.getComponent(hitRef, TransformComponent.getComponentType());
                    if (targetTransform != null) {
                        return new Vector3d(targetTransform.getPosition());
                    }
                }
            }
        }

        if (chainData != null && chainData.entityId > 0) {
            Ref<EntityStore> hitRef = store.getExternalData().getRefFromNetworkId(chainData.entityId);
            if (hitRef != null && hitRef.isValid()) {
                TransformComponent targetTransform = store.getComponent(hitRef, TransformComponent.getComponentType());
                if (targetTransform != null) {
                    return new Vector3d(targetTransform.getPosition());
                }
            }
        }

        if (data != null) {
            Position hit = data.raycastHit;
            if (hit != null) {
                return new Vector3d(hit.x, hit.y, hit.z);
            }
        }

        return null;
    }

    private static void handlePrimaryClickOnWorldThread(
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull SyncInteractionChain chain,
            @Nullable InteractionSyncData data) {
        Vector3d target = resolveTargetPosition(playerRef, chain, data);
        if (target == null) {
            return;
        }

        Vector3d clampedTarget = clampToMaxRange(playerRef, target, MobaMountController.MOBA_MAX_MOVE_RANGE);
        if (clampedTarget == null) {
            return;
        }

        MobaMountController.teleportPlayerTo(playerRef, clampedTarget);
        MobaMountController.moveMountedNpcToPlayer(playerRef);
    }

    @Nullable
    private static Vector3d clampToMaxRange(
            @Nonnull PlayerRef playerRef,
            @Nonnull Vector3d target,
            double maxRange) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return null;
        }

        Store<EntityStore> store = playerEntityRef.getStore();
        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }

        Vector3d playerPos = transform.getPosition();
        Vector3d delta = new Vector3d(target).subtract(playerPos);
        double distance = delta.length();
        if (distance <= maxRange || maxRange <= 0.0) {
            return target;
        }

        delta.setLength(maxRange);
        return new Vector3d(playerPos).add(delta);
    }
}
