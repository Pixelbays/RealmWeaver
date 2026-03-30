package org.pixelbays.rpg.item.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RandomizedEquipmentMathTest {

	@Test
	void scaleValue_keepsBaseValueAtLevelOne() {
		assertEquals(10.0f, RandomizedEquipmentMath.scaleValue(10.0f, 1, 3.0d), 0.0001f);
	}

	@Test
	void scaleValue_appliesPercentPerLevelAboveOne() {
		assertEquals(12.7f, RandomizedEquipmentMath.scaleValue(10.0f, 10, 3.0d), 0.0001f);
	}

	@Test
	void scaleValue_neverDropsBelowZeroMultiplier() {
		assertEquals(0.0f, RandomizedEquipmentMath.scaleValue(10.0f, 10, -20.0d), 0.0001f);
	}
}
