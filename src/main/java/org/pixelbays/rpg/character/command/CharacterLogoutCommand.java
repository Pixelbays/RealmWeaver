package org.pixelbays.rpg.character.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.character.CharacterActionResult;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CharacterLogoutCommand extends AbstractPlayerCommand {

    public CharacterLogoutCommand() {
        super("logout", "Return to the character select screen");
        requirePermission(HytalePermissions.fromCommand("player"));
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        CharacterActionResult result = ExamplePlugin.get().getCharacterManager().logoutToCharacterSelect(ref, store, playerRef);
        playerRef.sendMessage(ExamplePlugin.get().getCharacterManager().mapMessage(result.getMessage()));
    }
}
