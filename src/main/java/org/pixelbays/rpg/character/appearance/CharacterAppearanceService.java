package org.pixelbays.rpg.character.appearance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;

@SuppressWarnings("null")
public class CharacterAppearanceService {

    @Nonnull
    public CharacterAppearanceData createDefaultAppearance(@Nullable String raceId) {
        CharacterAppearanceData appearance = CharacterAppearanceData.fromPlayerSkin(
                CosmeticsModule.get().generateRandomSkin(new Random()));
        return sanitizeAppearance(resolveRace(raceId), appearance);
    }

    @Nonnull
    public CharacterAppearanceData sanitizeAppearance(@Nullable RaceDefinition raceDefinition,
            @Nullable CharacterAppearanceData source) {
        CharacterAppearanceData appearance = source == null ? new CharacterAppearanceData() : source.copy();
        for (CharacterAppearanceCatalog.Category category : CharacterAppearanceCatalog.getOrderedCategories()) {
            String value = appearance.getValue(category.fieldName());
            if (isAllowed(raceDefinition, category, value) && exists(category, value)) {
                continue;
            }

            String replacement = findSafeReplacement(raceDefinition, category);
            appearance.setValue(category.fieldName(), replacement);
        }

        appearance = repairCrossCategoryConflicts(appearance, raceDefinition);
        return appearance;
    }

    public boolean isAllowed(@Nullable RaceDefinition raceDefinition,
            @Nonnull CharacterAppearanceCatalog.Category category,
            @Nullable String value) {
        if (value == null || value.isBlank()) {
            return !category.required();
        }
        if (raceDefinition == null) {
            return true;
        }

        String normalizedCategory = CharacterAppearanceCatalog.normalizeKey(category.fieldName());
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        String assetId = extractAssetId(value).toLowerCase(Locale.ROOT);

        if (matchesValue(raceDefinition.getNotAllowedCosmeticIds(), normalizedValue, assetId)) {
            return false;
        }
        if (matchesCategory(raceDefinition.getNotAllowedCosmeticCategories(), normalizedCategory)) {
            return false;
        }

        boolean hasAllowRules = !raceDefinition.getAllowedCosmeticCategories().isEmpty()
                || !raceDefinition.getAllowedCosmeticIds().isEmpty();
        if (!hasAllowRules) {
            return true;
        }

        return matchesCategory(raceDefinition.getAllowedCosmeticCategories(), normalizedCategory)
                || matchesValue(raceDefinition.getAllowedCosmeticIds(), normalizedValue, assetId);
    }

    @Nonnull
    public List<CharacterAppearanceCatalog.Option> getAllowedOptions(@Nullable RaceDefinition raceDefinition,
            @Nonnull CharacterAppearanceCatalog.Category category,
            @Nullable String searchQuery) {
        List<CharacterAppearanceCatalog.Option> filtered = new ArrayList<>();
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (CharacterAppearanceCatalog.Option option : CharacterAppearanceCatalog.getOptions(category)) {
            if (!isAllowed(raceDefinition, category, option.id())) {
                continue;
            }
            if (!normalizedQuery.isBlank()
                    && !option.label().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && !option.assetId().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                continue;
            }
            filtered.add(option);
        }
        return filtered;
    }

    @Nonnull
    public List<CharacterAppearanceCatalog.Cosmetic> getAllowedCosmetics(@Nullable RaceDefinition raceDefinition,
            @Nonnull CharacterAppearanceCatalog.Category category,
            @Nullable String searchQuery) {
        List<CharacterAppearanceCatalog.Cosmetic> filtered = new ArrayList<>();
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (CharacterAppearanceCatalog.Cosmetic cosmetic : CharacterAppearanceCatalog.getCosmetics(category)) {
            if (!isAllowed(raceDefinition, category, cosmetic.defaultOptionId())) {
                continue;
            }
            if (!normalizedQuery.isBlank()
                    && !cosmetic.label().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && !cosmetic.assetId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && cosmetic.tags().stream().noneMatch(tag -> tag != null
                            && tag.toLowerCase(Locale.ROOT).contains(normalizedQuery))) {
                continue;
            }
            filtered.add(cosmetic);
        }
        return filtered;
    }

