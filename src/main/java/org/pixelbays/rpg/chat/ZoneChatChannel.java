package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.chat.config.settings.ChatChannelDefinition;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;

@SuppressWarnings("null")
public final class ZoneChatChannel extends BaseConfiguredChatChannel {

    public ZoneChatChannel(@Nonnull ChatChannelDefinition definition) {
        this(null, definition);
    }

    public ZoneChatChannel(@Nullable CharacterManager characterManager, @Nonnull ChatChannelDefinition definition) {
        super(characterManager, definition);
    }

    @Override
    public boolean canSend(@Nonnull PlayerRef sender) {
        return resolveZoneDescriptor(sender) != null;
    }

    @Override
    @Nonnull
    public List<PlayerRef> resolveTargets(@Nonnull PlayerRef sender) {
        ZoneDescriptor senderZone = resolveZoneDescriptor(sender);
        if (senderZone == null) {
            return List.of();
        }

        List<PlayerRef> targets = new ArrayList<>();
        for (PlayerRef target : Universe.get().getPlayers()) {
            ZoneDescriptor targetZone = resolveZoneDescriptor(target);
            if (senderZone.matches(targetZone)) {
                targets.add(target);
            }
        }
        return targets;
    }

    @Override
    @Nonnull
    public PlayerChatEvent.Formatter getFormatter() {
        return (sender, msg) -> {
            ZoneDescriptor zone = resolveZoneDescriptor(sender);
            Message message = createBaseMessage(sender, msg);
            if (zone != null && !zone.regionName().isBlank()) {
                message.param("zone", zone.regionName());
            }
            return finalizeMessage(message);
        };
    }

    @Nullable
    private ZoneDescriptor resolveZoneDescriptor(@Nonnull PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            return null;
        }

        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            return null;
        }

        if (!(world.getChunkStore().getGenerator() instanceof ChunkGenerator generator)) {
            return null;
        }

        Transform transform = playerRef.getTransform();
        ZoneBiomeResult result = generator.getZoneBiomeResultAt(
                (int) world.getWorldConfig().getSeed(),
                (int) transform.getPosition().getX(),
                (int) transform.getPosition().getZ());
        if (result == null || result.getZoneResult() == null || result.getZoneResult().getZone() == null) {
            return null;
        }

        return new ZoneDescriptor(worldUuid, result.getZoneResult().getZone().id(), result.getZoneResult().getZone().name());
    }

    private record ZoneDescriptor(@Nonnull UUID worldUuid, int zoneId, @Nonnull String regionName) {
        private boolean matches(@Nullable ZoneDescriptor other) {
            return other != null && zoneId == other.zoneId && worldUuid.equals(other.worldUuid);
        }
    }
}