package org.pixelbays.rpg.economy.banks.command;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.banks.BankAccount;
import org.pixelbays.rpg.economy.banks.BankManager;
import org.pixelbays.rpg.economy.banks.config.BankScope;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BankInfoCommand extends AbstractPlayerCommand {

    private final BankManager bankManager;
    private final GuildManager guildManager;

    public BankInfoCommand() {
        super("info", "Show your bank overview");
        this.bankManager = ExamplePlugin.get().getBankManager();
        this.guildManager = ExamplePlugin.get().getGuildManager();
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

    String playerId = String.valueOf(playerRef.getUuid());
    String characterOwnerId = ExamplePlugin.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
    List<BankAccount> characterBanks = characterOwnerId.isBlank()
        ? List.of()
        : bankManager.getBanksForOwner(BankScope.Character, characterOwnerId);
        List<BankAccount> accountBanks = bankManager.getBanksForOwner(BankScope.Account, playerId);

        player.sendMessage(Message.translation("pixelbays.rpg.bank.info.header"));
        player.sendMessage(Message.translation("pixelbays.rpg.bank.info.character")
                .param("banks", summarize(characterBanks)));
        player.sendMessage(Message.translation("pixelbays.rpg.bank.info.account")
                .param("banks", summarize(accountBanks)));

        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        if (guild != null) {
                List<BankAccount> guildBanks = bankManager.getBanksForOwner(BankScope.Guild, String.valueOf(guild.getId()));
            player.sendMessage(Message.translation("pixelbays.rpg.bank.info.guild")
                    .param("banks", summarize(guildBanks)));
        } else {
            player.sendMessage(Message.translation("pixelbays.rpg.bank.info.guild")
                    .param("banks", Message.translation("pixelbays.rpg.common.none")));
        }
    }

    @SuppressWarnings("null")
    private String summarize(@Nonnull List<BankAccount> banks) {
        if (banks.isEmpty()) {
            String rawText = Message.translation("pixelbays.rpg.common.none").getFormattedMessage().rawText;
            return rawText != null ? rawText : "None";
        }

        return banks.stream()
                .map(bank -> bank.getDisplayName() + " [" + bank.getBankTypeId() + "]")
                .collect(Collectors.joining(", "));
    }
}
