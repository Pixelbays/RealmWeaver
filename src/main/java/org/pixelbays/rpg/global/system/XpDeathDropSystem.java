package org.pixelbays.rpg.global.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import org.pixelbays.rpg.global.drop.ExpItemDropContainer;

public class XpDeathDropSystem extends DeathSystems.OnDeathSystem {
    @Nonnull
    private final XpGrantSystem xpGrantSystem;

    public XpDeathDropSystem(@Nonnull XpGrantSystem xpGrantSystem) {
        this.xpGrantSystem = xpGrantSystem;
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

        List<ItemDrop> configuredDrops = new ObjectArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        container.populateDrops(configuredDrops, random::nextDouble, dropListId);

        if (configuredDrops.isEmpty()) {
            RpgLogging.debugDeveloper("XP death drop skipped (no configured drops): droplist=%s entity=%s", dropListId, ref);
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        
        // Grant XP for each drop (grantExperience handles thread safety internally)
        for (ItemDrop drop : configuredDrops) {
            if (drop == null) {
                continue;
            }

            var metadata = drop.getMetadata();
            if (!ExpItemDropContainer.isExpMetadata(metadata)) {
                RpgLogging.debugDeveloper("XP death drop skipped (not exp drop): droplist=%s entity=%s", dropListId, ref);
                continue;
            }

            int expAmount = drop.getRandomQuantity(random);
            if (expAmount <= 0) {
                RpgLogging.debugDeveloper("XP death drop skipped (expAmount<=0): amount=%s droplist=%s entity=%s", expAmount, dropListId, ref);
                continue;
            }

            String systemOverride = ExpItemDropContainer.getSystemIdFromMetadata(metadata);
            RpgLogging.debugDeveloper("XP death drop grant: amount=%s systemOverride=%s attacker=%s victim=%s", expAmount, systemOverride, attackerRef, ref);
            xpGrantSystem.grantExperience(attackerRef, expAmount, "MobDeath", store, world, systemOverride);
        }
    }
}
