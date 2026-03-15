package org.pixelbays.rpg.economy.banks.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.banks.BankAccount;
import org.pixelbays.rpg.economy.banks.BankActionResult;
import org.pixelbays.rpg.economy.banks.BankManager;
import org.pixelbays.rpg.economy.banks.BankTypeRegistry;
import org.pixelbays.rpg.economy.banks.command.BankCommandUtil;
import org.pixelbays.rpg.economy.banks.config.BankScope;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public final class BankUiOpener {

    private BankUiOpener() {
    }

    public static void openSelectionUi(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef) {
        player.getPageManager().openCustomPage(ref, store, new BankPage(playerRef));
    }

    public static void openSelectionUi(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull List<String> bankTypeIds,
            @Nullable String professionId,
            @Nullable String customOwnerId,
            @Nullable String title) {
        List<String> normalized = normalizeBankTypeIds(bankTypeIds);
        if (normalized.isEmpty()) {
            openSelectionUi(ref, store, player, playerRef);
            return;
        }

        if (normalized.size() == 1) {
            openBankType(ref, store, player, playerRef, normalized.get(0), professionId, customOwnerId, false);
            return;
        }

        player.getPageManager().openCustomPage(ref, store,
                new BankSelectionPage(playerRef, normalized, professionId, customOwnerId, title));
    }

    public static boolean openBankType(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nullable String bankTypeId,
            @Nullable String professionId,
            @Nullable String customOwnerId,
            boolean openSelectionWhenMissing) {
        if (bankTypeId == null || bankTypeId.isBlank()) {
            if (openSelectionWhenMissing) {
                openSelectionUi(ref, store, player, playerRef);
                return true;
            }

            player.sendMessage(Message.translation("pixelbays.rpg.bank.error.unknownBankType").param("type", ""));
            return false;
        }

        BankTypeDefinition definition = BankTypeRegistry.get(bankTypeId.trim());
        if (definition == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.bank.error.unknownBankType").param("type", bankTypeId.trim()));
            return false;
        }

        String ownerId = resolveOwnerId(definition.getScope(), playerRef, professionId, customOwnerId);
        if (ownerId == null || ownerId.isBlank()) {
            Message error = resolveOwnerError(definition.getScope());
            if (error != null) {
                player.sendMessage(error);
            }
            return false;
        }

        BankManager bankManager = ExamplePlugin.get().getBankManager();
        BankActionResult result = bankManager.getOrCreateBank(definition, ownerId, playerRef.getUuid(), player.getInventory());
        if (!result.isSuccess() || result.getBankAccount() == null) {
            player.sendMessage(BankCommandUtil.managerResultMessage(result.getMessage()));
            return false;
        }

        BankAccount bankAccount = result.getBankAccount();
        String initialTabId = bankManager.resolveInitialTabId(bankAccount);
        if (initialTabId == null || initialTabId.isBlank()) {
            player.sendMessage(Message.translation("pixelbays.rpg.common.unknownError"));
            return false;
        }

        player.getPageManager().openCustomPage(ref, store, new BankStoragePage(playerRef, bankAccount.getId(), initialTabId));
        return true;
    }

    @Nullable
    private static String resolveOwnerId(@Nonnull BankScope scope,
            @Nonnull PlayerRef playerRef,
            @Nullable String professionId,
            @Nullable String customOwnerId) {
        String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
        String resolvedCharacterOwnerId = characterOwnerId.isBlank() ? playerRef.getUuid().toString() : characterOwnerId;
        return switch (scope) {
            case Character -> resolvedCharacterOwnerId;
            case Player, Account -> playerRef.getUuid().toString();
            case Guild -> resolveGuildOwnerId(playerRef);
            case Warband -> customOwnerId != null && !customOwnerId.isBlank()
                    ? customOwnerId.trim()
                    : playerRef.getUuid().toString();
            case Profession -> professionId == null || professionId.isBlank()
                    ? null
                    : BankManager.createQualifiedOwnerId(resolvedCharacterOwnerId, professionId.trim());
            case Global -> customOwnerId != null && !customOwnerId.isBlank() ? customOwnerId.trim() : "global";
            case Custom -> customOwnerId == null || customOwnerId.isBlank() ? null : customOwnerId.trim();
        };
    }

    @Nullable
    private static String resolveGuildOwnerId(@Nonnull PlayerRef playerRef) {
        GuildManager guildManager = ExamplePlugin.get().getGuildManager();
        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        return guild == null ? null : guild.getId().toString();
    }

    @Nullable
    private static Message resolveOwnerError(@Nonnull BankScope scope) {
        return switch (scope) {
            case Guild -> Message.translation("pixelbays.rpg.guild.error.notInGuild");
            case Profession -> Message.translation("pixelbays.rpg.bank.error.professionRequired");
            case Custom -> Message.translation("pixelbays.rpg.bank.error.ownerRequired");
            case Character, Player, Account, Warband, Global -> null;
        };
    }

    @Nonnull
    public static List<String> normalizeBankTypeIds(@Nonnull List<String> bankTypeIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String bankTypeId : bankTypeIds) {
            if (bankTypeId == null) {
                continue;
            }

            String trimmed = bankTypeId.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }
        }
        return new ArrayList<>(normalized);
    }
}