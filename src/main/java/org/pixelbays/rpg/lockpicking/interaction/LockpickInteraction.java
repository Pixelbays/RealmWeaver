package org.pixelbays.rpg.lockpicking.interaction;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.builder.ClassRefCodec;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.lockpicking.system.LockpickingSystem;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class LockpickInteraction extends SimpleInstantInteraction {

    private static final Codec<String> CLASS_REF_CODEC = new ClassRefCodec();

    @Nonnull
    public static final BuilderCodec<LockpickInteraction> LOCKPICK_CODEC = BuilderCodec.builder(
            LockpickInteraction.class, LockpickInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Checks key/lockpick requirements and starts the lockpicking mini game.")
                .append(new KeyedCodec<>("KeyItemId", Codec.STRING, false), (i, v) -> i.keyItemId = v,
                    i -> i.keyItemId)
            .add()
                .append(new KeyedCodec<>("DifficultyTier", Codec.STRING, false), (i, v) -> i.difficultyTierId = v,
                    i -> i.difficultyTierId)
            .add()
                .append(new KeyedCodec<>("SuccessInteraction", RootInteraction.CHILD_ASSET_CODEC, false),
                    (i, v) -> i.successInteractionId = v, i -> i.successInteractionId)
            .add()
                .append(new KeyedCodec<>("FailureInteraction", RootInteraction.CHILD_ASSET_CODEC, false),
                    (i, v) -> i.failureInteractionId = v, i -> i.failureInteractionId)
            .add()
                .append(new KeyedCodec<>("ConsumeKey", Codec.BOOLEAN, false), (i, v) -> i.consumeKey = v,
                    i -> i.consumeKey)
            .add()
                .append(new KeyedCodec<>("RequiredClassIds", new ArrayCodec<>(CLASS_REF_CODEC, String[]::new), false),
                    (i, v) -> i.requiredClassIds = v, i -> i.requiredClassIds)
            .add()
                .append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false),
                    (i, v) -> i.requiredClassLevel = v, i -> i.requiredClassLevel)
            .add()
            .build();

    private String keyItemId = "";
    private String difficultyTierId = "";
    private String successInteractionId = "";
    private String failureInteractionId = "";
    private boolean consumeKey = false;
    private String[] requiredClassIds = new String[0];
    private int requiredClassLevel = 1;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            RpgLogging.debug("LockpickInteraction: missing command buffer for %s", type);
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        if (!entityRef.isValid()) {
            RpgLogging.debug("LockpickInteraction: invalid player ref for %s", type);
            return;
        }

        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            RpgLogging.debug("LockpickInteraction: missing player component for %s", type);
            return;
        }

        RpgModConfig config = resolveConfig();
        if (config == null) {
            RpgLogging.warn("LockpickInteraction: missing RpgModConfig asset");
            return;
        }

        if (!Realmweavers.get().isLockpickingModuleEnabled()) {
            RpgLogging.debug("LockpickInteraction: lockpicking module disabled");
            triggerFailure(context);
            return;
        }

        if (!meetsClassRequirements(commandBuffer, entityRef)) {
            RpgLogging.debug("LockpickInteraction: class requirements not met");
            triggerFailure(context);
            player.sendMessage(requiredClassLevel > 1
                ? Message.translation("pixelbays.rpg.lockpicking.requiresClassLevel")
                    .param("classes", describeRequiredClasses())
                    .param("level", Integer.toString(requiredClassLevel))
                : Message.translation("pixelbays.rpg.lockpicking.requiresClass")
                    .param("classes", describeRequiredClasses()));
            return;
        }

        if (LockpickingSystem.get().isDoorTemporarilyUnlocked(entityRef.getStore(), context.getTargetBlock())) {
            context.getState().state = InteractionState.Finished;
            RpgLogging.debug("LockpickInteraction: door already unlocked, bypassing mini game");
            triggerSuccess(context);
            return;
        }

        if (hasKey(player.getInventory())) {
            if (consumeKey) {
                consumeKey(player.getInventory());
            }

            LockpickingSystem.get().registerTemporaryUnlock(entityRef.getStore(), difficultyTierId, context.getTargetBlock());

            RpgLogging.debug("LockpickInteraction: unlocked with key %s (consume=%s)",
                    keyItemId, consumeKey);
            triggerSuccess(context);
            player.sendMessage(Message.translation("pixelbays.rpg.lockpicking.unlockedWithKey"));
            return;
        }

        String lockpickTag = config.getLockpickItemTag();
        if (lockpickTag == null || lockpickTag.isEmpty()) {
            RpgLogging.debug("LockpickInteraction: missing LockpickItemTag in config");
            triggerFailure(context);
            player.sendMessage(Message.translation("pixelbays.rpg.lockpicking.noLockpicks"));
            return;
        }

        int tagIndex = AssetRegistry.getTagIndex(lockpickTag);
        if (tagIndex == Integer.MIN_VALUE) {
            RpgLogging.warn("LockpickInteraction: unknown lockpick tag '%s'", lockpickTag);
            triggerFailure(context);
            player.sendMessage(Message.translation("pixelbays.rpg.lockpicking.noLockpicks"));
            return;
        }

        if (!hasLockpickWithTag(player.getInventory(), tagIndex)) {
            RpgLogging.debug("LockpickInteraction: no lockpicks found for tag '%s'", lockpickTag);
            triggerFailure(context);
            player.sendMessage(Message.translation("pixelbays.rpg.lockpicking.noLockpicks"));
            return;
        }

        context.getState().state = InteractionState.Finished;
        RpgLogging.debug("LockpickInteraction: start session tier=%s success=%s failure=%s", 
            difficultyTierId, successInteractionId, failureInteractionId);
        LockpickingSystem.get().startSession(
                entityRef,
                entityRef.getStore(),
            commandBuffer,
                type,
                difficultyTierId,
                successInteractionId,
                failureInteractionId,
                context.getTargetEntity(),
                context.getTargetBlock(),
                context.getMetaStore().getIfPresentMetaObject(
                        com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.HIT_LOCATION),
                context.getMetaStore().getIfPresentMetaObject(
                        com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.HIT_DETAIL),
                context.getMetaStore().getIfPresentMetaObject(
                        com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.TARGET_SLOT));
    }

    private boolean meetsClassRequirements(@Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> entityRef) {
        if (requiredClassIds == null || requiredClassIds.length == 0) {
            return true;
        }

        ClassComponent classComponent = commandBuffer.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComponent == null) {
            return false;
        }

        LevelProgressionComponent levelComponent = requiredClassLevel > 1
                ? commandBuffer.getComponent(entityRef, LevelProgressionComponent.getComponentType())
                : null;

        for (String classId : requiredClassIds) {
            if (classId == null || classId.isEmpty() || !classComponent.hasLearnedClass(classId)) {
                continue;
            }

            if (requiredClassLevel <= 1) {
                return true;
            }

            ClassDefinition classDefinition = ClassDefinition.getAssetMap().getAsset(classId);
            if (classDefinition == null) {
                continue;
            }

            String levelSystemId = classDefinition.usesCharacterLevel()
                    ? "Base_Character_Level"
                    : classDefinition.getLevelSystemId();
            if (levelSystemId == null || levelSystemId.isEmpty() || levelComponent == null) {
                continue;
            }

            LevelProgressionComponent.LevelSystemData levelSystemData = levelComponent.getSystem(levelSystemId);
            int currentLevel = levelSystemData != null ? levelSystemData.getCurrentLevel() : 0;
            if (currentLevel >= requiredClassLevel) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    private String describeRequiredClasses() {
        if (requiredClassIds == null || requiredClassIds.length == 0) {
            return "";
        }

        List<String> names = new ArrayList<>();
        for (String classId : requiredClassIds) {
            if (classId == null || classId.isEmpty()) {
                continue;
            }

            ClassDefinition definition = ClassDefinition.getAssetMap().getAsset(classId);
            if (definition != null && definition.getDisplayName() != null && !definition.getDisplayName().isEmpty()) {
                names.add(definition.getDisplayName());
            } else {
                names.add(classId);
            }
        }

        return names.isEmpty() ? "" : String.join(", ", names);
    }

    private boolean hasKey(@Nonnull Inventory inventory) {
        if (keyItemId == null || keyItemId.isEmpty()) {
            return false;
        }

        return hasItemId(inventory, keyItemId);
    }

    private void consumeKey(@Nonnull Inventory inventory) {
        if (keyItemId == null || keyItemId.isEmpty()) {
            return;
        }

        removeSingleItem(inventory, keyItemId);
    }

    private boolean hasLockpickWithTag(@Nonnull Inventory inventory, int tagIndex) {
        return hasItemWithTag(inventory, tagIndex);
    }

    private void triggerSuccess(@Nonnull InteractionContext context) {
        if (successInteractionId == null || successInteractionId.isEmpty()) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        RootInteraction root = RootInteraction.getAssetMap().getAsset(successInteractionId);
        if (root == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        context.getState().state = InteractionState.Finished;
        context.execute(root);
    }

    private void triggerFailure(@Nonnull InteractionContext context) {
        if (failureInteractionId == null || failureInteractionId.isEmpty()) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        RootInteraction root = RootInteraction.getAssetMap().getAsset(failureInteractionId);
        if (root == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        context.getState().state = InteractionState.Finished;
        context.execute(root);
    }

    private boolean hasItemId(@Nonnull Inventory inventory, @Nonnull String itemId) {
        return findItemInContainer(inventory.getHotbar(), itemId)
                || findItemInContainer(inventory.getStorage(), itemId)
                || findItemInContainer(inventory.getUtility(), itemId)
                || findItemInContainer(inventory.getArmor(), itemId)
                || findItemInContainer(inventory.getBackpack(), itemId);
    }

    private boolean hasItemWithTag(@Nonnull Inventory inventory, int tagIndex) {
        return findTaggedItem(inventory.getHotbar(), tagIndex)
                || findTaggedItem(inventory.getStorage(), tagIndex)
                || findTaggedItem(inventory.getUtility(), tagIndex)
                || findTaggedItem(inventory.getArmor(), tagIndex)
                || findTaggedItem(inventory.getBackpack(), tagIndex);
    }

    private boolean findItemInContainer(@Nullable ItemContainer container, @Nonnull String itemId) {
        if (container == null) {
            return false;
        }

        int capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && !stack.isEmpty() && itemId.equals(stack.getItemId())) {
                return true;
            }
        }

        return false;
    }

    private boolean findTaggedItem(@Nullable ItemContainer container, int tagIndex) {
        if (container == null) {
            return false;
        }

        int capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == null || stack.getItem().getData() == null) {
                continue;
            }

            if (stack.getItem().getData().getExpandedTagIndexes().contains(tagIndex)) {
                return true;
            }
        }

        return false;
    }

    private void removeSingleItem(@Nonnull Inventory inventory, @Nonnull String itemId) {
        if (removeSingleItemFromContainer(inventory.getHotbar(), itemId)) {
            return;
        }
        if (removeSingleItemFromContainer(inventory.getStorage(), itemId)) {
            return;
        }
        if (removeSingleItemFromContainer(inventory.getUtility(), itemId)) {
            return;
        }
        if (removeSingleItemFromContainer(inventory.getArmor(), itemId)) {
            return;
        }
        removeSingleItemFromContainer(inventory.getBackpack(), itemId);
    }

    private boolean removeSingleItemFromContainer(@Nullable ItemContainer container, @Nonnull String itemId) {
        if (container == null) {
            return false;
        }

        int capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || !itemId.equals(stack.getItemId())) {
                continue;
            }

            int quantity = stack.getQuantity();
            if (quantity <= 1) {
                container.setItemStackForSlot(i, ItemStack.EMPTY);
            } else {
                container.setItemStackForSlot(i, stack.withQuantity(quantity - 1));
            }

            return true;
        }

        return false;
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

    @Nonnull
    @Override
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }
}
