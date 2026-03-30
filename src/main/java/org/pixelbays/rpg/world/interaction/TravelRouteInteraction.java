package org.pixelbays.rpg.world.interaction;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.interaction.InteractionPlayerUtil;

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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class TravelRouteInteraction extends SimpleInstantInteraction {

	@Nonnull
	public static final BuilderCodec<TravelRouteInteraction> CODEC = BuilderCodec.builder(
			TravelRouteInteraction.class,
			TravelRouteInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Travels the player or their group using a configured RPG world route.")
			.append(new KeyedCodec<>("RouteId", Codec.STRING),
					(i, v) -> i.routeId = v,
					i -> i.routeId)
			.add()
			.build();

	private String routeId = "";

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		Store<EntityStore> store = InteractionPlayerUtil.resolveStore(context);
		Ref<EntityStore> entityRef = InteractionPlayerUtil.getEntityRef(context);
		PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);
		if (store == null || entityRef == null || playerRef == null) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		boolean success = Realmweavers.get().getWorldTravelManager().travelRoute(routeId, store, entityRef, playerRef);
		context.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
	}
}