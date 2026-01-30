package org.pixelbays.rpg.global.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.common.map.IWeightedMap;
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
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.config.ExpItemDropContainer;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;

@SuppressWarnings("null")
public class XpDeathDropSystem extends DeathSystems.OnDeathSystem {
    public XpDeathDropSystem() {
    }

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
        RpgLogging.debugDeveloper("XP death drop triggered: entity=%s", ref);
        if (component.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            RpgLogging.debugDeveloper("XP death drop skipped (items loss mode): mode=%s entity=%s", component.getItemsLossMode(), ref);
            return;
        }

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null || !(deathInfo.getSource() instanceof Damage.EntitySource)) {
            RpgLogging.debugDeveloper("XP death drop skipped (no entity source): entity=%s", ref);
            return;
        }

        Ref<EntityStore> attackerRef = ((Damage.EntitySource) deathInfo.getSource()).getRef();
        if (!attackerRef.isValid() || store.getComponent(attackerRef, Player.getComponentType()) == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (invalid attacker or not player): attacker=%s victim=%s", attackerRef, ref);
            return;
        }

        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (no NPC component): entity=%s", ref);
            return;
        }

        if (commandBuffer.getComponent(ref, Player.getComponentType()) != null) {
            RpgLogging.debugDeveloper("XP death drop skipped (victim is player): entity=%s", ref);
            return;
        }

        Role role = npcComponent.getRole();
        if (role == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (role null): entity=%s", ref);
            return;
        }

        String dropListId = role.getDropListId();
        if (dropListId == null || dropListId.isEmpty()) {
            return;
        }

        ItemDropList itemDropList = ItemDropList.getAssetMap().getAsset(dropListId);
        if (itemDropList == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (droplist missing): droplist=%s entity=%s", dropListId, ref);
            return;
        }

        ItemDropContainer container = itemDropList.getContainer();
        if (container == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (no container): droplist=%s entity=%s", dropListId, ref);
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        ExpItemDropContainer expContainer = findExpContainer(container, new HashSet<>(), new HashSet<>());
        if (expContainer == null) {
            RpgLogging.debugDeveloper("XP death drop skipped (container not exp): droplist=%s entity=%s", dropListId, ref);
            return;
        }
        int min = Math.max(0, expContainer.getExpMin());
        int max = Math.max(min, expContainer.getExpMax());
        if (max <= 0) {
            RpgLogging.debugDeveloper("XP death drop skipped (max<=0): min=%s max=%s droplist=%s entity=%s", min, max, dropListId, ref);
            return;
        }

        int expAmount = (min == max) ? min : random.nextInt(min, max + 1);
        if (expAmount <= 0) {
            RpgLogging.debugDeveloper("XP death drop skipped (expAmount<=0): amount=%s droplist=%s entity=%s", expAmount, dropListId, ref);
            return;
        }

        String systemOverride = expContainer.getSystemId();
        RpgLogging.debugDeveloper("XP death drop grant: amount=%s systemOverride=%s attacker=%s victim=%s", expAmount, systemOverride, attackerRef, ref);
        GiveXPEvent.dispatch(attackerRef, expAmount, systemOverride);
    }

    @Nullable
    private static ExpItemDropContainer findExpContainer(
            @Nullable ItemDropContainer container,
            @Nonnull Set<ItemDropContainer> visited,
            @Nonnull Set<String> droplistRefs) {
        if (container == null) {
            return null;
        }

        if (container instanceof ExpItemDropContainer) {
            return (ExpItemDropContainer) container;
        }

        if (!visited.add(container)) {
            return null;
        }

        if (container instanceof DroplistItemDropContainer) {
            String droplistId = getField(container, "droplistId", String.class);
            if (droplistId != null && droplistRefs.add(droplistId)) {
                ItemDropList droplist = ItemDropList.getAssetMap().getAsset(droplistId);
                if (droplist != null) {
                    return findExpContainer(droplist.getContainer(), visited, droplistRefs);
                }
            }
            return null;
        }

        if (container instanceof MultipleItemDropContainer) {
            ItemDropContainer[] children = getField(container, "containers", ItemDropContainer[].class);
            if (children != null) {
                for (ItemDropContainer child : children) {
                    ExpItemDropContainer found = findExpContainer(child, visited, droplistRefs);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        if (container instanceof ChoiceItemDropContainer) {
            IWeightedMap<ItemDropContainer> map = getField(container, "containers", IWeightedMap.class);
            if (map != null) {
                for (ItemDropContainer child : map.internalKeys()) {
                    ExpItemDropContainer found = findExpContainer(child, visited, droplistRefs);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }

        return null;
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
