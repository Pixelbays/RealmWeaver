package org.pixelbays.rpg.lockpicking.component;

import java.util.UUID;

import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({"PMD", "CloneMethodMustDeclareCloneNotSupportedException", "CloneDoesntDeclareCloneNotSupportedException",
    "CloneDoesntCallSuperClone", "all", "clone", "null"})
public class LockpickingSessionComponent implements Component<EntityStore> {

    public static final BuilderCodec<LockpickingSessionComponent> CODEC = BuilderCodec
            .builder(LockpickingSessionComponent.class, LockpickingSessionComponent::new)
            .append(new KeyedCodec<>("Active", Codec.BOOLEAN), (i, v) -> i.active = v, i -> i.active)
            .add()
            .append(new KeyedCodec<>("DifficultyTier", Codec.STRING), (i, v) -> i.difficultyTierId = v,
                    i -> i.difficultyTierId)
            .add()
            .append(new KeyedCodec<>("PinCount", Codec.INTEGER), (i, v) -> i.pinCount = v, i -> i.pinCount)
            .add()
            .append(new KeyedCodec<>("CurrentPin", Codec.INTEGER), (i, v) -> i.currentPin = v, i -> i.currentPin)
            .add()
            .append(new KeyedCodec<>("TimeRemainingSeconds", Codec.FLOAT), (i, v) -> i.timeRemainingSeconds = v,
                    i -> i.timeRemainingSeconds)
            .add()
            .append(new KeyedCodec<>("TotalTimeLimitSeconds", Codec.FLOAT), (i, v) -> i.totalTimeLimitSeconds = v,
                    i -> i.totalTimeLimitSeconds)
            .add()
            .append(new KeyedCodec<>("NeedlePosition", Codec.FLOAT), (i, v) -> i.needlePosition = v,
                    i -> i.needlePosition)
            .add()
            .append(new KeyedCodec<>("NeedleDirection", Codec.FLOAT), (i, v) -> i.needleDirection = v,
                    i -> i.needleDirection)
            .add()
            .append(new KeyedCodec<>("NeedleSpeed", Codec.FLOAT), (i, v) -> i.needleSpeed = v, i -> i.needleSpeed)
            .add()
            .append(new KeyedCodec<>("SweetSpotCenter", Codec.FLOAT), (i, v) -> i.sweetSpotCenter = v,
                    i -> i.sweetSpotCenter)
            .add()
            .append(new KeyedCodec<>("SweetSpotSize", Codec.FLOAT), (i, v) -> i.sweetSpotSize = v,
                    i -> i.sweetSpotSize)
            .add()
            .append(new KeyedCodec<>("BaseSweetSpotSize", Codec.FLOAT), (i, v) -> i.baseSweetSpotSize = v,
                    i -> i.baseSweetSpotSize)
            .add()
            .append(new KeyedCodec<>("BaseNeedleSpeed", Codec.FLOAT), (i, v) -> i.baseNeedleSpeed = v,
                    i -> i.baseNeedleSpeed)
            .add()
            .append(new KeyedCodec<>("SweetSpotSizeScale", Codec.FLOAT), (i, v) -> i.sweetSpotSizeScale = v,
                    i -> i.sweetSpotSizeScale)
            .add()
            .append(new KeyedCodec<>("NeedleSpeedScale", Codec.FLOAT), (i, v) -> i.needleSpeedScale = v,
                    i -> i.needleSpeedScale)
            .add()
            .append(new KeyedCodec<>("MaxMistakes", Codec.INTEGER), (i, v) -> i.maxMistakes = v, i -> i.maxMistakes)
            .add()
            .append(new KeyedCodec<>("Mistakes", Codec.INTEGER), (i, v) -> i.mistakes = v, i -> i.mistakes)
            .add()
            .append(new KeyedCodec<>("SuccessInteraction", Codec.STRING), (i, v) -> i.successInteractionId = v,
                    i -> i.successInteractionId)
            .add()
            .append(new KeyedCodec<>("FailureInteraction", Codec.STRING), (i, v) -> i.failureInteractionId = v,
                    i -> i.failureInteractionId)
            .add()
            .append(new KeyedCodec<>("InteractionType", new EnumCodec<>(InteractionType.class)),
                    (i, v) -> i.interactionType = v, i -> i.interactionType)
            .add()
            .append(new KeyedCodec<>("TargetEntityUuid", Codec.UUID_STRING), (i, v) -> i.targetEntityUuid = v,
                    i -> i.targetEntityUuid)
            .add()
            .append(new KeyedCodec<>("HasTargetBlock", Codec.BOOLEAN), (i, v) -> i.hasTargetBlock = v,
                    i -> i.hasTargetBlock)
            .add()
            .append(new KeyedCodec<>("TargetBlockX", Codec.INTEGER), (i, v) -> i.targetBlockX = v,
                    i -> i.targetBlockX)
            .add()
            .append(new KeyedCodec<>("TargetBlockY", Codec.INTEGER), (i, v) -> i.targetBlockY = v,
                    i -> i.targetBlockY)
            .add()
            .append(new KeyedCodec<>("TargetBlockZ", Codec.INTEGER), (i, v) -> i.targetBlockZ = v,
                    i -> i.targetBlockZ)
            .add()
            .append(new KeyedCodec<>("TargetSlot", Codec.INTEGER), (i, v) -> i.targetSlot = v, i -> i.targetSlot)
            .add()
            .append(new KeyedCodec<>("HitDetail", Codec.STRING), (i, v) -> i.hitDetail = v, i -> i.hitDetail)
            .add()
            .append(new KeyedCodec<>("HasHitLocation", Codec.BOOLEAN), (i, v) -> i.hasHitLocation = v,
                    i -> i.hasHitLocation)
            .add()
            .append(new KeyedCodec<>("HitLocationX", Codec.FLOAT), (i, v) -> i.hitLocationX = v,
                    i -> i.hitLocationX)
            .add()
            .append(new KeyedCodec<>("HitLocationY", Codec.FLOAT), (i, v) -> i.hitLocationY = v,
                    i -> i.hitLocationY)
            .add()
            .append(new KeyedCodec<>("HitLocationZ", Codec.FLOAT), (i, v) -> i.hitLocationZ = v,
                    i -> i.hitLocationZ)
            .add()
            .append(new KeyedCodec<>("HitLocationW", Codec.FLOAT), (i, v) -> i.hitLocationW = v,
                    i -> i.hitLocationW)
            .add()
            .build();

