package org.pixelbays.rpg.economy.currency.config;

import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;

@SuppressWarnings({ "deprecation", "all", "null" })
public class CurrencyItemDropContainer extends ItemDropContainer {

    public static final BuilderCodec<CurrencyItemDropContainer> CODEC = BuilderCodec
            .builder(CurrencyItemDropContainer.class, CurrencyItemDropContainer::new, ItemDropContainer.DEFAULT_CODEC)
            .appendInherited(new KeyedCodec<>("CurrencyId", Codec.STRING, false, true),
                    (container, value) -> container.currencyId = value,
                    container -> container.currencyId,
                    (container, parent) -> container.currencyId = parent.currencyId)
            .add()
            .appendInherited(new KeyedCodec<>("AmountMin", Codec.LONG, false, true),
                    (container, value) -> container.amountMin = value,
                    container -> container.amountMin,
                    (container, parent) -> container.amountMin = parent.amountMin)
            .add()
            .appendInherited(new KeyedCodec<>("AmountMax", Codec.LONG, false, true),
                    (container, value) -> container.amountMax = value,
                    container -> container.amountMax,
                    (container, parent) -> container.amountMax = parent.amountMax)
            .add()
                .appendInherited(new KeyedCodec<>("Scope", new EnumCodec<>(CurrencyScope.class), false, true),
                    (container, value) -> container.scope = value,
                    container -> container.scope,
                    (container, parent) -> container.scope = parent.scope)
            .add()
            .build();

    private String currencyId = "";
    private long amountMin = 1L;
    private long amountMax = 1L;
    private CurrencyScope scope = CurrencyScope.Character;

    protected CurrencyItemDropContainer() {
        super();
    }

    public CurrencyItemDropContainer(double weight, @Nonnull String currencyId, long amountMin, long amountMax,
            @Nonnull CurrencyScope scope) {
        super(weight);
        this.currencyId = currencyId;
        this.amountMin = amountMin;
        this.amountMax = amountMax;
        this.scope = scope;
    }

    @Nonnull
    public String getCurrencyId() {
        return currencyId == null ? "" : currencyId;
    }

    public long getAmountMin() {
        return amountMin;
    }

    public long getAmountMax() {
        return amountMax;
    }

    @Nonnull
    public CurrencyScope getScope() {
        return scope == null ? CurrencyScope.Character : scope;
    }

    @Override
    protected void populateDrops(List<ItemDrop> drops, DoubleSupplier chanceProvider, Set<String> droplistReferences) {
    }

    @Override
    public List<ItemDrop> getAllDrops(List<ItemDrop> drops) {
        return drops;
    }
}
