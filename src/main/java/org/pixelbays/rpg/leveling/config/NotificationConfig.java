package org.pixelbays.rpg.leveling.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Notification configuration for level up banner
 */
public class NotificationConfig {
    public static final BuilderCodec<NotificationConfig> NOTIFICATION_CODEC = BuilderCodec
            .builder(NotificationConfig.class, NotificationConfig::new)
            .appendInherited(new KeyedCodec<>("PrimaryMessage", Codec.STRING, false, true),
                    (i, s) -> i.PrimaryMessage = s,
                    i -> i.PrimaryMessage,
                    (i, parent) -> i.PrimaryMessage = parent.PrimaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("SecondaryMessage", Codec.STRING, false, true),
                    (i, s) -> i.SecondaryMessage = s,
                    i -> i.SecondaryMessage,
                    (i, parent) -> i.SecondaryMessage = parent.SecondaryMessage)
            .add()
            .appendInherited(new KeyedCodec<>("IconItemId", Codec.STRING),
                    (i, s) -> i.IconItemId = (s == null || s.isEmpty()) ? null : s,
                    i -> i.IconItemId,
                    (i, parent) -> i.IconItemId = parent.IconItemId)
            .add()

            .build();
    public static final BuilderCodec<NotificationConfig> CODEC = NOTIFICATION_CODEC;

    private String PrimaryMessage; // Main message (supports color codes)
    private String SecondaryMessage; // Secondary message
    private String IconItemId; // Item icon ID to display

    public NotificationConfig() {
    }

    public NotificationConfig(String primaryMessage, String secondaryMessage, String iconItemId) {
        this.PrimaryMessage = primaryMessage;
        this.SecondaryMessage = secondaryMessage;
        this.IconItemId = iconItemId;
    }

    // Getters and setters
    public String getPrimaryMessage() {
        return PrimaryMessage;
    }

    public void setPrimaryMessage(String primaryMessage) {
        this.PrimaryMessage = primaryMessage;
    }

    public String getSecondaryMessage() {
        return SecondaryMessage;
    }

    public void setSecondaryMessage(String secondaryMessage) {
        this.SecondaryMessage = secondaryMessage;
    }

    public String getIconItemId() {
        return IconItemId;
    }

    public void setIconItemId(String iconItemId) {
        this.IconItemId = iconItemId;
    }
}
