package org.pixelbays.rpg.economy.banks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.economy.banks.config.BankData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class BankPersistence {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public List<BankData> loadAll() {
        var assetMap = BankData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void saveBank(BankAccount bankAccount) {
        Path bankDirectory = resolveBankDirectory();
        if (bankDirectory == null || bankAccount == null) {
            return;
        }

        BankData data = BankData.fromBankAccount(bankAccount);
        BsonValue value = BankData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            LOGGER.atWarning().log("Bank persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = bankDirectory.resolve(bankAccount.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteBank(String bankId) {
        Path bankDirectory = resolveBankDirectory();
        if (bankDirectory == null || bankId == null || bankId.isBlank()) {
            return;
        }

        Path filePath = bankDirectory.resolve(bankId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            LOGGER.atWarning().withCause(ex).log("Failed to delete bank file %s", filePath);
        }
    }

    @Nullable
    private Path resolveBankDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            LOGGER.atWarning().log("Bank persistence disabled: no writable asset pack found");
            return null;
        }

        return pack.getRoot().resolve("Server").resolve("BankData");
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
