package org.pixelbays.rpg.guild.command;

import java.util.UUID;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public final class GuildCommandUtil {

    private GuildCommandUtil() {
    }

    public static PlayerRef findPlayerByName(String name) {
        return Universe.get().getPlayerByUsername(name, NameMatching.DEFAULT);
    }

    public static String resolveDisplayName(UUID playerId) {
        PlayerRef ref = Universe.get().getPlayer(playerId);
        return ref != null ? ref.getUsername() : playerId.toString();
    }
}
