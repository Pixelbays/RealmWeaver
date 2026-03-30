package org.pixelbays.rpg.npc.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

public class NpcModSettings {

    public static final BuilderCodec<NpcModSettings> CODEC = BuilderCodec
            .builder(NpcModSettings.class, NpcModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("ThreatEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.threatEnabled = s, i -> i.threatEnabled)
            .add()
            .append(new KeyedCodec<>("ThreatLookbackSeconds", Codec.FLOAT, false, true),
                    (i, s) -> i.threatLookbackSeconds = s, i -> i.threatLookbackSeconds)
            .add()
            .append(new KeyedCodec<>("DefaultThreatMultiplier", Codec.FLOAT, false, true),
                    (i, s) -> i.defaultThreatMultiplier = s, i -> i.defaultThreatMultiplier)
            .add()
            .append(new KeyedCodec<>("RoleThreatMultipliers",
                    new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new), true),
                    (i, s) -> i.roleThreatMultipliers = s, i -> i.roleThreatMultipliers)
            .add()
            .build();

    private boolean enabled;
    private boolean threatEnabled;
    private float threatLookbackSeconds;
    private float defaultThreatMultiplier;
    private Object2FloatMap<String> roleThreatMultipliers;

    public NpcModSettings() {
        this.enabled = true;
        this.threatEnabled = true;
        this.threatLookbackSeconds = 6.0f;
        this.defaultThreatMultiplier = 1.0f;
        this.roleThreatMultipliers = new Object2FloatOpenHashMap<>();
        this.roleThreatMultipliers.put("tank", 1.5f);
        this.roleThreatMultipliers.put("healer", 1.0f);
        this.roleThreatMultipliers.put("dps", 1.0f);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isThreatEnabled() {
        return threatEnabled;
    }

    public float getThreatLookbackSeconds() {
        return threatLookbackSeconds <= 0.0f ? 6.0f : threatLookbackSeconds;
    }

    public float getDefaultThreatMultiplier() {
        return defaultThreatMultiplier <= 0.0f ? 1.0f : defaultThreatMultiplier;
    }

    public Object2FloatMap<String> getRoleThreatMultipliers() {
        if (roleThreatMultipliers == null) {
            roleThreatMultipliers = new Object2FloatOpenHashMap<>();
        }
        return roleThreatMultipliers;
    }

    public float getThreatMultiplierForRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return getDefaultThreatMultiplier();
        }

        for (Object2FloatMap.Entry<String> entry : getRoleThreatMultipliers().object2FloatEntrySet()) {
            String configuredRoleId = entry.getKey();
            if (configuredRoleId != null && configuredRoleId.equalsIgnoreCase(roleId)) {
                float value = entry.getFloatValue();
                return value <= 0.0f ? getDefaultThreatMultiplier() : value;
            }
        }

        return getDefaultThreatMultiplier();
    }
}