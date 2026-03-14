package org.pixelbays.rpg.ability.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class AbilityModSettings {

    public enum AbilityControlType {
        Weapons,
        Hotbar,
        AbilitySlots123
    }

    public static final BuilderCodec<AbilityModSettings> CODEC = BuilderCodec
            .builder(AbilityModSettings.class, AbilityModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("BaseGlobalCooldown", Codec.INTEGER, false, true),
                    (i, s) -> i.baseGlobalCooldown = s, i -> i.baseGlobalCooldown)
            .add()
            .append(new KeyedCodec<>("GlobalCooldownCategories",
                    new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new), true),
                    (i, s) -> i.globalCooldownCategories = s, i -> i.globalCooldownCategories)
            .add()
            .append(new KeyedCodec<>("AbilityControlType", new EnumCodec<>(AbilityControlType.class), false, true),
                    (i, s) -> i.abilityControlType = s, i -> i.abilityControlType)
            .add()
            .append(new KeyedCodec<>("HotbarAbilitySlots", Codec.INT_ARRAY, false, true),
                    (i, s) -> i.hotbarAbilitySlots = s, i -> i.hotbarAbilitySlots)
            .add()
            .build();

    private boolean enabled;
    private int baseGlobalCooldown;
    private Object2IntMap<String> globalCooldownCategories;
    private AbilityControlType abilityControlType;
    private int[] hotbarAbilitySlots;

    public AbilityModSettings() {
        this.enabled = true;
        this.baseGlobalCooldown = 0;
        this.globalCooldownCategories = new Object2IntOpenHashMap<>();
        this.abilityControlType = AbilityControlType.Hotbar;
        this.hotbarAbilitySlots = new int[] { 6, 7, 8 };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getBaseGlobalCooldown() {
        return baseGlobalCooldown;
    }

    public Object2IntMap<String> getGlobalCooldownCategories() {
        return globalCooldownCategories;
    }

    public AbilityControlType getAbilityControlType() {
        return abilityControlType;
    }

    public int[] getHotbarAbilitySlots() {
        return hotbarAbilitySlots;
    }
}
