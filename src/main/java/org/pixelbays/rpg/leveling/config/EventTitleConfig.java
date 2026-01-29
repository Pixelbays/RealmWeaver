package org.pixelbays.rpg.leveling.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event title configuration for level up (big screen title style)
 */
public class EventTitleConfig {
    public static final BuilderCodec<EventTitleConfig> EVENT_TITLE_CODEC = BuilderCodec
            .builder(EventTitleConfig.class, EventTitleConfig::new)
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
            .appendInherited(new KeyedCodec<>("Major", Codec.BOOLEAN, false, true), (i, s) -> i.Major = s, i -> i.Major,
                    (i, parent) -> i.Major = parent.Major)
            .add()
            .build();

            
    public static final BuilderCodec<EventTitleConfig> CODEC = EVENT_TITLE_CODEC;

    private String PrimaryMessage; // Main message
    private String SecondaryMessage; // Secondary message
    private boolean Major; // Major style

    public EventTitleConfig() {
    }

    public EventTitleConfig(String primaryMessage, String secondaryMessage, boolean major) {
        this.PrimaryMessage = primaryMessage;
        this.SecondaryMessage = secondaryMessage;
        this.Major = major;
    }

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

    public boolean isMajor() {
        return Major;
    }

    public void setMajor(boolean major) {
        this.Major = major;
    }
}
