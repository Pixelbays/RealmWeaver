package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Main packet filter for intercepting player input and routing to appropriate
 * ability handlers.
 * Filters SyncInteractionChains packets (ID 290) and delegates to
 * control-type-specific handlers.
 */
public class AbilityInputFilter implements PlayerPacketFilter {

    private final WeaponsInputHandler weaponsHandler;
    private final HotbarInputHandler hotbarHandler;
    private final AbilitySlotsInputHandler abilitySlotsHandler;

    public AbilityInputFilter(@Nonnull Realmweavers plugin) {
        this.weaponsHandler = new WeaponsInputHandler(plugin);
        this.hotbarHandler = new HotbarInputHandler(plugin);
        this.abilitySlotsHandler = new AbilitySlotsInputHandler(plugin);
    }

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        // Only process SyncInteractionChains packets (ID 290)
        if (!(packet instanceof SyncInteractionChains syncPacket)) {
            return false; // Let packet through
        }

        if (playerRef == null) {
            return false;
        }

        @Nonnull
        PlayerRef safeRef = playerRef;
        

        RpgModConfig config = resolveConfig();
        if (config == null || !config.isAbilityModuleEnabled()) {
            return false;
        }

        // Determine which control type to use for this player
        String activeClassId = Realmweavers.get().getCharacterManager().resolveActivePrimaryClassId(safeRef);
        AbilityControlType controlType = getPlayerControlType(safeRef);
        if (controlType == null) {
            return false; // No control type configured
        }

        logAbilitySlotPresses(safeRef, syncPacket, activeClassId, controlType);

        // Route to appropriate handler based on control type
        return switch (controlType) {
            case Weapons -> weaponsHandler.handlePacket(safeRef, syncPacket);
            case Hotbar -> hotbarHandler.handlePacket(safeRef, syncPacket);
            case AbilitySlots123 -> abilitySlotsHandler.handlePacket(safeRef, syncPacket);
        };
    }

    /**
     * Determine the active control type for a player.
     * Checks active class override, then falls back to global config.
     */
    private AbilityControlType getPlayerControlType(@Nonnull PlayerRef playerRef) {
        String classId = Realmweavers.get().getCharacterManager().resolveActivePrimaryClassId(playerRef);
        if (!classId.isBlank()) {
            return getControlTypeForClass(classId, "default");
        }

        RpgModConfig config = resolveConfig();
        return config != null && config.getAbilityControlType() != null
                ? config.getAbilityControlType()
                : AbilityControlType.Hotbar;
    }

    /**
     * Get control type for a specific class.
     * Checks class override, then falls back to global config.
     */
    public static AbilityControlType getControlTypeForClass(@Nonnull String classId, @Nonnull String configId) {
        ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
        if (classDef != null) {
            return classDef.getEffectiveAbilityControlType(configId);
        }

        // Fallback
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset(configId);
        if (config == null && "default".equalsIgnoreCase(configId)) {
            config = resolveConfig();
        }
        return config != null ? config.getAbilityControlType() : AbilityControlType.Hotbar;
    }

    private static RpgModConfig resolveConfig() {
        if (RpgModConfig.getAssetMap() == null) {
            return null;
        }

        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config != null) {
            return config;
        }

        config = RpgModConfig.getAssetMap().getAsset("Default");
        if (config != null) {
            return config;
        }

        if (RpgModConfig.getAssetMap().getAssetMap().isEmpty()) {
            return null;
        }

        return RpgModConfig.getAssetMap().getAssetMap().values().iterator().next();
    }

    private void logAbilitySlotPresses(@Nonnull PlayerRef playerRef,
            @Nonnull SyncInteractionChains syncPacket,
            @Nonnull String activeClassId,
            @Nonnull AbilityControlType controlType) {
        for (SyncInteractionChain chain : syncPacket.updates) {
            if (!chain.initial) {
                continue;
            }

            int slotNumber = resolveAbilitySlotNumber(chain.interactionType);
            if (slotNumber <= 0) {
                continue;
            }

            RpgLogging.debugDeveloper(
                    "[AbilityInputFilter] spell-slot press player=%s slot=%d interaction=%s class=%s controlType=%s",
                    playerRef.getUsername(),
                    slotNumber,
                    chain.interactionType,
                    activeClassId.isBlank() ? "<none>" : activeClassId,
                    controlType);
        }
    }

    private int resolveAbilitySlotNumber(@Nonnull InteractionType interactionType) {
        return switch (interactionType) {
            case Ability1 -> 1;
            case Ability2 -> 2;
            case Ability3 -> 3;
            default -> 0;
        };
    }

}
