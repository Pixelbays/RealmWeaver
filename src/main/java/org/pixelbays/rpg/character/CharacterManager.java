package org.pixelbays.rpg.character;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.character.config.CharacterRosterData;
import org.pixelbays.rpg.character.config.CharacterRosterData.CharacterProfileData;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectList;

@SuppressWarnings("null")
public final class CharacterManager {

    private static final String BASE_CHARACTER_LEVEL = "Base_Character_Level";

    private final CharacterPersistence persistence = new CharacterPersistence();
    private final Map<UUID, CharacterRosterData> rostersByAccountId = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeCharacterIds = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> pendingLogoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService logoutScheduler = Executors.newSingleThreadScheduledExecutor(new LogoutThreadFactory());
    private volatile boolean persistenceLoaded;

    public CharacterManager() {
    }

    public void clear() {
        rostersByAccountId.clear();
        activeCharacterIds.clear();
        persistenceLoaded = false;
        pendingLogoutTasks.values().forEach(task -> task.cancel(false));
        pendingLogoutTasks.clear();
    }

    public void loadFromAssets() {
        ensurePersistenceLoaded();
    }

    private void ensurePersistenceLoaded() {
        if (persistenceLoaded || EntityStatType.getAssetStore() == null) {
            return;
        }
        synchronized (this) {
            if (persistenceLoaded || EntityStatType.getAssetStore() == null) {
                return;
            }
            reloadFromAssetsInternal();
            persistenceLoaded = true;
        }
    }

    private void reloadFromAssetsInternal() {
        rostersByAccountId.clear();
        for (CharacterRosterData roster : persistence.loadAll()) {
            if (roster == null || roster.getId() == null || roster.getId().isBlank()) {
                continue;
            }
            try {
                rostersByAccountId.put(UUID.fromString(roster.getId()), roster);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Nonnull
    public CharacterRosterData getOrCreateRoster(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = rostersByAccountId.computeIfAbsent(accountId, id -> {
            CharacterRosterData loaded = persistence.loadRoster(id);
            if (loaded != null) {
                return loaded;
            }
            CharacterRosterData created = new CharacterRosterData();
            created.setId(id.toString());
            created.setAccountUsername(username);
            return created;
        });
        roster.setId(accountId.toString());
        roster.setAccountUsername(username);
        cleanupRosterState(roster);
        return roster;
    }

    public void handlePlayerReady(@Nonnull PlayerReadyEvent event) {
        if (!isEnabled()) {
            return;
        }
        loadFromAssets();
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        if (ensureLegacyMigration(roster, ref, store, playerRef)) {
            persistRoster(roster);
        }

        activeCharacterIds.remove(playerRef.getUuid());
        enterCharacterSelect(ref, store, playerRef);
    }

    public void handlePlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        if (!isEnabled()) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        cancelPendingLogout(playerRef.getUuid());

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                if (ref.isValid()) {
                    saveActiveCharacter(playerRef.getUuid(), ref, store);
                }
                activeCharacterIds.remove(playerRef.getUuid());
            });
            return;
        }

