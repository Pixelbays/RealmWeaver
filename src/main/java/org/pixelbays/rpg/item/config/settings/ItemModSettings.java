package org.pixelbays.rpg.item.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

@SuppressWarnings("deprecation")
public class ItemModSettings {

	public static final BuilderCodec<ItemModSettings> CODEC = BuilderCodec
			.builder(ItemModSettings.class, ItemModSettings::new)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
					(i, s) -> i.enabled = s, i -> i.enabled)
			.add()
			.append(new KeyedCodec<>("LevelScalingTag", Codec.STRING, false, true),
					(i, s) -> i.levelScalingTag = s, i -> i.levelScalingTag)
			.add()
			.append(new KeyedCodec<>("ScalePerLevelPercent", Codec.DOUBLE, false, true),
					(i, s) -> i.scalePerLevelPercent = s, i -> i.scalePerLevelPercent)
			.add()
			.build();

	private boolean enabled;
	private String levelScalingTag;
	private double scalePerLevelPercent;

	public ItemModSettings() {
		this.enabled = true;
		this.levelScalingTag = "RPG.ScaleWithLevel";
		this.scalePerLevelPercent = 3.0d;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getLevelScalingTag() {
		return levelScalingTag == null ? "" : levelScalingTag;
	}

	public double getScalePerLevelPercent() {
		return scalePerLevelPercent;
	}
}
