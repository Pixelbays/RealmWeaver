package org.pixelbays.rpg.leveling.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class LevelingModSettings {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

        private static final FunctionCodec<CurrencyScope[], List<CurrencyScope>> CURRENCY_SCOPE_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(new EnumCodec<>(CurrencyScope.class), CurrencyScope[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(CurrencyScope[]::new));

    public enum HardcoreLossType {
        ResetToZero,
        LosePercent
    }

    public static final BuilderCodec<LevelingModSettings> CODEC = BuilderCodec
            .builder(LevelingModSettings.class, LevelingModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("RestedXpEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.restedXpEnabled = s, i -> i.restedXpEnabled)
            .add()
            .append(new KeyedCodec<>("RestedXpBonusPercent", Codec.INTEGER, false, true),
                    (i, s) -> i.restedXpBonusPercent = s, i -> i.restedXpBonusPercent)
            .add()
            .append(new KeyedCodec<>("RestedXpConsumeRatio", Codec.INTEGER, false, true),
                    (i, s) -> i.restedXpConsumeRatio = s, i -> i.restedXpConsumeRatio)
            .add()
            .append(new KeyedCodec<>("RestedXpGainTags", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.restedXpGainTags = s, i -> i.restedXpGainTags)
            .add()
            .append(new KeyedCodec<>("HardcoreEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.hardcoreEnabled = s, i -> i.hardcoreEnabled)
            .add()
            .append(new KeyedCodec<>("HardcoreLossType", new EnumCodec<>(HardcoreLossType.class), false, true),
                    (i, s) -> i.hardcoreLossType = s, i -> i.hardcoreLossType)
            .add()
            .append(new KeyedCodec<>("HardcoreLevelLossPercent", Codec.INTEGER, false, true),
                    (i, s) -> i.hardcoreLevelLossPercent = s, i -> i.hardcoreLevelLossPercent)
            .add()
                .append(new KeyedCodec<>("HardcoreCurrencyLossEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.hardcoreCurrencyLossEnabled = s, i -> i.hardcoreCurrencyLossEnabled)
                .add()
                .append(new KeyedCodec<>("HardcoreCurrencyLossPercent", Codec.INTEGER, false, true),
                    (i, s) -> i.hardcoreCurrencyLossPercent = s, i -> i.hardcoreCurrencyLossPercent)
                .add()
                .append(new KeyedCodec<>("HardcoreCurrencyLossScopes", CURRENCY_SCOPE_LIST_CODEC, false, true),
                    (i, s) -> i.hardcoreCurrencyLossScopes = s, i -> i.hardcoreCurrencyLossScopes)
                .add()
            .append(new KeyedCodec<>("BaseXpMultiplier", Codec.FLOAT, false, true),
                    (i, s) -> i.baseXpMultiplier = s, i -> i.baseXpMultiplier)
            .add()
            .build();

    private boolean enabled;
    private boolean restedXpEnabled;
    private int restedXpBonusPercent;
    private int restedXpConsumeRatio;
    private List<String> restedXpGainTags;
    private boolean hardcoreEnabled;
    private HardcoreLossType hardcoreLossType;
    private int hardcoreLevelLossPercent;
    private boolean hardcoreCurrencyLossEnabled;
    private int hardcoreCurrencyLossPercent;
    private List<CurrencyScope> hardcoreCurrencyLossScopes;
    private float baseXpMultiplier;

    public LevelingModSettings() {
        this.enabled = true;
        this.restedXpEnabled = false;
        this.restedXpBonusPercent = 0;
        this.restedXpConsumeRatio = 1;
        this.restedXpGainTags = new ArrayList<>();
        this.hardcoreEnabled = false;
        this.hardcoreLossType = HardcoreLossType.ResetToZero;
        this.hardcoreLevelLossPercent = 50;
        this.hardcoreCurrencyLossEnabled = false;
        this.hardcoreCurrencyLossPercent = 25;
        this.hardcoreCurrencyLossScopes = new ArrayList<>(List.of(CurrencyScope.Character));
        this.baseXpMultiplier = 1.0f;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRestedXpEnabled() {
        return restedXpEnabled;
    }

    public int getRestedXpBonusPercent() {
        return restedXpBonusPercent;
    }

    public int getRestedXpConsumeRatio() {
        return restedXpConsumeRatio;
    }

    public List<String> getRestedXpGainTags() {
        return restedXpGainTags;
    }

    public boolean isHardcoreEnabled() {
        return hardcoreEnabled;
    }

    public HardcoreLossType getHardcoreLossType() {
        return hardcoreLossType;
    }

    public int getHardcoreLevelLossPercent() {
        return hardcoreLevelLossPercent;
    }

    public boolean isHardcoreCurrencyLossEnabled() {
        return hardcoreCurrencyLossEnabled;
    }

    public int getHardcoreCurrencyLossPercent() {
        return hardcoreCurrencyLossPercent;
    }

    public List<CurrencyScope> getHardcoreCurrencyLossScopes() {
        return hardcoreCurrencyLossScopes == null ? List.of() : hardcoreCurrencyLossScopes;
    }

    public float getBaseXpMultiplier() {
        return baseXpMultiplier;
    }
}