    private boolean active;
    private String difficultyTierId;
    private int pinCount;
    private int currentPin;
    private float timeRemainingSeconds;
    private float totalTimeLimitSeconds;
    private float needlePosition;
    private float needleDirection;
    private float needleSpeed;
    private float sweetSpotCenter;
    private float sweetSpotSize;
    private float baseSweetSpotSize;
    private float baseNeedleSpeed;
    private float sweetSpotSizeScale;
    private float needleSpeedScale;
    private int maxMistakes;
    private int mistakes;
    private String successInteractionId;
    private String failureInteractionId;
    private InteractionType interactionType;
    private UUID targetEntityUuid;
    private boolean hasTargetBlock;
    private int targetBlockX;
    private int targetBlockY;
    private int targetBlockZ;
    private int targetSlot;
    private String hitDetail;
    private boolean hasHitLocation;
    private float hitLocationX;
    private float hitLocationY;
    private float hitLocationZ;
    private float hitLocationW;
    // Transient (not persisted): count of lockpicks in inventory, updated at session start and on consume
    private int lockpickCount;

    public LockpickingSessionComponent() {
        this.active = false;
        this.difficultyTierId = "";
        this.pinCount = 0;
        this.currentPin = 0;
        this.timeRemainingSeconds = 0f;
        this.totalTimeLimitSeconds = 0f;
        this.needlePosition = 0f;
        this.needleDirection = 1f;
        this.needleSpeed = 0f;
        this.sweetSpotCenter = 0f;
        this.sweetSpotSize = 0f;
        this.baseSweetSpotSize = 0f;
        this.baseNeedleSpeed = 0f;
        this.sweetSpotSizeScale = 1f;
        this.needleSpeedScale = 1f;
        this.maxMistakes = 0;
        this.mistakes = 0;
        this.successInteractionId = "";
        this.failureInteractionId = "";
        this.interactionType = InteractionType.Primary;
        this.targetEntityUuid = null;
        this.hasTargetBlock = false;
        this.targetBlockX = 0;
        this.targetBlockY = 0;
        this.targetBlockZ = 0;
        this.targetSlot = 0;
        this.hitDetail = "";
        this.hasHitLocation = false;
        this.hitLocationX = 0f;
        this.hitLocationY = 0f;
        this.hitLocationZ = 0f;
        this.hitLocationW = 0f;
        this.lockpickCount = 0;
    }

