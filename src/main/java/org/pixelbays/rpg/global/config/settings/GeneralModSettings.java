package org.pixelbays.rpg.global.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class GeneralModSettings {

    private static final FunctionCodec<RollModifierRange[], List<RollModifierRange>> ROLL_MODIFIER_RANGE_LIST_CODEC =
            new FunctionCodec<>(
                    new ArrayCodec<>(RollModifierRange.CODEC, RollModifierRange[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(RollModifierRange[]::new));

    public enum DebuggingMode {
        None,
        Min,
        Max,
        DeveloperDontUse
    }

    public static class RollModifierRange {
        public static final BuilderCodec<RollModifierRange> CODEC = BuilderCodec
                .builder(RollModifierRange.class, RollModifierRange::new)
                .append(new KeyedCodec<>("Min", Codec.INTEGER),
                        (i, s) -> i.minInclusive = s, i -> i.minInclusive)
                .add()
                .append(new KeyedCodec<>("Max", Codec.INTEGER),
                        (i, s) -> i.maxInclusive = s, i -> i.maxInclusive)
                .add()
                .append(new KeyedCodec<>("Modifier", Codec.INTEGER),
                        (i, s) -> i.modifier = s, i -> i.modifier)
                .add()
                .build();

        private int minInclusive;
        private int maxInclusive;
        private int modifier;

        public RollModifierRange() {
            this.minInclusive = 0;
            this.maxInclusive = 0;
            this.modifier = 0;
        }

        public int getMinInclusive() {
            return minInclusive;
        }

        public int getMaxInclusive() {
            return maxInclusive;
        }

        public int getModifier() {
            return modifier;
        }
    }

    public static final BuilderCodec<GeneralModSettings> CODEC = BuilderCodec
            .builder(GeneralModSettings.class, GeneralModSettings::new)
            .append(new KeyedCodec<>("ServerName", Codec.STRING, false, true),
                    (i, s) -> i.serverName = s, i -> i.serverName)
            .add()
            .append(new KeyedCodec<>("DiscordJoin", Codec.STRING, false, true),
                    (i, s) -> i.discordJoin = s, i -> i.discordJoin)
            .add()
            .append(new KeyedCodec<>("Website", Codec.STRING, false, true),
                    (i, s) -> i.website = s, i -> i.website)
            .add()
            .append(new KeyedCodec<>("AdvantageRollModifiers",
                    new MapCodec<>(ROLL_MODIFIER_RANGE_LIST_CODEC, HashMap::new, false), true),
                    (i, s) -> i.advantageRollModifiers = s, i -> i.advantageRollModifiers)
            .add()
            .append(new KeyedCodec<>("DebuggingMode", new EnumCodec<>(DebuggingMode.class), false, true),
                    (i, s) -> i.debuggingMode = s, i -> i.debuggingMode)
            .add()
            .append(new KeyedCodec<>("PlayerLogging", Codec.BOOLEAN, false, true),
                    (i, s) -> i.playerLogging = s, i -> i.playerLogging)
            .add()
            .append(new KeyedCodec<>("AntiGrindMod", Codec.BOOLEAN, false, true),
                    (i, s) -> i.antiGrindMod = s, i -> i.antiGrindMod)
            .add()
            .append(new KeyedCodec<>("RequireRaceAtStart", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireRaceAtStart = s, i -> i.requireRaceAtStart)
            .add()
            .append(new KeyedCodec<>("GlobalMobScaling", Codec.BOOLEAN, false, true),
                    (i, s) -> i.globalMobScaling = s, i -> i.globalMobScaling)
            .add()
            .build();

    private String serverName;
    private String discordJoin;
    private String website;
    private Map<String, List<RollModifierRange>> advantageRollModifiers;
    private DebuggingMode debuggingMode;
    private boolean playerLogging;
    private boolean antiGrindMod;
    private boolean requireRaceAtStart;
    private boolean globalMobScaling;

    public GeneralModSettings() {
        this.serverName = "";
        this.discordJoin = "";
        this.website = "";
        this.advantageRollModifiers = new HashMap<>();
        this.debuggingMode = DebuggingMode.None;
        this.playerLogging = false;
        this.antiGrindMod = false;
        this.requireRaceAtStart = false;
        this.globalMobScaling = false;
    }

    public String getServerName() {
        return serverName;
    }

    public String getDiscordJoin() {
        return discordJoin;
    }

    public String getWebsite() {
        return website;
    }

    public Map<String, List<RollModifierRange>> getAdvantageRollModifiers() {
        return advantageRollModifiers;
    }

    public DebuggingMode getDebuggingMode() {
        return debuggingMode;
    }

    public boolean isPlayerLogging() {
        return playerLogging;
    }

    public boolean isAntiGrindMod() {
        return antiGrindMod;
    }

    public boolean isRequireRaceAtStart() {
        return requireRaceAtStart;
    }

    public boolean isGlobalMobScaling() {
        return globalMobScaling;
    }
}
