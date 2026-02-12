package org.pixelbays.rpg.global.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.guild.GuildJoinPolicy;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;

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

        private static final FunctionCodec<RollModifierRange[], List<RollModifierRange>> ROLL_MODIFIER_RANGE_LIST_CODEC =
            new FunctionCodec<>(
                new ArrayCodec<>(RollModifierRange.CODEC, RollModifierRange[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? null : list.toArray(RollModifierRange[]::new));

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

    public enum InventoryHandlingMode {
        Vanilla,
        Item,
        Strength
    }

    public enum PartyXpGrantingMode {
        SplitEqualInRange,
        FullInRange
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

        public static class CameraSettings {
        public static final BuilderCodec<CameraSettings> CODEC = BuilderCodec
            .builder(CameraSettings.class, CameraSettings::new)
            .append(new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, false, true),
                (i, s) -> i.positionLerpSpeed = s, i -> i.positionLerpSpeed)
            .add()
            .append(new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, false, true),
                (i, s) -> i.rotationLerpSpeed = s, i -> i.rotationLerpSpeed)
            .add()
            .append(new KeyedCodec<>("Distance", Codec.FLOAT, false, true),
                (i, s) -> i.distance = s, i -> i.distance)
            .add()
            .append(new KeyedCodec<>("DisplayCursor", Codec.BOOLEAN, false, true),
                (i, s) -> i.displayCursor = s, i -> i.displayCursor)
            .add()
            .append(new KeyedCodec<>("IsFirstPerson", Codec.BOOLEAN, false, true),
                (i, s) -> i.isFirstPerson = s, i -> i.isFirstPerson)
            .add()
            .append(new KeyedCodec<>("MovementForceRotationType",
                new EnumCodec<>(MovementForceRotationType.class), false, true),
                (i, s) -> i.movementForceRotationType = s, i -> i.movementForceRotationType)
            .add()
            .append(new KeyedCodec<>("EyeOffset", Codec.BOOLEAN, false, true),
                (i, s) -> i.eyeOffset = s, i -> i.eyeOffset)
            .add()
            .append(new KeyedCodec<>("PositionDistanceOffsetType",
                new EnumCodec<>(PositionDistanceOffsetType.class), false, true),
                (i, s) -> i.positionDistanceOffsetType = s, i -> i.positionDistanceOffsetType)
            .add()
            .append(new KeyedCodec<>("RotationType", new EnumCodec<>(RotationType.class), false, true),
                (i, s) -> i.rotationType = s, i -> i.rotationType)
            .add()
            .append(new KeyedCodec<>("RotationPitchRadians", Codec.FLOAT, false, true),
                (i, s) -> i.rotationPitchRadians = s, i -> i.rotationPitchRadians)
            .add()
            .append(new KeyedCodec<>("RotationYawRadians", Codec.FLOAT, false, true),
                (i, s) -> i.rotationYawRadians = s, i -> i.rotationYawRadians)
            .add()
            .append(new KeyedCodec<>("RotationRollRadians", Codec.FLOAT, false, true),
                (i, s) -> i.rotationRollRadians = s, i -> i.rotationRollRadians)
            .add()
            .append(new KeyedCodec<>("MouseInputType", new EnumCodec<>(MouseInputType.class), false, true),
                (i, s) -> i.mouseInputType = s, i -> i.mouseInputType)
            .add()
            .append(new KeyedCodec<>("PlaneNormalX", Codec.FLOAT, false, true),
                (i, s) -> i.planeNormalX = s, i -> i.planeNormalX)
            .add()
            .append(new KeyedCodec<>("PlaneNormalY", Codec.FLOAT, false, true),
                (i, s) -> i.planeNormalY = s, i -> i.planeNormalY)
            .add()
            .append(new KeyedCodec<>("PlaneNormalZ", Codec.FLOAT, false, true),
                (i, s) -> i.planeNormalZ = s, i -> i.planeNormalZ)
            .add()
            .build();

        private float positionLerpSpeed;
        private float rotationLerpSpeed;
        private float distance;
        private boolean displayCursor;
        private boolean isFirstPerson;
        private MovementForceRotationType movementForceRotationType;
        private boolean eyeOffset;
        private PositionDistanceOffsetType positionDistanceOffsetType;
        private RotationType rotationType;
        private float rotationPitchRadians;
        private float rotationYawRadians;
        private float rotationRollRadians;
        private MouseInputType mouseInputType;
        private float planeNormalX;
        private float planeNormalY;
        private float planeNormalZ;

        public CameraSettings() {
            this.positionLerpSpeed = 0.2F;
            this.rotationLerpSpeed = 0.2F;
            this.distance = 20.0F;
            this.displayCursor = true;
            this.isFirstPerson = false;
            this.movementForceRotationType = MovementForceRotationType.Custom;
            this.eyeOffset = true;
            this.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
            this.rotationType = RotationType.Custom;
            this.rotationPitchRadians = 0.0F;
            this.rotationYawRadians = 0.0F;
            this.rotationRollRadians = 0.0F;
            this.mouseInputType = MouseInputType.LookAtPlane;
            this.planeNormalX = 0.0F;
            this.planeNormalY = 1.0F;
            this.planeNormalZ = 0.0F;
        }

        public static CameraSettings topDownDefaults() {
            CameraSettings settings = new CameraSettings();
            settings.distance = 20.0F;
            settings.rotationPitchRadians = 0.0F;
            settings.rotationYawRadians = (float) (-Math.PI / 2.0);
            return settings;
        }

        public static CameraSettings isometricDefaults() {
            CameraSettings settings = new CameraSettings();
            settings.distance = 18.0F;
            settings.rotationPitchRadians = (float) (Math.PI / 4.0);
            settings.rotationYawRadians = (float) (-Math.PI / 4.0);
            return settings;
        }

        public float getPositionLerpSpeed() {
            return positionLerpSpeed;
        }

        public float getRotationLerpSpeed() {
            return rotationLerpSpeed;
        }

        public float getDistance() {
            return distance;
        }

        public boolean isDisplayCursor() {
            return displayCursor;
        }

        public boolean isFirstPerson() {
            return isFirstPerson;
        }

        public MovementForceRotationType getMovementForceRotationType() {
            return movementForceRotationType;
        }

        public boolean isEyeOffset() {
            return eyeOffset;
        }

        public PositionDistanceOffsetType getPositionDistanceOffsetType() {
            return positionDistanceOffsetType;
        }

        public RotationType getRotationType() {
            return rotationType;
        }

        public float getRotationPitchRadians() {
            return rotationPitchRadians;
        }

        public float getRotationYawRadians() {
            return rotationYawRadians;
        }

        public float getRotationRollRadians() {
            return rotationRollRadians;
        }

        public MouseInputType getMouseInputType() {
            return mouseInputType;
        }

        public float getPlaneNormalX() {
            return planeNormalX;
        }

        public float getPlaneNormalY() {
            return planeNormalY;
        }

        public float getPlaneNormalZ() {
            return planeNormalZ;
        }
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

                .append(new KeyedCodec<>("PartyEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyEnabled = s, i -> i.partyEnabled)
                .add()
                .append(new KeyedCodec<>("PartyMaxSize", Codec.INTEGER, false, true),
                    (i, s) -> i.partyMaxSize = s, i -> i.partyMaxSize)
                .add()
                .append(new KeyedCodec<>("RaidEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.raidEnabled = s, i -> i.raidEnabled)
                .add()
                .append(new KeyedCodec<>("RaidMaxSize", Codec.INTEGER, false, true),
                    (i, s) -> i.raidMaxSize = s, i -> i.raidMaxSize)
                .add()
                .append(new KeyedCodec<>("PartyXpEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyXpEnabled = s, i -> i.partyXpEnabled)
                .add()
                .append(new KeyedCodec<>("PartyXpGrantingMode", new EnumCodec<>(PartyXpGrantingMode.class), false, true),
                    (i, s) -> i.partyXpGrantingMode = s, i -> i.partyXpGrantingMode)
                .add()
                .append(new KeyedCodec<>("PartyXpRangeBlocks", Codec.INTEGER, false, true),
                    (i, s) -> i.partyXpRangeBlocks = s, i -> i.partyXpRangeBlocks)
                .add()
                .append(new KeyedCodec<>("PartyXpMinMembersInRange", Codec.INTEGER, false, true),
                    (i, s) -> i.partyXpMinMembersInRange = s, i -> i.partyXpMinMembersInRange)
                .add()
                .append(new KeyedCodec<>("PartyNpcAllowed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyNpcAllowed = s, i -> i.partyNpcAllowed)
                .add()
                .append(new KeyedCodec<>("PartyPersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyPersistenceEnabled = s, i -> i.partyPersistenceEnabled)
                .add()
                .append(new KeyedCodec<>("PartyPersistenceIntervalSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.partyPersistenceIntervalSeconds = s, i -> i.partyPersistenceIntervalSeconds)
                .add()

                .append(new KeyedCodec<>("GuildEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.guildEnabled = s, i -> i.guildEnabled)
                .add()
                .append(new KeyedCodec<>("GuildMaxMembers", Codec.INTEGER, false, true),
                    (i, s) -> i.guildMaxMembers = s, i -> i.guildMaxMembers)
                .add()
                .append(new KeyedCodec<>("GuildInviteExpirySeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.guildInviteExpirySeconds = s, i -> i.guildInviteExpirySeconds)
                .add()
                .append(new KeyedCodec<>("GuildNameMinLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildNameMinLength = s, i -> i.guildNameMinLength)
                .add()
                .append(new KeyedCodec<>("GuildNameMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildNameMaxLength = s, i -> i.guildNameMaxLength)
                .add()
                .append(new KeyedCodec<>("GuildTagMinLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildTagMinLength = s, i -> i.guildTagMinLength)
                .add()
                .append(new KeyedCodec<>("GuildTagMaxLength", Codec.INTEGER, false, true),
                    (i, s) -> i.guildTagMaxLength = s, i -> i.guildTagMaxLength)
                .add()
                .append(new KeyedCodec<>("GuildDefaultJoinPolicy", new EnumCodec<>(GuildJoinPolicy.class), false, true),
                    (i, s) -> i.guildDefaultJoinPolicy = s, i -> i.guildDefaultJoinPolicy)
                .add()
                .append(new KeyedCodec<>("GuildPersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.guildPersistenceEnabled = s, i -> i.guildPersistenceEnabled)
                .add()
                .append(new KeyedCodec<>("GuildPersistenceIntervalSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.guildPersistenceIntervalSeconds = s, i -> i.guildPersistenceIntervalSeconds)
                .add()

            .append(new KeyedCodec<>("XPTagSplits",
                    new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                    (i, s) -> i.xpTagSplits = s, i -> i.xpTagSplits)
            .documentation("THIS IS A PLACEHOLDER DO NOT USE")
            .add()
                .append(new KeyedCodec<>("AdvantageRollModifiers",
                    new MapCodec<>(ROLL_MODIFIER_RANGE_LIST_CODEC, HashMap::new, false), true),
                    (i, s) -> i.advantageRollModifiers = s, i -> i.advantageRollModifiers)
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
                .append(new KeyedCodec<>("InventoryHandling", new EnumCodec<>(InventoryHandlingMode.class), false, true),
                    (i, s) -> i.inventoryHandling = s, i -> i.inventoryHandling)
                .add()
                .append(new KeyedCodec<>("DefaultInventorySize", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultInventorySize = s, i -> i.defaultInventorySize)
                .add()
                .append(new KeyedCodec<>("StrengthInventorySlots",
                    new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new), true),
                    (i, s) -> i.strengthInventorySlots = s, i -> i.strengthInventorySlots)
                .add()
                .append(new KeyedCodec<>("ExtraSlotsRingsEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsRingsEnabled = s, i -> i.extraSlotsRingsEnabled)
                .add()
                .append(new KeyedCodec<>("ExtraSlotsTrinketsEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsTrinketsEnabled = s, i -> i.extraSlotsTrinketsEnabled)
                .add()
                .append(new KeyedCodec<>("ExtraSlotsNeckEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsNeckEnabled = s, i -> i.extraSlotsNeckEnabled)
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
                .append(new KeyedCodec<>("CameraSettingsTopDown", CameraSettings.CODEC, false, true),
                    (i, s) -> i.cameraSettingsTopDown = s, i -> i.cameraSettingsTopDown)
                .add()
                .append(new KeyedCodec<>("CameraSettingsIsometric", CameraSettings.CODEC, false, true),
                    (i, s) -> i.cameraSettingsIsometric = s, i -> i.cameraSettingsIsometric)
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
    private Map<String, List<RollModifierRange>> advantageRollModifiers;
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
    private InventoryHandlingMode inventoryHandling;
    private int defaultInventorySize;
    private Object2IntMap<String> strengthInventorySlots;
    private boolean extraSlotsRingsEnabled;
    private boolean extraSlotsTrinketsEnabled;
    private boolean extraSlotsNeckEnabled;
    private boolean partyEnabled;
    private int partyMaxSize;
    private boolean raidEnabled;
    private int raidMaxSize;
    private boolean partyXpEnabled;
    private PartyXpGrantingMode partyXpGrantingMode;
    private int partyXpRangeBlocks;
    private int partyXpMinMembersInRange;
    private boolean partyNpcAllowed;
    private boolean partyPersistenceEnabled;
    private int partyPersistenceIntervalSeconds;
    private boolean guildEnabled;
    private int guildMaxMembers;
    private int guildInviteExpirySeconds;
    private int guildNameMinLength;
    private int guildNameMaxLength;
    private int guildTagMinLength;
    private int guildTagMaxLength;
    private GuildJoinPolicy guildDefaultJoinPolicy;
    private boolean guildPersistenceEnabled;
    private int guildPersistenceIntervalSeconds;
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
    private CameraSettings cameraSettingsTopDown;
    private CameraSettings cameraSettingsIsometric;

    public RpgModConfig() {
        this.id = "";
        this.classMode = ClassMode.SingleClass;
        this.activeClassMode = ActiveClassMode.Manual;
        this.xpRouting = XpRoutingMode.ActiveClassOnly;
        this.xpTags = new ArrayList<>();
        this.xpTagSplits = new Object2FloatOpenHashMap<>();
        this.advantageRollModifiers = new HashMap<>();
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
        this.inventoryHandling = InventoryHandlingMode.Vanilla;
        this.defaultInventorySize = 36;
        this.strengthInventorySlots = new Object2IntOpenHashMap<>();
        this.extraSlotsRingsEnabled = false;
        this.extraSlotsTrinketsEnabled = false;
        this.extraSlotsNeckEnabled = false;
        this.partyEnabled = true;
        this.partyMaxSize = 5;
        this.raidEnabled = true;
        this.raidMaxSize = 20;
        this.partyXpEnabled = true;
        this.partyXpGrantingMode = PartyXpGrantingMode.SplitEqualInRange;
        this.partyXpRangeBlocks = 48;
        this.partyXpMinMembersInRange = 1;
        this.partyNpcAllowed = true;
        this.partyPersistenceEnabled = false;
        this.partyPersistenceIntervalSeconds = 60;
        this.guildEnabled = true;
        this.guildMaxMembers = 50;
        this.guildInviteExpirySeconds = 3600;
        this.guildNameMinLength = 3;
        this.guildNameMaxLength = 24;
        this.guildTagMinLength = 2;
        this.guildTagMaxLength = 5;
        this.guildDefaultJoinPolicy = GuildJoinPolicy.INVITE_ONLY;
        this.guildPersistenceEnabled = true;
        this.guildPersistenceIntervalSeconds = 120;
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
        this.cameraSettingsTopDown = CameraSettings.topDownDefaults();
        this.cameraSettingsIsometric = CameraSettings.isometricDefaults();
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

    public Map<String, List<RollModifierRange>> getAdvantageRollModifiers() {
        return advantageRollModifiers;
    }

    public void setAdvantageRollModifiers(Map<String, List<RollModifierRange>> advantageRollModifiers) {
        this.advantageRollModifiers = advantageRollModifiers;
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

    public InventoryHandlingMode getInventoryHandling() {
        return inventoryHandling;
    }

    public void setInventoryHandling(InventoryHandlingMode inventoryHandling) {
        this.inventoryHandling = inventoryHandling;
    }

    public int getDefaultInventorySize() {
        return defaultInventorySize;
    }

    public void setDefaultInventorySize(int defaultInventorySize) {
        this.defaultInventorySize = defaultInventorySize;
    }

    public Object2IntMap<String> getStrengthInventorySlots() {
        return strengthInventorySlots;
    }

    public void setStrengthInventorySlots(Object2IntMap<String> strengthInventorySlots) {
        this.strengthInventorySlots = strengthInventorySlots;
    }

    public boolean isExtraSlotsRingsEnabled() {
        return extraSlotsRingsEnabled;
    }

    public void setExtraSlotsRingsEnabled(boolean extraSlotsRingsEnabled) {
        this.extraSlotsRingsEnabled = extraSlotsRingsEnabled;
    }

    public boolean isExtraSlotsTrinketsEnabled() {
        return extraSlotsTrinketsEnabled;
    }

    public void setExtraSlotsTrinketsEnabled(boolean extraSlotsTrinketsEnabled) {
        this.extraSlotsTrinketsEnabled = extraSlotsTrinketsEnabled;
    }

    public boolean isExtraSlotsNeckEnabled() {
        return extraSlotsNeckEnabled;
    }

    public void setExtraSlotsNeckEnabled(boolean extraSlotsNeckEnabled) {
        this.extraSlotsNeckEnabled = extraSlotsNeckEnabled;
    }

    public boolean isPartyEnabled() {
        return partyEnabled;
    }

    public void setPartyEnabled(boolean partyEnabled) {
        this.partyEnabled = partyEnabled;
    }

    public int getPartyMaxSize() {
        return partyMaxSize;
    }

    public void setPartyMaxSize(int partyMaxSize) {
        this.partyMaxSize = partyMaxSize;
    }

    public boolean isRaidEnabled() {
        return raidEnabled;
    }

    public void setRaidEnabled(boolean raidEnabled) {
        this.raidEnabled = raidEnabled;
    }

    public int getRaidMaxSize() {
        return raidMaxSize;
    }

    public void setRaidMaxSize(int raidMaxSize) {
        this.raidMaxSize = raidMaxSize;
    }

    public boolean isPartyXpEnabled() {
        return partyXpEnabled;
    }

    public void setPartyXpEnabled(boolean partyXpEnabled) {
        this.partyXpEnabled = partyXpEnabled;
    }

    public PartyXpGrantingMode getPartyXpGrantingMode() {
        return partyXpGrantingMode;
    }

    public void setPartyXpGrantingMode(PartyXpGrantingMode partyXpGrantingMode) {
        this.partyXpGrantingMode = partyXpGrantingMode;
    }

    public int getPartyXpRangeBlocks() {
        return partyXpRangeBlocks;
    }

    public void setPartyXpRangeBlocks(int partyXpRangeBlocks) {
        this.partyXpRangeBlocks = partyXpRangeBlocks;
    }

    public int getPartyXpMinMembersInRange() {
        return partyXpMinMembersInRange;
    }

    public void setPartyXpMinMembersInRange(int partyXpMinMembersInRange) {
        this.partyXpMinMembersInRange = partyXpMinMembersInRange;
    }

    public boolean isPartyNpcAllowed() {
        return partyNpcAllowed;
    }

    public void setPartyNpcAllowed(boolean partyNpcAllowed) {
        this.partyNpcAllowed = partyNpcAllowed;
    }

    public boolean isPartyPersistenceEnabled() {
        return partyPersistenceEnabled;
    }

    public void setPartyPersistenceEnabled(boolean partyPersistenceEnabled) {
        this.partyPersistenceEnabled = partyPersistenceEnabled;
    }

    public int getPartyPersistenceIntervalSeconds() {
        return partyPersistenceIntervalSeconds;
    }

    public void setPartyPersistenceIntervalSeconds(int partyPersistenceIntervalSeconds) {
        this.partyPersistenceIntervalSeconds = partyPersistenceIntervalSeconds;
    }

    public boolean isGuildEnabled() {
        return guildEnabled;
    }

    public void setGuildEnabled(boolean guildEnabled) {
        this.guildEnabled = guildEnabled;
    }

    public int getGuildMaxMembers() {
        return guildMaxMembers;
    }

    public void setGuildMaxMembers(int guildMaxMembers) {
        this.guildMaxMembers = guildMaxMembers;
    }

    public int getGuildInviteExpirySeconds() {
        return guildInviteExpirySeconds;
    }

    public void setGuildInviteExpirySeconds(int guildInviteExpirySeconds) {
        this.guildInviteExpirySeconds = guildInviteExpirySeconds;
    }

    public int getGuildNameMinLength() {
        return guildNameMinLength;
    }

    public void setGuildNameMinLength(int guildNameMinLength) {
        this.guildNameMinLength = guildNameMinLength;
    }

    public int getGuildNameMaxLength() {
        return guildNameMaxLength;
    }

    public void setGuildNameMaxLength(int guildNameMaxLength) {
        this.guildNameMaxLength = guildNameMaxLength;
    }

    public int getGuildTagMinLength() {
        return guildTagMinLength;
    }

    public void setGuildTagMinLength(int guildTagMinLength) {
        this.guildTagMinLength = guildTagMinLength;
    }

    public int getGuildTagMaxLength() {
        return guildTagMaxLength;
    }

    public void setGuildTagMaxLength(int guildTagMaxLength) {
        this.guildTagMaxLength = guildTagMaxLength;
    }

    public GuildJoinPolicy getGuildDefaultJoinPolicy() {
        return guildDefaultJoinPolicy;
    }

    public void setGuildDefaultJoinPolicy(GuildJoinPolicy guildDefaultJoinPolicy) {
        this.guildDefaultJoinPolicy = guildDefaultJoinPolicy;
    }

    public boolean isGuildPersistenceEnabled() {
        return guildPersistenceEnabled;
    }

    public void setGuildPersistenceEnabled(boolean guildPersistenceEnabled) {
        this.guildPersistenceEnabled = guildPersistenceEnabled;
    }

    public int getGuildPersistenceIntervalSeconds() {
        return guildPersistenceIntervalSeconds;
    }

    public void setGuildPersistenceIntervalSeconds(int guildPersistenceIntervalSeconds) {
        this.guildPersistenceIntervalSeconds = guildPersistenceIntervalSeconds;
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

    public CameraSettings getCameraSettingsTopDown() {
        return cameraSettingsTopDown;
    }

    public void setCameraSettingsTopDown(CameraSettings cameraSettingsTopDown) {
        this.cameraSettingsTopDown = cameraSettingsTopDown;
    }

    public CameraSettings getCameraSettingsIsometric() {
        return cameraSettingsIsometric;
    }

    public void setCameraSettingsIsometric(CameraSettings cameraSettingsIsometric) {
        this.cameraSettingsIsometric = cameraSettingsIsometric;
    }
}
