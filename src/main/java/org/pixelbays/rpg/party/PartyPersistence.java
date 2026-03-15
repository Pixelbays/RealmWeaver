package org.pixelbays.rpg.party;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.config.PartyData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class PartyPersistence {

    private static final Logger FALLBACK_LOGGER = Logger.getLogger(PartyPersistence.class.getName());

    public List<PartyData> loadAll() {
        var assetMap = PartyData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void saveParty(Party party) {
        Path partyDir = resolvePartyDirectory();
        if (partyDir == null) {
            return;
        }

        if (party == null) {
            return;
        }

        PartyData data = PartyData.fromParty(party);
        BsonValue value = PartyData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            logWarning("Party persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = partyDir.resolve(party.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteParty(UUID partyId) {
        Path partyDir = resolvePartyDirectory();
        if (partyDir == null) {
            return;
        }

        Path filePath = partyDir.resolve(partyId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            logWarning(ex, "Failed to delete party file %s", filePath);
        }
    }

    @Nullable
    private Path resolvePartyDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            logWarning("Party persistence disabled: no writable asset pack found");
            return null;
        }

        Path root = pack.getRoot();
        return root.resolve("Server").resolve("PartyData");
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

    private static void logWarning(String message, Object... args) {
        HytaleLogger logger = getLogger();
        if (logger != null) {
            logger.atWarning().log(message, args);
            return;
        }
        FALLBACK_LOGGER.log(Level.WARNING, String.format(message, args));
    }

    private static void logWarning(Throwable cause, String message, Object... args) {
        HytaleLogger logger = getLogger();
        if (logger != null) {
            logger.atWarning().withCause(cause).log(message, args);
            return;
        }
        FALLBACK_LOGGER.log(Level.WARNING, String.format(message, args), cause);
    }

    @Nullable
    private static HytaleLogger getLogger() {
        try {
            return HytaleLogger.forEnclosingClass();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
