package org.pixelbays.rpg.character;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.ability.binding.AbilityBindingService;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.system.HotbarAbilityIconManager;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceData;
import org.pixelbays.rpg.character.appearance.CharacterAppearanceService;
import org.pixelbays.rpg.character.config.CharacterRosterData;
import org.pixelbays.rpg.character.config.CharacterProfileData;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings.BarberShopSettings;
import org.pixelbays.rpg.character.config.settings.CharacterModSettings.LoginSpawnMode;
import org.pixelbays.rpg.character.token.CharacterTokenActionResult;
import org.pixelbays.rpg.character.token.CharacterTokenDefinition;
import org.pixelbays.rpg.character.token.CharacterTokenRegistry;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.prereq.PrerequisiteEvaluator;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.global.system.StatSystem;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.ApplyLookType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputTargetType;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
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
import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

@SuppressWarnings({ "null", "removal" })
public final class CharacterManager {

    private static final String BASE_CHARACTER_LEVEL = "Base_Character_Level";
    private static final float CHARACTER_PREVIEW_DEFAULT_YAW = 0.0F;
    private static final float CHARACTER_PREVIEW_ROTATION_STEP = (float) Math.toRadians(22.5D);

    private final CharacterPersistence persistence = new CharacterPersistence();
    private final CharacterAppearanceService appearanceService = new CharacterAppearanceService();
    private final AbilityBindingService abilityBindingService = new AbilityBindingService();
    private final Map<UUID, CharacterRosterData> rostersByAccountId = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeCharacterIds = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingUiTransitionSuppressions = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> pendingLogoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService logoutScheduler = Executors.newSingleThreadScheduledExecutor(new LogoutThreadFactory());
    private volatile boolean persistenceLoaded;

    public CharacterManager() {
    }

