package org.pixelbays.rpg.item.system;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.config.ClassDefinition.EquipmentRestrictions.RestrictMode;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Enforces class-based equipment restrictions.
 * 
 * Modes:
 * - None: no restrictions
 * - Soft: allow equip but suppress class bonus effects (placeholder)
 * - Hard: disallow equip by moving items out of equipment containers
 */
@SuppressWarnings({ "null", "UnusedReturnValue" })
public class EquipmentRestrictions {

	private static final class RestrictionEntry {
		private final ClassDefinition.EquipmentRestrictions restrictions;
		private final RestrictMode mode;

		private RestrictionEntry(@Nonnull ClassDefinition.EquipmentRestrictions restrictions,
				@Nonnull RestrictMode mode) {
			this.restrictions = restrictions;
			this.mode = mode;
		}
	}

	private final ClassManagementSystem classManagementSystem;
	private final ThreadLocal<Boolean> suppressEvents = ThreadLocal.withInitial(() -> false);

	public EquipmentRestrictions(@Nonnull ClassManagementSystem classManagementSystem) {
		this.classManagementSystem = classManagementSystem;
	}

	/**
	 * Register inventory change listener.
	 */
	public void register(@Nonnull EventRegistry eventRegistry) {
		eventRegistry.registerGlobal(LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
	}

	private void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
		if (Boolean.TRUE.equals(suppressEvents.get())) {
			return;
		}

		LivingEntity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}

		Ref<EntityStore> ref = entity.getReference();
		if (ref == null || !ref.isValid()) {
			return;
		}

		Store<EntityStore> store = ref.getStore();
		ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
		if (classComponent == null) {
			return;
		}

		if (classComponent.getLearnedClassIds().isEmpty()) {
			return;
		}

		List<RestrictionEntry> restrictionsList = new ArrayList<>();
		for (String classId : classComponent.getLearnedClassIds()) {
			if (classId == null || classId.isEmpty()) {
				continue;
			}

			ClassDefinition classDef = classManagementSystem.getClassDefinition(classId);
			if (classDef == null || classDef.getEquipmentRestrictions() == null) {
				continue;
			}

			ClassDefinition.EquipmentRestrictions restrictions = classDef.getEquipmentRestrictions();
			RestrictMode mode = restrictions.getRestrictionMode();
			if (mode == RestrictMode.None) {
				continue;
			}

			restrictionsList.add(new RestrictionEntry(restrictions, mode));
		}

		if (restrictionsList.isEmpty()) {
			return;
		}

		Inventory inventory = entity.getInventory();
		ItemContainer container = event.getItemContainer();
		if (container == null || inventory == null) {
			return;
		}

		if (container == inventory.getArmor()) {
			enforceContainer(inventory, container, restrictionsList, true);
		} else if (container == inventory.getHotbar()
				|| container == inventory.getUtility()
				|| container == inventory.getTools()) {
			enforceContainer(inventory, container, restrictionsList, false);
		}
	}

	private void enforceContainer(@Nonnull Inventory inventory,
			@Nonnull ItemContainer container,
			@Nonnull List<RestrictionEntry> restrictionsList,
			boolean armorContainer) {
		short capacity = container.getCapacity();

		for (short slot = 0; slot < capacity; slot++) {
			ItemStack itemStack = container.getItemStack(slot);
			if (ItemStack.isEmpty(itemStack)) {
				continue;
			}

			Item item = itemStack.getItem();
			if (item == null) {
				continue;
			}

			RestrictMode effectiveMode = resolveRestrictMode(item, restrictionsList, armorContainer);
			if (effectiveMode == null) {
				continue;
			}

			if (effectiveMode == RestrictMode.Hard) {
				suppressEvents.set(true);
				try {
					boolean moved = moveRestrictedItem(inventory, container, slot);
					if (!moved) {
						RpgLogging.debugDeveloper(
							"[EquipmentRestrictions] Failed to move restricted item %s from slot %s",
							item.getId(), slot);
					}
				} finally {
					suppressEvents.set(false);
				}
			} else if (effectiveMode == RestrictMode.Soft) {
				// TODO: Disable class bonus effects for this entity while restricted gear is equipped.
				RpgLogging.debugDeveloper(
					"[EquipmentRestrictions] Soft restriction active for item %s (placeholder)",
					item.getId());
			}
		}
	}

	@Nullable
	private RestrictMode resolveRestrictMode(@Nonnull Item item,
			@Nonnull List<RestrictionEntry> restrictionsList,
			boolean armorContainer) {
		RestrictMode result = null;

		for (RestrictionEntry entry : restrictionsList) {
			if (entry == null) {
				continue;
			}

			if (!isRestrictedItem(item, entry.restrictions, armorContainer)) {
				continue;
			}

			if (entry.mode == RestrictMode.Hard) {
				return RestrictMode.Hard;
			}

			if (entry.mode == RestrictMode.Soft) {
				result = RestrictMode.Soft;
			}
		}

		return result;
	}

	private boolean moveRestrictedItem(@Nonnull Inventory inventory, @Nonnull ItemContainer source, short slot) {
		ListTransaction<MoveTransaction<ItemStackTransaction>> transaction =
			source.moveItemStackFromSlot(slot, inventory.getStorage(), inventory.getBackpack());
		return transaction != null && transaction.succeeded();
	}

	private boolean isRestrictedItem(@Nonnull Item item,
			@Nonnull ClassDefinition.EquipmentRestrictions restrictions,
			boolean armorContainer) {
		if (armorContainer || item.getArmor() != null) {
			return !isAllowedByTags(item, restrictions.getAllowedArmorTypes());
		}

		if (item.getWeapon() != null || item.getTool() != null) {
			return !isAllowedByTags(item, restrictions.getAllowedWeaponTypes());
		}

		return false;
	}

	private boolean isAllowedByTags(@Nonnull Item item, @Nullable List<String> allowedTypes) {
		if (allowedTypes == null || allowedTypes.isEmpty()) {
			return true;
		}

		Map<String, String[]> tags = item.getData() != null ? item.getData().getRawTags() : null;
		if (tags == null || tags.isEmpty()) {
			return false;
		}

		String[] typeTags = tags.get("Type");
		String[] familyTags = tags.get("Family");

		for (String allowed : allowedTypes) {
			if (allowed == null || allowed.isEmpty()) {
				continue;
			}

			if (tagListContains(typeTags, allowed) || tagListContains(familyTags, allowed)) {
				return true;
			}
		}

		return false;
	}

	private boolean tagListContains(@Nullable String[] values, @Nonnull String expected) {
		if (values == null || values.length == 0) {
			return false;
		}

		for (String value : values) {
			if (value != null && value.equalsIgnoreCase(expected)) {
				return true;
			}
		}

		return false;
	}
}
