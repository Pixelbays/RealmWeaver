package org.pixelbays.rpg.item.system;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.item.config.RandomizedEquipmentDefinition;
import org.pixelbays.rpg.item.config.settings.ItemModSettings;
import org.pixelbays.rpg.item.metadata.RandomizedEquipmentData;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.ints.IntSet;

@SuppressWarnings("null")
public class RandomizedEquipmentManager {

	public static final String METADATA_KEY = "RpgRandomizedEquipment";
	private static final String BASE_LEVEL_SYSTEM_ID = "Base_Character_Level";

	private final ThreadLocal<Boolean> suppressEvents = ThreadLocal.withInitial(() -> false);
	private final Map<Ref<EntityStore>, AppliedEquipmentModifiers> equipmentModifiersCache = new HashMap<>();

	public void register(@Nonnull EventRegistry eventRegistry) {
		eventRegistry.registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
		eventRegistry.registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
		eventRegistry.registerGlobal(LevelUpEvent.class, this::onLevelUp);
	}

	@Nonnull
	public EntityEventSystem<EntityStore, InventoryChangeEvent> createInventoryChangeSystem() {
		return new EntityEventSystem<>(InventoryChangeEvent.class) {
			@Override
			public void handle(int index,
					@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
					@Nonnull Store<EntityStore> store,
					@Nonnull CommandBuffer<EntityStore> commandBuffer,
					@Nonnull InventoryChangeEvent event) {
				if (Boolean.TRUE.equals(suppressEvents.get())) {
					return;
				}

				Player player = archetypeChunk.getComponent(index, Player.getComponentType());
				if (player == null) {
					return;
				}

				Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
				if (ref == null || !ref.isValid()) {
					return;
				}

				onInventoryChange(player, ref, store, event.getItemContainer());
			}

			@Nonnull
			@Override
			public Query<EntityStore> getQuery() {
				return Query.any();
			}
		};
	}

	@Nullable
	public static RandomizedEquipmentData getEquipmentData(@Nullable ItemStack itemStack) {
		if (itemStack == null || ItemStack.isEmpty(itemStack)) {
			return null;
		}
		return itemStack.getFromMetadataOrNull(METADATA_KEY, RandomizedEquipmentData.CODEC);
	}

	private void onPlayerReady(@Nonnull PlayerReadyEvent event) {
		Player player = event.getPlayer();

		Ref<EntityStore> ref = player.getReference();
		if (ref == null || !ref.isValid()) {
			return;
		}

		Store<EntityStore> store = ref.getStore();
		initializeInventory(player, ref, store);
		recalculateEquipmentBonuses(ref, store, player.getInventory());
	}

	private void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
		Ref<EntityStore> ref = event.getPlayerRef() == null ? null : event.getPlayerRef().getReference();
		if (ref == null) {
			return;
		}

		AppliedEquipmentModifiers oldModifiers = equipmentModifiersCache.remove(ref);
		if (oldModifiers == null || !ref.isValid()) {
			return;
		}

