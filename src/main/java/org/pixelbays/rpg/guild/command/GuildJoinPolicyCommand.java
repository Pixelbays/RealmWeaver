package org.pixelbays.rpg.guild.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.guild.GuildActionResult;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.GuildManager;

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

public class GuildJoinPolicyCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> policyArg;
    private final GuildManager guildManager;

    public GuildJoinPolicyCommand() {
        super("joinpolicy", "Set guild join policy");
        this.policyArg = this.withOptionalArg("policy", "invite|open|application", ArgTypes.STRING);
        this.guildManager = ExamplePlugin.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (!policyArg.provided(ctx)) {
            player.sendMessage(Message.raw("Usage: /guild joinpolicy <invite|open|application>"));
            return;
        }

        String rawPolicy = policyArg.get(ctx);
        GuildJoinPolicy policy = parsePolicy(rawPolicy);
        if (policy == null) {
            player.sendMessage(Message.raw("Invalid policy. Use invite, open, or application."));
            return;
        }

        GuildActionResult result = guildManager.setJoinPolicy(playerRef.getUuid(), policy);
        player.sendMessage(Message.raw(result.getMessage()));
    }

    private GuildJoinPolicy parsePolicy(String rawPolicy) {
        if (rawPolicy == null) {
            return null;
        }
        String normalized = rawPolicy.trim().toLowerCase();
        return switch (normalized) {
            case "invite", "invite_only", "invited" -> GuildJoinPolicy.INVITE_ONLY;
            case "open" -> GuildJoinPolicy.OPEN;
            case "application", "apply", "apps" -> GuildJoinPolicy.APPLICATION;
            default -> null;
        };
    }
}
