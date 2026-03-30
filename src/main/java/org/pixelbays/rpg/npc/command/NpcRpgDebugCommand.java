package org.pixelbays.rpg.npc.command;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.npc.component.NpcRpgDebugComponent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.RoleDebugFlags;

public class NpcRpgDebugCommand extends AbstractCommandCollection {

    public NpcRpgDebugCommand() {
        super("npcdebugrpg", "Toggle RPG debug overlay for NPC roles");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.addSubCommand(new EnableCommand(true));
        this.addSubCommand(new EnableCommand(false));
    }

    private static class EnableCommand extends AbstractPlayerCommand {
        private final boolean enabled;
        private final RequiredArg<String> rolesArg;

        protected EnableCommand(boolean enabled) {
            super(enabled ? "on" : "off", enabled ? "Enable NPC RPG debug overlay" : "Disable NPC RPG debug overlay");
            this.enabled = enabled;
            this.rolesArg = this.withRequiredArg("roles", "Comma-separated role ids", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world) {

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }

            Set<String> roleFilters = parseRoles(this.rolesArg.get(ctx));
            if (roleFilters.isEmpty()) {
                player.sendMessage(Message.translation("pixelbays.rpg.npc.debug.noRoles"));
                return;
            }

            final int[] updated = new int[]{0};
            forEachNpc(store, npc -> {
                String roleName = npc.getRoleName();
                if (roleName == null || roleName.isEmpty() || !roleFilters.contains(roleName)) {
                    return;
                }

                Role role = npc.getRole();
                if (role == null) {
                    return;
                }

                if (enabled) {
                    if (store.getComponent(npc.getReference(), NpcRpgDebugComponent.getComponentType()) == null) {
                        store.addComponent(npc.getReference(), NpcRpgDebugComponent.getComponentType());
                    }
                    EnumSet<RoleDebugFlags> flags = EnumSet.copyOf(role.getDebugSupport().getDebugFlags());
                    flags.add(RoleDebugFlags.DisplayCustom);
                    role.getDebugSupport().setDebugFlags(flags);
                } else {
                    store.tryRemoveComponent(npc.getReference(), NpcRpgDebugComponent.getComponentType());
                    EnumSet<RoleDebugFlags> flags = EnumSet.copyOf(role.getDebugSupport().getDebugFlags());
                    flags.remove(RoleDebugFlags.DisplayCustom);
                    role.getDebugSupport().setDebugFlags(flags);
                }

                updated[0]++;
            });

                player.sendMessage(Message.translation("pixelbays.rpg.npc.debug.updated")
                    .param("state", enabled ? "Enabled" : "Disabled")
                    .param("count", updated[0]));
        }

        private void forEachNpc(@Nonnull Store<EntityStore> store, @Nonnull java.util.function.Consumer<NPCEntity> consumer) {
            store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                    if (npc != null) {
                        consumer.accept(npc);
                    }
                }
            });
        }

        @Nonnull
        private Set<String> parseRoles(@Nonnull String roles) {
            Set<String> result = new HashSet<>();
            for (String token : roles.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
    }
}
