package org.pixelbays.rpg.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.chat.config.ChatFilterData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class ChatFilterPersistence {

    @Nullable
    public ChatFilterData load() {
        var assetMap = ChatFilterData.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        ChatFilterData direct = assetMap.getAsset("Default");
        if (direct != null) {
            return direct;
        }

        for (ChatFilterData data : assetMap.getAssetMap().values()) {
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    public void save(@Nullable ChatFilterData data) {
        Path filterDirectory = resolveFilterDirectory();
        if (filterDirectory == null || data == null) {
            return;
        }

        try {
            Files.createDirectories(filterDirectory);
        } catch (IOException ex) {
            logger().atWarning().withCause(ex).log("Failed to create chat filter directory %s", filterDirectory);
            return;
        }

        BsonValue value = ChatFilterData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            logger().atWarning().log("Chat filter persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = filterDirectory.resolve(data.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    @Nullable
    private Path resolveFilterDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            logger().atWarning().log("Chat filter persistence disabled: no writable asset pack found");
            return null;
        }

        return pack.getRoot().resolve("Server").resolve("ChatFilterData");
    }

    @Nonnull
    private static HytaleLogger logger() {
        return HytaleLogger.forEnclosingClass();
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