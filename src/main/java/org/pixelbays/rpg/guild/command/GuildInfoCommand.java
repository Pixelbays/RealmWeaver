package org.pixelbays.rpg.guild.command;

import java.util.stream.Collectors;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;
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

public class GuildInfoCommand extends AbstractPlayerCommand {

    private final GuildManager guildManager;

    public GuildInfoCommand() {
        super("info", "Show guild info");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.guildManager = Realmweavers.get().getGuildManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        if (guild == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.error.notInGuild"));
            return;
        }

        String leaderName = GuildCommandUtil.resolveDisplayName(guild.getLeaderId());
        String roles = guild.getRoles().values().stream()
            .map(role -> role.getName() + " (" + role.getId() + ")")
                .collect(Collectors.joining(", "));

        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.header")
            .param("name", safeText(guild.getName()))
            .param("tag", safeText(guild.getTag())));
        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.leader").param("leader", safeText(leaderName)));
        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.members").param("count", guild.size()));
        String noneValue = Message.translation("pixelbays.rpg.common.none").getFormattedMessage().rawText;
        if (noneValue == null || noneValue.isBlank()) {
            noneValue = "None";
        }
        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.joinPolicy")
            .param("policy", Objects.requireNonNull(GuildCommandUtil.joinPolicyMessage(guild.getJoinPolicy()))));
        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.description")
            .param("description", safeText(guild.getDescription().isBlank() ? noneValue : guild.getDescription())));
        player.sendMessage(Message.translation("pixelbays.rpg.guild.info.motd")
            .param("motd", safeText(guild.getMotd().isBlank() ? noneValue : guild.getMotd())));
        if (roles.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.info.roles")
                    .param("roles", Message.translation("pixelbays.rpg.common.none")));
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.guild.info.roles")
                    .param("roles", roles));
        }
    }

    @Nonnull
    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
