package org.pixelbays.rpg.achievement.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import org.pixelbays.rpg.leveling.config.LevelUpEffects;

public class AchievementModSettings {

    public static final BuilderCodec<AchievementModSettings> CODEC = BuilderCodec
            .builder(AchievementModSettings.class, AchievementModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.enabled = value,
                    settings -> settings.enabled)
            .add()
            .append(new KeyedCodec<>("ToastNotificationsEnabled", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.toastNotificationsEnabled = value,
                    settings -> settings.toastNotificationsEnabled)
            .add()
            .append(new KeyedCodec<>("TitleNotificationsEnabled", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.titleNotificationsEnabled = value,
                    settings -> settings.titleNotificationsEnabled)
            .add()
            .append(new KeyedCodec<>("PersistProgressImmediately", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.persistProgressImmediately = value,
                    settings -> settings.persistProgressImmediately)
            .add()
                .append(new KeyedCodec<>("UnlockEffects", LevelUpEffects.CODEC, false, true),
                    (settings, value) -> settings.unlockEffects = value,
                    settings -> settings.unlockEffects)
                .add()
                .append(new KeyedCodec<>("ProgressGainedEffects", LevelUpEffects.CODEC, false, true),
                    (settings, value) -> settings.progressGainedEffects = value,
                    settings -> settings.progressGainedEffects)
                .add()
            .build();

    private boolean enabled;
    private boolean toastNotificationsEnabled;
    private boolean titleNotificationsEnabled;
    private boolean persistProgressImmediately;
            private LevelUpEffects unlockEffects;
            private LevelUpEffects progressGainedEffects;

    public AchievementModSettings() {
        this.enabled = true;
        this.toastNotificationsEnabled = true;
        this.titleNotificationsEnabled = true;
        this.persistProgressImmediately = true;
            this.unlockEffects = createDefaultUnlockEffects();
            this.progressGainedEffects = createDefaultProgressEffects();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isToastNotificationsEnabled() {
        return toastNotificationsEnabled;
    }

    public boolean isTitleNotificationsEnabled() {
        return titleNotificationsEnabled;
    }

    public boolean isPersistProgressImmediately() {
        return persistProgressImmediately;
    }

    public LevelUpEffects getUnlockEffects() {
        return unlockEffects == null ? new LevelUpEffects() : unlockEffects;
    }

    public LevelUpEffects getProgressGainedEffects() {
        return progressGainedEffects == null ? new LevelUpEffects() : progressGainedEffects;
    }

    private static LevelUpEffects createDefaultUnlockEffects() {
        LevelUpEffects effects = new LevelUpEffects();
        effects.setChatMessage("Achievement unlocked: {name} ({points} points)");
        return effects;
    }

    private static LevelUpEffects createDefaultProgressEffects() {
        LevelUpEffects effects = new LevelUpEffects();
        effects.setChatMessage("Achievement progress: {name} {current}/{target}");
        return effects;
    }
}
