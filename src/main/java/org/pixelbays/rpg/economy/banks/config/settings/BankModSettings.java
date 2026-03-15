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
            .append(new KeyedCodec<>("DefaultPersonalBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultPersonalBankTypeId = s, i -> i.defaultPersonalBankTypeId)
            .add()
            .append(new KeyedCodec<>("DefaultAccountBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultAccountBankTypeId = s, i -> i.defaultAccountBankTypeId)
            .add()
            .append(new KeyedCodec<>("DefaultGuildBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultGuildBankTypeId = s, i -> i.defaultGuildBankTypeId)
            .add()
            .append(new KeyedCodec<>("DefaultVoidBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultVoidBankTypeId = s, i -> i.defaultVoidBankTypeId)
            .add()
            .append(new KeyedCodec<>("DefaultWarboundBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultWarboundBankTypeId = s, i -> i.defaultWarboundBankTypeId)
            .add()
            .append(new KeyedCodec<>("DefaultProfessionBankTypeId", Codec.STRING, false, true),
                    (i, s) -> i.defaultProfessionBankTypeId = s, i -> i.defaultProfessionBankTypeId)
            .add()
            .append(new KeyedCodec<>("AllowAssetDefinedBankTypes", Codec.BOOLEAN, false, true),
                    (i, s) -> i.allowAssetDefinedBankTypes = s, i -> i.allowAssetDefinedBankTypes)
            .add()
            .build();

    private boolean enabled;
    private String defaultPersonalBankTypeId;
    private String defaultAccountBankTypeId;
    private String defaultGuildBankTypeId;
    private String defaultVoidBankTypeId;
    private String defaultWarboundBankTypeId;
    private String defaultProfessionBankTypeId;
    private boolean allowAssetDefinedBankTypes;

    public BankModSettings() {
        this.enabled = true;
        this.defaultPersonalBankTypeId = "Personal";
        this.defaultAccountBankTypeId = "Account";
        this.defaultGuildBankTypeId = "Guild";
        this.defaultVoidBankTypeId = "Void";
        this.defaultWarboundBankTypeId = "Warbound";
        this.defaultProfessionBankTypeId = "Professions";
        this.allowAssetDefinedBankTypes = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDefaultPersonalBankTypeId() {
        return defaultPersonalBankTypeId;
    }

    public String getDefaultAccountBankTypeId() {
        return defaultAccountBankTypeId;
    }

    public String getDefaultGuildBankTypeId() {
        return defaultGuildBankTypeId;
    }

    public String getDefaultVoidBankTypeId() {
        return defaultVoidBankTypeId;
    }

    public String getDefaultWarboundBankTypeId() {
        return defaultWarboundBankTypeId;
    }

    public String getDefaultProfessionBankTypeId() {
        return defaultProfessionBankTypeId;
    }

    public boolean isAllowAssetDefinedBankTypes() {
        return allowAssetDefinedBankTypes;
    }
}
