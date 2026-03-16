package org.pixelbays.rpg.character.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.race.component.RaceComponent;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class CharacterRosterData implements JsonAssetWithMap<String, DefaultAssetMap<String, CharacterRosterData>> {

    private static final FunctionCodec<CharacterProfileData[], List<CharacterProfileData>> PROFILE_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(CharacterProfileData.CODEC, CharacterProfileData[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(CharacterProfileData[]::new));
        private static final FunctionCodec<Long[], List<Long>> LONG_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(Codec.LONG, Long[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? null : list.toArray(Long[]::new));

    public static final AssetBuilderCodec<String, CharacterRosterData> CODEC = AssetBuilderCodec.builder(
            CharacterRosterData.class,
            CharacterRosterData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            CharacterRosterData::ensureAssetData)
            .append(new KeyedCodec<>("AccountUsername", Codec.STRING, false, true),
                    (i, s) -> i.accountUsername = s, i -> i.accountUsername)
            .add()
            .append(new KeyedCodec<>("SelectedCharacterId", Codec.STRING, false, true),
                    (i, s) -> i.selectedCharacterId = s, i -> i.selectedCharacterId)
            .add()
                .append(new KeyedCodec<>("AccountAchievementProgress", AchievementComponent.CODEC, false, true),
                    (i, s) -> i.accountAchievementProgress = s, i -> i.accountAchievementProgress)
                .add()
                .append(new KeyedCodec<>("RecoveryHistoryEpochMs", LONG_LIST_CODEC, false, true),
                    (i, s) -> i.recoveryHistoryEpochMs = s, i -> i.recoveryHistoryEpochMs)
                .add()
            .append(new KeyedCodec<>("Profiles", PROFILE_LIST_CODEC, false, true),
                    (i, s) -> i.profiles = s, i -> i.profiles)
            .add()
            .build();

    private static DefaultAssetMap<String, CharacterRosterData> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private String accountUsername;
    private String selectedCharacterId;
    private AchievementComponent accountAchievementProgress;
    private List<Long> recoveryHistoryEpochMs;
    private List<CharacterProfileData> profiles;

    public CharacterRosterData() {
        this.data = new AssetExtraInfo.Data(CharacterRosterData.class, "", null);
        this.id = "";
        this.accountUsername = "";
        this.selectedCharacterId = "";
        this.accountAchievementProgress = new AchievementComponent();
        this.recoveryHistoryEpochMs = new ArrayList<>();
        this.profiles = new ArrayList<>();
    }

    @Nonnull
    private AssetExtraInfo.Data ensureAssetData() {
        if (data == null) {
            data = new AssetExtraInfo.Data(CharacterRosterData.class, getId(), null);
        }
        return Objects.requireNonNull(data);
    }

    @Nullable
    public static DefaultAssetMap<String, CharacterRosterData> getAssetMap() {
        if (ASSET_MAP == null) {
            var assetStore = AssetRegistry.getAssetStore(CharacterRosterData.class);
            if (assetStore != null) {
                ASSET_MAP = (DefaultAssetMap<String, CharacterRosterData>) assetStore.getAssetMap();
            }
        }

        return ASSET_MAP;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id == null ? "" : id;
    }

    @Nonnull
    public String getAccountUsername() {
        return accountUsername == null ? "" : accountUsername;
    }

    public void setAccountUsername(@Nullable String accountUsername) {
        this.accountUsername = accountUsername == null ? "" : accountUsername;
    }

    @Nonnull
    public String getSelectedCharacterId() {
        return selectedCharacterId == null ? "" : selectedCharacterId;
    }

    public void setSelectedCharacterId(@Nullable String selectedCharacterId) {
        this.selectedCharacterId = selectedCharacterId == null ? "" : selectedCharacterId;
    }

    @Nonnull
    public AchievementComponent getAccountAchievementProgress() {
        if (accountAchievementProgress == null) {
            accountAchievementProgress = new AchievementComponent();
        }
        AchievementComponent progress = accountAchievementProgress;
        return progress == null ? new AchievementComponent() : progress;
    }

    public void setAccountAchievementProgress(@Nullable AchievementComponent accountAchievementProgress) {
        this.accountAchievementProgress = accountAchievementProgress == null
                ? new AchievementComponent()
                : (AchievementComponent) accountAchievementProgress.clone();
    }

    @Nonnull
    public List<Long> getRecoveryHistoryEpochMs() {
        List<Long> history = recoveryHistoryEpochMs;
        if (history == null) {
            history = new ArrayList<>();
            recoveryHistoryEpochMs = history;
        }
        return history;
    }

    public void setRecoveryHistoryEpochMs(@Nullable List<Long> recoveryHistoryEpochMs) {
        this.recoveryHistoryEpochMs = recoveryHistoryEpochMs == null ? new ArrayList<>() : new ArrayList<>(recoveryHistoryEpochMs);
    }

    @Nonnull
    public List<CharacterProfileData> getProfiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        return profiles == null ? new ArrayList<>() : profiles;
    }

    @Nullable
    public CharacterProfileData getProfile(@Nullable String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return null;
        }
        for (CharacterProfileData profile : getProfiles()) {
            if (profile != null && characterId.equalsIgnoreCase(profile.getCharacterId())) {
                return profile;
            }
        }
        return null;
    }

    public int getActiveProfileCount() {
        int count = 0;
        for (CharacterProfileData profile : getProfiles()) {
            if (profile != null && !profile.isSoftDeleted()) {
                count++;
            }
        }
        return count;
    }

    public void upsertProfile(@Nonnull CharacterProfileData profile) {
        CharacterProfileData existing = getProfile(profile.getCharacterId());
        if (existing != null) {
            int index = getProfiles().indexOf(existing);
            getProfiles().set(index, profile);
            return;
        }
        getProfiles().add(profile);
    }

    public static class CharacterProfileData {

        public static final BuilderCodec<CharacterProfileData> CODEC = BuilderCodec
                .builder(CharacterProfileData.class, CharacterProfileData::new)
                .append(new KeyedCodec<>("CharacterId", Codec.STRING, false, true),
                        (i, s) -> i.characterId = s, i -> i.characterId)
                .add()
                .append(new KeyedCodec<>("CharacterName", Codec.STRING, false, true),
                        (i, s) -> i.characterName = s, i -> i.characterName)
                .add()
                .append(new KeyedCodec<>("RaceId", Codec.STRING, false, true),
                        (i, s) -> i.raceId = s, i -> i.raceId)
                .add()
                .append(new KeyedCodec<>("PrimaryClassId", Codec.STRING, false, true),
                        (i, s) -> i.primaryClassId = s, i -> i.primaryClassId)
                .add()
                .append(new KeyedCodec<>("CreatedAtEpochMs", Codec.LONG, false, true),
                        (i, s) -> i.createdAtEpochMs = s, i -> i.createdAtEpochMs)
                .add()
                .append(new KeyedCodec<>("LastPlayedEpochMs", Codec.LONG, false, true),
                        (i, s) -> i.lastPlayedEpochMs = s, i -> i.lastPlayedEpochMs)
                .add()
                .append(new KeyedCodec<>("SoftDeleted", Codec.BOOLEAN, false, true),
                        (i, s) -> i.softDeleted = s, i -> i.softDeleted)
                .add()
                .append(new KeyedCodec<>("DeletedAtEpochMs", Codec.LONG, false, true),
                        (i, s) -> i.deletedAtEpochMs = s, i -> i.deletedAtEpochMs)
                .add()
                .append(new KeyedCodec<>("LevelProgression", LevelProgressionComponent.CODEC, false, true),
                        (i, s) -> i.levelProgression = s, i -> i.levelProgression)
                .add()
                .append(new KeyedCodec<>("ClassProgression", ClassComponent.CODEC, false, true),
                        (i, s) -> i.classProgression = s, i -> i.classProgression)
                .add()
                .append(new KeyedCodec<>("RaceProgression", RaceComponent.CODEC, false, true),
                        (i, s) -> i.raceProgression = s, i -> i.raceProgression)
                .add()
                .append(new KeyedCodec<>("ClassAbilities", ClassAbilityComponent.CODEC, false, true),
                        (i, s) -> i.classAbilities = s, i -> i.classAbilities)
                .add()
                .append(new KeyedCodec<>("AbilityBindings", AbilityBindingComponent.CODEC, false, true),
                        (i, s) -> i.abilityBindings = s, i -> i.abilityBindings)
                .add()
                .append(new KeyedCodec<>("AchievementProgress", AchievementComponent.CODEC, false, true),
                    (i, s) -> i.achievementProgress = s, i -> i.achievementProgress)
                .add()
                .append(new KeyedCodec<>("StatSnapshot", EntityStatMap.CODEC, false, true),
                    (i, s) -> i.statSnapshot = s, i -> i.statSnapshot)
                .add()
                .append(new KeyedCodec<>("SavedWorldName", Codec.STRING, false, true),
                    (i, s) -> i.savedWorldName = s, i -> i.savedWorldName)
                .add()
                .append(new KeyedCodec<>("SavedTransform", TransformComponent.CODEC, false, true),
                    (i, s) -> i.savedTransform = s, i -> i.savedTransform)
                .add()
                .append(new KeyedCodec<>("InventorySnapshot", Inventory.CODEC, false, true),
                    (i, s) -> i.inventorySnapshot = s, i -> i.inventorySnapshot)
                .add()
                .append(new KeyedCodec<>("ActiveHotbarSlot", Codec.BYTE, false, true),
                    (i, s) -> i.activeHotbarSlot = s, i -> i.activeHotbarSlot)
                .add()
                .append(new KeyedCodec<>("ActiveUtilitySlot", Codec.BYTE, false, true),
                    (i, s) -> i.activeUtilitySlot = s, i -> i.activeUtilitySlot)
                .add()
                .append(new KeyedCodec<>("ActiveToolsSlot", Codec.BYTE, false, true),
                    (i, s) -> i.activeToolsSlot = s, i -> i.activeToolsSlot)
                .add()
                .build();

        private String characterId;
        private String characterName;
        private String raceId;
        private String primaryClassId;
        private long createdAtEpochMs;
        private long lastPlayedEpochMs;
        private boolean softDeleted;
        private long deletedAtEpochMs;
        private LevelProgressionComponent levelProgression;
        private ClassComponent classProgression;
        private RaceComponent raceProgression;
        private ClassAbilityComponent classAbilities;
        private AbilityBindingComponent abilityBindings;
        private AchievementComponent achievementProgress;
        private EntityStatMap statSnapshot;
        private String savedWorldName;
        private TransformComponent savedTransform;
        private Inventory inventorySnapshot;
        private byte activeHotbarSlot;
        private byte activeUtilitySlot;
        private byte activeToolsSlot;

        public CharacterProfileData() {
            this.characterId = "";
            this.characterName = "";
            this.raceId = "";
            this.primaryClassId = "";
            this.createdAtEpochMs = 0L;
            this.lastPlayedEpochMs = 0L;
            this.softDeleted = false;
            this.deletedAtEpochMs = 0L;
            this.levelProgression = new LevelProgressionComponent();
            this.classProgression = new ClassComponent();
            this.raceProgression = new RaceComponent();
            this.classAbilities = new ClassAbilityComponent();
            this.abilityBindings = new AbilityBindingComponent();
            this.achievementProgress = new AchievementComponent();
            this.statSnapshot = new EntityStatMap();
            this.savedWorldName = "";
            this.savedTransform = new TransformComponent();
            this.inventorySnapshot = new Inventory();
            this.activeHotbarSlot = 0;
            this.activeUtilitySlot = -1;
            this.activeToolsSlot = -1;
        }

        @Nonnull
        public String getCharacterId() {
            return characterId == null ? "" : characterId;
        }

        public void setCharacterId(@Nullable String characterId) {
            this.characterId = characterId == null ? "" : characterId;
        }

        @Nonnull
        public String getCharacterName() {
            return characterName == null ? "" : characterName;
        }

        public void setCharacterName(@Nullable String characterName) {
            this.characterName = characterName == null ? "" : characterName;
        }

        @Nonnull
        public String getRaceId() {
            return raceId == null ? "" : raceId;
        }

        public void setRaceId(@Nullable String raceId) {
            this.raceId = raceId == null ? "" : raceId;
        }

        @Nonnull
        public String getPrimaryClassId() {
            return primaryClassId == null ? "" : primaryClassId;
        }

        public void setPrimaryClassId(@Nullable String primaryClassId) {
            this.primaryClassId = primaryClassId == null ? "" : primaryClassId;
        }

        public long getCreatedAtEpochMs() {
            return createdAtEpochMs;
        }

        public void setCreatedAtEpochMs(long createdAtEpochMs) {
            this.createdAtEpochMs = createdAtEpochMs;
        }

        public long getLastPlayedEpochMs() {
            return lastPlayedEpochMs;
        }

        public void setLastPlayedEpochMs(long lastPlayedEpochMs) {
            this.lastPlayedEpochMs = lastPlayedEpochMs;
        }

        public boolean isSoftDeleted() {
            return softDeleted;
        }

        public void setSoftDeleted(boolean softDeleted) {
            this.softDeleted = softDeleted;
        }

        public long getDeletedAtEpochMs() {
            return deletedAtEpochMs;
        }

        public void setDeletedAtEpochMs(long deletedAtEpochMs) {
            this.deletedAtEpochMs = deletedAtEpochMs;
        }

        @Nonnull
        public LevelProgressionComponent getLevelProgression() {
            return levelProgression == null ? new LevelProgressionComponent() : levelProgression;
        }

        public void setLevelProgression(@Nullable LevelProgressionComponent levelProgression) {
            this.levelProgression = levelProgression == null ? new LevelProgressionComponent() : levelProgression;
        }

        @Nonnull
        public ClassComponent getClassProgression() {
            return classProgression == null ? new ClassComponent() : classProgression;
        }

        public void setClassProgression(@Nullable ClassComponent classProgression) {
            this.classProgression = classProgression == null ? new ClassComponent() : classProgression;
        }

        @Nonnull
        public RaceComponent getRaceProgression() {
            return raceProgression == null ? new RaceComponent() : raceProgression;
        }

        public void setRaceProgression(@Nullable RaceComponent raceProgression) {
            this.raceProgression = raceProgression == null ? new RaceComponent() : raceProgression;
        }

        @Nonnull
        public ClassAbilityComponent getClassAbilities() {
            return classAbilities == null ? new ClassAbilityComponent() : classAbilities;
        }

        public void setClassAbilities(@Nullable ClassAbilityComponent classAbilities) {
            this.classAbilities = classAbilities == null ? new ClassAbilityComponent() : classAbilities;
        }

        @Nonnull
        public AbilityBindingComponent getAbilityBindings() {
            return abilityBindings == null ? new AbilityBindingComponent() : abilityBindings;
        }

        public void setAbilityBindings(@Nullable AbilityBindingComponent abilityBindings) {
            this.abilityBindings = abilityBindings == null ? new AbilityBindingComponent() : abilityBindings;
        }

        @Nonnull
        public AchievementComponent getAchievementProgress() {
            if (achievementProgress == null) {
                achievementProgress = new AchievementComponent();
            }
            AchievementComponent progress = achievementProgress;
            return progress == null ? new AchievementComponent() : progress;
        }

        public void setAchievementProgress(@Nullable AchievementComponent achievementProgress) {
            this.achievementProgress = achievementProgress == null
                    ? new AchievementComponent()
                    : (AchievementComponent) achievementProgress.clone();
        }

        @Nonnull
        public EntityStatMap getStatSnapshot() {
            return statSnapshot == null ? new EntityStatMap() : statSnapshot;
        }

        public void setStatSnapshot(@Nullable EntityStatMap statSnapshot) {
            this.statSnapshot = statSnapshot == null ? new EntityStatMap() : statSnapshot;
        }

        @Nonnull
        public String getSavedWorldName() {
            return savedWorldName == null ? "" : savedWorldName;
        }

        public void setSavedWorldName(@Nullable String savedWorldName) {
            this.savedWorldName = savedWorldName == null ? "" : savedWorldName;
        }

        @Nonnull
        public TransformComponent getSavedTransform() {
            return savedTransform == null ? new TransformComponent() : savedTransform;
        }

        public void setSavedTransform(@Nullable TransformComponent savedTransform) {
            this.savedTransform = savedTransform == null ? new TransformComponent() : savedTransform;
        }

        @Nonnull
        public Inventory getInventorySnapshot() {
            return inventorySnapshot == null ? new Inventory() : inventorySnapshot;
        }

        public void setInventorySnapshot(@Nullable Inventory inventorySnapshot) {
            this.inventorySnapshot = copyInventorySnapshot(inventorySnapshot);
        }

        public byte getActiveHotbarSlot() {
            return activeHotbarSlot;
        }

        public void setActiveHotbarSlot(byte activeHotbarSlot) {
            this.activeHotbarSlot = activeHotbarSlot;
        }

        public byte getActiveUtilitySlot() {
            return activeUtilitySlot;
        }

        public void setActiveUtilitySlot(byte activeUtilitySlot) {
            this.activeUtilitySlot = activeUtilitySlot;
        }

        public byte getActiveToolsSlot() {
            return activeToolsSlot;
        }

        public void setActiveToolsSlot(byte activeToolsSlot) {
            this.activeToolsSlot = activeToolsSlot;
        }

        @Nonnull
        public CharacterProfileData copy() {
            CharacterProfileData copy = new CharacterProfileData();
            copy.characterId = getCharacterId();
            copy.characterName = getCharacterName();
            copy.raceId = getRaceId();
            copy.primaryClassId = getPrimaryClassId();
            copy.createdAtEpochMs = createdAtEpochMs;
            copy.lastPlayedEpochMs = lastPlayedEpochMs;
            copy.softDeleted = softDeleted;
            copy.deletedAtEpochMs = deletedAtEpochMs;
            copy.levelProgression = (LevelProgressionComponent) getLevelProgression().clone();
            copy.classProgression = (ClassComponent) getClassProgression().clone();
            copy.raceProgression = (RaceComponent) getRaceProgression().clone();
            copy.classAbilities = getClassAbilities().clone();
            copy.abilityBindings = (AbilityBindingComponent) getAbilityBindings().clone();
            copy.achievementProgress = (AchievementComponent) getAchievementProgress().clone();
            copy.statSnapshot = getStatSnapshot().clone();
            copy.savedWorldName = getSavedWorldName();
            copy.savedTransform = getSavedTransform().clone();
            copy.inventorySnapshot = copyInventorySnapshot(getInventorySnapshot());
            copy.activeHotbarSlot = activeHotbarSlot;
            copy.activeUtilitySlot = activeUtilitySlot;
            copy.activeToolsSlot = activeToolsSlot;
            return copy;
        }

        @Nonnull
        private static Inventory copyInventorySnapshot(@Nullable Inventory inventory) {
            if (inventory == null) {
                return new Inventory();
            }

            return new Inventory(
                    inventory.getStorage().clone(),
                    inventory.getArmor().clone(),
                    inventory.getHotbar().clone(),
                    inventory.getUtility().clone(),
                    inventory.getTools().clone(),
                    inventory.getBackpack().clone());
        }

        public int getCharacterLevel() {
            LevelProgressionComponent.LevelSystemData system = getLevelProgression().getSystem("Base_Character_Level");
            return system == null ? 1 : Math.max(1, system.getCurrentLevel());
        }
    }
}
