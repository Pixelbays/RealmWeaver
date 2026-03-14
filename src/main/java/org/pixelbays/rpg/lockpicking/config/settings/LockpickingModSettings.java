package org.pixelbays.rpg.lockpicking.config.settings;

import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

public class LockpickingModSettings {

    public static class LockpickingDifficultyTier {
        public static final BuilderCodec<LockpickingDifficultyTier> CODEC = BuilderCodec
                .builder(LockpickingDifficultyTier.class, LockpickingDifficultyTier::new)
                .append(new KeyedCodec<>("PinCount", Codec.INTEGER, false, true),
                        (i, s) -> i.pinCount = s, i -> i.pinCount)
                .add()
                .append(new KeyedCodec<>("TimeLimitSeconds", Codec.FLOAT, false, true),
                        (i, s) -> i.timeLimitSeconds = s, i -> i.timeLimitSeconds)
                .add()
                .append(new KeyedCodec<>("SweetSpotSize", Codec.FLOAT, false, true),
                        (i, s) -> i.sweetSpotSize = s, i -> i.sweetSpotSize)
                .add()
                .append(new KeyedCodec<>("NeedleSpeed", Codec.FLOAT, false, true),
                        (i, s) -> i.needleSpeed = s, i -> i.needleSpeed)
                .add()
                .append(new KeyedCodec<>("SweetSpotSizeScale", Codec.FLOAT, false, true),
                        (i, s) -> i.sweetSpotSizeScale = s, i -> i.sweetSpotSizeScale)
                .add()
                .append(new KeyedCodec<>("NeedleSpeedScale", Codec.FLOAT, false, true),
                        (i, s) -> i.needleSpeedScale = s, i -> i.needleSpeedScale)
                .add()
                .append(new KeyedCodec<>("MaxMistakes", Codec.INTEGER, false, true),
                        (i, s) -> i.maxMistakes = s, i -> i.maxMistakes)
                .add()
                .append(new KeyedCodec<>("DoorUnlockTime", Codec.INTEGER, false, true),
                        (i, s) -> i.doorUnlockTime = s, i -> i.doorUnlockTime)
                .add()
                .build();

        private int pinCount;
        private float timeLimitSeconds;
        private float sweetSpotSize;
        private float needleSpeed;
        private float sweetSpotSizeScale;
        private float needleSpeedScale;
        private int maxMistakes;
        private int doorUnlockTime;

        public LockpickingDifficultyTier() {
            this.pinCount = 3;
            this.timeLimitSeconds = 20.0f;
            this.sweetSpotSize = 0.18f;
            this.needleSpeed = 0.45f;
            this.sweetSpotSizeScale = 1.0f;
            this.needleSpeedScale = 1.0f;
            this.maxMistakes = 2;
            this.doorUnlockTime = 5;
        }

        public int getPinCount() {
            return pinCount;
        }

        public float getTimeLimitSeconds() {
            return timeLimitSeconds;
        }

        public float getSweetSpotSize() {
            return sweetSpotSize;
        }

        public float getNeedleSpeed() {
            return needleSpeed;
        }

        public float getSweetSpotSizeScale() {
            return sweetSpotSizeScale;
        }

        public float getNeedleSpeedScale() {
            return needleSpeedScale;
        }

        public int getMaxMistakes() {
            return maxMistakes;
        }

        public int getDoorUnlockTime() {
            return doorUnlockTime;
        }
    }

    public static final BuilderCodec<LockpickingModSettings> CODEC = BuilderCodec
            .builder(LockpickingModSettings.class, LockpickingModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("LockpickItemTag", Codec.STRING, false, true),
                    (i, s) -> i.lockpickItemTag = s, i -> i.lockpickItemTag)
            .add()
            .append(new KeyedCodec<>("LockpickingDifficultyTiers",
                    new MapCodec<>(LockpickingDifficultyTier.CODEC, HashMap::new, false), true),
                    (i, s) -> i.lockpickingDifficultyTiers = s, i -> i.lockpickingDifficultyTiers)
            .add()
            .build();

    private boolean enabled;
    private String lockpickItemTag;
    private Map<String, LockpickingDifficultyTier> lockpickingDifficultyTiers;

    public LockpickingModSettings() {
        this.enabled = true;
        this.lockpickItemTag = "Lockpick";
        this.lockpickingDifficultyTiers = new HashMap<>();
    }

    public String getLockpickItemTag() {
        return lockpickItemTag;
    }

    public Map<String, LockpickingDifficultyTier> getLockpickingDifficultyTiers() {
        return lockpickingDifficultyTiers;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
