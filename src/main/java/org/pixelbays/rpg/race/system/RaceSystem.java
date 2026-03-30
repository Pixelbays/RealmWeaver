package org.pixelbays.rpg.race.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.event.RaceAbilityUnlockedEvent;
import org.pixelbays.rpg.race.event.RaceChangedEvent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * System that applies race selections to entities and integrates with other RPG systems.
 */
@SuppressWarnings("null")
public class RaceSystem {

    private final RaceManagementSystem raceManagementSystem;
    private StatSystem statSystem;

    public RaceSystem(@Nonnull RaceManagementSystem raceManagementSystem) {
        this.raceManagementSystem = raceManagementSystem;
    }

    public void setStatSystem(@Nullable StatSystem statSystem) {
        this.statSystem = statSystem;
    }

    /**
     * Assign a race to an entity, applying any race-related effects.
     */
    public boolean setRace(@Nonnull Ref<EntityStore> entityRef, @Nonnull String raceId,
            @Nullable Store<EntityStore> store) {
        if (raceId.isEmpty()) {
            return false;
        }

        RaceDefinition raceDef = raceManagementSystem.getRaceDefinition(raceId);
        if (raceDef == null || !raceDef.isEnabled()) {
            return false;
        }

        Store<EntityStore> effectiveStore = store != null ? store : entityRef.getStore();

        RaceComponent raceComponent = getOrCreateRaceComponent(entityRef, effectiveStore);
        String oldRaceId = raceComponent.getRaceId() == null ? "" : raceComponent.getRaceId();
        raceComponent.setRaceId(raceId);

        applyRaceStats(entityRef, raceDef, effectiveStore);
        applyRaceAbilities(entityRef, raceDef, raceComponent);
        applyHeroRaceStartingLevel(entityRef, raceDef, effectiveStore);
        applyCosmeticRules(raceDef);

        RaceChangedEvent.dispatch(entityRef, oldRaceId, raceId);

        RpgLogging.debugDeveloper("[RaceSystem] Set race to ", raceId);
        return true;
    }

    /**
     * Returns the active race id (empty if none).
     */
    @Nonnull
    public String getRaceId(@Nonnull Ref<EntityStore> entityRef) {
        RaceComponent raceComponent = getRaceComponent(entityRef);
        return raceComponent == null ? "" : raceComponent.getRaceId();
    }

    /**
     * Get the active race definition for an entity, or null if none.
     */
    @Nullable
    public RaceDefinition getRaceDefinitionForEntity(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        RaceComponent raceComponent = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (raceComponent == null || raceComponent.getRaceId() == null || raceComponent.getRaceId().isEmpty()) {
            return null;
        }

        RaceDefinition raceDef = raceManagementSystem.getRaceDefinition(raceComponent.getRaceId());
        return raceDef != null && raceDef.isEnabled() ? raceDef : null;
    }

    private void applyRaceStats(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull RaceDefinition raceDef,
            @Nonnull Store<EntityStore> store) {
        if (statSystem == null) {
            return;
        }

        // Apply starting stat increases (one-time flat bonuses)
        if (raceDef.getStartingStats() != null && !raceDef.getStartingStats().isEmpty()) {
            statSystem.applyStatIncreases(entityRef, raceDef.getStartingStats(), store);
        }

        // Apply race stat modifiers (additive/multiplicative bonuses)
        statSystem.recalculateRaceStatBonuses(entityRef, store);
    }

    private void applyRaceAbilities(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull RaceDefinition raceDef,
            @Nonnull RaceComponent raceComponent) {
        if (raceDef.getAbilityIds() == null) {
            return;
        }

        for (String abilityId : raceDef.getAbilityIds()) {
            if (abilityId == null || abilityId.isEmpty()) {
                continue;
            }

            if (!raceComponent.hasUnlockedRaceAbility(abilityId)) {
                raceComponent.unlockRaceAbility(abilityId);
                RaceAbilityUnlockedEvent.dispatch(entityRef, raceDef.getRaceId(), abilityId);
            }
        }

    }

    private void applyCosmeticRules(@Nonnull RaceDefinition raceDef) {
        // Placeholder for AllowedCosmeticCategories, AllowedCosmeticIds and NotAllowed lists.
        if (raceDef.getAllowedCosmeticCategories() != null && !raceDef.getAllowedCosmeticCategories().isEmpty()) {
            RpgLogging.debugDeveloper("[RaceSystem] TODO: Apply AllowedCosmeticCategories for ", raceDef.getRaceId());
        }
    }

    private void applyHeroRaceStartingLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull RaceDefinition raceDef,
            @Nonnull Store<EntityStore> store) {
        if (!raceDef.isHeroRace()) {
            return;
        }

        var levelSystem = Realmweavers.get().getLevelProgressionSystem();
        if (levelSystem == null) {
            return;
        }

        int targetLevel = raceDef.getInitialCharacterLevel();
        int currentLevel = levelSystem.getLevel(entityRef, "Base_Character_Level");
        if (targetLevel <= currentLevel) {
            return;
        }

        levelSystem.addLevels(entityRef, "Base_Character_Level", targetLevel - currentLevel, store,
                store.getExternalData().getWorld());
    }

    @Nonnull
    private RaceComponent getOrCreateRaceComponent(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        RaceComponent component = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (component == null) {
            component = store.addComponent(entityRef, RaceComponent.getComponentType());
        }
        return component;
    }

    @Nullable
    private RaceComponent getRaceComponent(@Nonnull Ref<EntityStore> entityRef) {
        return entityRef.getStore().getComponent(entityRef, RaceComponent.getComponentType());
    }
}
