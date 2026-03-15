package org.pixelbays.rpg.item.config;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.item.metadata.RandomizedEquipmentData;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

@SuppressWarnings({ "deprecation", "FieldHidesSuperclassField" })
public class RandomizedEquipmentDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, RandomizedEquipmentDefinition>> {

	public static final AssetBuilderCodec<String, RandomizedEquipmentDefinition> CODEC = AssetBuilderCodec.builder(
			RandomizedEquipmentDefinition.class,
			RandomizedEquipmentDefinition::new,
			Codec.STRING,
			(i, s) -> i.id = s,
			i -> i.id,
			(asset, data) -> asset.data = data,
			asset -> asset.data)
			.append(new KeyedCodec<>("Priority", Codec.INTEGER, false, true),
					(i, s) -> i.priority = s, i -> i.priority)
			.add()
			.append(new KeyedCodec<>("ItemIds", new ArrayCodec<>(Codec.STRING, String[]::new), false, true),
					(i, s) -> i.itemIds = s, i -> i.itemIds)
			.add()
			.append(new KeyedCodec<>("MatchTags", new ArrayCodec<>(Codec.STRING, String[]::new), false, true),
					(i, s) -> i.matchTags = s, i -> i.matchTags)
			.add()
			.append(new KeyedCodec<>("StatRolls", new ArrayCodec<>(StatRollDefinition.CODEC, StatRollDefinition[]::new), false, true),
					(i, s) -> i.statRolls = s, i -> i.statRolls)
			.add()
			.append(new KeyedCodec<>("RequiredClassIds", new ArrayCodec<>(Codec.STRING, String[]::new), false, true),
					(i, s) -> i.requiredClassIds = s, i -> i.requiredClassIds)
			.add()
			.append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false, true),
					(i, s) -> i.requiredClassLevel = s, i -> i.requiredClassLevel)
			.add()
			.build();

	private static DefaultAssetMap<String, RandomizedEquipmentDefinition> ASSET_MAP;

	private String id;
	private int priority;
	private String[] itemIds;
	private String[] matchTags;
	private StatRollDefinition[] statRolls;
	private String[] requiredClassIds;
	private int requiredClassLevel;
	private AssetExtraInfo.Data data;

	public RandomizedEquipmentDefinition() {
		this.id = "";
		this.priority = 0;
		this.itemIds = new String[0];
		this.matchTags = new String[0];
		this.statRolls = new StatRollDefinition[0];
		this.requiredClassIds = new String[0];
		this.requiredClassLevel = 1;
	}

	public static DefaultAssetMap<String, RandomizedEquipmentDefinition> getAssetMap() {
		if (ASSET_MAP == null) {
			AssetStore<String, RandomizedEquipmentDefinition, DefaultAssetMap<String, RandomizedEquipmentDefinition>> assetStore = AssetRegistry
					.getAssetStore(RandomizedEquipmentDefinition.class);
			if (assetStore == null) {
				return null;
			}
			ASSET_MAP = assetStore.getAssetMap();
		}
		return ASSET_MAP;
	}

	@Override
	public String getId() {
		return id;
	}

	public int getPriority() {
		return priority;
	}

	@Nonnull
	public String[] getItemIds() {
		return itemIds == null ? new String[0] : itemIds;
	}

	@Nonnull
	public String[] getMatchTags() {
		return matchTags == null ? new String[0] : matchTags;
	}

	@Nonnull
	public StatRollDefinition[] getStatRolls() {
		return statRolls == null ? new StatRollDefinition[0] : statRolls;
	}

	@Nonnull
	public String[] getRequiredClassIds() {
		return requiredClassIds == null ? new String[0] : requiredClassIds;
	}

	public int getRequiredClassLevel() {
		return Math.max(1, requiredClassLevel);
	}

	public static class StatRollDefinition {

		public static final BuilderCodec<StatRollDefinition> CODEC = BuilderCodec.builder(
				StatRollDefinition.class, StatRollDefinition::new)
				.append(new KeyedCodec<>("StatId", Codec.STRING, false, true),
						(i, s) -> i.statId = s, i -> i.statId)
				.addValidatorLate(() -> EntityStatType.VALIDATOR_CACHE.getValidator().late())
				.add()
				.append(new KeyedCodec<>("ModifierKind", new EnumCodec<>(RandomizedEquipmentData.ModifierKind.class), false, true),
						(i, s) -> i.modifierKind = s, i -> i.modifierKind)
				.add()
				.append(new KeyedCodec<>("MinValue", Codec.FLOAT, false, true),
						(i, s) -> i.minValue = s, i -> i.minValue)
				.add()
				.append(new KeyedCodec<>("MaxValue", Codec.FLOAT, false, true),
						(i, s) -> i.maxValue = s, i -> i.maxValue)
				.add()
				.build();

		private String statId;
		private RandomizedEquipmentData.ModifierKind modifierKind;
		private float minValue;
		private float maxValue;

		public StatRollDefinition() {
			this.statId = "";
			this.modifierKind = RandomizedEquipmentData.ModifierKind.Additive;
			this.minValue = 0.0f;
			this.maxValue = 0.0f;
		}

		public String getStatId() {
			return statId == null ? "" : statId;
		}

		public RandomizedEquipmentData.ModifierKind getModifierKind() {
			return modifierKind == null ? RandomizedEquipmentData.ModifierKind.Additive : modifierKind;
		}

		public float getMinValue() {
			return minValue;
		}

		public float getMaxValue() {
			return maxValue < minValue ? minValue : maxValue;
		}
	}
}
