package org.pixelbays.rpg.character.appearance;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.PlayerSkin;

@SuppressWarnings({ "null", "deprecation" })
public class CharacterAppearanceData {

    public static final BuilderCodec<CharacterAppearanceData> CODEC = BuilderCodec
            .builder(CharacterAppearanceData.class, CharacterAppearanceData::new)
            .append(new KeyedCodec<>("BodyCharacteristic", Codec.STRING, false, true),
                    (i, s) -> i.bodyCharacteristic = normalize(s), i -> i.bodyCharacteristic)
            .add()
            .append(new KeyedCodec<>("Underwear", Codec.STRING, false, true),
                    (i, s) -> i.underwear = normalize(s), i -> i.underwear)
            .add()
            .append(new KeyedCodec<>("Face", Codec.STRING, false, true),
                    (i, s) -> i.face = normalize(s), i -> i.face)
            .add()
            .append(new KeyedCodec<>("Eyes", Codec.STRING, false, true),
                    (i, s) -> i.eyes = normalize(s), i -> i.eyes)
            .add()
            .append(new KeyedCodec<>("Ears", Codec.STRING, false, true),
                    (i, s) -> i.ears = normalize(s), i -> i.ears)
            .add()
            .append(new KeyedCodec<>("Mouth", Codec.STRING, false, true),
                    (i, s) -> i.mouth = normalize(s), i -> i.mouth)
            .add()
            .append(new KeyedCodec<>("FacialHair", Codec.STRING, false, true),
                    (i, s) -> i.facialHair = normalize(s), i -> i.facialHair)
            .add()
            .append(new KeyedCodec<>("Haircut", Codec.STRING, false, true),
                    (i, s) -> i.haircut = normalize(s), i -> i.haircut)
            .add()
            .append(new KeyedCodec<>("Eyebrows", Codec.STRING, false, true),
                    (i, s) -> i.eyebrows = normalize(s), i -> i.eyebrows)
            .add()
            .append(new KeyedCodec<>("Pants", Codec.STRING, false, true),
                    (i, s) -> i.pants = normalize(s), i -> i.pants)
            .add()
            .append(new KeyedCodec<>("Overpants", Codec.STRING, false, true),
                    (i, s) -> i.overpants = normalize(s), i -> i.overpants)
            .add()
            .append(new KeyedCodec<>("Undertop", Codec.STRING, false, true),
                    (i, s) -> i.undertop = normalize(s), i -> i.undertop)
            .add()
            .append(new KeyedCodec<>("Overtop", Codec.STRING, false, true),
                    (i, s) -> i.overtop = normalize(s), i -> i.overtop)
            .add()
            .append(new KeyedCodec<>("Shoes", Codec.STRING, false, true),
                    (i, s) -> i.shoes = normalize(s), i -> i.shoes)
            .add()
            .append(new KeyedCodec<>("HeadAccessory", Codec.STRING, false, true),
                    (i, s) -> i.headAccessory = normalize(s), i -> i.headAccessory)
            .add()
            .append(new KeyedCodec<>("FaceAccessory", Codec.STRING, false, true),
                    (i, s) -> i.faceAccessory = normalize(s), i -> i.faceAccessory)
            .add()
            .append(new KeyedCodec<>("EarAccessory", Codec.STRING, false, true),
                    (i, s) -> i.earAccessory = normalize(s), i -> i.earAccessory)
            .add()
            .append(new KeyedCodec<>("SkinFeature", Codec.STRING, false, true),
                    (i, s) -> i.skinFeature = normalize(s), i -> i.skinFeature)
            .add()
            .append(new KeyedCodec<>("Gloves", Codec.STRING, false, true),
                    (i, s) -> i.gloves = normalize(s), i -> i.gloves)
            .add()
            .append(new KeyedCodec<>("Cape", Codec.STRING, false, true),
                    (i, s) -> i.cape = normalize(s), i -> i.cape)
            .add()
            .build();

    private String bodyCharacteristic;
    private String underwear;
    private String face;
    private String eyes;
    private String ears;
    private String mouth;
    private String facialHair;
    private String haircut;
    private String eyebrows;
    private String pants;
    private String overpants;
    private String undertop;
    private String overtop;
    private String shoes;
    private String headAccessory;
    private String faceAccessory;
    private String earAccessory;
    private String skinFeature;
    private String gloves;
    private String cape;

    public CharacterAppearanceData() {
        this.bodyCharacteristic = "";
        this.underwear = "";
        this.face = "";
        this.eyes = "";
        this.ears = "";
        this.mouth = "";
        this.facialHair = "";
        this.haircut = "";
        this.eyebrows = "";
        this.pants = "";
        this.overpants = "";
        this.undertop = "";
        this.overtop = "";
        this.shoes = "";
        this.headAccessory = "";
        this.faceAccessory = "";
        this.earAccessory = "";
        this.skinFeature = "";
        this.gloves = "";
        this.cape = "";
    }

    @Nonnull
    public static CharacterAppearanceData fromPlayerSkin(@Nullable PlayerSkin playerSkin) {
        CharacterAppearanceData appearance = new CharacterAppearanceData();
        if (playerSkin == null) {
            return appearance;
        }

        appearance.bodyCharacteristic = normalize(playerSkin.bodyCharacteristic);
        appearance.underwear = normalize(playerSkin.underwear);
        appearance.face = normalize(playerSkin.face);
        appearance.eyes = normalize(playerSkin.eyes);
        appearance.ears = normalize(playerSkin.ears);
        appearance.mouth = normalize(playerSkin.mouth);
        appearance.facialHair = normalize(playerSkin.facialHair);
        appearance.haircut = normalize(playerSkin.haircut);
        appearance.eyebrows = normalize(playerSkin.eyebrows);
        appearance.pants = normalize(playerSkin.pants);
        appearance.overpants = normalize(playerSkin.overpants);
        appearance.undertop = normalize(playerSkin.undertop);
        appearance.overtop = normalize(playerSkin.overtop);
        appearance.shoes = normalize(playerSkin.shoes);
        appearance.headAccessory = normalize(playerSkin.headAccessory);
        appearance.faceAccessory = normalize(playerSkin.faceAccessory);
        appearance.earAccessory = normalize(playerSkin.earAccessory);
        appearance.skinFeature = normalize(playerSkin.skinFeature);
        appearance.gloves = normalize(playerSkin.gloves);
        appearance.cape = normalize(playerSkin.cape);
        return appearance;
    }

