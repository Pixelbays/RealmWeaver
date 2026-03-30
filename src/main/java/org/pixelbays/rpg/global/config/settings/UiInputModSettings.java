package org.pixelbays.rpg.global.config.settings;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.InteractionType;

public class UiInputModSettings {

    public static final boolean ALLOW_SPELLBOOK_PAGE = true;
    public static final boolean ALLOW_RPG_INVENTORY_PAGE = true;
    public static final boolean ALLOW_PARTY_PAGE = true;
    public static final boolean ALLOW_GUILD_PAGE = true;
    public static final boolean ALLOW_MAIL_PAGE = true;
        public static final boolean ALLOW_BANK_PAGE = true;
        public static final boolean ALLOW_CURRENCY_PAGE = true;
        public static final boolean ALLOW_GROUP_FINDER_PAGE = true;

    public enum UiInputPageTarget {
        None,
        Spellbook,
        RpgInventory,
        Party,
        Guild,
        Mail,
        Bank,
        Currency,
        GroupFinder
    }

    public static final BuilderCodec<UiInputModSettings> CODEC = BuilderCodec
            .builder(UiInputModSettings.class, UiInputModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("UseTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.useTarget = s, i -> i.useTarget)
            .add()
            .append(new KeyedCodec<>("PickTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.pickTarget = s, i -> i.pickTarget)
            .add()
            .append(new KeyedCodec<>("PickupTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.pickupTarget = s, i -> i.pickupTarget)
            .add()
            .append(new KeyedCodec<>("CollisionEnterTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.collisionEnterTarget = s, i -> i.collisionEnterTarget)
            .add()
            .append(new KeyedCodec<>("CollisionLeaveTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.collisionLeaveTarget = s, i -> i.collisionLeaveTarget)
            .add()
            .append(new KeyedCodec<>("CollisionTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.collisionTarget = s, i -> i.collisionTarget)
            .add()
            .append(new KeyedCodec<>("EntityStatEffectTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.entityStatEffectTarget = s, i -> i.entityStatEffectTarget)
            .add()
            .append(new KeyedCodec<>("SwapToTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.swapToTarget = s, i -> i.swapToTarget)
            .add()
            .append(new KeyedCodec<>("SwapFromTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.swapFromTarget = s, i -> i.swapFromTarget)
            .add()
            .append(new KeyedCodec<>("DeathTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.deathTarget = s, i -> i.deathTarget)
            .add()
            .append(new KeyedCodec<>("WieldingTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.wieldingTarget = s, i -> i.wieldingTarget)
            .add()
            .append(new KeyedCodec<>("ProjectileSpawnTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.projectileSpawnTarget = s, i -> i.projectileSpawnTarget)
            .add()
            .append(new KeyedCodec<>("ProjectileHitTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.projectileHitTarget = s, i -> i.projectileHitTarget)
            .add()
            .append(new KeyedCodec<>("ProjectileMissTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.projectileMissTarget = s, i -> i.projectileMissTarget)
            .add()
            .append(new KeyedCodec<>("ProjectileBounceTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.projectileBounceTarget = s, i -> i.projectileBounceTarget)
            .add()
            .append(new KeyedCodec<>("HeldTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.heldTarget = s, i -> i.heldTarget)
            .add()
            .append(new KeyedCodec<>("HeldOffhandTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.heldOffhandTarget = s, i -> i.heldOffhandTarget)
            .add()
            .append(new KeyedCodec<>("EquippedTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.equippedTarget = s, i -> i.equippedTarget)
            .add()
            .append(new KeyedCodec<>("DodgeTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.dodgeTarget = s, i -> i.dodgeTarget)
            .add()
            .append(new KeyedCodec<>("GameModeSwapTarget", new EnumCodec<>(UiInputPageTarget.class), false, true),
                    (i, s) -> i.gameModeSwapTarget = s, i -> i.gameModeSwapTarget)
            .add()
            .build();

    private boolean enabled;
    private UiInputPageTarget useTarget;
    private UiInputPageTarget pickTarget;
    private UiInputPageTarget pickupTarget;
    private UiInputPageTarget collisionEnterTarget;
    private UiInputPageTarget collisionLeaveTarget;
    private UiInputPageTarget collisionTarget;
    private UiInputPageTarget entityStatEffectTarget;
    private UiInputPageTarget swapToTarget;
    private UiInputPageTarget swapFromTarget;
    private UiInputPageTarget deathTarget;
    private UiInputPageTarget wieldingTarget;
    private UiInputPageTarget projectileSpawnTarget;
    private UiInputPageTarget projectileHitTarget;
    private UiInputPageTarget projectileMissTarget;
    private UiInputPageTarget projectileBounceTarget;
    private UiInputPageTarget heldTarget;
    private UiInputPageTarget heldOffhandTarget;
    private UiInputPageTarget equippedTarget;
    private UiInputPageTarget dodgeTarget;
    private UiInputPageTarget gameModeSwapTarget;

    public UiInputModSettings() {
        this.enabled = false;
        this.useTarget = UiInputPageTarget.None;
        this.pickTarget = UiInputPageTarget.None;
        this.pickupTarget = UiInputPageTarget.None;
        this.collisionEnterTarget = UiInputPageTarget.None;
        this.collisionLeaveTarget = UiInputPageTarget.None;
        this.collisionTarget = UiInputPageTarget.None;
        this.entityStatEffectTarget = UiInputPageTarget.None;
        this.swapToTarget = UiInputPageTarget.None;
        this.swapFromTarget = UiInputPageTarget.None;
        this.deathTarget = UiInputPageTarget.None;
        this.wieldingTarget = UiInputPageTarget.None;
        this.projectileSpawnTarget = UiInputPageTarget.None;
        this.projectileHitTarget = UiInputPageTarget.None;
        this.projectileMissTarget = UiInputPageTarget.None;
        this.projectileBounceTarget = UiInputPageTarget.None;
        this.heldTarget = UiInputPageTarget.None;
        this.heldOffhandTarget = UiInputPageTarget.None;
        this.equippedTarget = UiInputPageTarget.None;
        this.dodgeTarget = UiInputPageTarget.None;
        this.gameModeSwapTarget = UiInputPageTarget.None;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nonnull
    public UiInputPageTarget getConfiguredTarget(@Nonnull InteractionType interactionType) {
        return switch (interactionType) {
            case Use -> useTarget;
            case Pick -> pickTarget;
            case Pickup -> pickupTarget;
            case CollisionEnter -> collisionEnterTarget;
            case CollisionLeave -> collisionLeaveTarget;
            case Collision -> collisionTarget;
            case EntityStatEffect -> entityStatEffectTarget;
            case SwapTo -> swapToTarget;
            case SwapFrom -> swapFromTarget;
            case Death -> deathTarget;
            case Wielding -> wieldingTarget;
            case ProjectileSpawn -> projectileSpawnTarget;
            case ProjectileHit -> projectileHitTarget;
            case ProjectileMiss -> projectileMissTarget;
            case ProjectileBounce -> projectileBounceTarget;
            case Held -> heldTarget;
            case HeldOffhand -> heldOffhandTarget;
            case Equipped -> equippedTarget;
            case Dodge -> dodgeTarget;
            case GameModeSwap -> gameModeSwapTarget;
            default -> UiInputPageTarget.None;
        };
    }

    @Nonnull
    public UiInputPageTarget getEffectiveTarget(@Nonnull InteractionType interactionType) {
        UiInputPageTarget target = getConfiguredTarget(interactionType);
        return isPageTargetAllowed(target) ? target : UiInputPageTarget.None;
    }

    public boolean hasConfiguredTargets() {
        return getConfiguredTarget(InteractionType.Use) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Pick) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Pickup) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.CollisionEnter) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.CollisionLeave) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Collision) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.EntityStatEffect) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.SwapTo) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.SwapFrom) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Death) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Wielding) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.ProjectileSpawn) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.ProjectileHit) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.ProjectileMiss) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.ProjectileBounce) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Held) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.HeldOffhand) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Equipped) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.Dodge) != UiInputPageTarget.None
                || getConfiguredTarget(InteractionType.GameModeSwap) != UiInputPageTarget.None;
    }

    public static boolean isPageTargetAllowed(@Nonnull UiInputPageTarget target) {
        return switch (target) {
            case None -> true;
            case Spellbook -> ALLOW_SPELLBOOK_PAGE;
            case RpgInventory -> ALLOW_RPG_INVENTORY_PAGE;
            case Party -> ALLOW_PARTY_PAGE;
            case Guild -> ALLOW_GUILD_PAGE;
            case Mail -> ALLOW_MAIL_PAGE;
                        case Bank -> ALLOW_BANK_PAGE;
                        case Currency -> ALLOW_CURRENCY_PAGE;
                        case GroupFinder -> ALLOW_GROUP_FINDER_PAGE;
        };
    }
}