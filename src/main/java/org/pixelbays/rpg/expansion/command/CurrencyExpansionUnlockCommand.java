package org.pixelbays.rpg.expansion.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.command.CurrencyCommandUtil;
import org.pixelbays.rpg.expansion.ExpansionManager;
import org.pixelbays.rpg.expansion.ExpansionPurchaseResult;
import org.pixelbays.rpg.global.config.settings.GeneralModSettings;

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

public class CurrencyExpansionUnlockCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> expansionIdArg;
    private final ExpansionManager expansionManager;

    public CurrencyExpansionUnlockCommand() {
        super("unlock", "Unlock an expansion with currency");
        requirePermission(HytalePermissions.fromCommand("player"));
        this.expansionIdArg = this.withRequiredArg("expansionId", "Expansion id", ArgTypes.STRING);
        this.expansionManager = Realmweavers.get().getExpansionManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String expansionId = expansionIdArg.get(ctx);
        if (player == null || expansionId == null || expansionId.isBlank()) {
            return;
        }

        ExpansionPurchaseResult result = expansionManager.purchase(player, playerRef, expansionId);
        GeneralModSettings.ExpansionDefinition definition = result.getExpansion();

        switch (result.getStatus()) {
            case Success -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.unlock.success")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case UnknownExpansion -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.error.unknown")
                    .param("id", expansionId));
            case Disabled -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.error.disabled")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case AlreadyOwned -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.unlock.alreadyOwned")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case FreeAccess -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.unlock.freeAccess")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case FreeNotLive -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.unlock.freeNotLive")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId())
                    .param("time", definition == null ? "" : expansionManager.formatReleaseTime(definition)));
            case AccessGrantedByPermission -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.unlock.permissionGranted")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case CurrencyModuleDisabled -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.error.currencyDisabled"));
            case InvalidPurchaseScope -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.error.invalidScope")
                    .param("name", definition == null ? expansionId : definition.getDisplayNameOrId()));
            case OwnerUnavailable -> player.sendMessage(Message.translation("pixelbays.rpg.expansion.error.scopeUnavailable")
                    .param("scope", definition == null ? "unknown" : definition.getPurchaseCurrencyScope().name().toLowerCase()));
            case CurrencyFailed -> {
                if (result.getCurrencyResult() != null) {
                    player.sendMessage(CurrencyCommandUtil.managerResultMessage(result.getCurrencyResult()));
                } else {
                    player.sendMessage(Message.translation("pixelbays.rpg.common.unknownError"));
                }
            }
        }
    }
}
