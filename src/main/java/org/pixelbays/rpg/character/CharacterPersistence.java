package org.pixelbays.rpg.character;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.character.config.CharacterRosterData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

public class CharacterPersistence {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile boolean legacyCodecsRegistered;

    @Nonnull
    public List<CharacterRosterData> loadAll() {
        Map<String, CharacterRosterData> rostersById = new LinkedHashMap<>();

        var assetMap = CharacterRosterData.getAssetMap();
        if (assetMap != null) {
            for (CharacterRosterData roster : assetMap.getAssetMap().values()) {
                if (roster == null || roster.getId() == null || roster.getId().isBlank()) {
                    continue;
                }
                rostersById.put(roster.getId(), roster);
            }
        }

        Path rosterDir = resolveRosterDirectory();
        if (rosterDir != null && Files.isDirectory(rosterDir)) {
            try (Stream<Path> files = Files.list(rosterDir)) {
                files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                        .map(this::readRosterFile)
                        .filter(Objects::nonNull)
                        .forEach(roster -> rostersById.put(roster.getId(), roster));
            } catch (IOException ex) {
                LOGGER.atWarning().withCause(ex).log("Failed to load character rosters from %s", rosterDir);
            }
        }

        return new ArrayList<>(rostersById.values());
    }

    @Nullable
    public CharacterRosterData loadRoster(@Nonnull UUID accountId) {
        Path rosterDir = resolveRosterDirectory();
        if (rosterDir != null) {
            CharacterRosterData roster = readRosterFile(Objects.requireNonNull(rosterDir.resolve(accountId + ".json")));
            if (roster != null) {
                return roster;
            }
        }

        var assetMap = CharacterRosterData.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        return assetMap.getAsset(accountId.toString());
    }

    public void saveRoster(@Nullable CharacterRosterData roster) {
        Path rosterDir = resolveRosterDirectory();
        if (rosterDir == null || roster == null) {
            return;
        }

        try {
            Files.createDirectories(rosterDir);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to create character roster directory %s", rosterDir);
            return;
        }

        BsonValue value = CharacterRosterData.CODEC.encode(roster, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            LOGGER.atWarning().log("Character persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = rosterDir.resolve(roster.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteRoster(@Nonnull UUID accountId) {
        Path rosterDir = resolveRosterDirectory();
        if (rosterDir == null) {
            return;
        }

        Path filePath = rosterDir.resolve(accountId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to delete character roster file %s", filePath);
        }
    }

    @Nullable
    private Path resolveRosterDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            LOGGER.atWarning().log("Character persistence disabled: no writable asset pack found");
            return null;
        }

        Path root = pack.getRoot();
        return root.resolve("Server").resolve("CharacterData");
    }

    @Nullable
    private AssetPack findWritablePack() {
        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            return null;
        }

        PluginManifest manifest = ExamplePlugin.get().getManifest();
        String pluginId = new PluginIdentifier(manifest).toString();

        AssetPack fallback = null;
        for (AssetPack pack : assetModule.getAssetPacks()) {
            if (pack.isImmutable()) {
                continue;
            }
            if (fallback == null) {
                fallback = pack;
            }
            if (pack.getName().equalsIgnoreCase(pluginId)) {
                return pack;
            }
        }

        return fallback;
    }

    @Nullable
    private CharacterRosterData readRosterFile(@Nonnull Path filePath) {
        if (!Files.isRegularFile(filePath)) {
            return null;
        }

        try {
            ensureLegacyCodecsRegistered();

            String contents = Files.readString(filePath);
            if (contents == null || contents.isBlank()) {
                return null;
            }

            BsonDocument document = BsonDocument.parse(contents);
            CharacterRosterData roster = CharacterRosterData.CODEC.decode(Objects.requireNonNull(document), new ExtraInfo());
            if (roster == null || roster.getId().isBlank()) {
                return null;
            }
            return roster;
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to read character roster file %s", filePath);
            return null;
        }
    }

    private static void ensureLegacyCodecsRegistered() {
        if (legacyCodecsRegistered) {
            return;
        }

        synchronized (CharacterPersistence.class) {
            if (legacyCodecsRegistered) {
                return;
            }

            Modifier.CODEC.register("Boost", StaticModifier.class, StaticModifier.ENTITY_CODEC);
            Modifier.CODEC.register("Static", StaticModifier.class, StaticModifier.ENTITY_CODEC);
            legacyCodecsRegistered = true;
        }
    }
}
