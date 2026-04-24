package org.pixelbays.rpg.ability.binding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.race.component.RaceComponent;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Shared binding rules for RPG abilities.
 * Centralizes unlock checks, allowed target resolution, and binding map access.
 */
@SuppressWarnings("null")
public class AbilityBindingService {

    public static final String DEFAULT_CONFIG_ID = "default";
    private static final String DEFAULT_LANGUAGE = "en-US";

    @Nonnull
    public AbilityControlType getControlType() {
        return getControlType(DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public AbilityControlType getControlType(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return getControlType(entityRef, store, DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public AbilityControlType getControlType(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String configId) {
        return resolveControlType(resolveActiveClassId(entityRef, store), configId);
    }

    @Nonnull
    public AbilityControlType getControlType(@Nonnull String configId) {
        return resolveControlType(null, configId);
    }

    @Nonnull
    public List<BindingTarget> getAllowedTargets() {
        return getAllowedTargets(DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public List<BindingTarget> getAllowedTargets(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return getAllowedTargets(entityRef, store, DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public List<BindingTarget> getAllowedTargets(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String configId) {
        return buildAllowedTargets(resolveConfig(configId), getControlType(entityRef, store, configId));
    }

    @Nonnull
    public List<BindingTarget> getAllowedTargets(@Nonnull String configId) {
        return buildAllowedTargets(resolveConfig(configId), getControlType(configId));
    }

    @Nonnull
    private List<BindingTarget> buildAllowedTargets(@Nullable RpgModConfig config,
            @Nonnull AbilityControlType controlType) {
        List<BindingTarget> targets = new ArrayList<>();

        switch (controlType) {
            case Hotbar -> {
                int[] configuredSlots = config != null ? config.getHotbarAbilitySlots() : null;
                if (configuredSlots != null) {
                    LinkedHashSet<Integer> dedupedSlots = new LinkedHashSet<>();
                    for (int configuredSlot : configuredSlots) {
                        if (configuredSlot < 1 || configuredSlot > 9) {
                            continue;
                        }
                        dedupedSlots.add(configuredSlot);
                    }
                    for (int configuredSlot : dedupedSlots) {
                        targets.add(BindingTarget.hotbar(configuredSlot, configuredSlot));
                    }
                }
            }
            case AbilitySlots123 -> {
                targets.add(BindingTarget.abilitySlot(1));
                targets.add(BindingTarget.abilitySlot(2));
                targets.add(BindingTarget.abilitySlot(3));
            }
            case Weapons -> {
                targets.add(BindingTarget.weapon(InteractionType.Primary));
                targets.add(BindingTarget.weapon(InteractionType.Secondary));
            }
        }

        return targets;
    }

    @Nonnull
    public List<BindingTarget> getBoundTargets(@Nonnull AbilityBindingComponent bindingComponent,
            @Nonnull String abilityId) {
        List<BindingTarget> targets = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : bindingComponent.getHotbarBindings().entrySet()) {
            String boundAbilityId = entry.getValue();
            Integer internalSlot = entry.getKey();
            if (internalSlot == null || boundAbilityId == null || !boundAbilityId.equalsIgnoreCase(abilityId)) {
                continue;
            }
            targets.add(BindingTarget.hotbar(internalSlot, internalSlot + 1));
        }

        for (Map.Entry<Integer, String> entry : bindingComponent.getAbilitySlotBindings().entrySet()) {
            String boundAbilityId = entry.getValue();
            Integer slotNumber = entry.getKey();
            if (slotNumber == null || boundAbilityId == null || !boundAbilityId.equalsIgnoreCase(abilityId)) {
                continue;
            }
            targets.add(BindingTarget.abilitySlot(slotNumber));
        }

        for (Map.Entry<String, String> entry : bindingComponent.getWeaponBindings().entrySet()) {
            String interactionKey = entry.getKey();
            String boundAbilityId = entry.getValue();
            if (interactionKey == null || boundAbilityId == null || !boundAbilityId.equalsIgnoreCase(abilityId)) {
                continue;
            }

            BindingTarget parsedTarget = parseTargetId("weapon:" + interactionKey);
            if (parsedTarget != null) {
                targets.add(parsedTarget);
            }
        }

        targets.sort(Comparator
                .comparingInt((BindingTarget target) -> target.kind().sortOrder)
                .thenComparingInt(BindingTarget::displaySlot)
                .thenComparing(BindingTarget::storageKey));
        return targets;
    }

    public boolean isAbilityUnlocked(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String abilityId) {
        org.pixelbays.rpg.ability.component.ClassAbilityComponent classAbilityComponent = store.getComponent(
                entityRef,
                org.pixelbays.rpg.ability.component.ClassAbilityComponent.getComponentType());
        if (classAbilityComponent != null && classAbilityComponent.hasAbility(abilityId)) {
            return true;
        }

        RaceComponent raceComponent = store.getComponent(entityRef, RaceComponent.getComponentType());
        return raceComponent != null && raceComponent.hasUnlockedRaceAbility(abilityId);
    }

    @Nullable
    public String getBinding(@Nullable AbilityBindingComponent bindingComponent, @Nonnull BindingTarget target) {
        if (bindingComponent == null) {
            return null;
        }

        return switch (target.kind()) {
            case HOTBAR -> bindingComponent.getHotbarBinding(target.internalSlot());
            case ABILITY_SLOT -> bindingComponent.getAbilitySlotBinding(target.displaySlot());
            case WEAPON -> bindingComponent.getWeaponBinding(target.storageKey());
        };
    }

    public void setBinding(@Nonnull AbilityBindingComponent bindingComponent,
            @Nonnull BindingTarget target,
            @Nullable String abilityId) {
        switch (target.kind()) {
            case HOTBAR -> bindingComponent.setHotbarBinding(target.internalSlot(), abilityId);
            case ABILITY_SLOT -> bindingComponent.setAbilitySlotBinding(target.displaySlot(), abilityId);
            case WEAPON -> bindingComponent.setWeaponBinding(target.storageKey(), abilityId);
        }
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindings(@Nullable AbilityBindingComponent bindingComponent) {
        return sanitizeInvalidBindings(bindingComponent, DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindings(@Nullable AbilityBindingComponent bindingComponent,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return sanitizeInvalidBindings(bindingComponent, entityRef, store, DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindings(@Nullable AbilityBindingComponent bindingComponent,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String configId) {
        return sanitizeInvalidBindings(bindingComponent, resolveActiveClassId(entityRef, store), configId);
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindingsForClass(@Nullable AbilityBindingComponent bindingComponent,
            @Nullable String activeClassId) {
        return sanitizeInvalidBindings(bindingComponent, activeClassId, DEFAULT_CONFIG_ID);
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindings(@Nullable AbilityBindingComponent bindingComponent,
            @Nonnull String configId) {
        return sanitizeInvalidBindings(bindingComponent, null, configId);
    }

    @Nonnull
    public BindingSanitizationResult sanitizeInvalidBindings(@Nullable AbilityBindingComponent bindingComponent,
            @Nullable String activeClassId,
            @Nonnull String configId) {
        if (bindingComponent == null) {
            return BindingSanitizationResult.none();
        }

        RpgModConfig config = resolveConfig(configId);
        AbilityControlType controlType = resolveControlType(activeClassId, configId);
        List<Integer> clearedHotbarSlots = new ArrayList<>();
        boolean changed = false;

        switch (controlType) {
            case Hotbar -> {
                LinkedHashSet<Integer> allowedHotbarSlots = new LinkedHashSet<>();
                int[] configuredSlots = config != null ? config.getHotbarAbilitySlots() : null;
                if (configuredSlots != null) {
                    for (int configuredSlot : configuredSlots) {
                        if (configuredSlot < 1 || configuredSlot > 9) {
                            continue;
                        }
                        allowedHotbarSlots.add(configuredSlot);
                    }
                }

                changed |= clearDisallowedHotbarBindings(bindingComponent, allowedHotbarSlots, clearedHotbarSlots);
                changed |= clearAllAbilitySlotBindings(bindingComponent);
                changed |= clearAllWeaponBindings(bindingComponent);
            }
            case AbilitySlots123 -> {
                changed |= migrateLegacyHotbarBindingsToAbilitySlots(bindingComponent, config);
                changed |= clearAllHotbarBindings(bindingComponent, clearedHotbarSlots);
                changed |= clearInvalidAbilitySlotBindings(bindingComponent);
                changed |= clearAllWeaponBindings(bindingComponent);
            }
            case Weapons -> {
                changed |= clearAllHotbarBindings(bindingComponent, clearedHotbarSlots);
                changed |= clearAllAbilitySlotBindings(bindingComponent);
                changed |= clearInvalidWeaponBindings(bindingComponent);
            }
        }

        clearedHotbarSlots.sort(Integer::compareTo);
        return new BindingSanitizationResult(changed, clearedHotbarSlots);
    }

    @Nullable
    public BindingTarget parseTargetId(@Nullable String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }

        String[] parts = targetId.split(":", 2);
        if (parts.length != 2) {
            return null;
        }

        String kindToken = parts[0].trim().toLowerCase(Locale.ROOT);
        String valueToken = parts[1].trim();
        try {
            if ("hotbar".equals(kindToken)) {
                int internalSlot = Integer.parseInt(valueToken);
                if (internalSlot < 0 || internalSlot > 8) {
                    return null;
                }
                return BindingTarget.hotbar(internalSlot, internalSlot + 1);
            }

            if ("ability".equals(kindToken)) {
                int slotNumber = Integer.parseInt(valueToken);
                if (slotNumber < 1 || slotNumber > 3) {
                    return null;
                }
                return BindingTarget.abilitySlot(slotNumber);
            }

            if ("weapon".equals(kindToken)) {
                String normalized = valueToken.toLowerCase(Locale.ROOT);
                if ("primary".equals(normalized)) {
                    return BindingTarget.weapon(InteractionType.Primary);
                }
                if ("secondary".equals(normalized)) {
                    return BindingTarget.weapon(InteractionType.Secondary);
                }
            }

            return null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    public Message getControlTypeMessage(@Nonnull AbilityControlType controlType) {
        return switch (controlType) {
            case Hotbar -> Message.translation("pixelbays.rpg.spellbook.ui.mode.hotbar");
            case AbilitySlots123 -> Message.translation("pixelbays.rpg.spellbook.ui.mode.abilitySlots");
            case Weapons -> Message.translation("pixelbays.rpg.spellbook.ui.mode.weapons");
        };
    }

    @Nonnull
    public Message getTargetMessage(@Nonnull BindingTarget target) {
        return switch (target.kind()) {
            case HOTBAR -> Message.translation("pixelbays.rpg.spellbook.ui.target.hotbar")
                    .param("slot", target.displaySlot());
            case ABILITY_SLOT -> Message.translation("pixelbays.rpg.spellbook.ui.target.ability")
                    .param("slot", target.displaySlot());
            case WEAPON -> Message.translation(target.weaponInteractionType() == InteractionType.Secondary
                    ? "pixelbays.rpg.spellbook.ui.target.secondary"
                    : "pixelbays.rpg.spellbook.ui.target.primary");
        };
    }

    @Nonnull
    public static String resolveMessageText(@Nonnull Message message, @Nonnull String fallback) {
        var formatted = message.getFormattedMessage();
        if (formatted == null) {
            return fallback;
        }

        String rawText = formatted.rawText;
        if (rawText != null && !rawText.isBlank()) {
            return rawText;
        }

        String messageId = formatted.messageId;
        if (messageId != null && !messageId.isBlank()) {
            String translated = resolveTranslation(messageId, null);
            if (translated != null && !translated.isBlank()) {
                if (formatted.params == null || formatted.params.isEmpty()) {
                    return translated;
                }
                return substituteParams(translated, formatted.params);
            }
        }

        return fallback;
    }

    @Nonnull
    public static String resolveDisplayText(@Nullable String maybeTranslationKey, @Nonnull String fallback) {
        if (maybeTranslationKey == null || maybeTranslationKey.isBlank()) {
            return fallback;
        }
        if (!maybeTranslationKey.contains(".")) {
            return maybeTranslationKey;
        }

        String translated = resolveTranslation(maybeTranslationKey, null);
        if (translated != null && !translated.isBlank()) {
            return translated;
        }

        return fallback;
    }

    @Nullable
    private static String resolveTranslation(@Nonnull String key, @Nullable String fallback) {
        I18nModule i18n = I18nModule.get();
        if (i18n != null) {
            String translated = i18n.getMessage(DEFAULT_LANGUAGE, key);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        return fallback;
    }

    @Nonnull
    private static String substituteParams(@Nonnull String template, @Nonnull Map<String, ParamValue> params) {
        String resolved = template;
        for (Map.Entry<String, ParamValue> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = paramToString(entry.getValue());
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            resolved = resolved.replace("{" + key + "}", value);
        }
        return resolved;
    }

    @Nullable
    private static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue stringValue) {
            return stringValue.value;
        }
        if (value instanceof IntParamValue intValue) {
            return Integer.toString(intValue.value);
        }
        if (value instanceof LongParamValue longValue) {
            return Long.toString(longValue.value);
        }
        if (value instanceof DoubleParamValue doubleValue) {
            return Double.toString(doubleValue.value);
        }
        if (value instanceof BoolParamValue boolValue) {
            return Boolean.toString(boolValue.value);
        }
        return String.valueOf(value);
    }

    private boolean clearDisallowedHotbarBindings(@Nonnull AbilityBindingComponent bindingComponent,
            @Nonnull LinkedHashSet<Integer> allowedHotbarSlots,
            @Nonnull List<Integer> clearedHotbarSlots) {
        boolean changed = false;
        var iterator = bindingComponent.getHotbarBindings().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            Integer slot = entry.getKey();
            String abilityId = entry.getValue();
            if (slot == null || abilityId == null || abilityId.isBlank() || !allowedHotbarSlots.contains(slot)) {
                if (slot != null) {
                    clearedHotbarSlots.add(slot);
                }
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Nonnull
    private AbilityControlType resolveControlType(@Nullable String activeClassId, @Nonnull String configId) {
        if (activeClassId != null && !activeClassId.isBlank()) {
            ClassDefinition classDefinition = ClassDefinition.getAssetMap().getAsset(activeClassId);
            if (classDefinition != null) {
                return classDefinition.getEffectiveAbilityControlType(configId);
            }
        }

        RpgModConfig config = resolveConfig(configId);
        if (config == null) {
            return AbilityControlType.Hotbar;
        }

        AbilityControlType controlType = config.getAbilityControlType();
        return controlType != null ? controlType : AbilityControlType.Hotbar;
    }

    @Nullable
    private RpgModConfig resolveConfig(@Nonnull String configId) {
        return RpgModConfig.getAssetMap().getAsset(configId);
    }

    @Nonnull
    private String resolveActiveClassId(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        ClassComponent classComponent = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComponent == null) {
            return "";
        }

        String primaryClassId = classComponent.getPrimaryClassId();
        if (isKnownClassId(primaryClassId)) {
            return primaryClassId;
        }

        for (String classId : classComponent.getLearnedClassIds()) {
            if (isKnownClassId(classId)) {
                return classId;
            }
        }

        return "";
    }

    private boolean isKnownClassId(@Nullable String classId) {
        return classId != null
                && !classId.isBlank()
                && ClassDefinition.getAssetMap().getAsset(classId) != null;
    }

    private boolean migrateLegacyHotbarBindingsToAbilitySlots(@Nonnull AbilityBindingComponent bindingComponent,
            @Nullable RpgModConfig config) {
        if (!bindingComponent.getAbilitySlotBindings().isEmpty() || bindingComponent.getHotbarBindings().isEmpty()) {
            return false;
        }

        List<Integer> orderedHotbarSlots = resolveLegacyHotbarBindingOrder(bindingComponent, config);
        boolean changed = false;
        int abilitySlot = 1;
        for (Integer hotbarSlot : orderedHotbarSlots) {
            if (hotbarSlot == null || abilitySlot > 3) {
                break;
            }

            String abilityId = bindingComponent.getHotbarBinding(hotbarSlot);
            if (abilityId == null || abilityId.isBlank()) {
                continue;
            }

            bindingComponent.setAbilitySlotBinding(abilitySlot, abilityId);
            abilitySlot++;
            changed = true;
        }
        return changed;
    }

    @Nonnull
    private List<Integer> resolveLegacyHotbarBindingOrder(@Nonnull AbilityBindingComponent bindingComponent,
            @Nullable RpgModConfig config) {
        LinkedHashSet<Integer> orderedSlots = new LinkedHashSet<>();
        int[] configuredSlots = config != null ? config.getHotbarAbilitySlots() : null;
        if (configuredSlots != null) {
            for (int configuredSlot : configuredSlots) {
                if (configuredSlot < 1 || configuredSlot > 9) {
                    continue;
                }

                addLegacyHotbarSlotCandidate(orderedSlots, bindingComponent, configuredSlot);
                addLegacyHotbarSlotCandidate(orderedSlots, bindingComponent, configuredSlot - 1);
            }
        }

        List<Integer> fallbackSlots = new ArrayList<>(bindingComponent.getHotbarBindings().keySet());
        fallbackSlots.sort(Integer::compareTo);
        for (Integer hotbarSlot : fallbackSlots) {
            if (hotbarSlot != null) {
                orderedSlots.add(hotbarSlot);
            }
        }

        return new ArrayList<>(orderedSlots);
    }

    private void addLegacyHotbarSlotCandidate(@Nonnull LinkedHashSet<Integer> orderedSlots,
            @Nonnull AbilityBindingComponent bindingComponent,
            int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 9) {
            return;
        }
        if (bindingComponent.getHotbarBindings().containsKey(hotbarSlot)) {
            orderedSlots.add(hotbarSlot);
        }
    }

    private boolean clearAllHotbarBindings(@Nonnull AbilityBindingComponent bindingComponent,
            @Nonnull List<Integer> clearedHotbarSlots) {
        boolean changed = false;
        var iterator = bindingComponent.getHotbarBindings().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            Integer slot = entry.getKey();
            if (slot != null) {
                clearedHotbarSlots.add(slot);
            }
            iterator.remove();
            changed = true;
        }
        return changed;
    }

    private boolean clearAllAbilitySlotBindings(@Nonnull AbilityBindingComponent bindingComponent) {
        if (bindingComponent.getAbilitySlotBindings().isEmpty()) {
            return false;
        }
        bindingComponent.clearAbilitySlotBindings();
        return true;
    }

    private boolean clearInvalidAbilitySlotBindings(@Nonnull AbilityBindingComponent bindingComponent) {
        boolean changed = false;
        var iterator = bindingComponent.getAbilitySlotBindings().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            Integer slot = entry.getKey();
            String abilityId = entry.getValue();
            if (slot == null || slot < 1 || slot > 3 || abilityId == null || abilityId.isBlank()) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    private boolean clearAllWeaponBindings(@Nonnull AbilityBindingComponent bindingComponent) {
        if (bindingComponent.getWeaponBindings().isEmpty()) {
            return false;
        }
        bindingComponent.clearWeaponBindings();
        return true;
    }

    private boolean clearInvalidWeaponBindings(@Nonnull AbilityBindingComponent bindingComponent) {
        boolean changed = false;
        var iterator = bindingComponent.getWeaponBindings().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String interactionKey = entry.getKey();
            String abilityId = entry.getValue();
            boolean validKey = "Primary".equals(interactionKey) || "Secondary".equals(interactionKey);
            if (!validKey || abilityId == null || abilityId.isBlank()) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public static final class BindingSanitizationResult {

        private static final BindingSanitizationResult NONE = new BindingSanitizationResult(false, List.of());

        private final boolean changed;
        private final List<Integer> clearedHotbarSlots;

        private BindingSanitizationResult(boolean changed, @Nonnull List<Integer> clearedHotbarSlots) {
            this.changed = changed;
            this.clearedHotbarSlots = List.copyOf(clearedHotbarSlots);
        }

        @Nonnull
        public static BindingSanitizationResult none() {
            return NONE;
        }

        public boolean changed() {
            return changed;
        }

        @Nonnull
        public List<Integer> clearedHotbarSlots() {
            return clearedHotbarSlots;
        }
    }

    public static final class BindingTarget {

        private final BindingKind kind;
        private final int internalSlot;
        private final int displaySlot;
        private final String storageKey;
        private final InteractionType weaponInteractionType;

        private BindingTarget(@Nonnull BindingKind kind,
                int internalSlot,
                int displaySlot,
                @Nonnull String storageKey,
                @Nullable InteractionType weaponInteractionType) {
            this.kind = kind;
            this.internalSlot = internalSlot;
            this.displaySlot = displaySlot;
            this.storageKey = storageKey;
            this.weaponInteractionType = weaponInteractionType;
        }

        @Nonnull
        public static BindingTarget hotbar(int internalSlot, int displaySlot) {
            return new BindingTarget(BindingKind.HOTBAR, internalSlot, displaySlot, Integer.toString(internalSlot), null);
        }

        @Nonnull
        public static BindingTarget abilitySlot(int slotNumber) {
            return new BindingTarget(BindingKind.ABILITY_SLOT, -1, slotNumber, Integer.toString(slotNumber), null);
        }

        @Nonnull
        public static BindingTarget weapon(@Nonnull InteractionType interactionType) {
            return new BindingTarget(BindingKind.WEAPON, -1, 0, interactionType.name(), interactionType);
        }

        @Nonnull
        public BindingKind kind() {
            return kind;
        }

        public int internalSlot() {
            return internalSlot;
        }

        public int displaySlot() {
            return displaySlot;
        }

        @Nonnull
        public String storageKey() {
            return storageKey;
        }

        @Nullable
        public InteractionType weaponInteractionType() {
            return weaponInteractionType;
        }

        @Nonnull
        public String targetId() {
            return switch (kind) {
                case HOTBAR -> "hotbar:" + internalSlot;
                case ABILITY_SLOT -> "ability:" + displaySlot;
                case WEAPON -> "weapon:" + storageKey;
            };
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BindingTarget that)) {
                return false;
            }
            return this.kind == that.kind
                    && this.internalSlot == that.internalSlot
                    && this.displaySlot == that.displaySlot
                    && this.storageKey.equals(that.storageKey);
        }

        @Override
        public int hashCode() {
            int result = kind.hashCode();
            result = 31 * result + internalSlot;
            result = 31 * result + displaySlot;
            result = 31 * result + storageKey.hashCode();
            return result;
        }
    }

    public enum BindingKind {
        HOTBAR(0),
        ABILITY_SLOT(1),
        WEAPON(2);

        private final int sortOrder;

        BindingKind(int sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}