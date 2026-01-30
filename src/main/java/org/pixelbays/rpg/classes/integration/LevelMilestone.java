package org.pixelbays.rpg.classes.integration;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Listens for level-up events and grants class milestone rewards.
 */
public class LevelMilestone {

	private static final String CHARACTER_LEVEL_SYSTEM_ID = "Base_Character_Level";

	public LevelMilestone() {
	}

	public void register(@Nonnull EventRegistry eventRegistry) {
		eventRegistry.register(LevelUpEvent.class, this::onLevelUp);
	}

	private void onLevelUp(@Nonnull LevelUpEvent event) {
		Ref<EntityStore> playerRef = event.playerRef();
		Store<EntityStore> store = playerRef.getStore();
		String systemId = event.systemId();
		if (systemId == null || systemId.isEmpty()) {
			return;
		}

		ClassComponent classComp = store.getComponent(playerRef, ClassComponent.getComponentType());
		if (classComp == null) {
			return;
		}

		int startLevel = event.oldLevel() + 1;
		int endLevel = event.newLevel();
		if (endLevel < startLevel) {
			return;
		}

		LevelProgressionComponent.LevelSystemData levelData = getLevelData(store, playerRef, systemId);
		for (String classId : classComp.getLearnedClassIds()) {
			if (classId == null || classId.isEmpty()) {
				continue;
			}
			ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
			if (classDef == null) {
				continue;
			}

			if (!systemId.equals(resolveSystemId(classDef))) {
				continue;
			}

			List<ClassDefinition.LevelMilestone> milestones = classDef.getLevelMilestones();
			if (milestones == null || milestones.isEmpty()) {
				continue;
			}

			for (int level = startLevel; level <= endLevel; level++) {
				ClassDefinition.LevelMilestone milestone = classDef.getMilestoneAtLevel(level);
				if (milestone == null) {
					continue;
				}

				applyMilestone(playerRef, classDef, milestone, levelData, systemId);
			}
		}
	}

	@Nullable
	private LevelProgressionComponent.LevelSystemData getLevelData(@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> playerRef,
			@Nullable String systemId) {
		if (systemId == null || systemId.isEmpty()) {
			return null;
		}

		LevelProgressionComponent levelComp = store.getComponent(playerRef,
				LevelProgressionComponent.getComponentType());
		if (levelComp == null) {
			return null;
		}

		return levelComp.getOrCreateSystem(systemId);
	}

	@Nullable
	private String resolveSystemId(@Nonnull ClassDefinition classDef) {
		if (classDef.usesCharacterLevel()) {
			return CHARACTER_LEVEL_SYSTEM_ID;
		}

		String systemId = classDef.getLevelSystemId();
		return systemId == null || systemId.isEmpty() ? null : systemId;
	}

	private void applyMilestone(@Nonnull Ref<EntityStore> playerRef,
			@Nonnull ClassDefinition classDef,
			@Nonnull ClassDefinition.LevelMilestone milestone,
			@Nullable LevelProgressionComponent.LevelSystemData levelData,
			@Nullable String systemId) {
		if (milestone.getSkillPoints() > 0 && levelData != null) {
			int before = levelData.getAvailableSkillPoints();
			levelData.addSkillPoints(milestone.getSkillPoints());
			RpgLogging.debugDeveloper(
					"[ClassMilestone] Granted %s skill points to %s (system=%s, before=%s, after=%s)",
					milestone.getSkillPoints(),
					classDef.getId(),
					systemId,
					before,
					levelData.getAvailableSkillPoints());
		}

		if (milestone.getItemRewards() != null && !milestone.getItemRewards().isEmpty()) {
			RpgLogging.debugDeveloper(
					"[ClassMilestone] TODO grant items %s for class %s",
					milestone.getItemRewards(),
					classDef.getId());
		}

		if (milestone.getInteractionChain() != null && !milestone.getInteractionChain().isEmpty()) {
			RpgLogging.debugDeveloper(
					"[ClassMilestone] TODO trigger interaction chain %s for class %s",
					milestone.getInteractionChain(),
					classDef.getId());
		}
	}
}
