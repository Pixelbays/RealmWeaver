package org.pixelbays.rpg.economy.banks.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class BankModSettings {

    public static final BuilderCodec<BankModSettings> CODEC = BuilderCodec
            .builder(BankModSettings.class, BankModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .build();

    private boolean enabled;


    public BankModSettings() {
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }


}
