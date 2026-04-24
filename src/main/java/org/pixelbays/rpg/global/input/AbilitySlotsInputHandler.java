package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.binding.AbilityBindingService;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
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
@SuppressWarnings("null")
public class AbilitySlotsInputHandler {

    private final Realmweavers plugin;
    private final AbilityBindingService bindingService;

    public AbilitySlotsInputHandler(@Nonnull Realmweavers plugin) {
        this.plugin = plugin;
        this.bindingService = new AbilityBindingService();
    }

    /**
     * Process ability slot interaction packets.
     * 
     * @return true to block the packet, false to let it through
     */
    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        for (SyncInteractionChain chain : syncPacket.updates) {
            int slotNumber = resolveAbilitySlotNumber(chain.interactionType);
            if (slotNumber > 0 && chain.initial) {
                RpgLogging.debugDeveloper(
                        "[AbilitySlotsInput] press player=%s slot=%d interaction=%s",
                        playerRef.getUsername(),
                        slotNumber,
                        chain.interactionType);

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

            int slotNumber = resolveAbilitySlotNumber(abilityType);

            // Look up ability binding for this ability slot
            AbilityBindingComponent bindingComp = store.getComponent(entityRef,
                    Realmweavers.get().getAbilityBindingComponentType());
            if (bindingComp == null) {
                playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.noBindingData"));
                return;
            }

            String abilityId = bindingComp.getAbilitySlotBinding(slotNumber);
            if (abilityId == null || abilityId.isEmpty()) {
                playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.noBoundSlot")
                        .param("slot", slotNumber));
                return;
            }
            String resolvedAbilityId = abilityId;

            // Verify ability is unlocked from either class or race progression.
            if (!bindingService.isAbilityUnlocked(entityRef, store, resolvedAbilityId)) {
                playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.notUnlocked")
                        .param("abilityId", resolvedAbilityId));
                return;
            }

            // Resolve ability definition
            ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(resolvedAbilityId);
            if (abilityDef == null) {
                playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.notFound")
                        .param("abilityId", resolvedAbilityId));
                return;
            }

            // Execute ability
            ClassAbilitySystem abilitySystem = plugin.getClassAbilitySystem();
            if (abilitySystem == null) {
                return;
            }
            ClassAbilitySystem.TriggerResult result = abilitySystem.triggerAbility(entityRef, store, resolvedAbilityId,
                    abilityType);
            if (result.isFailure()) {
                String errorMsg = result.getErrorMessage();
                if (!result.shouldSuppressPlayerErrorMessage() && errorMsg != null && !errorMsg.isEmpty()) {
                    playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.error")
                            .param("error", errorMsg));
                }
                return;
            }

        });
    }

    private int resolveAbilitySlotNumber(@Nonnull InteractionType abilityType) {
        return switch (abilityType) {
            case Ability1 -> 1;
            case Ability2 -> 2;
            case Ability3 -> 3;
            default -> 0;
        };
    }
}
