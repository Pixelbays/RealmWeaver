package org.pixelbays.rpg.item.system;

public final class RandomizedEquipmentMath {

	private RandomizedEquipmentMath() {
	}

	public static float scaleValue(float baseValue, int playerLevel, double scalePerLevelPercent) {
		int normalizedLevel = Math.max(1, playerLevel);
		if (normalizedLevel <= 1 || scalePerLevelPercent == 0.0d) {
			return baseValue;
		}

		double multiplier = 1.0d + ((normalizedLevel - 1) * (scalePerLevelPercent / 100.0d));
		if (multiplier < 0.0d) {
			multiplier = 0.0d;
		}
		return (float) (baseValue * multiplier);
	}
}
