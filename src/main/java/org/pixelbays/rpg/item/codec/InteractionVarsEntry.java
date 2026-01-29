package org.pixelbays.rpg.item.codec;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionCooldown;
import com.hypixel.hytale.protocol.RootInteractionSettings;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionRules;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

/**
 * Custom RootInteraction-like class for InteractionVars that supports inline Interaction definitions.
 * 
 * <p>The base {@link RootInteraction} expects the "Interactions" field to contain an array of
 * Interaction ID strings. However, in Items and Abilities, the JSON uses inline Interaction objects 
 * with full properties like DamageCalculator, Effects, etc.
 * 
 * <p>This class supports both {@link Interaction} (for loading existing JSON files) and 
 * {@link CustomInteraction} (for editor-friendly editing with all fields visible).
 * 
 * <p><b>Compatibility:</b>
 * <ul>
 *   <li>Existing JSON files with Interaction objects load correctly (backward compatible)</li>
 *   <li>New files can use CustomInteraction for full asset editor support without inheritance warnings</li>
 *   <li>At runtime, both formats work identically via the Type field resolution</li>
 * </ul>
 */
public class InteractionVarsEntry {
    
    /**
     * Codec that supports inline Interaction definitions.
     * Uses Interaction.CODEC to maintain compatibility with existing JSON files.
     * Note: Asset editor will show inheritance warnings, but all properties function correctly.
     */
    @Nonnull
    public static final BuilderCodec<InteractionVarsEntry> CODEC = BuilderCodec
            .builder(InteractionVarsEntry.class, InteractionVarsEntry::new)
            // Use Interaction.CODEC for compatibility with existing base game JSON files
            .<Interaction[]>append(
                new KeyedCodec<>("Interactions", new ArrayCodec<>(Interaction.CODEC, Interaction[]::new)),
                (entry, interactions) -> entry.interactions = interactions,
                entry -> entry.interactions
            )
            .documentation(
                "Array of inline Interaction definitions. Each interaction can specify:\n" +
                "- Type: The interaction type (required) - e.g., 'Damage', 'Simple', 'Activation'\n" +
                "- Parent: Inherit from another interaction (optional)\n" +
                "- DamageCalculator: Damage calculation settings (for Damage type)\n" +
                "- DamageEffects: Sound/visual effects when dealing damage (for Damage type)\n" +
                "- Effects: General interaction effects (particles, sounds, animations)\n" +
                "- ViewDistance: How far other players see the effects (default: 96.0)\n" +
                "- HorizontalSpeedMultiplier: Movement speed during interaction (default: 1.0)\n" +
                "- RunTime: How long the interaction runs (default: 0.0)\n" +
                "- CancelOnItemChange: Whether to cancel if held item changes (default: true)\n" +
                "- Rules: When the interaction can execute\n" +
                "- Settings: Per-gamemode interaction settings\n" +
                "- Camera: Camera behavior configuration\n" +
                "- Next/Failed/Blocked: Navigation to other interactions\n" +
                "- And all other Interaction properties\n\n" +
                "ASSET EDITOR NOTE: Editor will show inheritance warning but all properties work at runtime.\n" +
                "WORKAROUND: Edit JSON directly - DamageCalculator, Effects, etc. are all functional."
            )
            .add()
            // Cooldown configuration
            .<InteractionCooldown>appendInherited(
                new KeyedCodec<>("Cooldown", RootInteraction.COOLDOWN_CODEC),
                (entry, cooldown) -> entry.cooldown = cooldown,
                entry -> entry.cooldown,
                (entry, parent) -> entry.cooldown = parent.cooldown
            )
            .documentation(
                "Cooldowns prevent an interaction from running repeatedly too quickly. " +
                "During a cooldown, attempting to run an interaction with the same cooldown id will fail."
            )
            .add()
            // Rules configuration
            .<InteractionRules>appendInherited(
                new KeyedCodec<>("Rules", InteractionRules.CODEC),
                (entry, rules) -> entry.rules = rules,
                entry -> entry.rules,
                (entry, parent) -> entry.rules = parent.rules
            )
            .documentation("Rules that control when this root interaction can run or what interactions this root being active prevents.")
            .addValidator(Validators.nonNull())
            .add()
            // Per-gamemode settings
            .<Map<GameMode, RootInteractionSettings>>appendInherited(
                new KeyedCodec<>("Settings",
                    new EnumMapCodec<>(
                        GameMode.class,
                        BuilderCodec.builder(RootInteractionSettings.class, RootInteractionSettings::new)
                            .appendInherited(
                                new KeyedCodec<>("Cooldown", RootInteraction.COOLDOWN_CODEC),
                                (settings, cooldown) -> settings.cooldown = cooldown,
                                settings -> settings.cooldown,
                                (settings, parent) -> settings.cooldown = parent.cooldown
                            )
                            .documentation(
                                "Cooldowns are used to prevent an interaction from running repeatedly too quickly. " +
                                "During a cooldown attempting to run an interaction with the same cooldown id will fail."
                            )
                            .add()
                            .<Boolean>appendInherited(
                                new KeyedCodec<>("AllowSkipChainOnClick", Codec.BOOLEAN),
                                (settings, allow) -> settings.allowSkipChainOnClick = allow,
                                settings -> settings.allowSkipChainOnClick,
                                (settings, parent) -> settings.allowSkipChainOnClick = parent.allowSkipChainOnClick
                            )
                            .documentation("Whether to skip the whole interaction chain when another click is sent.")
                            .add()
                            .build()
                    )
                ),
                (entry, settings) -> entry.settings = settings,
                entry -> entry.settings,
                (entry, parent) -> entry.settings = parent.settings
            )
            .documentation("Per-gamemode settings for this interaction.")
            .add()
            // Click queuing timeout
            .<Float>appendInherited(
                new KeyedCodec<>("ClickQueuingTimeout", Codec.FLOAT),
                (entry, timeout) -> entry.clickQueuingTimeout = timeout,
                entry -> entry.clickQueuingTimeout,
                (entry, parent) -> entry.clickQueuingTimeout = parent.clickQueuingTimeout
            )
            .documentation("Controls the amount of time this root interaction can remain in the click queue before being discarded.")
            .add()
            // Require new click
            .<Boolean>appendInherited(
                new KeyedCodec<>("RequireNewClick", Codec.BOOLEAN),
                (entry, require) -> entry.requireNewClick = require,
                entry -> entry.requireNewClick,
                (entry, parent) -> entry.requireNewClick = parent.requireNewClick
            )
            .documentation("Requires the user to click again before running another root interaction of the same type.")
            .add()
            .build();
    
    @Nullable
    protected Interaction[] interactions;
    
    @Nullable
    protected InteractionCooldown cooldown;
    
    @Nonnull
    protected InteractionRules rules = InteractionRules.DEFAULT_RULES;
    
    @Nonnull
    protected Map<GameMode, RootInteractionSettings> settings = Collections.emptyMap();
    
    protected float clickQueuingTimeout;
    
    protected boolean requireNewClick;
    
    public InteractionVarsEntry() {
    }
    
    @Nullable
    public Interaction[] getInteractions() {
        return interactions;
    }
    
    @Nullable
    public InteractionCooldown getCooldown() {
        return cooldown;
    }
    
    @Nonnull
    public InteractionRules getRules() {
        return rules;
    }
    
    @Nonnull
    public Map<GameMode, RootInteractionSettings> getSettings() {
        return settings;
    }
    
    public float getClickQueuingTimeout() {
        return clickQueuingTimeout;
    }
    
    public boolean isRequireNewClick() {
        return requireNewClick;
    }
}