    public static ComponentType<EntityStore, LockpickingSessionComponent> getComponentType() {
        return Realmweavers.get().getLockpickingSessionComponentType();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDifficultyTierId() {
        return difficultyTierId;
    }

    public void setDifficultyTierId(String difficultyTierId) {
        this.difficultyTierId = difficultyTierId;
    }

    public int getPinCount() {
        return pinCount;
    }

    public void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }

    public int getCurrentPin() {
        return currentPin;
    }

    public void setCurrentPin(int currentPin) {
        this.currentPin = currentPin;
    }

    public float getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(float timeRemainingSeconds) {
        this.timeRemainingSeconds = timeRemainingSeconds;
    }

    public float getTotalTimeLimitSeconds() {
        return totalTimeLimitSeconds;
    }

    public void setTotalTimeLimitSeconds(float totalTimeLimitSeconds) {
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
    }

    public float getNeedlePosition() {
        return needlePosition;
    }

    public void setNeedlePosition(float needlePosition) {
        this.needlePosition = needlePosition;
    }

    public float getNeedleDirection() {
        return needleDirection;
    }

    public void setNeedleDirection(float needleDirection) {
        this.needleDirection = needleDirection;
    }

    public float getNeedleSpeed() {
        return needleSpeed;
    }

    public void setNeedleSpeed(float needleSpeed) {
        this.needleSpeed = needleSpeed;
    }

    public float getSweetSpotCenter() {
        return sweetSpotCenter;
    }

    public void setSweetSpotCenter(float sweetSpotCenter) {
        this.sweetSpotCenter = sweetSpotCenter;
    }

    public float getSweetSpotSize() {
        return sweetSpotSize;
    }

    public void setSweetSpotSize(float sweetSpotSize) {
        this.sweetSpotSize = sweetSpotSize;
    }

    public float getBaseSweetSpotSize() {
        return baseSweetSpotSize;
    }

    public void setBaseSweetSpotSize(float baseSweetSpotSize) {
        this.baseSweetSpotSize = baseSweetSpotSize;
    }

    public float getBaseNeedleSpeed() {
        return baseNeedleSpeed;
    }

    public void setBaseNeedleSpeed(float baseNeedleSpeed) {
        this.baseNeedleSpeed = baseNeedleSpeed;
    }

    public float getSweetSpotSizeScale() {
        return sweetSpotSizeScale;
    }

    public void setSweetSpotSizeScale(float sweetSpotSizeScale) {
        this.sweetSpotSizeScale = sweetSpotSizeScale;
    }

    public float getNeedleSpeedScale() {
        return needleSpeedScale;
    }

    public void setNeedleSpeedScale(float needleSpeedScale) {
        this.needleSpeedScale = needleSpeedScale;
    }

    public int getMaxMistakes() {
        return maxMistakes;
    }

