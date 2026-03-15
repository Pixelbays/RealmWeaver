package org.pixelbays.rpg.item.metadata;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

@SuppressWarnings("deprecation")
public class RandomizedEquipmentData {

	public static final BuilderCodec<RandomizedEquipmentData> CODEC = BuilderCodec.builder(
			RandomizedEquipmentData.class, RandomizedEquipmentData::new)
			.append(new KeyedCodec<>("DefinitionId", Codec.STRING, false, true),
					(i, s) -> i.definitionId = s, i -> i.definitionId)
			.add()
			.append(new KeyedCodec<>("ScalesWithPlayerLevel", Codec.BOOLEAN, false, true),
					(i, s) -> i.scalesWithPlayerLevel = s, i -> i.scalesWithPlayerLevel)
			.add()
			.append(new KeyedCodec<>("GeneratedForLevel", Codec.INTEGER, false, true),
					(i, s) -> i.generatedForLevel = s, i -> i.generatedForLevel)
			.add()
			.append(new KeyedCodec<>("RequiredClassIds", new ArrayCodec<>(Codec.STRING, String[]::new), false, true),
					(i, s) -> i.requiredClassIds = s, i -> i.requiredClassIds)
			.add()
			.append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false, true),
					(i, s) -> i.requiredClassLevel = s, i -> i.requiredClassLevel)
			.add()
			.append(new KeyedCodec<>("RolledStats", new ArrayCodec<>(RolledStat.CODEC, RolledStat[]::new), false, true),
					(i, s) -> i.rolledStats = s, i -> i.rolledStats)
			.add()
			.build();

	private String definitionId;
	private boolean scalesWithPlayerLevel;
	private int generatedForLevel;
	private String[] requiredClassIds;
	private int requiredClassLevel;
	private RolledStat[] rolledStats;

	public RandomizedEquipmentData() {
		this.definitionId = "";
		this.scalesWithPlayerLevel = false;
		this.generatedForLevel = 1;
		this.requiredClassIds = new String[0];
		this.requiredClassLevel = 1;
		this.rolledStats = new RolledStat[0];
	}

	public RandomizedEquipmentData(@Nonnull String definitionId,
			boolean scalesWithPlayerLevel,
			int generatedForLevel,
			@Nonnull String[] requiredClassIds,
			int requiredClassLevel,
			@Nonnull RolledStat[] rolledStats) {
		this.definitionId = definitionId;
		this.scalesWithPlayerLevel = scalesWithPlayerLevel;
		this.generatedForLevel = generatedForLevel;
		this.requiredClassIds = requiredClassIds;
		this.requiredClassLevel = requiredClassLevel;
		this.rolledStats = rolledStats;
	}

	public String getDefinitionId() {
		return definitionId == null ? "" : definitionId;
	}

	public boolean scalesWithPlayerLevel() {
		return scalesWithPlayerLevel;
	}

	public int getGeneratedForLevel() {
		return Math.max(1, generatedForLevel);
	}

	@Nonnull
	public String[] getRequiredClassIds() {
		return requiredClassIds == null ? new String[0] : requiredClassIds;
	}

	public int getRequiredClassLevel() {
		return Math.max(1, requiredClassLevel);
	}

	@Nonnull
	public RolledStat[] getRolledStats() {
		return rolledStats == null ? new RolledStat[0] : rolledStats;
	}

	public boolean hasClassRequirement() {
		return getRequiredClassIds().length > 0;
	}

	public enum ModifierKind {
		Additive,
		Multiplicative
	}

	public static class RolledStat {

		public static final BuilderCodec<RolledStat> CODEC = BuilderCodec.builder(RolledStat.class, RolledStat::new)
				.append(new KeyedCodec<>("StatId", Codec.STRING, false, true),
						(i, s) -> i.statId = s, i -> i.statId)
				.add()
				.append(new KeyedCodec<>("ModifierKind", new EnumCodec<>(ModifierKind.class), false, true),
						(i, s) -> i.modifierKind = s, i -> i.modifierKind)
				.add()
				.append(new KeyedCodec<>("BaseValue", Codec.FLOAT, false, true),
						(i, s) -> i.baseValue = s, i -> i.baseValue)
				.add()
				.build();

		private String statId;
		private ModifierKind modifierKind;
		private float baseValue;

		public RolledStat() {
			this.statId = "";
			this.modifierKind = ModifierKind.Additive;
			this.baseValue = 0.0f;
		}

		public RolledStat(@Nonnull String statId, @Nonnull ModifierKind modifierKind, float baseValue) {
			this.statId = statId;
			this.modifierKind = modifierKind;
			this.baseValue = baseValue;
		}

		public String getStatId() {
			return statId == null ? "" : statId;
		}

		public ModifierKind getModifierKind() {
			return modifierKind == null ? ModifierKind.Additive : modifierKind;
		}

		public float getBaseValue() {
			return baseValue;
		}
	}
}
