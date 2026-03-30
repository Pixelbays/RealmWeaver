package org.pixelbays.rpg.economy.currency.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.economy.currency.CurrencyAccessContext;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.command.CurrencyCommandUtil;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeDefinition;
import org.pixelbays.rpg.economy.currency.config.CurrencyTypeRegistry;

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
public class CurrencyPage extends CustomUIPage {

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String CONVERSION_LABEL = "#ConversionLabel";
    private static final String CHARACTER_LABEL = "#CharacterLabel";
    private static final String ACCOUNT_LABEL = "#AccountLabel";
    private static final String GUILD_LABEL = "#GuildLabel";

    private static final String REFRESH_BUTTON = "#RefreshButton";
    private static final String NORMALIZE_CHARACTER_BUTTON = "#NormalizeCharacterButton";
    private static final String NORMALIZE_ACCOUNT_BUTTON = "#NormalizeAccountButton";
    private static final String NORMALIZE_GUILD_BUTTON = "#NormalizeGuildButton";

    private final CurrencyManager currencyManager;

    public CurrencyPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.currencyManager = Realmweavers.get().getCurrencyManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/CurrencyPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, ref, store, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> handleAction(ref, store, action));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Message statusMessage = null;
        switch (action) {
            case "Refresh" -> statusMessage = Message.translation("pixelbays.rpg.currency.ui.status.refreshed");
            case "NormalizeCharacter" -> statusMessage = normalizeScope(CurrencyScope.Character);
            case "NormalizeAccount" -> statusMessage = normalizeScope(CurrencyScope.Account);
            case "NormalizeGuild" -> statusMessage = normalizeScope(CurrencyScope.Guild);
            default -> statusMessage = Message.translation("pixelbays.rpg.common.unknownError");
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, ref, store, statusMessage);
        sendUpdate(commandBuilder);
    }

    @Nonnull
    private Message normalizeScope(@Nonnull CurrencyScope scope) {
        String ownerId = CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            return Message.translation("pixelbays.rpg.currency.error.scopeUnavailable")
                    .param("scope", scope.name().toLowerCase());
        }

        boolean changed = currencyManager.normalizeWallet(scope, ownerId);
        return Message.translation(changed
                ? "pixelbays.rpg.currency.success.normalized"
                : "pixelbays.rpg.currency.success.normalizeNoChange")
                .param("scope", scope.name().toLowerCase());
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                REFRESH_BUTTON,
                new EventData().append("Action", "Refresh"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                NORMALIZE_CHARACTER_BUTTON,
                new EventData().append("Action", "NormalizeCharacter"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                NORMALIZE_ACCOUNT_BUTTON,
                new EventData().append("Action", "NormalizeAccount"));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                NORMALIZE_GUILD_BUTTON,
                new EventData().append("Action", "NormalizeGuild"));
    }

        private void appendView(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable Message statusMessage) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        CurrencyAccessContext characterContext = player == null
                ? CurrencyAccessContext.empty()
                : CurrencyAccessContext.fromInventory(player.getInventory());

        commandBuilder.set(CONVERSION_LABEL + ".Text", buildConversionSummary());
        commandBuilder.set(CHARACTER_LABEL + ".Text", buildScopeSummary(CurrencyScope.Character, characterContext));
        commandBuilder.set(ACCOUNT_LABEL + ".Text", buildScopeSummary(CurrencyScope.Account, CurrencyAccessContext.empty()));
        commandBuilder.set(GUILD_LABEL + ".Text", buildScopeSummary(CurrencyScope.Guild, CurrencyAccessContext.empty()));
    }

    @Nonnull
    private String buildScopeSummary(@Nonnull CurrencyScope scope, @Nonnull CurrencyAccessContext accessContext) {
        String ownerId = CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            return noneText();
        }

        List<String> lines = new ArrayList<>();
        for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getByScope(scope)) {
            if (!definition.isVisibleInUi()) {
                continue;
            }

            long balance = currencyManager.getBalance(scope, ownerId, definition.getId(), accessContext);
            StringBuilder line = new StringBuilder();
            line.append(definition.getDisplayName())
                    .append(": ")
                    .append(balance)
                    .append(" [")
                    .append(modeLabel(definition))
                    .append(']');
            if (definition.hasAutoConversions()) {
                line.append(" ↺");
            }
            lines.add(line.toString());
        }

        return lines.isEmpty() ? noneText() : String.join("\n", lines);
    }

    @Nonnull
    private String buildConversionSummary() {
        List<String> lines = new ArrayList<>();
        for (CurrencyTypeDefinition definition : CurrencyTypeRegistry.getVisible()) {
            for (CurrencyTypeDefinition.CurrencyConversionDefinition conversion : definition.getConversions()) {
                if (conversion == null || !conversion.isValid()) {
                    continue;
                }

                CurrencyTypeDefinition target = CurrencyTypeRegistry.get(conversion.getTargetCurrencyId());
                String targetName = target == null ? conversion.getTargetCurrencyId() : target.getDisplayName();
                lines.add(definition.getDisplayName() + " → " + targetName + " ("
                        + conversion.getSourceAmount() + ":" + conversion.getTargetAmount() + ")");
            }
        }

        return lines.isEmpty() ? noneText() : String.join("\n", lines);
    }

    @Nonnull
    private String modeLabel(@Nonnull CurrencyTypeDefinition definition) {
        return switch (definition.getStorageMode()) {
            case NumericWallet -> "wallet";
            case PhysicalItem -> "item";
            case ItemWallet -> "wallet-item";
            case Hybrid -> "hybrid";
        };
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
