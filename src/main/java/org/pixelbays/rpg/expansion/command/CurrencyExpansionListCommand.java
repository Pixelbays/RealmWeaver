package org.pixelbays.rpg.expansion.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.expansion.ExpansionManager;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;

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

public class CurrencyExpansionListCommand extends AbstractPlayerCommand {

    private final ExpansionManager expansionManager;

    public CurrencyExpansionListCommand() {
        super("list", "List configured expansions");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.expansionManager = Realmweavers.get().getExpansionManager();
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

        player.sendMessage(Message.translation("pixelbays.rpg.expansion.list.header"));
        var definitions = expansionManager.getDefinitions();
        if (definitions.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.expansion.list.none"));
            return;
        }

        for (GeneralModSettings.ExpansionDefinition definition : definitions) {
            if (definition == null || !definition.isVisible()) {
                continue;
            }

            Message status = resolveStatus(playerRef, definition);
            String price = definition.getUnlockPrice().isFree()
                    ? Message.translation("pixelbays.rpg.expansion.price.free").getFormattedMessage().rawText
                    : Message.translation("pixelbays.rpg.expansion.price.amount")
                            .param("amount", Long.toString(definition.getUnlockPrice().getAmount()))
                            .param("currency", definition.getUnlockPrice().getCurrencyId())
                            .param("scope", definition.getPurchaseCurrencyScope().name())
                            .getFormattedMessage().rawText;
            String release = expansionManager.isReleased(definition)
                    ? Message.translation("pixelbays.rpg.expansion.release.live").getFormattedMessage().rawText
                    : Message.translation("pixelbays.rpg.expansion.release.at")
                            .param("time", expansionManager.formatReleaseTime(definition))
                            .getFormattedMessage().rawText;

            player.sendMessage(Message.translation("pixelbays.rpg.expansion.list.entry")
                    .param("status", status)
                    .param("name", definition.getDisplayNameOrId())
                    .param("id", definition.getId())
                    .param("price", price == null ? "" : price)
                    .param("release", release == null ? "" : release));

            if (!definition.getDescription().isBlank()) {
                player.sendMessage(Message.translation("pixelbays.rpg.expansion.list.description")
                        .param("description", definition.getDescription()));
            }
            if (!definition.getWebsiteUrl().isBlank()) {
                player.sendMessage(Message.translation("pixelbays.rpg.expansion.list.website")
                        .param("url", definition.getWebsiteUrl()));
            }
        }
    }

    @Nonnull
    private Message resolveStatus(@Nonnull PlayerRef playerRef, @Nonnull GeneralModSettings.ExpansionDefinition definition) {
        java.util.UUID uuid = playerRef.getUuid();
        if (definition.isEnabled() && uuid != null && expansionManager.hasAccessPermission(uuid, definition)) {
            return Message.translation("pixelbays.rpg.expansion.status.granted");
        }
        if (!definition.isEnabled()) {
            return Message.translation("pixelbays.rpg.expansion.status.disabled");
        }
        if (uuid != null && expansionManager.isUnlocked(uuid, definition.getId())) {
            return expansionManager.isReleased(definition) || expansionManager.hasReleaseBypassPermission(uuid, definition)
                    ? Message.translation("pixelbays.rpg.expansion.status.owned")
                    : Message.translation("pixelbays.rpg.expansion.status.ownedPending");
        }
        if (!expansionManager.isReleased(definition)
                && (uuid == null || !expansionManager.hasReleaseBypassPermission(uuid, definition))) {
            return Message.translation("pixelbays.rpg.expansion.status.unreleased");
        }
        if (definition.getUnlockPrice().isFree()) {
            return Message.translation("pixelbays.rpg.expansion.status.free");
        }
        return Message.translation("pixelbays.rpg.expansion.status.available");
    }
}
