package org.pixelbays.rpg.world.config;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.global.prereq.PrerequisiteRequirements;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class WorldRouteDefinition implements JsonAssetWithMap<String, DefaultAssetMap<String, WorldRouteDefinition>> {

	public enum DestinationType {
		World,
		InstanceTemplate,
		ShardTemplate
	}

	public enum TravelAudience {
		Self,
		GroupMembers
	}

	public static final AssetBuilderCodec<String, WorldRouteDefinition> CODEC = AssetBuilderCodec.builder(
			WorldRouteDefinition.class,
			WorldRouteDefinition::new,
			Codec.STRING,
			(definition, key) -> definition.id = key,
			definition -> definition.id,
			(asset, data) -> asset.data = data,
			WorldRouteDefinition::ensureAssetData)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
					(definition, value) -> definition.enabled = value,
					definition -> definition.enabled)
			.add()
			.append(new KeyedCodec<>("DisplayName", Codec.STRING, false, true),
					(definition, value) -> definition.displayName = value,
					definition -> definition.displayName)
			.add()
			.append(new KeyedCodec<>("Description", Codec.STRING, false, true),
					(definition, value) -> definition.description = value,
					definition -> definition.description)
			.add()
			.append(new KeyedCodec<>("DestinationType", new EnumCodec<>(DestinationType.class), false, true),
					(definition, value) -> definition.destinationType = value,
					definition -> definition.destinationType)
			.add()
			.append(new KeyedCodec<>("TargetWorldName", Codec.STRING, false, true),
					(definition, value) -> definition.targetWorldName = value,
					definition -> definition.targetWorldName)
			.add()
			.append(new KeyedCodec<>("InstanceTemplateId", Codec.STRING, false, true),
					(definition, value) -> definition.instanceTemplateId = value,
					definition -> definition.instanceTemplateId)
			.add()
			.append(new KeyedCodec<>("ShardKey", Codec.STRING, false, true),
					(definition, value) -> definition.shardKey = value,
					definition -> definition.shardKey)
			.add()
			.append(new KeyedCodec<>("TravelAudience", new EnumCodec<>(TravelAudience.class), false, true),
					(definition, value) -> definition.travelAudience = value,
					definition -> definition.travelAudience)
			.add()
			.append(new KeyedCodec<>("RequireLeaderForGroupTravel", Codec.BOOLEAN, false, true),
					(definition, value) -> definition.requireLeaderForGroupTravel = value,
					definition -> definition.requireLeaderForGroupTravel)
			.add()
			.append(new KeyedCodec<>("PersonalReturnPoint", Codec.BOOLEAN, false, true),
					(definition, value) -> definition.personalReturnPoint = value,
					definition -> definition.personalReturnPoint)
			.add()
			.append(new KeyedCodec<>("Prerequisites", PrerequisiteRequirements.CODEC, false, true),
					(definition, value) -> definition.prerequisites = value,
					definition -> definition.prerequisites)
			.add()
			.build();

	private static DefaultAssetMap<String, WorldRouteDefinition> ASSET_MAP;

	private AssetExtraInfo.Data data;
	private String id;
	private boolean enabled;
	private String displayName;
	private String description;
	private DestinationType destinationType;
	private String targetWorldName;
	private String instanceTemplateId;
	private String shardKey;
	private TravelAudience travelAudience;
	private boolean requireLeaderForGroupTravel;
	private boolean personalReturnPoint;
	private PrerequisiteRequirements prerequisites;

	public WorldRouteDefinition() {
		this.data = new AssetExtraInfo.Data(WorldRouteDefinition.class, "", null);
		this.id = "";
		this.enabled = true;
		this.displayName = "";
		this.description = "";
		this.destinationType = DestinationType.World;
		this.targetWorldName = "";
		this.instanceTemplateId = "";
		this.shardKey = "";
		this.travelAudience = TravelAudience.Self;
		this.requireLeaderForGroupTravel = true;
		this.personalReturnPoint = true;
		this.prerequisites = new PrerequisiteRequirements();
	}

	@Nonnull
	private AssetExtraInfo.Data ensureAssetData() {
		if (data == null) {
			data = new AssetExtraInfo.Data(WorldRouteDefinition.class, getId(), null);
		}
		return Objects.requireNonNull(data);
	}

	@Nullable
	public static DefaultAssetMap<String, WorldRouteDefinition> getAssetMap() {
		if (ASSET_MAP == null) {
			var assetStore = AssetRegistry.getAssetStore(WorldRouteDefinition.class);
			if (assetStore != null) {
				ASSET_MAP = (DefaultAssetMap<String, WorldRouteDefinition>) assetStore.getAssetMap();
			}
		}
		return ASSET_MAP;
	}

	@Override
	public String getId() {
		return id;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Nonnull
	public String getDisplayName() {
		return displayName == null ? "" : displayName;
	}

	@Nonnull
	public String getDisplayNameOrId() {
		return getDisplayName().isBlank() ? getId() : getDisplayName();
	}

	@Nonnull
	public String getDescription() {
		return description == null ? "" : description;
	}

	@Nonnull
	public DestinationType getDestinationType() {
		return destinationType == null ? DestinationType.World : destinationType;
	}

	@Nonnull
	public String getTargetWorldName() {
		return targetWorldName == null ? "" : targetWorldName;
	}

	@Nonnull
	public String getInstanceTemplateId() {
		return instanceTemplateId == null ? "" : instanceTemplateId;
	}

	@Nonnull
	public String getShardKey() {
		return shardKey == null ? "" : shardKey;
	}

	@Nonnull
	public String getEffectiveShardKey() {
		return getShardKey().isBlank() ? getId() : getShardKey();
	}

	@Nonnull
	public TravelAudience getTravelAudience() {
		return travelAudience == null ? TravelAudience.Self : travelAudience;
	}

	public boolean isRequireLeaderForGroupTravel() {
		return requireLeaderForGroupTravel;
	}

	public boolean isPersonalReturnPoint() {
		return personalReturnPoint;
	}

	@Nonnull
	public PrerequisiteRequirements getPrerequisites() {
		return prerequisites == null ? new PrerequisiteRequirements() : prerequisites;
	}
}