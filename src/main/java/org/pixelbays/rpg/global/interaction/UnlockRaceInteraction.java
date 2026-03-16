package org.pixelbays.rpg.global.interaction;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.global.config.builder.RaceRefCodec;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class UnlockRaceInteraction extends SimpleInstantInteraction {

	@Nonnull
	public static final BuilderCodec<UnlockRaceInteraction> CODEC = BuilderCodec.builder(
			UnlockRaceInteraction.class,
			UnlockRaceInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Sets the race for the interacting entity.")
			.append(new KeyedCodec<>("RaceId", new RaceRefCodec()),
					(i, v) -> i.raceId = v,
					i -> i.raceId)
			.add()
			.build();

	private String raceId = "";

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		var store = InteractionPlayerUtil.resolveStore(context);
		var entityRef = InteractionPlayerUtil.getEntityRef(context);
		if (store == null || entityRef == null || raceId.isBlank()) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		RaceComponent raceComponent = store.getComponent(entityRef, RaceComponent.getComponentType());
		if (raceComponent != null && raceId.equalsIgnoreCase(raceComponent.getRaceId())) {
			context.getState().state = InteractionState.Finished;
			return;
		}

		boolean success = ExamplePlugin.get().getRaceSystem().setRace(entityRef, raceId, store);
		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}