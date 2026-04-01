package org.pixelbays.rpg.chat.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.chat.ChatFilterManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChatFilterRemoveCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> wordArg;

    public ChatFilterRemoveCommand() {
        super("remove", "Remove a custom filtered word");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.wordArg = this.withRequiredArg("word", "Word or phrase to remove", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        String rawWord = wordArg.get(ctx);
        ChatFilterManager manager = Realmweavers.get().getChatFilterManager();
        String normalizedWord = manager.normalizeForDisplay(rawWord);
        Message message = switch (manager.removeCustomWord(rawWord)) {
            case Removed -> Message.translation("pixelbays.rpg.chat.filter.success.removed").param("word", normalizedWord);
            case NotPresent -> Message.translation("pixelbays.rpg.chat.filter.info.missing").param("word", normalizedWord);
            case Invalid -> Message.translation("pixelbays.rpg.chat.filter.error.invalidWord");
            case Added, AlreadyPresent -> Message.translation("pixelbays.rpg.common.unknownError");
        };
        player.sendMessage(message);
    }
}