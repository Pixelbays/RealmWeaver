package org.pixelbays.rpg.ability.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.binding.AbilityBindingService;
import org.pixelbays.rpg.ability.binding.AbilityBindingService.BindingTarget;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.config.settings.AbilityModSettings.AbilityControlType;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class SpellbookPage extends CustomUIPage {

    private static final int MAX_TAB_SLOTS = 8;
    private static final int MAX_ABILITY_ROWS = 30;
    private static final int MAX_BIND_BUTTONS = 9;

    private final AbilityBindingService bindingService;

    @Nullable
    private String activeTabId;

    @Nullable
    private String selectedAbilityId;

    public SpellbookPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.bindingService = new AbilityBindingService();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/SpellbookPage.ui");
        bindStaticEvents(eventBuilder);
        appendView(ref, store, commandBuilder, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> handleAction(ref, store, rawData, action));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String rawData,
            @Nonnull String action) {
        switch (action) {
            case "Tab" -> {
                SpellbookView view = buildView(store, ref);
                Integer tabIndex = extractInt(rawData, "TabIndex");
                if (tabIndex != null && tabIndex >= 0 && tabIndex < view.tabs().size()) {
                    activeTabId = view.tabs().get(tabIndex).id();
                    selectedAbilityId = null;
                }
                refresh(ref, store, null);
            }
            case "SelectAbility" -> {
                SpellbookView view = buildView(store, ref);
                Integer abilityIndex = extractInt(rawData, "AbilityIndex");
                SpellbookTab activeTab = view.activeTab();
                if (activeTab != null && abilityIndex != null && abilityIndex >= 0 && abilityIndex < activeTab.abilityIds().size()) {
                    selectedAbilityId = activeTab.abilityIds().get(abilityIndex);
                }
                refresh(ref, store, null);
            }
            case "Bind" -> handleBindAction(ref, store, rawData);
            default -> {
            }
        }
    }

    private void handleBindAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String rawData) {
        if (selectedAbilityId == null || selectedAbilityId.isBlank()) {
            refresh(ref, store, Message.translation("pixelbays.rpg.spellbook.ui.status.noSelectedAbility"));
            return;
        }

        SpellbookView view = buildView(store, ref);
        Integer bindIndex = extractInt(rawData, "BindIndex");
        if (bindIndex == null || bindIndex < 0 || bindIndex >= view.availableTargets().size()) {
            refresh(ref, store, Message.translation("pixelbays.rpg.spellbook.ui.status.unknownTarget"));
            return;
        }
        BindingTarget target = view.availableTargets().get(bindIndex);

        ClassAbilityDefinition abilityDefinition = ClassAbilityDefinition.getAssetMap().getAsset(selectedAbilityId);
        String abilityName = resolveAbilityName(abilityDefinition, selectedAbilityId);
        if (!bindingService.isAbilityUnlocked(ref, store, selectedAbilityId)) {
            refresh(ref, store, Message.translation("pixelbays.rpg.ability.bind.notLearned")
                    .param("ability", abilityName));
            return;
        }
        if (abilityDefinition != null
                && abilityDefinition.getAbilityType() == ClassAbilityDefinition.AbilityType.Passive) {
            refresh(ref, store, Message.translation("pixelbays.rpg.ability.bind.passive")
                    .param("ability", abilityName));
            return;
        }

        AbilityBindingComponent bindingComponent = store.getComponent(ref, AbilityBindingComponent.getComponentType());
        if (bindingComponent == null) {
            bindingComponent = store.addComponent(ref, AbilityBindingComponent.getComponentType());
        }

        String currentAbilityId = bindingService.getBinding(bindingComponent, target);
        Message statusMessage;
        if (selectedAbilityId.equalsIgnoreCase(currentAbilityId)) {
            bindingService.setBinding(bindingComponent, target, null);
            if (target.kind() == AbilityBindingService.BindingKind.HOTBAR) {
                Realmweavers.get().getHotbarIconManager().updateHotbarSlot(ref, store, target.internalSlot(), null);
            }
            statusMessage = Message.translation("pixelbays.rpg.spellbook.ui.status.cleared")
                    .param("ability", abilityName)
                    .param("target", resolveTargetName(target));
        } else {
            bindingService.setBinding(bindingComponent, target, selectedAbilityId);
            if (target.kind() == AbilityBindingService.BindingKind.HOTBAR) {
                Realmweavers.get().getHotbarIconManager().updateHotbarSlot(ref, store, target.internalSlot(), selectedAbilityId);
            }
            statusMessage = Message.translation("pixelbays.rpg.spellbook.ui.status.bound")
                    .param("ability", abilityName)
                    .param("target", resolveTargetName(target));
        }

        refresh(ref, store, statusMessage);
    }

    private void refresh(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable Message statusMessage) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(ref, store, commandBuilder, statusMessage);
        sendUpdate(commandBuilder);
    }

    private void appendView(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable Message statusMessage) {
        SpellbookView view = buildView(store, ref);

        commandBuilder.set("#ModeLabel.Text",
                resolveModeLabel(bindingService.getControlType(ref, store), view.availableTargets()));
        if (statusMessage != null) {
            commandBuilder.setObject("#StatusLabel.Text", toLocalizableString(statusMessage));
        } else if (view.tabs().isEmpty()) {
            commandBuilder.setObject("#StatusLabel.Text",
                    toLocalizableString(Message.translation("pixelbays.rpg.spellbook.ui.status.noUnlockedAbilities")));
        } else {
            commandBuilder.set("#StatusLabel.Text", "");
        }

        buildTabs(commandBuilder, view.tabs());
        buildAbilityRows(commandBuilder, view.activeTab(), view.unlockedAbilityIds(), view.bindingComponent());
        buildDetailPanel(commandBuilder, view);
    }

    private void bindStaticEvents(@Nonnull UIEventBuilder eventBuilder) {
        for (int index = 0; index < MAX_TAB_SLOTS; index++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    tabSelector(index),
                    new EventData().append("Action", "Tab").append("TabIndex", Integer.toString(index)));
        }

        for (int index = 0; index < MAX_ABILITY_ROWS; index++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    abilityRowSelector(index),
                    new EventData().append("Action", "SelectAbility").append("AbilityIndex", Integer.toString(index)));
        }

        for (int index = 0; index < MAX_BIND_BUTTONS; index++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    bindButtonSelector(index),
                    new EventData().append("Action", "Bind").append("BindIndex", Integer.toString(index)));
        }
    }

    @Nonnull
    private SpellbookView buildView(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        List<SpellbookTab> tabs = new ArrayList<>();
        ClassAbilityComponent classAbilityComponent = store.getComponent(ref, ClassAbilityComponent.getComponentType());
        ClassComponent classComponent = store.getComponent(ref, ClassComponent.getComponentType());
        RaceComponent raceComponent = store.getComponent(ref, RaceComponent.getComponentType());
        AbilityBindingComponent bindingComponent = store.getComponent(ref, AbilityBindingComponent.getComponentType());
        Set<String> tabbedAbilityIds = new HashSet<>();
        Set<String> unlockedAbilityIds = new HashSet<>();

        if (classAbilityComponent != null) {
            unlockedAbilityIds.addAll(sanitizeAbilityIds(classAbilityComponent.getUnlockedAbilityIds()));
        }
        if (raceComponent != null) {
            unlockedAbilityIds.addAll(sanitizeAbilityIds(raceComponent.getUnlockedRaceAbilities()));
        }

        if (classComponent != null && classAbilityComponent != null) {
            for (String classId : classComponent.getLearnedClassIds()) {
                ClassDefinition classDefinition = ClassDefinition.getAssetMap().getAsset(classId);
                LinkedHashSet<String> classAbilityIds = new LinkedHashSet<>();
                if (classDefinition != null) {
                    classAbilityIds.addAll(sanitizeAbilityIds(classDefinition.getAbilityIds()));
                    classAbilityIds.addAll(collectTalentAbilityIds(classDefinition));
                }
                classAbilityIds.addAll(sanitizeAbilityIds(classAbilityComponent.getAbilitiesForClass(classId)));

                List<String> abilityIds = new ArrayList<>(classAbilityIds);
                abilityIds.sort((left, right) -> resolveAbilityName(
                        ClassAbilityDefinition.getAssetMap().getAsset(left),
                        left).compareToIgnoreCase(resolveAbilityName(
                                ClassAbilityDefinition.getAssetMap().getAsset(right),
                                right)));
                if (abilityIds.isEmpty()) {
                    continue;
                }

                tabbedAbilityIds.addAll(abilityIds);
                String classLabel = resolveDisplayLabel(
                        classDefinition != null ? classDefinition.getDisplayName() : null,
                        classId);
                tabs.add(new SpellbookTab(
                        "class:" + classId,
                        classLabel + " (" + abilityIds.size() + ")",
                        classLabel,
                        abilityIds));
            }
        }

        if (raceComponent != null) {
            String raceId = raceComponent.getRaceId();
            RaceDefinition raceDefinition = raceId == null || raceId.isBlank()
                ? null
                : RaceDefinition.getAssetMap().getAsset(raceId);
            LinkedHashSet<String> raceAbilitySet = new LinkedHashSet<>();
            if (raceDefinition != null) {
            raceAbilitySet.addAll(sanitizeAbilityIds(raceDefinition.getAbilityIds()));
            }
            raceAbilitySet.addAll(sanitizeAbilityIds(raceComponent.getUnlockedRaceAbilities()));

            List<String> raceAbilityIds = new ArrayList<>(raceAbilitySet);
            raceAbilityIds.sort((left, right) -> resolveAbilityName(
                ClassAbilityDefinition.getAssetMap().getAsset(left),
                left).compareToIgnoreCase(resolveAbilityName(
                    ClassAbilityDefinition.getAssetMap().getAsset(right),
                    right)));
            if (!raceAbilityIds.isEmpty()) {
            tabbedAbilityIds.addAll(raceAbilityIds);
                String raceLabel = resolveDisplayLabel(
                        raceDefinition != null ? raceDefinition.getDisplayName() : null,
                        AbilityBindingService.resolveMessageText(
                                Message.translation("pixelbays.rpg.spellbook.ui.tab.race"),
                                "Race"));
                tabs.add(new SpellbookTab(
                        "race:" + (raceId == null ? "unknown" : raceId),
                        raceLabel + " (" + raceAbilityIds.size() + ")",
                        raceLabel,
                        raceAbilityIds));
            }
        }

        if (classAbilityComponent != null) {
            List<String> uncategorizedAbilityIds = sanitizeAbilityIds(classAbilityComponent.getUnlockedAbilityIds());
            uncategorizedAbilityIds.removeIf(tabbedAbilityIds::contains);
            uncategorizedAbilityIds.sort((left, right) -> resolveAbilityName(
                    ClassAbilityDefinition.getAssetMap().getAsset(left),
                    left).compareToIgnoreCase(resolveAbilityName(
                            ClassAbilityDefinition.getAssetMap().getAsset(right),
                            right)));
            if (!uncategorizedAbilityIds.isEmpty()) {
                String otherLabel = AbilityBindingService.resolveMessageText(
                        Message.translation("pixelbays.rpg.spellbook.ui.tab.other"),
                        "Other");
                tabs.add(new SpellbookTab(
                        "other",
                        otherLabel + " (" + uncategorizedAbilityIds.size() + ")",
                        otherLabel,
                        uncategorizedAbilityIds));
            }
        }

        SpellbookTab activeTab = resolveActiveTab(tabs);
        ClassAbilityDefinition selectedAbility = null;
        if (activeTab != null) {
            if (selectedAbilityId == null || !activeTab.abilityIds().contains(selectedAbilityId)) {
                selectedAbilityId = activeTab.abilityIds().isEmpty() ? null : activeTab.abilityIds().get(0);
            }
            if (selectedAbilityId != null) {
                selectedAbility = ClassAbilityDefinition.getAssetMap().getAsset(selectedAbilityId);
            }
        } else {
            activeTabId = null;
            selectedAbilityId = null;
        }

        return new SpellbookView(
                tabs,
                activeTab,
                bindingService.getAllowedTargets(ref, store),
                unlockedAbilityIds,
                bindingComponent,
                selectedAbility,
                selectedAbilityId);
    }

    @Nullable
    private SpellbookTab resolveActiveTab(@Nonnull List<SpellbookTab> tabs) {
        if (tabs.isEmpty()) {
            return null;
        }
        if (activeTabId != null) {
            for (SpellbookTab tab : tabs) {
                if (tab.id().equals(activeTabId)) {
                    return tab;
                }
            }
        }
        activeTabId = tabs.get(0).id();
        return tabs.get(0);
    }

        private void buildTabs(@Nonnull UICommandBuilder commandBuilder,
            @Nonnull List<SpellbookTab> tabs) {
        resetTabSlots(commandBuilder);

        int index = 0;
        for (SpellbookTab tab : tabs) {
            if (index >= MAX_TAB_SLOTS) {
                break;
            }

            String selector = tabSelector(index);
            String label = tab.id().equals(activeTabId) ? "> " + tab.label() : tab.label();
            commandBuilder.set(selector + ".Text", label);
            commandBuilder.setObject(selector + ".Anchor", createHorizontalAnchor(150, 36, 6));
            commandBuilder.set(selector + ".Visible", true);
            index++;
        }
    }

            private void buildAbilityRows(@Nonnull UICommandBuilder commandBuilder,
            @Nullable SpellbookTab activeTab,
            @Nonnull Set<String> unlockedAbilityIds,
            @Nullable AbilityBindingComponent bindingComponent) {
        resetAbilityRows(commandBuilder);
        if (activeTab == null) {
            return;
        }

        int index = 0;
        for (String abilityId : activeTab.abilityIds()) {
            if (index >= MAX_ABILITY_ROWS) {
                break;
            }

            ClassAbilityDefinition abilityDefinition = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
            String abilityName = resolveAbilityName(abilityDefinition, abilityId);
            String label = abilityId.equals(selectedAbilityId) ? "> " + abilityName : abilityName;
            if (!unlockedAbilityIds.contains(abilityId)) {
                label = label + " " + AbilityBindingService.resolveMessageText(
                        Message.translation("pixelbays.rpg.spellbook.ui.lockedSuffix"),
                        "[Locked]");
            }

            if (bindingComponent != null) {
                List<BindingTarget> boundTargets = bindingService.getBoundTargets(bindingComponent, abilityId);
                if (!boundTargets.isEmpty()) {
                    label = label + " [" + summarizeTargets(boundTargets) + "]";
                }
            }

            String selector = abilityRowSelector(index);
            commandBuilder.set(selector + ".Text", label);
            commandBuilder.setObject(selector + ".Anchor", createVerticalAnchor(36, 6));
            commandBuilder.set(selector + ".Visible", true);
            index++;
        }
    }

            private void buildDetailPanel(@Nonnull UICommandBuilder commandBuilder,
                @Nonnull SpellbookView view) {
        resetBindButtons(commandBuilder);

        SpellbookTab activeTab = view.activeTab();
        ClassAbilityDefinition selectedAbility = view.selectedAbility();
        String selectedAbilityKey = view.selectedAbilityId();
        if (activeTab == null || selectedAbilityKey == null) {
            commandBuilder.set("#SelectedAbilityName.Text", "");
            commandBuilder.set("#SelectedAbilitySource.Text", "");
            commandBuilder.set("#SelectedAbilityType.Text", "");
            commandBuilder.setObject("#SelectedAbilityDescription.Text",
                    toLocalizableString(Message.translation("pixelbays.rpg.spellbook.ui.noSelection")));
            commandBuilder.setObject("#CurrentBindingsLabel.Text",
                    toLocalizableString(Message.translation("pixelbays.rpg.spellbook.ui.binding.noneDetailed")));
            return;
        }

        String abilityName = resolveAbilityName(selectedAbility, selectedAbilityKey);
        boolean unlocked = view.unlockedAbilityIds().contains(selectedAbilityKey);
    boolean passive = selectedAbility != null
        && selectedAbility.getAbilityType() == ClassAbilityDefinition.AbilityType.Passive;
        commandBuilder.set("#SelectedAbilityName.Text", abilityName);
        commandBuilder.set("#SelectedAbilitySource.Text", AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.source")
                        .param("source", activeTab.sourceLabel()),
                "Source: " + activeTab.sourceLabel()));
        commandBuilder.set("#SelectedAbilityType.Text", AbilityBindingService.resolveMessageText(
            Message.translation("pixelbays.rpg.spellbook.ui.type")
                .param("type", resolveAbilityTypeName(selectedAbility, unlocked)),
            "Type: " + resolveAbilityTypeName(selectedAbility, unlocked)));
        commandBuilder.set("#SelectedAbilityDescription.Text", resolveAbilityDescription(selectedAbility));

        List<BindingTarget> boundTargets = view.bindingComponent() != null
                ? bindingService.getBoundTargets(view.bindingComponent(), selectedAbilityKey)
                : List.of();
        String bindingSummary;
        if (!unlocked) {
            bindingSummary = AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.binding.locked"),
                "Locked until unlocked");
        } else if (passive) {
            bindingSummary = AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.binding.passive"),
                "Passive abilities activate automatically.");
        } else if (boundTargets.isEmpty()) {
            bindingSummary = AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.binding.none"),
                "none");
        } else {
            bindingSummary = summarizeTargets(boundTargets);
        }
        commandBuilder.set("#CurrentBindingsLabel.Text", AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.binding.summary")
                        .param("bindings", bindingSummary),
                "Current bindings: " + bindingSummary));

        if (!unlocked || passive) {
            return;
        }

        int index = 0;
        for (BindingTarget target : view.availableTargets()) {
            if (index >= MAX_BIND_BUTTONS) {
                break;
            }

            String selector = bindButtonSelector(index);
            String currentAbilityId = bindingService.getBinding(view.bindingComponent(), target);
            String targetName = resolveTargetName(target);
            boolean selectedBoundToTarget = selectedAbilityKey.equalsIgnoreCase(currentAbilityId);
            commandBuilder.set(selector + ".Text", AbilityBindingService.resolveMessageText(
                    Message.translation(selectedBoundToTarget
                            ? "pixelbays.rpg.spellbook.ui.clear"
                            : "pixelbays.rpg.spellbook.ui.bind")
                            .param("target", targetName),
                    (selectedBoundToTarget ? "Clear " : "Bind ") + targetName));
            commandBuilder.setObject(selector + ".Anchor", createVerticalAnchor(36, 6));
            commandBuilder.set(selector + ".Visible", true);
            index++;
        }
    }

    @Nonnull
    private List<String> sanitizeAbilityIds(@Nullable Iterable<String> abilityIds) {
        if (abilityIds == null) {
            return new ArrayList<>();
        }

        List<String> sanitized = new ArrayList<>();
        Set<String> seenAbilityIds = new HashSet<>();
        for (String abilityId : abilityIds) {
            if (abilityId == null || abilityId.isBlank()) {
                continue;
            }
            if (!seenAbilityIds.add(abilityId)) {
                continue;
            }
            sanitized.add(abilityId);
        }
        return sanitized;
    }

    @Nonnull
    private List<String> collectTalentAbilityIds(@Nonnull ClassDefinition classDefinition) {
        LinkedHashSet<String> abilityIds = new LinkedHashSet<>();
        if (classDefinition.getTalentTrees() == null) {
            return new ArrayList<>();
        }

        for (ClassDefinition.TalentTree tree : classDefinition.getTalentTrees()) {
            if (tree == null || tree.getNodes() == null) {
                continue;
            }
            for (ClassDefinition.TalentNode node : tree.getNodes()) {
                if (node == null) {
                    continue;
                }
                String abilityId = node.getGrantsAbilityId();
                if (abilityId != null && !abilityId.isBlank()) {
                    abilityIds.add(abilityId);
                }
            }
        }

        return new ArrayList<>(abilityIds);
    }

    @Nonnull
    private String summarizeTargets(@Nonnull List<BindingTarget> targets) {
        List<String> labels = new ArrayList<>();
        for (BindingTarget target : targets) {
            labels.add(resolveTargetName(target));
        }
        return String.join(", ", labels);
    }

    @Nonnull
    private String resolveModeLabel(@Nonnull AbilityControlType controlType,
            @Nonnull List<BindingTarget> availableTargets) {
        String modeName = AbilityBindingService.resolveMessageText(
                bindingService.getControlTypeMessage(controlType),
                controlType.name());
        String header = AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.mode")
                        .param("mode", modeName),
                "Control mode: " + modeName);
        if (!availableTargets.isEmpty()) {
            return header;
        }
        return header + " | " + AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.noTargets"),
                "No bind targets configured.");
    }

    @Nonnull
    private String resolveAbilityName(@Nullable ClassAbilityDefinition abilityDefinition, @Nonnull String fallback) {
        if (abilityDefinition == null) {
            return fallback;
        }
        return AbilityBindingService.resolveDisplayText(abilityDefinition.getDisplayName(), abilityDefinition.getId());
    }

    @Nonnull
    private String resolveAbilityTypeName(@Nullable ClassAbilityDefinition abilityDefinition, boolean unlocked) {
        if (abilityDefinition == null || abilityDefinition.getAbilityType() == null) {
            return AbilityBindingService.resolveMessageText(
                    Message.translation("pixelbays.rpg.ability.type.unknown"),
                    "Unknown");
        }

        String typeKey = switch (abilityDefinition.getAbilityType()) {
            case Active -> "pixelbays.rpg.ability.type.active";
            case Passive -> "pixelbays.rpg.ability.type.passive";
            case Toggle -> "pixelbays.rpg.ability.type.toggle";
        };
        String typeName = AbilityBindingService.resolveMessageText(
            Message.translation(typeKey),
            abilityDefinition.getAbilityType().name());
        String stateName = AbilityBindingService.resolveMessageText(
            Message.translation(unlocked
                ? "pixelbays.rpg.spellbook.ui.state.unlocked"
                : "pixelbays.rpg.spellbook.ui.state.locked"),
            unlocked ? "Unlocked" : "Locked");
        return typeName + " | " + stateName;
    }

    @Nonnull
    private String resolveAbilityDescription(@Nullable ClassAbilityDefinition abilityDefinition) {
        if (abilityDefinition == null) {
            return AbilityBindingService.resolveMessageText(
                    Message.translation("pixelbays.rpg.spellbook.ui.noDescription"),
                    "No description available.");
        }

        String description = AbilityBindingService.resolveDisplayText(
                abilityDefinition.getDescriptionTranslationKey(),
                abilityDefinition.getDescriptionTranslationKey());
        if (!description.equals(abilityDefinition.getDescriptionTranslationKey())) {
            return description;
        }

        String tooltip = abilityDefinition.getTooltip();
        if (tooltip != null && !tooltip.isBlank()) {
            return AbilityBindingService.resolveDisplayText(tooltip, tooltip);
        }

        return AbilityBindingService.resolveMessageText(
                Message.translation("pixelbays.rpg.spellbook.ui.noDescription"),
                "No description available.");
    }

    @Nonnull
    private String resolveTargetName(@Nonnull BindingTarget target) {
        return AbilityBindingService.resolveMessageText(bindingService.getTargetMessage(target), target.targetId());
    }

    @Nonnull
    private String resolveDisplayLabel(@Nullable String maybeTranslationKey, @Nonnull String fallback) {
        return AbilityBindingService.resolveDisplayText(maybeTranslationKey, fallback);
    }

    private static void resetTabSlots(@Nonnull UICommandBuilder commandBuilder) {
        for (int index = 0; index < MAX_TAB_SLOTS; index++) {
            String selector = tabSelector(index);
            commandBuilder.set(selector + ".Text", "");
            commandBuilder.setObject(selector + ".Anchor", createHorizontalAnchor(0, 0, 0));
            commandBuilder.set(selector + ".Visible", false);
        }
    }

    private static void resetAbilityRows(@Nonnull UICommandBuilder commandBuilder) {
        for (int index = 0; index < MAX_ABILITY_ROWS; index++) {
            String selector = abilityRowSelector(index);
            commandBuilder.set(selector + ".Text", "");
            commandBuilder.setObject(selector + ".Anchor", createVerticalAnchor(0, 0));
            commandBuilder.set(selector + ".Visible", false);
        }
    }

    private static void resetBindButtons(@Nonnull UICommandBuilder commandBuilder) {
        for (int index = 0; index < MAX_BIND_BUTTONS; index++) {
            String selector = bindButtonSelector(index);
            commandBuilder.set(selector + ".Text", "");
            commandBuilder.setObject(selector + ".Anchor", createVerticalAnchor(0, 0));
            commandBuilder.set(selector + ".Visible", false);
        }
    }

    @Nonnull
    private static String tabSelector(int index) {
        return "#Tab" + index;
    }

    @Nonnull
    private static String abilityRowSelector(int index) {
        return "#AbilityRow" + index;
    }

    @Nonnull
    private static String bindButtonSelector(int index) {
        return "#BindButton" + index;
    }

    @Nonnull
    private static Anchor createHorizontalAnchor(int width, int height, int right) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        anchor.setRight(Value.of(right));
        return anchor;
    }

    @Nonnull
    private static Anchor createVerticalAnchor(int height, int bottom) {
        Anchor anchor = new Anchor();
        anchor.setHeight(Value.of(height));
        anchor.setBottom(Value.of(bottom));
        return anchor;
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
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
            return String.valueOf(intValue.value);
        }
        if (value instanceof LongParamValue longValue) {
            return String.valueOf(longValue.value);
        }
        if (value instanceof DoubleParamValue doubleValue) {
            return String.valueOf(doubleValue.value);
        }
        if (value instanceof BoolParamValue boolValue) {
            return String.valueOf(boolValue.value);
        }
        return String.valueOf(value);
    }

    @Nullable
    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = rawData.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }

        int valueEnd = rawData.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }

        return rawData.substring(valueStart + 1, valueEnd);
    }

    @Nullable
    private static Integer extractInt(@Nonnull String rawData, @Nonnull String key) {
        String value = extractString(rawData, key);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record SpellbookTab(String id, String label, String sourceLabel, List<String> abilityIds) {
    }

    private record SpellbookView(
            List<SpellbookTab> tabs,
            @Nullable SpellbookTab activeTab,
            List<BindingTarget> availableTargets,
            Set<String> unlockedAbilityIds,
            @Nullable AbilityBindingComponent bindingComponent,
            @Nullable ClassAbilityDefinition selectedAbility,
            @Nullable String selectedAbilityId) {
    }
}