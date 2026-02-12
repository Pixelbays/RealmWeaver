package org.pixelbays.rpg.global.system;

import java.util.concurrent.ThreadLocalRandom;

public final class RNGSystem {

	private static final int DEFAULT_MIN = 1;
	private static final int DEFAULT_MAX = 20;

	private RNGSystem() {
	}

	public static int roll() {
		return roll(DEFAULT_MIN, DEFAULT_MAX);
	}

	public static int roll(int minInclusive, int maxInclusive) {
		if (minInclusive > maxInclusive) {
			throw new IllegalArgumentException("minInclusive must be <= maxInclusive");
		}
		return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
	}
}