    @Nullable
    public RaceDefinition resolveRace(@Nullable String raceId) {
        if (raceId == null || raceId.isBlank() || RaceDefinition.getAssetMap() == null) {
            return null;
        }
        return RaceDefinition.getAssetMap().getAsset(raceId);
    }

    public boolean exists(@Nonnull CharacterAppearanceCatalog.Category category, @Nullable String optionId) {
        if (optionId == null || optionId.isBlank()) {
            return !category.required();
        }
        return CharacterAppearanceCatalog.getOptions(category).stream().anyMatch(option -> option.id().equalsIgnoreCase(optionId));
    }

    @Nonnull
    public CharacterAppearanceData fromPlayerSkin(@Nullable PlayerSkin playerSkin, @Nullable String raceId) {
        return sanitizeAppearance(resolveRace(raceId), CharacterAppearanceData.fromPlayerSkin(playerSkin));
    }

    @Nonnull
    public String findSafeReplacement(@Nullable RaceDefinition raceDefinition,
            @Nonnull CharacterAppearanceCatalog.Category category) {
        List<CharacterAppearanceCatalog.Option> options = getAllowedOptions(raceDefinition, category, null);
        if (options.isEmpty()) {
            return category.required() ? "" : "";
        }

        for (CharacterAppearanceCatalog.Option option : options) {
            if (option.isDefaultAsset()) {
                return option.id();
            }
        }
        return options.getFirst().id();
    }

    @Nonnull
    private CharacterAppearanceData repairCrossCategoryConflicts(@Nonnull CharacterAppearanceData appearance,
            @Nullable RaceDefinition raceDefinition) {
        try {
            CosmeticsModule.get().validateSkin(appearance.toPlayerSkin());
            return appearance;
        } catch (CosmeticsModule.InvalidSkinException ignored) {
        }

        CharacterAppearanceData repaired = appearance.copy();
        repaired.setValue("HeadAccessory", "");
        try {
            CosmeticsModule.get().validateSkin(repaired.toPlayerSkin());
            return repaired;
        } catch (CosmeticsModule.InvalidSkinException ignored) {
        }

        repaired.setValue("Haircut", findSafeReplacement(raceDefinition, CharacterAppearanceCatalog.Category.HAIRCUT));
        repaired.setValue("HeadAccessory", findSafeReplacement(raceDefinition, CharacterAppearanceCatalog.Category.HEAD_ACCESSORY));
        return repaired;
    }

    private boolean matchesCategory(@Nonnull List<String> configuredValues, @Nonnull String normalizedCategory) {
        for (String configuredValue : configuredValues) {
            if (configuredValue == null) {
                continue;
            }
            CharacterAppearanceCatalog.Category category = CharacterAppearanceCatalog.findCategory(configuredValue);
            if (category != null && CharacterAppearanceCatalog.normalizeKey(category.fieldName()).equals(normalizedCategory)) {
                return true;
            }
            if (CharacterAppearanceCatalog.normalizeKey(configuredValue).equals(normalizedCategory)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesValue(@Nonnull List<String> configuredValues,
            @Nonnull String normalizedValue,
            @Nonnull String assetId) {
        for (String configuredValue : configuredValues) {
            if (configuredValue == null || configuredValue.isBlank()) {
                continue;
            }
            String normalizedConfigured = configuredValue.toLowerCase(Locale.ROOT).trim();
            if (Objects.equals(normalizedConfigured, normalizedValue) || Objects.equals(normalizedConfigured, assetId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private String extractAssetId(@Nonnull String optionId) {
        int delimiter = optionId.indexOf('.');
        return delimiter < 0 ? optionId : optionId.substring(0, delimiter);
    }
}
