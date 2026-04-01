package org.pixelbays.rpg.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

final class ChatTargetingSupport {

    private ChatTargetingSupport() {
    }

    @Nonnull
    static List<PlayerRef> resolvePlayersInRange(@Nonnull PlayerRef sender, int rangeBlocks) {
        if (rangeBlocks <= 0) {
            return List.of(sender);
        }

        UUID senderWorldUuid = sender.getWorldUuid();
        if (senderWorldUuid == null) {
            return List.of(sender);
        }

        Transform senderTransform = sender.getTransform();
        Vector3d senderPosition = senderTransform.getPosition();
        double maxDistanceSquared = (double) rangeBlocks * (double) rangeBlocks;

        List<PlayerRef> targets = new ArrayList<>();
        for (PlayerRef target : Universe.get().getPlayers()) {
            if (!isWithinRange(senderWorldUuid, senderPosition, target, maxDistanceSquared)) {
                continue;
            }
            targets.add(target);
        }

        return targets.isEmpty() ? List.of(sender) : targets;
    }

    private static boolean isWithinRange(
            @Nonnull UUID senderWorldUuid,
            @Nonnull Vector3d senderPosition,
            @Nullable PlayerRef target,
            double maxDistanceSquared) {
        if (target == null || target.getWorldUuid() == null || !senderWorldUuid.equals(target.getWorldUuid())) {
            return false;
        }

        Vector3d targetPosition = target.getTransform().getPosition();
        double dx = senderPosition.getX() - targetPosition.getX();
        double dy = senderPosition.getY() - targetPosition.getY();
        double dz = senderPosition.getZ() - targetPosition.getZ();
        return dx * dx + dy * dy + dz * dz <= maxDistanceSquared;
    }
}