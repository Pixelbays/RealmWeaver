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
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class UnlockExpansionInteraction extends SimpleInstantInteraction {

	private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(Codec.STRING, String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	@Nonnull
	public static final BuilderCodec<UnlockExpansionInteraction> CODEC = BuilderCodec.builder(
			UnlockExpansionInteraction.class,
			UnlockExpansionInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Grants one or more expansion unlocks to the interacting player.")
			.append(new KeyedCodec<>("ExpansionIds", STRING_LIST_CODEC, false),
					(i, v) -> i.expansionIds = v,
					i -> i.expansionIds)
			.add()
			.build();

	private List<String> expansionIds = new ArrayList<>();

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);
		if (playerRef == null || expansionIds.isEmpty()) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		boolean success = false;
		for (String expansionId : expansionIds) {
			if (expansionId == null || expansionId.isBlank()) {
				continue;
			}
			if (ExamplePlugin.get().getExpansionManager().unlockForPlayer(playerRef.getUuid(), expansionId)) {
				success = true;
			}
		}

		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}