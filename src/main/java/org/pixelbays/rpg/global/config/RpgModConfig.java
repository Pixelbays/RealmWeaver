package org.pixelbays.rpg.global.config;

import java.util.List;
import java.util.Map;

import org.pixelbays.rpg.ability.config.settings.AbilityModSettings;
import org.pixelbays.rpg.achievement.config.settings.AchievementModSettings;
import org.pixelbays.rpg.camera.config.settings.CameraModSettings;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings;
import org.pixelbays.rpg.chat.config.settings.ChatModSettings;
import org.pixelbays.rpg.classes.config.settings.ClassModSettings;
import org.pixelbays.rpg.classes.config.settings.TalentModSettings;
import org.pixelbays.rpg.economy.auctions.config.settings.AuctionHouseModSettings;
import org.pixelbays.rpg.economy.banks.config.settings.BankModSettings;
import org.pixelbays.rpg.economy.currency.config.settings.CurrencyModSettings;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;
import org.pixelbays.rpg.global.config.settings.StatModSettings;
import org.pixelbays.rpg.global.config.settings.UiInputModSettings;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.config.settings.GuildModSettings;
import org.pixelbays.rpg.inventory.config.settings.InventoryModSettings;
import org.pixelbays.rpg.item.config.settings.ItemModSettings;
import org.pixelbays.rpg.leveling.config.settings.LevelingModSettings;
import org.pixelbays.rpg.lockpicking.config.settings.LockpickingModSettings;
import org.pixelbays.rpg.mail.config.settings.MailModSettings;
import org.pixelbays.rpg.npc.config.settings.NpcModSettings;
import org.pixelbays.rpg.party.config.settings.PartyModSettings;
import org.pixelbays.rpg.world.config.settings.WorldModSettings;

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

@SuppressWarnings("deprecation")
/**
 * Master configuration for Realmweaver.
 * Loaded from /Server/RpgModConfig/{ConfigName}.json
 */
public class RpgModConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, RpgModConfig>> {

