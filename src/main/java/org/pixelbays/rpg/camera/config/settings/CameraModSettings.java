package org.pixelbays.rpg.camera.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;

public class CameraModSettings {

    public enum TargetingStyle {
        Vanilla,
        TabTargeting,
        MOBA,
        PlayerConfig,
    }

    public enum CameraStyle {
        Vanilla,
        ThirdPersonOnly,
        Isometric,
        TopDown,
        PlayerConfig,
    }

    public static class CameraSettings {
        public static final BuilderCodec<CameraSettings> CODEC = BuilderCodec
                .builder(CameraSettings.class, CameraSettings::new)
                .append(new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, false, true),
                        (i, s) -> i.positionLerpSpeed = s, i -> i.positionLerpSpeed)
                .add()
                .append(new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, false, true),
                        (i, s) -> i.rotationLerpSpeed = s, i -> i.rotationLerpSpeed)
                .add()
                .append(new KeyedCodec<>("Distance", Codec.FLOAT, false, true),
                        (i, s) -> i.distance = s, i -> i.distance)
                .add()
                .append(new KeyedCodec<>("DisplayCursor", Codec.BOOLEAN, false, true),
                        (i, s) -> i.displayCursor = s, i -> i.displayCursor)
                .add()
                .append(new KeyedCodec<>("IsFirstPerson", Codec.BOOLEAN, false, true),
                        (i, s) -> i.isFirstPerson = s, i -> i.isFirstPerson)
                .add()
                .append(new KeyedCodec<>("MovementForceRotationType",
                        new EnumCodec<>(MovementForceRotationType.class), false, true),
                        (i, s) -> i.movementForceRotationType = s, i -> i.movementForceRotationType)
                .add()
                .append(new KeyedCodec<>("EyeOffset", Codec.BOOLEAN, false, true),
                        (i, s) -> i.eyeOffset = s, i -> i.eyeOffset)
                .add()
                .append(new KeyedCodec<>("PositionDistanceOffsetType",
                        new EnumCodec<>(PositionDistanceOffsetType.class), false, true),
                        (i, s) -> i.positionDistanceOffsetType = s, i -> i.positionDistanceOffsetType)
                .add()
                .append(new KeyedCodec<>("RotationType", new EnumCodec<>(RotationType.class), false, true),
                        (i, s) -> i.rotationType = s, i -> i.rotationType)
                .add()
                .append(new KeyedCodec<>("RotationPitchRadians", Codec.FLOAT, false, true),
                        (i, s) -> i.rotationPitchRadians = s, i -> i.rotationPitchRadians)
                .add()
                .append(new KeyedCodec<>("RotationYawRadians", Codec.FLOAT, false, true),
                        (i, s) -> i.rotationYawRadians = s, i -> i.rotationYawRadians)
                .add()
                .append(new KeyedCodec<>("RotationRollRadians", Codec.FLOAT, false, true),
                        (i, s) -> i.rotationRollRadians = s, i -> i.rotationRollRadians)
                .add()
                .append(new KeyedCodec<>("MouseInputType", new EnumCodec<>(MouseInputType.class), false, true),
                        (i, s) -> i.mouseInputType = s, i -> i.mouseInputType)
                .add()
                .append(new KeyedCodec<>("PlaneNormalX", Codec.FLOAT, false, true),
                        (i, s) -> i.planeNormalX = s, i -> i.planeNormalX)
                .add()
                .append(new KeyedCodec<>("PlaneNormalY", Codec.FLOAT, false, true),
                        (i, s) -> i.planeNormalY = s, i -> i.planeNormalY)
                .add()
                .append(new KeyedCodec<>("PlaneNormalZ", Codec.FLOAT, false, true),
                        (i, s) -> i.planeNormalZ = s, i -> i.planeNormalZ)
                .add()
                .build();

        private float positionLerpSpeed;
        private float rotationLerpSpeed;
        private float distance;
        private boolean displayCursor;
        private boolean isFirstPerson;
        private MovementForceRotationType movementForceRotationType;
        private boolean eyeOffset;
        private PositionDistanceOffsetType positionDistanceOffsetType;
        private RotationType rotationType;
        private float rotationPitchRadians;
        private float rotationYawRadians;
        private float rotationRollRadians;
        private MouseInputType mouseInputType;
        private float planeNormalX;
        private float planeNormalY;
        private float planeNormalZ;

        public CameraSettings() {
            this.positionLerpSpeed = 0.2F;
            this.rotationLerpSpeed = 0.2F;
            this.distance = 20.0F;
            this.displayCursor = true;
            this.isFirstPerson = false;
            this.movementForceRotationType = MovementForceRotationType.Custom;
            this.eyeOffset = true;
            this.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
            this.rotationType = RotationType.Custom;
            this.rotationPitchRadians = 0.0F;
            this.rotationYawRadians = 0.0F;
            this.rotationRollRadians = 0.0F;
            this.mouseInputType = MouseInputType.LookAtPlane;
            this.planeNormalX = 0.0F;
            this.planeNormalY = 1.0F;
            this.planeNormalZ = 0.0F;
        }

        public static CameraSettings topDownDefaults() {
            CameraSettings settings = new CameraSettings();
            settings.distance = 20.0F;
            settings.rotationPitchRadians = 0.0F;
            settings.rotationYawRadians = (float) (-Math.PI / 2.0);
            return settings;
        }

        public static CameraSettings isometricDefaults() {
            CameraSettings settings = new CameraSettings();
            settings.distance = 18.0F;
            settings.rotationPitchRadians = (float) (Math.PI / 4.0);
            settings.rotationYawRadians = (float) (-Math.PI / 4.0);
            return settings;
        }

        public float getPositionLerpSpeed() {
            return positionLerpSpeed;
        }

        public float getRotationLerpSpeed() {
            return rotationLerpSpeed;
        }

        public float getDistance() {
            return distance;
        }

        public boolean isDisplayCursor() {
            return displayCursor;
        }

        public boolean isFirstPerson() {
            return isFirstPerson;
        }

        public MovementForceRotationType getMovementForceRotationType() {
            return movementForceRotationType;
        }

        public boolean isEyeOffset() {
            return eyeOffset;
        }

        public PositionDistanceOffsetType getPositionDistanceOffsetType() {
            return positionDistanceOffsetType;
        }

        public RotationType getRotationType() {
            return rotationType;
        }

        public float getRotationPitchRadians() {
            return rotationPitchRadians;
        }

        public float getRotationYawRadians() {
            return rotationYawRadians;
        }

        public float getRotationRollRadians() {
            return rotationRollRadians;
        }

        public MouseInputType getMouseInputType() {
            return mouseInputType;
        }

        public float getPlaneNormalX() {
            return planeNormalX;
        }

        public float getPlaneNormalY() {
            return planeNormalY;
        }

        public float getPlaneNormalZ() {
            return planeNormalZ;
        }
    }

    public static final BuilderCodec<CameraModSettings> CODEC = BuilderCodec
            .builder(CameraModSettings.class, CameraModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("TargetingStyle", new EnumCodec<>(TargetingStyle.class), false, true),
                    (i, s) -> i.targetingStyle = s, i -> i.targetingStyle)
            .add()
            .append(new KeyedCodec<>("CameraStyle", new EnumCodec<>(CameraStyle.class), false, true),
                    (i, s) -> i.cameraStyle = s, i -> i.cameraStyle)
            .add()
            .append(new KeyedCodec<>("CameraSettingsTopDown", CameraSettings.CODEC, false, true),
                    (i, s) -> i.cameraSettingsTopDown = s, i -> i.cameraSettingsTopDown)
            .add()
            .append(new KeyedCodec<>("CameraSettingsIsometric", CameraSettings.CODEC, false, true),
                    (i, s) -> i.cameraSettingsIsometric = s, i -> i.cameraSettingsIsometric)
            .add()
            .build();

    private boolean enabled;
    private TargetingStyle targetingStyle;
    private CameraStyle cameraStyle;
    private CameraSettings cameraSettingsTopDown;
    private CameraSettings cameraSettingsIsometric;

    public CameraModSettings() {
        this.enabled = true;
        this.targetingStyle = TargetingStyle.Vanilla;
        this.cameraStyle = CameraStyle.Vanilla;
        this.cameraSettingsTopDown = CameraSettings.topDownDefaults();
        this.cameraSettingsIsometric = CameraSettings.isometricDefaults();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TargetingStyle getTargetingStyle() {
        return targetingStyle;
    }

    public CameraStyle getCameraStyle() {
        return cameraStyle;
    }

    public CameraSettings getCameraSettingsTopDown() {
        return cameraSettingsTopDown;
    }

    public CameraSettings getCameraSettingsIsometric() {
        return cameraSettingsIsometric;
    }
}
