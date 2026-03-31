package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class PlayerHudContext {

    @Nonnull
    private final Ref<EntityStore> ref;
    @Nonnull
    private final Store<EntityStore> store;
    @Nonnull
    private final Player player;
    @Nonnull
    private final PlayerRef playerRef;
    @Nullable
    private final String activeClassId;

    private PlayerHudContext(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nullable String activeClassId) {
        this.ref = ref;
        this.store = store;
        this.player = player;
        this.playerRef = playerRef;
        this.activeClassId = activeClassId;
    }

    @Nullable
    static PlayerHudContext create(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (!ref.isValid()) {
            return null;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return null;
        }

        return new PlayerHudContext(ref, store, player, playerRef, PlayerHudServiceSupport.resolveActiveClassId(ref, store));
    }

    @Nonnull
    Ref<EntityStore> getRef() {
        return ref;
    }

    @Nonnull
    Store<EntityStore> getStore() {
        return store;
    }

    @Nonnull
    Player getPlayer() {
        return player;
    }

    @Nonnull
    PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Nullable
    String getActiveClassId() {
        return activeClassId;
    }
}