package org.pixelbays.rpg.global.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.achievement.config.AchievementDefinition;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.builder.ClassRefCodec;
import org.pixelbays.rpg.global.config.builder.LevelSystemRefCodec;
import org.pixelbays.rpg.global.config.builder.RaceRefCodec;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyType;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class PrerequisiteCheckInteraction extends SimpleInstantInteraction {

	private static final FunctionCodec<String[], List<String>> CLASS_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(new ClassRefCodec(), String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	private static final FunctionCodec<String[], List<String>> RACE_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(new RaceRefCodec(), String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(Codec.STRING, String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	@Nonnull
	public static final BuilderCodec<PrerequisiteCheckInteraction> CODEC = BuilderCodec.builder(
			PrerequisiteCheckInteraction.class,
			PrerequisiteCheckInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Checks optional class, race, level, achievement, party, and raid requirements before allowing the interaction chain to continue.")
			.append(new KeyedCodec<>("RequiredClassIds", CLASS_LIST_CODEC, false),
					(i, v) -> i.requiredClassIds = v,
					i -> i.requiredClassIds)
			.add()
			.append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false),
					(i, v) -> i.requiredClassLevel = v,
					i -> i.requiredClassLevel)
			.add()
			.append(new KeyedCodec<>("RequiredRaceIds", RACE_LIST_CODEC, false),
					(i, v) -> i.requiredRaceIds = v,
					i -> i.requiredRaceIds)
			.add()
			.append(new KeyedCodec<>("RequiredLevel", Codec.INTEGER, false),
					(i, v) -> i.requiredLevel = v,
					i -> i.requiredLevel)
			.add()
			.append(new KeyedCodec<>("RequiredLevelSystemId", new LevelSystemRefCodec(), false),
					(i, v) -> i.requiredLevelSystemId = v,
					i -> i.requiredLevelSystemId)
			.add()
			.append(new KeyedCodec<>("RequiredAchievementIds", STRING_LIST_CODEC, false),
					(i, v) -> i.requiredAchievementIds = v,
					i -> i.requiredAchievementIds)
			.add()
			.append(new KeyedCodec<>("RequireParty", Codec.BOOLEAN, false),
					(i, v) -> i.requireParty = v,
					i -> i.requireParty)
			.add()
			.append(new KeyedCodec<>("RequiredPartyCount", Codec.INTEGER, false),
					(i, v) -> i.requiredPartyCount = v,
					i -> i.requiredPartyCount)
			.add()
			.append(new KeyedCodec<>("RequireRaid", Codec.BOOLEAN, false),
					(i, v) -> i.requireRaid = v,
					i -> i.requireRaid)
			.add()
			.append(new KeyedCodec<>("RequiredRaidCount", Codec.INTEGER, false),
					(i, v) -> i.requiredRaidCount = v,
					i -> i.requiredRaidCount)
			.add()
			.append(new KeyedCodec<>("SendFailureMessage", Codec.BOOLEAN, false),
					(i, v) -> i.sendFailureMessage = v,
					i -> i.sendFailureMessage)
			.add()
			.build();

	private List<String> requiredClassIds = new ArrayList<>();
	private int requiredClassLevel = 0;
	private List<String> requiredRaceIds = new ArrayList<>();
	private int requiredLevel = 0;
	private String requiredLevelSystemId = "";
	private List<String> requiredAchievementIds = new ArrayList<>();
	private boolean requireParty = false;
	private int requiredPartyCount = 0;
	private boolean requireRaid = false;
	private int requiredRaidCount = 0;
	private boolean sendFailureMessage = true;

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		Store<EntityStore> store = InteractionPlayerUtil.resolveStore(context);
		Ref<EntityStore> entityRef = InteractionPlayerUtil.getEntityRef(context);
		if (store == null || entityRef == null) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		FailureResult failure = evaluateFailure(context, store, entityRef);
		if (failure == null) {
			context.getState().state = InteractionState.Finished;
			return;
		}

		if (sendFailureMessage && failure.message() != null) {
			InteractionPlayerUtil.sendMessage(context, failure.message());
		}
		context.getState().state = InteractionState.Failed;
	}

	@Nullable
	private FailureResult evaluateFailure(@Nonnull InteractionContext context,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef) {
		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		ClassComponent classComponent = getComponent(commandBuffer, store, entityRef, ClassComponent.getComponentType());
		RaceComponent raceComponent = getComponent(commandBuffer, store, entityRef, RaceComponent.getComponentType());
		LevelProgressionComponent levelComponent = getComponent(commandBuffer, store, entityRef,
				LevelProgressionComponent.getComponentType());
		PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);

		if (!requiredClassIds.isEmpty()) {
			if (classComponent == null) {
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.class")
						.param("classes", describeClasses(requiredClassIds)));
			}

			String matchedClassId = findMatchingClass(classComponent, levelComponent);
			if (matchedClassId == null) {
				if (requiredClassLevel > 0) {
					return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.classLevel")
							.param("classes", describeClasses(requiredClassIds))
							.param("level", requiredClassLevel));
				}
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.class")
						.param("classes", describeClasses(requiredClassIds)));
			}
		}

		if (!requiredRaceIds.isEmpty()) {
			String currentRaceId = raceComponent == null ? "" : raceComponent.getRaceId();
			boolean raceMatched = requiredRaceIds.stream()
					.filter(id -> id != null && !id.isBlank())
					.anyMatch(id -> id.equalsIgnoreCase(currentRaceId));
			if (!raceMatched) {
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.race")
						.param("races", describeRaces(requiredRaceIds)));
			}
		}

		if (requiredLevel > 0) {
			int currentLevel = resolveCurrentLevel(levelComponent);
			if (currentLevel < requiredLevel) {
				String levelSystemId = getEffectiveLevelSystemId();
				if (!levelSystemId.equals("Base_Character_Level")) {
					return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.levelSystem")
							.param("system", levelSystemId)
							.param("level", requiredLevel));
				}
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.level")
						.param("level", requiredLevel));
			}
		}

		if (!requiredAchievementIds.isEmpty()) {
			if (playerRef == null || !hasRequiredAchievements(store, entityRef, playerRef)) {
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.achievement")
						.param("achievements", describeAchievements(requiredAchievementIds)));
			}
		}

		PartyManager partyManager = ExamplePlugin.get().getPartyManager();
		Party party = playerRef == null || partyManager == null ? null : partyManager.getPartyForMember(playerRef.getUuid());

		if (requireParty || requiredPartyCount > 0) {
			if (party == null || party.getType() != PartyType.PARTY) {
				if (requiredPartyCount > 1) {
					return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.partyCount")
							.param("count", Math.max(1, requiredPartyCount)));
				}
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.party"));
			}
			if (requiredPartyCount > 0 && party.size() < requiredPartyCount) {
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.partyCount")
						.param("count", requiredPartyCount));
			}
		}

		if (requireRaid || requiredRaidCount > 0) {
			if (party == null || party.getType() != PartyType.RAID) {
				if (requiredRaidCount > 1) {
					return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.raidCount")
							.param("count", Math.max(1, requiredRaidCount)));
				}
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.raid"));
			}
			if (requiredRaidCount > 0 && party.size() < requiredRaidCount) {
				return new FailureResult(Message.translation("pixelbays.rpg.interaction.prereq.raidCount")
						.param("count", requiredRaidCount));
			}
		}

		return null;
	}

	@Nullable
	private String findMatchingClass(@Nonnull ClassComponent classComponent,
			@Nullable LevelProgressionComponent levelComponent) {
		for (String classId : requiredClassIds) {
			if (classId == null || classId.isBlank() || !classComponent.hasLearnedClass(classId)) {
				continue;
			}

			if (requiredClassLevel <= 0) {
				return classId;
			}

			ClassDefinition definition = ClassDefinition.getAssetMap().getAsset(classId);
			if (definition == null || levelComponent == null) {
				continue;
			}

			String levelSystemId = definition.usesCharacterLevel() ? "Base_Character_Level" : definition.getLevelSystemId();
			LevelProgressionComponent.LevelSystemData levelData = levelComponent.getSystem(levelSystemId);
			int currentLevel = levelData == null ? 1 : levelData.getCurrentLevel();
			if (currentLevel >= requiredClassLevel) {
				return classId;
			}
		}
		return null;
	}

	private int resolveCurrentLevel(@Nullable LevelProgressionComponent levelComponent) {
		if (levelComponent == null) {
			return 0;
		}
		LevelProgressionComponent.LevelSystemData levelData = levelComponent.getSystem(getEffectiveLevelSystemId());
		return levelData == null ? 0 : levelData.getCurrentLevel();
	}

	@Nonnull
	private String getEffectiveLevelSystemId() {
		return requiredLevelSystemId == null || requiredLevelSystemId.isBlank()
				? "Base_Character_Level"
				: requiredLevelSystemId;
	}

	private boolean hasRequiredAchievements(@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> entityRef,
			@Nonnull PlayerRef playerRef) {
		AchievementComponent characterState = store.getComponent(entityRef, AchievementComponent.getComponentType());
		CharacterManager characterManager = ExamplePlugin.get().getCharacterManager();
		AchievementComponent accountState = characterManager == null
				? null
				: characterManager.getOrCreateAccountAchievementProgress(playerRef.getUuid(), playerRef.getUsername());

		for (String achievementId : requiredAchievementIds) {
			if (achievementId == null || achievementId.isBlank()) {
				continue;
			}
			AchievementDefinition definition = AchievementDefinition.getAssetMap() == null
					? null
					: AchievementDefinition.getAssetMap().getAsset(achievementId);
			AchievementComponent targetState = definition != null && definition.isAccountWide() ? accountState : characterState;
			if (targetState == null || !targetState.isUnlocked(achievementId)) {
				return false;
			}
		}
		return true;
	}

	@Nonnull
	private String describeClasses(@Nonnull List<String> classIds) {
		return joinDisplayNames(classIds, id -> {
			ClassDefinition definition = ClassDefinition.getAssetMap() == null ? null : ClassDefinition.getAssetMap().getAsset(id);
			return definition == null ? id : definition.getDisplayName();
		});
	}

	@Nonnull
	private String describeRaces(@Nonnull List<String> raceIds) {
		return joinDisplayNames(raceIds, id -> {
			RaceDefinition definition = RaceDefinition.getAssetMap() == null ? null : RaceDefinition.getAssetMap().getAsset(id);
			return definition == null ? id : definition.getDisplayName();
		});
	}

	@Nonnull
	private String describeAchievements(@Nonnull List<String> achievementIds) {
		return joinDisplayNames(achievementIds, id -> {
			AchievementDefinition definition = AchievementDefinition.getAssetMap() == null
					? null
					: AchievementDefinition.getAssetMap().getAsset(id);
			return definition == null ? id : definition.getDisplayName();
		});
	}

	@Nonnull
	private String joinDisplayNames(@Nonnull List<String> ids, @Nonnull java.util.function.Function<String, String> resolver) {
		List<String> names = new ArrayList<>();
		for (String id : ids) {
			if (id == null || id.isBlank()) {
				continue;
			}
			names.add(resolver.apply(id));
		}
		return names.isEmpty() ? "Unknown" : String.join(", ", names);
	}

	@Nullable
	private <T extends Component<EntityStore>> T getComponent(@Nullable CommandBuffer<EntityStore> commandBuffer,
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

	private record FailureResult(@Nullable Message message) {
	}
}