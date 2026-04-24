package org.pixelbays.rpg.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.prereq.PrerequisiteEvaluator;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.world.config.WorldRouteDefinition;
import org.pixelbays.rpg.world.config.WorldRouteDefinition.DestinationType;
import org.pixelbays.rpg.world.config.WorldRouteDefinition.TravelAudience;
import org.pixelbays.rpg.world.config.settings.WorldModSettings;
import org.pixelbays.rpg.world.config.settings.WorldModSettings.WorldMode;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class WorldTravelManager {

	public boolean travelRoute(@Nullable String requestedRouteId,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef) {
		WorldModSettings settings = resolveSettings();
		if (!settings.isEnabled()) {
			playerRef.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.systemDisabled"));
			return false;
		}

		WorldRouteDefinition route = resolveRoute(requestedRouteId);
		if (route == null) {
			playerRef.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.routeNotFound")
					.param("route", requestedRouteId == null ? "" : requestedRouteId));
			return false;
		}
		if (!route.isEnabled()) {
			playerRef.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.routeDisabled")
					.param("route", route.getDisplayNameOrId()));
			return false;
		}
		if (!isDestinationAllowed(settings, route.getDestinationType())) {
			playerRef.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.modeBlocked")
					.param("route", route.getDisplayNameOrId())
					.param("mode", settings.getWorldMode().name()));
			return false;
		}

		Message failure = PrerequisiteEvaluator.evaluateFailure(route.getPrerequisites(), null, store, ref, playerRef);
		if (failure != null) {
			playerRef.sendMessage(failure);
			return false;
		}

		List<PlayerRef> targets = resolveTargets(route, playerRef);
		if (targets.isEmpty()) {
			playerRef.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.noTargets")
					.param("route", route.getDisplayNameOrId()));
			return false;
		}

		if (!validateTargets(route, playerRef, targets)) {
			return false;
		}

		if (!beginTravel(route, playerRef, store, ref, targets)) {
			return false;
		}

		notifyTargets(route, playerRef, targets);
		return true;
	}

	public void sendRouteList(@Nonnull com.hypixel.hytale.server.core.entity.entities.Player player) {
		List<WorldRouteDefinition> routes = getEnabledRoutes();
		if (routes.isEmpty()) {
			player.sendMessage(Message.translation("pixelbays.rpg.world.travel.list.none"));
			return;
		}

		player.sendMessage(Message.translation("pixelbays.rpg.world.travel.list.header"));
		for (WorldRouteDefinition route : routes) {
			player.sendMessage(Message.translation("pixelbays.rpg.world.travel.list.entry")
					.param("routeId", route.getId())
					.param("name", route.getDisplayNameOrId())
					.param("type", route.getDestinationType().name()));
		}
	}

	@Nonnull
	public List<WorldRouteDefinition> getEnabledRoutes() {
		var assetMap = WorldRouteDefinition.getAssetMap();
		if (assetMap == null || assetMap.getAssetMap().isEmpty()) {
			return List.of();
		}

		List<WorldRouteDefinition> routes = new ArrayList<>();
		for (WorldRouteDefinition route : assetMap.getAssetMap().values()) {
			if (route != null && route.isEnabled()) {
				routes.add(route);
			}
		}
		routes.sort(Comparator.comparing(WorldRouteDefinition::getDisplayNameOrId, String.CASE_INSENSITIVE_ORDER));
		return routes;
	}

	@Nullable
	public WorldRouteDefinition resolveRoute(@Nullable String requestedRouteId) {
		if (requestedRouteId == null || requestedRouteId.isBlank()) {
			return null;
		}

		var assetMap = WorldRouteDefinition.getAssetMap();
		if (assetMap == null) {
			return null;
		}

		WorldRouteDefinition exact = assetMap.getAsset(requestedRouteId);
		if (exact != null) {
			return exact;
		}

		for (WorldRouteDefinition route : assetMap.getAssetMap().values()) {
			if (route != null && route.getId().equalsIgnoreCase(requestedRouteId)) {
				return route;
			}
		}
		return null;
	}

	private boolean validateTargets(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator,
			@Nonnull List<PlayerRef> targets) {
		for (PlayerRef target : targets) {
			Ref<EntityStore> targetRef = target.getReference();
			if (targetRef == null || !targetRef.isValid()) {
				continue;
			}
			Store<EntityStore> targetStore = targetRef.getStore();
			Message failure = PrerequisiteEvaluator.evaluateFailure(route.getPrerequisites(), null, targetStore, targetRef, target);
			if (failure == null) {
				continue;
			}
			target.sendMessage(failure);
			if (!target.getUuid().equals(initiator.getUuid())) {
				initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.groupMemberFailed")
						.param("player", target.getUsername()));
			}
			return false;
		}
		return true;
	}

	private boolean beginTravel(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator,
			@Nonnull Store<EntityStore> initiatorStore,
			@Nonnull Ref<EntityStore> initiatorRef,
			@Nonnull List<PlayerRef> targets) {
		return switch (route.getDestinationType()) {
			case World -> travelToWorld(route, initiator, targets);
			case InstanceTemplate -> travelToInstance(route, initiator, initiatorStore, initiatorRef, targets, false);
			case ShardTemplate -> travelToInstance(route, initiator, initiatorStore, initiatorRef, targets, true);
		};
	}

	private boolean travelToWorld(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator,
			@Nonnull List<PlayerRef> targets) {
		World targetWorld = resolveTargetWorld(route);
		if (targetWorld == null) {
			initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.destinationWorldMissing")
					.param("route", route.getDisplayNameOrId())
					.param("world", route.getTargetWorldName()));
			return false;
		}

		for (PlayerRef target : targets) {
			Ref<EntityStore> targetRef = target.getReference();
			if (targetRef == null || !targetRef.isValid()) {
				continue;
			}
			Store<EntityStore> targetStore = targetRef.getStore();
			World playerWorld = targetStore.getExternalData().getWorld();
			playerWorld.execute(() -> {
				if (!targetRef.isValid()) {
					return;
				}
				Transform targetTransform = resolveSpawnTransform(targetWorld, target.getUuid(), targetStore, targetRef);
				targetStore.addComponent(targetRef, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, targetTransform));
			});
		}
		return true;
	}

	private boolean travelToInstance(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator,
			@Nonnull Store<EntityStore> initiatorStore,
			@Nonnull Ref<EntityStore> initiatorRef,
			@Nonnull List<PlayerRef> targets,
			boolean shardMode) {
		String templateId = route.getInstanceTemplateId();
		if (templateId.isBlank() || !InstancesPlugin.doesInstanceAssetExist(templateId)) {
			initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.instanceMissing")
					.param("route", route.getDisplayNameOrId())
					.param("instance", templateId));
			return false;
		}

		World initiatorWorld = initiatorStore.getExternalData().getWorld();
		Transform initialReturnPoint = resolveCurrentTransform(initiatorStore, initiatorRef, initiator.getUuid(), initiatorWorld);
		CompletableFuture<World> worldFuture = shardMode
				? InstancesPlugin.get().spawnInstance(templateId, route.getEffectiveShardKey(), initiatorWorld, initialReturnPoint)
				: InstancesPlugin.get().spawnInstance(templateId, initiatorWorld, initialReturnPoint);
		worldFuture.thenAccept(InstanceWorldLifecycle::configureEphemeralInstance);

		for (PlayerRef target : targets) {
			Ref<EntityStore> targetRef = target.getReference();
			if (targetRef == null || !targetRef.isValid()) {
				continue;
			}
			Store<EntityStore> targetStore = targetRef.getStore();
			World playerWorld = targetStore.getExternalData().getWorld();
			Transform personalReturnPoint = route.isPersonalReturnPoint()
					? resolveCurrentTransform(targetStore, targetRef, target.getUuid(), playerWorld)
					: null;
			playerWorld.execute(() -> {
				if (!targetRef.isValid()) {
					return;
				}
				InstancesPlugin.teleportPlayerToLoadingInstance(targetRef, targetStore, worldFuture, personalReturnPoint);
			});
		}
		return true;
	}

	@Nonnull
	private List<PlayerRef> resolveTargets(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator) {
		if (route.getTravelAudience() == TravelAudience.Self) {
			return List.of(initiator);
		}

		PartyManager partyManager = Realmweavers.get().getPartyManager();
		Party party = partyManager.getPartyForMember(initiator.getUuid());
		if (party == null) {
			initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.requiresGroup")
					.param("route", route.getDisplayNameOrId()));
			return List.of();
		}
		if (route.isRequireLeaderForGroupTravel() && !party.isLeader(initiator.getUuid())) {
			initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.error.leaderRequired")
					.param("route", route.getDisplayNameOrId()));
			return List.of();
		}

		List<PlayerRef> targets = new ArrayList<>();
		for (PartyMember member : party.getMemberList()) {
			PlayerRef target = Universe.get().getPlayer(member.getEntityId());
			if (target != null) {
				targets.add(target);
			}
		}
		if (targets.stream().noneMatch(target -> target.getUuid().equals(initiator.getUuid()))) {
			targets.add(initiator);
		}
		return targets;
	}

	private void notifyTargets(@Nonnull WorldRouteDefinition route,
			@Nonnull PlayerRef initiator,
			@Nonnull List<PlayerRef> targets) {
		if (route.getTravelAudience() == TravelAudience.GroupMembers) {
			initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.success.group")
					.param("route", route.getDisplayNameOrId())
					.param("count", Integer.toString(targets.size())));
			for (PlayerRef target : targets) {
				if (!target.getUuid().equals(initiator.getUuid())) {
					target.sendMessage(Message.translation("pixelbays.rpg.world.travel.success.self")
							.param("route", route.getDisplayNameOrId()));
				}
			}
			return;
		}
		initiator.sendMessage(Message.translation("pixelbays.rpg.world.travel.success.self")
				.param("route", route.getDisplayNameOrId()));
	}

	@Nullable
	private World resolveTargetWorld(@Nonnull WorldRouteDefinition route) {
		if (route.getTargetWorldName().isBlank()) {
			return Universe.get().getDefaultWorld();
		}
		World targetWorld = Universe.get().getWorld(route.getTargetWorldName());
		if (targetWorld != null) {
			return targetWorld;
		}
		return Universe.get().getDefaultWorld();
	}

	@Nonnull
	private Transform resolveCurrentTransform(@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UUID playerId,
			@Nonnull World currentWorld) {
		TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
		if (transformComponent != null) {
			return new Transform(transformComponent.getPosition().clone(), transformComponent.getRotation().clone());
		}
		return resolveSpawnTransform(currentWorld, playerId, store, ref);
	}

	@Nonnull
	private Transform resolveSpawnTransform(@Nonnull World targetWorld,
			@Nonnull UUID playerId,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref) {
		ISpawnProvider spawnProvider = targetWorld.getWorldConfig().getSpawnProvider();
		if (spawnProvider != null) {
			Transform transform = spawnProvider.getSpawnPoint(targetWorld, playerId);
			if (transform != null) {
				return transform;
			}
		}
		TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
		if (transformComponent != null) {
			return new Transform(transformComponent.getPosition().clone(), transformComponent.getRotation().clone());
		}
		return new Transform();
	}

	private boolean isDestinationAllowed(@Nonnull WorldModSettings settings, @Nonnull DestinationType destinationType) {
		if (settings.getWorldMode() != WorldMode.VanillaWorld) {
			return true;
		}
		return destinationType == DestinationType.World;
	}

	@Nonnull
	private WorldModSettings resolveSettings() {
		RpgModConfig config = resolveConfig();
		return config == null ? new WorldModSettings() : config.getWorldSettings();
	}

	@Nullable
	private RpgModConfig resolveConfig() {
		var assetMap = RpgModConfig.getAssetMap();
		if (assetMap == null) {
			return null;
		}
		RpgModConfig config = assetMap.getAsset("default");
		if (config != null) {
			return config;
		}
		return assetMap.getAsset("Default");
	}
}