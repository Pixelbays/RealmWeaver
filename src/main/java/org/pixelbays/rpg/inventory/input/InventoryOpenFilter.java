package org.pixelbays.rpg.inventory.input;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.RpgModConfig.InventoryHandlingMode;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.inventory.ui.RpgInventoryPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class InventoryOpenFilter implements PlayerPacketFilter {

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof ClientOpenWindow openWindow)) {
            return false;
        }

        RpgLogging.debugDeveloper("[InventoryUI] ClientOpenWindow type=%s", openWindow.type);
        if (!isInventoryWindow(openWindow.type)) {
            RpgLogging.debugDeveloper("[InventoryUI] Not an inventory window, letting vanilla open.");
            return false;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            RpgLogging.debugDeveloper("[InventoryUI] No config resolved, letting vanilla open.");
            return false;
        }

        InventoryHandlingMode mode = config.getInventoryHandling();
        if (mode == null || mode == InventoryHandlingMode.Vanilla) {
            RpgLogging.debugDeveloper("[InventoryUI] InventoryHandling=%s, letting vanilla open.", mode);
            return false;
        }

        RpgLogging.debugDeveloper("[InventoryUI] InventoryHandling=%s, intercepting open.", mode);

        if (playerRef == null) {
            RpgLogging.debugDeveloper("[InventoryUI] PlayerRef missing, suppressing open.");
            return true;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            RpgLogging.debugDeveloper("[InventoryUI] PlayerRef has no valid entity, suppressing open.");
            return true;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) {
            RpgLogging.debugDeveloper("[InventoryUI] World not available, suppressing open.");
            return true;
        }

        world.execute(() -> openInventoryOnWorldThread(ref, playerRef));

        return false;
    }

    private void openInventoryOnWorldThread(@Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef) {
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            RpgLogging.debugDeveloper("[InventoryUI] Player component missing on world thread.");
            return;
        }

        if (player.getGameMode() != com.hypixel.hytale.protocol.GameMode.Adventure) {
            RpgLogging.debugDeveloper("[InventoryUI] GameMode=%s, letting vanilla open.", player.getGameMode());
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new RpgInventoryPage(playerRef));
        RpgLogging.debugDeveloper("[InventoryUI] Opened RPG inventory for %s", playerRef.getUsername());
    }

    private boolean isInventoryWindow(WindowType type) {
        return type == WindowType.Container || type == WindowType.PocketCrafting;
    }

    private RpgModConfig resolveConfig() {
        return resolveConfigForLog();
    }

    public static RpgModConfig resolveConfigForLog() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("default");
        if (config != null) {
            return config;
        }

        config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }
}