    public void setMaxMistakes(int maxMistakes) {
        this.maxMistakes = maxMistakes;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public String getSuccessInteractionId() {
        return successInteractionId;
    }

    public void setSuccessInteractionId(String successInteractionId) {
        this.successInteractionId = successInteractionId;
    }

    public String getFailureInteractionId() {
        return failureInteractionId;
    }

    public void setFailureInteractionId(String failureInteractionId) {
        this.failureInteractionId = failureInteractionId;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public UUID getTargetEntityUuid() {
        return targetEntityUuid;
    }

    public void setTargetEntityUuid(UUID targetEntityUuid) {
        this.targetEntityUuid = targetEntityUuid;
    }

    public boolean hasTargetBlock() {
        return hasTargetBlock;
    }

    public void setHasTargetBlock(boolean hasTargetBlock) {
        this.hasTargetBlock = hasTargetBlock;
    }

    public int getTargetBlockX() {
        return targetBlockX;
    }

    public void setTargetBlockX(int targetBlockX) {
        this.targetBlockX = targetBlockX;
    }

    public int getTargetBlockY() {
        return targetBlockY;
    }

    public void setTargetBlockY(int targetBlockY) {
        this.targetBlockY = targetBlockY;
    }

    public int getTargetBlockZ() {
        return targetBlockZ;
    }

    public void setTargetBlockZ(int targetBlockZ) {
        this.targetBlockZ = targetBlockZ;
    }

    public int getTargetSlot() {
        return targetSlot;
    }

    public void setTargetSlot(int targetSlot) {
        this.targetSlot = targetSlot;
    }

    public String getHitDetail() {
        return hitDetail;
    }

    public void setHitDetail(String hitDetail) {
        this.hitDetail = hitDetail;
    }

    public boolean hasHitLocation() {
        return hasHitLocation;
    }

    public void setHasHitLocation(boolean hasHitLocation) {
        this.hasHitLocation = hasHitLocation;
    }

    public float getHitLocationX() {
        return hitLocationX;
    }

    public void setHitLocationX(float hitLocationX) {
        this.hitLocationX = hitLocationX;
    }

    public float getHitLocationY() {
        return hitLocationY;
    }

    public void setHitLocationY(float hitLocationY) {
        this.hitLocationY = hitLocationY;
    }

    public float getHitLocationZ() {
        return hitLocationZ;
    }

    public void setHitLocationZ(float hitLocationZ) {
        this.hitLocationZ = hitLocationZ;
    }

    public float getHitLocationW() {
        return hitLocationW;
    }

    public void setHitLocationW(float hitLocationW) {
        this.hitLocationW = hitLocationW;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        LockpickingSessionComponent cloned = new LockpickingSessionComponent();
        cloned.active = this.active;
        cloned.difficultyTierId = this.difficultyTierId;
        cloned.pinCount = this.pinCount;
        cloned.currentPin = this.currentPin;
        cloned.timeRemainingSeconds = this.timeRemainingSeconds;
        cloned.totalTimeLimitSeconds = this.totalTimeLimitSeconds;
        cloned.needlePosition = this.needlePosition;
        cloned.needleDirection = this.needleDirection;
        cloned.needleSpeed = this.needleSpeed;
        cloned.sweetSpotCenter = this.sweetSpotCenter;
        cloned.sweetSpotSize = this.sweetSpotSize;
        cloned.baseSweetSpotSize = this.baseSweetSpotSize;
        cloned.baseNeedleSpeed = this.baseNeedleSpeed;
        cloned.sweetSpotSizeScale = this.sweetSpotSizeScale;
        cloned.needleSpeedScale = this.needleSpeedScale;
        cloned.maxMistakes = this.maxMistakes;
        cloned.mistakes = this.mistakes;
        cloned.successInteractionId = this.successInteractionId;
        cloned.failureInteractionId = this.failureInteractionId;
        cloned.interactionType = this.interactionType;
        cloned.targetEntityUuid = this.targetEntityUuid;
        cloned.hasTargetBlock = this.hasTargetBlock;
        cloned.targetBlockX = this.targetBlockX;
        cloned.targetBlockY = this.targetBlockY;
        cloned.targetBlockZ = this.targetBlockZ;
        cloned.targetSlot = this.targetSlot;
        cloned.hitDetail = this.hitDetail;
        cloned.hasHitLocation = this.hasHitLocation;
        cloned.hitLocationX = this.hitLocationX;
        cloned.hitLocationY = this.hitLocationY;
        cloned.hitLocationZ = this.hitLocationZ;
        cloned.hitLocationW = this.hitLocationW;
        cloned.lockpickCount = this.lockpickCount;
        return cloned;
    }

    public int getLockpickCount() {
        return lockpickCount;
    }

    public void setLockpickCount(int lockpickCount) {
        this.lockpickCount = lockpickCount;
    }
}
