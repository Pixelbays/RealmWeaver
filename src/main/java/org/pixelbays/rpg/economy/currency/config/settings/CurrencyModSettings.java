package org.pixelbays.rpg.economy.currency.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CurrencyModSettings {

    public static final BuilderCodec<CurrencyModSettings> CODEC = BuilderCodec
            .builder(CurrencyModSettings.class, CurrencyModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("PersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.persistenceEnabled = s, i -> i.persistenceEnabled)
            .add()
            .append(new KeyedCodec<>("AllowAssetDefinedCurrencyTypes", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowAssetDefinedCurrencyTypes = s, i -> i.allowAssetDefinedCurrencyTypes)
            .add()
            .build();

    private boolean enabled;
    private boolean persistenceEnabled;
    private boolean allowAssetDefinedCurrencyTypes;

    public CurrencyModSettings() {
        this.enabled = true;
        this.persistenceEnabled = true;
        this.allowAssetDefinedCurrencyTypes = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public boolean isAllowAssetDefinedCurrencyTypes() {
        return allowAssetDefinedCurrencyTypes;
    }
}
