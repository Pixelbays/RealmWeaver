package org.pixelbays.rpg.hud;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HotbarHudServiceModule implements PlayerHudServiceModule {

    @Override
    public void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        HotbarLayoutData layout = resolveLayout(context);
        hud.getHotbarModule().prime(layout.usableSlots, layout.spellSlots);
    }

    @Override
    public void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        HotbarLayoutData layout = resolveLayout(context);
        hud.getHotbarModule().update(layout.usableSlots, layout.spellSlots);
    }

    @Nonnull
    private static HotbarLayoutData resolveLayout(@Nonnull PlayerHudContext context) {
        InventoryComponent.Hotbar hotbarComp = context.getStore().getComponent(context.getRef(), InventoryComponent.Hotbar.getComponentType());
        InventoryComponent.Utility utilityComp = context.getStore().getComponent(context.getRef(), InventoryComponent.Utility.getComponentType());
        ItemContainer hotbar = hotbarComp != null ? hotbarComp.getInventory() : null;
        ItemContainer utility = utilityComp != null ? utilityComp.getInventory() : null;
        int activeSlot = hotbarComp != null ? hotbarComp.getActiveSlot() : 0;
        AbilityControlType controlType = PlayerHudServiceSupport.resolveEffectiveAbilityControlType(context.getActiveClassId());

        List<Integer> reservedAbilitySlots = controlType == AbilityControlType.Hotbar
            ? PlayerHudServiceSupport.resolveConfiguredAbilityHotbarSlots()
            : List.of();
        Set<Integer> reservedLookup = new HashSet<>(reservedAbilitySlots);

        List<HotbarHudModule.SlotViewData> usableSlots = new ArrayList<>();
        ItemStack utilityStack = utility == null ? null : utility.getItemStack((short) 0);
        usableSlots.add(new HotbarHudModule.SlotViewData(
                createInventoryGridSlot(utilityStack),
                "Z",
                false,
                buildInventorySignature(utilityStack, -1, false)));

        for (int internalSlot = 0; internalSlot < 9; internalSlot++) {
            if (reservedLookup.contains(internalSlot)) {
                continue;
            }

            ItemStack stack = hotbar == null ? null : hotbar.getItemStack((short) internalSlot);
            usableSlots.add(new HotbarHudModule.SlotViewData(
                    createInventoryGridSlot(stack),
                    Integer.toString(internalSlot + 1),
                    activeSlot == internalSlot,
                    buildInventorySignature(stack, internalSlot, activeSlot == internalSlot)));
        }

        AbilityBindingComponent bindingComponent = context.getStore().getComponent(
                context.getRef(),
                AbilityBindingComponent.getComponentType());

        List<HotbarHudModule.SpellSlotViewData> spellSlots = switch (controlType) {
            case Hotbar -> buildHotbarSpellSlots(reservedAbilitySlots, bindingComponent, activeSlot, context.getRef(), context.getStore());
            case AbilitySlots123 -> buildAbilitySlotSpellSlots(bindingComponent, context.getRef(), context.getStore());
            case Weapons -> List.of();
        };

        return new HotbarLayoutData(usableSlots, spellSlots);
    }

    @Nonnull
    private static List<HotbarHudModule.SpellSlotViewData> buildHotbarSpellSlots(
            @Nonnull List<Integer> reservedAbilitySlots,
            @Nullable AbilityBindingComponent bindingComponent,
            int activeSlot,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        List<HotbarHudModule.SpellSlotViewData> spellSlots = new ArrayList<>(reservedAbilitySlots.size());
        for (int internalSlot : reservedAbilitySlots) {
            String abilityId = bindingComponent == null ? null : bindingComponent.getHotbarBinding(internalSlot);
            spellSlots.add(new HotbarHudModule.SpellSlotViewData(
                    resolveAbilityIconPath(abilityId),
                    Integer.toString(internalSlot + 1),
                    activeSlot == internalSlot,
                    buildAbilitySignature(abilityId, internalSlot, activeSlot == internalSlot),
                    resolveAbilityCooldownText(abilityId, entityRef, store)));
        }

        return spellSlots;
    }

    @Nonnull
    private static List<HotbarHudModule.SpellSlotViewData> buildAbilitySlotSpellSlots(
            @Nullable AbilityBindingComponent bindingComponent,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        List<HotbarHudModule.SpellSlotViewData> spellSlots = new ArrayList<>(3);
        for (int slotNumber = 1; slotNumber <= 3; slotNumber++) {
            String abilityId = bindingComponent == null ? null : bindingComponent.getAbilitySlotBinding(slotNumber);
            spellSlots.add(new HotbarHudModule.SpellSlotViewData(
                    resolveAbilityIconPath(abilityId),
                    resolveAbilitySlotKeyLabel(slotNumber),
                    false,
                    buildAbilitySignature(abilityId, slotNumber, false),
                    resolveAbilityCooldownText(abilityId, entityRef, store)));
        }

        return spellSlots;
    }

    @Nonnull
    private static String resolveAbilitySlotKeyLabel(int slotNumber) {
        return switch (slotNumber) {
            case 1 -> "Q";
            case 2 -> "E";
            case 3 -> "R";
            default -> Integer.toString(slotNumber);
        };
    }

    @Nonnull
    private static String resolveAbilityIconPath(@Nullable String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return "";
        }
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            return "";
        }
        String icon = abilityDef.getIcon();
        return (icon == null || icon.isBlank()) ? "" : icon;
    }

    @Nonnull
    private static String resolveAbilityCooldownText(
            @Nullable String abilityId,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        if (abilityId == null || abilityId.isBlank()) {
            return "";
        }
        ClassAbilitySystem abilitySystem = Realmweavers.get().getClassAbilitySystem();
        if (abilitySystem == null) {
            return "";
        }
        float remaining = abilitySystem.getAbilityCooldownRemaining(entityRef, store, abilityId);
        return remaining > 0f ? abilitySystem.formatCooldownSeconds(remaining) : "";
    }

    @Nonnull
    private static ItemGridSlot createInventoryGridSlot(@Nullable ItemStack stack) {
        ItemGridSlot slot = stack == null || stack.isEmpty() ? new ItemGridSlot() : new ItemGridSlot(stack);
        slot.setActivatable(true);
        return slot;
    }

    @Nonnull
    private static String buildInventorySignature(@Nullable ItemStack stack, int internalSlot, boolean active) {
        if (stack == null || stack.isEmpty()) {
            return internalSlot + ":empty:" + active;
        }

        return internalSlot + ":"
                + stack.getItemId() + ':'
                + stack.getQuantity() + ':'
                + Math.round(stack.getDurability() * 1000d) + ':'
                + Math.round(stack.getMaxDurability() * 1000d) + ':'
                + active;
    }

    @Nonnull
    private static String buildAbilitySignature(@Nullable String abilityId, int internalSlot, boolean active) {
        return internalSlot + ":" + (abilityId == null ? "empty" : abilityId)
                + ':' + active
                + ':' + resolveAbilityIconPath(abilityId);
    }

    private record HotbarLayoutData(
            @Nonnull List<HotbarHudModule.SlotViewData> usableSlots,
            @Nonnull List<HotbarHudModule.SpellSlotViewData> spellSlots) {
    }
}