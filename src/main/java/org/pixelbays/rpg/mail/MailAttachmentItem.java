package org.pixelbays.rpg.mail;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

@SuppressWarnings({ "null", "deprecation" })
public class MailAttachmentItem {

    public static final BuilderCodec<MailAttachmentItem> CODEC = BuilderCodec.builder(MailAttachmentItem.class,
            MailAttachmentItem::new)
            .append(new KeyedCodec<>("ItemStack", ItemStack.CODEC, false, true),
                    (i, s) -> i.itemStack = s, i -> i.itemStack)
            .add()
            .build();

    private ItemStack itemStack;

    public MailAttachmentItem() {
        this(ItemStack.EMPTY);
    }

    public MailAttachmentItem(@Nullable ItemStack itemStack) {
        this.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack;
    }

    @Nonnull
    public ItemStack getItemStack() {
        return itemStack == null ? ItemStack.EMPTY : itemStack;
    }
}
