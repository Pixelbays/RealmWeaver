package org.pixelbays.rpg.leveling.handlers;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.config.settings.LevelingModSettings.HardcoreLossType;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class HardcoreHandler extends DeathSystems.OnDeathSystem {

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		var playerComponentType = Player.getComponentType();
		if (playerComponentType == null) {
			RpgLogging.debugDeveloper("HardcoreHandler registered before Player component type is available. Falling back to any().");
			return Query.any();
		}
		return playerComponentType;
	}

	@Override
	public void onComponentAdded(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull DeathComponent component,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer) {
		RpgModConfig config = resolveConfig();
		if (config == null || !config.isHardcoreEnabled()) {
			return;
		}

		LevelProgressionComponent levelComp = store.getComponent(ref, LevelProgressionComponent.getComponentType());
		if (levelComp == null) {
			return;
		}

		HardcoreLossType lossType = config.getHardcoreLossType();
		int lossPercent = clampPercent(config.getHardcoreLevelLossPercent());
		StatSystem statSystem = ExamplePlugin.get().getStatSystem();

		for (Map.Entry<String, LevelProgressionComponent.LevelSystemData> entry : levelComp.getAllSystems().entrySet()) {
			String systemId = entry.getKey();
			LevelProgressionComponent.LevelSystemData levelData = entry.getValue();
			if (levelData == null) {
				continue;
			}

			int currentLevel = levelData.getCurrentLevel();
			int newLevel = currentLevel;

			if (lossType == HardcoreLossType.ResetToZero) {
				newLevel = 1;
			} else if (lossType == HardcoreLossType.LosePercent) {
				int lossAmount = (int) Math.floor(currentLevel * (lossPercent / 100f));
				newLevel = Math.max(1, currentLevel - lossAmount);
			}

			if (newLevel == currentLevel && lossType == HardcoreLossType.LosePercent) {
				continue;
			}

			levelData.setCurrentLevel(newLevel);
			levelData.setCurrentExp(0f);

			LevelSystemConfig systemConfig = ExamplePlugin.get().getLevelProgressionSystem().getConfig(systemId);
			if (systemConfig != null) {
				float expRequired = systemConfig.calculateExpForLevel(newLevel + 1);
				levelData.setExpToNextLevel(expRequired);
			}

			RpgLogging.debugDeveloper("[Hardcore] Applied loss: system=%s level=%s->%s type=%s percent=%s entity=%s",
					systemId, currentLevel, newLevel, lossType, lossPercent, ref);
		}

		if (statSystem != null) {
			if (lossType == HardcoreLossType.ResetToZero) {
				statSystem.clearClassStatBonuses(ref, store);
			} else if (lossType == HardcoreLossType.LosePercent) {
				statSystem.recalculateRaceStatBonuses(ref, store);
			}
		}
	}

	private int clampPercent(int percent) {
		return Math.max(0, Math.min(100, percent));
	}

	private RpgModConfig resolveConfig() {
		var assetMap = RpgModConfig.getAssetMap();
		if (assetMap == null) {
			return null;
		}

		RpgModConfig config = assetMap.getAsset("Default");
		if (config != null) {
			return config;
		}

		if (assetMap.getAssetMap().isEmpty()) {
			return null;
		}

		return assetMap.getAssetMap().values().iterator().next();
	}
}
