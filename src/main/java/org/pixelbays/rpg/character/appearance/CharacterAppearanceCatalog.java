package org.pixelbays.rpg.character.appearance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

@SuppressWarnings("null")
public final class CharacterAppearanceCatalog {

    private static final String DEFAULT_LANGUAGE = "en-US";

    private CharacterAppearanceCatalog() {
    }

    @Nonnull
    public static List<Category> getOrderedCategories() {
        return List.of(Category.values());
    }

    @Nullable
    public static Category findCategory(@Nullable String rawCategoryId) {
        if (rawCategoryId == null || rawCategoryId.isBlank()) {
            return null;
        }
        String normalized = normalizeKey(rawCategoryId);
        for (Category category : Category.values()) {
            if (category.matches(normalized)) {
                return category;
            }
        }
        return null;
    }

    @Nonnull
    public static List<Option> getOptions(@Nonnull Category category) {
        List<Option> options = new ArrayList<>();
        for (Cosmetic cosmetic : getCosmetics(category)) {
            for (SelectionOption selection : cosmetic.selections()) {
                options.add(new Option(
                        category,
                        selection.optionId(),
                        cosmetic.assetId(),
                        buildLabel(cosmetic.label(), cosmetic.assetId(), selection.textureId(), selection.variantId()),
                        cosmetic.isDefaultAsset(),
                        cosmetic.tags()));
            }
        }

        options.sort(Comparator.comparing(Option::isDefaultAsset).reversed()
                .thenComparing(Option::label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    @Nonnull
    public static List<Cosmetic> getCosmetics(@Nonnull Category category) {
        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
        Map<String, PlayerSkinPart> parts = category.getParts(registry);
        List<Cosmetic> cosmetics = new ArrayList<>();
        for (PlayerSkinPart part : parts.values()) {
            if (part == null) {
                continue;
            }

            LinkedHashMap<String, TextureChoice> colors = new LinkedHashMap<>();
            LinkedHashMap<String, VariantChoice> variants = new LinkedHashMap<>();
            List<SelectionOption> selections = new ArrayList<>();

            if (part.getVariants() != null && !part.getVariants().isEmpty()) {
                for (Map.Entry<String, PlayerSkinPart.Variant> variantEntry : part.getVariants().entrySet()) {
                    PlayerSkinPart.Variant variant = variantEntry.getValue();
                    if (variant == null) {
                        continue;
                    }

                    String variantId = variantEntry.getKey();
                    String variantLabel = resolveChoiceLabel(variantId);
                    if (!variantId.isBlank()) {
                        variants.putIfAbsent(variantId, new VariantChoice(variantId, variantLabel));
                    }

                    if (variant.getTextures() != null && !variant.getTextures().isEmpty()) {
                        for (Map.Entry<String, PlayerSkinPartTexture> textureEntry : variant.getTextures().entrySet()) {
                            String textureId = textureEntry.getKey();
                            String textureLabel = resolveChoiceLabel(textureId);
                            PlayerSkinPartTexture texture = textureEntry.getValue();
                            String texturePath = resolveUiTexturePath(texture);
                            colors.putIfAbsent(textureId,
                                new TextureChoice(textureId, textureLabel, texturePath, resolveBaseColors(texture)));
                            selections.add(new SelectionOption(
                                    textureId,
                                    textureLabel,
                                    variantId,
                                    variantLabel,
                                    texturePath,
                                    buildOptionId(part.getId(), textureId, variantId)));
                        }
                    }

                    appendGradientSelections(
                            registry,
                            part.getId(),
                            variantId,
                            variantLabel,
                            variant.getGreyscaleTexture(),
                            part.getGradientSet(),
                            colors,
                            selections);
                }
            } else {
                if (part.getTextures() != null) {
                    for (Map.Entry<String, PlayerSkinPartTexture> textureEntry : part.getTextures().entrySet()) {
                        String textureId = textureEntry.getKey();
                        String textureLabel = resolveChoiceLabel(textureId);
                        PlayerSkinPartTexture texture = textureEntry.getValue();
                        String texturePath = resolveUiTexturePath(texture);
                        colors.putIfAbsent(textureId,
                            new TextureChoice(textureId, textureLabel, texturePath, resolveBaseColors(texture)));
                        selections.add(new SelectionOption(
                                textureId,
                                textureLabel,
                                "",
                                "",
                                texturePath,
                                buildOptionId(part.getId(), textureId, "")));
                    }
                }

                appendGradientSelections(
                        registry,
                        part.getId(),
                        "",
                        "",
                        part.getGreyscaleTexture(),
                        part.getGradientSet(),
                        colors,
                        selections);
            }

            if (selections.isEmpty()) {
                continue;
            }

            SelectionOption defaultSelection = resolveDefaultSelection(selections);
            cosmetics.add(new Cosmetic(
                    category,
                    part.getId(),
                    resolveDisplayName(part.getName(), part.getId()),
                    part.isDefaultAsset(),
                    safeTags(part.getTags()),
                    List.copyOf(colors.values()),
                    List.copyOf(variants.values()),
                    List.copyOf(selections),
                    defaultSelection.optionId(),
                    defaultSelection.textureId(),
                    defaultSelection.variantId()));
        }

        cosmetics.sort(Comparator.comparing(Cosmetic::isDefaultAsset).reversed()
                .thenComparing(Cosmetic::label, String.CASE_INSENSITIVE_ORDER));
        return cosmetics;
    }

    @Nonnull
    public static ParsedSelection parseOptionId(@Nullable String optionId) {
        if (optionId == null || optionId.isBlank()) {
            return new ParsedSelection("", "", "");
        }

        String[] segments = optionId.trim().split("\\.");
        String assetId = segments.length >= 1 ? segments[0] : "";
        String textureId = segments.length >= 2 ? segments[1] : "";
        String variantId = "";
        if (segments.length >= 3) {
            StringBuilder builder = new StringBuilder();
            for (int index = 2; index < segments.length; index++) {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(segments[index]);
            }
            variantId = builder.toString();
        }
        return new ParsedSelection(assetId, textureId, variantId);
    }

    @Nonnull
    public static String buildOptionId(@Nonnull String assetId, @Nonnull String textureId, @Nullable String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return assetId + "." + textureId;
        }
        return assetId + "." + textureId + "." + variantId;
    }

    @Nonnull
    private static String buildLabel(@Nonnull String nameKey,
            @Nonnull String assetId,
            @Nonnull String textureId,
            @Nullable String variantId) {
        String translatedName = nameKey.contains(".") ? resolveDisplayName(nameKey, assetId) : nameKey;
        String translatedTexture = resolveChoiceLabel(textureId);
        if (variantId == null || variantId.isBlank()) {
            return translatedName + " - " + translatedTexture;
        }
        return translatedName + " - " + translatedTexture + " (" + resolveChoiceLabel(variantId) + ")";
    }

    @Nonnull
    private static String resolveDisplayName(@Nonnull String nameKey, @Nonnull String assetId) {
        I18nModule i18n = I18nModule.get();
        if (i18n != null && !nameKey.isBlank()) {
            String translated = i18n.getMessage(DEFAULT_LANGUAGE, nameKey);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        return humanizeToken(assetId);
    }

    @Nonnull
    private static String resolveChoiceLabel(@Nonnull String value) {
        if (value.isBlank()) {
            return "";
        }
        return switch (normalizeKey(value)) {
            case "default" -> "Default";
            default -> humanizeToken(value);
        };
    }

    @Nonnull
    private static SelectionOption resolveDefaultSelection(@Nonnull List<SelectionOption> selections) {
        for (SelectionOption selection : selections) {
            if (normalizeKey(selection.textureId()).equals("default")
                    && (selection.variantId().isBlank() || normalizeKey(selection.variantId()).equals("default"))) {
                return selection;
            }
        }
        for (SelectionOption selection : selections) {
            if (selection.variantId().isBlank() || normalizeKey(selection.variantId()).equals("default")) {
                return selection;
            }
        }
        return selections.getFirst();
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nonnull
    private static String resolveUiTexturePath(@Nullable PlayerSkinPartTexture texture) {
        String texturePath = texture == null ? "" : safeString(texture.getTexture()).trim();
        return resolveUiTexturePath(texturePath);
    }

    @Nonnull
    private static String resolveUiTexturePath(@Nullable String texturePath) {
        texturePath = safeString(texturePath).trim();
        if (texturePath.isBlank()) {
            return "";
        }
        if (texturePath.startsWith("Common/")) {
            return texturePath.substring("Common/".length());
        }
        return texturePath;
    }

    @Nonnull
    private static List<String> resolveBaseColors(@Nullable PlayerSkinPartTexture texture) {
        if (texture == null || texture.getBaseColor() == null || texture.getBaseColor().length == 0) {
            return List.of();
        }

        List<String> colors = Arrays.stream(texture.getBaseColor())
                .filter(baseColor -> baseColor != null && !baseColor.isBlank())
                .map(String::trim)
                .toList();
        return colors.isEmpty() ? List.of() : List.copyOf(colors);
    }

    private static void appendGradientSelections(
            @Nonnull CosmeticRegistry registry,
            @Nonnull String assetId,
            @Nullable String variantId,
            @Nullable String variantLabel,
            @Nullable String greyscaleTexturePath,
            @Nullable String gradientSetId,
            @Nonnull LinkedHashMap<String, TextureChoice> colors,
            @Nonnull List<SelectionOption> selections) {
        String texturePath = resolveUiTexturePath(greyscaleTexturePath);
        if (texturePath.isBlank() || gradientSetId == null || gradientSetId.isBlank()) {
            return;
        }

        PlayerSkinGradientSet gradientSet = registry.getGradientSets().get(gradientSetId);
        if (gradientSet == null || gradientSet.getGradients() == null || gradientSet.getGradients().isEmpty()) {
            return;
        }

        String safeVariantId = safeString(variantId);
        String safeVariantLabel = safeString(variantLabel);
        for (Map.Entry<String, PlayerSkinPartTexture> gradientEntry : gradientSet.getGradients().entrySet()) {
            String textureId = gradientEntry.getKey();
            String optionId = buildOptionId(assetId, textureId, safeVariantId);
            if (containsSelectionOption(selections, optionId)) {
                continue;
            }

            String textureLabel = resolveChoiceLabel(textureId);
            PlayerSkinPartTexture gradientTexture = gradientEntry.getValue();
            colors.putIfAbsent(textureId,
                new TextureChoice(textureId, textureLabel, texturePath, resolveBaseColors(gradientTexture)));
            selections.add(new SelectionOption(
                    textureId,
                    textureLabel,
                    safeVariantId,
                    safeVariantLabel,
                    texturePath,
                    optionId));
        }
    }

    private static boolean containsSelectionOption(
            @Nonnull List<SelectionOption> selections,
            @Nonnull String optionId) {
        for (SelectionOption selection : selections) {
            if (selection.optionId().equalsIgnoreCase(optionId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static String humanizeToken(@Nonnull String value) {
        String normalized = value
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('.', ' ')
                .trim();
        if (normalized.isBlank()) {
            return value;
        }

        StringBuilder builder = new StringBuilder(normalized.length());
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    @Nonnull
    private static List<String> safeTags(@Nullable String[] tags) {
        return tags == null ? List.of() : Arrays.asList(tags.clone());
    }

    @Nonnull
    public static String normalizeKey(@Nonnull String value) {
        return value.replace("_", "").replace("-", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    public record Option(
            @Nonnull Category category,
            @Nonnull String id,
            @Nonnull String assetId,
            @Nonnull String label,
            boolean isDefaultAsset,
            @Nonnull List<String> tags) {
    }

        public record TextureChoice(
            @Nonnull String id,
            @Nonnull String label,
            @Nonnull String texturePath,
            @Nonnull List<String> swatchColors) {
    }

    public record VariantChoice(@Nonnull String id, @Nonnull String label) {
    }

    public record SelectionOption(
            @Nonnull String textureId,
            @Nonnull String textureLabel,
            @Nonnull String variantId,
            @Nonnull String variantLabel,
            @Nonnull String texturePath,
            @Nonnull String optionId) {
    }

    public record ParsedSelection(
            @Nonnull String assetId,
            @Nonnull String textureId,
            @Nonnull String variantId) {
    }

    public record Cosmetic(
            @Nonnull Category category,
            @Nonnull String assetId,
            @Nonnull String label,
            boolean isDefaultAsset,
            @Nonnull List<String> tags,
            @Nonnull List<TextureChoice> colors,
            @Nonnull List<VariantChoice> variants,
            @Nonnull List<SelectionOption> selections,
            @Nonnull String defaultOptionId,
            @Nonnull String defaultColorId,
            @Nonnull String defaultVariantId) {

        public boolean supportsColorSelection() {
            return colors.size() > 1;
        }

        public boolean supportsVariantSelection() {
            return variants.size() > 1;
        }

        public boolean hasColor(@Nullable String colorId) {
            if (colorId == null || colorId.isBlank()) {
                return false;
            }
            return colors.stream().anyMatch(color -> color.id().equalsIgnoreCase(colorId));
        }

        public boolean hasVariant(@Nullable String variantId) {
            if (variantId == null || variantId.isBlank()) {
                return !supportsVariantSelection();
            }
            return variants.stream().anyMatch(variant -> variant.id().equalsIgnoreCase(variantId));
        }

        @Nonnull
        public String resolveOptionId(@Nullable String preferredColorId, @Nullable String preferredVariantId) {
            SelectionOption exact = findSelection(preferredColorId, preferredVariantId);
            if (exact != null) {
                return exact.optionId();
            }

            if (preferredColorId != null && !preferredColorId.isBlank()) {
                for (SelectionOption selection : selections) {
                    if (selection.textureId().equalsIgnoreCase(preferredColorId)) {
                        return selection.optionId();
                    }
                }
            }

            if (preferredVariantId != null && !preferredVariantId.isBlank()) {
                for (SelectionOption selection : selections) {
                    if (selection.variantId().equalsIgnoreCase(preferredVariantId)) {
                        return selection.optionId();
                    }
                }
            }

            return defaultOptionId;
        }

        @Nonnull
        public SelectionOption resolveSelection(@Nullable String preferredColorId, @Nullable String preferredVariantId) {
            SelectionOption selection = findSelection(preferredColorId, preferredVariantId);
            return selection != null ? selection : selections.getFirst();
        }

        @Nullable
        public SelectionOption findSelection(@Nullable String preferredColorId, @Nullable String preferredVariantId) {
            for (SelectionOption selection : selections) {
                boolean colorMatches = preferredColorId == null || preferredColorId.isBlank()
                        ? selection.textureId().equalsIgnoreCase(defaultColorId)
                        : selection.textureId().equalsIgnoreCase(preferredColorId);
                boolean variantMatches = preferredVariantId == null || preferredVariantId.isBlank()
                        ? selection.variantId().equalsIgnoreCase(defaultVariantId)
                        : selection.variantId().equalsIgnoreCase(preferredVariantId);
                if (colorMatches && variantMatches) {
                    return selection;
                }
            }
            return null;
        }
    }

    public enum Category {
        BODY_CHARACTERISTIC("BodyCharacteristic", "rpg.character.appearance.category.body", true, "bodycharacteristics"),
        UNDERWEAR("Underwear", "rpg.character.appearance.category.underwear", true),
        FACE("Face", "rpg.character.appearance.category.face", true, "faces"),
        EYES("Eyes", "rpg.character.appearance.category.eyes", true),
        EARS("Ears", "rpg.character.appearance.category.ears", true),
        MOUTH("Mouth", "rpg.character.appearance.category.mouth", true, "mouths"),
        FACIAL_HAIR("FacialHair", "rpg.character.appearance.category.facialHair", false),
        HAIRCUT("Haircut", "rpg.character.appearance.category.haircut", false, "haircuts"),
        EYEBROWS("Eyebrows", "rpg.character.appearance.category.eyebrows", false),
        PANTS("Pants", "rpg.character.appearance.category.pants", false),
        OVERPANTS("Overpants", "rpg.character.appearance.category.overpants", false),
        UNDERTOP("Undertop", "rpg.character.appearance.category.undertop", true, "undertops"),
        OVERTOP("Overtop", "rpg.character.appearance.category.overtop", false, "overtops"),
        SHOES("Shoes", "rpg.character.appearance.category.shoes", false),
        HEAD_ACCESSORY("HeadAccessory", "rpg.character.appearance.category.headAccessory", false),
        FACE_ACCESSORY("FaceAccessory", "rpg.character.appearance.category.faceAccessory", false),
        EAR_ACCESSORY("EarAccessory", "rpg.character.appearance.category.earAccessory", false),
        SKIN_FEATURE("SkinFeature", "rpg.character.appearance.category.skinFeature", false, "skinfeatures"),
        GLOVES("Gloves", "rpg.character.appearance.category.gloves", false),
        CAPE("Cape", "rpg.character.appearance.category.cape", false, "capes");

        private final String fieldName;
        private final String translationKey;
        private final boolean required;
        private final List<String> aliases;

        Category(@Nonnull String fieldName, @Nonnull String translationKey, boolean required, String... aliases) {
            this.fieldName = fieldName;
            this.translationKey = translationKey;
            this.required = required;
            List<String> values = new ArrayList<>();
            values.add(normalizeKey(name()));
            values.add(normalizeKey(fieldName));
            for (String alias : aliases) {
                values.add(normalizeKey(alias));
            }
            this.aliases = List.copyOf(values);
        }

        @Nonnull
        public String fieldName() {
            return fieldName;
        }

        @Nonnull
        public String translationKey() {
            return translationKey;
        }

        public boolean required() {
            return required;
        }

        public boolean matches(@Nonnull String normalizedCategory) {
            return aliases.contains(normalizedCategory);
        }

        @Nonnull
        public Map<String, PlayerSkinPart> getParts(@Nonnull CosmeticRegistry registry) {
            return switch (this) {
                case BODY_CHARACTERISTIC -> registry.getBodyCharacteristics();
                case UNDERWEAR -> registry.getUnderwear();
                case FACE -> registry.getFaces();
                case EYES -> registry.getEyes();
                case EARS -> registry.getEars();
                case MOUTH -> registry.getMouths();
                case FACIAL_HAIR -> registry.getFacialHairs();
                case HAIRCUT -> registry.getHaircuts();
                case EYEBROWS -> registry.getEyebrows();
                case PANTS -> registry.getPants();
                case OVERPANTS -> registry.getOverpants();
                case UNDERTOP -> registry.getUndertops();
                case OVERTOP -> registry.getOvertops();
                case SHOES -> registry.getShoes();
                case HEAD_ACCESSORY -> registry.getHeadAccessories();
                case FACE_ACCESSORY -> registry.getFaceAccessories();
                case EAR_ACCESSORY -> registry.getEarAccessories();
                case SKIN_FEATURE -> registry.getSkinFeatures();
                case GLOVES -> registry.getGloves();
                case CAPE -> registry.getCapes();
            };
        }
    }
}
