package org.pixelbays.rpg.global.config.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class GeneralModSettings {

    private static final FunctionCodec<ExpansionDefinition[], List<ExpansionDefinition>> EXPANSION_DEFINITION_LIST_CODEC =
        new FunctionCodec<>(
            new ArrayCodec<>(ExpansionDefinition.CODEC, ExpansionDefinition[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(ExpansionDefinition[]::new));

    public enum DebuggingMode {
        None,
        Min,
        Max,
        DeveloperDontUse
    }

    public static class ExpansionDefinition {
        public static final BuilderCodec<ExpansionDefinition> CODEC = BuilderCodec
                .builder(ExpansionDefinition.class, ExpansionDefinition::new)
                .append(new KeyedCodec<>("Id", Codec.STRING, false, true),
                        (i, s) -> i.id = s, i -> i.id)
                .add()
                .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                        (i, s) -> i.displayName = s, i -> i.displayName)
                .add()
                .append(new KeyedCodec<>("Description", Codec.STRING, false, true),
                        (i, s) -> i.description = s, i -> i.description)
                .add()
                .append(new KeyedCodec<>("WebsiteUrl", Codec.STRING, false, true),
                        (i, s) -> i.websiteUrl = s, i -> i.websiteUrl)
                .add()
                .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                        (i, s) -> i.enabled = s, i -> i.enabled)
                .add()
                .append(new KeyedCodec<>("Visible", Codec.BOOLEAN, false, true),
                        (i, s) -> i.visible = s, i -> i.visible)
                .add()
                .append(new KeyedCodec<>("SortOrder", Codec.INTEGER, false, true),
                        (i, s) -> i.sortOrder = s, i -> i.sortOrder)
                .add()
                .append(new KeyedCodec<>("LevelCap", Codec.INTEGER, false, true),
                    (i, s) -> i.levelCap = s, i -> i.levelCap)
                .add()
                .append(new KeyedCodec<>("UnlockPrice", CurrencyAmountDefinition.CODEC, false, true),
                        (i, s) -> i.unlockPrice = s, i -> i.unlockPrice)
                .add()
                .append(new KeyedCodec<>("PurchaseCurrencyScope", new EnumCodec<>(CurrencyScope.class), false, true),
                        (i, s) -> i.purchaseCurrencyScope = s, i -> i.purchaseCurrencyScope)
                .add()
                .append(new KeyedCodec<>("ReleaseTimeEpochMs", Codec.LONG, false, true),
                        (i, s) -> i.releaseTimeEpochMs = s, i -> i.releaseTimeEpochMs)
                .add()
                .append(new KeyedCodec<>("AccessPermission", Codec.STRING, false, true),
                        (i, s) -> i.accessPermission = s, i -> i.accessPermission)
                .add()
                .append(new KeyedCodec<>("ReleaseBypassPermission", Codec.STRING, false, true),
                        (i, s) -> i.releaseBypassPermission = s, i -> i.releaseBypassPermission)
                .add()
                .build();

        private String id;
        private String displayName;
        private String description;
        private String websiteUrl;
        private boolean enabled;
        private boolean visible;
        private int sortOrder;
        private int levelCap;
        private CurrencyAmountDefinition unlockPrice;
        private CurrencyScope purchaseCurrencyScope;
        private long releaseTimeEpochMs;
        private String accessPermission;
        private String releaseBypassPermission;

        public ExpansionDefinition() {
            this.id = "";
            this.displayName = "";
            this.description = "";
            this.websiteUrl = "";
            this.enabled = true;
            this.visible = true;
            this.sortOrder = 0;
            this.levelCap = 0;
            this.unlockPrice = new CurrencyAmountDefinition();
            this.purchaseCurrencyScope = CurrencyScope.Account;
            this.releaseTimeEpochMs = 0L;
            this.accessPermission = "";
            this.releaseBypassPermission = "";
        }

        public String getId() {
            return id == null ? "" : id;
        }

        public String getDisplayName() {
            return displayName == null ? "" : displayName;
        }

        public String getDisplayNameOrId() {
            return getDisplayName().isBlank() ? getId() : getDisplayName();
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public String getWebsiteUrl() {
            return websiteUrl == null ? "" : websiteUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isVisible() {
            return visible;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public int getLevelCap() {
            return levelCap;
        }

        public CurrencyAmountDefinition getUnlockPrice() {
            return unlockPrice == null ? new CurrencyAmountDefinition() : unlockPrice;
        }

        public CurrencyScope getPurchaseCurrencyScope() {
            return purchaseCurrencyScope == null ? CurrencyScope.Account : purchaseCurrencyScope;
        }

        public long getReleaseTimeEpochMs() {
            return releaseTimeEpochMs;
        }

        public String getAccessPermission() {
            return accessPermission == null ? "" : accessPermission;
        }

        public String getReleaseBypassPermission() {
            return releaseBypassPermission == null ? "" : releaseBypassPermission;
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
                .append(new KeyedCodec<>("Expansions", EXPANSION_DEFINITION_LIST_CODEC, false, true),
                    (i, s) -> i.expansions = s, i -> i.expansions)
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
    private List<ExpansionDefinition> expansions;
    private DebuggingMode debuggingMode;
    private boolean playerLogging;
    private boolean antiGrindMod;
    private boolean requireRaceAtStart;
    private boolean globalMobScaling;

    public GeneralModSettings() {
        this.serverName = "";
        this.discordJoin = "";
        this.website = "";
        this.expansions = new ArrayList<>();
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

    public List<ExpansionDefinition> getExpansions() {
        if (expansions == null) {
            expansions = new ArrayList<>();
        }
        expansions.sort(Comparator
                .comparingInt(ExpansionDefinition::getSortOrder)
                .thenComparing(ExpansionDefinition::getDisplayNameOrId, String.CASE_INSENSITIVE_ORDER));
        return expansions;
    }

    public ExpansionDefinition getExpansion(String expansionId) {
        if (expansionId == null || expansionId.isBlank()) {
            return null;
        }
        for (ExpansionDefinition expansion : getExpansions()) {
            if (expansion != null && expansionId.equalsIgnoreCase(expansion.getId())) {
                return expansion;
            }
        }
        return null;
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
