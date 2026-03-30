package org.pixelbays.rpg.character.input;

import org.pixelbays.plugin.ExamplePlugin;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class CharacterLobbyInputFilter implements PlayerPacketFilter {

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        if (!ExamplePlugin.get().getCharacterManager().requiresCharacterUiLock(playerRef)) {
            return false;
        }

        return packet instanceof ClientMovement
                || packet instanceof SyncInteractionChains
                || packet instanceof ClientOpenWindow;
    }
}