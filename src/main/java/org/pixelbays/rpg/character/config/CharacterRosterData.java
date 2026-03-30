package org.pixelbays.rpg.character.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.achievement.component.AchievementComponent;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

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
            .append(new KeyedCodec<>("AccountTokens", new MapCodec<>(Codec.LONG, LinkedHashMap::new, false), false, true),
                    (i, s) -> i.accountTokens = s, i -> i.accountTokens)
            .add()
                .append(new KeyedCodec<>("UnlockedExtraCharacterSlots", Codec.INTEGER, false, true),
                    (i, s) -> i.setUnlockedExtraCharacterSlots(s), i -> i.unlockedExtraCharacterSlots)
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
    private Map<String, Long> accountTokens;
    private int unlockedExtraCharacterSlots;
    private List<CharacterProfileData> profiles;

    public CharacterRosterData() {
        this.data = new AssetExtraInfo.Data(CharacterRosterData.class, "", null);
        this.id = "";
        this.accountUsername = "";
        this.selectedCharacterId = "";
        this.accountAchievementProgress = new AchievementComponent();
        this.recoveryHistoryEpochMs = new ArrayList<>();
        this.accountTokens = new LinkedHashMap<>();
        this.unlockedExtraCharacterSlots = 0;
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
    public Map<String, Long> getAccountTokens() {
        Map<String, Long> tokens = accountTokens;
        if (tokens == null) {
            tokens = new LinkedHashMap<>();
            accountTokens = tokens;
        }
        return tokens;
    }

    public void setAccountTokens(@Nullable Map<String, Long> accountTokens) {
        this.accountTokens = accountTokens == null ? new LinkedHashMap<>() : new LinkedHashMap<>(accountTokens);
    }

    public int getUnlockedExtraCharacterSlots() {
        return Math.max(0, unlockedExtraCharacterSlots);
    }

    public void setUnlockedExtraCharacterSlots(int unlockedExtraCharacterSlots) {
        this.unlockedExtraCharacterSlots = Math.max(0, unlockedExtraCharacterSlots);
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

}
