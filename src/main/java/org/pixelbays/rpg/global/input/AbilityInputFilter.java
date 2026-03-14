package org.pixelbays.rpg.global.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.movement.input.ClickToMoveInputHandler;

import com.hypixel.hytale.protocol.Packet;
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
    private final ClickToMoveInputHandler clickToMoveHandler;

    public AbilityInputFilter(@Nonnull ExamplePlugin plugin) {
        this.weaponsHandler = new WeaponsInputHandler(plugin);
        this.hotbarHandler = new HotbarInputHandler(plugin);
        this.abilitySlotsHandler = new AbilitySlotsInputHandler(plugin);
        this.clickToMoveHandler = new ClickToMoveInputHandler();
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
        
        if (clickToMoveHandler.handlePacket(safeRef, syncPacket)) {
            return true; // Consume movement click
        }

        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config == null || !config.isAbilityModuleEnabled()) {
            return false;
        }

        // Determine which control type to use for this player
        AbilityControlType controlType = getPlayerControlType(safeRef);
        if (controlType == null) {
            return false; // No control type configured
        }

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
        // TODO: Get player's active class and check for override
        // For now, just use global default
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config != null && config.isAbilityModuleEnabled()) {
            return config.getAbilityControlType();
        }

        // Fallback to Hotbar
        return AbilityControlType.Hotbar;
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
        return config != null ? config.getAbilityControlType() : AbilityControlType.Hotbar;
    }

}
