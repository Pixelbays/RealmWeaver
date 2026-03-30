package org.pixelbays.rpg.achievement.system;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.achievement.component.AchievementComponent.AchievementProgress;
import org.pixelbays.rpg.achievement.config.AchievementDefinition;
import org.pixelbays.rpg.achievement.config.AchievementDefinition.AchievementCriterionDefinition;
import org.pixelbays.rpg.achievement.config.AchievementDefinition.AchievementReward;
import org.pixelbays.rpg.achievement.config.settings.AchievementModSettings;
import org.pixelbays.rpg.achievement.event.AchievementUnlockedEvent;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.event.ClassLearnedEvent;
import org.pixelbays.rpg.economy.currency.CurrencyManager;
import org.pixelbays.rpg.economy.currency.config.CurrencyScope;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.config.EventTitleConfig;
import org.pixelbays.rpg.leveling.config.LevelUpEffects;
import org.pixelbays.rpg.leveling.config.NotificationConfig;
import org.pixelbays.rpg.leveling.event.LevelUpEvent;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.event.RaceChangedEvent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.event.ClassAbilityUnlockedEvent;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.protocol.packets.interface_.KillFeedMessage;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import it.unimi.dsi.fastutil.objects.ObjectList;
import com.hypixel.hytale.component.spatial.SpatialResource;

@SuppressWarnings("null")
public final class AchievementSystem {

    private final CharacterManager characterManager;
    private final CurrencyManager currencyManager;

    public AchievementSystem(@Nonnull CharacterManager characterManager, @Nonnull CurrencyManager currencyManager) {
        this.characterManager = characterManager;
        this.currencyManager = currencyManager;
    }

    public void register() {
        ExamplePlugin.get().getEventRegistry().registerGlobal(PlayerReadyEvent.class,
                event -> scheduleSync(event.getPlayerRef(), event.getPlayerRef().getStore()));
        ExamplePlugin.get().getEventRegistry().register(LevelUpEvent.class,
                event -> scheduleSync(event.playerRef(), event.playerRef().getStore()));
        ExamplePlugin.get().getEventRegistry().register(ClassLearnedEvent.class,
                event -> scheduleSync(event.entityRef(), event.entityRef().getStore()));
        ExamplePlugin.get().getEventRegistry().register(RaceChangedEvent.class,
                event -> scheduleSync(event.entityRef(), event.entityRef().getStore()));
        ExamplePlugin.get().getEventRegistry().register(ClassAbilityUnlockedEvent.class,
                event -> scheduleSync(event.entityRef(), event.entityRef().getStore()));
    }

    public void synchronizeEntityAchievements(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        if (!isEnabled() || !entityRef.isValid()) {
            return;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        DefaultAssetMap<String, AchievementDefinition> assetMap = AchievementDefinition.getAssetMap();
        if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
            return;
        }

        AchievementComponent characterState = getOrCreateCharacterState(entityRef, store);
        AchievementComponent accountState = characterManager.getOrCreateAccountAchievementProgress(
                playerRef.getUuid(),
                playerRef.getUsername());

        if (characterState.getDisplayedTitle().isBlank() && !accountState.getDisplayedTitle().isBlank()) {
            characterState.setDisplayedTitle(accountState.getDisplayedTitle());
        }

        Snapshot snapshot = Snapshot.capture(store, entityRef);
        boolean characterChanged = false;
        boolean accountChanged = false;

        for (AchievementDefinition definition : assetMap.getAssetMap().values()) {
            if (definition == null || definition.getCriteria().isEmpty()) {
                continue;
            }

            AchievementComponent targetState = definition.isAccountWide() ? accountState : characterState;
            boolean changed = evaluateDefinition(definition, targetState, snapshot);
            if (changed) {
                if (definition.isAccountWide()) {
                    accountChanged = true;
                } else {
                    characterChanged = true;
                }
            }
        }

        if (accountChanged) {
            characterManager.saveAccountAchievementProgress(playerRef.getUuid(), playerRef.getUsername(), accountState);
        }
        if (characterChanged) {
            persistCharacterProgress(playerRef, entityRef, store);
        }
    }

