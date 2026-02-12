package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Handles native ability slot input (Ability1, Ability2, Ability3).
 * Uses Hytale's built-in ability interaction types (IDs 2, 3, 4).
 */
public class AbilitySlotsInputHandler {

    private final ExamplePlugin plugin;

    public AbilitySlotsInputHandler(@Nonnull ExamplePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Process ability slot interaction packets.
     * 
     * @return true to block the packet, false to let it through
     */
    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        for (SyncInteractionChain chain : syncPacket.updates) {
            // Check for Ability1, Ability2, or Ability3
            if ((chain.interactionType == InteractionType.Ability1 ||
                    chain.interactionType == InteractionType.Ability2 ||
                    chain.interactionType == InteractionType.Ability3) &&
                    chain.initial) {

                handleAbilitySlot(playerRef, chain.interactionType);
                return true; // Block the packet - we're handling it
            }
        }

        return false; // Let packet through
    }

    /**
     * Trigger ability slot action.
     */
    private void handleAbilitySlot(@Nonnull PlayerRef playerRef, @Nonnull InteractionType abilityType) {
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

            // Get ability slot number (1, 2, or 3)
            int slotNumber = switch (abilityType) {
                case Ability1 -> 1;
                case Ability2 -> 2;
                case Ability3 -> 3;
                default -> 0;
            };

            // Look up ability binding for this ability slot
            AbilityBindingComponent bindingComp = store.getComponent(entityRef,
                    ExamplePlugin.get().getAbilityBindingComponentType());
            if (bindingComp == null) {
                playerComponent.sendMessage(Message.raw("No ability binding data. Use /bindability"));
                return;
            }

            String abilityId = bindingComp.getAbilitySlotBinding(slotNumber);
            if (abilityId == null || abilityId.isEmpty()) {
                playerComponent.sendMessage(Message.raw("No ability bound to slot " + slotNumber + ". Use /bindability"));
                return;
            }

            // Verify ability is unlocked
            ClassAbilityComponent abilityComp = store.getComponent(entityRef,
                    ExamplePlugin.get().getClassAbilityComponentType());
            if (abilityComp == null || !abilityComp.hasAbility(abilityId)) {
                playerComponent.sendMessage(Message.raw("Ability not unlocked: " + abilityId));
                return;
            }

            // Resolve ability definition
            ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
            if (abilityDef == null) {
                playerComponent.sendMessage(Message.raw("Ability not found: " + abilityId));
                return;
            }

            // Execute ability
            ClassAbilitySystem abilitySystem = plugin.getClassAbilitySystem();
            ClassAbilitySystem.TriggerResult result = abilitySystem.triggerAbility(entityRef, store, abilityId, abilityType);
            if (result.isFailure()) {
                String errorMsg = result.getErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    playerComponent.sendMessage(Message.raw(errorMsg));
                }
                return;
            }

            // Debug logging
                RpgLogging.debugDeveloper(
                    "Ability slot trigger: player=%s, slot=%d, ability=%s",
                    playerRef.getUsername(),
                    slotNumber,
                    abilityId);
        });
    }
}
