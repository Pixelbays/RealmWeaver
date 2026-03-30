package org.pixelbays.rpg.economy.banks.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.banks.BankAccount;
import org.pixelbays.rpg.economy.banks.BankActionResult;
import org.pixelbays.rpg.economy.banks.BankManager;
import org.pixelbays.rpg.economy.banks.command.BankCommandUtil;
import org.pixelbays.rpg.economy.banks.config.BankScope;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class BankPage extends CustomUIPage {

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String OVERVIEW_LABEL = "#OverviewLabel";
    private static final String PROFESSION_FIELD = "#ProfessionField";

    private static final String PERSONAL_BUTTON = "#PersonalButton";
    private static final String ACCOUNT_BUTTON = "#AccountButton";
    private static final String GUILD_BUTTON = "#GuildButton";
    private static final String PROFESSION_BUTTON = "#ProfessionButton";

    private final BankManager bankManager;
    private final GuildManager guildManager;

    public BankPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.bankManager = Realmweavers.get().getBankManager();
        this.guildManager = Realmweavers.get().getGuildManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/BankPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String professionId = extractString(rawData, "@ProfessionId");
        World world = store.getExternalData().getWorld();
        world.execute(() -> handleAction(ref, store, action, professionId));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String professionId) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Message statusMessage;
        BankAccount selectedBank = null;

        switch (action) {
            case "OpenPersonal" -> {
                BankActionResult result = bankManager.getOrCreateDefaultPersonalBank(playerRef.getUuid(), player.getInventory());
                statusMessage = BankCommandUtil.managerResultMessage(result.getMessage());
                selectedBank = result.getBankAccount();
            }
            case "OpenAccount" -> {
                BankActionResult result = bankManager.getOrCreateDefaultAccountBank(playerRef.getUuid(), player.getInventory());
                statusMessage = BankCommandUtil.managerResultMessage(result.getMessage());
                selectedBank = result.getBankAccount();
            }
            case "OpenGuild" -> {
                Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
                if (guild == null) {
                    statusMessage = Message.translation("pixelbays.rpg.guild.error.notInGuild");
                } else {
                    BankActionResult result = bankManager.getOrCreateDefaultGuildBank(guild.getId(), playerRef.getUuid(),
                            player.getInventory());
                    statusMessage = BankCommandUtil.managerResultMessage(result.getMessage());
                    selectedBank = result.getBankAccount();
                }
            }
            case "OpenProfession" -> {
                if (professionId == null || professionId.isBlank()) {
                    statusMessage = Message.translation("pixelbays.rpg.bank.error.professionRequired");
                } else {
                    BankActionResult result = bankManager.getOrCreateDefaultProfessionBank(playerRef.getUuid(),
                            professionId.trim(), player.getInventory());
                    statusMessage = BankCommandUtil.managerResultMessage(result.getMessage());
                    selectedBank = result.getBankAccount();
                }
            }
            default -> statusMessage = Message.translation("pixelbays.rpg.common.unknownError");
        }

        if (selectedBank != null) {
            BankTypeDefinition definition = bankManager.getDefinition(selectedBank);
            String initialTabId = bankManager.resolveInitialTabId(selectedBank);
            if (definition != null && initialTabId != null) {
                player.getPageManager().openCustomPage(ref, store, new BankStoragePage(playerRef, selectedBank.getId(), initialTabId));
                return;
            }
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage, selectedBank);
        sendUpdate(commandBuilder);
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, PERSONAL_BUTTON,
                new EventData().append("Action", "OpenPersonal"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, ACCOUNT_BUTTON,
                new EventData().append("Action", "OpenAccount"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, GUILD_BUTTON,
                new EventData().append("Action", "OpenGuild"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, PROFESSION_BUTTON,
                new EventData().append("Action", "OpenProfession")
                        .append("@ProfessionId", PROFESSION_FIELD + ".Value"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nullable Message statusMessage,
            @Nullable BankAccount selectedBank) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        Map<String, String> params = new HashMap<>();
        String characterOwnerId = Realmweavers.get().getCharacterManager().resolveCharacterOwnerId(playerRef);
        params.put("character", summarize(characterOwnerId.isBlank()
            ? List.of()
            : bankManager.getBanksForOwner(BankScope.Character, characterOwnerId)));
        params.put("account", summarize(bankManager.getBanksForOwner(BankScope.Account, playerRef.getUuid().toString())));

        Guild guild = guildManager.getGuildForMember(playerRef.getUuid());
        if (guild != null) {
            params.put("guild", summarize(bankManager.getBanksForOwner(BankScope.Guild, guild.getId().toString())));
        } else {
            params.put("guild", noneText());
        }

        if (selectedBank != null && selectedBank.getOwnerScope() == BankScope.Profession) {
            params.put("profession", summarize(List.of(selectedBank)));
        } else {
            params.put("profession", noneText());
        }

        commandBuilder.setObject(OVERVIEW_LABEL + ".Text",
                LocalizableString.fromMessageId("pixelbays.rpg.bank.ui.overviewBody", params));
    }

    @Nonnull
    private String summarize(@Nonnull List<BankAccount> banks) {
        if (banks.isEmpty()) {
            return noneText();
        }

        StringBuilder builder = new StringBuilder();
        for (BankAccount bank : banks) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(bank.getDisplayName())
                    .append(" [")
                    .append(bank.getBankTypeId())
                    .append("] tabs=")
                    .append(bank.getUnlockedTabIds().size());
        }
        return builder.toString();
    }

    @Nonnull
    private String noneText() {
        var formatted = Message.translation("pixelbays.rpg.common.none").getFormattedMessage();
        return formatted.rawText != null ? formatted.rawText : "None";
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
    }

    @Nullable
    private static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue sp) {
            return sp.value;
        }
        if (value instanceof IntParamValue ip) {
            return String.valueOf(ip.value);
        }
        if (value instanceof LongParamValue lp) {
            return String.valueOf(lp.value);
        }
        if (value instanceof DoubleParamValue dp) {
            return String.valueOf(dp.value);
        }
        if (value instanceof BoolParamValue bp) {
            return String.valueOf(bp.value);
        }
        return String.valueOf(value);
    }

    @Nullable
    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = rawData.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return rawData.substring(firstQuote + 1, secondQuote);
    }
}
