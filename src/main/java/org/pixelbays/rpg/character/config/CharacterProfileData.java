package org.pixelbays.rpg.character.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceData;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.race.component.RaceComponent;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;

@SuppressWarnings({ "deprecation", "removal", "null" })
public class CharacterProfileData {

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
                .append(new KeyedCodec<>("Hardcore", Codec.BOOLEAN, false, true),
                    (i, s) -> i.hardcore = s, i -> i.hardcore)
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
            .append(new KeyedCodec<>("Appearance", CharacterAppearanceData.CODEC, false, true),
                    (i, s) -> i.appearance = s, i -> i.appearance)
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
    private boolean hardcore;
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
    private CharacterAppearanceData appearance;

    public CharacterProfileData() {
        this.characterId = "";
        this.characterName = "";
        this.raceId = "";
        this.primaryClassId = "";
        this.createdAtEpochMs = 0L;
        this.lastPlayedEpochMs = 0L;
        this.softDeleted = false;
        this.deletedAtEpochMs = 0L;
        this.hardcore = false;
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
        this.appearance = new CharacterAppearanceData();
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

    public boolean isHardcore() {
        return hardcore;
    }

    public void setHardcore(boolean hardcore) {
        this.hardcore = hardcore;
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
    public CharacterAppearanceData getAppearance() {
        return appearance == null ? new CharacterAppearanceData() : appearance;
    }

    public void setAppearance(@Nullable CharacterAppearanceData appearance) {
        this.appearance = appearance == null ? new CharacterAppearanceData() : appearance.copy();
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
        copy.hardcore = hardcore;
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
        copy.appearance = getAppearance().copy();
        return copy;
    }

    @Nonnull
    private static Inventory copyInventorySnapshot(@Nullable Inventory inventory) {
        if (inventory == null) {
            return new Inventory();
        }

        Inventory copy = new Inventory();
        copyContainerContents(inventory.getStorage(), copy.getStorage());
        copyContainerContents(inventory.getArmor(), copy.getArmor());
        copyContainerContents(inventory.getHotbar(), copy.getHotbar());
        copyContainerContents(inventory.getUtility(), copy.getUtility());
        copyContainerContents(inventory.getTools(), copy.getTools());
        copyContainerContents(inventory.getBackpack(), copy.getBackpack());
        return copy;
    }

    private static void copyContainerContents(@Nullable ItemContainer source, @Nullable ItemContainer target) {
        if (target == null) {
            return;
        }
        target.clear();
        if (source == null) {
            return;
        }

        short maxSlots = (short) Math.min(source.getCapacity(), target.getCapacity());
        for (short slot = 0; slot < maxSlots; slot++) {
            ItemStack itemStack = source.getItemStack(slot);
            if (!ItemStack.isEmpty(itemStack)) {
                target.setItemStackForSlot(slot, new ItemStack(
                        itemStack.getItemId(),
                        itemStack.getQuantity(),
                        itemStack.getDurability(),
                        itemStack.getMaxDurability(),
                        itemStack.getMetadata() == null ? null : itemStack.getMetadata().clone()));
            }
        }
    }

    public int getCharacterLevel() {
        LevelProgressionComponent.LevelSystemData system = getLevelProgression().getSystem("Base_Character_Level");
        return system == null ? 1 : Math.max(1, system.getCurrentLevel());
    }
}
