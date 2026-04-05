package org.pixelbays.rpg.achievement.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.achievement.component.AchievementComponent.AchievementProgress;
import org.pixelbays.rpg.leveling.config.EventTitleConfig;
import org.pixelbays.rpg.leveling.config.NotificationConfig;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class AchievementDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, AchievementDefinition>> {

    private static final FunctionCodec<AchievementCriterionDefinition[], List<AchievementCriterionDefinition>> CRITERIA_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(AchievementCriterionDefinition.CODEC, AchievementCriterionDefinition[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(AchievementCriterionDefinition[]::new));

    public static final AssetBuilderCodec<String, AchievementDefinition> CODEC = AssetBuilderCodec.builder(
            AchievementDefinition.class,
            AchievementDefinition::new,
            Codec.STRING,
            (definition, key) -> definition.id = key,
            definition -> definition.id,
            (asset, data) -> asset.data = data,
            AchievementDefinition::ensureAssetData)
            .append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
                    (definition, value) -> definition.displayName = value,
                    definition -> definition.displayName)
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING, false, true),
                    (definition, value) -> definition.description = value,
                    definition -> definition.description)
            .add()
            .append(new KeyedCodec<>("NameTranslationKey", Codec.STRING, false, true),
                    (definition, value) -> definition.nameTranslationKey = value,
                    definition -> definition.nameTranslationKey)
            .add()
            .append(new KeyedCodec<>("DescriptionTranslationKey", Codec.STRING, false, true),
                    (definition, value) -> definition.descriptionTranslationKey = value,
                    definition -> definition.descriptionTranslationKey)
            .add()
            .append(new KeyedCodec<>("Category", new EnumCodec<>(AchievementCategory.class), false, true),
                    (definition, value) -> definition.category = value,
                    definition -> definition.category)
            .add()
            .append(new KeyedCodec<>("Points", Codec.INTEGER, false, true),
                    (definition, value) -> definition.points = value,
                    definition -> definition.points)
            .add()
            .append(new KeyedCodec<>("Criteria", CRITERIA_LIST_CODEC, false, true),
                    (definition, value) -> definition.criteria = value,
                    definition -> definition.criteria)
            .add()
            .append(new KeyedCodec<>("Reward", AchievementReward.CODEC, false, true),
                    (definition, value) -> definition.reward = value,
                    definition -> definition.reward)
            .add()
            .append(new KeyedCodec<>("AccountWide", Codec.BOOLEAN, false, true),
                    (definition, value) -> definition.accountWide = value,
                    definition -> definition.accountWide)
            .add()
            .append(new KeyedCodec<>("Hidden", Codec.BOOLEAN, false, true),
                    (definition, value) -> definition.hidden = value,
                    definition -> definition.hidden)
            .add()
            .append(new KeyedCodec<>("IconId", Codec.STRING, false, true),
                    (definition, value) -> definition.iconId = value,
                    definition -> definition.iconId)
            .add()
            .build();

    private static DefaultAssetMap<String, AchievementDefinition> ASSET_MAP;

    private AssetExtraInfo.Data data;
    private String id;
    private String displayName;
    private String description;
    private String nameTranslationKey;
    private String descriptionTranslationKey;
    private AchievementCategory category;
    private int points;
    private List<AchievementCriterionDefinition> criteria;
    private AchievementReward reward;
    private boolean accountWide;
    private boolean hidden;
    private String iconId;

    public AchievementDefinition() {
        this.data = new AssetExtraInfo.Data(AchievementDefinition.class, "", null);
        this.id = "";
        this.displayName = "";
        this.description = "";
        this.nameTranslationKey = "";
        this.descriptionTranslationKey = "";
        this.category = AchievementCategory.Progression;
        this.points = 0;
        this.criteria = new ArrayList<>();
        this.reward = new AchievementReward();
        this.accountWide = false;
        this.hidden = false;
        this.iconId = "";
    }

    @Nonnull
    private AssetExtraInfo.Data ensureAssetData() {
        if (data == null) {
            data = new AssetExtraInfo.Data(AchievementDefinition.class, getId(), null);
        }
        return Objects.requireNonNull(data);
    }

    @Nullable
    public static DefaultAssetMap<String, AchievementDefinition> getAssetMap() {
        if (ASSET_MAP == null) {
            var assetStore = AssetRegistry.getAssetStore(AchievementDefinition.class);
            if (assetStore != null) {
                ASSET_MAP = (DefaultAssetMap<String, AchievementDefinition>) assetStore.getAssetMap();
            }
        }
        return ASSET_MAP;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName == null ? "" : displayName;
    }

    @Nonnull
    public String getDescription() {
        return description == null ? "" : description;
    }

    @Nonnull
    public String getNameTranslationKey() {
        return nameTranslationKey == null ? "" : nameTranslationKey;
    }

    @Nonnull
    public String getDescriptionTranslationKey() {
        return descriptionTranslationKey == null ? "" : descriptionTranslationKey;
    }

    @Nonnull
    public AchievementCategory getCategory() {
        return category == null ? AchievementCategory.Progression : category;
    }

    public int getPoints() {
        return Math.max(0, points);
    }

    @Nonnull
    public List<AchievementCriterionDefinition> getCriteria() {
        List<AchievementCriterionDefinition> value = criteria;
        return value == null ? List.of() : value;
    }

    @Nonnull
    public AchievementReward getReward() {
        return reward == null ? new AchievementReward() : reward;
    }

    public boolean isAccountWide() {
        return accountWide;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Nonnull
    public String getIconId() {
        return iconId == null ? "" : iconId;
    }

    public boolean isSatisfiedBy(@Nullable AchievementProgress progress) {
        if (progress == null) {
            return false;
        }
        for (AchievementCriterionDefinition criterion : getCriteria()) {
            if (criterion == null) {
                continue;
            }
            if (progress.getCriterionProgress(criterion.getId()) < criterion.getTargetValue()) {
                return false;
            }
        }
        return !getCriteria().isEmpty();
    }

    public enum AchievementCategory {
        Progression,
        Class,
        Race,
        Ability,
        Exploration,
        Social,
        Collection
    }

    public enum CriterionType {
        ReachLevel,
        LearnClass,
        ChangeRace,
        UnlockAbility
    }

    public static final class AchievementCriterionDefinition {
        public static final BuilderCodec<AchievementCriterionDefinition> CODEC = BuilderCodec
                .builder(AchievementCriterionDefinition.class, AchievementCriterionDefinition::new)
                .append(new KeyedCodec<>("Id", Codec.STRING, false, true),
                        (criterion, value) -> criterion.id = value,
                        criterion -> criterion.id)
                .add()
                .append(new KeyedCodec<>("Type", new EnumCodec<>(CriterionType.class), false, true),
                        (criterion, value) -> criterion.type = value,
                        criterion -> criterion.type)
                .add()
                .append(new KeyedCodec<>("TargetValue", Codec.INTEGER, false, true),
                        (criterion, value) -> criterion.targetValue = value,
                        criterion -> criterion.targetValue)
                .add()
                .append(new KeyedCodec<>("SystemId", Codec.STRING, false, true),
                        (criterion, value) -> criterion.systemId = value,
                        criterion -> criterion.systemId)
                .add()
                .append(new KeyedCodec<>("ClassId", Codec.STRING, false, true),
                        (criterion, value) -> criterion.classId = value,
                        criterion -> criterion.classId)
                .add()
                .append(new KeyedCodec<>("RaceId", Codec.STRING, false, true),
                        (criterion, value) -> criterion.raceId = value,
                        criterion -> criterion.raceId)
                .add()
                .append(new KeyedCodec<>("AbilityId", Codec.STRING, false, true),
                        (criterion, value) -> criterion.abilityId = value,
                        criterion -> criterion.abilityId)
                .add()
                .build();

        private String id;
        private CriterionType type;
        private int targetValue;
        private String systemId;
        private String classId;
        private String raceId;
        private String abilityId;

        public AchievementCriterionDefinition() {
            this.id = "";
            this.type = CriterionType.ReachLevel;
            this.targetValue = 1;
            this.systemId = "";
            this.classId = "";
            this.raceId = "";
            this.abilityId = "";
        }

        @Nonnull
        public String getId() {
            String value = id;
            return value == null || value.isBlank() ? "criterion" : value;
        }

        @Nonnull
        public CriterionType getType() {
            return type == null ? CriterionType.ReachLevel : type;
        }

        public int getTargetValue() {
            return Math.max(1, targetValue);
        }

        @Nonnull
        public String getSystemId() {
            return systemId == null ? "" : systemId;
        }

        @Nonnull
        public String getClassId() {
            return classId == null ? "" : classId;
        }

        @Nonnull
        public String getRaceId() {
            return raceId == null ? "" : raceId;
        }

        @Nonnull
        public String getAbilityId() {
            return abilityId == null ? "" : abilityId;
        }
    }

    public static final class AchievementReward {
        public static final BuilderCodec<AchievementReward> CODEC = BuilderCodec
                .builder(AchievementReward.class, AchievementReward::new)
                .append(new KeyedCodec<>("DisplayedTitle", Codec.STRING, false, true),
                        (reward, value) -> reward.displayedTitle = value,
                        reward -> reward.displayedTitle)
                .add()
                .append(new KeyedCodec<>("DisplayedTitleTranslationKey", Codec.STRING, false, true),
                        (reward, value) -> reward.displayedTitleTranslationKey = value,
                        reward -> reward.displayedTitleTranslationKey)
                .add()
            .append(new KeyedCodec<>("DisplayedTitlePrefix", Codec.STRING, false, true),
                (reward, value) -> reward.displayedTitlePrefix = value,
                reward -> reward.displayedTitlePrefix)
            .add()
            .append(new KeyedCodec<>("DisplayedTitlePrefixTranslationKey", Codec.STRING, false, true),
                (reward, value) -> reward.displayedTitlePrefixTranslationKey = value,
                reward -> reward.displayedTitlePrefixTranslationKey)
            .add()
            .append(new KeyedCodec<>("DisplayedTitleSuffix", Codec.STRING, false, true),
                (reward, value) -> reward.displayedTitleSuffix = value,
                reward -> reward.displayedTitleSuffix)
            .add()
            .append(new KeyedCodec<>("DisplayedTitleSuffixTranslationKey", Codec.STRING, false, true),
                (reward, value) -> reward.displayedTitleSuffixTranslationKey = value,
                reward -> reward.displayedTitleSuffixTranslationKey)
            .add()
                .append(new KeyedCodec<>("CurrencyRewards", new MapCodec<>(Codec.LONG, java.util.LinkedHashMap::new, false), false, true),
                        (reward, value) -> reward.currencyRewards = value,
                        reward -> reward.currencyRewards)
                .add()
                .append(new KeyedCodec<>("Notification", NotificationConfig.CODEC, false, true),
                        (reward, value) -> reward.notification = value,
                        reward -> reward.notification)
                .add()
                .append(new KeyedCodec<>("EventTitle", EventTitleConfig.CODEC, false, true),
                        (reward, value) -> reward.eventTitle = value,
                        reward -> reward.eventTitle)
                .add()
                .build();

        private String displayedTitle;
        private String displayedTitleTranslationKey;
        private String displayedTitlePrefix;
        private String displayedTitlePrefixTranslationKey;
        private String displayedTitleSuffix;
        private String displayedTitleSuffixTranslationKey;
        private Map<String, Long> currencyRewards;
        private NotificationConfig notification;
        private EventTitleConfig eventTitle;

        public AchievementReward() {
            this.displayedTitle = "";
            this.displayedTitleTranslationKey = "";
            this.displayedTitlePrefix = "";
            this.displayedTitlePrefixTranslationKey = "";
            this.displayedTitleSuffix = "";
            this.displayedTitleSuffixTranslationKey = "";
            this.currencyRewards = new java.util.LinkedHashMap<>();
            this.notification = null;
            this.eventTitle = null;
        }

        @Nonnull
        public String getDisplayedTitle() {
            return displayedTitle == null ? "" : displayedTitle;
        }

        @Nonnull
        public String getDisplayedTitleTranslationKey() {
            return displayedTitleTranslationKey == null ? "" : displayedTitleTranslationKey;
        }

        @Nonnull
        public String getDisplayedTitlePrefix() {
            return displayedTitlePrefix == null ? "" : displayedTitlePrefix;
        }

        @Nonnull
        public String getDisplayedTitlePrefixTranslationKey() {
            return displayedTitlePrefixTranslationKey == null ? "" : displayedTitlePrefixTranslationKey;
        }

        @Nonnull
        public String getDisplayedTitleSuffix() {
            return displayedTitleSuffix == null ? "" : displayedTitleSuffix;
        }

        @Nonnull
        public String getDisplayedTitleSuffixTranslationKey() {
            return displayedTitleSuffixTranslationKey == null ? "" : displayedTitleSuffixTranslationKey;
        }

        @Nonnull
        public Map<String, Long> getCurrencyRewards() {
            Map<String, Long> value = currencyRewards;
            return value == null ? Map.of() : value;
        }

        @Nullable
        public NotificationConfig getNotification() {
            return notification;
        }

        @Nullable
        public EventTitleConfig getEventTitle() {
            return eventTitle;
        }

        public boolean hasAnyReward() {
            return !getDisplayedTitle().isBlank()
                    || !getDisplayedTitleTranslationKey().isBlank()
                    || !getDisplayedTitlePrefix().isBlank()
                    || !getDisplayedTitlePrefixTranslationKey().isBlank()
                    || !getDisplayedTitleSuffix().isBlank()
                    || !getDisplayedTitleSuffixTranslationKey().isBlank()
                    || !getCurrencyRewards().isEmpty()
                    || notification != null
                    || eventTitle != null;
        }
    }
}
