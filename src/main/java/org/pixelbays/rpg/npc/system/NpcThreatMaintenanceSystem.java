package org.pixelbays.rpg.npc.system;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.npc.component.NpcThreatComponent;
import org.pixelbays.rpg.npc.config.settings.NpcModSettings;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleSystems;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

@SuppressWarnings("null")
public class NpcThreatMaintenanceSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, NpcThreatComponent> threatComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public NpcThreatMaintenanceSystem(@Nonnull ComponentType<EntityStore, NpcThreatComponent> threatComponentType) {
        this.threatComponentType = threatComponentType;
        this.query = Query.and(NPCEntity.getComponentType(), threatComponentType);
        this.dependencies = Set.of(new SystemDependency<>(Order.BEFORE, RoleSystems.BehaviourTickSystem.class));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NpcModSettings settings = resolveNpcSettings();
        if (!settings.isEnabled() || !settings.isThreatEnabled()) {
            return;
        }

        NpcThreatComponent threatComponent = archetypeChunk.getComponent(index, threatComponentType);
        if (threatComponent == null) {
            return;
        }

        Ref<EntityStore> selectedTarget = threatComponent.pruneAndResolveCurrentTarget(System.currentTimeMillis(),
                settings.getThreatLookbackSeconds());
        TargetMemory targetMemory = store.getComponent(archetypeChunk.getReferenceTo(index), TargetMemory.getComponentType());
        if (targetMemory == null) {
            return;
        }

        if (selectedTarget == null || !selectedTarget.isValid()) {
            targetMemory.setClosestHostile(null);
            return;
        }

        rememberHostile(targetMemory, selectedTarget, settings.getThreatLookbackSeconds());
        targetMemory.setClosestHostile(selectedTarget);
    }

    private void rememberHostile(@Nonnull TargetMemory targetMemory, @Nonnull Ref<EntityStore> hostileRef,
            float threatLookbackSeconds) {
        Int2FloatOpenHashMap hostileMap = targetMemory.getKnownHostiles();
        float duration = Math.max(targetMemory.getRememberFor(), threatLookbackSeconds);
        if (hostileMap.put(hostileRef.getIndex(), duration) <= 0.0f) {
            targetMemory.getKnownHostilesList().add(hostileRef);
        }
    }

    @Nonnull
    private NpcModSettings resolveNpcSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new NpcModSettings() : config.getNpcSettings();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }
}