package org.pixelbays.rpg.world;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;
import com.hypixel.hytale.builtin.instances.removal.WorldEmptyCondition;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;

public final class InstanceWorldLifecycle {

	private InstanceWorldLifecycle() {
	}

	public static void configureEphemeralInstance(@Nullable World instanceWorld) {
		if (instanceWorld == null || !instanceWorld.isAlive()) {
			return;
		}

		instanceWorld.execute(() -> {
			if (!instanceWorld.isAlive()) {
				return;
			}

			WorldConfig config = instanceWorld.getWorldConfig();
			config.setDeleteOnRemove(true);
			InstanceWorldConfig.ensureAndGet(config).setRemovalConditions(WorldEmptyCondition.REMOVE_WHEN_EMPTY);
			config.markChanged();
		});
	}
}