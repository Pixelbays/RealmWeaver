package org.pixelbays.rpg.classes.config.settings;

import org.pixelbays.rpg.economy.currency.config.CurrencyAmountDefinition;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class TalentModSettings {

    public enum TalentSpecMode {
        Free,
        SoftCap,
        Exclusive
    }

    public enum TalentResetMode {
        Free,
        Paid,
        Partial
    }

    public static final BuilderCodec<TalentModSettings> CODEC = BuilderCodec
            .builder(TalentModSettings.class, TalentModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("SpecMode", new EnumCodec<>(TalentSpecMode.class), false, true),
                    (i, s) -> i.specMode = s, i -> i.specMode)
            .add()
            .append(new KeyedCodec<>("ResetMode", new EnumCodec<>(TalentResetMode.class), false, true),
                    (i, s) -> i.resetMode = s, i -> i.resetMode)
            .add()
                .append(new KeyedCodec<>("ResetCost", CurrencyAmountDefinition.CODEC, false, true),
                    (i, s) -> i.resetCost = s, i -> i.resetCost)
            .add()
            .append(new KeyedCodec<>("PartialRefundPercent", Codec.FLOAT, false, true),
                    (i, s) -> i.partialRefundPercent = s, i -> i.partialRefundPercent)
            .add()
            .build();

    private boolean enabled;
    private TalentSpecMode specMode;
    private TalentResetMode resetMode;
    private CurrencyAmountDefinition resetCost;
    private float partialRefundPercent;

    public TalentModSettings() {
        this.enabled = true;
        this.specMode = TalentSpecMode.Free;
        this.resetMode = TalentResetMode.Free;
        this.resetCost = new CurrencyAmountDefinition();
        this.partialRefundPercent = 1.0f;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TalentSpecMode getSpecMode() {
        return specMode;
    }

    public void setSpecMode(TalentSpecMode specMode) {
        this.specMode = specMode;
    }

    public TalentResetMode getResetMode() {
        return resetMode;
    }

    public void setResetMode(TalentResetMode resetMode) {
        this.resetMode = resetMode;
    }

    public CurrencyAmountDefinition getResetCost() {
        return resetCost == null ? new CurrencyAmountDefinition() : resetCost;
    }

    public void setResetCost(CurrencyAmountDefinition resetCost) {
        this.resetCost = resetCost;
    }

    public float getPartialRefundPercent() {
        return partialRefundPercent;
    }

    public void setPartialRefundPercent(float partialRefundPercent) {
        this.partialRefundPercent = partialRefundPercent;
    }
}
