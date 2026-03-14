package org.pixelbays.rpg.camera;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.camera.config.settings.CameraModSettings.CameraSettings;
import org.pixelbays.rpg.camera.config.settings.CameraModSettings.CameraStyle;
import org.pixelbays.rpg.camera.config.settings.CameraModSettings.TargetingStyle;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.protocol.AttachedToType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class RpgCameraController implements Consumer<PlayerReadyEvent> {

    // Delay MOBA camera to avoid client join crash
    public static final long MOBA_CAMERA_DELAY_MS = 500L;

    @Override
    public void accept(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            return;
        }

        CameraStyle style = config.getCameraStyle();
        if (style == null || style == CameraStyle.Vanilla || style == CameraStyle.PlayerConfig) {
            return;
        }

        if (config.getTargetingStyle() == TargetingStyle.MOBA && style == CameraStyle.Isometric) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> applyCameraStyle(playerRef, style, -1, config),
                    MOBA_CAMERA_DELAY_MS,
                    TimeUnit.MILLISECONDS);
            return;
        }

        applyCameraStyle(playerRef, style, -1, config);

        // Logging handled in applyCameraStyle
    }

        public static void applyCameraStyle(@Nonnull PlayerRef playerRef, @Nonnull CameraStyle style, int attachedEntityId,
            @Nonnull RpgModConfig config) {
        switch (style) {
            case ThirdPersonOnly -> playerRef.getPacketHandler()
                    .writeNoCache(new SetServerCamera(ClientCameraView.ThirdPerson, true, null));
            case TopDown -> playerRef.getPacketHandler()
                .writeNoCache(new SetServerCamera(ClientCameraView.Custom, true,
                    applyAttachment(buildCustom(config.getCameraSettingsTopDown(),
                        CameraSettings.topDownDefaults()), attachedEntityId)));
            case Isometric -> playerRef.getPacketHandler()
                .writeNoCache(new SetServerCamera(ClientCameraView.Custom, true,
                    applyAttachment(buildCustom(config.getCameraSettingsIsometric(),
                        CameraSettings.isometricDefaults()), attachedEntityId)));
            default -> {
            }
        }

        RpgLogging.debugDeveloper("[Camera] Applied camera style %s to %s (attached=%s)", style,
                playerRef.getUsername(), attachedEntityId > 0 ? attachedEntityId : "local");
    }

    private static ServerCameraSettings applyAttachment(ServerCameraSettings settings, int attachedEntityId) {
        if (attachedEntityId > 0) {
            settings.attachedToType = AttachedToType.EntityId;
            settings.attachedToEntityId = attachedEntityId;
        }

        return settings;
    }

    private static ServerCameraSettings buildCustom(CameraSettings settings,
            CameraSettings fallback) {
        CameraSettings resolved = settings != null ? settings : fallback;
        ServerCameraSettings cameraSettings = new ServerCameraSettings();
        cameraSettings.positionLerpSpeed = resolved.getPositionLerpSpeed();
        cameraSettings.rotationLerpSpeed = resolved.getRotationLerpSpeed();
        cameraSettings.distance = resolved.getDistance();
        cameraSettings.displayCursor = resolved.isDisplayCursor();
        cameraSettings.isFirstPerson = resolved.isFirstPerson();
        cameraSettings.movementForceRotationType = resolved.getMovementForceRotationType();
        cameraSettings.eyeOffset = resolved.isEyeOffset();
        cameraSettings.positionDistanceOffsetType = resolved.getPositionDistanceOffsetType();
        cameraSettings.rotationType = resolved.getRotationType();
        cameraSettings.rotation = new Direction(resolved.getRotationPitchRadians(),
            resolved.getRotationYawRadians(), resolved.getRotationRollRadians());
        cameraSettings.mouseInputType = resolved.getMouseInputType();
        cameraSettings.planeNormal = new Vector3f(resolved.getPlaneNormalX(), resolved.getPlaneNormalY(),
            resolved.getPlaneNormalZ());
        return cameraSettings;
    }

    public static RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("default");
        if (config != null) {
            return config;
        }

        config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }
}
