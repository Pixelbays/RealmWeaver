package org.pixelbays.rpg.character.input;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class CharacterLobbyInputFilter implements PlayerPacketFilter {

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        if (!Realmweavers.get().getCharacterManager().requiresCharacterUiLock(playerRef)) {
            return false;
        }

        if (packet instanceof SyncInteractionChains syncPacket) {
            logBlockedAbilitySlotPresses(playerRef, syncPacket);
        }

        return packet instanceof ClientMovement
                || packet instanceof SyncInteractionChains
                || packet instanceof ClientOpenWindow;
    }

        private void logBlockedAbilitySlotPresses(PlayerRef playerRef,
            SyncInteractionChains syncPacket) {
        for (SyncInteractionChain chain : syncPacket.updates) {
            if (!chain.initial) {
                continue;
            }

            int slotNumber = resolveAbilitySlotNumber(chain.interactionType);
            if (slotNumber <= 0) {
                continue;
            }

            RpgLogging.debugDeveloper(
                    "[CharacterLobbyInputFilter] blocked spell-slot press player=%s slot=%d interaction=%s reason=character-ui-lock",
                    playerRef.getUsername(),
                    slotNumber,
                    chain.interactionType);
        }
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