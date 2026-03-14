package org.pixelbays.rpg.classes.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ClassModSettings {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    public enum ClassMode {
        SingleClass,
        MultiClass
    }

    public enum ActiveClassMode {
        Manual,
        AutoLastUsed,
        AutoHighestLevel,
        AutoByTag
    }

    public enum XpRoutingMode {
        ActiveClassOnly,
        SplitByTag,
        AllMatchingTags
    }

    public static final BuilderCodec<ClassModSettings> CODEC = BuilderCodec
            .builder(ClassModSettings.class, ClassModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("ClassMode", new EnumCodec<>(ClassMode.class), false, true),
                    (i, s) -> i.classMode = s, i -> i.classMode)
            .add()
            .append(new KeyedCodec<>("ActiveClassMode", new EnumCodec<>(ActiveClassMode.class), false, true),
                    (i, s) -> i.activeClassMode = s, i -> i.activeClassMode)
            .add()
            .append(new KeyedCodec<>("ClassTags", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.xpTags = s, i -> i.xpTags)
            .add()
            .append(new KeyedCodec<>("XPRouting", new EnumCodec<>(XpRoutingMode.class), false, true),
                    (i, s) -> i.xpRouting = s, i -> i.xpRouting)
            .add()
            .append(new KeyedCodec<>("XPTagSplits",
                    new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                    (i, s) -> i.xpTagSplits = s, i -> i.xpTagSplits)
            .add()
            .append(new KeyedCodec<>("MaxCombatClasses", Codec.INTEGER, false, true),
                    (i, s) -> i.maxCombatClasses = s, i -> i.maxCombatClasses)
            .add()
            .append(new KeyedCodec<>("MaxProfessionClasses", Codec.INTEGER, false, true),
                    (i, s) -> i.maxProfessionClasses = s, i -> i.maxProfessionClasses)
            .add()
            .append(new KeyedCodec<>("ClassSwitchingRules", Codec.STRING, false, true),
                    (i, s) -> i.classSwitchingRules = s, i -> i.classSwitchingRules)
            .add()
            .append(new KeyedCodec<>("RequireClassAtStart", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireClassAtStart = s, i -> i.requireClassAtStart)
            .add()
            .build();

    private boolean enabled;
    private ClassMode classMode;
    private ActiveClassMode activeClassMode;
    private XpRoutingMode xpRouting;
    private List<String> xpTags;
    private Object2FloatMap<String> xpTagSplits;
    private int maxCombatClasses;
    private int maxProfessionClasses;
    private String classSwitchingRules;
    private boolean requireClassAtStart;

    public ClassModSettings() {
        this.enabled = true;
        this.classMode = ClassMode.SingleClass;
        this.activeClassMode = ActiveClassMode.Manual;
        this.xpRouting = XpRoutingMode.ActiveClassOnly;
        this.xpTags = new ArrayList<>();
        this.xpTagSplits = new Object2FloatOpenHashMap<>();
        this.maxCombatClasses = 1;
        this.maxProfessionClasses = 2;
        this.classSwitchingRules = "";
        this.requireClassAtStart = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ClassMode getClassMode() {
        return classMode;
    }

    public ActiveClassMode getActiveClassMode() {
        return activeClassMode;
    }

    public XpRoutingMode getXpRouting() {
        return xpRouting;
    }

    public List<String> getXpTags() {
        return xpTags;
    }

    public Object2FloatMap<String> getXpTagSplits() {
        return xpTagSplits;
    }

    public int getMaxCombatClasses() {
        return maxCombatClasses;
    }

    public int getMaxProfessionClasses() {
        return maxProfessionClasses;
    }

    public String getClassSwitchingRules() {
        return classSwitchingRules;
    }

    public boolean isRequireClassAtStart() {
        return requireClassAtStart;
    }
}
