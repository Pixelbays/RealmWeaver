package org.pixelbays.rpg.item.codec;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionSettings;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionCameraSettings;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionEffects;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionRules;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageEffects;

/**
 * Custom Interaction representation with all fields PUBLIC for asset editor support.
 * 
 * <p>This class mirrors the base game Interaction structure but uses public fields,
 * allowing us to create a codec with regular {@code append} instead of {@code appendInherited}.
 * This removes the "Parent and inheritance is not yet supported" warning in the asset editor.
 * 
 * <p><b>Type Field:</b> The Type field determines which Interaction subclass gets instantiated
 * at runtime (e.g., "Damage" → DamageEntityInteraction, "Simple" → SimpleInteraction).
 * This is the only field that references parent behavior for determining the concrete class.
 * 
 * <p><b>All Other Fields:</b> Directly editable without inheritance - set the values you want explicitly.
 * 
 * <p>At runtime, these values are converted to real Interaction objects via the base game's
 * asset loading system using the Type field.
 */
public class CustomInteraction {
    
    /**
     * Codec that makes all Interaction fields visible and editable in the asset editor.
     * Uses append() instead of appendInherited() to avoid editor warnings.
     */
    @Nonnull
    public static final BuilderCodec<CustomInteraction> CODEC = BuilderCodec
            .builder(CustomInteraction.class, CustomInteraction::new)
            // Type field - determines which Interaction subclass to instantiate
            .<String>append(
                new KeyedCodec<>("Type", Codec.STRING),
                (interaction, value) -> interaction.type = value,
                interaction -> interaction.type
            )
            .documentation("The type of interaction (e.g., 'Damage', 'Activation', 'SendMessage')")
            .addValidator(Validators.nonNull())
            .add()
            // Parent field - optional inheritance (editor visible but no auto-copy)
            .<String>append(
                new KeyedCodec<>("Parent", Codec.STRING),
                (interaction, value) -> interaction.parent = value,
                interaction -> interaction.parent
            )
            .documentation("Optional parent interaction to inherit from (values must be set explicitly)")
            .add()
            // ViewDistance
            .<Double>append(
                new KeyedCodec<>("ViewDistance", Codec.DOUBLE),
                (interaction, value) -> interaction.viewDistance = value,
                interaction -> interaction.viewDistance
            )
            .documentation("Configures the distance in which other players will be able to see the effects of this interaction.")
            .add()
            // Effects
            .<InteractionEffects>append(
                new KeyedCodec<>("Effects", InteractionEffects.CODEC),
                (interaction, value) -> interaction.effects = value,
                interaction -> interaction.effects
            )
            .documentation("Sets effects that will be applied whilst the interaction is running.")
            .add()
            // HorizontalSpeedMultiplier
            .<Float>append(
                new KeyedCodec<>("HorizontalSpeedMultiplier", Codec.FLOAT),
                (interaction, value) -> interaction.horizontalSpeedMultiplier = value,
                interaction -> interaction.horizontalSpeedMultiplier
            )
            .documentation("The multiplier to apply to the horizontal speed of the entity whilst this interaction is running.")
            .metadata(new UIEditor(new UIEditor.FormattedNumber(0.1, null, null)))
            .add()
            // RunTime
            .<Float>append(
                new KeyedCodec<>("RunTime", Codec.FLOAT),
                (interaction, value) -> interaction.runTime = value,
                interaction -> interaction.runTime
            )
            .documentation(
                "The time in seconds this interaction should run for. \n\n" +
                "If *Effects.WaitForAnimationToFinish* is set and the length of the animation is longer than the runtime " +
                "then the interaction will run for longer than the set time."
            )
            .metadata(new UIEditor(new UIEditor.FormattedNumber(0.01, "s", null)))
            .add()
            // CancelOnItemChange
            .<Boolean>append(
                new KeyedCodec<>("CancelOnItemChange", Codec.BOOLEAN),
                (interaction, value) -> interaction.cancelOnItemChange = value,
                interaction -> interaction.cancelOnItemChange
            )
            .documentation("Whether the interaction will be cancelled when the entity's held item changes.")
            .add()
            // Rules
            .<InteractionRules>append(
                new KeyedCodec<>("Rules", InteractionRules.CODEC),
                (interaction, value) -> interaction.rules = value,
                interaction -> interaction.rules
            )
            .documentation("A set of rules that control when this interaction can run.")
            .add()
            // Settings
            .<Map<GameMode, InteractionSettings>>append(
                new KeyedCodec<>(
                    "Settings",
                    new EnumMapCodec<>(
                        GameMode.class,
                        BuilderCodec.builder(InteractionSettings.class, InteractionSettings::new)
                            .append(
                                new KeyedCodec<>("AllowSkipOnClick", Codec.BOOLEAN),
                                (settings, value) -> settings.allowSkipOnClick = value,
                                settings -> settings.allowSkipOnClick
                            )
                            .documentation("Whether to skip this interaction when another click is sent.")
                            .add()
                            .build()
                    )
                ),
                (interaction, value) -> interaction.settings = value,
                interaction -> interaction.settings
            )
            .documentation("Per a gamemode settings.")
            .add()
            // Camera
            .<InteractionCameraSettings>append(
                new KeyedCodec<>("Camera", InteractionCameraSettings.CODEC),
                (interaction, value) -> interaction.camera = value,
                interaction -> interaction.camera
            )
            .documentation("Configures the camera behaviour for this interaction.")
            .add()
            // TYPE-SPECIFIC FIELDS (shown for all types, fill in those relevant to your Type)
            // DamageCalculator - for Damage type interactions
            .<DamageCalculator>append(
                new KeyedCodec<>("DamageCalculator", DamageCalculator.CODEC),
                (interaction, value) -> interaction.damageCalculator = value,
                interaction -> interaction.damageCalculator
            )
            .documentation("Damage calculation settings (for Damage type interactions)")
            .add()
            // DamageEffects - for Damage type interactions
            .<DamageEffects>append(
                new KeyedCodec<>("DamageEffects", DamageEffects.CODEC),
                (interaction, value) -> interaction.damageEffects = value,
                interaction -> interaction.damageEffects
            )
            .documentation("Visual and sound effects when dealing damage (for Damage type interactions)")
            .add()
            // Next - interaction to run on success
            .<String>append(
                new KeyedCodec<>("Next", Codec.STRING),
                (interaction, value) -> interaction.next = value,
                interaction -> interaction.next
            )
            .documentation("The interaction to run when this interaction succeeds")
            .add()
            // Failed - interaction to run on failure
            .<String>append(
                new KeyedCodec<>("Failed", Codec.STRING),
                (interaction, value) -> interaction.failed = value,
                interaction -> interaction.failed
            )
            .documentation("The interaction to run when this interaction fails")
            .add()
            // Blocked - interaction to run when blocked
            .<String>append(
                new KeyedCodec<>("Blocked", Codec.STRING),
                (interaction, value) -> interaction.blocked = value,
                interaction -> interaction.blocked
            )
            .documentation("The interaction to run when this interaction is blocked (for Damage type interactions)")
            .add()
            .build();
    
    // All fields are PUBLIC for codec access
    @Nonnull
    public String type = "";
    
    @Nullable
    public String parent;
    
    public double viewDistance = 96.0;
    
    @Nullable
    public InteractionEffects effects;
    
    public float horizontalSpeedMultiplier = 1.0F;
    
    public float runTime = 0.0F;
    
    public boolean cancelOnItemChange = true;
    
    @Nonnull
    public Map<GameMode, InteractionSettings> settings = Collections.emptyMap();
    
    @Nullable
    public InteractionRules rules;
    
    @Nullable
    public InteractionCameraSettings camera;
    
    // TYPE-SPECIFIC FIELDS
    // These fields are only used by specific interaction types but are shown for all types in the editor
    
    @Nullable
    public DamageCalculator damageCalculator;  // For "Damage" type
    
    @Nullable
    public DamageEffects damageEffects;  // For "Damage" type
    
    @Nullable
    public String next;  // Navigation: interaction to run on success
    
    @Nullable
    public String failed;  // Navigation: interaction to run on failure
    
    @Nullable
    public String blocked;  // Navigation: interaction to run when blocked (Damage type)
    
    public CustomInteraction() {
    }
}
