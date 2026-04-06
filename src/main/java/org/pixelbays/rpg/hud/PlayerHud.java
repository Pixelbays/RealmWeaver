package org.pixelbays.rpg.hud;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerHud extends CustomUIHud {

    private static final String UI_ASSET_PATH = "Hud/PlayerHud.ui";

    private final HotbarHudModule hotbarModule;
    private final ProgressionHudModule progressionModule;
    private final ResourceBarsHudModule resourceBarsModule;
    private final PartyMembersHudModule partyMembersModule;
    private final DiceOverlayHudModule diceOverlayModule;
    private final List<PlayerHudModule> modules;

    public PlayerHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
        this.hotbarModule = new HotbarHudModule(this);
        this.progressionModule = new ProgressionHudModule(this);
        this.resourceBarsModule = new ResourceBarsHudModule(this);
        this.partyMembersModule = new PartyMembersHudModule(this);
        this.diceOverlayModule = new DiceOverlayHudModule(this);
        this.modules = List.of(
            this.hotbarModule,
                this.resourceBarsModule,
                this.partyMembersModule,
                this.diceOverlayModule,
                this.progressionModule);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append(UI_ASSET_PATH);
        for (PlayerHudModule module : this.modules) {
            module.build(cmd);
        }
    }

    void applyModuleUpdate(@Nonnull UICommandBuilder cmd) {
        update(false, cmd);
    }

    @Nonnull
    public String buildDebugDocument() {
        StringBuilder ui = new StringBuilder(12_288);
        ui.append("$C = \"../Common.ui\";\n\n");
        this.diceOverlayModule.appendDebugUi(ui);
        this.partyMembersModule.appendDebugUi(ui);
        this.resourceBarsModule.appendDebugUi(ui);
        this.hotbarModule.appendDebugUi(ui);
        this.progressionModule.appendDebugUi(ui);
        return ui.toString();
    }

    @Nonnull
    public HotbarHudModule getHotbarModule() {
        return hotbarModule;
    }

    @Nonnull
    public ProgressionHudModule getProgressionModule() {
        return progressionModule;
    }

    @Nonnull
    public ResourceBarsHudModule getResourceBarsModule() {
        return resourceBarsModule;
    }

    @Nonnull
    public PartyMembersHudModule getPartyMembersModule() {
        return partyMembersModule;
    }

    @Nonnull
    public DiceOverlayHudModule getDiceOverlayModule() {
        return diceOverlayModule;
    }
}