package org.pixelbays.rpg.economy.currency.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;

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

public class CurrencyInfoCommand extends AbstractPlayerCommand {

    private final CurrencyManager currencyManager;

    public CurrencyInfoCommand() {
        super("info", "Show your currencies");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.currencyManager = Realmweavers.get().getCurrencyManager();
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

        player.sendMessage(Message.translation("pixelbays.rpg.currency.info.header"));
        CurrencyAccessContext characterContext = CurrencyAccessContext.fromInventory(player.getInventory());

        sendScopeLine(player, CurrencyScope.Character, playerRef, characterContext);
        sendScopeLine(player, CurrencyScope.Account, playerRef, CurrencyAccessContext.empty());
        sendScopeLine(player, CurrencyScope.Guild, playerRef, CurrencyAccessContext.empty());
    }

    private void sendScopeLine(@Nonnull Player player,
            @Nonnull CurrencyScope scope,
            @Nonnull PlayerRef playerRef,
            @Nonnull CurrencyAccessContext accessContext) {
        String ownerId = CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
        String summary = summarize(scope, ownerId, accessContext);
        player.sendMessage(Message.translation("pixelbays.rpg.currency.info.scope")
                .param("scope", scope.name())
                .param("entries", summary));
    }

    @SuppressWarnings("null")
    private String summarize(@Nonnull CurrencyScope scope,
            @Nullable String ownerId,
            @Nonnull CurrencyAccessContext accessContext) {
        if (ownerId == null || ownerId.isBlank()) {
            String rawText = Message.translation("pixelbays.rpg.common.none").getFormattedMessage().rawText;
            return rawText != null ? rawText : "None";
        }

        List<String> entries = new ArrayList<>();
        for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getByScope(scope)) {
            if (!definition.isVisibleInUi()) {
                continue;
            }
            long balance = currencyManager.getBalance(scope, ownerId, definition.getId(), accessContext);
            entries.add(definition.getDisplayName() + "=" + balance);
        }

        if (entries.isEmpty()) {
            String rawText = Message.translation("pixelbays.rpg.common.none").getFormattedMessage().rawText;
            return rawText != null ? rawText : "None";
        }

        return entries.stream().collect(Collectors.joining(", "));
    }
}
