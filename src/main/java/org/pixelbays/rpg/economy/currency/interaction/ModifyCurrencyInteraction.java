package org.pixelbays.rpg.economy.currency.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.currency.CurrencyActionResult;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.command.CurrencyCommandUtil;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.interaction.InteractionPlayerUtil;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class ModifyCurrencyInteraction extends SimpleInstantInteraction {

    public enum CurrencyMutationMode {
        Add,
        Remove
    }

    @Nonnull
    public static final BuilderCodec<ModifyCurrencyInteraction> CODEC = BuilderCodec.builder(
            ModifyCurrencyInteraction.class,
            ModifyCurrencyInteraction::new,
            SimpleInstantInteraction.CODEC)
            .documentation("Adds or removes configured currency from the interacting player.")
            .append(new KeyedCodec<>("CurrencyId", Codec.STRING),
                    (i, v) -> i.currencyId = v,
                    i -> i.currencyId)
            .add()
            .append(new KeyedCodec<>("Amount", Codec.LONG),
                    (i, v) -> i.amount = v,
                    i -> i.amount)
            .add()
            .append(new KeyedCodec<>("Scope", new EnumCodec<>(CurrencyScope.class), false, true),
                    (i, v) -> i.scope = v,
                    i -> i.scope)
            .add()
            .append(new KeyedCodec<>("Mode", new EnumCodec<>(CurrencyMutationMode.class), false, true),
                    (i, v) -> i.mode = v,
                    i -> i.mode)
            .add()
            .append(new KeyedCodec<>("CustomOwnerId", Codec.STRING, false),
                    (i, v) -> i.customOwnerId = v,
                    i -> i.customOwnerId)
            .add()
            .append(new KeyedCodec<>("SendResultMessage", Codec.BOOLEAN, false),
                    (i, v) -> i.sendResultMessage = v,
                    i -> i.sendResultMessage)
            .add()
            .build();

    private String currencyId = "";
    private long amount = 0L;
    private CurrencyScope scope = CurrencyScope.Character;
    private CurrencyMutationMode mode = CurrencyMutationMode.Add;
    private String customOwnerId = "";
    private boolean sendResultMessage = false;

    @Override
    protected void firstRun(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        context.getState().state = InteractionState.Failed;

        if (currencyId == null || currencyId.isBlank() || amount <= 0L) {
            return;
        }

        PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);
        if (playerRef == null) {
            return;
        }

        String ownerId = resolveOwnerId(playerRef);
        if (ownerId == null || ownerId.isBlank()) {
            sendOwnerResolutionFailure(context);
            return;
        }

        CurrencyManager currencyManager = ExamplePlugin.get().getCurrencyManager();
        CurrencyActionResult result = switch (mode) {
            case Add -> currencyManager.addBalance(scope, ownerId, currencyId, amount);
            case Remove -> currencyManager.removeBalance(scope, ownerId, currencyId, amount);
        };

        if (sendResultMessage) {
            InteractionPlayerUtil.sendMessage(context, CurrencyCommandUtil.managerResultMessage(result));
        }

        context.getState().state = result.isSuccess() ? InteractionState.Finished : InteractionState.Failed;
    }

    @Nullable
    private String resolveOwnerId(@Nonnull PlayerRef playerRef) {
        if (customOwnerId != null && !customOwnerId.isBlank()) {
            return customOwnerId;
        }

        return CurrencyCommandUtil.resolveOwnerId(scope, playerRef);
    }

    private void sendOwnerResolutionFailure(@Nonnull InteractionContext context) {
        if (!sendResultMessage) {
            return;
        }

        Message message = scope == CurrencyScope.Custom
                ? Message.translation("pixelbays.rpg.currency.error.ownerRequired")
                : Message.translation("pixelbays.rpg.currency.error.scopeUnavailable")
                        .param("scope", scope.name().toLowerCase());
        InteractionPlayerUtil.sendMessage(context, message);
    }
}