package org.pixelbays.rpg.character.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.prereq.PrerequisiteRequirements;

import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class CharacterModSettings {

    public static final int HARD_MAX_CHARACTER_SLOTS = 50;

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));
        private static final FunctionCodec<ConditionalSlotGrant[], List<ConditionalSlotGrant>> SLOT_GRANT_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(ConditionalSlotGrant.CODEC, ConditionalSlotGrant[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? null : list.toArray(ConditionalSlotGrant[]::new));

    public enum DeletionMode {
        HardDelete,
        SoftDeleteRecovery
    }

    public enum LobbyBackgroundMode {
        Disabled,
        SingleBackground,
        RaceSpecific
    }

    public enum PreviewCameraMode {
        Vanilla,
        SharedPreset,
        RaceSpecific
    }

    public enum BarberPricingMode {
        Flat,
        PerChangedSlot
    }

    public enum LoginSpawnMode {
        LastSavedLocation,
        LocalSafeSpot,
        WorldSpawn
    }

    public static final BuilderCodec<CharacterModSettings> CODEC = BuilderCodec
            .builder(CharacterModSettings.class, CharacterModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("MaxCharacterSlots", Codec.INTEGER, false, true),
                    (i, s) -> i.maxCharacterSlots = s, i -> i.maxCharacterSlots)
            .add()
                .append(new KeyedCodec<>("DefaultCharacterSlots", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultCharacterSlots = s, i -> i.defaultCharacterSlots)
                .add()
                .append(new KeyedCodec<>("ExtraCharacterSlotTokenId", Codec.STRING, false, true),
                    (i, s) -> i.extraCharacterSlotTokenId = s, i -> i.extraCharacterSlotTokenId)
                .add()
                .append(new KeyedCodec<>("ConditionalSlotGrants", SLOT_GRANT_LIST_CODEC, false, true),
                    (i, s) -> i.conditionalSlotGrants = s, i -> i.conditionalSlotGrants)
                .add()
            .append(new KeyedCodec<>("AllowCharacterCreation", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowCharacterCreation = s, i -> i.allowCharacterCreation)
            .add()
            .append(new KeyedCodec<>("AllowCharacterDeletion", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowCharacterDeletion = s, i -> i.allowCharacterDeletion)
            .add()
            .append(new KeyedCodec<>("AllowLogoutToCharacterSelect", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowLogoutToCharacterSelect = s, i -> i.allowLogoutToCharacterSelect)
            .add()
            .append(new KeyedCodec<>("DeletionMode", new EnumCodec<>(DeletionMode.class), false, true),
                    (i, s) -> i.deletionMode = s, i -> i.deletionMode)
            .add()
            .append(new KeyedCodec<>("SoftDeleteRecoveryDays", Codec.INTEGER, false, true),
                    (i, s) -> i.softDeleteRecoveryDays = s, i -> i.softDeleteRecoveryDays)
            .add()
                .append(new KeyedCodec<>("DeletedCharacterRetentionHours", Codec.INTEGER, false, true),
                    (i, s) -> i.deletedCharacterRetentionHours = s, i -> i.deletedCharacterRetentionHours)
                .add()
                .append(new KeyedCodec<>("MaxRecoveriesPerWindow", Codec.INTEGER, false, true),
                    (i, s) -> i.maxRecoveriesPerWindow = s, i -> i.maxRecoveriesPerWindow)
                .add()
                .append(new KeyedCodec<>("RecoveryWindowHours", Codec.INTEGER, false, true),
                    (i, s) -> i.recoveryWindowHours = s, i -> i.recoveryWindowHours)
                .add()
                .append(new KeyedCodec<>("LogoutTimerSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.logoutTimerSeconds = s, i -> i.logoutTimerSeconds)
                .add()
                .append(new KeyedCodec<>("LoginSpawnMode", new EnumCodec<>(LoginSpawnMode.class), false, true),
                    (i, s) -> i.loginSpawnMode = s, i -> i.loginSpawnMode)
                .add()
                .append(new KeyedCodec<>("LoginVfxId", Codec.STRING, false, true),
                    CharacterModSettings::applyLegacyLoginVfxId, i -> i.loginVfxId)
                .add()
                .append(new KeyedCodec<>("LogoutVfxId", Codec.STRING, false, true),
                    CharacterModSettings::applyLegacyLogoutVfxId, i -> i.logoutVfxId)
                .add()
                .append(new KeyedCodec<>("LoginVfx", ModelParticle.ARRAY_CODEC, false, true),
                    (i, s) -> i.loginVfx = s, i -> i.loginVfx)
                .add()
                .append(new KeyedCodec<>("LogoutVfx", ModelParticle.ARRAY_CODEC, false, true),
                    (i, s) -> i.logoutVfx = s, i -> i.logoutVfx)
                .add()
            .append(new KeyedCodec<>("AutoMigrateLegacyPlayerData", Codec.BOOLEAN, false, true),
                    (i, s) -> i.autoMigrateLegacyPlayerData = s, i -> i.autoMigrateLegacyPlayerData)
            .add()
            .append(new KeyedCodec<>("LegacyMigrationCharacterName", Codec.STRING, false, true),
                    (i, s) -> i.legacyMigrationCharacterName = s, i -> i.legacyMigrationCharacterName)
            .add()
            .append(new KeyedCodec<>("RequireUniqueCharacterNames", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireUniqueCharacterNames = s, i -> i.requireUniqueCharacterNames)
            .add()
            .append(new KeyedCodec<>("MinCharacterNameLength", Codec.INTEGER, false, true),
                    (i, s) -> i.minCharacterNameLength = s, i -> i.minCharacterNameLength)
            .add()
            .append(new KeyedCodec<>("MaxCharacterNameLength", Codec.INTEGER, false, true),
                    (i, s) -> i.maxCharacterNameLength = s, i -> i.maxCharacterNameLength)
            .add()
            .append(new KeyedCodec<>("ReservedCharacterNames", STRING_LIST_CODEC, false, true),
                    (i, s) -> i.reservedCharacterNames = s, i -> i.reservedCharacterNames)
            .add()
            .append(new KeyedCodec<>("RequireRaceOnCreation", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireRaceOnCreation = s, i -> i.requireRaceOnCreation)
            .add()
            .append(new KeyedCodec<>("RequireStarterClassOnCreation", Codec.BOOLEAN, false, true),
                    (i, s) -> i.requireStarterClassOnCreation = s, i -> i.requireStarterClassOnCreation)
            .add()
            .append(new KeyedCodec<>("UseSharedLobbyWorld", Codec.BOOLEAN, false, true),
                    (i, s) -> i.useLobbyInstance = s, i -> i.useLobbyInstance)
            .add()
            .append(new KeyedCodec<>("LobbyWorldId", Codec.STRING, false, true),
                    (i, s) -> i.lobbyInstanceTemplateId = s, i -> i.lobbyInstanceTemplateId)
                .add()
                .append(new KeyedCodec<>("UseLobbyInstance", Codec.BOOLEAN, false, true),
                    (i, s) -> i.useLobbyInstance = s, i -> i.useLobbyInstance)
                .add()
                .append(new KeyedCodec<>("LobbyInstanceTemplateId", Codec.STRING, false, true),
                    (i, s) -> i.lobbyInstanceTemplateId = s, i -> i.lobbyInstanceTemplateId)
            .add()
            .append(new KeyedCodec<>("DefaultLobbySpawnPointId", Codec.STRING, false, true),
                    (i, s) -> i.defaultLobbySpawnPointId = s, i -> i.defaultLobbySpawnPointId)
            .add()
            .append(new KeyedCodec<>("UseRaceSpecificSpawnPoints", Codec.BOOLEAN, false, true),
                    (i, s) -> i.useRaceSpecificSpawnPoints = s, i -> i.useRaceSpecificSpawnPoints)
            .add()
            .append(new KeyedCodec<>("RaceLobbySpawnPointIds", new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (i, s) -> i.raceLobbySpawnPointIds = s, i -> i.raceLobbySpawnPointIds)
            .add()
            .append(new KeyedCodec<>("LobbyBackgroundMode", new EnumCodec<>(LobbyBackgroundMode.class), false, true),
                    (i, s) -> i.lobbyBackgroundMode = s, i -> i.lobbyBackgroundMode)
            .add()
            .append(new KeyedCodec<>("DefaultLobbyBackgroundId", Codec.STRING, false, true),
                    (i, s) -> i.defaultLobbyBackgroundId = s, i -> i.defaultLobbyBackgroundId)
            .add()
            .append(new KeyedCodec<>("RaceLobbyBackgroundIds", new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (i, s) -> i.raceLobbyBackgroundIds = s, i -> i.raceLobbyBackgroundIds)
            .add()
            .append(new KeyedCodec<>("PreviewCameraMode", new EnumCodec<>(PreviewCameraMode.class), false, true),
                    (i, s) -> i.previewCameraMode = s, i -> i.previewCameraMode)
            .add()
            .append(new KeyedCodec<>("DefaultPreviewCameraPresetId", Codec.STRING, false, true),
                    (i, s) -> i.defaultPreviewCameraPresetId = s, i -> i.defaultPreviewCameraPresetId)
            .add()
            .append(new KeyedCodec<>("RacePreviewCameraPresetIds", new MapCodec<>(Codec.STRING, HashMap::new, false), true),
                    (i, s) -> i.racePreviewCameraPresetIds = s, i -> i.racePreviewCameraPresetIds)
            .add()
                .append(new KeyedCodec<>("BarberShopSettings", BarberShopSettings.CODEC, false, true),
                    (i, s) -> i.barberShopSettings = s, i -> i.barberShopSettings)
                .add()
            .build();

    private boolean enabled;
    private int maxCharacterSlots;
    private int defaultCharacterSlots;
    private String extraCharacterSlotTokenId;
    private List<ConditionalSlotGrant> conditionalSlotGrants;
    private boolean allowCharacterCreation;
    private boolean allowCharacterDeletion;
    private boolean allowLogoutToCharacterSelect;
    private DeletionMode deletionMode;
    private int softDeleteRecoveryDays;
    private int deletedCharacterRetentionHours;
    private int maxRecoveriesPerWindow;
    private int recoveryWindowHours;
    private int logoutTimerSeconds;
    private LoginSpawnMode loginSpawnMode;
    private String loginVfxId;
    private String logoutVfxId;
    private ModelParticle[] loginVfx;
    private ModelParticle[] logoutVfx;
    private boolean autoMigrateLegacyPlayerData;
    private String legacyMigrationCharacterName;
    private boolean requireUniqueCharacterNames;
    private int minCharacterNameLength;
    private int maxCharacterNameLength;
    private List<String> reservedCharacterNames;
    private boolean requireRaceOnCreation;
    private boolean requireStarterClassOnCreation;
    private boolean useLobbyInstance;
    private String lobbyInstanceTemplateId;
    private String defaultLobbySpawnPointId;
    private boolean useRaceSpecificSpawnPoints;
    private Map<String, String> raceLobbySpawnPointIds;
    private LobbyBackgroundMode lobbyBackgroundMode;
    private String defaultLobbyBackgroundId;
    private Map<String, String> raceLobbyBackgroundIds;
    private PreviewCameraMode previewCameraMode;
    private String defaultPreviewCameraPresetId;
    private Map<String, String> racePreviewCameraPresetIds;
    private BarberShopSettings barberShopSettings;

    public CharacterModSettings() {
        this.enabled = true;
        this.maxCharacterSlots = 6;
        this.defaultCharacterSlots = 6;
        this.extraCharacterSlotTokenId = "ExtraCharacterSlot";
        this.conditionalSlotGrants = new ArrayList<>();
        this.allowCharacterCreation = true;
        this.allowCharacterDeletion = true;
        this.allowLogoutToCharacterSelect = true;
        this.deletionMode = DeletionMode.HardDelete;
        this.softDeleteRecoveryDays = 14;
        this.deletedCharacterRetentionHours = 14 * 24;
        this.maxRecoveriesPerWindow = 3;
        this.recoveryWindowHours = 24;
        this.logoutTimerSeconds = 0;
        this.loginSpawnMode = LoginSpawnMode.LastSavedLocation;
        this.loginVfxId = "";
        this.logoutVfxId = "";
        this.loginVfx = new ModelParticle[0];
        this.logoutVfx = new ModelParticle[0];
        this.autoMigrateLegacyPlayerData = true;
        this.legacyMigrationCharacterName = "Legacy Character";
        this.requireUniqueCharacterNames = false;
        this.minCharacterNameLength = 3;
        this.maxCharacterNameLength = 24;
        this.reservedCharacterNames = new ArrayList<>();
        this.requireRaceOnCreation = true;
        this.requireStarterClassOnCreation = true;
        this.useLobbyInstance = true;
        this.lobbyInstanceTemplateId = "Default_Void";
        this.defaultLobbySpawnPointId = "default";
        this.useRaceSpecificSpawnPoints = false;
        this.raceLobbySpawnPointIds = new HashMap<>();
        this.lobbyBackgroundMode = LobbyBackgroundMode.RaceSpecific;
        this.defaultLobbyBackgroundId = "";
        this.raceLobbyBackgroundIds = new HashMap<>();
        this.previewCameraMode = PreviewCameraMode.RaceSpecific;
        this.defaultPreviewCameraPresetId = "";
        this.racePreviewCameraPresetIds = new HashMap<>();
        this.barberShopSettings = new BarberShopSettings();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxCharacterSlots() {
        return Math.max(1, Math.min(HARD_MAX_CHARACTER_SLOTS, maxCharacterSlots));
    }

    public int getDefaultCharacterSlots() {
        return Math.max(1, Math.min(getMaxCharacterSlots(), defaultCharacterSlots));
    }

    public String getExtraCharacterSlotTokenId() {
        return extraCharacterSlotTokenId == null ? "" : extraCharacterSlotTokenId;
    }

    public List<ConditionalSlotGrant> getConditionalSlotGrants() {
        return conditionalSlotGrants == null ? List.of() : conditionalSlotGrants;
    }

    public boolean isAllowCharacterCreation() {
        return allowCharacterCreation;
    }

    public boolean isAllowCharacterDeletion() {
        return allowCharacterDeletion;
    }

    public boolean isAllowLogoutToCharacterSelect() {
        return allowLogoutToCharacterSelect;
    }

    public DeletionMode getDeletionMode() {
        return deletionMode;
    }

    public int getSoftDeleteRecoveryDays() {
        return softDeleteRecoveryDays;
    }

    public int getDeletedCharacterRetentionHours() {
        return deletedCharacterRetentionHours > 0 ? deletedCharacterRetentionHours : Math.max(0, softDeleteRecoveryDays) * 24;
    }

    public int getMaxRecoveriesPerWindow() {
        return maxRecoveriesPerWindow;
    }

    public int getRecoveryWindowHours() {
        return recoveryWindowHours;
    }

    public int getLogoutTimerSeconds() {
        return logoutTimerSeconds;
    }

    public LoginSpawnMode getLoginSpawnMode() {
        return loginSpawnMode == null ? LoginSpawnMode.LastSavedLocation : loginSpawnMode;
    }

    public String getLoginVfxId() {
        return loginVfxId;
    }

    public String getLogoutVfxId() {
        return logoutVfxId;
    }

    public ModelParticle[] getLoginVfx() {
        return loginVfx == null ? new ModelParticle[0] : loginVfx;
    }

    public ModelParticle[] getLogoutVfx() {
        return logoutVfx == null ? new ModelParticle[0] : logoutVfx;
    }

    public boolean isAutoMigrateLegacyPlayerData() {
        return autoMigrateLegacyPlayerData;
    }

    public String getLegacyMigrationCharacterName() {
        return legacyMigrationCharacterName;
    }

    public boolean isRequireUniqueCharacterNames() {
        return requireUniqueCharacterNames;
    }

    public int getMinCharacterNameLength() {
        return minCharacterNameLength;
    }

    public int getMaxCharacterNameLength() {
        return maxCharacterNameLength;
    }

    public List<String> getReservedCharacterNames() {
        return reservedCharacterNames;
    }

    public boolean isRequireRaceOnCreation() {
        return requireRaceOnCreation;
    }

    public boolean isRequireStarterClassOnCreation() {
        return requireStarterClassOnCreation;
    }

    public boolean isUseLobbyInstance() {
        return useLobbyInstance;
    }

    public String getLobbyInstanceTemplateId() {
        return lobbyInstanceTemplateId;
    }

    public boolean isUseSharedLobbyWorld() {
        return useLobbyInstance;
    }

    public String getLobbyWorldId() {
        return lobbyInstanceTemplateId;
    }

    public String getDefaultLobbySpawnPointId() {
        return defaultLobbySpawnPointId;
    }

    public boolean isUseRaceSpecificSpawnPoints() {
        return useRaceSpecificSpawnPoints;
    }

    public Map<String, String> getRaceLobbySpawnPointIds() {
        return raceLobbySpawnPointIds;
    }

    public LobbyBackgroundMode getLobbyBackgroundMode() {
        return lobbyBackgroundMode;
    }

    public String getDefaultLobbyBackgroundId() {
        return defaultLobbyBackgroundId;
    }

    public Map<String, String> getRaceLobbyBackgroundIds() {
        return raceLobbyBackgroundIds;
    }

    public PreviewCameraMode getPreviewCameraMode() {
        return previewCameraMode;
    }

    public String getDefaultPreviewCameraPresetId() {
        return defaultPreviewCameraPresetId;
    }

    public Map<String, String> getRacePreviewCameraPresetIds() {
        return racePreviewCameraPresetIds;
    }

    public BarberShopSettings getBarberShopSettings() {
        return barberShopSettings == null ? new BarberShopSettings() : barberShopSettings;
    }

    public boolean usesSoftDeleteRecovery() {
        return deletionMode == DeletionMode.SoftDeleteRecovery;
    }

    private void applyLegacyLoginVfxId(String value) {
        this.loginVfxId = value;
        if (!hasConfiguredVfx(this.loginVfx)) {
            this.loginVfx = createLegacyVfx(value);
        }
    }

    private void applyLegacyLogoutVfxId(String value) {
        this.logoutVfxId = value;
        if (!hasConfiguredVfx(this.logoutVfx)) {
            this.logoutVfx = createLegacyVfx(value);
        }
    }

    private static boolean hasConfiguredVfx(ModelParticle[] particles) {
        return particles != null && particles.length > 0;
    }

    private static ModelParticle[] createLegacyVfx(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return new ModelParticle[0];
        }

        return new ModelParticle[] {
                new ModelParticle(systemId, EntityPart.Self, null, null, 1.0f, null, null, false)
        };
    }

    public static class BarberShopSettings {

        public static final BuilderCodec<BarberShopSettings> CODEC = BuilderCodec
                .builder(BarberShopSettings.class, BarberShopSettings::new)
                .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                        (i, s) -> i.enabled = s, i -> i.enabled)
                .add()
                .append(new KeyedCodec<>("CurrencyScope", new EnumCodec<>(CurrencyScope.class), false, true),
                        (i, s) -> i.currencyScope = s, i -> i.currencyScope)
                .add()
                .append(new KeyedCodec<>("PricingMode", new EnumCodec<>(BarberPricingMode.class), false, true),
                        (i, s) -> i.pricingMode = s, i -> i.pricingMode)
                .add()
                .append(new KeyedCodec<>("FlatCost", CurrencyAmountDefinition.CODEC, false, true),
                        (i, s) -> i.flatCost = s, i -> i.flatCost)
                .add()
                .append(new KeyedCodec<>("PerChangedSlotCost", CurrencyAmountDefinition.CODEC, false, true),
                        (i, s) -> i.perChangedSlotCost = s, i -> i.perChangedSlotCost)
                .add()
                .build();

        private boolean enabled;
        private CurrencyScope currencyScope;
        private BarberPricingMode pricingMode;
        private CurrencyAmountDefinition flatCost;
        private CurrencyAmountDefinition perChangedSlotCost;

        public BarberShopSettings() {
            this.enabled = true;
            this.currencyScope = CurrencyScope.Character;
            this.pricingMode = BarberPricingMode.Flat;
            this.flatCost = new CurrencyAmountDefinition();
            this.perChangedSlotCost = new CurrencyAmountDefinition();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public CurrencyScope getCurrencyScope() {
            return currencyScope == null ? CurrencyScope.Character : currencyScope;
        }

        public BarberPricingMode getPricingMode() {
            return pricingMode == null ? BarberPricingMode.Flat : pricingMode;
        }

        public CurrencyAmountDefinition getFlatCost() {
            return flatCost == null ? new CurrencyAmountDefinition() : flatCost;
        }

        public CurrencyAmountDefinition getPerChangedSlotCost() {
            return perChangedSlotCost == null ? new CurrencyAmountDefinition() : perChangedSlotCost;
        }
    }

    public static class ConditionalSlotGrant {

        public static final BuilderCodec<ConditionalSlotGrant> CODEC = BuilderCodec
                .builder(ConditionalSlotGrant.class, ConditionalSlotGrant::new)
                .append(new KeyedCodec<>("SlotCount", Codec.INTEGER, false, true),
                        (i, s) -> i.slotCount = s, i -> i.slotCount)
                .add()
                .append(new KeyedCodec<>("Prerequisites", PrerequisiteRequirements.CODEC, false, true),
                        (i, s) -> i.prerequisites = s, i -> i.prerequisites)
                .add()
                .build();

        private int slotCount;
        private PrerequisiteRequirements prerequisites;

        public ConditionalSlotGrant() {
            this.slotCount = 1;
            this.prerequisites = new PrerequisiteRequirements();
        }

        public int getSlotCount() {
            return Math.max(0, slotCount);
        }

        public PrerequisiteRequirements getPrerequisites() {
            return prerequisites == null ? new PrerequisiteRequirements() : prerequisites;
        }
    }
}
