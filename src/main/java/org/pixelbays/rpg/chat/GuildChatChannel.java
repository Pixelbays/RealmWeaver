package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildMember;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class GuildChatChannel implements ChatChannel {

    private final GuildManager guildManager;
    private final String id;
    private final List<String> aliases;
    private final String formatTranslationKey;

    public GuildChatChannel(@Nonnull GuildManager guildManager) {
        this(guildManager, ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Guild,
                "guild",
                List.of("g"),
                "rpg.chat.guild.message"));
    }

    public GuildChatChannel(@Nonnull GuildManager guildManager, @Nonnull ChatChannelDefinition definition) {
        this.guildManager = guildManager;
        this.id = definition.getId();
        this.aliases = List.copyOf(definition.getAliases());
        this.formatTranslationKey = definition.getFormatTranslationKey();
    }

    @Override
    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    @Nonnull
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean canSend(@Nonnull PlayerRef sender) {
        var assetMap = RpgModConfig.getAssetMap();
        RpgModConfig config = assetMap != null ? assetMap.getAsset("default") : null;
        if (config == null || !config.isGuildEnabled()) {
            return false;
        }
        return guildManager.getGuildForMember(sender.getUuid()) != null;
    }

    @Override
    @Nonnull
    public List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender) {
        Guild guild = guildManager.getGuildForMember(sender.getUuid());
        if (guild == null) {
            return List.of();
        }

        List<PlayerRef> targets = new ArrayList<>();
        for (GuildMember member : guild.getMemberList()) {
            if (member == null) {
                continue;
            }

            PlayerRef ref = Universe.get().getPlayer(member.getMemberId());
            if (ref != null) {
                targets.add(ref);
            }
        }

        return targets;
    }

    @Override
    @Nonnull
    public PlayerChatEvent.Formatter getFormatter() {
        return (sender, msg) -> {
            Guild guild = guildManager.getGuildForMember(sender.getUuid());
            String tag = guild != null ? guild.getTag() : "";
            return Message.translation(formatTranslationKey)
                    .param("tag", tag)
                    .param("username", sender.getUsername())
                    .param("message", msg);
        };
    }
}
