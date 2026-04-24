package org.pixelbays.rpg.hud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerHudUpdateSystem extends DelayedEntitySystem<EntityStore> {

    private final PlayerHudService hudService;

    public PlayerHudUpdateSystem(float intervalSec, @Nonnull PlayerHudService hudService) {
        super(intervalSec);
        this.hudService = hudService;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("Default");
        if (config == null || !config.isLevelingModuleEnabled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && player.isWaitingForClientReady()) {
            return;
        }

        hudService.ensureAndUpdate(ref, store);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        var playerType = Player.getComponentType();
        var playerRefType = PlayerRef.getComponentType();

        if (playerType == null && playerRefType == null) {
            return Query.any();
        }
        if (playerType == null) {
            return playerRefType;
        }
        if (playerRefType == null) {
            return playerType;
        }
        return Query.and(playerType, playerRefType);
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }
}