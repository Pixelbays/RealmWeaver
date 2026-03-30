package org.pixelbays.rpg.leveling.command;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /leveltest all
 * /leveltest <LevelSystemId>
 */
@SuppressWarnings("null")
public class LevelTestCommand extends AbstractPlayerCommand {

    private final LevelProgressionSystem levelSystem;
    private final RequiredArg<String> targetArg;

    public LevelTestCommand() {
        super("leveltest", "Run a quick test of one or all level systems");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.levelSystem = Realmweavers.get().getLevelProgressionSystem();
        this.targetArg = null;
        this.addUsageVariant(new LevelTestCommand("Run a quick test of one or all level systems"));
    }

    private LevelTestCommand(String description) {
        super(description);
        this.levelSystem = Realmweavers.get().getLevelProgressionSystem();
        this.targetArg = this.withRequiredArg("target", "all or <SystemId>", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        String rawTarget = targetArg != null ? targetArg.get(ctx) : "all";
        if (rawTarget == null || rawTarget.isEmpty()) {
            rawTarget = "all";
        }

        boolean all = rawTarget.equalsIgnoreCase("all");
        String systemId = rawTarget;

        if (all) {
            List<String> systems = new ArrayList<>(levelSystem.getRegisteredSystems());
            if (systems.isEmpty()) {
                player.sendMessage(Message.translation("pixelbays.rpg.level.test.noSystems"));
                return;
            }

            player.sendMessage(Message.translation("pixelbays.rpg.level.test.headerAll"));
            for (String id : systems) {
                if (id == null || id.isEmpty()) {
                    continue;
                }
                runSingleTest(player, ref, id);
            }
            player.sendMessage(Message.translation("pixelbays.rpg.level.test.complete"));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.level.test.headerSingle"));
        runSingleTest(player, ref, systemId);
        player.sendMessage(Message.translation("pixelbays.rpg.level.test.complete"));
    }

    private void runSingleTest(@Nonnull Player player,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String systemId) {

        LevelSystemConfig config = levelSystem.getConfig(systemId);
        if (config == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.level.test.systemNotFound").param("systemId", systemId));
            return;
        }

        if (!config.isEnabled()) {
            player.sendMessage(Message.translation("pixelbays.rpg.level.test.systemDisabled").param("systemId", systemId));
            return;
        }

        levelSystem.initializeLevelSystem(ref, systemId);

        int startLevel = levelSystem.getLevel(ref, systemId);
        float expToNext = levelSystem.getExpToNextLevel(ref, systemId);

        if (expToNext <= 0f) {
            player.sendMessage(Message.translation("pixelbays.rpg.level.test.systemMax")
                    .param("systemId", systemId)
                    .param("level", startLevel));
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

        player.sendMessage(Message.translation("pixelbays.rpg.level.test.systemResult")
            .param("displayName", displayName)
            .param("systemId", systemId)
            .param("startLevel", startLevel)
            .param("newLevel", newLevel)
            .param("currentExp", String.format("%.0f", currentExp))
            .param("newExpToNext", String.format("%.0f", newExpToNext)));
    }
}
