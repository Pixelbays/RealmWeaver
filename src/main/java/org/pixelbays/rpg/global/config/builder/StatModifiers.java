package org.pixelbays.rpg.global.config.builder;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

/**
 * Configuration for an exp curve definition loaded from asset pack.
 * Example file: Server/Entity/ExpCurves/Curve_Linear.json
 */
@SuppressWarnings({ "FieldHidesSuperclassField" })
public class StatModifiers {
        public static final BuilderCodec<StatModifiers> CODEC = BuilderCodec
                        .builder(StatModifiers.class, StatModifiers::new)
                        .append(
                                        new KeyedCodec<>("AdditiveModifiers",
                                                        new Object2FloatMapCodec<>(Codec.STRING,
                                                                        Object2FloatOpenHashMap::new),
                                                        true),
                                        (i,
                                                        stringObject2DoubleMap) -> i.AdditiveModifiers = stringObject2DoubleMap,
                                        i -> i.AdditiveModifiers)
                        .addValidator(Validators.nonNull())
                        .addValidator(Validators.nonEmptyMap())
                        .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
                        .documentation("Modifiers to apply to EntityStats.")
                        .add()
                        .append(
                                        new KeyedCodec<>("MultiplicativeModifiers",
                                                        new Object2FloatMapCodec<>(Codec.STRING,
                                                                        Object2FloatOpenHashMap::new),
                                                        true),
                                        (i,
                                                        stringObject2DoubleMap) -> i.MultiplicativeModifiers = stringObject2DoubleMap,
                                        i -> i.MultiplicativeModifiers)
                        .addValidator(Validators.nonNull())
                        .addValidator(Validators.nonEmptyMap())
                        .addValidator(EntityStatType.VALIDATOR_CACHE.getMapKeyValidator())
                        .documentation("Modifiers to apply to EntityStats.")
                        .add()
                        .build();

        private Object2FloatMap<String> AdditiveModifiers; // Flat bonuses (e.g., +10 Strength)
        private Object2FloatMap<String> MultiplicativeModifiers; // % bonuses (e.g., +10% Strength)

        public StatModifiers() {
                this.AdditiveModifiers = new Object2FloatOpenHashMap<>();
                this.MultiplicativeModifiers = new Object2FloatOpenHashMap<>();
        }

        public Object2FloatMap<String> getAdditiveModifiers() {
                return AdditiveModifiers;
        }

        public void setAdditiveModifiers(Object2FloatMap<String> additiveModifiers) {
                this.AdditiveModifiers = additiveModifiers;
        }

        public Object2FloatMap<String> getMultiplicativeModifiers() {
                return MultiplicativeModifiers;
        }

        public void setMultiplicativeModifiers(Object2FloatMap<String> multiplicativeModifiers) {
                this.MultiplicativeModifiers = multiplicativeModifiers;
        }

        public boolean isEmpty() {
                return (AdditiveModifiers == null || AdditiveModifiers.isEmpty())
                                && (MultiplicativeModifiers == null || MultiplicativeModifiers.isEmpty());
        }

}
