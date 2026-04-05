package org.pixelbays.rpg.achievement.component;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone" })
public class AchievementComponent implements Component<EntityStore>, Cloneable {

    public static final BuilderCodec<AchievementUnlockRecord> UNLOCK_RECORD_CODEC = BuilderCodec
            .builder(AchievementUnlockRecord.class, AchievementUnlockRecord::new)
            .append(new KeyedCodec<>("AchievementId", Codec.STRING, false, true),
                    (record, value) -> record.achievementId = value,
                    record -> record.achievementId)
            .add()
            .append(new KeyedCodec<>("UnlockedAtEpochMs", Codec.LONG, false, true),
                    (record, value) -> record.unlockedAtEpochMs = value,
                    record -> record.unlockedAtEpochMs)
            .add()
            .append(new KeyedCodec<>("PointsAwarded", Codec.INTEGER, false, true),
                    (record, value) -> record.pointsAwarded = value,
                    record -> record.pointsAwarded)
            .add()
            .build();

    public static final BuilderCodec<AchievementProgress> PROGRESS_CODEC = BuilderCodec
            .builder(AchievementProgress.class, AchievementProgress::new)
            .append(new KeyedCodec<>("AchievementId", Codec.STRING, false, true),
                    (progress, value) -> progress.achievementId = value,
                    progress -> progress.achievementId)
            .add()
            .append(new KeyedCodec<>("CriteriaProgress", new MapCodec<>(Codec.INTEGER, HashMap::new, false), false, true),
                    (progress, value) -> progress.criteriaProgress = value,
                    progress -> progress.criteriaProgress)
            .add()
            .append(new KeyedCodec<>("StartDateEpochMs", Codec.LONG, false, true),
                    (progress, value) -> progress.startDateEpochMs = value,
                    progress -> progress.startDateEpochMs)
            .add()
            .build();

    public static final BuilderCodec<AchievementComponent> CODEC = BuilderCodec
            .builder(AchievementComponent.class, AchievementComponent::new)
            .append(new KeyedCodec<>("UnlockedAchievements", new MapCodec<>(UNLOCK_RECORD_CODEC, HashMap::new, false), false, true),
                    (component, value) -> component.unlockedAchievements = value,
                    component -> component.unlockedAchievements)
            .add()
            .append(new KeyedCodec<>("ProgressTracking", new MapCodec<>(PROGRESS_CODEC, HashMap::new, false), false, true),
                    (component, value) -> component.progressTracking = value,
                    component -> component.progressTracking)
            .add()
            .append(new KeyedCodec<>("TotalAchievementPoints", Codec.INTEGER, false, true),
                    (component, value) -> component.totalAchievementPoints = value,
                    component -> component.totalAchievementPoints)
            .add()
            .append(new KeyedCodec<>("DisplayedTitle", Codec.STRING, false, true),
                    (component, value) -> component.displayedTitle = value,
                    component -> component.displayedTitle)
            .add()
                .append(new KeyedCodec<>("DisplayedTitlePrefix", Codec.STRING, false, true),
                    (component, value) -> component.displayedTitlePrefix = value,
                    component -> component.displayedTitlePrefix)
                .add()
                .append(new KeyedCodec<>("DisplayedTitleSuffix", Codec.STRING, false, true),
                    (component, value) -> component.displayedTitleSuffix = value,
                    component -> component.displayedTitleSuffix)
                .add()
            .build();

    private Map<String, AchievementUnlockRecord> unlockedAchievements;
    private Map<String, AchievementProgress> progressTracking;
    private int totalAchievementPoints;
    private String displayedTitle;
    private String displayedTitlePrefix;
    private String displayedTitleSuffix;

    public AchievementComponent() {
        this.unlockedAchievements = new HashMap<>();
        this.progressTracking = new HashMap<>();
        this.totalAchievementPoints = 0;
        this.displayedTitle = "";
        this.displayedTitlePrefix = "";
        this.displayedTitleSuffix = "";
    }

    public static ComponentType<EntityStore, AchievementComponent> getComponentType() {
        return Realmweavers.get().getAchievementComponentType();
    }

    public boolean isUnlocked(@Nonnull String achievementId) {
        return !achievementId.isBlank() && unlockedAchievements.containsKey(achievementId);
    }

    @Nonnull
    public Map<String, AchievementUnlockRecord> getUnlockedAchievements() {
        return unlockedAchievements;
    }

    @Nonnull
    public Map<String, AchievementProgress> getProgressTracking() {
        return progressTracking;
    }

    @Nonnull
    public AchievementProgress getOrCreateProgress(@Nonnull String achievementId) {
        return progressTracking.computeIfAbsent(achievementId, AchievementProgress::new);
    }

    public int getTotalAchievementPoints() {
        return totalAchievementPoints;
    }

    @Nonnull
    public String getDisplayedTitle() {
        return displayedTitle == null ? "" : displayedTitle;
    }

    public void setDisplayedTitle(String displayedTitle) {
        this.displayedTitle = displayedTitle == null ? "" : displayedTitle;
    }

