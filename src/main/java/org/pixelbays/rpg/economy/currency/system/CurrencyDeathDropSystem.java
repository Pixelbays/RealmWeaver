package org.pixelbays.rpg.economy.currency.system;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.economy.currency.config.CurrencyItemDropContainer;
import org.pixelbays.rpg.economy.currency.event.GiveCurrencyEvent;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.common.map.IWeightedMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ChoiceItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.DroplistItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.MultipleItemDropContainer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

@SuppressWarnings("null")
public class CurrencyDeathDropSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (component.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            return;
        }

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null || !(deathInfo.getSource() instanceof Damage.EntitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = ((Damage.EntitySource) deathInfo.getSource()).getRef();
        if (!attackerRef.isValid() || store.getComponent(attackerRef, Player.getComponentType()) == null) {
            return;
        }

        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null || commandBuffer.getComponent(ref, Player.getComponentType()) != null) {
            return;
        }

        Role role = npcComponent.getRole();
        if (role == null) {
            return;
        }

        String dropListId = role.getDropListId();
        if (dropListId == null || dropListId.isEmpty()) {
            return;
        }

        ItemDropList itemDropList = ItemDropList.getAssetMap().getAsset(dropListId);
        if (itemDropList == null || itemDropList.getContainer() == null) {
            return;
        }

        List<CurrencyItemDropContainer> currencyContainers = new ArrayList<>();
        collectCurrencyContainers(itemDropList.getContainer(), new HashSet<>(), new HashSet<>(), currencyContainers);
        if (currencyContainers.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (CurrencyItemDropContainer currencyContainer : currencyContainers) {
            if (currencyContainer == null || currencyContainer.getCurrencyId().isBlank()) {
                continue;
            }

            long min = Math.max(0L, currencyContainer.getAmountMin());
            long max = Math.max(min, currencyContainer.getAmountMax());
            if (max <= 0L) {
                continue;
            }

            long amount = min == max ? min : random.nextLong(min, max + 1L);
            if (amount <= 0L) {
                continue;
            }

            RpgLogging.debugDeveloper("Currency death drop grant: currency=%s amount=%s scope=%s attacker=%s victim=%s",
                    currencyContainer.getCurrencyId(), amount, currencyContainer.getScope(), attackerRef, ref);
            GiveCurrencyEvent.dispatch(attackerRef, currencyContainer.getCurrencyId(), amount, currencyContainer.getScope());
        }
    }

    private static void collectCurrencyContainers(
            @Nullable ItemDropContainer container,
            @Nonnull Set<ItemDropContainer> visited,
            @Nonnull Set<String> droplistRefs,
            @Nonnull List<CurrencyItemDropContainer> results) {
        if (container == null || !visited.add(container)) {
            return;
        }

        if (container instanceof CurrencyItemDropContainer currencyContainer) {
            results.add(currencyContainer);
            return;
        }

        if (container instanceof DroplistItemDropContainer) {
            String droplistId = getField(container, "droplistId", String.class);
            if (droplistId != null && droplistRefs.add(droplistId)) {
                ItemDropList droplist = ItemDropList.getAssetMap().getAsset(droplistId);
                if (droplist != null) {
                    collectCurrencyContainers(droplist.getContainer(), visited, droplistRefs, results);
                }
            }
            return;
        }

        if (container instanceof MultipleItemDropContainer) {
            ItemDropContainer[] children = getField(container, "containers", ItemDropContainer[].class);
            if (children != null) {
                for (ItemDropContainer child : children) {
                    collectCurrencyContainers(child, visited, droplistRefs, results);
                }
            }
            return;
        }

        if (container instanceof ChoiceItemDropContainer) {
            IWeightedMap<ItemDropContainer> map = getField(container, "containers", IWeightedMap.class);
            if (map != null) {
                for (ItemDropContainer child : map.internalKeys()) {
                    collectCurrencyContainers(child, visited, droplistRefs, results);
                }
            }
        }
    }

    @Nullable
    private static <T> T getField(@Nonnull Object target, @Nonnull String name, @Nonnull Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
