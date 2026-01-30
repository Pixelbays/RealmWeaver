package org.pixelbays.rpg.leveling.system;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class RestedXpSystem {

    private static final String RESTED_XP_STAT = "Rested_XP";
    private static final String RESTED_XP_RATE_STAT = "Rested_XP_Rate";

    public RestedXpSystem(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.register(PlayerConnectEvent.class, onPlayerConnect());
        eventRegistry.register(PlayerDisconnectEvent.class, onPlayerDisconnect());
    }

    private Consumer<PlayerConnectEvent> onPlayerConnect() {
        return event -> {
            var playerRef = event.getPlayerRef();
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> store = entityRef.getStore();
            RpgModConfig config = resolveConfig();
            if (config == null || !config.isRestedXpEnabled()) {
                return;
            }

            if (!canGainRestedXp(entityRef, store, config)) {
                return;
            }

            LevelProgressionComponent levelComp = store.getComponent(entityRef, LevelProgressionComponent.getComponentType());
            if (levelComp == null) {
                levelComp = store.addComponent(entityRef, LevelProgressionComponent.getComponentType());
            }

            long lastLogout = levelComp.getLastLogoutEpochMs();
            if (lastLogout <= 0L) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now <= lastLogout) {
                return;
            }

            float hoursOffline = (now - lastLogout) / 3600000f;
            if (hoursOffline <= 0f) {
                return;
            }

            EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }

            int restedIndex = EntityStatType.getAssetMap().getIndex(RESTED_XP_STAT);
            if (restedIndex == Integer.MIN_VALUE) {
                return;
            }

            EntityStatValue restedValue = statMap.get(restedIndex);
            if (restedValue == null) {
                return;
            }

            float ratePerHour = getRatePerHour(statMap);
            if (ratePerHour <= 0f) {
                return;
            }

            float gain = ratePerHour * hoursOffline;
            if (gain <= 0f) {
                return;
            }

            float current = restedValue.get();
            float max = restedValue.getMax();
            float add = Math.max(0f, Math.min(gain, max - current));
            if (add <= 0f) {
                return;
            }

            statMap.addStatValue(restedIndex, add);
            RpgLogging.debugDeveloper("Rested XP gained: +%s (hours=%s rate=%s current=%s max=%s)", add, hoursOffline, ratePerHour, current, max);
        };
    }

    private Consumer<PlayerDisconnectEvent> onPlayerDisconnect() {
        return event -> {
            var playerRef = event.getPlayerRef();
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return;
            }

            Store<EntityStore> store = entityRef.getStore();
            LevelProgressionComponent levelComp = store.getComponent(entityRef, LevelProgressionComponent.getComponentType());
            if (levelComp == null) {
                levelComp = store.addComponent(entityRef, LevelProgressionComponent.getComponentType());
            }

            levelComp.setLastLogoutEpochMs(System.currentTimeMillis());
        };
    }

    private boolean canGainRestedXp(@Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull RpgModConfig config) {
        List<String> tags = config.getRestedXpGainTags();
        if (tags == null || tags.isEmpty()) {
            return false;
        }

        ClassComponent classComp = store.getComponent(playerRef, ClassComponent.getComponentType());
        if (classComp == null || classComp.getLearnedClassIds().isEmpty()) {
            return false;
        }

        for (String classId : classComp.getLearnedClassIds()) {
            ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
            if (classDef == null || !classDef.isEnabled()) {
                continue;
            }

            if (hasAnyTag(classDef, tags)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyTag(@Nonnull ClassDefinition classDef, List<String> tags) {
        for (String tag : tags) {
            if (tag != null && !tag.isEmpty() && classDef.hasTag("type", tag.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private float getRatePerHour(@Nonnull EntityStatMap statMap) {
        int rateIndex = EntityStatType.getAssetMap().getIndex(RESTED_XP_RATE_STAT);
        if (rateIndex == Integer.MIN_VALUE) {
            return 0f;
        }

        EntityStatValue rateValue = statMap.get(rateIndex);
        if (rateValue == null) {
            return 0f;
        }

        return rateValue.get();
    }

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