    public boolean unlockAchievement(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String achievementId) {
        if (!isEnabled() || achievementId.isBlank() || !entityRef.isValid()) {
            return false;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return false;
        }

        AchievementDefinition definition = resolveDefinition(achievementId);
        if (definition == null) {
            return false;
        }

        AchievementComponent characterState = getOrCreateCharacterState(entityRef, store);
        AchievementComponent accountState = characterManager.getOrCreateAccountAchievementProgress(
                playerRef.getUuid(),
                playerRef.getUsername());
        AchievementComponent targetState = definition.isAccountWide() ? accountState : characterState;
        if (targetState.isUnlocked(definition.getId())) {
            return true;
        }

        if (!targetState.unlock(definition.getId(), definition.getPoints())) {
            return false;
        }

        Snapshot snapshot = Snapshot.capture(store, entityRef);
        applyRewards(definition, targetState, snapshot);
        triggerConfiguredEffects(getSettings().getUnlockEffects(), definition, snapshot, null);
        AchievementUnlockedEvent.dispatch(entityRef, definition.getId(), definition.getPoints(), definition.isAccountWide());
        persistAchievementState(definition, targetState, playerRef, entityRef, store);
        return true;
    }

    public boolean grantAchievementProgress(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String achievementId,
            @Nullable String criterionId,
            int amount) {
        if (!isEnabled() || achievementId.isBlank() || amount <= 0 || !entityRef.isValid()) {
            return false;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return false;
        }

        AchievementDefinition definition = resolveDefinition(achievementId);
        if (definition == null) {
            return false;
        }

        AchievementCriterionDefinition criterion = resolveCriterion(definition, criterionId);
        if (criterion == null) {
            return false;
        }

        AchievementComponent characterState = getOrCreateCharacterState(entityRef, store);
        AchievementComponent accountState = characterManager.getOrCreateAccountAchievementProgress(
                playerRef.getUuid(),
                playerRef.getUsername());
        AchievementComponent targetState = definition.isAccountWide() ? accountState : characterState;
        if (targetState.isUnlocked(definition.getId())) {
            return true;
        }

        AchievementProgress progress = targetState.getOrCreateProgress(definition.getId());
        int currentValue = progress.getCriterionProgress(criterion.getId()) + amount;
        if (!progress.recordProgress(criterion.getId(), currentValue)) {
            return false;
        }

        Snapshot snapshot = Snapshot.capture(store, entityRef);
        ProgressUpdate progressUpdate = new ProgressUpdate(criterion.getId(), currentValue, criterion.getTargetValue());
        if (definition.isSatisfiedBy(progress) && targetState.unlock(definition.getId(), definition.getPoints())) {
            applyRewards(definition, targetState, snapshot);
            triggerConfiguredEffects(getSettings().getUnlockEffects(), definition, snapshot, progressUpdate);
            AchievementUnlockedEvent.dispatch(entityRef, definition.getId(), definition.getPoints(), definition.isAccountWide());
        } else {
            triggerConfiguredEffects(getSettings().getProgressGainedEffects(), definition, snapshot, progressUpdate);
        }

        persistAchievementState(definition, targetState, playerRef, entityRef, store);
        return true;
    }

