package org.pixelbays.rpg.hud;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.config.ClassDefinition.ResourceDisplayDefinition;
import org.pixelbays.rpg.classes.config.ClassDefinition.ResourceDisplayMode;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

public final class ResourceBarsHudServiceModule implements PlayerHudServiceModule {

    @Override
    public void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getResourceBarsModule().prime(resolveResourceBars(context));
    }

    @Override
    public void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getResourceBarsModule().update(resolveResourceBars(context));
    }

    @Nonnull
    private static List<ResourceBarsHudModule.ResourceBarData> resolveResourceBars(@Nonnull PlayerHudContext context) {
        String activeClassId = context.getActiveClassId();
        if (activeClassId == null || activeClassId.isEmpty()) {
            return List.of();
        }

        ClassDefinition classDef = PlayerHudServiceSupport.getClassDefinition(activeClassId);
        if (classDef == null) {
            return List.of();
        }

        List<ResourceDisplayDefinition> resourceDisplays = classDef.getResolvedResourceDisplays();
        if (resourceDisplays == null || resourceDisplays.isEmpty()) {
            return List.of();
        }

        EntityStatMap statMap = context.getStore().getComponent(context.getRef(), EntityStatMap.getComponentType());
        if (statMap == null) {
            return List.of();
        }

        List<ResourceBarsHudModule.ResourceBarData> result = new ArrayList<>(resourceDisplays.size());
        for (ResourceDisplayDefinition resourceDisplay : resourceDisplays) {
            if (resourceDisplay == null) {
                continue;
            }

            String statId = resourceDisplay.getStatId();
            if (statId.isBlank()) {
                continue;
            }

            int statIndex = EntityStatType.getAssetMap().getIndex(statId);
            EntityStatValue value = statMap.get(statIndex);
            if (value == null) {
                continue;
            }

            String label = resolveResourceLabel(resourceDisplay, statId);
            String fillColor = resolveResourceFillColor(resourceDisplay, statId);
            String assetPath = resolveResourceAssetPath(resourceDisplay);
            if (resourceDisplay.getDisplayMode() == ResourceDisplayMode.CHARGES) {
                int maxCharges = resolveChargeCapacity(resourceDisplay, value);
                if (maxCharges <= 0) {
                    continue;
                }

                int currentCharges = Math.max(0,
                        Math.min(maxCharges, (int) Math.floor(Math.max(0f, value.get()) + 0.0001f)));
                result.add(new ResourceBarsHudModule.ResourceBarData(
                        statId,
                        label,
                        ResourceBarsHudModule.ResourceDisplayMode.CHARGES,
                        fillColor,
                        assetPath,
                        0f,
                        currentCharges,
                        maxCharges));
                continue;
            }

            result.add(new ResourceBarsHudModule.ResourceBarData(
                    statId,
                    label,
                    ResourceBarsHudModule.ResourceDisplayMode.BAR,
                    fillColor,
                    assetPath,
                    value.asPercentage(),
                    0,
                    0));
        }

        return result;
    }

    @Nonnull
    private static String resolveResourceLabel(
            @Nonnull ResourceDisplayDefinition resourceDisplay,
            @Nonnull String statId) {
        String label = resourceDisplay.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        return PlayerHudServiceSupport.humanizeIdentifier(statId);
    }

    @Nonnull
    private static String resolveResourceFillColor(
            @Nonnull ResourceDisplayDefinition resourceDisplay,
            @Nonnull String statId) {
        String fillColor = resourceDisplay.getFillColor();
        if (fillColor != null && !fillColor.isBlank()) {
            return fillColor;
        }
        return PlayerHudServiceSupport.resolveStatColor(statId);
    }

    @Nonnull
    private static String resolveResourceAssetPath(@Nonnull ResourceDisplayDefinition resourceDisplay) {
        String assetPath = resourceDisplay.getAsset();
        return assetPath == null ? "" : assetPath.trim();
    }

    private static int resolveChargeCapacity(
            @Nonnull ResourceDisplayDefinition resourceDisplay,
            @Nonnull EntityStatValue value) {
        if (resourceDisplay.getMaxCharges() > 0) {
            return resourceDisplay.getMaxCharges();
        }

        float maxValue = Math.max(0f, value.getMax());
        return Math.max(0, (int) Math.ceil(maxValue));
    }
}