package org.pixelbays.rpg.leveling.command;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /leveltest all
 * /leveltest <LevelSystemId>
 */
@SuppressWarnings("null")
public class LevelTestCommand extends AbstractPlayerCommand {

    private final LevelProgressionSystem levelSystem;
    private final OptionalArg<String> targetArg;

    public LevelTestCommand() {
        super("leveltest", "Run a quick test of one or all level systems");
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.targetArg = this.withOptionalArg("target", "all or <SystemId>", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        String rawTarget = targetArg.provided(ctx) ? targetArg.get(ctx) : "all";
        if (rawTarget == null || rawTarget.isEmpty()) {
            rawTarget = "all";
        }

        String cleanedTarget = rawTarget.startsWith("-") ? rawTarget.substring(1) : rawTarget;
        boolean all = cleanedTarget.equalsIgnoreCase("all");
        String systemId = cleanedTarget;

        if (all) {
            List<String> systems = new ArrayList<>(levelSystem.getRegisteredSystems());
            if (systems.isEmpty()) {
                player.sendMessage(Message.raw("No level systems registered."));
                return;
            }

            player.sendMessage(Message.raw("=== Level System Test (ALL) ==="));
            for (String id : systems) {
                if (id == null || id.isEmpty()) {
                    continue;
                }
                runSingleTest(player, ref, id, store, world);
            }
            player.sendMessage(Message.raw("=== Level System Test Complete ==="));
            return;
        }

        if (systemId == null || systemId.isEmpty()) {
            player.sendMessage(Message.raw("Usage: /leveltest all or /leveltest <SystemId>"));
            return;
        }

        player.sendMessage(Message.raw("=== Level System Test ==="));
        runSingleTest(player, ref, systemId, store, world);
        player.sendMessage(Message.raw("=== Level System Test Complete ==="));
    }

    private void runSingleTest(@Nonnull Player player,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String systemId,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world) {

        LevelSystemConfig config = levelSystem.getConfig(systemId);
        if (config == null) {
            player.sendMessage(Message.raw("- " + systemId + ": NOT FOUND"));
            return;
        }

        if (!config.isEnabled()) {
            player.sendMessage(Message.raw("- " + systemId + ": DISABLED"));
            return;
        }

        levelSystem.initializeLevelSystem(ref, systemId);

        int startLevel = levelSystem.getLevel(ref, systemId);
        float expToNext = levelSystem.getExpToNextLevel(ref, systemId);

        if (expToNext <= 0f) {
            player.sendMessage(Message.raw("- " + systemId + ": level " + startLevel + " (MAX)"));
            return;
        }

        long expToGrant = (long) Math.ceil(expToNext);
        GiveXPEvent.dispatch(ref, expToGrant, systemId);

        int newLevel = levelSystem.getLevel(ref, systemId);
        float currentExp = levelSystem.getExperience(ref, systemId);
        float newExpToNext = levelSystem.getExpToNextLevel(ref, systemId);

        String displayName = config.getDisplayName() != null && !config.getDisplayName().isEmpty()
                ? config.getDisplayName()
                : systemId;

        player.sendMessage(Message.raw("- " + displayName + " (" + systemId + "): "
                + startLevel + " -> " + newLevel
                + " | exp " + String.format("%.0f", currentExp) + "/" + String.format("%.0f", newExpToNext)));
    }
}
