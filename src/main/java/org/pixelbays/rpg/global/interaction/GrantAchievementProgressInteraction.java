package org.pixelbays.rpg.global.interaction;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class GrantAchievementProgressInteraction extends SimpleInstantInteraction {

	@Nonnull
	public static final BuilderCodec<GrantAchievementProgressInteraction> CODEC = BuilderCodec.builder(
			GrantAchievementProgressInteraction.class,
			GrantAchievementProgressInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Grants progress to a specific achievement criterion and unlocks the achievement when its criteria are satisfied.")
			.append(new KeyedCodec<>("AchievementId", Codec.STRING),
					(i, v) -> i.achievementId = v,
					i -> i.achievementId)
			.add()
			.append(new KeyedCodec<>("CriterionId", Codec.STRING, false),
					(i, v) -> i.criterionId = v,
					i -> i.criterionId)
			.add()
			.append(new KeyedCodec<>("Amount", Codec.INTEGER, false),
					(i, v) -> i.amount = v,
					i -> i.amount)
			.add()
			.build();

	private String achievementId = "";
	private String criterionId = "";
	private int amount = 1;

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		Store<EntityStore> store = InteractionPlayerUtil.resolveStore(context);
		Ref<EntityStore> entityRef = InteractionPlayerUtil.getEntityRef(context);
		if (store == null || entityRef == null || achievementId.isBlank() || amount <= 0) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		boolean success = ExamplePlugin.get().getAchievementSystem()
				.grantAchievementProgress(entityRef, store, achievementId, criterionId, amount);
		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}