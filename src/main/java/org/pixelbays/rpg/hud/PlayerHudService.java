package org.pixelbays.rpg.hud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerHudService {

    private static final Path PLAYER_HUD_SOURCE = Path.of("src", "main", "resources", "common", "UI", "Custom", "Hud", "PlayerHud.ui");
    private static final Path PLAYER_HUD_DEBUG_OUTPUT = Path.of("src", "main", "resources", "common", "debug", "ui", "Player_Hud_UI.ui");

    private final ConcurrentHashMap<UUID, PlayerHud> hudByPlayerId = new ConcurrentHashMap<>();
    private final HotbarHudServiceModule hotbarModule;
    private final DiceOverlayHudServiceModule diceOverlayModule;
    private final ResourceBarsHudServiceModule resourceBarsModule;
    private final PartyMembersHudServiceModule partyMembersModule;
    private final ProgressionHudServiceModule progressionModule;
    private final List<PlayerHudServiceModule> modules;

    public PlayerHudService(@Nonnull LevelProgressionSystem levelSystem) {
        this.hotbarModule = new HotbarHudServiceModule();
        this.diceOverlayModule = new DiceOverlayHudServiceModule();
        this.resourceBarsModule = new ResourceBarsHudServiceModule();
        this.partyMembersModule = new PartyMembersHudServiceModule();
        this.progressionModule = new ProgressionHudServiceModule(levelSystem);
        this.modules = List.of(
            this.hotbarModule,
                this.resourceBarsModule,
                this.partyMembersModule,
                this.diceOverlayModule,
                this.progressionModule);
    }

    public void remove(@Nonnull UUID playerId) {
        hudByPlayerId.remove(playerId);
        for (PlayerHudServiceModule module : this.modules) {
            module.remove(playerId);
        }
    }

    public void showDiceRoll(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull DiceOverlayHudServiceModule.DiceRollRequest request) {
        this.diceOverlayModule.showDiceRoll(ref, store, request);
        ensureAndUpdate(ref, store);
    }

    public void ensureAndUpdate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerHudContext context = PlayerHudContext.create(ref, store);
        if (context == null) {
            return;
        }

        PlayerHud hud = getOrCreateHud(context);

        HudManager hudManager = context.getPlayer().getHudManager();
        if (org.pixelbays.plugin.Realmweavers.get().getCharacterManager().requiresCharacterUiLock(context.getPlayerRef())) {
            if (hudManager.getCustomHud() == hud) {
                hudManager.setCustomHud(context.getPlayerRef(), null);
                hudManager.showHudComponents(context.getPlayerRef(), HudComponent.Hotbar, HudComponent.InputBindings);
            }
            return;
        }

        if (hudManager.getVisibleHudComponents().contains(HudComponent.Hotbar)
                || hudManager.getVisibleHudComponents().contains(HudComponent.InputBindings)) {
            hudManager.hideHudComponents(context.getPlayerRef(), HudComponent.Hotbar, HudComponent.InputBindings);
        }

        if (hudManager.getCustomHud() != hud) {
            primeHud(hud, context);
            hudManager.setCustomHud(context.getPlayerRef(), hud);
            return;
        }

        for (PlayerHudServiceModule module : this.modules) {
            module.update(hud, context);
        }
    }

    private void primeHud(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        for (PlayerHudServiceModule module : this.modules) {
            module.prime(hud, context);
        }
    }

    @Nonnull
    public Path exportDebugUi(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) throws IOException {
        PlayerHudContext context = PlayerHudContext.create(ref, store);
        if (context == null) {
            throw new IOException("No player HUD context available for export.");
        }

        PlayerHud hud = getOrCreateHud(context);
        primeHud(hud, context);

        Path outputPath = resolveDebugOutputPath();
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, hud.buildDebugDocument(), StandardCharsets.UTF_8);
        return outputPath;
    }

    @Nonnull
    private PlayerHud getOrCreateHud(@Nonnull PlayerHudContext context) {
        PlayerHud hud = hudByPlayerId.compute(context.getPlayerRef().getUuid(), (id, existing) -> {
            if (existing == null) {
                return new PlayerHud(context.getPlayerRef());
            }
            if (!existing.getPlayerRef().equals(context.getPlayerRef())) {
                return new PlayerHud(context.getPlayerRef());
            }
            return existing;
        });
        return Objects.requireNonNull(hud, "hud");
    }

    @Nonnull
    private static Path resolveDebugOutputPath() throws IOException {
        Path root = resolveProjectRoot();
        return root.resolve(PLAYER_HUD_DEBUG_OUTPUT);
    }

    @Nonnull
    private static Path resolveProjectRoot() throws IOException {
        Path root = findProjectRoot(Path.of("").toAbsolutePath());
        if (root != null) {
            return root;
        }

        try {
            Path codeSourcePath = Path.of(PlayerHudService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path start = Files.isDirectory(codeSourcePath) ? codeSourcePath : codeSourcePath.getParent();
            root = findProjectRoot(start);
            if (root != null) {
                return root;
            }
        } catch (URISyntaxException ex) {
            throw new IOException("Failed to resolve plugin code source path.", ex);
        }

        throw new IOException("Could not locate the project root that contains " + PLAYER_HUD_SOURCE + '.');
    }

    private static Path findProjectRoot(Path start) {
        if (start == null) {
            return null;
        }

        for (Path current = start; current != null; current = current.getParent()) {
            if (Files.exists(current.resolve("build.gradle")) && Files.exists(current.resolve(PLAYER_HUD_SOURCE))) {
                return current;
            }
        }

        return null;
    }
}