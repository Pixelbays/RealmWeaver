package org.pixelbays.rpg.global.config;

import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.achievement.config.settings.AchievementModSettings;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings;
import org.pixelbays.rpg.camera.config.settings.CameraModSettings;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings;
import org.pixelbays.rpg.classes.config.settings.ClassModSettings;
import org.pixelbays.rpg.classes.config.settings.TalentModSettings;
import org.pixelbays.rpg.economy.auctions.config.settings.AuctionHouseModSettings;
import org.pixelbays.rpg.economy.banks.config.settings.BankModSettings;
import org.pixelbays.rpg.economy.currency.config.settings.CurrencyModSettings;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.config.settings.GuildModSettings;
import org.pixelbays.rpg.inventory.config.settings.InventoryModSettings;
import org.pixelbays.rpg.item.config.settings.ItemModSettings;
import org.pixelbays.rpg.leveling.config.settings.LevelingModSettings;
import org.pixelbays.rpg.lockpicking.config.settings.LockpickingModSettings;
import org.pixelbays.rpg.mail.config.settings.MailModSettings;
import org.pixelbays.rpg.party.config.settings.PartyModSettings;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorSectionStart;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * Master configuration for the RPG mod.
 * Loaded from /Server/RpgModConfig/{ConfigName}.json
 */
public class RpgModConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, RpgModConfig>> {

