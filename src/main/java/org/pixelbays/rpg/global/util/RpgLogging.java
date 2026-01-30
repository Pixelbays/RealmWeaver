package org.pixelbays.rpg.global.util;

import javax.annotation.Nullable;

import org.pixelbays.rpg.global.config.RpgModConfig;

import java.util.logging.Level;

public final class RpgLogging {

	private RpgLogging() {
	}

	public static void info(String message, Object... args) {
		println(formatMessage(message, args));
	}

	public static void warn(String message, Object... args) {
		println(formatMessage(message, args));
	}

	public static void error(String message, Object... args) {
		println(formatMessage(message, args));
	}

	public static void error(Throwable cause, String message, Object... args) {
		println(formatMessage(message, args) + " - " + cause);
	}

	public static void debug(String message, Object... args) {
		Level level = getDebugLevel();
		if (level == null) {
			return;
		}

		println(formatMessage(message, args));
	}

	public static void debugDeveloper(String message, Object... args) {
		if (!isDeveloperDebugEnabled()) {
			return;
		}

		println(formatMessage(message, args));
	}

	public static void player(String message, Object... args) {
		if (!isPlayerLoggingEnabled()) {
			return;
		}

		println(formatMessage(message, args));
	}

	private static void println(String message) {
		System.out.println(message);
	}

	private static String formatMessage(String message, Object... args) {
		if (args == null || args.length == 0) {
			return message;
		}
		return String.format(message, args);
	}

	public static boolean isDebugEnabled() {
		return getDebugLevel() != null;
	}

	public static boolean isDeveloperDebugEnabled() {
		RpgModConfig config = resolveConfig();
		return config != null && config.getDebuggingMode() == RpgModConfig.DebuggingMode.DeveloperDontUse;
	}

	public static boolean isPlayerLoggingEnabled() {
		RpgModConfig config = resolveConfig();
		return config != null && config.isPlayerLogging();
	}

	@Nullable
	private static Level getDebugLevel() {
		RpgModConfig config = resolveConfig();
		if (config == null) {
			return null;
		}

		RpgModConfig.DebuggingMode mode = config.getDebuggingMode();
		if (mode == null || mode == RpgModConfig.DebuggingMode.None) {
			return null;
		}

		if (mode == RpgModConfig.DebuggingMode.DeveloperDontUse) {
			return Level.FINEST;
		}

		return Level.FINE;
	}

	@Nullable
	private static RpgModConfig resolveConfig() {
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

	/*
	 * Usage examples:
	 *
	 * // Basic logs
	 * RpgLogging.info("Loaded %s classes", classCount);
	 * RpgLogging.warn("Missing config for %s", configId);
	 * RpgLogging.error("Failed to save profile %s", profileId);
	 *
	 * // Error with cause
	 * try {
	 *     saveProfile(profile);
	 * } catch (Exception ex) {
	 *     RpgLogging.error(ex, "Profile save failed for %s", profile.getId());
	 * }
	 *
	 * // Debug logging (controlled by DebuggingMode in RpgModConfig)
	 * RpgLogging.debug("XP grant: base=%s final=%s source=%s", baseExp, expToGrant, source);
	 *
	 * // Player-specific logging (controlled by PlayerLogging in RpgModConfig)
	 * RpgLogging.player("Player %s gained %s XP", playerName, expToGrant);
	 */
}
