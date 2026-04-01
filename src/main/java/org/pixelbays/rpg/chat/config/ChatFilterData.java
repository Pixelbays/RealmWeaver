package org.pixelbays.rpg.chat.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "null", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class ChatFilterData implements JsonAssetWithMap<String, DefaultAssetMap<String, ChatFilterData>> {

    private static final FunctionCodec<String[], List<String>> WORD_LIST_CODEC = new FunctionCodec<>(
            Codec.STRING_ARRAY,
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(String[]::new));

    public static final AssetBuilderCodec<String, ChatFilterData> CODEC = AssetBuilderCodec.builder(
            ChatFilterData.class,
            ChatFilterData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("CustomWords", WORD_LIST_CODEC, false, true),
                    (i, s) -> i.customWords = s, i -> i.customWords)
            .add()
            .build();

    private static DefaultAssetMap<String, ChatFilterData> assetMap;

    private AssetExtraInfo.Data data;
    private String id;
    private List<String> customWords;

    public ChatFilterData() {
        this.id = "Default";
        this.customWords = new ArrayList<>();
    }

    public static DefaultAssetMap<String, ChatFilterData> getAssetMap() {
        if (assetMap == null) {
            var assetStore = AssetRegistry.getAssetStore(ChatFilterData.class);
            if (assetStore == null) {
                return null;
            }
            assetMap = (DefaultAssetMap<String, ChatFilterData>) assetStore.getAssetMap();
        }
        return assetMap;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<String> getCustomWords() {
        return customWords == null ? List.of() : new ArrayList<>(customWords);
    }

    public void setCustomWords(List<String> words) {
        this.customWords = words == null ? new ArrayList<>() : new ArrayList<>(words);
    }
}