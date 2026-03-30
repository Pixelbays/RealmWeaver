package org.pixelbays.rpg.leveling.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /testlevel - Placeholder automated test for leveling system
 */
@SuppressWarnings({"unused", "null"})
public class TestLevelCommand extends AbstractPlayerCommand {

    private final LevelProgressionSystem levelSystem;

    public TestLevelCommand() {
        super("testlevel", "Tests the RPG leveling system - runs automated test");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.levelSystem = Realmweavers.get().getLevelProgressionSystem();
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.sendMessage(Message.translation("pixelbays.rpg.level.test.placeholder"));
    }

    @SuppressWarnings("unused")
    private void showSystemStatus(Player player, String systemId, LevelProgressionComponent.LevelSystemData data) {
        player.sendMessage(Message.translation("pixelbays.rpg.level.test.status")
            .param("systemId", systemId)
            .param("level", data.getCurrentLevel())
            .param("currentExp", String.format("%.1f", data.getCurrentExp()))
            .param("expToNext", String.format("%.1f", data.getExpToNextLevel()))
            .param("stats", data.getAvailableStatPoints())
            .param("skills", data.getAvailableSkillPoints()));
    }
}
