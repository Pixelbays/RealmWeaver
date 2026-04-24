package org.pixelbays.rpg.global.input;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.StringJoiner;

public class AbilityInputDebugWatcher implements PlayerPacketWatcher {

    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) {
            return;
        }

        if (!RpgLogging.isDeveloperDebugEnabled()) {
            return;
        }

        CharacterManager characterManager = Realmweavers.get().getCharacterManager();
        String activeCharacterId = characterManager.getActiveCharacterId(playerRef.getUuid());
        String activeClassId = characterManager.resolveActivePrimaryClassId(playerRef);
        boolean characterUiLock = characterManager.requiresCharacterUiLock(playerRef);

        RpgLogging.debugDeveloper(
                "[AbilityInputWatcher] interaction packet player=%s updates=%s activeCharacter=%s class=%s uiLock=%s",
                playerRef.getUsername(),
                describeUpdates(syncPacket),
                activeCharacterId.isBlank() ? "<none>" : activeCharacterId,
                activeClassId.isBlank() ? "<none>" : activeClassId,
                characterUiLock);

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (!chain.initial) {
                continue;
            }

            int slotNumber = resolveAbilitySlotNumber(chain.interactionType);
            if (slotNumber <= 0) {
                continue;
            }

            RpgLogging.debugDeveloper(
                    "[AbilityInputWatcher] spell-slot press player=%s slot=%d interaction=%s activeCharacter=%s class=%s uiLock=%s",
                    playerRef.getUsername(),
                    slotNumber,
                    chain.interactionType,
                    activeCharacterId.isBlank() ? "<none>" : activeCharacterId,
                    activeClassId.isBlank() ? "<none>" : activeClassId,
                    characterUiLock);
        }
    }

    private String describeUpdates(SyncInteractionChains syncPacket) {
        if (syncPacket.updates == null || syncPacket.updates.length == 0) {
            return "[]";
        }

        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain == null) {
                continue;
            }

            joiner.add(String.format(
                    "%s(initial=%s,state=%s,chainId=%d)",
                    chain.interactionType,
                    chain.initial,
                    chain.state,
                    chain.chainId));
        }
        return joiner.toString();
    }

    private int resolveAbilitySlotNumber(InteractionType interactionType) {
        return switch (interactionType) {
            case Ability1 -> 1;
            case Ability2 -> 2;
            case Ability3 -> 3;
            default -> 0;
        };
    }
}