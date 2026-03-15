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
            .build();

    private boolean enabled;
    private boolean persistenceEnabled;

    public CurrencyModSettings() {
        this.enabled = true;
        this.persistenceEnabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }


}
