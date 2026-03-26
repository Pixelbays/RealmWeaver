package org.pixelbays.rpg.global.interaction;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.global.prereq.PrerequisiteEvaluator;
import org.pixelbays.rpg.global.prereq.PrerequisiteRequirements;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
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

	@Nonnull
	public static final BuilderCodec<PrerequisiteCheckInteraction> PREREQUISITE_CHECK_CODEC = BuilderCodec.builder(
			PrerequisiteCheckInteraction.class,
			PrerequisiteCheckInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Checks optional class, race, expansion, level, achievement, party, and raid requirements before allowing the interaction chain to continue.")
			.append(new KeyedCodec<>("RequiredClassIds", PrerequisiteRequirements.CLASS_LIST_CODEC, false),
					(i, v) -> i.prerequisites.setRequiredClassIds(v),
					i -> i.prerequisites.getRequiredClassIds())
			.add()
			.append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false),
					(i, v) -> i.prerequisites.setRequiredClassLevel(v),
					i -> i.prerequisites.getRequiredClassLevel())
			.add()
			.append(new KeyedCodec<>("RequiredRaceIds", PrerequisiteRequirements.RACE_LIST_CODEC, false),
					(i, v) -> i.prerequisites.setRequiredRaceIds(v),
					i -> i.prerequisites.getRequiredRaceIds())
			.add()
			.append(new KeyedCodec<>("RequiredExpansionIds", PrerequisiteRequirements.STRING_LIST_CODEC, false),
					(i, v) -> i.prerequisites.setRequiredExpansionIds(v),
					i -> i.prerequisites.getRequiredExpansionIds())
			.add()
			.append(new KeyedCodec<>("RequiredLevel", Codec.INTEGER, false),
					(i, v) -> i.prerequisites.setRequiredLevel(v),
					i -> i.prerequisites.getRequiredLevel())
			.add()
			.append(new KeyedCodec<>("RequiredLevelSystemId", new org.pixelbays.rpg.global.config.builder.LevelSystemRefCodec(), false),
					(i, v) -> i.prerequisites.setRequiredLevelSystemId(v),
					i -> i.prerequisites.getRequiredLevelSystemId())
			.add()
			.append(new KeyedCodec<>("RequiredAchievementIds", PrerequisiteRequirements.STRING_LIST_CODEC, false),
					(i, v) -> i.prerequisites.setRequiredAchievementIds(v),
					i -> i.prerequisites.getRequiredAchievementIds())
			.add()
			.append(new KeyedCodec<>("RequireParty", Codec.BOOLEAN, false),
					(i, v) -> i.prerequisites.setRequireParty(v),
					i -> i.prerequisites.isRequireParty())
			.add()
			.append(new KeyedCodec<>("RequiredPartyCount", Codec.INTEGER, false),
					(i, v) -> i.prerequisites.setRequiredPartyCount(v),
					i -> i.prerequisites.getRequiredPartyCount())
			.add()
			.append(new KeyedCodec<>("RequireRaid", Codec.BOOLEAN, false),
					(i, v) -> i.prerequisites.setRequireRaid(v),
					i -> i.prerequisites.isRequireRaid())
			.add()
			.append(new KeyedCodec<>("RequiredRaidCount", Codec.INTEGER, false),
					(i, v) -> i.prerequisites.setRequiredRaidCount(v),
					i -> i.prerequisites.getRequiredRaidCount())
			.add()
			.append(new KeyedCodec<>("SendFailureMessage", Codec.BOOLEAN, false),
					(i, v) -> i.sendFailureMessage = v,
					i -> i.sendFailureMessage)
			.add()
			.build();

	private PrerequisiteRequirements prerequisites = new PrerequisiteRequirements();
	private boolean sendFailureMessage = true;

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		Store<EntityStore> store = InteractionPlayerUtil.resolveStore(context);
		Ref<EntityStore> entityRef = InteractionPlayerUtil.getEntityRef(context);
		PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);
		if (store == null || entityRef == null) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		Message failure = PrerequisiteEvaluator.evaluateFailure(prerequisites, context.getCommandBuffer(), store, entityRef, playerRef);
		if (failure == null) {
			context.getState().state = InteractionState.Finished;
			return;
		}

		if (sendFailureMessage) {
			InteractionPlayerUtil.sendMessage(context, failure);
		}
		context.getState().state = InteractionState.Failed;
	}
}