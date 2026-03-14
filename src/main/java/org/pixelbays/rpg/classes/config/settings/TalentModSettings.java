package org.pixelbays.rpg.classes.config.settings;

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
            .append(new KeyedCodec<>("ResetCostItemId", Codec.STRING, false, true),
                    (i, s) -> i.resetCostItemId = s, i -> i.resetCostItemId)
            .add()
            .append(new KeyedCodec<>("PartialRefundPercent", Codec.FLOAT, false, true),
                    (i, s) -> i.partialRefundPercent = s, i -> i.partialRefundPercent)
            .add()
            .build();

    private boolean enabled;
    private TalentSpecMode specMode;
    private TalentResetMode resetMode;
    private String resetCostItemId;
    private float partialRefundPercent;

    public TalentModSettings() {
        this.enabled = true;
        this.specMode = TalentSpecMode.Free;
        this.resetMode = TalentResetMode.Free;
        this.resetCostItemId = "";
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

    public String getResetCostItemId() {
        return resetCostItemId;
    }

    public void setResetCostItemId(String resetCostItemId) {
        this.resetCostItemId = resetCostItemId;
    }

    public float getPartialRefundPercent() {
        return partialRefundPercent;
    }

    public void setPartialRefundPercent(float partialRefundPercent) {
        this.partialRefundPercent = partialRefundPercent;
    }
}
