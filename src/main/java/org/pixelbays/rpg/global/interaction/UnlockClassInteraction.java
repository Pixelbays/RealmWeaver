package org.pixelbays.rpg.global.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.pixelbays.rpg.global.config.builder.ClassRefCodec;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class UnlockClassInteraction extends SimpleInstantInteraction {

	private static final FunctionCodec<String[], List<String>> CLASS_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(new ClassRefCodec(), String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	@Nonnull
	public static final BuilderCodec<UnlockClassInteraction> CODEC = BuilderCodec.builder(
			UnlockClassInteraction.class,
			UnlockClassInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Learns one or more classes for the interacting entity.")
			.append(new KeyedCodec<>("ClassIds", CLASS_LIST_CODEC, false),
					(i, v) -> i.classIds = v,
					i -> i.classIds)
			.add()
			.build();

	private List<String> classIds = new ArrayList<>();

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		var store = InteractionPlayerUtil.resolveStore(context);
		var entityRef = InteractionPlayerUtil.getEntityRef(context);
		if (store == null || entityRef == null || classIds.isEmpty()) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		ClassComponent classComponent = store.getComponent(entityRef, ClassComponent.getComponentType());
		boolean success = false;
		for (String classId : classIds) {
			if (classId == null || classId.isBlank()) {
				continue;
			}
			if (classComponent != null && classComponent.hasLearnedClass(classId)) {
				success = true;
				continue;
			}
			String result = ExamplePlugin.get().getClassManagementSystem().learnClass(entityRef, classId, store);
			if (result.startsWith("SUCCESS:")) {
				success = true;
			}
		}

		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}