    public void clear() {
        rostersByAccountId.clear();
        activeCharacterIds.clear();
        pendingUiTransitionSuppressions.clear();
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
        String activeCharacterId = activeCharacterIds.get(playerRef.getUuid());
        if (activeCharacterId != null && !activeCharacterId.isBlank()) {
            return;
        }

        World world = store.getExternalData() == null ? null : store.getExternalData().getWorld();
        if (event.getReadyId() > 0 && !isLobbyWorld(world)) {
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
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            if (!isLobbyWorld(world)) {
                moveToLobby(ref, store, playerRef);
            }
            openCharacterSelectPage(ref, store, playerRef);
        });
    }

    public void openCharacterCreator(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            suppressNextCharacterUiReopen(playerRef);
            player.getPageManager().openCustomPage(ref, store,
                    new org.pixelbays.rpg.character.ui.CharacterAppearancePage(playerRef,
                            org.pixelbays.rpg.character.ui.CharacterAppearancePage.Mode.CREATE));
        });
    }

    public void openBarberShop(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            suppressNextCharacterUiReopen(playerRef);
            player.getPageManager().openCustomPage(ref, store,
                    new org.pixelbays.rpg.character.ui.CharacterAppearancePage(playerRef,
                            org.pixelbays.rpg.character.ui.CharacterAppearancePage.Mode.BARBER));
        });
    }

    public boolean requiresCharacterUiLock(@Nullable PlayerRef playerRef) {
        if (!isEnabled() || playerRef == null || playerRef.getUuid() == null) {
            return false;
        }

        String activeCharacterId = activeCharacterIds.get(playerRef.getUuid());
        if (activeCharacterId != null && !activeCharacterId.isBlank()) {
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null || store.getExternalData() == null) {
            return false;
        }

        return isLobbyWorld(store.getExternalData().getWorld());
    }

    public void ensureCharacterSelectionUiOpen(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        if (!requiresCharacterUiLock(playerRef)) {
            return;
        }
        if (consumePendingCharacterUiSuppression(playerRef)) {
            return;
        }

        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid() || !requiresCharacterUiLock(playerRef)) {
                return;
            }
            openCharacterSelectPage(ref, store, playerRef);
        });
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
        return createCharacter(ref, store, playerRef, rawName, rawRaceId, rawStarterClassId, null, false);
    }

    @Nonnull
    public CharacterActionResult createCharacter(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String rawName,
            @Nullable String rawRaceId,
            @Nullable String rawStarterClassId,
            @Nullable CharacterAppearanceData requestedAppearance) {
        return createCharacter(ref, store, playerRef, rawName, rawRaceId, rawStarterClassId, requestedAppearance, false);
    }

    @Nonnull
    public CharacterActionResult createCharacter(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String rawName,
            @Nullable String rawRaceId,
            @Nullable String rawStarterClassId,
            @Nullable CharacterAppearanceData requestedAppearance,
            boolean hardcore) {
        if (!isEnabled()) {
            return CharacterActionResult.failure("Character system is disabled.");
        }
        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterModSettings settings = getSettings();
        if (!settings.isAllowCharacterCreation()) {
            return CharacterActionResult.failure("Character creation is disabled.");
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

        CharacterActionResult slotAvailability = ensureCharacterSlotAvailableForCreation(roster, ref, store, playerRef, settings);
        if (!slotAvailability.isSuccess()) {
            return slotAvailability;
        }

        CharacterAppearanceData sanitizedAppearance = sanitizeAppearance(raceId, requestedAppearance);
        CharacterProfileData profile = buildNewProfile(name, raceId, starterClassId, playerRef, sanitizedAppearance,
                hardcore && isHardcoreCharacterCreationEnabled());
        roster.upsertProfile(profile);
        if (roster.getSelectedCharacterId().isBlank()) {
            roster.setSelectedCharacterId(profile.getCharacterId());
        }
        persistRoster(roster);
        if ("Character slot unlocked.".equals(slotAvailability.getMessage())) {
            return CharacterActionResult.success("Character created and unlocked a new slot.", profile);
        }
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
        if (Realmweavers.get().getAchievementSystem() != null) {
            Realmweavers.get().getAchievementSystem().synchronizeEntityAchievements(ref, store);
        }
        roster.setSelectedCharacterId(target.getCharacterId());
        target.setLastPlayedEpochMs(System.currentTimeMillis());
        persistRoster(roster);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }

        resetCharacterAppearancePreviewCamera(playerRef);
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
            normalizeSelectedCharacterId(roster);
            persistRoster(roster);
            return CharacterActionResult.success("Character marked for recovery.", target);
        }

        roster.getProfiles().remove(target);
        normalizeSelectedCharacterId(roster);
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
        normalizeSelectedCharacterId(roster);
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

    public void sanitizeAbilityBindingsForCurrentConfig() {
        if (!isEnabled()) {
            return;
        }

        loadFromAssets();

        for (Map.Entry<UUID, CharacterRosterData> rosterEntry : rostersByAccountId.entrySet()) {
            UUID accountId = rosterEntry.getKey();
            CharacterRosterData roster = rosterEntry.getValue();
            if (roster == null) {
                continue;
            }

            boolean changed = false;
            String activeCharacterId = activeCharacterIds.get(accountId);
            for (CharacterProfileData profile : roster.getProfiles()) {
                if (profile == null) {
                    continue;
                }
                if (activeCharacterId != null && activeCharacterId.equalsIgnoreCase(profile.getCharacterId())) {
                    continue;
                }

                if (abilityBindingService.sanitizeInvalidBindings(profile.getAbilityBindings()).changed()) {
                    changed = true;
                }
            }

            if (changed) {
                persistRoster(roster);
            }
        }

        for (Map.Entry<UUID, String> activeEntry : new ArrayList<>(activeCharacterIds.entrySet())) {
            UUID accountId = activeEntry.getKey();
            PlayerRef playerRef = Universe.get().getPlayer(accountId);
            if (playerRef == null) {
                continue;
            }

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                continue;
            }

            Store<EntityStore> store = ref.getStore();
            if (store.getExternalData() == null || store.getExternalData().getWorld() == null) {
                continue;
            }

            World world = store.getExternalData().getWorld();
            world.execute(() -> sanitizeActiveAbilityBindings(accountId, ref, store));
        }
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
    public List<CharacterProfileData> getSelectableProfilesFor(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        List<CharacterProfileData> profiles = new ArrayList<>();
        for (CharacterProfileData profile : roster.getProfiles()) {
            if (profile != null && !profile.isSoftDeleted()) {
                profiles.add(profile);
            }
        }
        profiles.sort((left, right) -> Long.compare(right.getLastPlayedEpochMs(), left.getLastPlayedEpochMs()));
        return profiles;
    }

    @Nonnull
    public List<CharacterProfileData> getDeletedProfilesFor(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        List<CharacterProfileData> profiles = new ArrayList<>();
        for (CharacterProfileData profile : roster.getProfiles()) {
            if (profile != null && profile.isSoftDeleted()) {
                profiles.add(profile);
            }
        }
        profiles.sort((left, right) -> {
            int deletedAtCompare = Long.compare(right.getDeletedAtEpochMs(), left.getDeletedAtEpochMs());
            if (deletedAtCompare != 0) {
                return deletedAtCompare;
            }
            return Long.compare(right.getLastPlayedEpochMs(), left.getLastPlayedEpochMs());
        });
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
    public Map<CharacterTokenDefinition, Long> getVisibleAccountTokenBalances(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        LinkedHashMap<CharacterTokenDefinition, Long> balances = new LinkedHashMap<>();
        for (CharacterTokenDefinition definition : CharacterTokenRegistry.getVisibleInCharacterSelect()) {
            if (definition == null) {
                continue;
            }
            balances.put(definition, roster.getAccountTokens().getOrDefault(definition.getId(), definition.getStartingBalance()));
        }
        return balances;
    }

    public long getAccountTokenBalance(@Nonnull UUID accountId, @Nullable String username, @Nonnull String tokenId) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        CharacterTokenDefinition definition = CharacterTokenRegistry.get(tokenId);
        if (definition == null || !definition.isEnabled()) {
            return 0L;
        }
        return roster.getAccountTokens().getOrDefault(definition.getId(), definition.getStartingBalance());
    }

    public int getUnlockedExtraCharacterSlots(@Nonnull UUID accountId, @Nullable String username) {
        return getOrCreateRoster(accountId, username).getUnlockedExtraCharacterSlots();
    }

    public int getGrantedCharacterSlotCount(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        return calculatePrerequisiteGrantedCharacterSlots(ref, store, playerRef, getSettings());
    }

    public int getCurrentCharacterSlotCapacity(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        return calculateCharacterSlotCapacity(roster, ref, store, playerRef, getSettings());
    }

    public int getVisibleCharacterSlotCount(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        int capacity = calculateCharacterSlotCapacity(roster, ref, store, playerRef, getSettings());
        return Math.max(1, Math.max(roster.getActiveProfileCount(), capacity));
    }

    @Nonnull
    public CharacterTokenActionResult addAccountTokenBalance(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull String tokenId,
            long amount) {
        return mutateAccountTokenBalance(accountId, username, tokenId, amount, TokenMutationType.Add);
    }

    @Nonnull
    public CharacterTokenActionResult removeAccountTokenBalance(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull String tokenId,
            long amount) {
        return mutateAccountTokenBalance(accountId, username, tokenId, amount, TokenMutationType.Remove);
    }

    @Nonnull
    public CharacterTokenActionResult setAccountTokenBalance(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull String tokenId,
            long amount) {
        return mutateAccountTokenBalance(accountId, username, tokenId, amount, TokenMutationType.Set);
    }

    @Nonnull
    public CharacterTokenActionResult spendAccountToken(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull String tokenId,
            long amount) {
        return mutateAccountTokenBalance(accountId, username, tokenId, amount, TokenMutationType.Spend);
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
    public String resolveChatDisplayName(@Nonnull UUID accountId, @Nullable String username, boolean preferCharacterName) {
        String accountName = username == null || username.isBlank() ? resolveAccountUsername(accountId) : username;
        if (!preferCharacterName) {
            return accountName == null || accountName.isBlank() ? accountId.toString() : accountName;
        }

        CharacterRosterData roster = rostersByAccountId.get(accountId);
        if (roster != null) {
            CharacterProfileData profile = null;
            String activeCharacterId = activeCharacterIds.get(accountId);
            if (activeCharacterId != null && !activeCharacterId.isBlank()) {
                profile = roster.getProfile(activeCharacterId);
            }
            if (profile == null) {
                String selectedCharacterId = roster.getSelectedCharacterId();
                if (selectedCharacterId != null && !selectedCharacterId.isBlank()) {
                    profile = roster.getProfile(selectedCharacterId);
                }
            }
            if (profile != null && !profile.isSoftDeleted()) {
                String characterName = profile.getCharacterName();
                if (characterName != null && !characterName.isBlank()) {
                    return characterName;
                }
            }
        }

        return accountName == null || accountName.isBlank() ? accountId.toString() : accountName;
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

    public void applyCharacterAppearancePreviewCamera(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            playerRef.getPacketHandler().writeNoCache(
                    new SetServerCamera(ClientCameraView.Custom, true, createCharacterAppearancePreviewCameraSettings()));
        });
    }

    public void applyCharacterPreview(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String raceId,
            @Nullable CharacterAppearanceData appearance) {
        applyAppearanceToPlayer(ref, store, raceId, appearance);
        RpgLogging.debugDeveloper(
                "[CharacterManager] applyCharacterPreview player=%s race=%s appearance=%s",
                playerRef.getUsername(),
                raceId,
                appearance == null ? "null" : appearance.asMap());
        applyCharacterAppearancePreviewCamera(ref, store, playerRef);
    }

    public void resetCharacterAppearancePreviewCamera(@Nonnull PlayerRef playerRef) {
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    public void resetCharacterPreviewRotation(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        setCharacterPreviewRotation(ref, store, CHARACTER_PREVIEW_DEFAULT_YAW);
    }

    public void rotateCharacterPreview(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            float yawDelta) {
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return;
        }

        float nextYaw = transformComponent.getRotation().getYaw() + yawDelta;
        setCharacterPreviewRotation(ref, store, normalizePreviewYaw(nextYaw));
    }

    public void rotateCharacterPreviewLeft(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        rotateCharacterPreview(ref, store, -CHARACTER_PREVIEW_ROTATION_STEP);
    }

    public void rotateCharacterPreviewRight(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        rotateCharacterPreview(ref, store, CHARACTER_PREVIEW_ROTATION_STEP);
    }

    @Nonnull
    public String resolveServerName() {
        RpgModConfig config = resolveConfig();
        if (config == null || config.getServerName() == null || config.getServerName().isBlank()) {
            return "Server Name";
        }
        return config.getServerName();
    }

    @Nonnull
    private ServerCameraSettings createCharacterAppearancePreviewCameraSettings() {
        ServerCameraSettings cameraSettings = new ServerCameraSettings();
        cameraSettings.positionLerpSpeed = 0.2F;
        cameraSettings.rotationLerpSpeed = 0.2F;
        cameraSettings.distance = 3.75F;
        cameraSettings.allowPitchControls = false;
        cameraSettings.displayCursor = true;
        cameraSettings.sendMouseMotion = false;
        cameraSettings.isFirstPerson = false;
        cameraSettings.applyLookType = ApplyLookType.Rotation;
        cameraSettings.mouseInputTargetType = MouseInputTargetType.None;
        cameraSettings.movementForceRotationType = MovementForceRotationType.Custom;
        cameraSettings.eyeOffset = false;
        cameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        cameraSettings.rotationType = RotationType.Custom;
        cameraSettings.rotation = new Direction((float) Math.PI, -0.18F, 0.0F);
        cameraSettings.movementForceRotation = new Direction((float) Math.PI, 0.0F, 0.0F);
        cameraSettings.mouseInputType = MouseInputType.LookAtPlane;
        cameraSettings.planeNormal = new Vector3f(0.0F, 1.0F, 0.0F);
        return cameraSettings;
    }

    private void setCharacterPreviewRotation(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            float yaw) {
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return;
        }

        com.hypixel.hytale.math.vector.Vector3f rotation = transformComponent.getRotation().clone();
        rotation.setPitch(0.0F);
        rotation.setYaw(yaw);
        rotation.setRoll(0.0F);
        transformComponent.teleportRotation(rotation);
    }

    private float normalizePreviewYaw(float yaw) {
        float fullTurn = (float) (Math.PI * 2.0D);
        float normalized = yaw % fullTurn;
        if (normalized < 0.0F) {
            normalized += fullTurn;
        }
        return normalized;
    }

    public int resolveDisplayedLevel(@Nullable CharacterProfileData profile) {
        if (profile == null) {
            return 1;
        }

        LevelProgressionComponent levelProgression = profile.getLevelProgression();

        String primaryClassId = resolvePrimaryKnownClassId(profile);
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

    @Nonnull
    public String resolveDisplayedClassName(@Nullable CharacterProfileData profile) {
        String classId = resolvePrimaryKnownClassId(profile);
        if (classId.isBlank()) {
            return "";
        }

        ClassDefinition classDefinition = resolveClassDefinition(classId);
        if (classDefinition != null && classDefinition.getDisplayName() != null
                && !classDefinition.getDisplayName().isBlank()) {
            return classDefinition.getDisplayName();
        }

        return classId;
    }

    public boolean isEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isCharacterModuleEnabled();
    }

    @Nonnull
        private CharacterProfileData buildNewProfile(@Nonnull String name,
            @Nonnull String raceId,
            @Nonnull String starterClassId,
            @Nullable PlayerRef playerRef,
                @Nonnull CharacterAppearanceData appearance,
                boolean hardcore) {
        CharacterProfileData profile = new CharacterProfileData();
        profile.setCharacterId(UUID.randomUUID().toString());
        profile.setCharacterName(name);
        profile.setCreatedAtEpochMs(System.currentTimeMillis());
        profile.setLastPlayedEpochMs(System.currentTimeMillis());
            profile.setHardcore(hardcore);

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
        profile.setAchievementProgress(new AchievementComponent());
        profile.setStatSnapshot(new EntityStatMap());
        profile.setInventorySnapshot(new Inventory());
        profile.setAppearance(appearance);
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

        return Realmweavers.get().getExpansionManager().getAccessibleLevelCap(playerRef, baseMaxLevel);
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
        AchievementComponent achievementProgress = store.getComponent(ref, AchievementComponent.getComponentType());
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        PlayerSkinComponent playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        profile.setLevelProgression(levelComponent == null ? new LevelProgressionComponent() : (LevelProgressionComponent) levelComponent.clone());
        profile.setClassProgression(classComponent == null ? new ClassComponent() : (ClassComponent) classComponent.clone());
        profile.setRaceProgression(raceComponent == null ? new RaceComponent() : (RaceComponent) raceComponent.clone());
        profile.setClassAbilities(classAbilities == null ? new ClassAbilityComponent() : classAbilities.clone());
        AbilityBindingComponent capturedBindings = abilityBindings == null
            ? new AbilityBindingComponent()
            : (AbilityBindingComponent) abilityBindings.clone();
        abilityBindingService.sanitizeInvalidBindings(capturedBindings);
        profile.setAbilityBindings(capturedBindings);
        profile.setAchievementProgress(achievementProgress == null ? new AchievementComponent() : (AchievementComponent) achievementProgress.clone());
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
        profile.setPrimaryClassId(resolvePrimaryKnownClassId(profile.getClassProgression()));
        profile.setAppearance(appearanceService.fromPlayerSkin(
                playerSkinComponent == null ? null : playerSkinComponent.getPlayerSkin(),
                profile.getRaceId()));
    }

    private void applyProfileToPlayer(@Nonnull CharacterProfileData profile,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        store.putComponent(ref, LevelProgressionComponent.getComponentType(), (LevelProgressionComponent) profile.getLevelProgression().clone());
        store.putComponent(ref, ClassComponent.getComponentType(), (ClassComponent) profile.getClassProgression().clone());
        store.putComponent(ref, RaceComponent.getComponentType(), (RaceComponent) profile.getRaceProgression().clone());
        store.putComponent(ref, ClassAbilityComponent.getComponentType(), profile.getClassAbilities().clone());
        AbilityBindingComponent appliedBindings = (AbilityBindingComponent) profile.getAbilityBindings().clone();
        abilityBindingService.sanitizeInvalidBindings(appliedBindings);
        store.putComponent(ref, AbilityBindingComponent.getComponentType(), appliedBindings);
        store.putComponent(ref, AchievementComponent.getComponentType(), (AchievementComponent) profile.getAchievementProgress().clone());
        store.putComponent(ref, EntityStatMap.getComponentType(), profile.getStatSnapshot().clone());

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            applyInventorySnapshot(ref, store, profile.getInventorySnapshot(), profile.getActiveHotbarSlot(),
                    profile.getActiveUtilitySlot(), profile.getActiveToolsSlot());
        }

        applyAppearanceToPlayer(ref, store, profile.getRaceId(), profile.getAppearance());

        StatSystem statSystem = Realmweavers.get().getStatSystem();
    Realmweavers.get().getClassManagementSystem().pruneUnknownClasses(ref, store);
        statSystem.recalculateClassStatBonuses(ref, store);
        statSystem.recalculateTalentStatBonuses(ref, store);
        statSystem.recalculateRaceStatBonuses(ref, store);
    }

    @Nonnull
    public CharacterAppearanceService getAppearanceService() {
        return appearanceService;
    }

    @Nonnull
    public CharacterAppearanceData sanitizeAppearance(@Nullable String raceId,
            @Nullable CharacterAppearanceData appearance) {
        return appearanceService.sanitizeAppearance(resolveRaceDefinition(raceId), appearance);
    }

    @Nonnull
    public CharacterAppearanceData createDefaultAppearance(@Nullable String raceId) {
        return appearanceService.createDefaultAppearance(raceId);
    }

    @Nullable
    public CharacterProfileData getActiveProfile(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        String activeCharacterId = activeCharacterIds.getOrDefault(accountId, roster.getSelectedCharacterId());
        if (activeCharacterId == null || activeCharacterId.isBlank()) {
            return null;
        }
        return roster.getProfile(activeCharacterId);
    }

    @Nullable
    public CharacterProfileData getLoadedActiveProfile(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        String activeCharacterId = activeCharacterIds.get(accountId);
        if (activeCharacterId == null || activeCharacterId.isBlank()) {
            return null;
        }
        return roster.getProfile(activeCharacterId);
    }

    public boolean isHardcoreCharacterCreationEnabled() {
        RpgModConfig config = resolveConfig();
        return config != null && config.isHardcoreEnabled();
    }

    @Nonnull
    public CharacterActionResult applyBarberAppearance(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterAppearanceData requestedAppearance) {
        CharacterProfileData activeProfile = getActiveProfile(playerRef.getUuid(), playerRef.getUsername());
        if (activeProfile == null) {
            return CharacterActionResult.failure("No active character is available for barber changes.");
        }

        CharacterAppearanceData sanitized = sanitizeAppearance(activeProfile.getRaceId(), requestedAppearance);
        CharacterAppearanceData current = activeProfile.getAppearance();
        int changes = sanitized.countDifferences(current);
        if (changes <= 0) {
            applyAppearanceToPlayer(ref, store, activeProfile.getRaceId(), sanitized);
            return CharacterActionResult.success("Appearance updated.", activeProfile);
        }

        BarberShopSettings barberSettings = getSettings().getBarberShopSettings();
        CurrencyActionResult spendResult = chargeBarberCost(barberSettings, activeProfile, playerRef, store, changes);
        if (!spendResult.isSuccess()) {
            return CharacterActionResult.failure(spendResult.getMessage());
        }

        CharacterRosterData roster = getOrCreateRoster(playerRef.getUuid(), playerRef.getUsername());
        CharacterProfileData storedProfile = roster.getProfile(activeProfile.getCharacterId());
        if (storedProfile == null) {
            return CharacterActionResult.failure("Character profile not found.");
        }
        storedProfile.setAppearance(sanitized);
        applyAppearanceToPlayer(ref, store, storedProfile.getRaceId(), sanitized);
        persistRoster(roster);
        return CharacterActionResult.success("Appearance updated.", storedProfile);
    }

    public void applyAppearanceToPlayer(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String raceId,
            @Nullable CharacterAppearanceData requestedAppearance) {
        CharacterAppearanceData sanitized = sanitizeAppearance(raceId, requestedAppearance);
        PlayerRef targetPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
        String targetLabel = targetPlayerRef == null
            ? String.valueOf(ref)
            : targetPlayerRef.getUsername() + " (" + targetPlayerRef.getUuid() + ")";
        RpgLogging.debugDeveloper(
            "[CharacterManager] applyAppearanceToPlayer target=%s race=%s requested=%s sanitized=%s",
            targetLabel,
                raceId,
                requestedAppearance == null ? "null" : requestedAppearance.asMap(),
                sanitized.asMap());
        PlayerSkinComponent playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        PlayerSkin fallbackSkin = playerSkinComponent == null ? null : new PlayerSkin(playerSkinComponent.getPlayerSkin());
        PlayerSkin sanitizedSkin = resolveValidPlayerSkin(sanitized.toPlayerSkin(), fallbackSkin);
        if (playerSkinComponent == null) {
            store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(sanitizedSkin));
        } else {
            PlayerSkin playerSkin = playerSkinComponent.getPlayerSkin();
            playerSkin.bodyCharacteristic = sanitizedSkin.bodyCharacteristic;
            playerSkin.underwear = sanitizedSkin.underwear;
            playerSkin.face = sanitizedSkin.face;
            playerSkin.eyes = sanitizedSkin.eyes;
            playerSkin.ears = sanitizedSkin.ears;
            playerSkin.mouth = sanitizedSkin.mouth;
            playerSkin.facialHair = sanitizedSkin.facialHair;
            playerSkin.haircut = sanitizedSkin.haircut;
            playerSkin.eyebrows = sanitizedSkin.eyebrows;
            playerSkin.pants = sanitizedSkin.pants;
            playerSkin.overpants = sanitizedSkin.overpants;
            playerSkin.undertop = sanitizedSkin.undertop;
            playerSkin.overtop = sanitizedSkin.overtop;
            playerSkin.shoes = sanitizedSkin.shoes;
            playerSkin.headAccessory = sanitizedSkin.headAccessory;
            playerSkin.faceAccessory = sanitizedSkin.faceAccessory;
            playerSkin.earAccessory = sanitizedSkin.earAccessory;
            playerSkin.skinFeature = sanitizedSkin.skinFeature;
            playerSkin.gloves = sanitizedSkin.gloves;
            playerSkin.cape = sanitizedSkin.cape;
        }

        playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (playerSkinComponent != null) {
            playerSkinComponent.setNetworkOutdated();
            var playerModel = com.hypixel.hytale.server.core.cosmetics.CosmeticsModule.get().createModel(playerSkinComponent.getPlayerSkin());
            if (playerModel != null) {
                store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(playerModel));
                RpgLogging.debugDeveloper(
                        "[CharacterManager] applyAppearanceToPlayer modelUpdated target=%s body=%s face=%s hair=%s headAccessory=%s",
                        targetLabel,
                        sanitizedSkin.bodyCharacteristic,
                        sanitizedSkin.face,
                        sanitizedSkin.haircut,
                        sanitizedSkin.headAccessory);
            } else {
                RpgLogging.debugDeveloper(
                        "[CharacterManager] applyAppearanceToPlayer createModel returned null target=%s skin=%s",
                        targetLabel,
                        CharacterAppearanceData.fromPlayerSkin(playerSkinComponent.getPlayerSkin()).asMap());
            }
        } else {
            RpgLogging.debugDeveloper("[CharacterManager] applyAppearanceToPlayer missing PlayerSkinComponent target=%s",
                    targetLabel);
        }
    }

    @Nonnull
    private PlayerSkin resolveValidPlayerSkin(@Nullable PlayerSkin requestedSkin, @Nullable PlayerSkin fallbackSkin) {
        if (isValidPlayerSkin(requestedSkin)) {
            return new PlayerSkin(requestedSkin);
        }
        PlayerSkin mergedSkin = mergePlayerSkin(requestedSkin, fallbackSkin);
        if (isValidPlayerSkin(mergedSkin)) {
            RpgLogging.debugDeveloper(
                    "[CharacterManager] resolveValidPlayerSkin merged requested skin over fallback successfully");
            return new PlayerSkin(mergedSkin);
        }
        if (isValidPlayerSkin(fallbackSkin)) {
            RpgLogging.debugDeveloper(
                    "[CharacterManager] resolveValidPlayerSkin falling back to existing skin because requested/merged skin was invalid");
            return new PlayerSkin(fallbackSkin);
        }

        PlayerSkin generatedSkin = com.hypixel.hytale.server.core.cosmetics.CosmeticsModule.get().generateRandomSkin(new Random());
        if (isValidPlayerSkin(generatedSkin)) {
            return new PlayerSkin(generatedSkin);
        }

        return requestedSkin == null ? new PlayerSkin(generatedSkin) : new PlayerSkin(requestedSkin);
    }

    @Nonnull
    private PlayerSkin mergePlayerSkin(@Nullable PlayerSkin requestedSkin, @Nullable PlayerSkin fallbackSkin) {
        PlayerSkin mergedSkin = fallbackSkin == null ? new PlayerSkin() : new PlayerSkin(fallbackSkin);
        if (requestedSkin == null) {
            return mergedSkin;
        }

        if (requestedSkin.bodyCharacteristic != null) {
            mergedSkin.bodyCharacteristic = requestedSkin.bodyCharacteristic;
        }
        if (requestedSkin.underwear != null) {
            mergedSkin.underwear = requestedSkin.underwear;
        }
        if (requestedSkin.face != null) {
            mergedSkin.face = requestedSkin.face;
        }
        if (requestedSkin.eyes != null) {
            mergedSkin.eyes = requestedSkin.eyes;
        }
        if (requestedSkin.ears != null) {
            mergedSkin.ears = requestedSkin.ears;
        }
        if (requestedSkin.mouth != null) {
            mergedSkin.mouth = requestedSkin.mouth;
        }
        mergedSkin.facialHair = requestedSkin.facialHair;
        mergedSkin.haircut = requestedSkin.haircut;
        mergedSkin.eyebrows = requestedSkin.eyebrows;
        mergedSkin.pants = requestedSkin.pants;
        mergedSkin.overpants = requestedSkin.overpants;
        if (requestedSkin.undertop != null) {
            mergedSkin.undertop = requestedSkin.undertop;
        }
        mergedSkin.overtop = requestedSkin.overtop;
        mergedSkin.shoes = requestedSkin.shoes;
        mergedSkin.headAccessory = requestedSkin.headAccessory;
        mergedSkin.faceAccessory = requestedSkin.faceAccessory;
        mergedSkin.earAccessory = requestedSkin.earAccessory;
        mergedSkin.skinFeature = requestedSkin.skinFeature;
        mergedSkin.gloves = requestedSkin.gloves;
        mergedSkin.cape = requestedSkin.cape;
        return mergedSkin;
    }

    private boolean isValidPlayerSkin(@Nullable PlayerSkin playerSkin) {
        if (playerSkin == null) {
            return false;
        }
        try {
            com.hypixel.hytale.server.core.cosmetics.CosmeticsModule.get().validateSkin(playerSkin);
            return true;
        } catch (com.hypixel.hytale.server.core.cosmetics.CosmeticsModule.InvalidSkinException exception) {
            return false;
        }
    }

    @Nonnull
    private CurrencyActionResult chargeBarberCost(@Nonnull BarberShopSettings settings,
            @Nonnull CharacterProfileData profile,
            @Nonnull PlayerRef playerRef,
            @Nonnull Store<EntityStore> store,
            int changes) {
        if (!settings.isEnabled()) {
            return CurrencyActionResult.success("Barber cost disabled.", null, 0L);
        }

        CurrencyAmountDefinition cost = switch (settings.getPricingMode()) {
            case PerChangedSlot -> new CurrencyAmountDefinition(
                    settings.getPerChangedSlotCost().getCurrencyId(),
                    settings.getPerChangedSlotCost().getAmount() * Math.max(1, changes));
            case Flat -> settings.getFlatCost();
        };

        if (cost.isFree()) {
            return CurrencyActionResult.success("Appearance updated.", cost.getCurrencyId(), 0L);
        }

        CurrencyManager currencyManager = Realmweavers.get().getCurrencyManager();
        String ownerId = resolveCurrencyOwnerId(settings.getCurrencyScope(), profile, playerRef);
        CurrencyAccessContext accessContext = CurrencyAccessContext.fromInventory(resolveInventory(store, playerRef));
        return currencyManager.spend(settings.getCurrencyScope(), ownerId, cost, accessContext);
    }

    @Nonnull
    private Inventory resolveInventory(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null && player.getInventory() != null) {
                return player.getInventory();
            }
        }
        return new Inventory();
    }

    @Nonnull
    private String resolveCurrencyOwnerId(@Nonnull CurrencyScope scope,
            @Nonnull CharacterProfileData profile,
            @Nonnull PlayerRef playerRef) {
        return switch (scope) {
            case Character -> profile.getCharacterId();
            case Account -> playerRef.getUuid().toString();
            case Guild, Global, Custom -> profile.getCharacterId();
        };
    }

    @Nonnull
    private Inventory copyInventory(@Nullable Inventory inventory) {
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

        private void applyInventorySnapshot(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable Inventory inventory,
            byte activeHotbarSlot,
            byte activeUtilitySlot,
            byte activeToolsSlot) {
        Inventory snapshot = inventory == null ? new Inventory() : inventory;
        store.putComponent(ref, InventoryComponent.Storage.getComponentType(),
            new InventoryComponent.Storage(copyContainer(snapshot.getStorage())));
        store.putComponent(ref, InventoryComponent.Armor.getComponentType(),
            new InventoryComponent.Armor(copyContainer(snapshot.getArmor())));
        store.putComponent(ref, InventoryComponent.Hotbar.getComponentType(),
            new InventoryComponent.Hotbar(copyContainer(snapshot.getHotbar()), activeHotbarSlot));
        store.putComponent(ref, InventoryComponent.Utility.getComponentType(),
            new InventoryComponent.Utility(copyContainer(snapshot.getUtility()), activeUtilitySlot));
        store.putComponent(ref, InventoryComponent.Tool.getComponentType(),
            new InventoryComponent.Tool(copyContainer(snapshot.getTools()), activeToolsSlot));
        store.putComponent(ref, InventoryComponent.Backpack.getComponentType(),
            new InventoryComponent.Backpack(copyContainer(snapshot.getBackpack())));
        }

        @Nonnull
        private ItemContainer copyContainer(@Nullable ItemContainer source) {
        short capacity = source == null ? 0 : source.getCapacity();
        ItemContainer target = capacity <= 0 ? EmptyItemContainer.INSTANCE : new SimpleItemContainer(capacity);
        copyContainerContents(source, target);
        return target;
        }

        private void copyContainerContents(@Nullable ItemContainer source, @Nonnull ItemContainer target) {
        target.clear();
        if (source == null) {
            return;
        }

        short maxSlots = (short) Math.min(source.getCapacity(), target.getCapacity());
        for (short slot = 0; slot < maxSlots; slot++) {
            ItemStack itemStack = source.getItemStack(slot);
            if (!ItemStack.isEmpty(itemStack)) {
                target.setItemStackForSlot(slot, copyItemStack(itemStack));
            }
        }
        }

    @Nonnull
    private ItemStack copyItemStack(@Nonnull ItemStack itemStack) {
        return new ItemStack(
                itemStack.getItemId(),
                itemStack.getQuantity(),
                itemStack.getDurability(),
                itemStack.getMaxDurability(),
                itemStack.getMetadata() == null ? null : itemStack.getMetadata().clone());
    }

    private void cleanupRosterState(@Nonnull CharacterRosterData roster) {
        CharacterModSettings settings = getSettings();
        boolean changed = false;

        pruneRecoveryHistory(roster, settings);
        changed |= normalizeAccountTokens(roster);
        roster.setUnlockedExtraCharacterSlots(roster.getUnlockedExtraCharacterSlots());

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
                normalizeSelectedCharacterId(roster);
                changed = true;
            }
        }

        if (changed) {
            persistence.saveRoster(roster);
        }
    }

    private void sanitizeActiveAbilityBindings(@Nonnull UUID accountId,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        if (!ref.isValid()) {
            return;
        }

        AbilityBindingComponent bindingComponent = store.getComponent(ref, AbilityBindingComponent.getComponentType());
        if (bindingComponent == null) {
            return;
        }

        AbilityBindingService.BindingSanitizationResult result = abilityBindingService.sanitizeInvalidBindings(bindingComponent);
        if (!result.changed()) {
            return;
        }

        HotbarAbilityIconManager hotbarIconManager = Realmweavers.get().getHotbarIconManager();
        if (hotbarIconManager != null) {
            for (int slot : result.clearedHotbarSlots()) {
                hotbarIconManager.updateHotbarSlot(ref, store, slot, null);
            }
            if (!bindingComponent.getHotbarBindings().isEmpty()) {
                hotbarIconManager.syncHotbarIcons(ref, store);
            }
        }

        saveActiveCharacter(accountId, ref, store);
    }

    private void pruneRecoveryHistory(@Nonnull CharacterRosterData roster, @Nonnull CharacterModSettings settings) {
        int recoveryWindowHours = Math.max(0, settings.getRecoveryWindowHours());
        if (recoveryWindowHours <= 0) {
            return;
        }

        long cutoff = System.currentTimeMillis() - recoveryWindowHours * 3_600_000L;
        roster.getRecoveryHistoryEpochMs().removeIf(timestamp -> timestamp == null || timestamp < cutoff);
    }

    private boolean normalizeAccountTokens(@Nonnull CharacterRosterData roster) {
        boolean changed = false;
        Map<String, Long> balances = roster.getAccountTokens();

        for (CharacterTokenDefinition definition : CharacterTokenRegistry.getEnabled()) {
            if (definition == null) {
                continue;
            }

            String tokenId = definition.getId();
            long current = balances.getOrDefault(tokenId, definition.getStartingBalance());
            long clamped = clampTokenBalance(definition, current);
            if (!balances.containsKey(tokenId) || current != clamped) {
                balances.put(tokenId, clamped);
                changed = true;
            }
        }

        return changed;
    }

    @Nonnull
    private CharacterActionResult ensureCharacterSlotAvailableForCreation(@Nonnull CharacterRosterData roster,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterModSettings settings) {
        int activeProfileCount = roster.getActiveProfileCount();
        int maxCharacterSlots = settings.getMaxCharacterSlots();
        if (activeProfileCount >= maxCharacterSlots) {
            return CharacterActionResult.failure("You have reached the maximum number of character slots.");
        }

        int currentCapacity = calculateCharacterSlotCapacity(roster, ref, store, playerRef, settings);
        if (activeProfileCount < currentCapacity) {
            return CharacterActionResult.success("Character slot available.");
        }
        if (currentCapacity >= maxCharacterSlots) {
            return CharacterActionResult.failure("You have reached the maximum number of character slots.");
        }

        CharacterTokenActionResult spendResult = spendAccountToken(playerRef.getUuid(),
                playerRef.getUsername(),
                settings.getExtraCharacterSlotTokenId(),
                1L);
        if (!spendResult.isSuccess()) {
            return CharacterActionResult.failure("Character slot unlock token is required.");
        }

        roster.setUnlockedExtraCharacterSlots(roster.getUnlockedExtraCharacterSlots() + 1);
        persistRoster(roster);
        return CharacterActionResult.success("Character slot unlocked.");
    }

    private int calculateCharacterSlotCapacity(@Nonnull CharacterRosterData roster,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterModSettings settings) {
        int baseSlots = settings.getDefaultCharacterSlots();
        int unlockedSlots = roster.getUnlockedExtraCharacterSlots();
        int prerequisiteGrantedSlots = calculatePrerequisiteGrantedCharacterSlots(ref, store, playerRef, settings);
        return Math.min(settings.getMaxCharacterSlots(), baseSlots + unlockedSlots + prerequisiteGrantedSlots);
    }

    private int calculatePrerequisiteGrantedCharacterSlots(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterModSettings settings) {
        int grantedSlots = 0;
        for (CharacterModSettings.ConditionalSlotGrant slotGrant : settings.getConditionalSlotGrants()) {
            if (slotGrant == null || slotGrant.getSlotCount() <= 0) {
                continue;
            }
            if (PrerequisiteEvaluator.meetsRequirements(slotGrant.getPrerequisites(), null, store, ref, playerRef)) {
                grantedSlots += slotGrant.getSlotCount();
            }
        }
        return Math.max(0, grantedSlots);
    }

    @Nonnull
    private CharacterTokenActionResult mutateAccountTokenBalance(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull String tokenId,
            long amount,
            @Nonnull TokenMutationType mutationType) {
        if (amount < 0L) {
            return CharacterTokenActionResult.failure("Amount must be zero or greater.", tokenId);
        }

        CharacterTokenDefinition definition = CharacterTokenRegistry.get(tokenId);
        if (definition == null) {
            return CharacterTokenActionResult.failure("Unknown token type.", tokenId);
        }
        if (!definition.isEnabled()) {
            return CharacterTokenActionResult.failure("Token type is disabled.", tokenId);
        }

        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        Map<String, Long> balances = roster.getAccountTokens();
        long current = balances.getOrDefault(definition.getId(), definition.getStartingBalance());
        long updated = switch (mutationType) {
            case Add -> current + amount;
            case Remove, Spend -> current - amount;
            case Set -> amount;
        };

        if ((mutationType == TokenMutationType.Remove || mutationType == TokenMutationType.Spend) && updated < 0L) {
            return CharacterTokenActionResult.failure("Insufficient tokens.", tokenId);
        }

        long clamped = clampTokenBalance(definition, updated);
        if ((mutationType == TokenMutationType.Remove || mutationType == TokenMutationType.Spend) && clamped > updated) {
            return CharacterTokenActionResult.failure("Insufficient tokens.", tokenId);
        }

        balances.put(definition.getId(), clamped);
        persistRoster(roster);

        String message = switch (mutationType) {
            case Add -> "Added token balance.";
            case Remove -> "Removed token balance.";
            case Set -> "Set token balance.";
            case Spend -> "Spent token balance.";
        };
        return CharacterTokenActionResult.success(message, definition.getId(), clamped);
    }

    private long clampTokenBalance(@Nonnull CharacterTokenDefinition definition, long value) {
        return Math.max(0L, Math.min(definition.getMaxBalance(), value));
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
            List<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
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
        if (!settings.isUseLobbyInstance()) {
            return;
        }
        String lobbyInstanceTemplateId = settings.getLobbyInstanceTemplateId();
        if (lobbyInstanceTemplateId == null || lobbyInstanceTemplateId.isBlank()) {
            return;
        }
        if (!InstancesPlugin.doesInstanceAssetExist(lobbyInstanceTemplateId)) {
            return;
        }

        World currentWorld = store.getExternalData().getWorld();

        Transform returnPoint = resolveCurrentTransform(store, ref, playerRef.getUuid(), currentWorld);
        CompletableFuture<World> lobbyWorldFuture = InstancesPlugin.get().spawnInstance(lobbyInstanceTemplateId, currentWorld, returnPoint);
        InstancesPlugin.teleportPlayerToLoadingInstance(ref, store, lobbyWorldFuture, null);
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    private boolean isLobbyWorld(@Nullable World world) {
        if (world == null) {
            return false;
        }

        CharacterModSettings settings = getSettings();
        if (!settings.isUseLobbyInstance()) {
            return false;
        }

        String lobbyTemplateId = settings.getLobbyInstanceTemplateId();
        if (lobbyTemplateId == null || lobbyTemplateId.isBlank()) {
            return false;
        }

        String worldName = world.getName();
        return worldName != null && (worldName.equals(lobbyTemplateId)
                || worldName.startsWith("instance-" + lobbyTemplateId + "-"));
    }

    private void openCharacterSelectPage(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        CharacterProfileData previewProfile = getActiveProfile(playerRef.getUuid(), playerRef.getUsername());
        if (previewProfile == null) {
            List<CharacterProfileData> profiles = getSelectableProfilesFor(playerRef.getUuid(), playerRef.getUsername());
            String lastSelectedCharacterId = getLastSelectedCharacterId(playerRef.getUuid(), playerRef.getUsername());
            if (!lastSelectedCharacterId.isBlank()) {
                previewProfile = profiles.stream()
                        .filter(profile -> profile != null
                                && lastSelectedCharacterId.equalsIgnoreCase(profile.getCharacterId()))
                        .findFirst()
                        .orElse(null);
            }
            if (previewProfile == null && !profiles.isEmpty()) {
                previewProfile = profiles.getFirst();
            }
        }
        if (previewProfile != null) {
            applyCharacterPreview(ref, store, playerRef, previewProfile.getRaceId(), previewProfile.getAppearance());
        } else {
            applyCharacterAppearancePreviewCamera(ref, store, playerRef);
        }

        suppressNextCharacterUiReopen(playerRef);
        player.getPageManager().openCustomPage(ref, store, new org.pixelbays.rpg.character.ui.CharacterSelectPage(playerRef));
    }

    public void reopenCharacterSelectPage(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            suppressNextCharacterUiReopen(playerRef);
            openCharacterSelectPage(ref, store, playerRef);
        });
    }

    public void openDeletedCharacterRecoveryPage(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            suppressNextCharacterUiReopen(playerRef);
            player.getPageManager().openCustomPage(ref, store,
                    new org.pixelbays.rpg.character.ui.CharacterRecoveryPage(playerRef));
        });
    }

    private void suppressNextCharacterUiReopen(@Nullable PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        pendingUiTransitionSuppressions.put(playerRef.getUuid(), Boolean.TRUE);
    }

    private boolean consumePendingCharacterUiSuppression(@Nullable PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return false;
        }
        return pendingUiTransitionSuppressions.remove(playerRef.getUuid()) != null;
    }

    private void moveToGameplayWorld(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull CharacterProfileData profile) {
        LoginSpawnMode loginSpawnMode = getSettings().getLoginSpawnMode();

        if (loginSpawnMode == LoginSpawnMode.LastSavedLocation) {
            World savedWorld = resolveSavedWorld(profile);
            Transform savedTransform = resolveSavedTransform(profile);
            if (savedWorld != null && savedTransform != null && !isLobbyWorld(savedWorld)) {
                store.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(savedWorld, savedTransform));
                return;
            }
        }

        if (loginSpawnMode == LoginSpawnMode.LocalSafeSpot) {
            World localWorld = resolveSavedWorld(profile);
            if (localWorld != null && !isLobbyWorld(localWorld)) {
                teleportToWorldSpawn(ref, store, localWorld, playerRef.getUuid());
                return;
            }
        }

        World world = Universe.get().getDefaultWorld();
        if (world != null) {
            teleportToWorldSpawn(ref, store, world, playerRef.getUuid());
        }
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
    private Transform resolveCurrentTransform(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UUID playerId,
            @Nonnull World currentWorld) {
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent != null) {
            return new Transform(transformComponent.getPosition().clone(), transformComponent.getRotation().clone());
        }
        return resolveSpawnTransform(currentWorld, playerId, store, ref);
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

    private void normalizeSelectedCharacterId(@Nonnull CharacterRosterData roster) {
        String selectedCharacterId = roster.getSelectedCharacterId();
        if (!selectedCharacterId.isBlank()) {
            CharacterProfileData selectedProfile = roster.getProfile(selectedCharacterId);
            if (selectedProfile != null && !selectedProfile.isSoftDeleted()) {
                return;
            }
        }

        for (CharacterProfileData profile : roster.getProfiles()) {
            if (profile != null && !profile.isSoftDeleted() && !profile.getCharacterId().isBlank()) {
                roster.setSelectedCharacterId(profile.getCharacterId());
                return;
            }
        }

        roster.setSelectedCharacterId("");
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
        RaceManagementSystem raceManagementSystem = Realmweavers.get().getRaceManagementSystem();
        RaceDefinition definition = raceManagementSystem.getRaceDefinition(raceId);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    @Nullable
    private ClassDefinition resolveClassDefinition(@Nullable String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        ClassDefinition definition = Realmweavers.get().getClassManagementSystem().getClassDefinition(classId);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    @Nonnull
    private String resolvePrimaryKnownClassId(@Nullable CharacterProfileData profile) {
        if (profile == null) {
            return "";
        }

        String primaryClassId = profile.getPrimaryClassId();
        if (resolveClassDefinition(primaryClassId) != null) {
            return primaryClassId;
        }

        return resolvePrimaryKnownClassId(profile.getClassProgression());
    }

    @Nonnull
    private String resolvePrimaryKnownClassId(@Nullable ClassComponent classComponent) {
        if (classComponent == null) {
            return "";
        }

        String primaryClassId = Realmweavers.get().getClassManagementSystem().getPrimaryKnownClassId(classComponent);
        return primaryClassId == null ? "" : primaryClassId;
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
        var classManagementSystem = Realmweavers.get().getClassManagementSystem();

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
    public CharacterModSettings getSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new CharacterModSettings() : config.getCharacterSettings();
    }

    @Nonnull
    public AchievementComponent getOrCreateAccountAchievementProgress(@Nonnull UUID accountId, @Nullable String username) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        return roster.getAccountAchievementProgress();
    }

    public void saveAccountAchievementProgress(@Nonnull UUID accountId,
            @Nullable String username,
            @Nonnull AchievementComponent achievementComponent) {
        CharacterRosterData roster = getOrCreateRoster(accountId, username);
        roster.setAccountAchievementProgress(achievementComponent);
        persistRoster(roster);
    }

    private void persistRoster(@Nonnull CharacterRosterData roster) {
        rostersByAccountId.put(UUID.fromString(roster.getId()), roster);
        persistence.saveRoster(roster);
    }

    private enum TokenMutationType {
        Add,
        Remove,
        Set,
        Spend
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
                case "Character created and unlocked a new slot." -> Message.translation("pixelbays.rpg.character.success.createdUnlockedSlot");
            case "Selected character." -> Message.translation("pixelbays.rpg.character.success.selected");
            case "Character deleted." -> Message.translation("pixelbays.rpg.character.success.deleted");
            case "Character marked for recovery." -> Message.translation("pixelbays.rpg.character.success.markedForRecovery");
            case "Character recovered." -> Message.translation("pixelbays.rpg.character.success.recovered");
            case "Character creation is disabled." -> Message.translation("pixelbays.rpg.character.error.creationDisabled");
            case "You have reached the maximum number of character slots." -> Message.translation("pixelbays.rpg.character.error.maxSlots");
                case "Character slot unlock token is required." -> Message.translation("pixelbays.rpg.character.error.slotUnlockTokenRequired")
                    .param("token", resolveExtraCharacterSlotTokenDisplayName());
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

    @Nonnull
    private String resolveExtraCharacterSlotTokenDisplayName() {
        CharacterTokenDefinition definition = CharacterTokenRegistry.get(getSettings().getExtraCharacterSlotTokenId());
        if (definition != null && definition.isEnabled()) {
            return definition.getDisplayName();
        }

        String configuredTokenId = getSettings().getExtraCharacterSlotTokenId();
        return configuredTokenId.isBlank() ? "Character Slot Token" : configuredTokenId;
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
