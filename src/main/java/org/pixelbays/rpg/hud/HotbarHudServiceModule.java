package org.pixelbays.rpg.hud;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;

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
        Inventory inventory = context.getPlayer().getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        ItemContainer utility = inventory.getUtility();
        int activeSlot = inventory.getActiveHotbarSlot();

        List<Integer> reservedAbilitySlots = PlayerHudServiceSupport.resolveConfiguredAbilityHotbarSlots();
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
        List<HotbarHudModule.SlotViewData> spellSlots = new ArrayList<>(reservedAbilitySlots.size());
        for (int internalSlot : reservedAbilitySlots) {
            String abilityId = bindingComponent == null ? null : bindingComponent.getHotbarBinding(internalSlot);
            spellSlots.add(new HotbarHudModule.SlotViewData(
                    createAbilityGridSlot(abilityId),
                    Integer.toString(internalSlot + 1),
                    activeSlot == internalSlot,
                    buildAbilitySignature(abilityId, internalSlot, activeSlot == internalSlot)));
        }

        return new HotbarLayoutData(usableSlots, spellSlots);
    }

    @Nonnull
    private static ItemGridSlot createInventoryGridSlot(@Nullable ItemStack stack) {
        ItemGridSlot slot = stack == null || stack.isEmpty() ? new ItemGridSlot() : new ItemGridSlot(stack);
        slot.setActivatable(true);
        return slot;
    }

    @Nonnull
    private static ItemGridSlot createAbilityGridSlot(@Nullable String abilityId) {
        ItemGridSlot slot = new ItemGridSlot();
        slot.setActivatable(true);
        slot.setSkipItemQualityBackground(true);

        if (abilityId == null || abilityId.isBlank()) {
            return slot;
        }

        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            slot.setName(abilityId);
            return slot;
        }

        String iconPath = abilityDef.getIcon();
        if (iconPath != null && !iconPath.isBlank()) {
            slot.setIcon(Value.of(new PatchStyle().setTexturePath(Value.of(iconPath))));
        }

        slot.setName(abilityDef.getTranslationKey());
        slot.setDescription(abilityDef.getDescriptionTranslationKey());
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
        return internalSlot + ":" + (abilityId == null ? "empty" : abilityId) + ':' + active;
    }

    private record HotbarLayoutData(
            @Nonnull List<HotbarHudModule.SlotViewData> usableSlots,
            @Nonnull List<HotbarHudModule.SlotViewData> spellSlots) {
    }
}