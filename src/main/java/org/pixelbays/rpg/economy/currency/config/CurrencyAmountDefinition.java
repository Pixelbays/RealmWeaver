package org.pixelbays.rpg.economy.currency.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CurrencyAmountDefinition {

    public static final BuilderCodec<CurrencyAmountDefinition> CODEC = BuilderCodec
            .builder(CurrencyAmountDefinition.class, CurrencyAmountDefinition::new)
            .append(new KeyedCodec<>("CurrencyId", Codec.STRING, false, true),
                    (i, s) -> i.currencyId = s, i -> i.currencyId)
            .add()
            .append(new KeyedCodec<>("Amount", Codec.LONG, false, true),
                    (i, s) -> i.amount = s, i -> i.amount)
            .add()
            .build();

    private String currencyId;
    private long amount;

    public CurrencyAmountDefinition() {
        this.currencyId = "";
        this.amount = 0L;
    }

    public CurrencyAmountDefinition(String currencyId, long amount) {
        this.currencyId = currencyId;
        this.amount = amount;
    }

    public String getCurrencyId() {
        return currencyId == null ? "" : currencyId;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isFree() {
        return amount <= 0L;
    }
}
