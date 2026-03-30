package org.pixelbays.rpg.npc.system;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.npc.component.NpcThreatComponent;
import org.pixelbays.rpg.npc.config.settings.NpcModSettings;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

@SuppressWarnings("null")
public class NpcThreatDamageSystem extends DamageEventSystem {

    @Nonnull
    private final ComponentType<EntityStore, NpcThreatComponent> threatComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final ClassManagementSystem classManagementSystem;

    public NpcThreatDamageSystem(@Nonnull ComponentType<EntityStore, NpcThreatComponent> threatComponentType) {
        this.threatComponentType = threatComponentType;
        this.query = NPCEntity.getComponentType();
        this.classManagementSystem = ExamplePlugin.get().getClassManagementSystem();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        NpcModSettings settings = resolveNpcSettings();
        if (!settings.isEnabled() || !settings.isThreatEnabled()) {
            return;
        }

        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return;
        }

        Player playerComponent = commandBuffer.getComponent(sourceRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        float damageAmount = damage.getAmount();
        if (damageAmount <= 0.0f) {
            return;
        }

        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(index);
        NpcThreatComponent threatComponent = store.getComponent(npcRef, threatComponentType);
        if (threatComponent == null) {
            threatComponent = store.addComponent(npcRef, threatComponentType);
        }

        float threatMultiplier = resolveThreatMultiplier(sourceRef, store, settings);
        float threatAmount = damageAmount * threatMultiplier;
        if (threatAmount <= 0.0f) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        threatComponent.addThreat(sourceRef, threatAmount, nowMillis);

        TargetMemory targetMemory = store.getComponent(npcRef, TargetMemory.getComponentType());
        rememberHostile(targetMemory, sourceRef, settings.getThreatLookbackSeconds());

        Ref<EntityStore> currentTarget = threatComponent.pruneAndResolveCurrentTarget(nowMillis,
                settings.getThreatLookbackSeconds());
        if (targetMemory != null) {
            targetMemory.setClosestHostile(currentTarget);
        }
    }

    private float resolveThreatMultiplier(@Nonnull Ref<EntityStore> sourceRef, @Nonnull Store<EntityStore> store,
            @Nonnull NpcModSettings settings) {
        float resolvedMultiplier = settings.getDefaultThreatMultiplier();

        ClassComponent classComponent = store.getComponent(sourceRef, ClassComponent.getComponentType());
        if (classComponent == null) {
            return resolvedMultiplier;
        }

        String activeClassId = classComponent.getPrimaryClassId();
        if (activeClassId == null || activeClassId.isBlank()) {
            return resolvedMultiplier;
        }

        ClassDefinition classDefinition = classManagementSystem.getClassDefinition(activeClassId);
        if (classDefinition == null) {
            return resolvedMultiplier;
        }

        List<String> roles = classDefinition.getEffectiveRoles(null);
        for (String roleId : roles) {
            resolvedMultiplier = Math.max(resolvedMultiplier, settings.getThreatMultiplierForRole(roleId));
        }

        return resolvedMultiplier;
    }

    private void rememberHostile(@Nullable TargetMemory targetMemory, @Nonnull Ref<EntityStore> hostileRef,
            float threatLookbackSeconds) {
        if (targetMemory == null || hostileRef == null || !hostileRef.isValid()) {
            return;
        }

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