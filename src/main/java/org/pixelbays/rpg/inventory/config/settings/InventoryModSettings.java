package org.pixelbays.rpg.inventory.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class InventoryModSettings {

    public enum InventoryHandlingMode {
        Vanilla,
        Item,
        Strength
    }

    public static final BuilderCodec<InventoryModSettings> CODEC = BuilderCodec
            .builder(InventoryModSettings.class, InventoryModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("InventoryHandling", new EnumCodec<>(InventoryHandlingMode.class), false, true),
                    (i, s) -> i.inventoryHandling = s, i -> i.inventoryHandling)
            .add()
            .append(new KeyedCodec<>("DefaultInventorySize", Codec.INTEGER, false, true),
                    (i, s) -> i.defaultInventorySize = s, i -> i.defaultInventorySize)
            .add()
            .append(new KeyedCodec<>("StrengthInventorySlots",
                    new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new), true),
                    (i, s) -> i.strengthInventorySlots = s, i -> i.strengthInventorySlots)
            .add()
            .append(new KeyedCodec<>("ExtraSlotsRingsEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsRingsEnabled = s, i -> i.extraSlotsRingsEnabled)
            .add()
            .append(new KeyedCodec<>("ExtraSlotsTrinketsEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsTrinketsEnabled = s, i -> i.extraSlotsTrinketsEnabled)
            .add()
            .append(new KeyedCodec<>("ExtraSlotsNeckEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.extraSlotsNeckEnabled = s, i -> i.extraSlotsNeckEnabled)
            .add()
            .build();

    private boolean enabled;
    private InventoryHandlingMode inventoryHandling;
    private int defaultInventorySize;
    private Object2IntMap<String> strengthInventorySlots;
    private boolean extraSlotsRingsEnabled;
    private boolean extraSlotsTrinketsEnabled;
    private boolean extraSlotsNeckEnabled;

    public InventoryModSettings() {
        this.enabled = true;
        this.inventoryHandling = InventoryHandlingMode.Vanilla;
        this.defaultInventorySize = 36;
        this.strengthInventorySlots = new Object2IntOpenHashMap<>();
        this.extraSlotsRingsEnabled = false;
        this.extraSlotsTrinketsEnabled = false;
        this.extraSlotsNeckEnabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InventoryHandlingMode getInventoryHandling() {
        return inventoryHandling;
    }

    public int getDefaultInventorySize() {
        return defaultInventorySize;
    }

    public Object2IntMap<String> getStrengthInventorySlots() {
        return strengthInventorySlots;
    }

    public boolean isExtraSlotsRingsEnabled() {
        return extraSlotsRingsEnabled;
    }

    public boolean isExtraSlotsTrinketsEnabled() {
        return extraSlotsTrinketsEnabled;
    }

    public boolean isExtraSlotsNeckEnabled() {
        return extraSlotsNeckEnabled;
    }
}
