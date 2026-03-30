package org.pixelbays.rpg.npc.system;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.npc.component.NpcRpgDebugComponent;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.DebugSupport;

public class NpcRpgDebugOverlaySystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    private final ComponentType<EntityStore, NpcRpgDebugComponent> debugComponentType;
    private final Query<EntityStore> query;
    private final LevelProgressionSystem levelSystem;

    public NpcRpgDebugOverlaySystem(
            @Nonnull ComponentType<EntityStore, NPCEntity> npcComponentType,
            @Nonnull ComponentType<EntityStore, NpcRpgDebugComponent> debugComponentType) {
        this.npcComponentType = npcComponentType;
        this.debugComponentType = debugComponentType;
        this.query = Query.and(npcComponentType, debugComponentType);
        this.levelSystem = Realmweavers.get().getLevelProgressionSystem();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NPCEntity npcComponent = archetypeChunk.getComponent(index, npcComponentType);
        NpcRpgDebugComponent debugComponent = archetypeChunk.getComponent(index, debugComponentType);
        if (npcComponent == null || debugComponent == null) {
            return;
        }

        debugComponent.tick(dt);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Role role = npcComponent.getRole();
        if (role == null) {
            return;
        }

        String classLine = buildClassLine(ref, store);
        String abilityLine = buildAbilityLine(debugComponent);

        String display = abilityLine.isEmpty()
                ? classLine
                : classLine + " | " + abilityLine;

        DebugSupport debugSupport = role.getDebugSupport();
        debugSupport.setDisplayCustomString(display);
    }

    private String buildClassLine(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
        if (classComponent == null) {
            return "Class: None";
        }

        String classId = classComponent.getPrimaryClassId();
        if (classId == null || classId.isEmpty()) {
            return "Class: None";
        }

        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        String displayName = classDef != null && classDef.getDisplayName() != null && !classDef.getDisplayName().isEmpty()
                ? classDef.getDisplayName()
                : classId;

        String systemId = classDef != null
                ? (classDef.usesCharacterLevel() ? "Base_Character_Level" : classDef.getLevelSystemId())
                : "";
        int level = systemId != null && !systemId.isEmpty() ? levelSystem.getLevel(ref, systemId) : 0;
        if (level <= 0) {
            level = 1;
        }

        return "Class: " + displayName + " L" + level;
    }

    private String buildAbilityLine(@Nonnull NpcRpgDebugComponent debugComponent) {
        if (!debugComponent.hasRecentAbility()) {
            return "";
        }

        String abilityId = debugComponent.getLastAbilityId();
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        String displayName = abilityDef != null && abilityDef.getDisplayName() != null && !abilityDef.getDisplayName().isEmpty()
                ? abilityDef.getDisplayName()
                : abilityId;

        return "Casting: " + displayName;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }
}
