package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.binding.AbilityBindingService;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
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
 * Handles weapon-based ability input (Primary = left-click, Secondary =
 * right-click).
 * Triggers abilities when player clicks with a weapon equipped.
 */
@SuppressWarnings("null")
public class WeaponsInputHandler {

    private final ExamplePlugin plugin;
    private final AbilityBindingService bindingService;

    public WeaponsInputHandler(@Nonnull ExamplePlugin plugin) {
        this.plugin = plugin;
        this.bindingService = new AbilityBindingService();
    }

    /**
     * Process weapon interaction packets.
     * 
     * @return true to block the packet, false to let it through
     */
    public boolean handlePacket(@Nonnull PlayerRef playerRef, @Nonnull SyncInteractionChains syncPacket) {
        for (SyncInteractionChain chain : syncPacket.updates) {
            // Check for Primary (left-click) or Secondary (right-click)
            if ((chain.interactionType == InteractionType.Primary ||
                    chain.interactionType == InteractionType.Secondary) &&
                    chain.initial) {

                // Check if player has a weapon equipped
                if (hasWeaponEquipped(playerRef)) {
                    handleWeaponAbility(playerRef, chain.interactionType);
                    // Don't block - let the weapon attack through for now
                    // Later can add option to block based on ability config
                }
            }
        }

        return false; // Don't block weapon attacks
    }

    /**
     * Check if player has a weapon equipped in main hand.
     * TODO: Implement weapon type checking (staff, sword, axe, etc.)
     */
    private boolean hasWeaponEquipped(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (!entityRef.isValid()) {
            return false;
        }

        Store<EntityStore> store = entityRef.getStore();
        Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
        if (playerComponent == null) {
            return false;
        }

        // TODO: Check inventory for weapon in active hotbar slot
        // For now, assume true to allow testing
        return true;
    }

    /**
     * Trigger weapon-based ability.
     */
    private void handleWeaponAbility(@Nonnull PlayerRef playerRef, @Nonnull InteractionType interactionType) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        World world = store.getExternalData().getWorld();

        // Schedule ability trigger on world thread
        world.execute(() -> {
            Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
            if (playerComponent == null) {
                return;
            }

            AbilityBindingComponent bindingComponent = store.getComponent(entityRef, AbilityBindingComponent.getComponentType());
            if (bindingComponent == null) {
                return;
            }

            String abilityId = bindingComponent.getWeaponBinding(interactionType.name());
            if (abilityId == null || abilityId.isEmpty()) {
                return;
            }
            String resolvedAbilityId = abilityId;

            if (!bindingService.isAbilityUnlocked(entityRef, store, resolvedAbilityId)) {
                playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.notUnlocked")
                        .param("abilityId", resolvedAbilityId));
                return;
            }

            ClassAbilitySystem abilitySystem = plugin.getClassAbilitySystem();
            if (abilitySystem == null) {
                return;
            }
            ClassAbilitySystem.TriggerResult result = abilitySystem.triggerAbility(entityRef, store, resolvedAbilityId, interactionType);
            if (result.isFailure()) {
                String errorMessage = result.getErrorMessage();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    playerComponent.sendMessage(Message.translation("pixelbays.rpg.ability.trigger.error")
                            .param("error", errorMessage));
                }
                return;
            }

            // Debug logging
            RpgLogging.debugDeveloper(
                    "Weapon ability trigger: player=%s, type=%s, ability=%s",
                    playerRef.getUsername(),
                    interactionType,
                    resolvedAbilityId);
        });
    }
}
