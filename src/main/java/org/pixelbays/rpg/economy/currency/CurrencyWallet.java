package org.pixelbays.rpg.economy.currency;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.economy.currency.config.CurrencyScope;

public class CurrencyWallet {

    private final String id;
    private final CurrencyScope scope;
    private final String ownerId;
    private final Map<String, Long> balances;
    private final long createdAt;
    private long updatedAt;

    public CurrencyWallet(@Nonnull String id,
            @Nonnull CurrencyScope scope,
            @Nonnull String ownerId,
            @Nonnull Map<String, Long> balances,
            long createdAt,
            long updatedAt) {
        this.id = id;
        this.scope = scope;
        this.ownerId = ownerId;
        this.balances = new LinkedHashMap<>(balances);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public CurrencyScope getScope() {
        return scope;
    }

    @Nonnull
    public String getOwnerId() {
        return ownerId;
    }

    @Nonnull
    public Map<String, Long> getBalances() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(balances));
    }

    public long getBalance(@Nonnull String currencyId) {
        return balances.getOrDefault(currencyId, 0L);
    }

    public void setBalance(@Nonnull String currencyId, long amount) {
        if (amount == 0L) {
            balances.remove(currencyId);
        } else {
            balances.put(currencyId, amount);
        }
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}
