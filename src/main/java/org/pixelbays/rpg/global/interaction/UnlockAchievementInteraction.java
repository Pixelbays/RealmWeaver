package org.pixelbays.rpg.global.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class UnlockAchievementInteraction extends SimpleInstantInteraction {

	private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(Codec.STRING, String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	@Nonnull
	public static final BuilderCodec<UnlockAchievementInteraction> CODEC = BuilderCodec.builder(
			UnlockAchievementInteraction.class,
			UnlockAchievementInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Directly unlocks one or more achievements for the interacting player.")
			.append(new KeyedCodec<>("AchievementIds", STRING_LIST_CODEC, false),
					(i, v) -> i.achievementIds = v,
					i -> i.achievementIds)
			.add()
			.build();

	private List<String> achievementIds = new ArrayList<>();

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		Store<EntityStore> store = InteractionPlayerUtil.resolveStore(context);
		Ref<EntityStore> entityRef = InteractionPlayerUtil.getEntityRef(context);
		if (store == null || entityRef == null || achievementIds.isEmpty()) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		boolean success = false;
		for (String achievementId : achievementIds) {
			if (achievementId == null || achievementId.isBlank()) {
				continue;
			}
			if (ExamplePlugin.get().getAchievementSystem().unlockAchievement(entityRef, store, achievementId)) {
				success = true;
			}
		}

		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}