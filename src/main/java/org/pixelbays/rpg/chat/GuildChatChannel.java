package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildMember;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

@SuppressWarnings("null")
public final class GuildChatChannel extends BaseConfiguredChatChannel {

    private final GuildManager guildManager;

    public GuildChatChannel(@Nonnull GuildManager guildManager) {
        this(guildManager, null, ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Guild,
                "guild",
                List.of("g"),
                ChatChannelDefinition.DEFAULT_GUILD_FORMAT_TRANSLATION_KEY));
    }

    public GuildChatChannel(
            @Nonnull GuildManager guildManager,
            @Nullable CharacterManager characterManager,
            @Nonnull ChatChannelDefinition definition) {
        super(characterManager, definition);
        this.guildManager = guildManager;
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
            return finalizeMessage(createBaseMessage(sender, msg)
                    .param("tag", tag));
        };
    }
}