    private void scheduleSync(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        if (!entityRef.isValid()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        world.execute(() -> synchronizeEntityAchievements(entityRef, store));
    }

    private boolean evaluateDefinition(@Nonnull AchievementDefinition definition,
            @Nonnull AchievementComponent state,
            @Nonnull Snapshot snapshot) {
        if (state.isUnlocked(definition.getId())) {
            return false;
        }

        AchievementProgress progress = state.getOrCreateProgress(definition.getId());
        boolean changed = false;
        ProgressUpdate progressUpdate = null;

        for (AchievementCriterionDefinition criterion : definition.getCriteria()) {
            if (criterion == null) {
                continue;
            }
            int currentValue = resolveCriterionValue(criterion, snapshot);
            if (progress.recordProgress(criterion.getId(), currentValue)) {
                changed = true;
                progressUpdate = new ProgressUpdate(
                        criterion.getId(),
                        currentValue,
                        criterion.getTargetValue());
            }
        }

        if (definition.isSatisfiedBy(progress) && state.unlock(definition.getId(), definition.getPoints())) {
            applyRewards(definition, state, snapshot);
            triggerConfiguredEffects(getSettings().getUnlockEffects(), definition, snapshot, progressUpdate);
            AchievementUnlockedEvent.dispatch(snapshot.entityRef(), definition.getId(), definition.getPoints(), definition.isAccountWide());
            changed = true;
        } else if (changed && progressUpdate != null) {
            triggerConfiguredEffects(getSettings().getProgressGainedEffects(), definition, snapshot, progressUpdate);
        }

        return changed;
    }

    @Nullable
    private AchievementDefinition resolveDefinition(@Nonnull String achievementId) {
        DefaultAssetMap<String, AchievementDefinition> assetMap = AchievementDefinition.getAssetMap();
        if (assetMap == null) {
            return null;
        }
        return assetMap.getAsset(achievementId);
    }

    @Nullable
    private AchievementCriterionDefinition resolveCriterion(@Nonnull AchievementDefinition definition,
            @Nullable String criterionId) {
        if (criterionId != null && !criterionId.isBlank()) {
            for (AchievementCriterionDefinition criterion : definition.getCriteria()) {
                if (criterion != null && criterionId.equalsIgnoreCase(criterion.getId())) {
                    return criterion;
                }
            }
            return null;
        }

        if (definition.getCriteria().size() == 1) {
            return definition.getCriteria().getFirst();
        }
        return null;
    }

    private void persistAchievementState(@Nonnull AchievementDefinition definition,
            @Nonnull AchievementComponent targetState,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        if (definition.isAccountWide()) {
            characterManager.saveAccountAchievementProgress(playerRef.getUuid(), playerRef.getUsername(), targetState);
        } else {
            persistCharacterProgress(playerRef, entityRef, store);
        }
    }

    private int resolveCriterionValue(@Nonnull AchievementCriterionDefinition criterion, @Nonnull Snapshot snapshot) {
        return switch (criterion.getType()) {
            case ReachLevel -> resolveLevelValue(criterion, snapshot.levelProgression());
            case LearnClass -> resolveLearnedClassValue(criterion, snapshot.classComponent());
            case ChangeRace -> resolveRaceValue(criterion, snapshot.raceComponent());
            case UnlockAbility -> resolveAbilityValue(criterion, snapshot.abilityComponent());
        };
    }

    private int resolveLevelValue(@Nonnull AchievementCriterionDefinition criterion,
            @Nullable LevelProgressionComponent levelProgression) {
        if (levelProgression == null) {
            return 0;
        }

        if (!criterion.getSystemId().isBlank()) {
            LevelProgressionComponent.LevelSystemData data = levelProgression.getSystem(criterion.getSystemId());
            return data == null ? 0 : data.getCurrentLevel();
        }

        int bestLevel = 0;
        for (LevelProgressionComponent.LevelSystemData data : levelProgression.getAllSystems().values()) {
            if (data != null) {
                bestLevel = Math.max(bestLevel, data.getCurrentLevel());
            }
        }
        return bestLevel;
    }

    private int resolveLearnedClassValue(@Nonnull AchievementCriterionDefinition criterion,
            @Nullable ClassComponent classComponent) {
        if (classComponent == null) {
            return 0;
        }

        if (!criterion.getClassId().isBlank()) {
            return classComponent.hasLearnedClass(criterion.getClassId()) ? 1 : 0;
        }

        return classComponent.getLearnedClassIds().size();
    }

    private int resolveRaceValue(@Nonnull AchievementCriterionDefinition criterion,
            @Nullable RaceComponent raceComponent) {
        if (raceComponent == null || raceComponent.getRaceId().isBlank()) {
            return 0;
        }

        if (!criterion.getRaceId().isBlank()) {
            return criterion.getRaceId().equalsIgnoreCase(raceComponent.getRaceId()) ? 1 : 0;
        }

        return 1;
    }

    private int resolveAbilityValue(@Nonnull AchievementCriterionDefinition criterion,
            @Nullable ClassAbilityComponent abilityComponent) {
        if (abilityComponent == null) {
            return 0;
        }

        if (!criterion.getAbilityId().isBlank()) {
            return abilityComponent.getAbilityRank(criterion.getAbilityId());
        }

        return abilityComponent.getUnlockedAbilityIds().size();
    }

    private void applyRewards(@Nonnull AchievementDefinition definition,
            @Nonnull AchievementComponent state,
            @Nonnull Snapshot snapshot) {
        AchievementReward reward = definition.getReward();
        String resolvedTitle = resolveRewardTitle(reward);
        if (!resolvedTitle.isBlank()) {
            state.setDisplayedTitle(resolvedTitle);
            AchievementComponent characterState = getOrCreateCharacterState(snapshot.entityRef(), snapshot.store());
            if (characterState.getDisplayedTitle().isBlank()) {
                characterState.setDisplayedTitle(resolvedTitle);
            }
        }

        grantCurrencyRewards(reward, snapshot.playerRef());
        sendUnlockNotification(definition, snapshot.playerRef(), reward);
        sendUnlockEventTitle(definition, snapshot.playerRef(), reward);
        sendUnlockChatMessage(definition, snapshot.playerRef());
    }

    private void triggerConfiguredEffects(@Nonnull LevelUpEffects effects,
            @Nonnull AchievementDefinition definition,
            @Nonnull Snapshot snapshot,
            @Nullable ProgressUpdate progressUpdate) {
        if (effects.isEmpty()) {
            return;
        }

        if (effects.getSoundId() != null && !effects.getSoundId().isBlank()) {
            playSoundEffect(snapshot.entityRef(), effects.getSoundId(), snapshot.store());
        }

        if (effects.getParticleEffect() != null && !effects.getParticleEffect().isBlank()) {
            spawnParticleEffect(snapshot.entityRef(), effects.getParticleEffect(), snapshot.store());
        }

        if (effects.getNotification() != null) {
            sendConfiguredNotification(snapshot.playerRef(), definition, progressUpdate, effects.getNotification());
        }

        if (effects.getEventTitle() != null) {
            sendConfiguredEventTitle(snapshot.playerRef(), definition, progressUpdate, effects.getEventTitle());
        }

        if (effects.getChatMessage() != null && !effects.getChatMessage().isBlank()) {
            snapshot.playerRef().sendMessage(Message.raw(applyPlaceholders(effects.getChatMessage(), definition, progressUpdate)));
        }

        if (effects.getKillFeedPopup() != null && !effects.getKillFeedPopup().isBlank()) {
            sendConfiguredKillFeed(snapshot.playerRef(), definition, progressUpdate, effects.getKillFeedPopup());
        }
    }

    private void grantCurrencyRewards(@Nonnull AchievementReward reward, @Nonnull PlayerRef playerRef) {
        if (reward.getCurrencyRewards().isEmpty()) {
            return;
        }

        String ownerId = characterManager.resolveCharacterOwnerId(playerRef);
        if (ownerId.isBlank()) {
            return;
        }

        for (Map.Entry<String, Long> entry : reward.getCurrencyRewards().entrySet()) {
            String currencyId = entry.getKey();
            Long amount = entry.getValue();
            if (currencyId == null || currencyId.isBlank() || amount == null || amount <= 0L) {
                continue;
            }
            currencyManager.addBalance(CurrencyScope.Character, ownerId, currencyId, amount);
        }
    }

    private void sendUnlockNotification(@Nonnull AchievementDefinition definition,
            @Nonnull PlayerRef playerRef,
            @Nonnull AchievementReward reward) {
        AchievementModSettings settings = getSettings();
        if (!settings.isToastNotificationsEnabled()) {
            return;
        }

        PacketHandler packetHandler = playerRef.getPacketHandler();

        NotificationConfig notification = reward.getNotification();
        Message primary = notification == null || notification.getPrimaryMessage() == null || notification.getPrimaryMessage().isBlank()
                ? Message.translation("pixelbays.rpg.achievement.notify.primary")
                        .param("name", resolveAchievementName(definition))
            : Message.raw(applyPlaceholders(notification.getPrimaryMessage(), definition, null));
        Message secondary = notification == null || notification.getSecondaryMessage() == null || notification.getSecondaryMessage().isBlank()
                ? Message.translation("pixelbays.rpg.achievement.notify.secondary")
                        .param("points", definition.getPoints())
            : Message.raw(applyPlaceholders(notification.getSecondaryMessage(), definition, null));

        ItemWithAllMetadata icon = null;
        if (notification != null && notification.getIconItemId() != null && !notification.getIconItemId().isBlank()) {
            try {
                icon = (ItemWithAllMetadata) new ItemStack(notification.getIconItemId(), 1).toPacket();
            } catch (Exception ex) {
                RpgLogging.debugDeveloper("[AchievementSystem] Failed to create notification icon %s: %s",
                        notification.getIconItemId(),
                        ex.getMessage());
            }
        }

        NotificationUtil.sendNotification(packetHandler, primary, secondary, null, icon, NotificationStyle.Default);
    }

    private void sendConfiguredNotification(@Nonnull PlayerRef playerRef,
            @Nonnull AchievementDefinition definition,
            @Nullable ProgressUpdate progressUpdate,
            @Nonnull NotificationConfig notification) {
        PacketHandler packetHandler = playerRef.getPacketHandler();
        Message primary = notification.getPrimaryMessage() == null || notification.getPrimaryMessage().isBlank()
                ? Message.raw(resolveAchievementName(definition))
                : Message.raw(applyPlaceholders(notification.getPrimaryMessage(), definition, progressUpdate));
        Message secondary = null;
        if (notification.getSecondaryMessage() != null && !notification.getSecondaryMessage().isBlank()) {
            secondary = Message.raw(applyPlaceholders(notification.getSecondaryMessage(), definition, progressUpdate));
        }

        ItemWithAllMetadata icon = null;
        if (notification.getIconItemId() != null && !notification.getIconItemId().isBlank()) {
            try {
                icon = (ItemWithAllMetadata) new ItemStack(notification.getIconItemId(), 1).toPacket();
            } catch (Exception ex) {
                RpgLogging.debugDeveloper("[AchievementSystem] Failed to create configured icon %s: %s",
                        notification.getIconItemId(), ex.getMessage());
            }
        }

        NotificationUtil.sendNotification(packetHandler, primary, secondary, null, icon, NotificationStyle.Default);
    }

    private void sendUnlockEventTitle(@Nonnull AchievementDefinition definition,
            @Nonnull PlayerRef playerRef,
            @Nonnull AchievementReward reward) {
        AchievementModSettings settings = getSettings();
        if (!settings.isTitleNotificationsEnabled()) {
            return;
        }

        EventTitleConfig eventTitle = reward.getEventTitle();
        Message primary = eventTitle == null || eventTitle.getPrimaryMessage() == null || eventTitle.getPrimaryMessage().isBlank()
                ? Message.translation("pixelbays.rpg.achievement.title.primary")
                        .param("name", resolveAchievementName(definition))
            : Message.raw(applyPlaceholders(eventTitle.getPrimaryMessage(), definition, null));
        Message secondary = eventTitle == null || eventTitle.getSecondaryMessage() == null || eventTitle.getSecondaryMessage().isBlank()
                ? Message.translation("pixelbays.rpg.achievement.title.secondary")
                        .param("points", definition.getPoints())
            : Message.raw(applyPlaceholders(eventTitle.getSecondaryMessage(), definition, null));

        boolean major = eventTitle == null || eventTitle.isMajor();
        EventTitleUtil.showEventTitleToPlayer(playerRef, primary, secondary, major);
    }

        private void sendConfiguredEventTitle(@Nonnull PlayerRef playerRef,
            @Nonnull AchievementDefinition definition,
            @Nullable ProgressUpdate progressUpdate,
            @Nonnull EventTitleConfig eventTitle) {
        String primaryText = eventTitle.getPrimaryMessage() == null || eventTitle.getPrimaryMessage().isBlank()
            ? resolveAchievementName(definition)
            : applyPlaceholders(eventTitle.getPrimaryMessage(), definition, progressUpdate);
        String secondaryText = eventTitle.getSecondaryMessage() == null || eventTitle.getSecondaryMessage().isBlank()
            ? ""
            : applyPlaceholders(eventTitle.getSecondaryMessage(), definition, progressUpdate);
        EventTitleUtil.showEventTitleToPlayer(
            playerRef,
            Message.raw(primaryText),
            Message.raw(secondaryText),
            eventTitle.isMajor());
        }

    private void sendUnlockChatMessage(@Nonnull AchievementDefinition definition, @Nonnull PlayerRef playerRef) {
        playerRef.sendMessage(Message.translation("pixelbays.rpg.achievement.chat.unlocked")
                .param("name", resolveAchievementName(definition))
                .param("points", definition.getPoints()));
    }

    private void sendConfiguredKillFeed(@Nonnull PlayerRef playerRef,
            @Nonnull AchievementDefinition definition,
            @Nullable ProgressUpdate progressUpdate,
            @Nonnull String template) {
        String messageText = applyPlaceholders(template, definition, progressUpdate);
        Message message = Message.raw(messageText);
        KillFeedMessage killFeedPacket = new KillFeedMessage(null, message.getFormattedMessage(), "Icon_LevelUp");
        playerRef.getPacketHandler().write(killFeedPacket);
    }

    @Nonnull
    private String applyPlaceholders(@Nonnull String template,
            @Nonnull AchievementDefinition definition,
            @Nullable ProgressUpdate progressUpdate) {
        String current = progressUpdate == null ? "0" : String.valueOf(progressUpdate.currentValue());
        String target = progressUpdate == null ? "0" : String.valueOf(progressUpdate.targetValue());
        String criterion = progressUpdate == null ? "" : progressUpdate.criterionId();
        return template
                .replace("{name}", resolveAchievementName(definition))
                .replace("{points}", String.valueOf(definition.getPoints()))
                .replace("{id}", definition.getId())
                .replace("{current}", current)
                .replace("{target}", target)
                .replace("{criterion}", criterion);
    }

    private void playSoundEffect(@Nonnull Ref<EntityStore> entityRef, @Nonnull String soundId, @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        try {
            int soundIndex = SoundEvent.getAssetMap().getIndex(soundId);
            if (soundIndex == 0 || soundIndex == Integer.MIN_VALUE) {
                return;
            }

            TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
            if (transform == null) {
                return;
            }

            world.execute(() -> SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.UI, transform.getPosition(), store));
        } catch (Exception ex) {
            RpgLogging.debugDeveloper("[AchievementSystem] Failed to play sound %s: %s", soundId, ex.getMessage());
        }
    }

