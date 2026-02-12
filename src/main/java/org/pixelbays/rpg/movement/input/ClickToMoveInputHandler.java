package org.pixelbays.rpg.movement.input;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.RpgModConfig.CameraStyle;
import org.pixelbays.rpg.global.config.RpgModConfig.TargetingStyle;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ClickToMoveInputHandler {

    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        var assetMap = RpgModConfig.getAssetMap();
        RpgModConfig config = assetMap != null ? assetMap.getAsset("default") : null;

        if (!isMobaClickToMoveEnabled(config)) {
            RpgLogging.debugDeveloper("[ClickToMove] Disabled by config: targeting=%s camera=%s", 
                    config != null ? config.getTargetingStyle() : null,
                    config != null ? config.getCameraStyle() : null);
            return false;
        }

        SyncInteractionChain primaryCandidate = null;
        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain == null || !chain.initial) {
                continue;
            }

            if (chain.interactionType != InteractionType.Secondary && chain.interactionType != InteractionType.Primary) {
                continue;
            }

            if (chain.interactionType == InteractionType.Primary && primaryCandidate == null) {
                primaryCandidate = chain;
                continue;
            }

            InteractionChainData data = chain.data;
            RpgLogging.debugDeveloper("[ClickToMove] Input: type=%s hitLocation=%s blockPosition=%s interactionDataCount=%s", 
                    chain.interactionType,
                    data != null ? data.hitLocation : null,
                    data != null ? data.blockPosition : null,
                    chain.interactionData != null ? chain.interactionData.length : 0);

            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                RpgLogging.debugDeveloper("[ClickToMove] Invalid player ref; ignoring click.");
                return false;
            }

            Vector3d target = resolveTarget(chain);
            if (target == null) {
                target = resolveTargetFromPacket(syncPacket);
            }
            if (target == null) {
                logNoTarget(chain);
            }


            return true;
        }

        if (primaryCandidate != null) {
            return handlePrimaryFallback(primaryCandidate, syncPacket, playerRef);
        }

        return false;
    }

    private boolean handlePrimaryFallback(@Nonnull SyncInteractionChain chain, @Nonnull SyncInteractionChains packet,
            @Nonnull PlayerRef playerRef) {
        InteractionChainData data = chain.data;
        RpgLogging.debugDeveloper("[ClickToMove] Primary fallback input: hitLocation=%s blockPosition=%s interactionDataCount=%s",
                data != null ? data.hitLocation : null,
                data != null ? data.blockPosition : null,
                chain.interactionData != null ? chain.interactionData.length : 0);

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            RpgLogging.debugDeveloper("[ClickToMove] Invalid player ref; ignoring primary fallback.");
            return false;
        }

        Store<EntityStore> store = entityRef.getStore();
        World world = store.getExternalData().getWorld();
        Vector3d target = resolveTarget(chain);
        if (target == null) {
            target = resolveTargetFromPacket(packet);
        }
        if (target == null) {
            logNoTarget(chain);
        }

        final Vector3d finalTarget = target;
        if (target != null) {
            RpgLogging.debugDeveloper("[ClickToMove] Primary fallback target set: player=%s target=(%.2f, %.2f, %.2f)",
                playerRef.getUsername(), target.getX(), target.getY(), target.getZ());
        } else {
            RpgLogging.debugDeveloper("[ClickToMove] Primary fallback target set: player=%s target=<fallback>",
                playerRef.getUsername());
        }
        return true;
    }

    private static boolean isMobaClickToMoveEnabled(@Nullable RpgModConfig config) {
        if (config == null) {
            return false;
        }

        if (config.getTargetingStyle() != TargetingStyle.MOBA) {
            return false;
        }

        CameraStyle cameraStyle = config.getCameraStyle();
        return cameraStyle == CameraStyle.Isometric || cameraStyle == CameraStyle.TopDown;
    }

    @Nullable
    private static Vector3d resolveTarget(@Nonnull SyncInteractionChain chain) {
        InteractionChainData data = chain.data;
        if (data != null) {
            Vector3d fromData = resolveTargetFromData(data);
            if (fromData != null) {
                return fromData;
            }
        }

        if (chain.interactionData != null) {
            for (var syncData : chain.interactionData) {
                if (syncData == null) {
                    continue;
                }

                if (syncData.raycastHit != null) {
                    return new Vector3d(syncData.raycastHit.x, syncData.raycastHit.y, syncData.raycastHit.z);
                }

                if (syncData.blockPosition != null) {
                    return new Vector3d(syncData.blockPosition.x + 0.5, syncData.blockPosition.y + 1.0,
                            syncData.blockPosition.z + 0.5);
                }
            }
        }

        return null;
    }

    private static Vector3d resolveTargetFromPacket(@Nonnull SyncInteractionChains packet) {
        if (packet.updates == null) {
            return null;
        }

        for (SyncInteractionChain other : packet.updates) {
            if (other == null) {
                continue;
            }

            Vector3d target = resolveTarget(other);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    private static void logNoTarget(@Nonnull SyncInteractionChain chain) {
        InteractionChainData data = chain.data;
        RpgLogging.debugDeveloper("[ClickToMove] No target resolved. type=%s hitLocation=%s blockPosition=%s interactionDataCount=%s",
                chain.interactionType,
                data != null ? data.hitLocation : null,
                data != null ? data.blockPosition : null,
                chain.interactionData != null ? chain.interactionData.length : 0);

        if (chain.interactionData != null) {
            int index = 0;
            for (var syncData : chain.interactionData) {
                if (syncData == null) {
                    index++;
                    continue;
                }
                RpgLogging.debugDeveloper(
                        "[ClickToMove] interactionData[%d]: raycastHit=%s blockPosition=%s blockFace=%s movementDirection=%s entityId=%s",
                        index,
                        syncData.raycastHit,
                        syncData.blockPosition,
                        syncData.blockFace,
                        syncData.movementDirection,
                        syncData.entityId);
                index++;
            }
        }
    }

    private static Vector3d resolveTargetFromData(@Nonnull InteractionChainData data) {
        Vector3f hitLocation = data.hitLocation;
        if (hitLocation != null) {
            return new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z);
        }

        BlockPosition blockPosition = data.blockPosition;
        if (blockPosition != null) {
            return new Vector3d(blockPosition.x + 0.5, blockPosition.y + 1.0, blockPosition.z + 0.5);
        }

        return null;
    }

    private static Vector3d computeFallbackTarget(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> entityRef) {
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }

        float yaw = 0.0F;
        HeadRotation headRotation = store.getComponent(entityRef, HeadRotation.getComponentType());
        if (headRotation != null) {
            yaw = headRotation.getRotation().getYaw();
        } else {
            yaw = transform.getRotation().getYaw();
        }

        Vector3d direction = new Vector3d();
        PhysicsMath.vectorFromAngles(yaw, 0.0F, direction);
        direction.y = 0.0;
        double len = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (len < 1e-6) {
            direction.assign(0.0, 0.0, 1.0);
            len = 1.0;
        }

        direction.x /= len;
        direction.z /= len;

        Vector3d pos = transform.getPosition();
        double distance = 8.0;
        return new Vector3d(pos.x + direction.x * distance, pos.y, pos.z + direction.z * distance);
    }
}
