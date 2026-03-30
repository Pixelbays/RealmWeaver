package org.pixelbays.rpg.global.prereq;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.achievement.config.AchievementDefinition;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyType;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public final class PrerequisiteEvaluator {

	private PrerequisiteEvaluator() {
	}

	public static boolean meetsRequirements(@Nullable PrerequisiteRequirements requirements,
			@Nullable CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef,
			@Nullable PlayerRef explicitPlayerRef) {
		return evaluateFailure(requirements, commandBuffer, store, entityRef, explicitPlayerRef) == null;
	}

	@Nullable
	public static Message evaluateFailure(@Nullable PrerequisiteRequirements requirements,
			@Nullable CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef,
			@Nullable PlayerRef explicitPlayerRef) {
		if (requirements == null || !requirements.hasRequirements()) {
			return null;
		}

		ClassComponent classComponent = getComponent(commandBuffer, store, entityRef, ClassComponent.getComponentType());
		RaceComponent raceComponent = getComponent(commandBuffer, store, entityRef, RaceComponent.getComponentType());
		LevelProgressionComponent levelComponent = getComponent(commandBuffer, store, entityRef,
				LevelProgressionComponent.getComponentType());
		PlayerRef playerRef = explicitPlayerRef != null
				? explicitPlayerRef
				: getComponent(commandBuffer, store, entityRef, PlayerRef.getComponentType());

		if (!requirements.getRequiredClassIds().isEmpty()) {
			if (classComponent == null) {
				return Message.translation("pixelbays.rpg.interaction.prereq.class")
						.param("classes", describeClasses(requirements.getRequiredClassIds()));
			}

			String matchedClassId = findMatchingClass(requirements, classComponent, levelComponent);
			if (matchedClassId == null) {
				if (requirements.getRequiredClassLevel() > 0) {
					return Message.translation("pixelbays.rpg.interaction.prereq.classLevel")
							.param("classes", describeClasses(requirements.getRequiredClassIds()))
							.param("level", requirements.getRequiredClassLevel());
				}
				return Message.translation("pixelbays.rpg.interaction.prereq.class")
						.param("classes", describeClasses(requirements.getRequiredClassIds()));
			}
		}

		if (!requirements.getRequiredRaceIds().isEmpty()) {
			String currentRaceId = raceComponent == null ? "" : raceComponent.getRaceId();
			boolean raceMatched = requirements.getRequiredRaceIds().stream()
					.filter(requiredRaceId -> requiredRaceId != null && !requiredRaceId.isBlank())
					.anyMatch(requiredRaceId -> requiredRaceId.equalsIgnoreCase(currentRaceId));
			if (!raceMatched) {
				return Message.translation("pixelbays.rpg.interaction.prereq.race")
						.param("races", describeRaces(requirements.getRequiredRaceIds()));
			}
		}

		if (!requirements.getRequiredExpansionIds().isEmpty()) {
			if (playerRef == null || !Realmweavers.get().getExpansionManager().hasAccess(playerRef,
					requirements.getRequiredExpansionIds())) {
				return Message.translation("pixelbays.rpg.interaction.prereq.expansion")
						.param("expansions", describeExpansions(requirements.getRequiredExpansionIds()));
			}
		}

		if (requirements.getRequiredLevel() > 0) {
			int currentLevel = resolveCurrentLevel(requirements, levelComponent);
			if (currentLevel < requirements.getRequiredLevel()) {
				String levelSystemId = getEffectiveLevelSystemId(requirements);
				if (!levelSystemId.equals("Base_Character_Level")) {
					return Message.translation("pixelbays.rpg.interaction.prereq.levelSystem")
							.param("system", levelSystemId)
							.param("level", requirements.getRequiredLevel());
				}
				return Message.translation("pixelbays.rpg.interaction.prereq.level")
						.param("level", requirements.getRequiredLevel());
			}
		}

		if (!requirements.getRequiredAchievementIds().isEmpty()) {
			if (playerRef == null || !hasRequiredAchievements(requirements, store, entityRef, playerRef)) {
				return Message.translation("pixelbays.rpg.interaction.prereq.achievement")
						.param("achievements", describeAchievements(requirements.getRequiredAchievementIds()));
			}
		}

		PartyManager partyManager = Realmweavers.get().getPartyManager();
		Party party = playerRef == null ? null : partyManager.getPartyForMember(playerRef.getUuid());

		if (requirements.isRequireParty() || requirements.getRequiredPartyCount() > 0) {
			if (party == null || party.getType() != PartyType.PARTY) {
				if (requirements.getRequiredPartyCount() > 1) {
					return Message.translation("pixelbays.rpg.interaction.prereq.partyCount")
							.param("count", Math.max(1, requirements.getRequiredPartyCount()));
				}
				return Message.translation("pixelbays.rpg.interaction.prereq.party");
			}
			if (requirements.getRequiredPartyCount() > 0 && party.size() < requirements.getRequiredPartyCount()) {
				return Message.translation("pixelbays.rpg.interaction.prereq.partyCount")
						.param("count", requirements.getRequiredPartyCount());
			}
		}

		if (requirements.isRequireRaid() || requirements.getRequiredRaidCount() > 0) {
			if (party == null || party.getType() != PartyType.RAID) {
				if (requirements.getRequiredRaidCount() > 1) {
					return Message.translation("pixelbays.rpg.interaction.prereq.raidCount")
							.param("count", Math.max(1, requirements.getRequiredRaidCount()));
				}
				return Message.translation("pixelbays.rpg.interaction.prereq.raid");
			}
			if (requirements.getRequiredRaidCount() > 0 && party.size() < requirements.getRequiredRaidCount()) {
				return Message.translation("pixelbays.rpg.interaction.prereq.raidCount")
						.param("count", requirements.getRequiredRaidCount());
			}
		}

		return null;
	}

	@Nullable
	private static String findMatchingClass(@Nonnull PrerequisiteRequirements requirements,
			@Nonnull ClassComponent classComponent,
			@Nullable LevelProgressionComponent levelComponent) {
		for (String classId : requirements.getRequiredClassIds()) {
			if (classId == null || classId.isBlank() || !classComponent.hasLearnedClass(classId)) {
				continue;
			}

			if (requirements.getRequiredClassLevel() <= 0) {
				return classId;
			}

			ClassDefinition definition = ClassDefinition.getAssetMap() == null ? null : ClassDefinition.getAssetMap().getAsset(classId);
			if (definition == null || levelComponent == null) {
				continue;
			}

			String levelSystemId = definition.usesCharacterLevel() ? "Base_Character_Level" : definition.getLevelSystemId();
			LevelProgressionComponent.LevelSystemData levelData = levelComponent.getSystem(levelSystemId);
			int currentLevel = levelData == null ? 1 : levelData.getCurrentLevel();
			if (currentLevel >= requirements.getRequiredClassLevel()) {
				return classId;
			}
		}
		return null;
	}

	private static int resolveCurrentLevel(@Nonnull PrerequisiteRequirements requirements,
			@Nullable LevelProgressionComponent levelComponent) {
		if (levelComponent == null) {
			return 0;
		}
		LevelProgressionComponent.LevelSystemData levelData = levelComponent.getSystem(getEffectiveLevelSystemId(requirements));
		return levelData == null ? 0 : levelData.getCurrentLevel();
	}

	@Nonnull
	private static String getEffectiveLevelSystemId(@Nonnull PrerequisiteRequirements requirements) {
		return requirements.getRequiredLevelSystemId().isBlank() ? "Base_Character_Level" : requirements.getRequiredLevelSystemId();
	}

	private static boolean hasRequiredAchievements(@Nonnull PrerequisiteRequirements requirements,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef,
			@Nonnull PlayerRef playerRef) {
		AchievementComponent characterState = store.getComponent(entityRef, AchievementComponent.getComponentType());
		CharacterManager characterManager = Realmweavers.get().getCharacterManager();
		AchievementComponent accountState = characterManager.getOrCreateAccountAchievementProgress(playerRef.getUuid(), playerRef.getUsername());

		for (String achievementId : requirements.getRequiredAchievementIds()) {
			if (achievementId == null || achievementId.isBlank()) {
				continue;
			}
			var achievementAssetMap = AchievementDefinition.getAssetMap();
			AchievementDefinition definition = achievementAssetMap == null ? null : achievementAssetMap.getAsset(achievementId);
			AchievementComponent targetState = definition != null && definition.isAccountWide() ? accountState : characterState;
			if (targetState == null || !targetState.isUnlocked(achievementId)) {
				return false;
			}
		}
		return true;
	}

	@Nonnull
	private static String describeClasses(@Nonnull List<String> classIds) {
		return joinDisplayNames(classIds, classId -> {
			ClassDefinition definition = ClassDefinition.getAssetMap() == null ? null : ClassDefinition.getAssetMap().getAsset(classId);
			return definition == null ? classId : definition.getDisplayName();
		});
	}

	@Nonnull
	private static String describeRaces(@Nonnull List<String> raceIds) {
		return joinDisplayNames(raceIds, raceId -> {
			RaceDefinition definition = RaceDefinition.getAssetMap() == null ? null : RaceDefinition.getAssetMap().getAsset(raceId);
			return definition == null ? raceId : definition.getDisplayName();
		});
	}

	@Nonnull
	private static String describeExpansions(@Nonnull List<String> expansionIds) {
		return Realmweavers.get().getExpansionManager().describeRequirements(expansionIds);
	}

	@Nonnull
	private static String describeAchievements(@Nonnull List<String> achievementIds) {
		return joinDisplayNames(achievementIds, achievementId -> {
			var achievementAssetMap = AchievementDefinition.getAssetMap();
			AchievementDefinition definition = achievementAssetMap == null ? null : achievementAssetMap.getAsset(achievementId);
			return definition == null ? achievementId : definition.getDisplayName();
		});
	}

	@Nonnull
	private static String joinDisplayNames(@Nonnull List<String> ids, @Nonnull java.util.function.Function<String, String> resolver) {
		List<String> names = new ArrayList<>();
		for (String value : ids) {
			if (value == null || value.isBlank()) {
				continue;
			}
			names.add(resolver.apply(value));
		}
		return names.isEmpty() ? "Unknown" : String.join(", ", names);
	}

	@Nullable
	private static <T extends Component<EntityStore>> T getComponent(@Nullable CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef,
			@Nonnull com.hypixel.hytale.component.ComponentType<EntityStore, T> componentType) {
		if (commandBuffer != null) {
			T component = commandBuffer.getComponent(entityRef, componentType);
			if (component != null) {
				return component;
			}
		}
		return store.getComponent(entityRef, componentType);
	}
}