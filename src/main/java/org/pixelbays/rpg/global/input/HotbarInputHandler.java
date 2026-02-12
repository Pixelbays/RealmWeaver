package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Handles hotbar-based ability input.
 * Intercepts hotbar slot switches and triggers abilities for designated slots.
 * Fixes client desync by forcing player back to original slot.
 */
public class HotbarInputHandler {

    private final ExamplePlugin plugin;

    public HotbarInputHandler(@Nonnull ExamplePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Process hotbar interaction packets.
     * 
     * @return true to block the packet, false to let it through
     */
    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        // Get configured ability slots from config
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config == null) {
            return false;
        }

        int[] abilitySlots = config.getHotbarAbilitySlots();
        if (abilitySlots == null || abilitySlots.length == 0) {
            return false;
        }

        for (SyncInteractionChain chain : syncPacket.updates) {
            // Look for SwapFrom interaction (player leaving current slot)
            if (chain.interactionType == InteractionType.SwapFrom &&
                chain.data != null &&
                chain.initial) {

                int targetSlot = chain.data.targetSlot;
                int originalSlot = chain.activeHotbarSlot;

                // Check if target slot is an ability slot
                if (isAbilitySlot(targetSlot, abilitySlots)) {
                    // Trigger ability and block the slot switch
                    handleHotbarAbility(playerRef, targetSlot, originalSlot);
                    return true; // Block the packet
                }
            }
        }

        return false; // Let packet through
    }

    /**
     * Check if a slot is configured as an ability slot.
     */
    private boolean isAbilitySlot(int slot, int[] abilitySlots) {
        for (int abilitySlot : abilitySlots) {
            if (slot == abilitySlot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trigger hotbar ability and fix client desync.
     */
    private void handleHotbarAbility(@Nonnull PlayerRef playerRef, int abilitySlot, int originalSlot) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        World world = store.getExternalData().getWorld();

        // Schedule on world thread for thread safety
        world.execute(() -> {
            Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
            if (playerComponent == null) {
                return;
            }
            // Fix client desync: force client back to original slot
            playerComponent.getInventory().setActiveHotbarSlot((byte) originalSlot);
            SetActiveSlot setActiveSlotPacket = new SetActiveSlot(
                Inventory.HOTBAR_SECTION_ID,  // -1 indicates the hotbar
                originalSlot
            );
            playerRef.getPacketHandler().write(setActiveSlotPacket);

            // Get ability binding for this slot
            AbilityBindingComponent bindingComp = store.getComponent(entityRef, AbilityBindingComponent.getComponentType());
            if (bindingComp == null) {
                playerComponent.sendMessage(Message.raw("No ability bound to slot " + abilitySlot + ". Use /bindability"));
                return;
            }

            String abilityId = bindingComp.getHotbarBinding(abilitySlot);
            if (abilityId == null || abilityId.isEmpty()) {
                playerComponent.sendMessage(Message.raw("No ability bound to slot " + abilitySlot + ". Use /bindability"));
                return;
            }

            // Get ability definition to determine interaction type
            ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
            if (abilityDef == null) {
                playerComponent.sendMessage(Message.raw("Ability not found: " + abilityId));
                return;
            }

            // Determine interaction type from ability binding
            ClassAbilityDefinition.AbilityInputBinding binding = abilityDef.getInputBinding();
            InteractionType type = switch (binding == null ? ClassAbilityDefinition.AbilityInputBinding.Ability1 : binding) {
                case Ability2 -> InteractionType.Ability2;
                case Ability3 -> InteractionType.Ability3;
                case Ability1 -> InteractionType.Ability1;
            };

            // Use ClassAbilitySystem to trigger the ability
            ClassAbilitySystem abilitySystem = plugin.getClassAbilitySystem();
            ClassAbilitySystem.TriggerResult result = abilitySystem.triggerAbility(entityRef, store, abilityId, type);

            if (result.isFailure()) {
                String errorMsg = result.getErrorMessage();
                if (errorMsg != null) {
                    playerComponent.sendMessage(Message.raw(errorMsg));
                }
                return;
            }

            // Debug logging
            RpgLogging.debugDeveloper(
                "Hotbar ability triggered: player=%s, slot=%d (key %d), ability=%s, chain=%s",
                playerRef.getUsername(),
                abilitySlot,
                abilitySlot + 1,
                result.getDisplayName(),
                result.getInteractionChainId()
            );

            // Send feedback to player
            playerComponent.sendMessage(Message.raw(result.getDisplayName()));
        });
    }
}
