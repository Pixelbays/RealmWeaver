package org.pixelbays.rpg.global.input;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.ui.SpellbookPage;
import org.pixelbays.rpg.economy.banks.ui.BankPage;
import org.pixelbays.rpg.economy.currency.ui.CurrencyPage;
import org.pixelbays.rpg.global.config.BuildFlags;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.settings.UiInputModSettings;
import org.pixelbays.rpg.global.config.settings.UiInputModSettings.UiInputPageTarget;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.ui.GuildPage;
import org.pixelbays.rpg.guild.ui.GuildApplicationsPage;
import org.pixelbays.rpg.inventory.ui.RpgInventoryPage;
import org.pixelbays.rpg.mail.ui.MailPage;
import org.pixelbays.rpg.party.finder.ui.GroupFinderPage;
import org.pixelbays.rpg.party.ui.PartyPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ForkedChainId;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.CancelInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class UiInputFilter implements PlayerPacketFilter {

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        if (playerRef == null || !(packet instanceof SyncInteractionChains syncPacket)) {
            return false;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            return false;
        }

        UiInputModSettings settings = config.getUiInputSettings();
        if (!settings.isEnabled() || !settings.hasConfiguredTargets()) {
            return false;
        }

        List<SyncInteractionChain> remainingChains = new ArrayList<>(syncPacket.updates.length);
        List<TriggeredUiInput> triggeredInputs = new ArrayList<>(1);

        for (SyncInteractionChain chain : syncPacket.updates) {
            UiInputPageTarget target = resolveTarget(config, settings, chain);
            if (target == UiInputPageTarget.None) {
                remainingChains.add(chain);
                continue;
            }

            triggeredInputs.add(new TriggeredUiInput(target, chain.chainId, chain.forkedId, chain.interactionType));
        }

        if (triggeredInputs.isEmpty()) {
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return false;
        }

        world.execute(() -> handleTriggeredInputs(ref, playerRef, triggeredInputs));

        syncPacket.updates = remainingChains.toArray(new SyncInteractionChain[0]);
        return remainingChains.isEmpty();
    }

    @Nonnull
    private UiInputPageTarget resolveTarget(@Nonnull RpgModConfig config,
            @Nonnull UiInputModSettings settings,
            @Nonnull SyncInteractionChain chain) {
        if (!chain.initial) {
            return UiInputPageTarget.None;
        }

        UiInputPageTarget target = settings.getEffectiveTarget(chain.interactionType);
        if (target == UiInputPageTarget.None || !isTargetAvailable(config, target)) {
            return UiInputPageTarget.None;
        }

        return target;
    }

    private boolean isTargetAvailable(@Nonnull RpgModConfig config, @Nonnull UiInputPageTarget target) {
        return switch (target) {
            case None -> false;
            case Spellbook -> BuildFlags.ABILITY_MODULE && config.isAbilityModuleEnabled();
            case RpgInventory -> BuildFlags.INVENTORY_MODULE && config.isInventoryModuleEnabled();
            case Party -> BuildFlags.PARTY_MODULE && config.isPartyModuleEnabled();
            case Guild -> BuildFlags.GUILD_MODULE && config.isGuildModuleEnabled();
            case Mail -> BuildFlags.MAIL_MODULE && config.isMailModuleEnabled();
            case Bank -> BuildFlags.BANK_MODULE && config.isBankModuleEnabled();
            case Currency -> BuildFlags.CURRENCY_MODULE && config.isCurrencyModuleEnabled();
            case GroupFinder -> BuildFlags.PARTY_MODULE
                    && config.isPartyModuleEnabled()
                    && config.getPartySettings().isGroupFinderEnabled();
        };
    }

    private void handleTriggeredInputs(@Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull List<TriggeredUiInput> triggeredInputs) {
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        for (TriggeredUiInput triggered : triggeredInputs) {
            playerRef.getPacketHandler().writeNoCache(new CancelInteractionChain(triggered.chainId(), triggered.forkedId()));
        }

        TriggeredUiInput firstTriggered = triggeredInputs.get(0);
        openTargetPage(ref, store, playerRef, player, firstTriggered.target());

        RpgLogging.debugDeveloper(
                "[UI Inputs] Opened %s from %s for %s",
                firstTriggered.target(),
                firstTriggered.interactionType(),
                playerRef.getUsername());
    }

    private void openTargetPage(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player,
            @Nonnull UiInputPageTarget target) {
        switch (target) {
            case Spellbook -> player.getPageManager().openCustomPage(ref, store, new SpellbookPage(playerRef));
            case RpgInventory -> player.getPageManager().openCustomPage(ref, store, new RpgInventoryPage(playerRef));
            case Party -> player.getPageManager().openCustomPage(ref, store, new PartyPage(playerRef));
                case Guild -> player.getPageManager().openCustomPage(
                    ref,
                    store,
                    Realmweavers.get().getGuildManager().getGuildForMember(playerRef.getUuid()) == null
                        ? new GuildApplicationsPage(playerRef)
                        : new GuildPage(playerRef));
            case Mail -> player.getPageManager().openCustomPage(ref, store, new MailPage(playerRef));
            case Bank -> player.getPageManager().openCustomPage(ref, store, new BankPage(playerRef));
            case Currency -> player.getPageManager().openCustomPage(ref, store, new CurrencyPage(playerRef));
            case GroupFinder -> player.getPageManager().openCustomPage(ref, store, new GroupFinderPage(playerRef));
            case None -> {
            }
        }
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

    private record TriggeredUiInput(
            @Nonnull UiInputPageTarget target,
            int chainId,
            @Nullable ForkedChainId forkedId,
            @Nonnull InteractionType interactionType) {
    }
}