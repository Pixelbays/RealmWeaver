package org.pixelbays.rpg.global.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Master configuration for the RPG mod.
 * Loaded from /Server/RpgModConfig/{ConfigName}.json
 */
@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class RpgModConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, RpgModConfig>> {

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

    public enum DebuggingMode {
        None,
        Min,
        Max,
        DeveloperDontUse
    }

    public enum HardcoreLossType {
        ResetToZero,
        LosePercent
    }

    /**
     * Ability control type determines how players trigger abilities:
     * - Weapons: Primary (left-click) and Secondary (right-click) on weapons
     * - Hotbar: Designated hotbar slots trigger abilities
     * - AbilitySlots123: Use Ability1, Ability2, Ability3 interaction types
     */
    public enum AbilityControlType {
        Weapons,
        Hotbar,
        AbilitySlots123
    }

    /**
     * Targeting style determines how players select targets:
     * - Vanilla: Default Hytale targeting behavior
     * - TabTargeting: Cycle through nearby targets with tab key
     * - MOBA: Right-click to move and target enemies via mouse cursor or hitboxes
     * - PlayerConfig: Allow players to configure their own targeting preference
     */
    public enum TargetingStyle {
        Vanilla,
        TabTargeting,
        MOBA,
        PlayerConfig,
    }

    /**
     * Camera style determines how players control the camera:
     * - Vanilla: Default Hytale camera behavior
     * - ThirdPersonOnly: Force third-person camera view
     * - Isometric: Right-click to move and target enemies via mouse cursor or
     * hitboxes
     * - Top-Down: Right-click to move and target enemies via mouse cursor or
     * hitboxes
     * - PlayerConfig: Allow players to configure their own camera preference
     */
    public enum CameraStyle {
        Vanilla,
        ThirdPersonOnly,
        Isometric,
        TopDown,
        PlayerConfig,
    }

    public static final AssetBuilderCodec<String, RpgModConfig> CODEC = AssetBuilderCodec.builder(
            RpgModConfig.class,
            RpgModConfig::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("ServerName", Codec.STRING, false, true),
                    (i, s) -> i.serverName = s, i -> i.serverName)
            .add()
            .append(new KeyedCodec<>("DiscordJoin", Codec.STRING, false, true),
                    (i, s) -> i.discordJoin = s, i -> i.discordJoin)
            .add()
            .append(new KeyedCodec<>("Website", Codec.STRING, false, true),
                    (i, s) -> i.website = s, i -> i.website)
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

            .append(new KeyedCodec<>("XPTagSplits",
                    new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                    (i, s) -> i.xpTagSplits = s, i -> i.xpTagSplits)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .append(new KeyedCodec<>("BaseGlobalCooldown", Codec.INTEGER, false, true),
                    (i, s) -> i.baseGlobalCooldown = s, i -> i.baseGlobalCooldown)
            .add()
            .append(new KeyedCodec<>("GlobalCooldownCategories",
                    new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new), true),
                    (i, s) -> i.globalCooldownCategories = s, i -> i.globalCooldownCategories)
            .add()
            .append(new KeyedCodec<>("BaseXpMultiplier", Codec.FLOAT, false, true),
                    (i, s) -> i.baseXpMultiplier = s, i -> i.baseXpMultiplier)
            .add()
            .append(new KeyedCodec<>("DefaultPlayerProfileCount", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultPlayerProfileCount = s, i -> i.defaultPlayerProfileCount)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")

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
            .append(new KeyedCodec<>("DebuggingMode", new EnumCodec<>(DebuggingMode.class), false, true),
                    (i, s) -> i.debuggingMode = s, i -> i.debuggingMode)
            .add()
            .append(new KeyedCodec<>("PlayerLogging", Codec.BOOLEAN, false, true),
                    (i, s) -> i.playerLogging = s, i -> i.playerLogging)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .append(new KeyedCodec<>("AntiGrindMod", Codec.BOOLEAN, false, true),
                    (i, s) -> i.antiGrindMod = s, i -> i.antiGrindMod)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .append(new KeyedCodec<>("RequireClassAtStart", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireClassAtStart = s, i -> i.requireClassAtStart)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")

            .add()
            .append(new KeyedCodec<>("RequireRaceAtStart", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireRaceAtStart = s, i -> i.requireRaceAtStart)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .append(new KeyedCodec<>("GlobalMobScaling", Codec.BOOLEAN, false, true),
                    (i, s) -> i.globalMobScaling = s, i -> i.globalMobScaling)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .append(new KeyedCodec<>("AbilityControlType", new EnumCodec<>(AbilityControlType.class), false, true),
                    (i, s) -> i.abilityControlType = s, i -> i.abilityControlType)
            .add()
            .append(new KeyedCodec<>("HotbarAbilitySlots", Codec.INT_ARRAY, false, true),
                    (i, s) -> i.hotbarAbilitySlots = s, i -> i.hotbarAbilitySlots)
            .add()
            .append(new KeyedCodec<>("TargetingStyle", new EnumCodec<>(TargetingStyle.class), false, true),
                    (i, s) -> i.targetingStyle = s, i -> i.targetingStyle)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")

            .add()
            .append(new KeyedCodec<>("CameraStyle", new EnumCodec<>(CameraStyle.class), false, true),
                    (i, s) -> i.cameraStyle = s, i -> i.cameraStyle)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
            .build();

    private static DefaultAssetMap<String, RpgModConfig> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private ClassMode classMode;
    private ActiveClassMode activeClassMode;
    private XpRoutingMode xpRouting;
    private List<String> xpTags;
    private Object2FloatMap<String> xpTagSplits;
    private int baseGlobalCooldown;
    private Object2IntMap<String> globalCooldownCategories;
    private boolean restedXpEnabled;
    private int restedXpBonusPercent;
    private int restedXpConsumeRatio;
    private List<String> restedXpGainTags;
    private boolean hardcoreEnabled;
    private HardcoreLossType hardcoreLossType;
    private int hardcoreLevelLossPercent;
    private float baseXpMultiplier;
    private int defaultPlayerProfileCount;
    private int maxCombatClasses;
    private int maxProfessionClasses;
    private String classSwitchingRules;
    private DebuggingMode debuggingMode;
    private boolean playerLogging;
    private boolean antiGrindMod;
    private boolean requireClassAtStart;
    private boolean requireRaceAtStart;
    private boolean globalMobScaling;
    private String serverName;
    private String discordJoin;
    private String website;
    private AbilityControlType abilityControlType;
    private int[] hotbarAbilitySlots;
    private TargetingStyle targetingStyle;
    private CameraStyle cameraStyle;

    public RpgModConfig() {
        this.id = "";
        this.classMode = ClassMode.SingleClass;
        this.activeClassMode = ActiveClassMode.Manual;
        this.xpRouting = XpRoutingMode.ActiveClassOnly;
        this.xpTags = new ArrayList<>();
        this.xpTagSplits = new Object2FloatOpenHashMap<>();
        this.baseGlobalCooldown = 0;
        this.globalCooldownCategories = new Object2IntOpenHashMap<>();
        this.restedXpEnabled = false;
        this.restedXpBonusPercent = 0;
        this.restedXpConsumeRatio = 1;
        this.restedXpGainTags = new ArrayList<>();
        this.hardcoreEnabled = false;
        this.hardcoreLossType = HardcoreLossType.ResetToZero;
        this.hardcoreLevelLossPercent = 50;
        this.baseXpMultiplier = 1.0f;
        this.defaultPlayerProfileCount = 3;
        this.maxCombatClasses = 1;
        this.maxProfessionClasses = 2;
        this.classSwitchingRules = "";
        this.debuggingMode = DebuggingMode.None;
        this.playerLogging = false;
        this.antiGrindMod = false;
        this.requireClassAtStart = false;
        this.requireRaceAtStart = false;
        this.globalMobScaling = false;
        this.serverName = "";
        this.discordJoin = "";
        this.website = "";
        this.abilityControlType = AbilityControlType.Hotbar;
        this.hotbarAbilitySlots = new int[] { 6, 7, 8 }; // Slots 7, 8, 9 (keys 8, 9, 0) default
        this.targetingStyle = TargetingStyle.Vanilla;
        this.cameraStyle = CameraStyle.Vanilla;
    }

    public static DefaultAssetMap<String, RpgModConfig> getAssetMap() {
        if (ASSET_MAP == null) {
            var assetStore = AssetRegistry.getAssetStore(RpgModConfig.class);
            if (assetStore == null) {
                return null;
            }
            ASSET_MAP = (DefaultAssetMap<String, RpgModConfig>) assetStore.getAssetMap();
        }

        return ASSET_MAP;
    }

    @Override
    public String getId() {
        return id;
    }

    public ClassMode getClassMode() {
        return classMode;
    }

    public void setClassMode(ClassMode classMode) {
        this.classMode = classMode;
    }

    public ActiveClassMode getActiveClassMode() {
        return activeClassMode;
    }

    public void setActiveClassMode(ActiveClassMode activeClassMode) {
        this.activeClassMode = activeClassMode;
    }

    public XpRoutingMode getXpRouting() {
        return xpRouting;
    }

    public void setXpRouting(XpRoutingMode xpRouting) {
        this.xpRouting = xpRouting;
    }

    public List<String> getXpTags() {
        return xpTags;
    }

    public void setXpTags(List<String> xpTags) {
        this.xpTags = xpTags;
    }

    public Object2FloatMap<String> getXpTagSplits() {
        return xpTagSplits;
    }

    public void setXpTagSplits(Object2FloatMap<String> xpTagSplits) {
        this.xpTagSplits = xpTagSplits;
    }

    public int getBaseGlobalCooldown() {
        return baseGlobalCooldown;
    }

    public void setBaseGlobalCooldown(int baseGlobalCooldown) {
        this.baseGlobalCooldown = baseGlobalCooldown;
    }

    public Object2IntMap<String> getGlobalCooldownCategories() {
        return globalCooldownCategories;
    }

    public void setGlobalCooldownCategories(Object2IntMap<String> globalCooldownCategories) {
        this.globalCooldownCategories = globalCooldownCategories;
    }

    public boolean isRestedXpEnabled() {
        return restedXpEnabled;
    }

    public void setRestedXpEnabled(boolean restedXpEnabled) {
        this.restedXpEnabled = restedXpEnabled;
    }

    public int getRestedXpBonusPercent() {
        return restedXpBonusPercent;
    }

    public void setRestedXpBonusPercent(int restedXpBonusPercent) {
        this.restedXpBonusPercent = restedXpBonusPercent;
    }

    public int getRestedXpConsumeRatio() {
        return restedXpConsumeRatio;
    }

    public void setRestedXpConsumeRatio(int restedXpConsumeRatio) {
        this.restedXpConsumeRatio = restedXpConsumeRatio;
    }

    public List<String> getRestedXpGainTags() {
        return restedXpGainTags;
    }

    public void setRestedXpGainTags(List<String> restedXpGainTags) {
        this.restedXpGainTags = restedXpGainTags;
    }

    public boolean isHardcoreEnabled() {
        return hardcoreEnabled;
    }

    public void setHardcoreEnabled(boolean hardcoreEnabled) {
        this.hardcoreEnabled = hardcoreEnabled;
    }

    public HardcoreLossType getHardcoreLossType() {
        return hardcoreLossType;
    }

    public void setHardcoreLossType(HardcoreLossType hardcoreLossType) {
        this.hardcoreLossType = hardcoreLossType;
    }

    public int getHardcoreLevelLossPercent() {
        return hardcoreLevelLossPercent;
    }

    public void setHardcoreLevelLossPercent(int hardcoreLevelLossPercent) {
        this.hardcoreLevelLossPercent = hardcoreLevelLossPercent;
    }

    public float getBaseXpMultiplier() {
        return baseXpMultiplier;
    }

    public void setBaseXpMultiplier(float baseXpMultiplier) {
        this.baseXpMultiplier = baseXpMultiplier;
    }

    public int getDefaultPlayerProfileCount() {
        return defaultPlayerProfileCount;
    }

    public void setDefaultPlayerProfileCount(int defaultPlayerProfileCount) {
        this.defaultPlayerProfileCount = defaultPlayerProfileCount;
    }

    public int getMaxCombatClasses() {
        return maxCombatClasses;
    }

    public void setMaxCombatClasses(int maxCombatClasses) {
        this.maxCombatClasses = maxCombatClasses;
    }

    public int getMaxProfessionClasses() {
        return maxProfessionClasses;
    }

    public void setMaxProfessionClasses(int maxProfessionClasses) {
        this.maxProfessionClasses = maxProfessionClasses;
    }

    public String getClassSwitchingRules() {
        return classSwitchingRules;
    }

    public void setClassSwitchingRules(String classSwitchingRules) {
        this.classSwitchingRules = classSwitchingRules;
    }

    public DebuggingMode getDebuggingMode() {
        return debuggingMode;
    }

    public void setDebuggingMode(DebuggingMode debuggingMode) {
        this.debuggingMode = debuggingMode;
    }

    public boolean isPlayerLogging() {
        return playerLogging;
    }

    public void setPlayerLogging(boolean playerLogging) {
        this.playerLogging = playerLogging;
    }

    public boolean isAntiGrindMod() {
        return antiGrindMod;
    }

    public void setAntiGrindMod(boolean antiGrindMod) {
        this.antiGrindMod = antiGrindMod;
    }

    public boolean isRequireClassAtStart() {
        return requireClassAtStart;
    }

    public void setRequireClassAtStart(boolean requireClassAtStart) {
        this.requireClassAtStart = requireClassAtStart;
    }

    public boolean isRequireRaceAtStart() {
        return requireRaceAtStart;
    }

    public void setRequireRaceAtStart(boolean requireRaceAtStart) {
        this.requireRaceAtStart = requireRaceAtStart;
    }

    public boolean isGlobalMobScaling() {
        return globalMobScaling;
    }

    public void setGlobalMobScaling(boolean globalMobScaling) {
        this.globalMobScaling = globalMobScaling;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDiscordJoin() {
        return discordJoin;
    }

    public void setDiscordJoin(String discordJoin) {
        this.discordJoin = discordJoin;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public AbilityControlType getAbilityControlType() {
        return abilityControlType;
    }

    public void setAbilityControlType(AbilityControlType abilityControlType) {
        this.abilityControlType = abilityControlType;
    }

    public int[] getHotbarAbilitySlots() {
        return hotbarAbilitySlots;
    }

    public void setHotbarAbilitySlots(int[] hotbarAbilitySlots) {
        this.hotbarAbilitySlots = hotbarAbilitySlots;
    }

    public TargetingStyle getTargetingStyle() {
        return targetingStyle;
    }

    public void setTargetingStyle(TargetingStyle targetingStyle) {
        this.targetingStyle = targetingStyle;
    }

    public CameraStyle getCameraStyle() {
        return cameraStyle;
    }

    public void setCameraStyle(CameraStyle cameraStyle) {
        this.cameraStyle = cameraStyle;
    }
}
