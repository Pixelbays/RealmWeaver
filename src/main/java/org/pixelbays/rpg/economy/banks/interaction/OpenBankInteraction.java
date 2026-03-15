package org.pixelbays.rpg.economy.banks.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.economy.banks.ui.BankUiOpener;
import org.pixelbays.rpg.economy.banks.config.BankTypeDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class OpenBankInteraction extends SimpleInstantInteraction {

        private static final FunctionCodec<String[], List<String>> BANK_TYPE_LIST_CODEC = new FunctionCodec<>(
                        new ArrayCodec<>(BankTypeDefinition.CHILD_ASSET_CODEC, String[]::new),
                        arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                        list -> list == null ? null : list.toArray(String[]::new));

    @Nonnull
    public static final BuilderCodec<OpenBankInteraction> CODEC = BuilderCodec.builder(
            OpenBankInteraction.class,
            OpenBankInteraction::new,
            SimpleInstantInteraction.CODEC)
            .documentation("Opens the RPG bank UI or a specific configured bank type.")
            .append(new KeyedCodec<>("BankTypeId", BankTypeDefinition.CHILD_ASSET_CODEC, false),
                    (i, v) -> i.bankTypeId = v,
                    i -> i.bankTypeId)
            .add()
            .append(new KeyedCodec<>("BankTypeIds", BANK_TYPE_LIST_CODEC, false),
                    (i, v) -> i.bankTypeIds = v,
                    i -> i.bankTypeIds)
            .add()
            .append(new KeyedCodec<>("ProfessionId", Codec.STRING, false),
                    (i, v) -> i.professionId = v,
                    i -> i.professionId)
            .add()
            .append(new KeyedCodec<>("CustomOwnerId", Codec.STRING, false),
                    (i, v) -> i.customOwnerId = v,
                    i -> i.customOwnerId)
            .add()
            .append(new KeyedCodec<>("MenuTitle", Codec.STRING, false),
                    (i, v) -> i.menuTitle = v,
                    i -> i.menuTitle)
            .add()
            .append(new KeyedCodec<>("OpenSelectionWhenTypeMissing", Codec.BOOLEAN, false),
                    (i, v) -> i.openSelectionWhenTypeMissing = v,
                    i -> i.openSelectionWhenTypeMissing)
            .add()
            .build();

    private String bankTypeId = "";
    private List<String> bankTypeIds = new ArrayList<>();
    private String professionId = "";
    private String customOwnerId = "";
    private String menuTitle = "";
    private boolean openSelectionWhenTypeMissing = true;

    @Override
    protected void firstRun(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        context.getState().state = InteractionState.Finished;

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

                List<String> configuredBankTypes = new ArrayList<>();
                if (bankTypeId != null && !bankTypeId.isBlank()) {
                        configuredBankTypes.add(bankTypeId);
                }
                if (bankTypeIds != null && !bankTypeIds.isEmpty()) {
                        configuredBankTypes.addAll(bankTypeIds);
                }

                List<String> normalizedBankTypes = BankUiOpener.normalizeBankTypeIds(configuredBankTypes);
                if (normalizedBankTypes.size() > 1) {
                        BankUiOpener.openSelectionUi(
                                        entityRef,
                                        entityRef.getStore(),
                                        player,
                                        playerRef,
                                        normalizedBankTypes,
                                        professionId,
                                        customOwnerId,
                                        menuTitle);
                        return;
                }

        BankUiOpener.openBankType(
                entityRef,
                entityRef.getStore(),
                player,
                playerRef,
                                normalizedBankTypes.isEmpty() ? bankTypeId : normalizedBankTypes.get(0),
                professionId,
                customOwnerId,
                openSelectionWhenTypeMissing);
    }
}