package org.pixelbays.rpg.guild;

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
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.config.GuildData;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.util.BsonUtil;

public class GuildPersistence {

    public List<GuildData> loadAll() {
        var assetMap = GuildData.getAssetMap();
        if (assetMap == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(assetMap.getAssetMap().values());
    }

    public void saveGuild(Guild guild) {
        saveGuild(guild, null);
    }

    public void saveGuild(Guild guild, List<GuildData.GuildApplicationData> applications) {
        Path guildDir = resolveGuildDirectory();
        if (guildDir == null) {
            return;
        }

        if (guild == null) {
            return;
        }

        GuildData data = GuildData.fromGuild(guild, applications);
        BsonValue value = GuildData.CODEC.encode(data, new ExtraInfo());
        if (!(value instanceof BsonDocument document)) {
            RpgLogging.warn("Guild persistence failed: encoded data is not a document");
            return;
        }

        Path filePath = guildDir.resolve(guild.getId() + ".json");
        BsonUtil.writeDocument(Objects.requireNonNull(filePath), document);
    }

    public void deleteGuild(UUID guildId) {
        Path guildDir = resolveGuildDirectory();
        if (guildDir == null) {
            return;
        }

        Path filePath = guildDir.resolve(guildId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            RpgLogging.error(ex, "Failed to delete guild file %s", filePath);
        }
    }

    @Nullable
    private Path resolveGuildDirectory() {
        AssetPack pack = findWritablePack();
        if (pack == null) {
            RpgLogging.warn("Guild persistence disabled: no writable asset pack found");
            return null;
        }

        Path root = pack.getRoot();
        return root.resolve("Server").resolve("GuildData");
    }

    @Nullable
    private AssetPack findWritablePack() {
        try {
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
        } catch (Throwable ex) {
            return null;
        }
    }
}
