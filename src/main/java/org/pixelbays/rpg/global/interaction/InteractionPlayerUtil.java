package org.pixelbays.rpg.global.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class InteractionPlayerUtil {

	private InteractionPlayerUtil() {
	}

	@Nullable
	public static Ref<EntityStore> getEntityRef(@Nonnull InteractionContext context) {
		Ref<EntityStore> entityRef = context.getEntity();
		return entityRef != null && entityRef.isValid() ? entityRef : null;
	}

	@Nullable
	public static Store<EntityStore> resolveStore(@Nonnull InteractionContext context) {
		Ref<EntityStore> entityRef = getEntityRef(context);
		return entityRef == null ? null : entityRef.getStore();
	}

	@Nullable
	public static PlayerRef resolvePlayerRef(@Nonnull InteractionContext context) {
		Ref<EntityStore> entityRef = getEntityRef(context);
		if (entityRef == null) {
			return null;
		}

		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		if (commandBuffer != null) {
			PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
			if (playerRef != null) {
				return playerRef;
			}
		}

		return entityRef.getStore().getComponent(entityRef, PlayerRef.getComponentType());
	}

	@Nullable
	public static Player resolvePlayer(@Nonnull InteractionContext context) {
		Ref<EntityStore> entityRef = getEntityRef(context);
		if (entityRef == null) {
			return null;
		}

		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		if (commandBuffer != null) {
			Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
			if (player != null) {
				return player;
			}
		}

		return entityRef.getStore().getComponent(entityRef, Player.getComponentType());
	}

	public static void sendMessage(@Nonnull InteractionContext context, @Nonnull Message message) {
		PlayerRef playerRef = resolvePlayerRef(context);
		if (playerRef != null) {
			playerRef.sendMessage(message);
		}
	}
}