package org.pixelbays.rpg.ability.system;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Global cooldown (GCD) tracker for abilities.
 *
 * GCD is a flat cooldown applied between abilities, separate from per-ability cooldowns.
 */
public class GlobalCoolDown {

	private final ConcurrentMap<Ref<EntityStore>, Long> nextAllowedEpochMs = new ConcurrentHashMap<>();

	/**
	 * Check if the entity can trigger an ability, and if allowed, consume the GCD.
	 */
	@Nonnull
	public GcdResult tryConsume(@Nonnull Ref<EntityStore> entityRef,
			@Nonnull Store<EntityStore> store,
			@Nonnull ClassAbilityDefinition abilityDef) {
		int cooldownMs = resolveCooldownMs(abilityDef);
		RpgLogging.debugDeveloper("[GCD] tryConsume ability=%s cooldownMs=%s entity=%s",
				abilityDef.getId(),
				cooldownMs,
				entityRef);
		if (cooldownMs <= 0) {
			RpgLogging.debugDeveloper("[GCD] bypassed (cooldownMs<=0) ability=%s entity=%s",
					abilityDef.getId(),
					entityRef);
			return GcdResult.allowed(0);
		}

		long now = System.currentTimeMillis();
		Long nextAllowed = nextAllowedEpochMs.get(entityRef);
		if (nextAllowed != null && nextAllowed > now) {
			long remaining = nextAllowed - now;
			RpgLogging.debugDeveloper("[GCD] blocked remainingMs=%s ability=%s entity=%s",
					remaining,
					abilityDef.getId(),
					entityRef);
			return GcdResult.blocked(remaining);
		}

		long nextTime = now + cooldownMs;
		nextAllowedEpochMs.put(entityRef, nextTime);
		RpgLogging.debugDeveloper("[GCD] applied nextAllowed=%s ability=%s entity=%s",
				nextTime,
				abilityDef.getId(),
				entityRef);
		return GcdResult.allowed(cooldownMs);
	}

	private int resolveCooldownMs(@Nonnull ClassAbilityDefinition abilityDef) {
		float abilityOverride = abilityDef.getGlobalCooldown();
		if (abilityOverride > 0) {
			RpgLogging.debugDeveloper("[GCD] ability override=%s ability=%s",
					abilityOverride,
					abilityDef.getId());
			return Math.round(abilityOverride);
		}

		RpgModConfig config = resolveConfig();
		if (config == null) {
			RpgLogging.debugDeveloper("[GCD] config not found, defaulting to 0. ability=%s",
					abilityDef.getId());
			return 0;
		}

		int base = config.getBaseGlobalCooldown();
		int categoryOverride = resolveCategoryOverrideMs(config, abilityDef);
		RpgLogging.debugDeveloper("[GCD] resolved base=%s categoryOverride=%s ability=%s",
				base,
				categoryOverride,
				abilityDef.getId());
		return categoryOverride > 0 ? categoryOverride : base;
	}

	private int resolveCategoryOverrideMs(@Nonnull RpgModConfig config,
			@Nonnull ClassAbilityDefinition abilityDef) {
		if (config.getGlobalCooldownCategories() == null
				|| config.getGlobalCooldownCategories().isEmpty()) {
			return 0;
		}

		String[] categories = abilityDef.getGlobalCooldownCategories();
		if (categories == null || categories.length == 0) {
			RpgLogging.debugDeveloper("[GCD] no categories on ability=%s",
					abilityDef.getId());
			return 0;
		}

		int best = 0;
		for (String category : categories) {
			if (category == null || category.isEmpty()) {
				continue;
			}

			Integer value = getCategoryValue(config, category);
			RpgLogging.debugDeveloper("[GCD] category lookup category=%s value=%s ability=%s",
					category,
					value,
					abilityDef.getId());
			if (value != null && value > best) {
				best = value;
			}
		}

		RpgLogging.debugDeveloper("[GCD] category best=%s ability=%s",
				best,
				abilityDef.getId());

		return best;
	}

	@Nullable
	private Integer getCategoryValue(@Nonnull RpgModConfig config, @Nonnull String category) {
		if (config.getGlobalCooldownCategories().containsKey(category)) {
			return config.getGlobalCooldownCategories().getInt(category);
		}

		for (var entry : config.getGlobalCooldownCategories().object2IntEntrySet()) {
			String key = entry.getKey();
			if (key != null && key.equalsIgnoreCase(category)) {
				return entry.getIntValue();
			}
		}

		return null;
	}

	@Nullable
	private RpgModConfig resolveConfig() {
		var assetMap = RpgModConfig.getAssetMap();
		if (assetMap == null) {
			RpgLogging.debugDeveloper("[GCD] RpgModConfig asset map is null");
			return null;
		}

		RpgModConfig config = assetMap.getAsset("Default");
		if (config != null) {
			RpgLogging.debugDeveloper("[GCD] using config id=Default");
			return config;
		}

		if (assetMap.getAssetMap().isEmpty()) {
			RpgLogging.debugDeveloper("[GCD] config asset map empty");
			return null;
		}

		RpgModConfig fallback = assetMap.getAssetMap().values().iterator().next();
		RpgLogging.debugDeveloper("[GCD] using fallback config id=%s", fallback != null ? fallback.getId() : "null");
		return fallback;
	}

	/**
	 * Result of a GCD check.
	 */
	public record GcdResult(boolean allowed, long remainingMs, int appliedMs) {
		public static GcdResult allowed(int appliedMs) {
			return new GcdResult(true, 0, appliedMs);
		}

		public static GcdResult blocked(long remainingMs) {
			return new GcdResult(false, remainingMs, 0);
		}
	}
}
