package org.pixelbays.rpg.mail.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.mail.ui.MailPage;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
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
public class OpenMailInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<OpenMailInteraction> CODEC = BuilderCodec.builder(
            OpenMailInteraction.class,
            OpenMailInteraction::new,
            SimpleInstantInteraction.CODEC)
            .documentation("Opens the RPG mailbox UI.")
            .append(new KeyedCodec<>("RequireMailModuleEnabled", com.hypixel.hytale.codec.Codec.BOOLEAN, false),
                    (i, v) -> i.requireMailModuleEnabled = v,
                    i -> i.requireMailModuleEnabled)
            .add()
            .build();

    private boolean requireMailModuleEnabled = true;

    @Override
    protected void firstRun(@Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        context.getState().state = InteractionState.Finished;

        if (requireMailModuleEnabled && !isMailModuleEnabled()) {
            return;
        }

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

        player.getPageManager().openCustomPage(entityRef, entityRef.getStore(), new MailPage(playerRef));
    }

    private boolean isMailModuleEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isMailModuleEnabled();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("default");
        if (config != null) {
            return config;
        }

        config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }
}
