package org.pixelbays.rpg.hud.command;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.hud.PlayerHudService;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class UiDebugCommand extends AbstractPlayerCommand {

    public UiDebugCommand() {
        super("uidebug", "Export the current player HUD to a debug .ui file");
        requirePermission(HytalePermissions.fromCommand("admin"));
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerHudService hudService = Realmweavers.get().getPlayerHudService();
        if (hudService == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.ui.debug.unavailable"));
            return;
        }

        try {
            Path outputPath = hudService.exportDebugUi(ref, store);
            player.sendMessage(Message.translation("pixelbays.rpg.ui.debug.exported")
                    .param("path", outputPath.toAbsolutePath().toString()));
        } catch (IOException ex) {
            RpgLogging.debugDeveloper("[UiDebug] Failed to export player HUD UI for %s: %s",
                    playerRef.getUuid(),
                    ex.getMessage());
            player.sendMessage(Message.translation("pixelbays.rpg.ui.debug.failed")
                    .param("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }
}