    private void spawnParticleEffect(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull String particleEffect,
            @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        try {
            NetworkId networkId = store.getComponent(entityRef, NetworkId.getComponentType());
            TransformComponent transform = store.getComponent(entityRef, EntityModule.get().getTransformComponentType());
            if (networkId == null || transform == null) {
                return;
            }

            ModelParticle modelParticle = new ModelParticle(
                    particleEffect,
                    com.hypixel.hytale.protocol.EntityPart.Self,
                    null,
                    null,
                    1.0f,
                    null,
                    null,
                    false);

            SpawnModelParticles packet = new SpawnModelParticles(
                    networkId.getId(),
                    new com.hypixel.hytale.protocol.ModelParticle[] { modelParticle.toPacket() });

            world.execute(() -> {
                SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = store
                        .getResource(EntityModule.get().getPlayerSpatialResourceType());
                List<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
                playerSpatialResource.getSpatialStructure().collect(transform.getPosition(), 75.0, nearbyPlayers);
                for (Ref<EntityStore> playerRef : nearbyPlayers) {
                    if (!playerRef.isValid()) {
                        continue;
                    }
                    PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
                    if (playerRefComponent != null) {
                        playerRefComponent.getPacketHandler().writeNoCache(packet);
                    }
                }
            });
        } catch (Exception ex) {
            RpgLogging.debugDeveloper("[AchievementSystem] Failed to spawn particle %s: %s", particleEffect, ex.getMessage());
        }
    }