    @Nonnull
    public CharacterAppearanceData copy() {
        CharacterAppearanceData copy = new CharacterAppearanceData();
        copy.bodyCharacteristic = bodyCharacteristic;
        copy.underwear = underwear;
        copy.face = face;
        copy.eyes = eyes;
        copy.ears = ears;
        copy.mouth = mouth;
        copy.facialHair = facialHair;
        copy.haircut = haircut;
        copy.eyebrows = eyebrows;
        copy.pants = pants;
        copy.overpants = overpants;
        copy.undertop = undertop;
        copy.overtop = overtop;
        copy.shoes = shoes;
        copy.headAccessory = headAccessory;
        copy.faceAccessory = faceAccessory;
        copy.earAccessory = earAccessory;
        copy.skinFeature = skinFeature;
        copy.gloves = gloves;
        copy.cape = cape;
        return copy;
    }

    @Nonnull
    public PlayerSkin toPlayerSkin() {
        return new PlayerSkin(
                nullable(bodyCharacteristic),
                nullable(underwear),
                nullable(face),
                nullable(eyes),
                nullable(ears),
                nullable(mouth),
                nullable(facialHair),
                nullable(haircut),
                nullable(eyebrows),
                nullable(pants),
                nullable(overpants),
                nullable(undertop),
                nullable(overtop),
                nullable(shoes),
                nullable(headAccessory),
                nullable(faceAccessory),
                nullable(earAccessory),
                nullable(skinFeature),
                nullable(gloves),
                nullable(cape));
    }

    public int countDifferences(@Nullable CharacterAppearanceData other) {
        if (other == null) {
            return asMap().size();
        }

        int count = 0;
        for (Map.Entry<String, String> entry : asMap().entrySet()) {
            if (!entry.getValue().equals(other.asMap().getOrDefault(entry.getKey(), ""))) {
                count++;
            }
        }
        return count;
    }

    @Nonnull
    public Map<String, String> asMap() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("BodyCharacteristic", bodyCharacteristic);
        values.put("Underwear", underwear);
        values.put("Face", face);
        values.put("Eyes", eyes);
        values.put("Ears", ears);
        values.put("Mouth", mouth);
        values.put("FacialHair", facialHair);
        values.put("Haircut", haircut);
        values.put("Eyebrows", eyebrows);
        values.put("Pants", pants);
        values.put("Overpants", overpants);
        values.put("Undertop", undertop);
        values.put("Overtop", overtop);
        values.put("Shoes", shoes);
        values.put("HeadAccessory", headAccessory);
        values.put("FaceAccessory", faceAccessory);
        values.put("EarAccessory", earAccessory);
        values.put("SkinFeature", skinFeature);
        values.put("Gloves", gloves);
        values.put("Cape", cape);
        return values;
    }

    @Nonnull
    public String getValue(@Nonnull String field) {
        return switch (field) {
            case "BodyCharacteristic" -> bodyCharacteristic;
            case "Underwear" -> underwear;
            case "Face" -> face;
            case "Eyes" -> eyes;
            case "Ears" -> ears;
            case "Mouth" -> mouth;
            case "FacialHair" -> facialHair;
            case "Haircut" -> haircut;
            case "Eyebrows" -> eyebrows;
            case "Pants" -> pants;
            case "Overpants" -> overpants;
            case "Undertop" -> undertop;
            case "Overtop" -> overtop;
            case "Shoes" -> shoes;
            case "HeadAccessory" -> headAccessory;
            case "FaceAccessory" -> faceAccessory;
            case "EarAccessory" -> earAccessory;
            case "SkinFeature" -> skinFeature;
            case "Gloves" -> gloves;
            case "Cape" -> cape;
            default -> "";
        };
    }

    public void setValue(@Nonnull String field, @Nullable String value) {
        String normalizedValue = normalize(value);
        switch (field) {
            case "BodyCharacteristic" -> bodyCharacteristic = normalizedValue;
            case "Underwear" -> underwear = normalizedValue;
            case "Face" -> face = normalizedValue;
            case "Eyes" -> eyes = normalizedValue;
            case "Ears" -> ears = normalizedValue;
            case "Mouth" -> mouth = normalizedValue;
            case "FacialHair" -> facialHair = normalizedValue;
            case "Haircut" -> haircut = normalizedValue;
            case "Eyebrows" -> eyebrows = normalizedValue;
            case "Pants" -> pants = normalizedValue;
            case "Overpants" -> overpants = normalizedValue;
            case "Undertop" -> undertop = normalizedValue;
            case "Overtop" -> overtop = normalizedValue;
            case "Shoes" -> shoes = normalizedValue;
            case "HeadAccessory" -> headAccessory = normalizedValue;
            case "FaceAccessory" -> faceAccessory = normalizedValue;
            case "EarAccessory" -> earAccessory = normalizedValue;
            case "SkinFeature" -> skinFeature = normalizedValue;
            case "Gloves" -> gloves = normalizedValue;
            case "Cape" -> cape = normalizedValue;
            default -> {
            }
        }
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private static String nullable(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}