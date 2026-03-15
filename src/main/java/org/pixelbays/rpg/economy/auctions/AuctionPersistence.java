package org.pixelbays.rpg.economy.auctions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.auctions.config.AuctionData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class AuctionPersistence {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public List<AuctionData> loadAll() {
        var assetMap = AuctionData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void saveListing(AuctionListing listing) {
        Path auctionDirectory = resolveAuctionDirectory();
        if (auctionDirectory == null || listing == null) {
            return;
        }

        AuctionData data = AuctionData.fromAuctionListing(listing);
        BsonValue value = AuctionData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            LOGGER.atWarning().log("Auction persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = auctionDirectory.resolve(listing.getListingId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteListing(UUID listingId) {
        Path auctionDirectory = resolveAuctionDirectory();
        if (auctionDirectory == null || listingId == null) {
            return;
        }

        Path filePath = auctionDirectory.resolve(listingId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to delete auction file %s", filePath);
        }
    }

    @Nullable
    private Path resolveAuctionDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            LOGGER.atWarning().log("Auction persistence disabled: no writable asset pack found");
            return null;
        }

        return pack.getRoot().resolve("Server").resolve("AuctionData");
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
}
