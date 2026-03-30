package org.pixelbays.rpg.ability.system;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Manages virtual ability icons in hotbar slots.
 * Creates placeholder items that display ability icons in the player's hotbar.
 * These items are visual representations only - actual ability triggering is handled by HotbarInputHandler.
 * 
 * NOTE: This is a temporary placeholder system using regular items.
 * In the future, this will be replaced with a custom UI overlay system.
 */
public class HotbarAbilityIconManager {

    private static final String PLACEHOLDER_ITEM_ID = "Tool_Hammer_Wood"; // Default placeholder item
    
    /**
     * Synchronize all hotbar ability icons for a player.
     * Clears existing ability icons and creates new ones based on current bindings.
     * 
     * @param entityRef The player entity reference
     * @param store The entity store
     */
    public void syncHotbarIcons(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        AbilityBindingComponent bindingComp = store.getComponent(entityRef, AbilityBindingComponent.getComponentType());
        if (bindingComp == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        Map<Integer, String> hotbarBindings = bindingComp.getHotbarBindings();
        
        // Update each hotbar slot
        for (int slot = 0; slot < 9; slot++) {
            String abilityId = hotbarBindings.get(slot);
            
            if (abilityId != null && !abilityId.isEmpty()) {
                // Ability is bound to this slot - create virtual item
                ItemStack abilityIcon = createAbilityIcon(abilityId);
                if (abilityIcon != null) {
                    // hotbar.setItemStackForSlot((short) slot, abilityIcon);
                }
            }
            // Note: We don't clear unbound slots automatically to avoid deleting player's items
        }
    }
    
    /**
     * Update a single hotbar slot with an ability icon.
     * 
     * @param entityRef The player entity reference
     * @param store The entity store
     * @param slot The hotbar slot (0-8)
     * @param abilityId The ability ID to display, or null to clear
     */
    public void updateHotbarSlot(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, int slot, @Nullable String abilityId) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        
        if (abilityId != null && !abilityId.isEmpty()) {
            // Create and place ability icon
            ItemStack abilityIcon = createAbilityIcon(abilityId);
            if (abilityIcon != null) {
                hotbar.setItemStackForSlot((short) slot, abilityIcon);
            }
        } else {
            // Clear the slot
            // Note: Be careful here - we might delete a player's actual item
            // Consider checking if the item is an ability icon before clearing
            hotbar.setItemStackForSlot((short) slot, null);
        }
    }
    
    /**
     * Create a virtual ItemStack representing an ability icon.
     * Uses a placeholder item temporarily.
     * 
     * TODO: Once custom UI is implemented, this will be replaced with UI overlays.
     * TODO: Support custom icons from ClassAbilityDefinition.getIcon()
     * 
     * @param abilityId The ability ID
     * @return The ItemStack, or null if ability not found
     */
    @Nullable
    private ItemStack createAbilityIcon(@Nonnull String abilityId) {
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            return null;
        }
        
        // Create item stack with placeholder item ID
        // Using the item ID directly - the ItemStack constructor will look up the item
        ItemStack itemStack = new ItemStack(PLACEHOLDER_ITEM_ID, 1);

        // Apply ability icon metadata when available
        String iconPath = abilityDef.getIcon();
        if (iconPath != null && !iconPath.isEmpty()) {
            itemStack = itemStack.withMetadata("Icon", Codec.STRING, iconPath);

            var iconProps = abilityDef.getIconProperties();
            if (iconProps != null) {
            com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties props =
                new com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties(
                    iconProps.scale,
                    iconProps.translation,
                    iconProps.rotation);
            itemStack = itemStack.withMetadata(
                "IconProperties",
                com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties.CODEC,
                props);
            }
        }

        // Apply ability translation keys so the client can resolve the name/description
        String abilityNameKey = abilityDef.getTranslationKey();
        String abilityDescriptionKey = abilityDef.getDescriptionTranslationKey();
        if (abilityNameKey != null || abilityDescriptionKey != null) {
            ItemTranslationProperties translation = new ItemTranslationProperties(
                abilityNameKey != null ? abilityNameKey : "",
                abilityDescriptionKey != null ? abilityDescriptionKey : "");
            itemStack = itemStack.withMetadata("TranslationProperties", ItemTranslationProperties.CODEC, translation);
        }
        
        return itemStack;
    }
    
    /**
     * Clear all ability icons from a player's hotbar.
     * 
     * WARNING: This will clear ALL items from the hotbar, including non-ability items!
     * Only use this for testing or when you're sure the hotbar only contains ability icons.
     * 
     * @param entityRef The player entity reference
     * @param store The entity store
     */
    public void clearAllIcons(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        
        // Clear all slots
        // WARNING: This deletes ALL items, not just ability icons!
        for (int slot = 0; slot < 9; slot++) {
            hotbar.setItemStackForSlot((short) slot, null);
        }
    }
}