    public static final AssetBuilderCodec<String, RpgModConfig> CODEC = AssetBuilderCodec.builder(
            RpgModConfig.class,
            RpgModConfig::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("GeneralSettings", GeneralModSettings.CODEC, false, true),
                    (i, s) -> i.generalSettings = s, i -> i.generalSettings)
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("ClassSettings", ClassModSettings.CODEC, false, true),
                    (i, s) -> i.classSettings = s, i -> i.classSettings)
            .metadata(new UIEditorSectionStart("Classes"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("CharacterSettings", CharacterModSettings.CODEC, false, true),
                    (i, s) -> i.characterSettings = s, i -> i.characterSettings)
            .metadata(new UIEditorSectionStart("Characters"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("AchievementSettings", AchievementModSettings.CODEC, false, true),
                    (i, s) -> i.achievementSettings = s, i -> i.achievementSettings)
            .metadata(new UIEditorSectionStart("Achievements"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("TalentSettings", TalentModSettings.CODEC, false, true),
                    (i, s) -> i.talentSettings = s, i -> i.talentSettings)
            .metadata(new UIEditorSectionStart("Talents"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("LevelingSettings", LevelingModSettings.CODEC, false, true),
                    (i, s) -> i.levelingSettings = s, i -> i.levelingSettings)
            .metadata(new UIEditorSectionStart("Leveling"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("AbilitySettings", AbilityModSettings.CODEC, false, true),
                    (i, s) -> i.abilitySettings = s, i -> i.abilitySettings)
            .metadata(new UIEditorSectionStart("Abilities"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("InventorySettings", InventoryModSettings.CODEC, false, true),
                    (i, s) -> i.inventorySettings = s, i -> i.inventorySettings)
            .metadata(new UIEditorSectionStart("Inventory"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
                .append(new KeyedCodec<>("ItemSettings", ItemModSettings.CODEC, false, true),
                    (i, s) -> i.itemSettings = s, i -> i.itemSettings)
                .metadata(new UIEditorSectionStart("Items"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add()
            .append(new KeyedCodec<>("PartySettings", PartyModSettings.CODEC, false, true),
                    (i, s) -> i.partySettings = s, i -> i.partySettings)
            .metadata(new UIEditorSectionStart("Parties"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("GuildSettings", GuildModSettings.CODEC, false, true),
                    (i, s) -> i.guildSettings = s, i -> i.guildSettings)
            .metadata(new UIEditorSectionStart("Guilds"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("CameraSettings", CameraModSettings.CODEC, false, true),
                    (i, s) -> i.cameraSettings = s, i -> i.cameraSettings)
            .metadata(new UIEditorSectionStart("Camera"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("BankSettings", BankModSettings.CODEC, false, true),
                    (i, s) -> i.bankSettings = s, i -> i.bankSettings)
            .metadata(new UIEditorSectionStart("Banks"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
                .append(new KeyedCodec<>("CurrencySettings", CurrencyModSettings.CODEC, false, true),
                    (i, s) -> i.currencySettings = s, i -> i.currencySettings)
                .metadata(new UIEditorSectionStart("Currency"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add()
                .append(new KeyedCodec<>("AuctionHouseSettings", AuctionHouseModSettings.CODEC, false, true),
                    (i, s) -> i.auctionHouseSettings = s, i -> i.auctionHouseSettings)
                .metadata(new UIEditorSectionStart("Auction House"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add()
                .append(new KeyedCodec<>("MailSettings", MailModSettings.CODEC, false, true),
                        (i, s) -> i.mailSettings = s, i -> i.mailSettings)
                .metadata(new UIEditorSectionStart("Mail"))
                    .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add()
            .append(new KeyedCodec<>("LockpickingSettings", LockpickingModSettings.CODEC, false, true),
                    (i, s) -> i.lockpickingSettings = s, i -> i.lockpickingSettings)
            .metadata(new UIEditorSectionStart("Lockpicking"))
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .build();

    private static DefaultAssetMap<String, RpgModConfig> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private GeneralModSettings generalSettings;
    private ClassModSettings classSettings;
    private CharacterModSettings characterSettings;
    private AchievementModSettings achievementSettings;
    private TalentModSettings talentSettings;
    private LevelingModSettings levelingSettings;
    private AbilityModSettings abilitySettings;
    private InventoryModSettings inventorySettings;
    private ItemModSettings itemSettings;
    private PartyModSettings partySettings;
    private GuildModSettings guildSettings;
    private CameraModSettings cameraSettings;
    private BankModSettings bankSettings;
    private CurrencyModSettings currencySettings;
    private AuctionHouseModSettings auctionHouseSettings;
    private MailModSettings mailSettings;
    private LockpickingModSettings lockpickingSettings;

    public RpgModConfig() {
        this.id = "";
        this.generalSettings = new GeneralModSettings();
        this.classSettings = new ClassModSettings();
        this.characterSettings = new CharacterModSettings();
        this.achievementSettings = new AchievementModSettings();
        this.talentSettings = new TalentModSettings();
        this.levelingSettings = new LevelingModSettings();
        this.abilitySettings = new AbilityModSettings();
        this.inventorySettings = new InventoryModSettings();
        this.itemSettings = new ItemModSettings();
        this.partySettings = new PartyModSettings();
        this.guildSettings = new GuildModSettings();
        this.cameraSettings = new CameraModSettings();
        this.bankSettings = new BankModSettings();
        this.currencySettings = new CurrencyModSettings();
        this.auctionHouseSettings = new AuctionHouseModSettings();
        this.mailSettings = new MailModSettings();
        this.lockpickingSettings = new LockpickingModSettings();
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

    public GeneralModSettings getGeneralSettings() {
        return generalSettings != null ? generalSettings : new GeneralModSettings();
    }

    public ClassModSettings getClassSettings() {
        return classSettings != null ? classSettings : new ClassModSettings();
    }

    public CharacterModSettings getCharacterSettings() {
        return characterSettings != null ? characterSettings : new CharacterModSettings();
    }

    public AchievementModSettings getAchievementSettings() {
        return achievementSettings != null ? achievementSettings : new AchievementModSettings();
    }

    public TalentModSettings getTalentSettings() {
        return talentSettings != null ? talentSettings : new TalentModSettings();
    }

    public LevelingModSettings getLevelingSettings() {
        return levelingSettings != null ? levelingSettings : new LevelingModSettings();
    }

    public AbilityModSettings getAbilitySettings() {
        return abilitySettings != null ? abilitySettings : new AbilityModSettings();
    }

    public InventoryModSettings getInventorySettings() {
        return inventorySettings != null ? inventorySettings : new InventoryModSettings();
    }

    public ItemModSettings getItemSettings() {
        return itemSettings != null ? itemSettings : new ItemModSettings();
    }

    public PartyModSettings getPartySettings() {
        return partySettings != null ? partySettings : new PartyModSettings();
    }

    public GuildModSettings getGuildSettings() {
        return guildSettings != null ? guildSettings : new GuildModSettings();
    }

    public CameraModSettings getCameraSettings() {
        return cameraSettings != null ? cameraSettings : new CameraModSettings();
    }

    public BankModSettings getBankSettings() {
        return bankSettings != null ? bankSettings : new BankModSettings();
    }

    public CurrencyModSettings getCurrencySettings() {
        return currencySettings != null ? currencySettings : new CurrencyModSettings();
    }

    public AuctionHouseModSettings getAuctionHouseSettings() {
        return auctionHouseSettings != null ? auctionHouseSettings : new AuctionHouseModSettings();
    }

    public MailModSettings getMailSettings() {
        return mailSettings != null ? mailSettings : new MailModSettings();
    }

    public LockpickingModSettings getLockpickingSettings() {
        return lockpickingSettings != null ? lockpickingSettings : new LockpickingModSettings();
    }

    public boolean isClassModuleEnabled() {
        return getClassSettings().isEnabled();
    }

    public boolean isTalentModuleEnabled() {
        return getTalentSettings().isEnabled();
    }

    public boolean isCharacterModuleEnabled() {
        return getCharacterSettings().isEnabled();
    }

    public boolean isAchievementModuleEnabled() {
        return getAchievementSettings().isEnabled();
    }

    public boolean isLevelingModuleEnabled() {
        return getLevelingSettings().isEnabled();
    }

    public boolean isAbilityModuleEnabled() {
        return getAbilitySettings().isEnabled();
    }

    public boolean isInventoryModuleEnabled() {
        return getInventorySettings().isEnabled();
    }

    public boolean isPartyModuleEnabled() {
        return getPartySettings().isEnabled();
    }

    public boolean isGuildModuleEnabled() {
        return getGuildSettings().isEnabled();
    }

    public boolean isCameraModuleEnabled() {
        return getCameraSettings().isEnabled();
    }

    public boolean isBankModuleEnabled() {
        return getBankSettings().isEnabled();
    }

    public boolean isCurrencyModuleEnabled() {
        return getCurrencySettings().isEnabled();
    }

    public boolean isAuctionHouseModuleEnabled() {
        return getAuctionHouseSettings().isEnabled();
    }

    public boolean isMailModuleEnabled() {
        return getMailSettings().isEnabled();
    }

    public boolean isLockpickingModuleEnabled() {
        return getLockpickingSettings().isEnabled();
    }

    public ClassModSettings.ClassMode getClassMode() {
        return getClassSettings().getClassMode();
    }

    public ClassModSettings.ActiveClassMode getActiveClassMode() {
        return getClassSettings().getActiveClassMode();
    }

    public ClassModSettings.XpRoutingMode getXpRouting() {
        return getClassSettings().getXpRouting();
    }

    public List<String> getXpTags() {
        return getClassSettings().getXpTags();
    }

    public Object2FloatMap<String> getXpTagSplits() {
        return getClassSettings().getXpTagSplits();
    }

    public Map<String, List<GeneralModSettings.RollModifierRange>> getAdvantageRollModifiers() {
        return getGeneralSettings().getAdvantageRollModifiers();
    }

    public String getLockpickItemTag() {
        return getLockpickingSettings().getLockpickItemTag();
    }

    public Map<String, LockpickingModSettings.LockpickingDifficultyTier> getLockpickingDifficultyTiers() {
        return getLockpickingSettings().getLockpickingDifficultyTiers();
    }

    public int getBaseGlobalCooldown() {
        return getAbilitySettings().getBaseGlobalCooldown();
    }

    public Object2IntMap<String> getGlobalCooldownCategories() {
        return getAbilitySettings().getGlobalCooldownCategories();
    }

    public boolean isRestedXpEnabled() {
        return isLevelingModuleEnabled() && getLevelingSettings().isRestedXpEnabled();
    }

    public int getRestedXpBonusPercent() {
        return getLevelingSettings().getRestedXpBonusPercent();
    }

    public int getRestedXpConsumeRatio() {
        return getLevelingSettings().getRestedXpConsumeRatio();
    }

    public List<String> getRestedXpGainTags() {
        return getLevelingSettings().getRestedXpGainTags();
    }

    public boolean isHardcoreEnabled() {
        return isLevelingModuleEnabled() && getLevelingSettings().isHardcoreEnabled();
    }

    public LevelingModSettings.HardcoreLossType getHardcoreLossType() {
        return getLevelingSettings().getHardcoreLossType();
    }

    public int getHardcoreLevelLossPercent() {
        return getLevelingSettings().getHardcoreLevelLossPercent();
    }

    public boolean isHardcoreCurrencyLossEnabled() {
        return getLevelingSettings().isHardcoreCurrencyLossEnabled();
    }

    public int getHardcoreCurrencyLossPercent() {
        return getLevelingSettings().getHardcoreCurrencyLossPercent();
    }

    public java.util.List<org.pixelbays.rpg.economy.currency.config.CurrencyScope> getHardcoreCurrencyLossScopes() {
        return getLevelingSettings().getHardcoreCurrencyLossScopes();
    }

    public float getBaseXpMultiplier() {
        return getLevelingSettings().getBaseXpMultiplier();
    }

    public InventoryModSettings.InventoryHandlingMode getInventoryHandling() {
        if (!isInventoryModuleEnabled()) {
            return InventoryModSettings.InventoryHandlingMode.Vanilla;
        }
        return getInventorySettings().getInventoryHandling();
    }

    public int getDefaultInventorySize() {
        return getInventorySettings().getDefaultInventorySize();
    }

    public Object2IntMap<String> getStrengthInventorySlots() {
        return getInventorySettings().getStrengthInventorySlots();
    }

    public boolean isExtraSlotsRingsEnabled() {
        return getInventorySettings().isExtraSlotsRingsEnabled();
    }

    public boolean isExtraSlotsTrinketsEnabled() {
        return getInventorySettings().isExtraSlotsTrinketsEnabled();
    }

    public boolean isExtraSlotsNeckEnabled() {
        return getInventorySettings().isExtraSlotsNeckEnabled();
    }

    public boolean isPartyEnabled() {
        return isPartyModuleEnabled() && getPartySettings().isPartyEnabled();
    }

    public boolean isGroupFinderEnabled() {
        return isPartyModuleEnabled() && getPartySettings().isGroupFinderEnabled();
    }

    public int getPartyMaxSize() {
        return getPartySettings().getPartyMaxSize();
    }

    public int getPartyMaxAssistants() {
        return getPartySettings().getPartyMaxAssistants();
    }

    public boolean isRaidEnabled() {
        return isPartyModuleEnabled() && getPartySettings().isRaidEnabled();
    }

    public int getRaidMaxSize() {
        return getPartySettings().getRaidMaxSize();
    }

    public int getRaidMaxAssistants() {
        return getPartySettings().getRaidMaxAssistants();
    }

    public int getPartyInviteExpirySeconds() {
        return getPartySettings().getPartyInviteExpirySeconds();
    }

    public boolean isPartyXpEnabled() {
        return isPartyModuleEnabled() && getPartySettings().isPartyXpEnabled();
    }

    public PartyModSettings.PartyXpGrantingMode getPartyXpGrantingMode() {
        return getPartySettings().getPartyXpGrantingMode();
    }

    public int getPartyXpRangeBlocks() {
        return getPartySettings().getPartyXpRangeBlocks();
    }

    public int getPartyXpMinMembersInRange() {
        return getPartySettings().getPartyXpMinMembersInRange();
    }

    public boolean isPartyNpcAllowed() {
        return isPartyModuleEnabled() && getPartySettings().isPartyNpcAllowed();
    }

    public boolean isPartyPersistenceEnabled() {
        return isPartyModuleEnabled() && getPartySettings().isPartyPersistenceEnabled();
    }

    public int getPartyPersistenceIntervalSeconds() {
        return getPartySettings().getPartyPersistenceIntervalSeconds();
    }

    public boolean isGuildEnabled() {
        return isGuildModuleEnabled() && getGuildSettings().isGuildEnabled();
    }

    public int getGuildMaxMembers() {
        return getGuildSettings().getGuildMaxMembers();
    }

    public int getGuildInviteExpirySeconds() {
        return getGuildSettings().getGuildInviteExpirySeconds();
    }

    public int getGuildNameMinLength() {
        return getGuildSettings().getGuildNameMinLength();
    }

    public int getGuildNameMaxLength() {
        return getGuildSettings().getGuildNameMaxLength();
    }

    public int getGuildTagMinLength() {
        return getGuildSettings().getGuildTagMinLength();
    }

    public int getGuildTagMaxLength() {
        return getGuildSettings().getGuildTagMaxLength();
    }

    public GuildJoinPolicy getGuildDefaultJoinPolicy() {
        return getGuildSettings().getGuildDefaultJoinPolicy();
    }

    public boolean isGuildPersistenceEnabled() {
        return isGuildModuleEnabled() && getGuildSettings().isGuildPersistenceEnabled();
    }

    public int getGuildPersistenceIntervalSeconds() {
        return getGuildSettings().getGuildPersistenceIntervalSeconds();
    }

    public int getMaxCombatClasses() {
        return getClassSettings().getMaxCombatClasses();
    }

    public int getMaxProfessionClasses() {
        return getClassSettings().getMaxProfessionClasses();
    }

    public String getClassSwitchingRules() {
        return getClassSettings().getClassSwitchingRules();
    }

    public GeneralModSettings.DebuggingMode getDebuggingMode() {
        return getGeneralSettings().getDebuggingMode();
    }

    public boolean isPlayerLogging() {
        return getGeneralSettings().isPlayerLogging();
    }

    public boolean isAntiGrindMod() {
        return getGeneralSettings().isAntiGrindMod();
    }

    public ClassModSettings.AbilityLearningMode getAbilityLearningMode() {
        return getClassSettings().getAbilityLearningMode();
    }

    public boolean shouldAutoLearnClassAbilitiesOnLevelUp() {
        return getClassSettings().shouldAutoLearnAbilitiesOnLevelUp();
    }

    public boolean isRequireRaceAtStart() {
        return getGeneralSettings().isRequireRaceAtStart();
    }

    public boolean isGlobalMobScaling() {
        return getGeneralSettings().isGlobalMobScaling();
    }

    public String getServerName() {
        return getGeneralSettings().getServerName();
    }

    public String getDiscordJoin() {
        return getGeneralSettings().getDiscordJoin();
    }

    public String getWebsite() {
        return getGeneralSettings().getWebsite();
    }

    public AbilityModSettings.AbilityControlType getAbilityControlType() {
        return getAbilitySettings().getAbilityControlType();
    }

    public int[] getHotbarAbilitySlots() {
        return getAbilitySettings().getHotbarAbilitySlots();
    }

    public CameraModSettings.TargetingStyle getTargetingStyle() {
        if (!isCameraModuleEnabled()) {
            return CameraModSettings.TargetingStyle.Vanilla;
        }
        return getCameraSettings().getTargetingStyle();
    }

    public CameraModSettings.CameraStyle getCameraStyle() {
        if (!isCameraModuleEnabled()) {
            return CameraModSettings.CameraStyle.Vanilla;
        }
        return getCameraSettings().getCameraStyle();
    }

    public CameraModSettings.CameraSettings getCameraSettingsTopDown() {
        return getCameraSettings().getCameraSettingsTopDown();
    }

    public CameraModSettings.CameraSettings getCameraSettingsIsometric() {
        return getCameraSettings().getCameraSettingsIsometric();
    }

    public String getDefaultPersonalBankTypeId() {
        return getBankSettings().getDefaultPersonalBankTypeId();
    }

    public String getDefaultAccountBankTypeId() {
        return getBankSettings().getDefaultAccountBankTypeId();
    }

    public String getDefaultGuildBankTypeId() {
        return getBankSettings().getDefaultGuildBankTypeId();
    }

    public String getDefaultVoidBankTypeId() {
        return getBankSettings().getDefaultVoidBankTypeId();
    }

    public String getDefaultWarboundBankTypeId() {
        return getBankSettings().getDefaultWarboundBankTypeId();
    }

    public String getDefaultProfessionBankTypeId() {
        return getBankSettings().getDefaultProfessionBankTypeId();
    }

    public boolean isAllowAssetDefinedBankTypes() {
        return isBankModuleEnabled();
    }

    public boolean isMailEnabled() {
        return isMailModuleEnabled() && getMailSettings().isMailEnabled();
    }
}
