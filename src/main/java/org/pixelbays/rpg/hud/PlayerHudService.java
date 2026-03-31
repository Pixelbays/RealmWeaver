package org.pixelbays.rpg.hud;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerHudService {

    private final ConcurrentHashMap<UUID, PlayerHud> hudByPlayerId = new ConcurrentHashMap<>();
    private final DiceOverlayHudServiceModule diceOverlayModule;
    private final ResourceBarsHudServiceModule resourceBarsModule;
    private final PartyMembersHudServiceModule partyMembersModule;
    private final ProgressionHudServiceModule progressionModule;
    private final List<PlayerHudServiceModule> modules;

    public PlayerHudService(@Nonnull LevelProgressionSystem levelSystem) {
        this.diceOverlayModule = new DiceOverlayHudServiceModule();
        this.resourceBarsModule = new ResourceBarsHudServiceModule();
        this.partyMembersModule = new PartyMembersHudServiceModule();
        this.progressionModule = new ProgressionHudServiceModule(levelSystem);
        this.modules = List.of(
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

        PlayerHud hud = hudByPlayerId.compute(context.getPlayerRef().getUuid(), (id, existing) -> {
            if (existing == null) {
                return new PlayerHud(context.getPlayerRef());
            }
            if (!existing.getPlayerRef().equals(context.getPlayerRef())) {
                return new PlayerHud(context.getPlayerRef());
            }
            return existing;
        });
        hud = Objects.requireNonNull(hud, "hud");

        HudManager hudManager = context.getPlayer().getHudManager();
        if (org.pixelbays.plugin.Realmweavers.get().getCharacterManager().requiresCharacterUiLock(context.getPlayerRef())) {
            if (hudManager.getCustomHud() == hud) {
                hudManager.setCustomHud(context.getPlayerRef(), null);
            }
            return;
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
}