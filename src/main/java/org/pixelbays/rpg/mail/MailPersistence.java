package org.pixelbays.rpg.mail;

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
import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.mail.config.MailData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class MailPersistence {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public List<MailData> loadAll() {
        var assetMap = MailData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void saveMail(MailMessage mailMessage) {
        Path mailDirectory = resolveMailDirectory();
        if (mailDirectory == null || mailMessage == null) {
            return;
        }

        MailData data = MailData.fromMailMessage(mailMessage);
        BsonValue value = MailData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            LOGGER.atWarning().log("Mail persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = mailDirectory.resolve(mailMessage.getMessageId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteMail(UUID messageId) {
        Path mailDirectory = resolveMailDirectory();
        if (mailDirectory == null || messageId == null) {
            return;
        }

        Path filePath = mailDirectory.resolve(messageId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to delete mail file %s", filePath);
        }
    }

    @Nullable
    private Path resolveMailDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            LOGGER.atWarning().log("Mail persistence disabled: no writable asset pack found");
            return null;
        }

        return pack.getRoot().resolve("Server").resolve("MailData");
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
