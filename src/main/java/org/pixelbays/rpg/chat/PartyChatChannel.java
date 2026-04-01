package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.party.PartyMemberType;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class PartyChatChannel extends BaseConfiguredChatChannel {

    private final PartyManager partyManager;

    public PartyChatChannel(@Nonnull PartyManager partyManager) {
        this(partyManager, null, ChatChannelDefinition.builtIn(
                ChatChannelDefinition.ChannelType.Party,
                "party",
                List.of("p"),
                ChatChannelDefinition.DEFAULT_PARTY_FORMAT_TRANSLATION_KEY));
    }

    public PartyChatChannel(
            @Nonnull PartyManager partyManager,
            @Nullable CharacterManager characterManager,
            @Nonnull ChatChannelDefinition definition) {
        super(characterManager, definition);
        this.partyManager = partyManager;
    }

    @Override
    public boolean canSend(@Nonnull PlayerRef sender) {
        var assetMap = RpgModConfig.getAssetMap();
        RpgModConfig config = assetMap != null ? assetMap.getAsset("default") : null;
        if (config == null || !config.isPartyEnabled()) {
            return false;
        }
        return partyManager.getPartyForMember(sender.getUuid()) != null;
    }

    @Override
    @Nonnull
    public List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender) {
        Party party = partyManager.getPartyForMember(sender.getUuid());
        if (party == null) {
            return List.of();
        }

        List<PlayerRef> targets = new ArrayList<>();
        for (PartyMember member : party.getMemberList()) {
            if (member == null || member.getMemberType() != PartyMemberType.PLAYER) {
                continue;
            }

            PlayerRef ref = Universe.get().getPlayer(member.getEntityId());
            if (ref != null) {
                targets.add(ref);
            }
        }

        return targets;
    }

    @Override
    @Nonnull
    public PlayerChatEvent.Formatter getFormatter() {
        return (sender, msg) -> finalizeMessage(createBaseMessage(sender, msg));
    }
}
