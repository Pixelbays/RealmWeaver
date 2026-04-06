package org.pixelbays.rpg.global.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class NameplateModSettings {

    public enum NameSource {
        AccountName,
        CharacterName
    }

    public enum TitlePlacement {
        Prefix,
        Suffix,
        AboveName,
        BelowName
    }

    public enum GuildNamePlacement {
        AboveName,
        BelowName
    }

    public enum GuildTagPlacement {
        Prefix,
        Suffix
    }

    public static final BuilderCodec<NameplateModSettings> CODEC = BuilderCodec
            .builder(NameplateModSettings.class, NameplateModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.enabled = value,
                    settings -> settings.enabled)
            .add()
            .append(new KeyedCodec<>("ShowName", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.showName = value,
                    settings -> settings.showName)
            .add()
            .append(new KeyedCodec<>("NameSource", new EnumCodec<>(NameSource.class), false, true),
                    (settings, value) -> settings.nameSource = value,
                    settings -> settings.nameSource)
            .add()
            .append(new KeyedCodec<>("ShowTitle", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.showTitle = value,
                    settings -> settings.showTitle)
            .add()
            .append(new KeyedCodec<>("TitlePlacement", new EnumCodec<>(TitlePlacement.class), false, true),
                    (settings, value) -> settings.titlePlacement = value,
                    settings -> settings.titlePlacement)
            .add()
            .append(new KeyedCodec<>("ShowGuildName", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.showGuildName = value,
                    settings -> settings.showGuildName)
            .add()
            .append(new KeyedCodec<>("GuildNamePlacement", new EnumCodec<>(GuildNamePlacement.class), false, true),
                    (settings, value) -> settings.guildNamePlacement = value,
                    settings -> settings.guildNamePlacement)
            .add()
            .append(new KeyedCodec<>("ShowGuildTag", Codec.BOOLEAN, false, true),
                    (settings, value) -> settings.showGuildTag = value,
                    settings -> settings.showGuildTag)
            .add()
            .append(new KeyedCodec<>("GuildTagPlacement", new EnumCodec<>(GuildTagPlacement.class), false, true),
                    (settings, value) -> settings.guildTagPlacement = value,
                    settings -> settings.guildTagPlacement)
            .add()
            .build();

    private boolean enabled;
    private boolean showName;
    private NameSource nameSource;
    private boolean showTitle;
    private TitlePlacement titlePlacement;
    private boolean showGuildName;
    private GuildNamePlacement guildNamePlacement;
    private boolean showGuildTag;
    private GuildTagPlacement guildTagPlacement;

    public NameplateModSettings() {
        this.enabled = true;
        this.showName = true;
        this.nameSource = NameSource.CharacterName;
        this.showTitle = true;
        this.titlePlacement = TitlePlacement.Prefix;
        this.showGuildName = false;
        this.guildNamePlacement = GuildNamePlacement.BelowName;
        this.showGuildTag = true;
        this.guildTagPlacement = GuildTagPlacement.Suffix;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowName() {
        return showName;
    }

    public NameSource getNameSource() {
        return nameSource == null ? NameSource.CharacterName : nameSource;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public TitlePlacement getTitlePlacement() {
        return titlePlacement == null ? TitlePlacement.Prefix : titlePlacement;
    }

    public boolean isShowGuildName() {
        return showGuildName;
    }

    public GuildNamePlacement getGuildNamePlacement() {
        return guildNamePlacement == null ? GuildNamePlacement.BelowName : guildNamePlacement;
    }

    public boolean isShowGuildTag() {
        return showGuildTag;
    }

    public GuildTagPlacement getGuildTagPlacement() {
        return guildTagPlacement == null ? GuildTagPlacement.Suffix : guildTagPlacement;
    }
}