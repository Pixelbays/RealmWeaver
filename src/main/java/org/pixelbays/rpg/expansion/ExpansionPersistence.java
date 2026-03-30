package org.pixelbays.rpg.expansion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class ExpansionPersistence {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public List<ExpansionUnlockData> loadAll() {
        var assetMap = ExpansionUnlockData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void save(ExpansionUnlockData data) {
        Path directory = resolveDirectory();
        if (directory == null || data == null) {
            return;
        }

        BsonValue value = ExpansionUnlockData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            LOGGER.atWarning().log("Expansion persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = directory.resolve(data.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void delete(String id) {
        Path directory = resolveDirectory();
        if (directory == null || id == null || id.isBlank()) {
            return;
        }

        Path filePath = directory.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to delete expansion unlock file %s", filePath);
        }
    }

    @Nullable
    private Path resolveDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            LOGGER.atWarning().log("Expansion persistence disabled: no writable asset pack found");
            return null;
        }

        return pack.getRoot().resolve("Server").resolve("ExpansionData");
    }

    @Nullable
    private AssetPack findWritablePack() {
        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            return null;
        }

        PluginManifest manifest = Realmweavers.get().getManifest();
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
}
