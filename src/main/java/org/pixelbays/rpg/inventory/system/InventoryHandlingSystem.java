package org.pixelbays.rpg.inventory.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.event.ClassStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.RaceStatBonusesRecalculatedEvent;
import org.pixelbays.rpg.global.event.StatGrowthAppliedEvent;
import org.pixelbays.rpg.global.event.StatIncreasesAppliedEvent;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.inventory.config.settings.InventoryModSettings.InventoryHandlingMode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class InventoryHandlingSystem implements Consumer<PlayerReadyEvent> {

    public void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerReadyEvent.class, this);
        eventRegistry.register(StatIncreasesAppliedEvent.class, event -> onStatChanged(event.entityRef()));
        eventRegistry.register(StatGrowthAppliedEvent.class, event -> onStatChanged(event.entityRef()));
        eventRegistry.register(ClassStatBonusesRecalculatedEvent.class, event -> onStatChanged(event.entityRef()));
        eventRegistry.register(RaceStatBonusesRecalculatedEvent.class, event -> onStatChanged(event.entityRef()));
    }

    @Override
    public void accept(@Nonnull PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        applyInventorySize(player, store, ref);
    }

    private void onStatChanged(@Nonnull Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        applyInventorySize(player, store, ref);
    }

    private void applyInventorySize(@Nonnull Player player,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref) {
        RpgModConfig config = resolveConfig();
        if (config == null) {
            return;
        }

        InventoryHandlingMode mode = config.getInventoryHandling();
        if (!config.isInventoryModuleEnabled() || mode == null || mode == InventoryHandlingMode.Vanilla) {
            return;
        }

        InventoryProfile profile = buildProfile(config, ref, store);
        int targetSlots = profile.totalSlots();
        if (targetSlots <= 0) {
            return;
        }

        Inventory inventory = player.getInventory();
        short currentSlots = inventory.getStorage().getCapacity();
        if (currentSlots == targetSlots) {
            return;
        }

        String playerName = resolvePlayerName(store, ref);
        if (currentSlots > targetSlots) {
            RpgLogging.debugDeveloper("[Inventory] Skipping shrink: current=%s target=%s player=%s",
                currentSlots, targetSlots, playerName);
            return;
        }

        List<ItemStack> remainder = new ArrayList<>();
        ItemContainer newStorage = ItemContainer.ensureContainerCapacity(
                inventory.getStorage(),
                (short) targetSlots,
                SimpleItemContainer::new,
                remainder);

        if (newStorage == inventory.getStorage()) {
            return;
        }

        store.putComponent(ref, InventoryComponent.Storage.getComponentType(), new InventoryComponent.Storage(newStorage));

        RpgLogging.debugDeveloper(
                "[Inventory] Applied size=%s (base=%s bonus=%s mode=%s) extraSlots=rings:%s trinkets:%s neck:%s player=%s",
                profile.totalSlots(),
                profile.baseSlots(),
                profile.bonusSlots(),
                profile.mode(),
                profile.extraRingsEnabled(),
                profile.extraTrinketsEnabled(),
                profile.extraNeckEnabled(),
            playerName);
    }

    private InventoryProfile buildProfile(@Nonnull RpgModConfig config,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        int baseSlots = Math.max(0, config.getDefaultInventorySize());
        int bonusSlots = 0;
        InventoryHandlingMode mode = config.getInventoryHandling();

        if (mode == InventoryHandlingMode.Strength) {
            int strengthValue = resolveStrengthValue(ref, store);
            bonusSlots = resolveStrengthBonus(config.getStrengthInventorySlots(), strengthValue);
        } else if (mode == InventoryHandlingMode.Item) {
            RpgLogging.debugDeveloper("[Inventory] Item-based handling not implemented; using base size only.");
        }

        int totalSlots = baseSlots + Math.max(0, bonusSlots);
        return new InventoryProfile(
                mode == null ? InventoryHandlingMode.Vanilla : mode,
                baseSlots,
                bonusSlots,
                totalSlots,
                config.isExtraSlotsRingsEnabled(),
                config.isExtraSlotsTrinketsEnabled(),
                config.isExtraSlotsNeckEnabled());
    }

    private int resolveStrengthValue(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return 0;
        }

        int statIndex = resolveStatIndex("Strength");
        if (statIndex == Integer.MIN_VALUE) {
            return 0;
        }

        EntityStatValue value = statMap.get(statIndex);
        if (value == null) {
            return 0;
        }

        return Math.max(0, (int) Math.floor(value.get()));
    }

    private int resolveStatIndex(@Nonnull String... candidates) {
        for (String candidate : candidates) {
            int index = EntityStatType.getAssetMap().getIndex(candidate);
            if (index != Integer.MIN_VALUE) {
                return index;
            }
        }

        return Integer.MIN_VALUE;
    }

    private int resolveStrengthBonus(@Nullable Object2IntMap<String> table, int strengthValue) {
        if (table == null || table.isEmpty()) {
            return 0;
        }

        int bestThreshold = Integer.MIN_VALUE;
        int bestBonus = 0;

        for (Object2IntMap.Entry<String> entry : table.object2IntEntrySet()) {
            int threshold = parseStrengthKey(entry.getKey());
            if (threshold <= strengthValue && threshold >= bestThreshold) {
                bestThreshold = threshold;
                bestBonus = entry.getIntValue();
            }
        }

        return bestBonus;
    }

    private int parseStrengthKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return Integer.MIN_VALUE;
        }

        try {
            return Integer.parseInt(key.trim());
        } catch (NumberFormatException ex) {
            return Integer.MIN_VALUE;
        }
    }

    private String resolvePlayerName(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null && playerRef.getUsername() != null) {
            return playerRef.getUsername();
        }

        return String.valueOf(ref.getIndex());
    }

    @Nullable
    public static RpgModConfig resolveConfig() {
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

    private static final class InventoryProfile {
        private final InventoryHandlingMode mode;
        private final int baseSlots;
        private final int bonusSlots;
        private final int totalSlots;
        private final boolean extraRingsEnabled;
        private final boolean extraTrinketsEnabled;
        private final boolean extraNeckEnabled;

        private InventoryProfile(InventoryHandlingMode mode, int baseSlots, int bonusSlots, int totalSlots,
                boolean extraRingsEnabled, boolean extraTrinketsEnabled, boolean extraNeckEnabled) {
            this.mode = mode;
            this.baseSlots = baseSlots;
            this.bonusSlots = bonusSlots;
            this.totalSlots = totalSlots;
            this.extraRingsEnabled = extraRingsEnabled;
            this.extraTrinketsEnabled = extraTrinketsEnabled;
            this.extraNeckEnabled = extraNeckEnabled;
        }

        private InventoryHandlingMode mode() {
            return mode;
        }

        private int baseSlots() {
            return baseSlots;
        }

        private int bonusSlots() {
            return bonusSlots;
        }

        private int totalSlots() {
            return totalSlots;
        }

        private boolean extraRingsEnabled() {
            return extraRingsEnabled;
        }

        private boolean extraTrinketsEnabled() {
            return extraTrinketsEnabled;
        }

        private boolean extraNeckEnabled() {
            return extraNeckEnabled;
        }
    }
}
