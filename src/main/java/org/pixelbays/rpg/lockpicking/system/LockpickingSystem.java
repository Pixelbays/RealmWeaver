package org.pixelbays.rpg.lockpicking.system;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.lockpicking.config.settings.LockpickingModSettings.LockpickingDifficultyTier;
import org.pixelbays.rpg.lockpicking.component.LockpickingSessionComponent;
import org.pixelbays.rpg.lockpicking.ui.LockpickingPage;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

@SuppressWarnings("null")
public class LockpickingSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, LockpickingSessionComponent> sessionComponentType;
    private final Map<DoorUnlockKey, ActiveDoorUnlock> activeDoorUnlocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService unlockScheduler = Executors.newSingleThreadScheduledExecutor(new DoorUnlockThreadFactory());
    private ComponentType<EntityStore, Player> playerComponentType = Player.getComponentType();

    private ComponentType<EntityStore, Player> resolvePlayerType() {
        if (playerComponentType == null) {
            playerComponentType = Player.getComponentType();
        }
        return playerComponentType;
    }
    private final Query<EntityStore> query;

    public LockpickingSystem(@Nonnull ComponentType<EntityStore, LockpickingSessionComponent> sessionComponentType) {
        this.sessionComponentType = sessionComponentType;
        if (playerComponentType == null) {
            this.query = Query.and(sessionComponentType);
        } else {
            this.query = Query.and(sessionComponentType, playerComponentType);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public java.util.Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.firstSet();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LockpickingSessionComponent session = chunk.getComponent(index, sessionComponentType);
        if (!session.isActive()) {
            return;
        }

        session.setTimeRemainingSeconds(session.getTimeRemainingSeconds() - dt);
        if (session.getTimeRemainingSeconds() <= 0f) {
            endSession(chunk.getReferenceTo(index), store, session, false, "pixelbays.rpg.lockpicking.failed");
            return;
        }

        updateNeedle(session, dt);
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) {
            return;
        }

        LockpickingPage.sendUpdate(ref, store, session);
    }

    public void startSession(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractionType interactionType,
            @Nonnull String difficultyTierId,
            @Nullable String successInteractionId,
            @Nullable String failureInteractionId,
            @Nullable Ref<EntityStore> targetEntity,
            @Nullable BlockPosition targetBlock,
            @Nullable Vector4d hitLocation,
            @Nullable String hitDetail,
            @Nullable Integer targetSlot) {
        LockpickingSessionComponent session = store.getComponent(ref, sessionComponentType);
        if (session == null) {
            session = commandBuffer.addComponent(ref, sessionComponentType);
            if (session == null) {
                RpgLogging.warn("Lockpicking: failed to add session component");
                return;
            }
        }

        if (session.isActive()) {
            RpgLogging.debug("Lockpicking: session already active");
            return;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            RpgLogging.warn("Lockpicking: missing RpgModConfig asset");
            return;
        }

        LockpickingDifficultyTier tier = resolveTier(config, difficultyTierId);
        if (tier == null) {
            RpgLogging.warn("Lockpicking: invalid difficulty tier '%s'", difficultyTierId);
            sendMessage(ref, store, Message.translation("pixelbays.rpg.lockpicking.invalidTier"));
            return;
        }

        session.setActive(true);
        session.setDifficultyTierId(difficultyTierId);
        session.setPinCount(tier.getPinCount());
        session.setCurrentPin(0);
        session.setTimeRemainingSeconds(tier.getTimeLimitSeconds());
        session.setTotalTimeLimitSeconds(tier.getTimeLimitSeconds());
        session.setBaseSweetSpotSize(tier.getSweetSpotSize());
        session.setBaseNeedleSpeed(tier.getNeedleSpeed());
        session.setSweetSpotSizeScale(tier.getSweetSpotSizeScale());
        session.setNeedleSpeedScale(tier.getNeedleSpeedScale());
        session.setMaxMistakes(tier.getMaxMistakes());
        session.setMistakes(0);
        session.setSuccessInteractionId(successInteractionId == null ? "" : successInteractionId);
        session.setFailureInteractionId(failureInteractionId == null ? "" : failureInteractionId);
        session.setInteractionType(interactionType);
        captureTargetData(store, session, targetEntity, targetBlock, hitLocation, hitDetail, targetSlot);
        resetForPin(session, 0);

        ComponentType<EntityStore, Player> resolvedPlayerType = resolvePlayerType();
        if (resolvedPlayerType == null) {
            RpgLogging.warn("Lockpicking: Player component type not yet registered");
            return;
        }
        Player player = commandBuffer.getComponent(ref, resolvedPlayerType);
        if (player == null) {
            RpgLogging.debug("Lockpicking: missing player component");
            return;
        }

        // Count lockpicks so the UI can display them from the start
        String lockpickTag = config.getLockpickItemTag();
        if (lockpickTag != null && !lockpickTag.isEmpty()) {
            int tagIndex = AssetRegistry.getTagIndex(lockpickTag);
            if (tagIndex != Integer.MIN_VALUE) {
                session.setLockpickCount(countLockpicksInInventory(player.getInventory(), tagIndex));
            }
        }

        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            RpgLogging.debug("Lockpicking: missing player ref component");
            return;
        }

        RpgLogging.debug("Lockpicking: opening UI (tier=%s pins=%s time=%.1f)",
                difficultyTierId, session.getPinCount(), session.getTimeRemainingSeconds());
        player.getPageManager().openCustomPage(ref, store, new LockpickingPage(playerRef));
        LockpickingPage.sendUpdate(ref, store, session);
    }

    public void handlePinAttempt(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        LockpickingSessionComponent session = store.getComponent(ref, sessionComponentType);
        if (session == null || !session.isActive()) {
            RpgLogging.debug("Lockpicking: pin attempt without active session");
            return;
        }

        float delta = Math.abs(session.getNeedlePosition() - session.getSweetSpotCenter());
        boolean success = delta <= (session.getSweetSpotSize() * 0.5f);
        if (success) {
            int nextPin = session.getCurrentPin() + 1;
            session.setCurrentPin(nextPin);
            if (nextPin >= session.getPinCount()) {
                RpgLogging.debug("Lockpicking: success");
                endSession(ref, store, session, true, "pixelbays.rpg.lockpicking.success");
                return;
            }

            resetForPin(session, nextPin);
        } else {
            session.setMistakes(session.getMistakes() + 1);
            if (session.getMistakes() >= session.getMaxMistakes()) {
                // Lockpick breaks — consume one from inventory
                RpgModConfig failConfig = resolveConfig();
                if (failConfig != null) {
                    String failTag = failConfig.getLockpickItemTag();
                    if (failTag != null && !failTag.isEmpty()) {
                        int tagIdx = AssetRegistry.getTagIndex(failTag);
                        ComponentType<EntityStore, Player> playerType = resolvePlayerType();
                        Player player = playerType != null ? store.getComponent(ref, playerType) : null;
                        if (player != null && tagIdx != Integer.MIN_VALUE) {
                            removeLockpickFromInventory(player.getInventory(), tagIdx);
                            session.setLockpickCount(Math.max(0, session.getLockpickCount() - 1));
                        }
                    }
                }
                RpgLogging.debug("Lockpicking: failed (mistakes=%s/%s)",
                        session.getMistakes(), session.getMaxMistakes());
                endSession(ref, store, session, false, "pixelbays.rpg.lockpicking.failed");
                return;
            }
        }

        LockpickingPage.sendUpdate(ref, store, session);
    }

    public void cancelSession(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        LockpickingSessionComponent session = store.getComponent(ref, sessionComponentType);
        if (session == null || !session.isActive()) {
            RpgLogging.debug("Lockpicking: cancel without active session");
            return;
        }

        RpgLogging.debug("Lockpicking: cancelled");
        endSession(ref, store, session, false, "pixelbays.rpg.lockpicking.failed");
    }

    private void endSession(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull LockpickingSessionComponent session,
            boolean success,
            @Nullable String messageKey) {
        session.setActive(false);
        ComponentType<EntityStore, Player> resolvedPlayerType = resolvePlayerType();
        Player player = resolvedPlayerType != null ? store.getComponent(ref, resolvedPlayerType) : null;
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }

        if (messageKey != null) {
            sendMessage(ref, store, Message.translation(messageKey));
        }

        if (success) {
            registerTemporaryUnlock(store, session.getDifficultyTierId(), getTargetBlock(session));
            executeInteraction(ref, store, session.getSuccessInteractionId(), session);
        } else {
            executeInteraction(ref, store, session.getFailureInteractionId(), session);
        }
    }

    public boolean isDoorTemporarilyUnlocked(@Nonnull Store<EntityStore> store, @Nullable BlockPosition targetBlock) {
        DoorUnlockKey key = createDoorUnlockKey(store, targetBlock);
        if (key == null) {
            return false;
        }

        ActiveDoorUnlock unlock = activeDoorUnlocks.get(key);
        if (unlock == null) {
            return false;
        }

        if (unlock.expiresAtMillis <= System.currentTimeMillis()) {
            activeDoorUnlocks.remove(key, unlock);
            return false;
        }

        return true;
    }

    public void registerTemporaryUnlock(@Nonnull Store<EntityStore> store,
            @Nonnull String difficultyTierId,
            @Nullable BlockPosition targetBlock) {
        DoorUnlockKey key = createDoorUnlockKey(store, targetBlock);
        if (key == null) {
            return;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            return;
        }

        LockpickingDifficultyTier tier = resolveTier(config, difficultyTierId);
        if (tier == null) {
            return;
        }

        int unlockSeconds = Math.max(0, tier.getDoorUnlockTime());
        if (unlockSeconds <= 0) {
            return;
        }

        World world = store.getExternalData().getWorld();

        Vector3i blockPosition = toVector(targetBlock);
        DoorPhysicalState originalState = getDoorState(world, blockPosition);
        if (originalState == null) {
            return;
        }

        ActiveDoorUnlock unlock = new ActiveDoorUnlock(
                key,
                world,
                blockPosition,
                originalState,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(unlockSeconds));
        ActiveDoorUnlock existing = activeDoorUnlocks.putIfAbsent(key, unlock);
        if (existing != null) {
            return;
        }

        unlock.relockTask = unlockScheduler.schedule(() -> relockDoor(unlock), unlockSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        for (ActiveDoorUnlock unlock : activeDoorUnlocks.values()) {
            if (unlock.relockTask != null) {
                unlock.relockTask.cancel(false);
            }
        }
        activeDoorUnlocks.clear();
        unlockScheduler.shutdownNow();
    }

    private void executeInteraction(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nullable String interactionId,
            @Nonnull LockpickingSessionComponent session) {
        if (interactionId == null || interactionId.isEmpty()) {
            RpgLogging.debug("Lockpicking: no follow-up interaction configured");
            return;
        }

        RootInteraction root = RootInteraction.getAssetMap().getAsset(interactionId);
        if (root == null) {
            RpgLogging.warn("Lockpicking: missing interaction asset '%s'", interactionId);
            return;
        }

        InteractionManager manager = store.getComponent(ref, InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            RpgLogging.warn("Lockpicking: missing interaction manager");
            return;
        }

        InteractionContext context = InteractionContext.forInteraction(manager, ref, session.getInteractionType(), store);
        applyTargetData(store, session, context);
        InteractionChain chain = manager.initChain(session.getInteractionType(), context, root, false);
        manager.queueExecuteChain(chain);
    }

    private void updateNeedle(@Nonnull LockpickingSessionComponent session, float dt) {
        float position = session.getNeedlePosition() + (session.getNeedleDirection() * session.getNeedleSpeed() * dt);
        if (position >= 1f) {
            position = 1f;
            session.setNeedleDirection(-1f);
        } else if (position <= 0f) {
            position = 0f;
            session.setNeedleDirection(1f);
        }

        session.setNeedlePosition(position);
    }

    private void resetForPin(@Nonnull LockpickingSessionComponent session, int pinIndex) {
        float size = session.getBaseSweetSpotSize() * (float) Math.pow(session.getSweetSpotSizeScale(), pinIndex);
        float speed = session.getBaseNeedleSpeed() * (float) Math.pow(session.getNeedleSpeedScale(), pinIndex);
        session.setSweetSpotSize(size);
        session.setNeedleSpeed(speed);
        session.setNeedlePosition(0f);
        session.setNeedleDirection(ThreadLocalRandom.current().nextBoolean() ? 1f : -1f);
        session.setSweetSpotCenter(ThreadLocalRandom.current().nextFloat());
    }

    private void captureTargetData(@Nonnull Store<EntityStore> store,
            @Nonnull LockpickingSessionComponent session,
            @Nullable Ref<EntityStore> targetEntity,
            @Nullable BlockPosition targetBlock,
            @Nullable Vector4d hitLocation,
            @Nullable String hitDetail,
            @Nullable Integer targetSlot) {
        if (targetEntity != null && targetEntity.isValid()) {
            UUIDComponent uuidComponent = store.getComponent(targetEntity, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                session.setTargetEntityUuid(uuidComponent.getUuid());
            }
        }

        if (targetBlock != null) {
            session.setHasTargetBlock(true);
            session.setTargetBlockX(targetBlock.x);
            session.setTargetBlockY(targetBlock.y);
            session.setTargetBlockZ(targetBlock.z);
        } else {
            session.setHasTargetBlock(false);
        }

        session.setTargetSlot(targetSlot != null ? targetSlot : 0);
        session.setHitDetail(hitDetail == null ? "" : hitDetail);
        if (hitLocation != null) {
            session.setHasHitLocation(true);
            session.setHitLocationX((float) hitLocation.x);
            session.setHitLocationY((float) hitLocation.y);
            session.setHitLocationZ((float) hitLocation.z);
            session.setHitLocationW((float) hitLocation.w);
        } else {
            session.setHasHitLocation(false);
        }
    }

    @Nullable
    private BlockPosition getTargetBlock(@Nonnull LockpickingSessionComponent session) {
        if (!session.hasTargetBlock()) {
            return null;
        }
        return new BlockPosition(session.getTargetBlockX(), session.getTargetBlockY(), session.getTargetBlockZ());
    }

    @Nullable
    private DoorUnlockKey createDoorUnlockKey(@Nonnull Store<EntityStore> store, @Nullable BlockPosition targetBlock) {
        if (targetBlock == null) {
            return null;
        }
        World world = store.getExternalData().getWorld();
        return new DoorUnlockKey(world.getName(), targetBlock.x, targetBlock.y, targetBlock.z);
    }

    private void relockDoor(@Nonnull ActiveDoorUnlock unlock) {
        if (!unlock.world.isAlive()) {
            activeDoorUnlocks.remove(unlock.key, unlock);
            return;
        }

        try {
            unlock.world.execute(() -> relockDoorOnWorldThread(unlock));
        } catch (RuntimeException ignored) {
            activeDoorUnlocks.remove(unlock.key, unlock);
        }
    }

    private void relockDoorOnWorldThread(@Nonnull ActiveDoorUnlock unlock) {
        if (!activeDoorUnlocks.remove(unlock.key, unlock)) {
            return;
        }

        DoorPhysicalState currentState = getDoorState(unlock.world, unlock.blockPosition);
        if (currentState == null || currentState == unlock.originalState) {
            return;
        }

        String interactionState = toInteractionState(currentState, unlock.originalState);
        if (interactionState == null) {
            return;
        }

        applyDoorInteractionState(unlock.world, unlock.blockPosition, interactionState);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static DoorPhysicalState getDoorState(@Nonnull World world, @Nonnull Vector3i blockPosition) {
        BlockType blockType = world.getBlockType(blockPosition);
        if (blockType == null || !blockType.isDoor()) {
            return null;
        }
        return DoorPhysicalState.fromBlockState(blockType.getStateForBlock(blockType));
    }

    @Nonnull
    private static Vector3i toVector(@Nonnull BlockPosition blockPosition) {
        return new Vector3i(blockPosition.x, blockPosition.y, blockPosition.z);
    }

    @Nullable
    private static String toInteractionState(@Nonnull DoorPhysicalState fromState, @Nonnull DoorPhysicalState toState) {
        if (fromState == toState) {
            return null;
        }
        if (toState == DoorPhysicalState.CLOSED) {
            return fromState == DoorPhysicalState.OPENED_IN ? "CloseDoorOut" : "CloseDoorIn";
        }
        return toState == DoorPhysicalState.OPENED_IN ? "OpenDoorOut" : "OpenDoorIn";
    }

    private static void applyDoorInteractionState(@Nonnull World world,
            @Nonnull Vector3i blockPosition,
            @Nonnull String interactionState) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z));
        if (chunk == null) {
            return;
        }

        BlockType blockType = chunk.getBlockType(blockPosition);
        if (blockType == null) {
            return;
        }

        int rotationIndex = getRotationIndex(chunk, blockPosition);
        BlockBoundingBoxes oldHitbox = BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
        world.setBlockInteractionState(blockPosition, blockType, interactionState);

        BlockType currentBlockType = world.getBlockType(blockPosition);
        if (currentBlockType == null) {
            return;
        }

        BlockType newBlockType = currentBlockType.getBlockForState(interactionState);
        if (oldHitbox != null) {
            FillerBlockUtil.forEachFillerBlock(oldHitbox.get(rotationIndex),
                    (x, y, z) -> world.performBlockUpdate(blockPosition.x + x, blockPosition.y + y, blockPosition.z + z));
        }

        if (newBlockType == null) {
            return;
        }

        BlockBoundingBoxes newHitbox = BlockBoundingBoxes.getAssetMap().getAsset(newBlockType.getHitboxTypeIndex());
        if (newHitbox == null || newHitbox == oldHitbox) {
            return;
        }

        FillerBlockUtil.forEachFillerBlock(newHitbox.get(rotationIndex),
                (x, y, z) -> world.performBlockUpdate(blockPosition.x + x, blockPosition.y + y, blockPosition.z + z));
    }

    @SuppressWarnings("removal")
    private static int getRotationIndex(@Nonnull WorldChunk chunk, @Nonnull Vector3i blockPosition) {
        return chunk.getRotationIndex(blockPosition.x, blockPosition.y, blockPosition.z);
    }

    private enum DoorPhysicalState {
        CLOSED,
        OPENED_IN,
        OPENED_OUT;

        @Nonnull
        private static DoorPhysicalState fromBlockState(@Nullable String state) {
            if (state == null) {
                return CLOSED;
            }

            return switch (state) {
                case "OpenDoorOut" -> OPENED_IN;
                case "OpenDoorIn" -> OPENED_OUT;
                default -> CLOSED;
            };
        }
    }

    private static final class DoorUnlockKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;

        private DoorUnlockKey(@Nonnull String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DoorUnlockKey other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z && worldName.equals(other.worldName);
        }

        @Override
        public int hashCode() {
            int result = worldName.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    private static final class ActiveDoorUnlock {
        private final DoorUnlockKey key;
        private final World world;
        private final Vector3i blockPosition;
        private final DoorPhysicalState originalState;
        private final long expiresAtMillis;
        private ScheduledFuture<?> relockTask;

        private ActiveDoorUnlock(@Nonnull DoorUnlockKey key,
                @Nonnull World world,
                @Nonnull Vector3i blockPosition,
                @Nonnull DoorPhysicalState originalState,
                long expiresAtMillis) {
            this.key = key;
            this.world = world;
            this.blockPosition = blockPosition;
            this.originalState = originalState;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static final class DoorUnlockThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@Nonnull Runnable runnable) {
            Thread thread = new Thread(runnable, "rpg-lockpicking-door-unlocks");
            thread.setDaemon(true);
            return thread;
        }
    }

    private void applyTargetData(@Nonnull Store<EntityStore> store,
            @Nonnull LockpickingSessionComponent session,
            @Nonnull InteractionContext context) {
        if (session.getTargetEntityUuid() != null) {
            Ref<EntityStore> targetRef = store.getExternalData().getRefFromUUID(session.getTargetEntityUuid());
            if (targetRef != null && targetRef.isValid()) {
                context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.TARGET_ENTITY, targetRef);
            }
        }

        if (session.hasTargetBlock()) {
            BlockPosition block = new BlockPosition(session.getTargetBlockX(), session.getTargetBlockY(), session.getTargetBlockZ());
            context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.TARGET_BLOCK, block);
            context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.TARGET_BLOCK_RAW, block);
        }

        context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.TARGET_SLOT,
                session.getTargetSlot());

        if (!session.getHitDetail().isEmpty()) {
            context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.HIT_DETAIL,
                    session.getHitDetail());
        }

        if (session.hasHitLocation()) {
            Vector4d hitLocation = new Vector4d(session.getHitLocationX(), session.getHitLocationY(), session.getHitLocationZ(),
                    session.getHitLocationW());
            context.getMetaStore().putMetaObject(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.HIT_LOCATION,
                    hitLocation);
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

    @Nullable
    private LockpickingDifficultyTier resolveTier(@Nonnull RpgModConfig config, @Nonnull String tierId) {
        Map<String, LockpickingDifficultyTier> tiers = config.getLockpickingDifficultyTiers();
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }

        LockpickingDifficultyTier tier = tiers.get(tierId);
        if (tier != null) {
            return tier;
        }

        if (tiers.size() == 1) {
            return tiers.values().iterator().next();
        }

        return null;
    }

    private void sendMessage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Message message) {
        ComponentType<EntityStore, Player> resolvedPlayerType = resolvePlayerType();
        if (resolvedPlayerType == null) {
            return;
        }

        Player player = store.getComponent(ref, resolvedPlayerType);
        if (player == null) {
            return;
        }

        player.sendMessage(message);
    }

    private static int countLockpicksInInventory(@Nonnull Inventory inventory, int tagIndex) {
        int count = 0;
        count += countTaggedInContainer(inventory.getHotbar(), tagIndex);
        count += countTaggedInContainer(inventory.getStorage(), tagIndex);
        count += countTaggedInContainer(inventory.getUtility(), tagIndex);
        count += countTaggedInContainer(inventory.getArmor(), tagIndex);
        count += countTaggedInContainer(inventory.getBackpack(), tagIndex);
        return count;
    }

    private static int countTaggedInContainer(@Nullable ItemContainer container, int tagIndex) {
        if (container == null) {
            return 0;
        }
        int count = 0;
        int capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || stack.getItem() == null || stack.getItem().getData() == null) {
                continue;
            }
            if (stack.getItem().getData().getExpandedTagIndexes().contains(tagIndex)) {
                count += stack.getQuantity();
            }
        }
        return count;
    }

    private static void removeLockpickFromInventory(@Nonnull Inventory inventory, int tagIndex) {
        if (removeTaggedFromContainer(inventory.getHotbar(), tagIndex)) return;
        if (removeTaggedFromContainer(inventory.getStorage(), tagIndex)) return;
        if (removeTaggedFromContainer(inventory.getUtility(), tagIndex)) return;
        if (removeTaggedFromContainer(inventory.getArmor(), tagIndex)) return;
        removeTaggedFromContainer(inventory.getBackpack(), tagIndex);
    }

    private static boolean removeTaggedFromContainer(@Nullable ItemContainer container, int tagIndex) {
        if (container == null) {
            return false;
        }
        int capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || stack.getItem() == null || stack.getItem().getData() == null) {
                continue;
            }
            if (!stack.getItem().getData().getExpandedTagIndexes().contains(tagIndex)) {
                continue;
            }
            int qty = stack.getQuantity();
            container.setItemStackForSlot(i, qty <= 1 ? ItemStack.EMPTY : stack.withQuantity(qty - 1));
            return true;
        }
        return false;
    }

    public static String formatPercent(float value) {
        return String.format(Locale.US, "%.0f", value * 100f);
    }

    @Nonnull
    public static LockpickingSystem get() {
        return ExamplePlugin.get().getLockpickingSystem();
    }
}
