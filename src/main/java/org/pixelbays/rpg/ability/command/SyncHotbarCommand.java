package org.pixelbays.rpg.ability.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Command to sync hotbar ability icons.
 * Usage: /synchotbar
 * 
 * Refreshes all ability icons in the hotbar based on current bindings.
 * Useful for fixing desynced icons or after logging in.
 */
public class SyncHotbarCommand extends AbstractPlayerCommand {

    public SyncHotbarCommand() {
        super("synchotbar", "Sync hotbar ability icons");
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

        // Check if player has any bindings
        AbilityBindingComponent bindingComp = store.getComponent(ref,
                ExamplePlugin.get().getAbilityBindingComponentType());
        if (bindingComp == null || bindingComp.getHotbarBindings().isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.ability.sync.none"));
            return;
        }

        // Sync all hotbar icons
        ExamplePlugin.get().getHotbarIconManager().syncHotbarIcons(ref, store);

        player.sendMessage(Message.translation("server.rpg.ability.sync.success")
            .param("count", bindingComp.getHotbarBindings().size()));
    }
}