        activeCharacterIds.remove(playerRef.getUuid());
    }

    public void enterCharacterSelect(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        if (!isEnabled()) {
            return;
        }
        cancelPendingLogout(playerRef.getUuid());
        activeCharacterIds.remove(playerRef.getUuid());
        moveToLobby(ref, store, playerRef);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new org.pixelbays.rpg.character.ui.CharacterSelectPage(playerRef));
    }

    @Nonnull
    public CharacterActionResult logoutToCharacterSelect(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        CharacterModSettings settings = getSettings();
        if (!settings.isAllowLogoutToCharacterSelect()) {
            return CharacterActionResult.failure("Logout to character select is disabled.");
        }

        int logoutTimerSeconds = Math.max(0, settings.getLogoutTimerSeconds());
        if (logoutTimerSeconds <= 0) {
            performLogoutToCharacterSelect(ref, store, playerRef);
            return CharacterActionResult.success("Logged out to character select.");
        }

        ScheduledFuture<?> existingTask = pendingLogoutTasks.get(playerRef.getUuid());
        if (existingTask != null && !existingTask.isDone()) {
            return CharacterActionResult.failure("Logout is already pending.");
        }

        World world = store.getExternalData().getWorld();

        ScheduledFuture<?> scheduled = logoutScheduler.schedule(() -> world.execute(() -> {
            pendingLogoutTasks.remove(playerRef.getUuid());
            if (!ref.isValid()) {
                return;
            }
            performLogoutToCharacterSelect(ref, store, playerRef);
            playerRef.sendMessage(mapMessage("Logged out to character select."));
        }), logoutTimerSeconds, TimeUnit.SECONDS);
        pendingLogoutTasks.put(playerRef.getUuid(), scheduled);
        return CharacterActionResult.success("Logout timer started.");
    }

    private void performLogoutToCharacterSelect(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        saveActiveCharacter(playerRef.getUuid(), ref, store);
        playConfiguredVfx(ref, store, getSettings().getLogoutVfx());
        enterCharacterSelect(ref, store, playerRef);
    }

    private void cancelPendingLogout(@Nonnull UUID accountId) {
        ScheduledFuture<?> task = pendingLogoutTasks.remove(accountId);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Nonnull
    public CharacterActionResult createCharacter(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String rawName,
            @Nullable String rawRaceId,
            @Nullable String rawStarterClassId) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterModSettings settings = getSettings();
        if (!settings.isAllowCharacterCreation()) {
            return CharacterActionResult.failure("Character creation is disabled.");
        }
        if (roster.getActiveProfileCount() >= Math.max(1, settings.getMaxCharacterSlots())) {
            return CharacterActionResult.failure("You have reached the maximum number of character slots.");
        }

        String name = rawName == null ? "" : rawName.trim();
        String raceId = rawRaceId == null ? "" : rawRaceId.trim();
        String starterClassId = rawStarterClassId == null ? "" : rawStarterClassId.trim();
        if (settings.isRequireStarterClassOnCreation() && starterClassId.isEmpty()) {
            starterClassId = resolveDefaultStarterClassId();
        }

        String validationError = validateNewCharacter(roster, name, raceId, starterClassId, settings);
        if (validationError != null) {
            return CharacterActionResult.failure(validationError);
        }

        CharacterProfileData profile = buildNewProfile(name, raceId, starterClassId, playerRef);
        roster.upsertProfile(profile);
        if (roster.getSelectedCharacterId().isBlank()) {
            roster.setSelectedCharacterId(profile.getCharacterId());
        }
        persistRoster(roster);
        return CharacterActionResult.success("Character created.", profile);
    }

    @Nonnull
    public CharacterActionResult selectCharacter(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String rawCharacterId) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        String characterId = rawCharacterId == null ? "" : rawCharacterId.trim();
        if (characterId.isEmpty()) {
            return CharacterActionResult.failure("Character id is required.");
        }

        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterProfileData target = roster.getProfile(characterId);
        if (target == null) {
            return CharacterActionResult.failure("Character not found.");
        }
        if (target.isSoftDeleted()) {
            return CharacterActionResult.failure("Character is pending recovery and cannot be selected.");
        }

        cancelPendingLogout(playerRef.getUuid());
        saveActiveCharacter(playerRef.getUuid(), ref, store);
        applyProfileToPlayer(target, ref, store);
        activeCharacterIds.put(playerRef.getUuid(), target.getCharacterId());
        roster.setSelectedCharacterId(target.getCharacterId());
        target.setLastPlayedEpochMs(System.currentTimeMillis());
        persistRoster(roster);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }

        playConfiguredVfx(ref, store, getSettings().getLoginVfx());
        moveToGameplayWorld(ref, store, playerRef, target);
        playerRef.sendMessage(mapMessage("Selected character."));
        return CharacterActionResult.success("Selected character.", target);
    }

    @Nonnull
    public CharacterActionResult deleteCharacter(@Nonnull PlayerRef playerRef, @Nullable String rawCharacterId) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        String characterId = rawCharacterId == null ? "" : rawCharacterId.trim();
        if (characterId.isEmpty()) {
            return CharacterActionResult.failure("Character id is required.");
        }

        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterProfileData target = roster.getProfile(characterId);
        if (target == null) {
            return CharacterActionResult.failure("Character not found.");
        }

        CharacterModSettings settings = getSettings();
        if (!settings.isAllowCharacterDeletion()) {
            return CharacterActionResult.failure("Character deletion is disabled.");
        }

        if (settings.usesSoftDeleteRecovery()) {
            target.setSoftDeleted(true);
            target.setDeletedAtEpochMs(System.currentTimeMillis());
            persistRoster(roster);
            return CharacterActionResult.success("Character marked for recovery.", target);
        }

        roster.getProfiles().remove(target);
        if (characterId.equalsIgnoreCase(roster.getSelectedCharacterId())) {
            roster.setSelectedCharacterId("");
        }
        persistRoster(roster);
        return CharacterActionResult.success("Character deleted.");
    }

    @Nonnull
    public CharacterActionResult recoverCharacter(@Nonnull PlayerRef playerRef, @Nullable String rawCharacterId) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        String characterId = rawCharacterId == null ? "" : rawCharacterId.trim();
        if (characterId.isEmpty()) {
            return CharacterActionResult.failure("Character id is required.");
        }

        CharacterModSettings settings = getSettings();
        if (!settings.usesSoftDeleteRecovery()) {
            return CharacterActionResult.failure("Soft-delete recovery is disabled.");
        }

        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterProfileData target = roster.getProfile(characterId);
        if (target == null) {
            return CharacterActionResult.failure("Character not found.");
        }
        if (!target.isSoftDeleted()) {
            return CharacterActionResult.failure("Character is not deleted.");
        }

        pruneRecoveryHistory(roster, settings);
        if (isRecoveryRateLimited(roster, settings)) {
            return CharacterActionResult.failure("Recovery limit reached.");
        }

        long recoveryWindowMs = Math.max(0, settings.getDeletedCharacterRetentionHours()) * 3_600_000L;
        if (recoveryWindowMs > 0 && target.getDeletedAtEpochMs() > 0
                && System.currentTimeMillis() - target.getDeletedAtEpochMs() > recoveryWindowMs) {
            roster.getProfiles().remove(target);
            persistRoster(roster);
            return CharacterActionResult.failure("Recovery window has expired.");
        }

        target.setSoftDeleted(false);
        target.setDeletedAtEpochMs(0L);
        roster.getRecoveryHistoryEpochMs().add(System.currentTimeMillis());
        persistRoster(roster);
        return CharacterActionResult.success("Character recovered.", target);
    }

    public void saveActiveCharacter(@Nonnull UUID accountId,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        if (!isEnabled()) {
            return;
        }
        CharacterRosterData roster = getOrCreateRoster(accountId, resolvePlayerUsername(store, ref));
        String activeCharacterId = activeCharacterIds.get(accountId);
        if (activeCharacterId == null || activeCharacterId.isBlank()) {
            activeCharacterId = roster.getSelectedCharacterId();
        }
        if (activeCharacterId == null || activeCharacterId.isBlank()) {
            return;
        }

        CharacterProfileData active = roster.getProfile(activeCharacterId);
        if (active == null) {
            return;
        }

        captureIntoProfile(active, ref, store);
        active.setLastPlayedEpochMs(System.currentTimeMillis());
        persistRoster(roster);
    }

    @Nonnull
    public List<CharacterProfileData> getProfilesFor(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        List<CharacterProfileData> profiles = new ArrayList<>(roster.getProfiles());
        profiles.sort(Comparator.comparing(CharacterProfileData::isSoftDeleted)
                .thenComparing(CharacterProfileData::getLastPlayedEpochMs).reversed());
        return profiles;
    }

    @Nonnull
    public String getActiveCharacterId(@Nonnull UUID accountId) {
        return activeCharacterIds.getOrDefault(accountId, "");
    }

    @Nonnull
    public String getLastSelectedCharacterId(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        return roster.getSelectedCharacterId();
    }

    @Nonnull
    public List<UUID> getKnownAccountIds() {
        return new ArrayList<>(rostersByAccountId.keySet());
    }

    @Nonnull
    public String resolveAccountUsername(@Nonnull UUID accountId) {
        CharacterRosterData roster = rostersByAccountId.get(accountId);
        return roster == null ? "" : roster.getAccountUsername();
    }

    @Nonnull
    public String resolveCharacterOwnerId(@Nonnull UUID accountId) {
        String activeCharacterId = getActiveCharacterId(accountId);
        if (!activeCharacterId.isBlank()) {
            return activeCharacterId;
        }

        CharacterRosterData roster = rostersByAccountId.get(accountId);
        if (roster == null) {
            roster = getOrCreateRoster(accountId, null);
        }

        String selectedCharacterId = roster.getSelectedCharacterId();
        if (!selectedCharacterId.isBlank()) {
            CharacterProfileData selectedProfile = roster.getProfile(selectedCharacterId);
            if (selectedProfile != null && !selectedProfile.isSoftDeleted()) {
                return selectedProfile.getCharacterId();
            }
        }

        for (CharacterProfileData profile : roster.getProfiles()) {
            if (profile != null && !profile.isSoftDeleted() && profile.getCharacterId() != null
                    && !profile.getCharacterId().isBlank()) {
                return profile.getCharacterId();
            }
        }

        return "";
    }

    @Nonnull
    public String resolveCharacterOwnerId(@Nonnull PlayerRef playerRef) {
        return resolveCharacterOwnerId(playerRef.getUuid());
    }

    @Nonnull
    public String resolveBackgroundId(@Nullable CharacterProfileData profile) {
        CharacterModSettings settings = getSettings();
        if (profile == null) {
            return settings.getDefaultLobbyBackgroundId();
        }
        if (settings.getLobbyBackgroundMode() == CharacterModSettings.LobbyBackgroundMode.RaceSpecific) {
            return settings.getRaceLobbyBackgroundIds().getOrDefault(profile.getRaceId(), settings.getDefaultLobbyBackgroundId());
        }
        return settings.getDefaultLobbyBackgroundId();
    }

    @Nonnull
    public String resolvePreviewCameraId(@Nullable CharacterProfileData profile) {
        CharacterModSettings settings = getSettings();
        if (profile == null) {
            return settings.getDefaultPreviewCameraPresetId();
        }
        if (settings.getPreviewCameraMode() == CharacterModSettings.PreviewCameraMode.RaceSpecific) {
            return settings.getRacePreviewCameraPresetIds().getOrDefault(profile.getRaceId(), settings.getDefaultPreviewCameraPresetId());
        }
        return settings.getDefaultPreviewCameraPresetId();
    }

    public int resolveDisplayedLevel(@Nullable CharacterProfileData profile) {
        if (profile == null) {
            return 1;
        }

        LevelProgressionComponent levelProgression = profile.getLevelProgression();

        String primaryClassId = profile.getPrimaryClassId();
        if (!primaryClassId.isBlank()) {
            ClassDefinition classDefinition = resolveClassDefinition(primaryClassId);
            String systemId = primaryClassId;
            if (classDefinition != null) {
                systemId = classDefinition.usesCharacterLevel()
                        ? BASE_CHARACTER_LEVEL
                        : classDefinition.getLevelSystemId();
                if (systemId == null || systemId.isBlank()) {
                    systemId = primaryClassId;
                }
            }

            LevelProgressionComponent.LevelSystemData classSystem = levelProgression.getSystem(systemId);
            if (classSystem != null) {
                return Math.max(1, classSystem.getCurrentLevel());
            }
        }

        int bestLevel = 1;
        for (var entry : levelProgression.getAllSystems().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            bestLevel = Math.max(bestLevel, entry.getValue().getCurrentLevel());
        }
        return bestLevel;
    }

    public boolean isEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isCharacterModuleEnabled();
    }

    @Nonnull
    private CharacterProfileData buildNewProfile(@Nonnull String name,
            @Nonnull String raceId,
            @Nonnull String starterClassId,
            @Nullable PlayerRef playerRef) {
        CharacterProfileData profile = new CharacterProfileData();
        profile.setCharacterId(UUID.randomUUID().toString());
        profile.setCharacterName(name);
        profile.setCreatedAtEpochMs(System.currentTimeMillis());
        profile.setLastPlayedEpochMs(System.currentTimeMillis());

        LevelProgressionComponent levelProgression = new LevelProgressionComponent();
        initializeProfileLevelSystem(levelProgression, BASE_CHARACTER_LEVEL, 1, playerRef);

        RaceComponent raceComponent = new RaceComponent();
        raceComponent.setRaceId(raceId);
        RaceDefinition raceDefinition = resolveRaceDefinition(raceId);
        if (raceDefinition != null) {
            initializeProfileLevelSystem(levelProgression, BASE_CHARACTER_LEVEL,
                    raceDefinition.getInitialCharacterLevel(), playerRef);
        }
        if (raceDefinition != null && raceDefinition.getAbilityIds() != null) {
            for (String abilityId : raceDefinition.getAbilityIds()) {
                if (abilityId != null && !abilityId.isBlank()) {
                    raceComponent.unlockRaceAbility(abilityId);
                }
            }
        }
        profile.setRaceProgression(raceComponent);
        profile.setRaceId(raceId);

        ClassComponent classComponent = new ClassComponent();
        ClassAbilityComponent classAbilities = new ClassAbilityComponent();
        if (!starterClassId.isBlank()) {
            classComponent.learnClass(starterClassId);
            classComponent.prioritizeClass(starterClassId);
            profile.setPrimaryClassId(starterClassId);
            ClassDefinition classDefinition = resolveClassDefinition(starterClassId);
            if (classDefinition != null) {
                String classSystemId = classDefinition.usesCharacterLevel()
                        ? BASE_CHARACTER_LEVEL
                        : classDefinition.getLevelSystemId();
                if (classSystemId != null && !classSystemId.isBlank()) {
                    initializeProfileLevelSystem(levelProgression, classSystemId,
                            classDefinition.getInitialClassLevel(), playerRef);
                }

                int unlockLevel = 1;
                if (classSystemId != null && !classSystemId.isBlank()) {
                    LevelProgressionComponent.LevelSystemData classLevelData = levelProgression.getSystem(classSystemId);
                    if (classLevelData != null) {
                        unlockLevel = Math.max(1, classLevelData.getCurrentLevel());
                    }
                }

                for (ClassDefinition.AbilityUnlock unlock : classDefinition.getAbilityUnlocksAtLevel(unlockLevel)) {
                    if (unlock != null && unlock.getAbilityId() != null && !unlock.getAbilityId().isBlank()) {
                        classAbilities.unlockAbility(unlock.getAbilityId(), starterClassId, 1);
                    }
                }
            }
        }
        profile.setLevelProgression(levelProgression);
        profile.setClassProgression(classComponent);
        profile.setClassAbilities(classAbilities);
        profile.setAbilityBindings(new AbilityBindingComponent());
        profile.setStatSnapshot(new EntityStatMap());
        profile.setInventorySnapshot(new Inventory());
        return profile;
    }

    private void initializeProfileLevelSystem(@Nonnull LevelProgressionComponent levelProgression,
            @Nonnull String systemId,
            int requestedLevel,
            @Nullable PlayerRef playerRef) {
        if (systemId.isBlank()) {
            return;
        }

        LevelProgressionComponent.LevelSystemData levelData = levelProgression.getOrCreateSystem(systemId);
        LevelSystemConfig levelSystemConfig = LevelSystemConfig.getAssetMap() == null
                ? null
                : LevelSystemConfig.getAssetMap().getAsset(systemId);

        int configuredStartingLevel = levelSystemConfig == null ? 1 : Math.max(1, levelSystemConfig.getStartingLevel());
        int currentLevel = Math.max(1, levelData.getCurrentLevel());
        int targetLevel = Math.max(currentLevel, Math.max(configuredStartingLevel, requestedLevel));

        int effectiveMaxLevel = resolveEffectiveProfileMaxLevel(playerRef, levelSystemConfig);
        if (effectiveMaxLevel > 0) {
            targetLevel = Math.min(targetLevel, effectiveMaxLevel);
        }

        levelData.setCurrentLevel(Math.max(1, targetLevel));
        levelData.setCurrentExp(0f);

        if (levelSystemConfig == null) {
            levelData.setExpToNextLevel(100f);
            return;
        }

        if (effectiveMaxLevel > 0 && targetLevel >= effectiveMaxLevel) {
            levelData.setExpToNextLevel(0f);
            return;
        }

        levelData.setExpToNextLevel(levelSystemConfig.calculateExpForLevel(targetLevel + 1));
    }

    private int resolveEffectiveProfileMaxLevel(@Nullable PlayerRef playerRef,
            @Nullable LevelSystemConfig levelSystemConfig) {
        if (levelSystemConfig == null) {
            return 0;
        }

        int baseMaxLevel = levelSystemConfig.getMaxLevel();
        if (baseMaxLevel <= 0) {
            return 0;
        }

        return ExamplePlugin.get().getExpansionManager().getAccessibleLevelCap(playerRef, baseMaxLevel);
    }

    private boolean ensureLegacyMigration(@Nonnull CharacterRosterData roster,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        CharacterModSettings settings = getSettings();
        if (!settings.isAutoMigrateLegacyPlayerData()) {
            return false;
        }
        if (!roster.getProfiles().isEmpty()) {
            return false;
        }

        CharacterProfileData profile = new CharacterProfileData();
        profile.setCharacterId(UUID.randomUUID().toString());
        String configuredName = settings.getLegacyMigrationCharacterName();
        profile.setCharacterName(configuredName == null || configuredName.isBlank()
                ? playerRef.getUsername()
                : configuredName);
        profile.setCreatedAtEpochMs(System.currentTimeMillis());
        profile.setLastPlayedEpochMs(System.currentTimeMillis());
        captureIntoProfile(profile, ref, store);
        roster.upsertProfile(profile);
        roster.setSelectedCharacterId(profile.getCharacterId());
        return true;
    }

    private void captureIntoProfile(@Nonnull CharacterProfileData profile,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        LevelProgressionComponent levelComponent = store.getComponent(ref, LevelProgressionComponent.getComponentType());
        ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
        RaceComponent raceComponent = store.getComponent(ref, RaceComponent.getComponentType());
        ClassAbilityComponent classAbilities = store.getComponent(ref, ClassAbilityComponent.getComponentType());
        AbilityBindingComponent abilityBindings = store.getComponent(ref, AbilityBindingComponent.getComponentType());
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        profile.setLevelProgression(levelComponent == null ? new LevelProgressionComponent() : (LevelProgressionComponent) levelComponent.clone());
        profile.setClassProgression(classComponent == null ? new ClassComponent() : (ClassComponent) classComponent.clone());
        profile.setRaceProgression(raceComponent == null ? new RaceComponent() : (RaceComponent) raceComponent.clone());
        profile.setClassAbilities(classAbilities == null ? new ClassAbilityComponent() : classAbilities.clone());
        profile.setAbilityBindings(abilityBindings == null ? new AbilityBindingComponent() : (AbilityBindingComponent) abilityBindings.clone());
        profile.setStatSnapshot(statMap == null ? new EntityStatMap() : statMap.clone());
        profile.setSavedWorldName(store.getExternalData().getWorld() == null ? "" : store.getExternalData().getWorld().getName());
        profile.setSavedTransform(transformComponent == null ? new TransformComponent() : transformComponent.clone());
        if (player != null && player.getInventory() != null) {
            profile.setInventorySnapshot(copyInventory(player.getInventory()));
            profile.setActiveHotbarSlot(player.getInventory().getActiveHotbarSlot());
            profile.setActiveUtilitySlot(player.getInventory().getActiveUtilitySlot());
            profile.setActiveToolsSlot(player.getInventory().getActiveToolsSlot());
        }
        profile.setRaceId(profile.getRaceProgression().getRaceId());
        profile.setPrimaryClassId(profile.getClassProgression().getPrimaryClassId());
    }

    private void applyProfileToPlayer(@Nonnull CharacterProfileData profile,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        store.putComponent(ref, LevelProgressionComponent.getComponentType(), (LevelProgressionComponent) profile.getLevelProgression().clone());
        store.putComponent(ref, ClassComponent.getComponentType(), (ClassComponent) profile.getClassProgression().clone());
        store.putComponent(ref, RaceComponent.getComponentType(), (RaceComponent) profile.getRaceProgression().clone());
        store.putComponent(ref, ClassAbilityComponent.getComponentType(), profile.getClassAbilities().clone());
        store.putComponent(ref, AbilityBindingComponent.getComponentType(), (AbilityBindingComponent) profile.getAbilityBindings().clone());
        store.putComponent(ref, EntityStatMap.getComponentType(), profile.getStatSnapshot().clone());

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player != null) {
            Inventory restoredInventory = copyInventory(profile.getInventorySnapshot());
            player.setInventory(restoredInventory, false);

            if (profile.getActiveHotbarSlot() >= 0 && profile.getActiveHotbarSlot() < restoredInventory.getHotbar().getCapacity()) {
                restoredInventory.setActiveHotbarSlot(profile.getActiveHotbarSlot());
            }
            if (profile.getActiveUtilitySlot() >= 0 && profile.getActiveUtilitySlot() < restoredInventory.getUtility().getCapacity()) {
                restoredInventory.setActiveUtilitySlot(profile.getActiveUtilitySlot());
            }
            if (profile.getActiveToolsSlot() >= 0 && profile.getActiveToolsSlot() < restoredInventory.getTools().getCapacity()) {
                restoredInventory.setActiveToolsSlot(profile.getActiveToolsSlot());
            }

            if (playerRef != null) {
                playerRef.getPacketHandler().writeNoCache(restoredInventory.toPacket());
            }
        }

        StatSystem statSystem = ExamplePlugin.get().getStatSystem();
        statSystem.recalculateClassStatBonuses(ref, store);
        statSystem.recalculateTalentStatBonuses(ref, store);
        statSystem.recalculateRaceStatBonuses(ref, store);
    }

    @Nonnull
    private Inventory copyInventory(@Nullable Inventory inventory) {
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

    private void cleanupRosterState(@Nonnull CharacterRosterData roster) {
        CharacterModSettings settings = getSettings();
        boolean changed = false;

        pruneRecoveryHistory(roster, settings);

        long retentionMs = Math.max(0, settings.getDeletedCharacterRetentionHours()) * 3_600_000L;
        if (retentionMs > 0) {
            long now = System.currentTimeMillis();
            List<CharacterProfileData> expiredProfiles = new ArrayList<>();
            for (CharacterProfileData profile : roster.getProfiles()) {
                if (profile == null || !profile.isSoftDeleted() || profile.getDeletedAtEpochMs() <= 0) {
                    continue;
                }
                if (now - profile.getDeletedAtEpochMs() > retentionMs) {
                    expiredProfiles.add(profile);
                }
            }

            if (!expiredProfiles.isEmpty()) {
                roster.getProfiles().removeAll(expiredProfiles);
                if (!roster.getSelectedCharacterId().isBlank()
                        && roster.getProfile(roster.getSelectedCharacterId()) == null) {
                    roster.setSelectedCharacterId("");
                }
                changed = true;
            }
        }

        if (changed) {
            persistence.saveRoster(roster);
        }
    }

    private void pruneRecoveryHistory(@Nonnull CharacterRosterData roster, @Nonnull CharacterModSettings settings) {
        int recoveryWindowHours = Math.max(0, settings.getRecoveryWindowHours());
        if (recoveryWindowHours <= 0) {
            return;
        }

        long cutoff = System.currentTimeMillis() - recoveryWindowHours * 3_600_000L;
        roster.getRecoveryHistoryEpochMs().removeIf(timestamp -> timestamp == null || timestamp < cutoff);
    }

    private boolean isRecoveryRateLimited(@Nonnull CharacterRosterData roster, @Nonnull CharacterModSettings settings) {
        int maxRecoveries = settings.getMaxRecoveriesPerWindow();
        int recoveryWindowHours = settings.getRecoveryWindowHours();
        if (maxRecoveries <= 0 || recoveryWindowHours <= 0) {
            return false;
        }

        return roster.getRecoveryHistoryEpochMs().size() >= maxRecoveries;
    }

    private void playConfiguredVfx(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable ModelParticle[] particles) {
        if (particles == null || particles.length == 0) {
            return;
        }

        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            NetworkId networkId = store.getComponent(ref, NetworkId.getComponentType());
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (networkId == null || transform == null) {
                return;
            }

            SpawnModelParticles packet = new SpawnModelParticles(networkId.getId(),
                    toProtocolParticles(particles));

            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                    store.getResource(EntityModule.get().getPlayerSpatialResourceType());
            ObjectList<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(transform.getPosition(), 96.0, nearbyPlayers);
            for (Ref<EntityStore> playerRef : nearbyPlayers) {
                if (!playerRef.isValid()) {
                    continue;
                }

                PlayerRef targetPlayer = store.getComponent(playerRef, PlayerRef.getComponentType());
                if (targetPlayer != null) {
                    targetPlayer.getPacketHandler().writeNoCache(packet);
                }
            }
        });
    }

    private com.hypixel.hytale.protocol.ModelParticle[] toProtocolParticles(@Nonnull ModelParticle[] particles) {
        com.hypixel.hytale.protocol.ModelParticle[] protocolParticles =
                new com.hypixel.hytale.protocol.ModelParticle[particles.length];
        for (int i = 0; i < particles.length; i++) {
            protocolParticles[i] = particles[i].toPacket();
        }
        return protocolParticles;
    }

    private void moveToLobby(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        CharacterModSettings settings = getSettings();
        if (!settings.isUseSharedLobbyWorld()) {
            return;
        }
        World lobbyWorld = Universe.get().getWorld(settings.getLobbyWorldId());
        if (lobbyWorld == null) {
            return;
        }
        teleportToWorldSpawn(ref, store, lobbyWorld, playerRef.getUuid());
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    private void moveToGameplayWorld(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterProfileData profile) {
        World savedWorld = resolveSavedWorld(profile);
        Transform savedTransform = resolveSavedTransform(profile);
        if (savedWorld != null && savedTransform != null) {
            store.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(savedWorld, savedTransform));
            return;
        }

        World world = Universe.get().getDefaultWorld();
        if (world == null) {
            return;
        }
        teleportToWorldSpawn(ref, store, world, playerRef.getUuid());
    }

    @Nullable
    private World resolveSavedWorld(@Nonnull CharacterProfileData profile) {
        String worldName = profile.getSavedWorldName();
        if (worldName.isBlank()) {
            return null;
        }
        return Universe.get().getWorld(worldName);
    }

    @Nullable
    private Transform resolveSavedTransform(@Nonnull CharacterProfileData profile) {
        TransformComponent transformComponent = profile.getSavedTransform();
        return new Transform(transformComponent.getPosition().clone(), transformComponent.getRotation().clone());
    }

    private void teleportToWorldSpawn(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World targetWorld,
            @Nonnull UUID playerId) {
        Transform targetTransform = resolveSpawnTransform(targetWorld, playerId, store, ref);
        store.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, targetTransform));
    }

    @Nonnull
    private Transform resolveSpawnTransform(@Nonnull World targetWorld,
            @Nonnull UUID playerId,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref) {
        ISpawnProvider spawnProvider = targetWorld.getWorldConfig().getSpawnProvider();
        if (spawnProvider != null) {
            Transform transform = spawnProvider.getSpawnPoint(targetWorld, playerId);
            if (transform != null) {
                return transform;
            }
        }

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent != null) {
            return new Transform(transformComponent.getPosition().clone(), transformComponent.getRotation().clone());
        }

        return new Transform(0, 100, 0, 0.0f, 0.0f, 0.0f);
    }

    @Nullable
    private String validateNewCharacter(@Nonnull CharacterRosterData roster,
            @Nonnull String name,
            @Nonnull String raceId,
            @Nonnull String starterClassId,
            @Nonnull CharacterModSettings settings) {
        if (name.isBlank()) {
            return "Character name is required.";
        }
        if (name.length() < Math.max(1, settings.getMinCharacterNameLength())) {
            return "Character name is too short.";
        }
        if (name.length() > Math.max(settings.getMinCharacterNameLength(), settings.getMaxCharacterNameLength())) {
            return "Character name is too long.";
        }
        for (String reserved : settings.getReservedCharacterNames()) {
            if (reserved != null && reserved.equalsIgnoreCase(name)) {
                return "Character name is reserved.";
            }
        }
        if (settings.isRequireUniqueCharacterNames() && isCharacterNameTaken(name)) {
            return "Character name is already taken.";
        }
        if (settings.isRequireRaceOnCreation()) {
            if (raceId.isBlank()) {
                return "Race is required.";
            }
            if (resolveRaceDefinition(raceId) == null) {
                return "Unknown race.";
            }
        }
        if (settings.isRequireStarterClassOnCreation()) {
            if (starterClassId.isBlank()) {
                return "Starter class is required.";
            }
            if (resolveClassDefinition(starterClassId) == null) {
                return "Unknown starter class.";
            }
        } else if (!starterClassId.isBlank() && resolveClassDefinition(starterClassId) == null) {
            return "Unknown starter class.";
        }
        if (!starterClassId.isBlank() && !isAllowedStarterClass(starterClassId)) {
            return "Starter class is not allowed.";
        }
        for (CharacterProfileData existing : roster.getProfiles()) {
            if (existing != null && !existing.isSoftDeleted() && existing.getCharacterName().equalsIgnoreCase(name)) {
                return "You already have a character with that name.";
            }
        }
        return null;
    }

    private boolean isCharacterNameTaken(@Nonnull String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (CharacterRosterData roster : rostersByAccountId.values()) {
            for (CharacterProfileData profile : roster.getProfiles()) {
                if (profile != null && !profile.isSoftDeleted()
                        && profile.getCharacterName().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private RaceDefinition resolveRaceDefinition(@Nullable String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return null;
        }
        RaceManagementSystem raceManagementSystem = ExamplePlugin.get().getRaceManagementSystem();
        RaceDefinition definition = raceManagementSystem.getRaceDefinition(raceId);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    @Nullable
    private ClassDefinition resolveClassDefinition(@Nullable String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        ClassDefinition definition = ExamplePlugin.get().getClassManagementSystem().getClassDefinition(classId);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    @Nonnull
    private String resolveDefaultStarterClassId() {
        List<ClassDefinition> startingClasses = getStartingClassDefinitions();
        if (startingClasses.isEmpty()) {
            return "";
        }

        ClassDefinition firstStartingClass = startingClasses.get(0);
        return firstStartingClass.getId() == null ? "" : firstStartingClass.getId();
    }

    private boolean isAllowedStarterClass(@Nonnull String starterClassId) {
        for (ClassDefinition classDefinition : getStartingClassDefinitions()) {
            if (classDefinition != null && starterClassId.equalsIgnoreCase(classDefinition.getId())) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private List<ClassDefinition> getStartingClassDefinitions() {
        List<ClassDefinition> startingClasses = new ArrayList<>();
        var classManagementSystem = ExamplePlugin.get().getClassManagementSystem();
        if (classManagementSystem == null) {
            return startingClasses;
        }

        for (ClassDefinition classDefinition : classManagementSystem.getAllClassDefinitions().values()) {
            if (classDefinition == null || !classDefinition.isEnabled() || !classDefinition.isStartingClass()) {
                continue;
            }
            startingClasses.add(classDefinition);
        }

        startingClasses.sort((left, right) -> {
            if (left == null && right == null) {
                return 0;
            }
            if (left == null) {
                return 1;
            }
            if (right == null) {
                return -1;
            }
            return left.getId().compareToIgnoreCase(right.getId());
        });
        return startingClasses;
    }

    @Nonnull
    private CharacterModSettings getSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new CharacterModSettings() : config.getCharacterSettings();
    }

    private void persistRoster(@Nonnull CharacterRosterData roster) {
        rostersByAccountId.put(UUID.fromString(roster.getId()), roster);
        persistence.saveRoster(roster);
    }

    @Nonnull
    private String resolvePlayerUsername(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        return playerRef == null ? "" : playerRef.getUsername();
    }

    @Nonnull
    public Message mapMessage(@Nonnull String managerMessage) {
        return switch (managerMessage) {
            case "Character system is disabled." -> Message.translation("pixelbays.rpg.character.error.systemDisabled");
            case "Logout to character select is disabled." -> Message.translation("pixelbays.rpg.character.error.logoutDisabled");
            case "Logout is already pending." -> Message.translation("pixelbays.rpg.character.error.logoutPending");
            case "Recovery limit reached." -> Message.translation("pixelbays.rpg.character.error.recoveryLimitReached");
            case "Logged out to character select." -> Message.translation("pixelbays.rpg.character.success.loggedOutToSelect");
            case "Logout timer started." -> Message.translation("pixelbays.rpg.character.success.logoutTimerStarted");
            case "Character created." -> Message.translation("pixelbays.rpg.character.success.created");
            case "Selected character." -> Message.translation("pixelbays.rpg.character.success.selected");
            case "Character deleted." -> Message.translation("pixelbays.rpg.character.success.deleted");
            case "Character marked for recovery." -> Message.translation("pixelbays.rpg.character.success.markedForRecovery");
            case "Character recovered." -> Message.translation("pixelbays.rpg.character.success.recovered");
            case "Character creation is disabled." -> Message.translation("pixelbays.rpg.character.error.creationDisabled");
            case "You have reached the maximum number of character slots." -> Message.translation("pixelbays.rpg.character.error.maxSlots");
            case "Character id is required." -> Message.translation("pixelbays.rpg.character.error.idRequired");
            case "Character not found." -> Message.translation("pixelbays.rpg.character.error.notFound");
            case "Character is pending recovery and cannot be selected." -> Message.translation("pixelbays.rpg.character.error.pendingRecovery");
            case "Character deletion is disabled." -> Message.translation("pixelbays.rpg.character.error.deletionDisabled");
            case "Soft-delete recovery is disabled." -> Message.translation("pixelbays.rpg.character.error.recoveryDisabled");
            case "Character is not deleted." -> Message.translation("pixelbays.rpg.character.error.notDeleted");
            case "Recovery window has expired." -> Message.translation("pixelbays.rpg.character.error.recoveryExpired");
            case "Character name is required." -> Message.translation("pixelbays.rpg.character.error.nameRequired");
            case "Character name is too short." -> Message.translation("pixelbays.rpg.character.error.nameTooShort");
            case "Character name is too long." -> Message.translation("pixelbays.rpg.character.error.nameTooLong");
            case "Character name is reserved." -> Message.translation("pixelbays.rpg.character.error.nameReserved");
            case "Character name is already taken." -> Message.translation("pixelbays.rpg.character.error.nameTaken");
            case "Race is required." -> Message.translation("pixelbays.rpg.character.error.raceRequired");
            case "Unknown race." -> Message.translation("pixelbays.rpg.character.error.unknownRace");
            case "Starter class is required." -> Message.translation("pixelbays.rpg.character.error.classRequired");
            case "Unknown starter class." -> Message.translation("pixelbays.rpg.character.error.unknownClass");
            case "Starter class is not allowed." -> Message.translation("pixelbays.rpg.character.error.classNotAllowed");
            case "You already have a character with that name." -> Message.translation("pixelbays.rpg.character.error.duplicateOwnedName");
            default -> Message.translation("pixelbays.rpg.common.unmappedMessage").param("text", managerMessage);
        };
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("default");
        if (config != null) {
            return config;
        }

        config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }

    private static final class LogoutThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@Nonnull Runnable runnable) {
            Thread thread = new Thread(runnable, "rpg-character-logout");
            thread.setDaemon(true);
            return thread;
        }
    }
}
