package org.pixelbays.rpg.leveling.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Level up effects (sound, particles, notification)
 */
public class LevelUpEffects {
    public static final BuilderCodec<LevelUpEffects> LEVEL_UP_EFFECTS_CODEC = BuilderCodec
            .builder(LevelUpEffects.class, LevelUpEffects::new)
            .appendInherited(new KeyedCodec<>("SoundId", Codec.STRING, false, true), (i, s) -> i.SoundId = s,
                    i -> i.SoundId,
                    (i, parent) -> i.SoundId = parent.SoundId)
            .add()
            .appendInherited(new KeyedCodec<>("ParticleEffect", Codec.STRING, false, true),
                    (i, s) -> i.ParticleEffect = s,
                    i -> i.ParticleEffect,
                    (i, parent) -> i.ParticleEffect = parent.ParticleEffect)
            .add()
            .appendInherited(new KeyedCodec<>("Notification", NotificationConfig.CODEC, false, true),
                    (i, s) -> i.Notification = s, i -> i.Notification,
                    (i, parent) -> i.Notification = parent.Notification)
            .add()
            .appendInherited(new KeyedCodec<>("EventTitle", EventTitleConfig.CODEC, false, true),
                    (i, s) -> i.EventTitle = s, i -> i.EventTitle,
                    (i, parent) -> i.EventTitle = parent.EventTitle)
            .add()
            .appendInherited(new KeyedCodec<>("ChatMessage", Codec.STRING, false, true), (i, s) -> i.ChatMessage = s,
                    i -> i.ChatMessage,
                    (i, parent) -> i.ChatMessage = parent.ChatMessage)
            .add()
            .appendInherited(new KeyedCodec<>("KillFeedPopup", Codec.STRING, false, true),
                    (i, s) -> i.KillFeedPopup = s,
                    i -> i.KillFeedPopup,
                    (i, parent) -> i.KillFeedPopup = parent.KillFeedPopup)
            .add()
            .build();
    public static final BuilderCodec<LevelUpEffects> CODEC = LEVEL_UP_EFFECTS_CODEC;

    private String SoundId; // Sound asset ID (e.g., "SFX_Level_Up")
    private String ParticleEffect; // Particle effect ID
    private NotificationConfig Notification; // Notification banner config
    private EventTitleConfig EventTitle; // Event title popup config
    private String ChatMessage; // Chat message to send (supports placeholders)
    private String KillFeedPopup; // Kill feed popup message (supports placeholders)

    public LevelUpEffects() {
        // Empty by default
    }

    public boolean isEmpty() {
        return SoundId == null && ParticleEffect == null && Notification == null && EventTitle == null
                && ChatMessage == null && KillFeedPopup == null;
    }

    // Getters and setters
    public String getSoundId() {
        return SoundId;
    }

    public void setSoundId(String soundId) {
        this.SoundId = soundId;
    }

    public String getParticleEffect() {
        return ParticleEffect;
    }

    public void setParticleEffect(String particleEffect) {
        this.ParticleEffect = particleEffect;
    }

    public NotificationConfig getNotification() {
        return Notification;
    }

    public void setNotification(NotificationConfig notification) {
        this.Notification = notification;
    }

    public EventTitleConfig getEventTitle() {
        return EventTitle;
    }

    public void setEventTitle(EventTitleConfig eventTitle) {
        this.EventTitle = eventTitle;
    }

    public String getChatMessage() {
        return ChatMessage;
    }

    public void setChatMessage(String chatMessage) {
        this.ChatMessage = chatMessage;
    }

    public String getKillFeedPopup() {
        return KillFeedPopup;
    }

    public void setKillFeedPopup(String killFeedPopup) {
        this.KillFeedPopup = killFeedPopup;
    }
}
