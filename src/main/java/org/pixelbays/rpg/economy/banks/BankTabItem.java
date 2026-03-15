package org.pixelbays.rpg.economy.banks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings({ "null", "deprecation" })
public class BankTabItem {

    public static final BuilderCodec<BankTabItem> CODEC = BuilderCodec.builder(BankTabItem.class, BankTabItem::new)
            .append(new KeyedCodec<>("Slot", Codec.INTEGER, false, true),
                    (i, s) -> i.slotIndex = s, i -> i.slotIndex)
            .add()
            .append(new KeyedCodec<>("ItemStack", ItemStack.CODEC, false, true),
                    (i, s) -> i.itemStack = s, i -> i.itemStack)
            .add()
            .build();

    private int slotIndex;
    private ItemStack itemStack;

    public BankTabItem() {
        this(0, ItemStack.EMPTY);
    }

    public BankTabItem(int slotIndex, @Nullable ItemStack itemStack) {
        this.slotIndex = slotIndex;
        this.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    @Nonnull
    public ItemStack getItemStack() {
        return itemStack == null ? ItemStack.EMPTY : itemStack;
    }
}
