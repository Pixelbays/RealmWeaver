package org.pixelbays.rpg.nameplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.achievement.component.AchievementComponent;
import org.pixelbays.rpg.character.CharacterManager;
import org.pixelbays.rpg.character.config.CharacterProfileData;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.config.settings.NameplateModSettings;
import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildManager;
import org.pixelbays.rpg.nameplate.component.PlayerSecondaryNameplateComponent;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerNameplateUpdateSystem extends DelayedEntitySystem<EntityStore> {

    private static final float DEFAULT_INTERVAL_SECONDS = 0.5f;
    private static final double DEFAULT_PLAYER_HEIGHT = 1.8d;
    private static final double SECONDARY_LINE_HEIGHT_OFFSET = 0.15d;
    private static final String SECONDARY_NAMEPLATE_PROJECTILE = "Projectile";
    private static final String SECONDARY_NAMEPLATE_UUID_NAMESPACE = "pixelbays:rpg:nameplate:secondary:";

    public PlayerNameplateUpdateSystem() {
        super(DEFAULT_INTERVAL_SECONDS);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        ComponentType<EntityStore, Nameplate> nameplateType = Nameplate.getComponentType();
        ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
        if (nameplateType == null || playerRefType == null) {
            return;
        }

        Nameplate nameplate = archetypeChunk.getComponent(index, nameplateType);
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefType);
        if (nameplate == null || playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        RpgModConfig config = resolveConfig();
        String text = resolveBaseNameplate(store, ref, playerRef);
        String secondaryText = "";
        if (config != null && config.getNameplateSettings().isEnabled()) {
            PlayerNameplateFormatter.Layout layout = resolveCustomNameplateLayout(
                    config.getNameplateSettings(), store, ref, playerRef, text);
            text = layout.mainLine();
            secondaryText = layout.secondaryLine();
        }
        nameplate.setText(text);
        syncSecondaryNameplate(store, commandBuffer, ref, playerRef, secondaryText);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, Player> playerType = Player.getComponentType();
        ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
        ComponentType<EntityStore, Nameplate> nameplateType = Nameplate.getComponentType();

        Query<EntityStore> query = null;
        if (playerType != null) {
            query = playerType;
        }
        if (playerRefType != null) {
            query = query == null ? playerRefType : Query.and(query, playerRefType);
        }
        if (nameplateType != null) {
            query = query == null ? nameplateType : Query.and(query, nameplateType);
        }
        return query == null ? Query.any() : query;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }

    @Nonnull
    private PlayerNameplateFormatter.Layout resolveCustomNameplateLayout(
            @Nonnull NameplateModSettings settings,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull String baseDisplayName) {

        Realmweavers plugin = Realmweavers.get();
        CharacterManager characterManager = plugin.getCharacterManager();
        GuildManager guildManager = plugin.getGuildManager();

        CharacterProfileData activeProfile = characterManager == null
                ? null
                : characterManager.getActiveProfile(playerRef.getUuid(), playerRef.getUsername());
        Guild guild = guildManager == null ? null : guildManager.getGuildForMember(playerRef.getUuid());

        String accountName = playerRef.getUsername();
        if (accountName == null || accountName.isBlank()) {
            accountName = baseDisplayName;
        }

        ResolvedDisplayedTitle resolvedTitle = resolveDisplayedTitle(store, ref, activeProfile);
        PlayerNameplateFormatter.Content content = new PlayerNameplateFormatter.Content(
                accountName,
                activeProfile == null ? "" : activeProfile.getCharacterName(),
                resolvedTitle.legacyTitle(),
                resolvedTitle.prefix(),
                resolvedTitle.suffix(),
                guild == null ? "" : guild.getName(),
                guild == null ? "" : guild.getTag());

        return PlayerNameplateFormatter.layout(settings, content);
    }

    private void syncSecondaryNameplate(
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> ownerRef,
            @Nonnull PlayerRef playerRef,
            @Nullable String secondaryText) {

        UUID hologramUuid = buildSecondaryNameplateUuid(playerRef.getUuid());
        Ref<EntityStore> hologramRef = store.getExternalData().getRefFromUUID(hologramUuid);
        String sanitizedSecondaryText = secondaryText == null ? "" : secondaryText.trim();

        if (sanitizedSecondaryText.isBlank()) {
            removeSecondaryNameplate(commandBuffer, hologramRef);
            return;
        }

        SecondaryNameplateAnchor anchor = resolveSecondaryNameplateAnchor(store, ownerRef);
        if (anchor == null) {
            removeSecondaryNameplate(commandBuffer, hologramRef);
            return;
        }

        ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType = PlayerSecondaryNameplateComponent
                .getComponentType();
        if (markerType == null) {
            return;
        }

        if (hologramRef == null || !hologramRef.isValid()) {
            spawnSecondaryNameplate(commandBuffer, store, playerRef, sanitizedSecondaryText, anchor, hologramUuid, markerType);
            return;
        }

        PlayerSecondaryNameplateComponent marker = store.getComponent(hologramRef, markerType);
        if (marker == null || !playerRef.getUuid().equals(marker.getOwnerPlayerUuid())) {
            commandBuffer.removeEntity(hologramRef, RemoveReason.REMOVE);
            spawnSecondaryNameplate(commandBuffer, store, playerRef, sanitizedSecondaryText, anchor, hologramUuid, markerType);
            return;
        }

        updateSecondaryNameplate(store, commandBuffer, hologramRef, sanitizedSecondaryText, anchor);
    }

    private void spawnSecondaryNameplate(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull String secondaryText,
            @Nonnull SecondaryNameplateAnchor anchor,
            @Nonnull UUID hologramUuid,
            @Nonnull ComponentType<EntityStore, PlayerSecondaryNameplateComponent> markerType) {

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(ProjectileComponent.getComponentType(), new ProjectileComponent(SECONDARY_NAMEPLATE_PROJECTILE));
        holder.addComponent(
                TransformComponent.getComponentType(),
                new TransformComponent(new Vector3d(anchor.position()), new Vector3f(anchor.rotation())));
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(hologramUuid));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        holder.ensureComponent(Intangible.getComponentType());
        holder.ensureComponent(EntityTrackerSystems.Visible.getComponentType());
        holder.addComponent(Nameplate.getComponentType(), new Nameplate(secondaryText));
        holder.addComponent(markerType, new PlayerSecondaryNameplateComponent(playerRef.getUuid()));
        holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
        commandBuffer.addEntity(holder, AddReason.SPAWN);
    }

    private void updateSecondaryNameplate(
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> hologramRef,
            @Nonnull String secondaryText,
            @Nonnull SecondaryNameplateAnchor anchor) {

        Nameplate hologramNameplate = store.getComponent(hologramRef, Nameplate.getComponentType());
        if (hologramNameplate == null) {
            commandBuffer.addComponent(hologramRef, Nameplate.getComponentType(), new Nameplate(secondaryText));
        } else {
            hologramNameplate.setText(secondaryText);
        }

        TransformComponent hologramTransform = store.getComponent(hologramRef, TransformComponent.getComponentType());
        if (hologramTransform == null) {
            commandBuffer.addComponent(
                    hologramRef,
                    TransformComponent.getComponentType(),
                    new TransformComponent(new Vector3d(anchor.position()), new Vector3f(anchor.rotation())));
            return;
        }

        hologramTransform.teleportPosition(anchor.position());
        hologramTransform.teleportRotation(anchor.rotation());
    }

    private void removeSecondaryNameplate(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nullable Ref<EntityStore> hologramRef) {
        if (hologramRef != null && hologramRef.isValid()) {
            commandBuffer.removeEntity(hologramRef, RemoveReason.REMOVE);
        }
    }

    @Nonnull
    private UUID buildSecondaryNameplateUuid(@Nonnull UUID playerUuid) {
        return UUID.nameUUIDFromBytes(
                (SECONDARY_NAMEPLATE_UUID_NAMESPACE + playerUuid).getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    private SecondaryNameplateAnchor resolveSecondaryNameplateAnchor(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ownerRef) {

        TransformComponent transformComponent = store.getComponent(ownerRef, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return null;
        }

        Vector3d position = new Vector3d(transformComponent.getPosition());
        position.y += resolvePlayerHeight(store, ownerRef) + SECONDARY_LINE_HEIGHT_OFFSET;
        return new SecondaryNameplateAnchor(position, new Vector3f(transformComponent.getRotation()));
    }

    private double resolvePlayerHeight(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ownerRef) {

        BoundingBox boundingBox = store.getComponent(ownerRef, BoundingBox.getComponentType());
        if (boundingBox == null) {
            return DEFAULT_PLAYER_HEIGHT;
        }

        Box box = boundingBox.getBoundingBox();
        if (box == null) {
            return DEFAULT_PLAYER_HEIGHT;
        }

        return Math.max(DEFAULT_PLAYER_HEIGHT, box.height());
    }

    @Nonnull
    private String resolveBaseNameplate(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef) {

        DisplayNameComponent displayNameComponent = store.getComponent(ref, DisplayNameComponent.getComponentType());
        Message displayName = displayNameComponent == null ? null : displayNameComponent.getDisplayName();
        if (displayName != null) {
            String ansi = displayName.getAnsiMessage();
            if (ansi != null && !ansi.isBlank()) {
                return ansi;
            }
        }
        return playerRef.getUsername() == null ? "" : playerRef.getUsername();
    }

    @Nonnull
    private ResolvedDisplayedTitle resolveDisplayedTitle(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nullable CharacterProfileData activeProfile) {

        AchievementComponent runtimeAchievements = store.getComponent(ref, AchievementComponent.getComponentType());
        if (runtimeAchievements != null && runtimeAchievements.hasDisplayedTitle()) {
            return new ResolvedDisplayedTitle(
                    runtimeAchievements.getDisplayedTitle(),
                    runtimeAchievements.getDisplayedTitlePrefix(),
                    runtimeAchievements.getDisplayedTitleSuffix());
        }

        if (activeProfile != null
                && activeProfile.getAchievementProgress() != null
                && activeProfile.getAchievementProgress().hasDisplayedTitle()) {
            AchievementComponent storedAchievements = activeProfile.getAchievementProgress();
            return new ResolvedDisplayedTitle(
                    storedAchievements.getDisplayedTitle(),
                    storedAchievements.getDisplayedTitlePrefix(),
                    storedAchievements.getDisplayedTitleSuffix());
        }

        return new ResolvedDisplayedTitle("", "", "");
    }

    @Nullable
    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        return assetMap == null ? null : assetMap.getAsset("default");
    }

    private record ResolvedDisplayedTitle(
            @Nonnull String legacyTitle,
            @Nonnull String prefix,
            @Nonnull String suffix) {
    }

        private record SecondaryNameplateAnchor(
            @Nonnull Vector3d position,
            @Nonnull Vector3f rotation) {
        }
}