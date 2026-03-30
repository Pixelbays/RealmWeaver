package org.pixelbays.rpg.world.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class WorldModSettings {

	public enum WorldMode {
		VanillaWorld,
		ShardedInstances,
		Hybrid
	}

	public static final BuilderCodec<WorldModSettings> CODEC = BuilderCodec
			.builder(WorldModSettings.class, WorldModSettings::new)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
					(i, s) -> i.enabled = s, i -> i.enabled)
			.add()
			.append(new KeyedCodec<>("WorldMode", new EnumCodec<>(WorldMode.class), false, true),
					(i, s) -> i.worldMode = s, i -> i.worldMode)
			.add()
			.append(new KeyedCodec<>("AllowTravelCommands", Codec.BOOLEAN, false, true),
					(i, s) -> i.allowTravelCommands = s, i -> i.allowTravelCommands)
			.add()
			.build();

	private boolean enabled;
	private WorldMode worldMode;
	private boolean allowTravelCommands;

	public WorldModSettings() {
		this.enabled = true;
		this.worldMode = WorldMode.Hybrid;
		this.allowTravelCommands = true;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public WorldMode getWorldMode() {
		return worldMode == null ? WorldMode.Hybrid : worldMode;
	}

	public boolean isAllowTravelCommands() {
		return allowTravelCommands;
	}
}