    // CODEC is built in a static block so that BuildFlags.* constants can
    // conditionally exclude sections for disabled modules at compile time.
    public static final AssetBuilderCodec<String, RpgModConfig> CODEC;
    static {
        AssetBuilderCodec.Builder<String, RpgModConfig> b = AssetBuilderCodec.builder(
                RpgModConfig.class,
                RpgModConfig::new,
                Codec.STRING,
                (t, k) -> t.id = k,
                t -> t.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data);

        // Always included — foundational settings
        b = b.append(new KeyedCodec<>("GeneralSettings", GeneralModSettings.CODEC, false, true),
                        (i, s) -> i.generalSettings = s, i -> i.generalSettings)
                .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add();
        b = b.append(new KeyedCodec<>("UiInputSettings", UiInputModSettings.CODEC, false, true),
                (i, s) -> i.uiInputSettings = s, i -> i.uiInputSettings)
            .metadata(new UIEditorSectionStart("UI Inputs")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add();
        b = b.append(new KeyedCodec<>("StatSettings", StatModSettings.CODEC, false, true),
                        (i, s) -> i.statSettings = s, i -> i.statSettings)
                .metadata(new UIEditorSectionStart("Stats")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                .add();

        // ── BuildFlags-gated sections ─────────────────────────────────────────
        if (BuildFlags.CLASS_MODULE) {
            b = b.append(new KeyedCodec<>("ClassSettings", ClassModSettings.CODEC, false, true),
                            (i, s) -> i.classSettings = s, i -> i.classSettings)
                    .metadata(new UIEditorSectionStart("Classes")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.CHARACTER_MODULE) {
            b = b.append(new KeyedCodec<>("CharacterSettings", CharacterModSettings.CODEC, false, true),
                            (i, s) -> i.characterSettings = s, i -> i.characterSettings)
                    .metadata(new UIEditorSectionStart("Characters")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.ACHIEVEMENT_MODULE) {
            b = b.append(new KeyedCodec<>("AchievementSettings", AchievementModSettings.CODEC, false, true),
                            (i, s) -> i.achievementSettings = s, i -> i.achievementSettings)
                    .metadata(new UIEditorSectionStart("Achievements")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.TALENT_MODULE) {
            b = b.append(new KeyedCodec<>("TalentSettings", TalentModSettings.CODEC, false, true),
                            (i, s) -> i.talentSettings = s, i -> i.talentSettings)
                    .metadata(new UIEditorSectionStart("Talents")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.LEVELING_MODULE) {
            b = b.append(new KeyedCodec<>("LevelingSettings", LevelingModSettings.CODEC, false, true),
                            (i, s) -> i.levelingSettings = s, i -> i.levelingSettings)
                    .metadata(new UIEditorSectionStart("Leveling")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.ABILITY_MODULE) {
            b = b.append(new KeyedCodec<>("AbilitySettings", AbilityModSettings.CODEC, false, true),
                            (i, s) -> i.abilitySettings = s, i -> i.abilitySettings)
                    .metadata(new UIEditorSectionStart("Abilities")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.INVENTORY_MODULE) {
            b = b.append(new KeyedCodec<>("InventorySettings", InventoryModSettings.CODEC, false, true),
                            (i, s) -> i.inventorySettings = s, i -> i.inventorySettings)
                    .metadata(new UIEditorSectionStart("Inventory")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.ITEM_MODULE) {
            b = b.append(new KeyedCodec<>("ItemSettings", ItemModSettings.CODEC, false, true),
                            (i, s) -> i.itemSettings = s, i -> i.itemSettings)
                    .metadata(new UIEditorSectionStart("Items")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.PARTY_MODULE) {
            b = b.append(new KeyedCodec<>("PartySettings", PartyModSettings.CODEC, false, true),
                            (i, s) -> i.partySettings = s, i -> i.partySettings)
                    .metadata(new UIEditorSectionStart("Parties")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.GUILD_MODULE) {
            b = b.append(new KeyedCodec<>("GuildSettings", GuildModSettings.CODEC, false, true),
                            (i, s) -> i.guildSettings = s, i -> i.guildSettings)
                    .metadata(new UIEditorSectionStart("Guilds")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.NPC_MODULE) {
            b = b.append(new KeyedCodec<>("NpcSettings", NpcModSettings.CODEC, false, true),
                            (i, s) -> i.npcSettings = s, i -> i.npcSettings)
                    .metadata(new UIEditorSectionStart("NPCs")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.CAMERA_MODULE) {
            b = b.append(new KeyedCodec<>("CameraSettings", CameraModSettings.CODEC, false, true),
                            (i, s) -> i.cameraSettings = s, i -> i.cameraSettings)
                    .metadata(new UIEditorSectionStart("Camera")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.BANK_MODULE) {
            b = b.append(new KeyedCodec<>("BankSettings", BankModSettings.CODEC, false, true),
                            (i, s) -> i.bankSettings = s, i -> i.bankSettings)
                    .metadata(new UIEditorSectionStart("Banks")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.CURRENCY_MODULE) {
            b = b.append(new KeyedCodec<>("CurrencySettings", CurrencyModSettings.CODEC, false, true),
                            (i, s) -> i.currencySettings = s, i -> i.currencySettings)
                    .metadata(new UIEditorSectionStart("Currency")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.AUCTION_HOUSE_MODULE) {
            b = b.append(new KeyedCodec<>("AuctionHouseSettings", AuctionHouseModSettings.CODEC, false, true),
                            (i, s) -> i.auctionHouseSettings = s, i -> i.auctionHouseSettings)
                    .metadata(new UIEditorSectionStart("Auction House")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.MAIL_MODULE) {
            b = b.append(new KeyedCodec<>("MailSettings", MailModSettings.CODEC, false, true),
                            (i, s) -> i.mailSettings = s, i -> i.mailSettings)
                    .metadata(new UIEditorSectionStart("Mail")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.CHAT_MODULE) {
            b = b.append(new KeyedCodec<>("ChatSettings", ChatModSettings.CODEC, false, true),
                            (i, s) -> i.chatSettings = s, i -> i.chatSettings)
                    .metadata(new UIEditorSectionStart("Chat")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.WORLD_MODULE) {
            b = b.append(new KeyedCodec<>("WorldSettings", WorldModSettings.CODEC, false, true),
                            (i, s) -> i.worldSettings = s, i -> i.worldSettings)
                    .metadata(new UIEditorSectionStart("World")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }
        if (BuildFlags.LOCKPICKING_MODULE) {
            b = b.append(new KeyedCodec<>("LockpickingSettings", LockpickingModSettings.CODEC, false, true),
                            (i, s) -> i.lockpickingSettings = s, i -> i.lockpickingSettings)
                    .metadata(new UIEditorSectionStart("Lockpicking")).metadata(UIDefaultCollapsedState.UNCOLLAPSED)
                    .add();
        }

        CODEC = b.build();
    }

    private static DefaultAssetMap<String, RpgModConfig> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private GeneralModSettings generalSettings;
    private UiInputModSettings uiInputSettings;
    private StatModSettings statSettings;
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
    private NpcModSettings npcSettings;
    private CameraModSettings cameraSettings;
    private BankModSettings bankSettings;
    private CurrencyModSettings currencySettings;
    private AuctionHouseModSettings auctionHouseSettings;
    private MailModSettings mailSettings;
    private ChatModSettings chatSettings;
    private WorldModSettings worldSettings;
    private LockpickingModSettings lockpickingSettings;

    public RpgModConfig() {
        this.id = "";
        this.generalSettings = new GeneralModSettings();
        this.uiInputSettings = new UiInputModSettings();
        this.statSettings = new StatModSettings();
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
        this.npcSettings = new NpcModSettings();
        this.cameraSettings = new CameraModSettings();
        this.bankSettings = new BankModSettings();
        this.currencySettings = new CurrencyModSettings();
        this.auctionHouseSettings = new AuctionHouseModSettings();
        this.mailSettings = new MailModSettings();
        this.chatSettings = new ChatModSettings();
        this.worldSettings = new WorldModSettings();
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

    public UiInputModSettings getUiInputSettings() {
        return uiInputSettings != null ? uiInputSettings : new UiInputModSettings();
    }

    public StatModSettings getStatSettings() {
        return statSettings != null ? statSettings : new StatModSettings();
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

    public NpcModSettings getNpcSettings() {
        return npcSettings != null ? npcSettings : new NpcModSettings();
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

    public ChatModSettings getChatSettings() {
        return chatSettings != null ? chatSettings : new ChatModSettings();
    }

    public WorldModSettings getWorldSettings() {
        return worldSettings != null ? worldSettings : new WorldModSettings();
    }

    public LockpickingModSettings getLockpickingSettings() {
        return lockpickingSettings != null ? lockpickingSettings : new LockpickingModSettings();
    }

    public boolean isClassModuleEnabled() {
        return BuildFlags.CLASS_MODULE && getClassSettings().isEnabled();
    }

    public boolean isTalentModuleEnabled() {
        return BuildFlags.TALENT_MODULE && getTalentSettings().isEnabled();
    }

    public boolean isCharacterModuleEnabled() {
        return BuildFlags.CHARACTER_MODULE && getCharacterSettings().isEnabled();
    }

    public boolean isAchievementModuleEnabled() {
        return BuildFlags.ACHIEVEMENT_MODULE && getAchievementSettings().isEnabled();
    }

    public boolean isLevelingModuleEnabled() {
        return BuildFlags.LEVELING_MODULE && getLevelingSettings().isEnabled();
    }

    public boolean isAbilityModuleEnabled() {
        return BuildFlags.ABILITY_MODULE && getAbilitySettings().isEnabled();
    }

    public boolean isInventoryModuleEnabled() {
        return BuildFlags.INVENTORY_MODULE && getInventorySettings().isEnabled();
    }

    public boolean isPartyModuleEnabled() {
        return BuildFlags.PARTY_MODULE && getPartySettings().isEnabled();
    }

    public boolean isGuildModuleEnabled() {
        return BuildFlags.GUILD_MODULE && getGuildSettings().isEnabled();
    }

    public boolean isNpcModuleEnabled() {
        return BuildFlags.NPC_MODULE && getNpcSettings().isEnabled();
    }

    public boolean isCameraModuleEnabled() {
        return BuildFlags.CAMERA_MODULE && getCameraSettings().isEnabled();
    }

    public boolean isBankModuleEnabled() {
        return BuildFlags.BANK_MODULE && getBankSettings().isEnabled();
    }

    public boolean isCurrencyModuleEnabled() {
        return BuildFlags.CURRENCY_MODULE && getCurrencySettings().isEnabled();
    }

    public boolean isAuctionHouseModuleEnabled() {
        return BuildFlags.AUCTION_HOUSE_MODULE && getAuctionHouseSettings().isEnabled();
    }

    public boolean isMailModuleEnabled() {
        return BuildFlags.MAIL_MODULE && getMailSettings().isEnabled();
    }

    public boolean isChatModuleEnabled() {
        return BuildFlags.CHAT_MODULE && getChatSettings().isEnabled() && getChatSettings().hasEnabledChannels();
    }

    public boolean isLockpickingModuleEnabled() {
        return BuildFlags.LOCKPICKING_MODULE && getLockpickingSettings().isEnabled();
    }

    public boolean isNpcThreatEnabled() {
        return isNpcModuleEnabled() && getNpcSettings().isThreatEnabled();
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

    public Map<String, List<StatModSettings.RollModifierRange>> getAdvantageRollModifiers() {
        return getStatSettings().getAdvantageRollModifiers();
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
        return BuildFlags.RESTED_XP && isLevelingModuleEnabled() && getLevelingSettings().isRestedXpEnabled();
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

    public float getNpcThreatLookbackSeconds() {
        return getNpcSettings().getThreatLookbackSeconds();
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


    public boolean isMailEnabled() {
        return isMailModuleEnabled() && getMailSettings().isMailEnabled();
    }
}
