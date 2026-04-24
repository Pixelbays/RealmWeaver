package org.pixelbays.rpg.classes.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.command.ClassCommandUtil;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.component.StarterClassSelectionComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.ui.FirstJoinClassSelectionPage;
import org.pixelbays.rpg.global.config.BuildFlags;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.hud.PlayerHudService;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class FirstJoinClassSelectionManager {

    private final ClassManagementSystem classManagementSystem;

    public FirstJoinClassSelectionManager(@Nonnull ClassManagementSystem classManagementSystem) {
        this.classManagementSystem = classManagementSystem;
    }

    public void handlePlayerReady(@Nonnull PlayerReadyEvent event) {
        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null || store.getExternalData() == null) {
            return;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> maybeOpenSelectionPage(entityRef, playerRef));
    }

    public boolean shouldDeferAutomaticStartingClass(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        if (!isFallbackSelectionEnabled()) {
            return false;
        }

        if (hasCompletedSelection(entityRef, store)) {
            return false;
        }

        ClassComponent classComponent = store.getComponent(entityRef, ClassComponent.getComponentType());
        return !classManagementSystem.hasKnownLearnedClasses(classComponent);
    }

    public boolean restoreSelectedStarterClassIfNeeded(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        if (!isFallbackSelectionEnabled()) {
            return false;
        }

        String selectedClassId = getSelectedStarterClassId(entityRef, store);
        if (selectedClassId == null || selectedClassId.isBlank()) {
            return false;
        }

        String result = classManagementSystem.learnClass(entityRef, selectedClassId, store);
        if (result != null && result.startsWith("SUCCESS")) {
            return true;
        }

        RpgLogging.debugDeveloper("[FirstJoinClassSelection] Failed to restore selected starter class %s: %s",
                selectedClassId,
                result);
        return false;
    }

    @Nonnull
    public List<ClassDefinition> getSelectableStartingClasses(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        List<ClassDefinition> results = new ArrayList<>();
        if (!isFallbackSelectionEnabled()) {
            return results;
        }

        for (ClassDefinition classDefinition : classManagementSystem.getAllClassDefinitions().values()) {
            if (classDefinition == null || !classDefinition.isEnabled() || !classDefinition.isStartingClass()) {
                continue;
            }

            if (!classManagementSystem.canLearnClass(entityRef, classDefinition.getId(), store)) {
                continue;
            }

            results.add(classDefinition);
        }

        results.sort(Comparator.comparing(this::sortKey, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    @Nullable
    public String getSelectedStarterClassId(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        StarterClassSelectionComponent component = store.getComponent(entityRef,
                StarterClassSelectionComponent.getComponentType());
        if (component == null || !component.isSelectionCompleted()) {
            return null;
        }

        String selectedClassId = component.getSelectedClassId();
        return selectedClassId.isBlank() ? null : selectedClassId;
    }

    @Nonnull
    public String chooseStartingClass(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable String classId) {
        String requestedClassId = classId == null ? "" : classId.trim();
        if (requestedClassId.isBlank()) {
            return "ERROR: Unknown class: " + requestedClassId;
        }

        String result = classManagementSystem.learnClass(entityRef, requestedClassId, store);
        if (result == null || !result.startsWith("SUCCESS")) {
            return result == null ? "ERROR: Failed to learn class." : result;
        }

        String resolvedClassId = classManagementSystem.resolveClassId(requestedClassId);
        if (resolvedClassId != null && !resolvedClassId.isBlank()) {
            persistSelection(entityRef, store, resolvedClassId);
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(entityRef, store, Page.None);
        }

        PlayerHudService playerHudService = Realmweavers.get().getPlayerHudService();
        if (playerHudService != null) {
            playerHudService.ensureAndUpdate(entityRef, store);
        }

        playerRef.sendMessage(ClassCommandUtil.managerResultMessage(result));
        return result;
    }

    private void maybeOpenSelectionPage(@Nonnull Ref<EntityStore> entityRef, @Nonnull PlayerRef playerRef) {
        if (!entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null || !shouldDeferAutomaticStartingClass(entityRef, store)) {
            return;
        }

        if (getSelectableStartingClasses(entityRef, store).isEmpty()) {
            return;
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.getPageManager().openCustomPage(entityRef, store, new FirstJoinClassSelectionPage(playerRef));
    }

    private boolean hasCompletedSelection(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        StarterClassSelectionComponent component = store.getComponent(entityRef,
                StarterClassSelectionComponent.getComponentType());
        return component != null
                && component.isSelectionCompleted()
                && !component.getSelectedClassId().isBlank();
    }

    private void persistSelection(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String classId) {
        StarterClassSelectionComponent component = store.getComponent(entityRef,
                StarterClassSelectionComponent.getComponentType());
        if (component == null) {
            component = new StarterClassSelectionComponent();
        }

        component.setSelectionCompleted(true);
        component.setSelectedClassId(classId);
        store.putComponent(entityRef, StarterClassSelectionComponent.getComponentType(), component);
    }

    private boolean isFallbackSelectionEnabled() {
        RpgModConfig config = resolveConfig();
        if (config != null) {
            return config.isClassModuleEnabled() && !config.isCharacterModuleEnabled();
        }
        return BuildFlags.CLASS_MODULE && !BuildFlags.CHARACTER_MODULE;
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }

    @Nonnull
    private String sortKey(@Nonnull ClassDefinition classDefinition) {
        String displayName = classDefinition.getDisplayName();
        return displayName == null || displayName.isBlank() ? classDefinition.getId() : displayName;
    }
}