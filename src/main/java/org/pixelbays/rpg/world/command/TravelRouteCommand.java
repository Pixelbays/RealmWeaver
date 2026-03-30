package org.pixelbays.rpg.world.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TravelRouteCommand extends AbstractPlayerCommand {

	private final RequiredArg<String> routeIdArg;

	public TravelRouteCommand() {
		super("travelroute", "Travel using a configured world route");
		requirePermission(HytalePermissions.fromCommand("player"));
		this.routeIdArg = this.withRequiredArg("routeId", "Configured route id or 'list'", ArgTypes.STRING);
	}

	@Override
	protected void execute(@Nonnull CommandContext ctx,
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> ref,
			@Nonnull PlayerRef playerRef,
			@Nonnull World world) {
		String requestedRouteId = routeIdArg.get(ctx);
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		if ("list".equalsIgnoreCase(requestedRouteId)) {
			Realmweavers.get().getWorldTravelManager().sendRouteList(player);
			return;
		}

		Realmweavers.get().getWorldTravelManager().travelRoute(requestedRouteId, store, ref, playerRef);
	}
}