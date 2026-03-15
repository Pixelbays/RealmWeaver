package org.pixelbays.rpg.leveling.handlers;

import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.config.settings.LevelingModSettings.HardcoreLossType;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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

		applyCurrencyPenalty(ref, store, config);
	}

	private void applyCurrencyPenalty(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull RpgModConfig config) {
		if (!config.isHardcoreCurrencyLossEnabled()) {
			return;
		}

		int lossPercent = clampPercent(config.getHardcoreCurrencyLossPercent());
		if (lossPercent <= 0) {
			return;
		}

		Player player = store.getComponent(ref, Player.getComponentType());
		PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
		if (player == null || playerRef == null) {
			return;
		}

		CurrencyManager currencyManager = ExamplePlugin.get().getCurrencyManager();
		for (CurrencyScope scope : config.getHardcoreCurrencyLossScopes()) {
			if (scope == null) {
				continue;
			}

			String ownerId = resolveCurrencyOwnerId(scope, playerRef);
			if (ownerId == null || ownerId.isBlank()) {
				continue;
			}

			CurrencyAccessContext accessContext = scope == CurrencyScope.Character
					? CurrencyAccessContext.fromInventory(player.getInventory())
					: CurrencyAccessContext.empty();

			for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getByScope(scope)) {
				if (definition == null || !definition.isEnabled()) {
					continue;
				}

				long balance = currencyManager.getBalance(scope, ownerId, definition.getId(), accessContext);
				long penalty = (long) Math.floor(balance * (lossPercent / 100f));
				if (penalty <= 0L) {
					continue;
				}

				var result = currencyManager.spend(scope, ownerId,
						new CurrencyAmountDefinition(definition.getId(), penalty), accessContext);
				RpgLogging.debugDeveloper(
						"[Hardcore] Applied currency loss: scope=%s currency=%s amount=%s success=%s entity=%s",
						scope, definition.getId(), penalty, result.isSuccess(), ref);
			}
		}
	}

	private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope, @Nonnull PlayerRef playerRef) {
		return switch (scope) {
			case Character -> ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
			case Account -> playerRef.getUuid().toString();
			case Guild -> {
				Guild guild = ExamplePlugin.get().getGuildManager().getGuildForMember(playerRef.getUuid());
				yield guild == null ? null : guild.getId().toString();
			}
			case Global -> "global";
			case Custom -> null;
		};
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
