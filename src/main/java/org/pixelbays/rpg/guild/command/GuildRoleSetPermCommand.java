package org.pixelbays.rpg.guild.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.guild.GuildPermission;

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

public class GuildRoleSetPermCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> roleArg;
    private final OptionalArg<String> permArg;
    private final OptionalArg<String> enabledArg;
    private final GuildManager guildManager;

    public GuildRoleSetPermCommand() {
        super("setperm", "Set a role permission");
        this.roleArg = this.withOptionalArg("role", "Role id", ArgTypes.STRING);
        this.permArg = this.withOptionalArg("permission", "Permission", ArgTypes.STRING);
        this.enabledArg = this.withOptionalArg("enabled", "true|false", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!roleArg.provided(ctx) || !permArg.provided(ctx) || !enabledArg.provided(ctx)) {
            player.sendMessage(Message.raw("Usage: /guild role setperm <role> <permission> <true|false>"));
            return;
        }

        String roleId = roleArg.get(ctx);
        String permissionRaw = permArg.get(ctx);
        String enabledRaw = enabledArg.get(ctx);
        if (roleId == null || permissionRaw == null || enabledRaw == null) {
            player.sendMessage(Message.raw("Usage: /guild role setperm <role> <permission> <true|false>"));
            return;
        }

        GuildPermission permission = parsePermission(permissionRaw);
        if (permission == null) {
            player.sendMessage(Message.raw("Unknown permission."));
            return;
        }

        Boolean enabled = parseEnabled(enabledRaw);
        if (enabled == null) {
            player.sendMessage(Message.raw("Enabled must be true or false."));
            return;
        }

        GuildActionResult result = guildManager.setRolePermission(playerRef.getUuid(), roleId, permission, enabled);
        player.sendMessage(Message.raw(result.getMessage()));
    }

    private GuildPermission parsePermission(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        for (GuildPermission permission : GuildPermission.values()) {
            if (permission.name().equalsIgnoreCase(normalized)) {
                return permission;
            }
        }
        return null;
    }

    private Boolean parseEnabled(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "true", "yes", "on", "1" -> Boolean.TRUE;
            case "false", "no", "off", "0" -> Boolean.FALSE;
            default -> null;
        };
    }
}