    @Nonnull
    public String getDisplayedTitlePrefix() {
        return displayedTitlePrefix == null ? "" : displayedTitlePrefix;
    }

    public void setDisplayedTitlePrefix(String displayedTitlePrefix) {
        this.displayedTitlePrefix = displayedTitlePrefix == null ? "" : displayedTitlePrefix;
    }

    @Nonnull
    public String getDisplayedTitleSuffix() {
        return displayedTitleSuffix == null ? "" : displayedTitleSuffix;
    }

    public void setDisplayedTitleSuffix(String displayedTitleSuffix) {
        this.displayedTitleSuffix = displayedTitleSuffix == null ? "" : displayedTitleSuffix;
    }

    public void applyDisplayedTitle(String displayedTitle, String displayedTitlePrefix, String displayedTitleSuffix) {
        setDisplayedTitle(displayedTitle);
        setDisplayedTitlePrefix(displayedTitlePrefix);
        setDisplayedTitleSuffix(displayedTitleSuffix);
    }

    public boolean hasDisplayedTitle() {
        return !getDisplayedTitle().isBlank()
                || !getDisplayedTitlePrefix().isBlank()
                || !getDisplayedTitleSuffix().isBlank();
    }

    public boolean unlock(@Nonnull String achievementId, int pointsAwarded) {
        if (achievementId.isBlank() || isUnlocked(achievementId)) {
            return false;
        }

        unlockedAchievements.put(achievementId,
                new AchievementUnlockRecord(achievementId, System.currentTimeMillis(), Math.max(0, pointsAwarded)));
        totalAchievementPoints += Math.max(0, pointsAwarded);
        return true;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        AchievementComponent cloned = new AchievementComponent();
        cloned.totalAchievementPoints = this.totalAchievementPoints;
        cloned.displayedTitle = this.getDisplayedTitle();
        cloned.displayedTitlePrefix = this.getDisplayedTitlePrefix();
        cloned.displayedTitleSuffix = this.getDisplayedTitleSuffix();
        cloned.unlockedAchievements = new HashMap<>();
        for (Map.Entry<String, AchievementUnlockRecord> entry : this.unlockedAchievements.entrySet()) {
            cloned.unlockedAchievements.put(entry.getKey(), entry.getValue().copy());
        }
        cloned.progressTracking = new HashMap<>();
        for (Map.Entry<String, AchievementProgress> entry : this.progressTracking.entrySet()) {
            cloned.progressTracking.put(entry.getKey(), entry.getValue().copy());
        }
        return cloned;
    }

    public static final class AchievementUnlockRecord {
        private String achievementId;
        private long unlockedAtEpochMs;
        private int pointsAwarded;

        public AchievementUnlockRecord() {
            this("", 0L, 0);
        }

        public AchievementUnlockRecord(String achievementId, long unlockedAtEpochMs, int pointsAwarded) {
            this.achievementId = achievementId == null ? "" : achievementId;
            this.unlockedAtEpochMs = unlockedAtEpochMs;
            this.pointsAwarded = pointsAwarded;
        }

        @Nonnull
        public String getAchievementId() {
            return achievementId == null ? "" : achievementId;
        }

        public long getUnlockedAtEpochMs() {
            return unlockedAtEpochMs;
        }

        public int getPointsAwarded() {
            return pointsAwarded;
        }

        @Nonnull
        public AchievementUnlockRecord copy() {
            return new AchievementUnlockRecord(getAchievementId(), unlockedAtEpochMs, pointsAwarded);
        }
    }

    public static final class AchievementProgress {
        private String achievementId;
        private Map<String, Integer> criteriaProgress;
        private long startDateEpochMs;

        public AchievementProgress() {
            this("");
        }

        public AchievementProgress(String achievementId) {
            this.achievementId = achievementId == null ? "" : achievementId;
            this.criteriaProgress = new HashMap<>();
            this.startDateEpochMs = System.currentTimeMillis();
        }

        @Nonnull
        public String getAchievementId() {
            return achievementId == null ? "" : achievementId;
        }

        @Nonnull
        public Map<String, Integer> getCriteriaProgress() {
            return criteriaProgress;
        }

        public int getCriterionProgress(@Nonnull String criterionId) {
            return criteriaProgress.getOrDefault(criterionId, 0);
        }

        public boolean recordProgress(@Nonnull String criterionId, int currentValue) {
            if (criterionId.isBlank()) {
                return false;
            }
            int sanitizedValue = Math.max(0, currentValue);
            int previous = criteriaProgress.getOrDefault(criterionId, 0);
            if (sanitizedValue <= previous) {
                return false;
            }
            criteriaProgress.put(criterionId, sanitizedValue);
            return true;
        }

        public long getStartDateEpochMs() {
            return startDateEpochMs;
        }

        @Nonnull
        public AchievementProgress copy() {
            AchievementProgress copy = new AchievementProgress(getAchievementId());
            copy.startDateEpochMs = this.startDateEpochMs;
            copy.criteriaProgress = new HashMap<>(this.criteriaProgress);
            return copy;
        }
    }
}