		removeModifiers(ref, oldModifiers, ref.getStore(), "rpg_equipment");
	}

	private void onLevelUp(@Nonnull LevelUpEvent event) {
		Ref<EntityStore> ref = event.playerRef();
		if (ref == null || !ref.isValid()) {
			return;
		}

		Store<EntityStore> store = ref.getStore();
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		recalculateEquipmentBonuses(ref, store, player.getInventory());
	}

	private void onInventoryChange(@Nonnull Player player,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nullable ItemContainer container) {
		if (container != null) {
			suppressEvents.set(true);
			try {
				initializeContainer(player, ref, store, container);
			} finally {
				suppressEvents.set(false);
			}
		}

		recalculateEquipmentBonuses(ref, store, player.getInventory());
	}

	private void initializeInventory(@Nonnull Player player,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store) {
		suppressEvents.set(true);
		try {
			Inventory inventory = player.getInventory();
			initializeContainer(player, ref, store, inventory.getArmor());
			initializeContainer(player, ref, store, inventory.getHotbar());
			initializeContainer(player, ref, store, inventory.getUtility());
			initializeContainer(player, ref, store, inventory.getTools());
			initializeContainer(player, ref, store, inventory.getStorage());
			initializeContainer(player, ref, store, inventory.getBackpack());
		} finally {
			suppressEvents.set(false);
		}
	}

	private void initializeContainer(@Nonnull Player player,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nullable ItemContainer container) {
		if (container == null) {
			return;
		}

		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack itemStack = container.getItemStack(slot);
			if (ItemStack.isEmpty(itemStack)) {
				continue;
			}

			ItemStack updated = initializeItemStack(itemStack, player, ref, store);
			if (updated != null && !updated.equals(itemStack)) {
				container.setItemStackForSlot(slot, updated);
			}
		}
	}

	@Nullable
	private ItemStack initializeItemStack(@Nullable ItemStack itemStack,
			@Nonnull Player player,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store) {
		if (itemStack == null || ItemStack.isEmpty(itemStack) || getEquipmentData(itemStack) != null) {
			return itemStack;
		}

		ItemModSettings settings = getItemSettings();
		if (!settings.isEnabled()) {
			return itemStack;
		}

		Item item = itemStack.getItem();
		if (item == null || item == Item.UNKNOWN) {
			return itemStack;
		}

		RandomizedEquipmentDefinition definition = findDefinition(item);
		if (definition == null || definition.getStatRolls().length == 0) {
			return itemStack;
		}

		RandomizedEquipmentData.RolledStat[] rolledStats = rollStats(definition);
		RandomizedEquipmentData data = new RandomizedEquipmentData(
				definition.getId(),
				itemHasTag(item, settings.getLevelScalingTag()),
				resolvePlayerLevel(ref, store),
				definition.getRequiredClassIds(),
				definition.getRequiredClassLevel(),
				rolledStats);

		RpgLogging.debugDeveloper("[RandomizedEquipment] Rolled %s for entity %s", definition.getId(), ref);
		return itemStack.withMetadata(METADATA_KEY, RandomizedEquipmentData.CODEC, data);
	}

	private RandomizedEquipmentData.RolledStat[] rollStats(@Nonnull RandomizedEquipmentDefinition definition) {
		RandomizedEquipmentDefinition.StatRollDefinition[] statRolls = definition.getStatRolls();
		RandomizedEquipmentData.RolledStat[] rolled = new RandomizedEquipmentData.RolledStat[statRolls.length];

		for (int i = 0; i < statRolls.length; i++) {
			RandomizedEquipmentDefinition.StatRollDefinition roll = statRolls[i];
			float min = roll.getMinValue();
			float max = roll.getMaxValue();
			float value = min == max ? min : (float) ThreadLocalRandom.current().nextDouble(min, max + 0.000001d);
			rolled[i] = new RandomizedEquipmentData.RolledStat(roll.getStatId(), roll.getModifierKind(), value);
		}

		return rolled;
	}

	@Nullable
	private RandomizedEquipmentDefinition findDefinition(@Nonnull Item item) {
		var assetMap = RandomizedEquipmentDefinition.getAssetMap();
		if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
			return null;
		}

		RandomizedEquipmentDefinition bestMatch = null;
		int bestScore = Integer.MIN_VALUE;

		for (RandomizedEquipmentDefinition definition : assetMap.getAssetMap().values()) {
			if (definition == null) {
				continue;
			}

			int matchScore = matchScore(item, definition);
			if (matchScore == Integer.MIN_VALUE) {
				continue;
			}

			if (bestMatch == null || matchScore > bestScore) {
				bestMatch = definition;
				bestScore = matchScore;
			}
		}

		return bestMatch;
	}

	private int matchScore(@Nonnull Item item, @Nonnull RandomizedEquipmentDefinition definition) {
		boolean idMatched = false;
		for (String itemId : definition.getItemIds()) {
			if (itemId != null && itemId.equalsIgnoreCase(item.getId())) {
				idMatched = true;
				break;
			}
		}

		int tagMatches = 0;
		for (String tag : definition.getMatchTags()) {
			if (tag != null && !tag.isEmpty() && itemHasTag(item, tag)) {
				tagMatches++;
			}
		}

		if (!idMatched && tagMatches == 0) {
			return Integer.MIN_VALUE;
		}

		return (definition.getPriority() * 1000) + (idMatched ? 100 : 0) + tagMatches;
	}

	private boolean itemHasTag(@Nonnull Item item, @Nonnull String tag) {
		if (tag.isEmpty() || item.getData() == null) {
			return false;
		}

		IntSet expandedTagIndexes = item.getData().getExpandedTagIndexes();
		if (expandedTagIndexes == null || expandedTagIndexes.isEmpty()) {
			return false;
		}

		int tagIndex = AssetRegistry.getTagIndex(tag);
		return tagIndex != Integer.MIN_VALUE && expandedTagIndexes.contains(tagIndex);
	}

	private void recalculateEquipmentBonuses(@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull Inventory inventory) {
		ItemModSettings settings = getItemSettings();
		AppliedEquipmentModifiers newModifiers = new AppliedEquipmentModifiers();

		if (settings.isEnabled()) {
			int playerLevel = resolvePlayerLevel(ref, store);
			accumulateContainer(newModifiers, inventory.getArmor(), ref, store, playerLevel, settings);
			accumulateContainer(newModifiers, inventory.getHotbar(), ref, store, playerLevel, settings);
			accumulateContainer(newModifiers, inventory.getUtility(), ref, store, playerLevel, settings);
			accumulateContainer(newModifiers, inventory.getTools(), ref, store, playerLevel, settings);
		}

		AppliedEquipmentModifiers oldModifiers = equipmentModifiersCache.get(ref);
		updateModifiers(ref, newModifiers, oldModifiers, store, "rpg_equipment");

		if (newModifiers.isEmpty()) {
			equipmentModifiersCache.remove(ref);
		} else {
			equipmentModifiersCache.put(ref, newModifiers);
		}
	}

	private void accumulateContainer(@Nonnull AppliedEquipmentModifiers modifiers,
			@Nullable ItemContainer container,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			int playerLevel,
			@Nonnull ItemModSettings settings) {
		if (container == null) {
			return;
		}

		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack itemStack = container.getItemStack(slot);
			RandomizedEquipmentData data = getEquipmentData(itemStack);
			if (data == null || !meetsClassRequirement(ref, store, data)) {
				continue;
			}

			for (RandomizedEquipmentData.RolledStat rolledStat : data.getRolledStats()) {
				if (rolledStat == null || rolledStat.getStatId().isEmpty()) {
					continue;
				}

				float value = rolledStat.getBaseValue();
				if (data.scalesWithPlayerLevel()) {
					value = RandomizedEquipmentMath.scaleValue(value, playerLevel, settings.getScalePerLevelPercent());
				}

				if (rolledStat.getModifierKind() == RandomizedEquipmentData.ModifierKind.Multiplicative) {
					modifiers.multiplicativeModifiers.merge(rolledStat.getStatId(), value, Float::sum);
				} else {
					modifiers.additiveModifiers.merge(rolledStat.getStatId(), value, Float::sum);
				}
			}
		}
	}

	public boolean meetsClassRequirement(@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull RandomizedEquipmentData data) {
		String[] requiredClassIds = data.getRequiredClassIds();
		if (requiredClassIds.length == 0) {
			return true;
		}

		ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
		if (classComponent == null) {
			return false;
		}

		for (String classId : requiredClassIds) {
			if (classId == null || classId.isEmpty() || !classComponent.hasLearnedClass(classId)) {
				continue;
			}

			if (data.getRequiredClassLevel() <= 1) {
				return true;
			}

			ClassDefinition classDefinition = ClassDefinition.getAssetMap().getAsset(classId);
			if (classDefinition == null) {
				continue;
			}

			String levelSystemId = classDefinition.usesCharacterLevel() ? BASE_LEVEL_SYSTEM_ID : classDefinition.getLevelSystemId();
			LevelProgressionComponent levelComponent = store.getComponent(ref, LevelProgressionComponent.getComponentType());
			if (levelComponent == null || levelSystemId == null || levelSystemId.isEmpty()) {
				continue;
			}

			LevelProgressionComponent.LevelSystemData levelSystemData = levelComponent.getSystem(levelSystemId);
			int currentLevel = levelSystemData != null ? levelSystemData.getCurrentLevel() : 1;
			if (currentLevel >= data.getRequiredClassLevel()) {
				return true;
			}
		}

		return false;
	}

	private int resolvePlayerLevel(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		LevelProgressionComponent levelComponent = store.getComponent(ref, LevelProgressionComponent.getComponentType());
		if (levelComponent == null) {
			return 1;
		}

		LevelProgressionComponent.LevelSystemData baseLevel = levelComponent.getSystem(BASE_LEVEL_SYSTEM_ID);
		if (baseLevel != null && baseLevel.getCurrentLevel() > 0) {
			return baseLevel.getCurrentLevel();
		}

		int highestLevel = 1;
		for (LevelProgressionComponent.LevelSystemData data : levelComponent.getAllSystems().values()) {
			if (data != null) {
				highestLevel = Math.max(highestLevel, data.getCurrentLevel());
			}
		}

		return highestLevel;
	}

	@Nonnull
	private ItemModSettings getItemSettings() {
		var assetMap = RpgModConfig.getAssetMap();
		RpgModConfig config = assetMap == null ? null : assetMap.getAsset("default");
		return config == null ? new ItemModSettings() : config.getItemSettings();
	}

	private void updateModifiers(@Nonnull Ref<EntityStore> entityRef,
			@Nonnull AppliedEquipmentModifiers newModifiers,
			@Nullable AppliedEquipmentModifiers oldModifiers,
			@Nonnull Store<EntityStore> store,
			@Nonnull String modifierKeyPrefix) {
		EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
		if (statMap == null) {
			return;
		}

		AppliedEquipmentModifiers oldSafe = oldModifiers == null ? new AppliedEquipmentModifiers() : oldModifiers;
		updateModifierMap(statMap, newModifiers.additiveModifiers, oldSafe.additiveModifiers,
				modifierKeyPrefix + "_additive_", true);
		updateModifierMap(statMap, newModifiers.multiplicativeModifiers, oldSafe.multiplicativeModifiers,
				modifierKeyPrefix + "_multiplicative_", false);
	}

	private void updateModifierMap(@Nonnull EntityStatMap statMap,
			@Nonnull Map<String, Float> newMap,
			@Nonnull Map<String, Float> oldMap,
			@Nonnull String keyPrefix,
			boolean additive) {
		Set<String> keys = new HashSet<>();
		keys.addAll(newMap.keySet());
		keys.addAll(oldMap.keySet());

		for (String statId : keys) {
			Float newValue = newMap.get(statId);
			Float oldValue = oldMap.get(statId);

			boolean newPresent = newValue != null && newValue != 0f;
			boolean oldPresent = oldValue != null && oldValue != 0f;
			if (!newPresent && !oldPresent) {
				continue;
			}

			int statIndex = EntityStatType.getAssetMap().getIndex(statId);
			if (statIndex == Integer.MIN_VALUE) {
				continue;
			}

			String modifierKey = keyPrefix + statId;
			if (!newPresent && oldPresent) {
				statMap.removeModifier(statIndex, modifierKey);
				continue;
			}

			if (newPresent && oldPresent && Objects.equals(roundFloat(newValue), roundFloat(oldValue))) {
				continue;
			}

			StaticModifier staticModifier;
			if (additive) {
				staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
						StaticModifier.CalculationType.ADDITIVE, newValue);
			} else {
				float multiplier = 1.0f + (newValue / 100.0f);
				staticModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
						StaticModifier.CalculationType.MULTIPLICATIVE, multiplier);
			}

			statMap.putModifier(statIndex, modifierKey, staticModifier);
		}
	}

	private float roundFloat(float value) {
		return Math.round(value * 1000.0f) / 1000.0f;
	}

	private void removeModifiers(@Nonnull Ref<EntityStore> entityRef,
			@Nonnull AppliedEquipmentModifiers modifiers,
			@Nonnull Store<EntityStore> store,
			@Nonnull String modifierKeyPrefix) {
		EntityStatMap statMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
		if (statMap == null) {
			return;
		}

		for (Map.Entry<String, Float> entry : modifiers.additiveModifiers.entrySet()) {
			int statIndex = EntityStatType.getAssetMap().getIndex(entry.getKey());
			if (statIndex != Integer.MIN_VALUE) {
				EntityStatValue statValue = statMap.get(statIndex);
				if (statValue != null) {
					statMap.removeModifier(statIndex, modifierKeyPrefix + "_additive_" + entry.getKey());
				}
			}
		}

		for (Map.Entry<String, Float> entry : modifiers.multiplicativeModifiers.entrySet()) {
			int statIndex = EntityStatType.getAssetMap().getIndex(entry.getKey());
			if (statIndex != Integer.MIN_VALUE) {
				EntityStatValue statValue = statMap.get(statIndex);
				if (statValue != null) {
					statMap.removeModifier(statIndex, modifierKeyPrefix + "_multiplicative_" + entry.getKey());
				}
			}
		}
	}

	private static class AppliedEquipmentModifiers {
		private final Map<String, Float> additiveModifiers = new HashMap<>();
		private final Map<String, Float> multiplicativeModifiers = new HashMap<>();

		private boolean isEmpty() {
			return additiveModifiers.isEmpty() && multiplicativeModifiers.isEmpty();
		}
	}
}