    @Nonnull
    private String resolveAchievementName(@Nonnull AchievementDefinition definition) {
        if (!definition.getNameTranslationKey().isBlank()) {
            return Message.translation(definition.getNameTranslationKey()).getFormattedMessage().toString();
        }
        return definition.getDisplayName();
    }

    @Nonnull
    private String resolveRewardTitle(@Nonnull AchievementReward reward) {
        if (!reward.getDisplayedTitleTranslationKey().isBlank()) {
            return Message.translation(reward.getDisplayedTitleTranslationKey()).getFormattedMessage().toString();
        }
        return reward.getDisplayedTitle();
    }

    @Nonnull
    private AchievementComponent getOrCreateCharacterState(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        AchievementComponent component = store.getComponent(entityRef, AchievementComponent.getComponentType());
        if (component != null) {
            return component;
        }
        AchievementComponent created = new AchievementComponent();
        store.putComponent(entityRef, AchievementComponent.getComponentType(), created);
        return created;
    }

    private void persistCharacterProgress(@Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        if (!getSettings().isPersistProgressImmediately()) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        characterManager.saveActiveCharacter(playerRef.getUuid(), entityRef, store);
    }

    private boolean isEnabled() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isAchievementModuleEnabled();
    }

    @Nonnull
    private AchievementModSettings getSettings() {
        RpgModConfig config = resolveConfig();
        return config == null ? new AchievementModSettings() : config.getAchievementSettings();
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        DefaultAssetMap<String, RpgModConfig> assetMap = RpgModConfig.getAssetMap();
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
        return assetMap.getAssetMap().isEmpty() ? null : assetMap.getAssetMap().values().iterator().next();
    }

    private record Snapshot(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nullable LevelProgressionComponent levelProgression,
            @Nullable ClassComponent classComponent,
            @Nullable RaceComponent raceComponent,
            @Nullable ClassAbilityComponent abilityComponent) {

        private static Snapshot capture(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> entityRef) {
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                throw new IllegalStateException("Achievement snapshot requires PlayerRef");
            }
            return new Snapshot(
                    entityRef,
                    store,
                    playerRef,
                    store.getComponent(entityRef, LevelProgressionComponent.getComponentType()),
                    store.getComponent(entityRef, ClassComponent.getComponentType()),
                    store.getComponent(entityRef, RaceComponent.getComponentType()),
                    store.getComponent(entityRef, ClassAbilityComponent.getComponentType()));
        }
    }

    private record ProgressUpdate(
            @Nonnull String criterionId,
            int currentValue,
            int targetValue) {
    }
}
