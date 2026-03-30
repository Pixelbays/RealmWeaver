package org.pixelbays.rpg.guild.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager;

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

public class GuildRoleAssignCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> roleArg;
    private final GuildManager guildManager;

    public GuildRoleAssignCommand() {
        super("assign", "Assign a guild role");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.roleArg = this.withRequiredArg("role", "Role id", ArgTypes.STRING);
        this.guildManager = Realmweavers.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String targetName = playerArg.get(ctx);
        String roleId = roleArg.get(ctx);
        if (targetName == null || roleId == null || targetName.isBlank() || roleId.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.usage.roleAssign"));
            return;
        }

        PlayerRef targetRef = GuildCommandUtil.findPlayerByName(targetName);
        if (targetRef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.playerNotFound"));
            return;
        }

        GuildActionResult result = guildManager.assignRole(playerRef.getUuid(), targetRef.getUuid(), roleId);
        player.sendMessage(GuildCommandUtil.managerResultMessage(result.getMessage()));
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.guild.notify.roleUpdated"));
        }
    }
}
