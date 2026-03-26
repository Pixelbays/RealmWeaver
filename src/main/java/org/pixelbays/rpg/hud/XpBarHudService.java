package org.pixelbays.rpg.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.config.ClassDefinition.ResourceDisplayDefinition;
import org.pixelbays.rpg.classes.config.ClassDefinition.ResourceDisplayMode;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.party.PartyMemberType;
import org.pixelbays.rpg.party.PartyRole;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class XpBarHudService {

    private static final String BASE_SYSTEM_ID = "Base_Character_Level";

    private final LevelProgressionSystem levelSystem;
    private final ConcurrentHashMap<UUID, XpBarHud> hudByPlayerId = new ConcurrentHashMap<>();

    public XpBarHudService(@Nonnull LevelProgressionSystem levelSystem) {
        this.levelSystem = levelSystem;
    }

    public void remove(@Nonnull UUID playerId) {
        hudByPlayerId.remove(playerId);
    }

    public void ensureAndUpdate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (!ref.isValid()) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        XpBarHud hud = hudByPlayerId.compute(playerRef.getUuid(), (id, existing) -> {
            if (existing == null) {
                return new XpBarHud(playerRef);
            }
            if (!existing.getPlayerRef().equals(playerRef)) {
                return new XpBarHud(playerRef);
            }
            return existing;
        });

        HudManager hudManager = player.getHudManager();
        if (hudManager.getCustomHud() != hud) {
            hudManager.setCustomHud(playerRef, hud);
        }

        String activeClassId = resolveActiveClassId(ref, store);

        hud.updateResources(resolveResourceBars(ref, store));
        hud.updatePartyMembers(resolvePartyMembers(playerRef));

        if (activeClassId == null || activeClassId.isBlank()) {
            hud.hideProgress();
            return;
        }

        String systemId = resolveActiveClassSystemId(activeClassId);

        String labelPrefix = resolveActiveClassLabel(activeClassId, systemId);
        int level = levelSystem.getLevel(ref, systemId);
        if (level <= 0) {
            level = 1;
        }

        float currentExp = levelSystem.getExperience(ref, systemId);
        float expToNext = levelSystem.getExpToNextLevel(ref, systemId);

        if (expToNext <= 0.0001f) {
            hud.updateMax(labelPrefix, level);
            return;
        }

        float ratio = currentExp / expToNext;
        int current = Math.max(0, Math.round(currentExp));
        int next = Math.max(0, Math.round(expToNext));
        int remaining = Math.max(0, next - current);

        hud.updateProgress(labelPrefix, level, ratio, current, next, remaining);
    }

    @Nonnull
    private static java.util.List<XpBarHud.ResourceBarData> resolveResourceBars(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(ref, ClassComponent.getComponentType());
        String activeClassId = getClassManagementSystem().getPrimaryKnownClassId(classComp);

        if (activeClassId == null || activeClassId.isEmpty()) {
            return java.util.List.of();
        }

        ClassDefinition classDef = getClassDefinition(activeClassId);
        if (classDef == null) {
            return java.util.List.of();
        }

        java.util.List<ResourceDisplayDefinition> resourceDisplays = classDef.getResolvedResourceDisplays();
        if (resourceDisplays == null || resourceDisplays.isEmpty()) {
            return java.util.List.of();
        }

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return java.util.List.of();
        }

        java.util.ArrayList<XpBarHud.ResourceBarData> result = new java.util.ArrayList<>(resourceDisplays.size());
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
                result.add(new XpBarHud.ResourceBarData(
                        statId,
                        label,
                        XpBarHud.ResourceDisplayMode.CHARGES,
                        fillColor,
                    assetPath,
                        0f,
                        currentCharges,
                        maxCharges));
                continue;
            }

            result.add(new XpBarHud.ResourceBarData(
                    statId,
                    label,
                    XpBarHud.ResourceDisplayMode.BAR,
                    fillColor,
                    assetPath,
                    value.asPercentage(),
                    0,
                    0));
        }

        return result;
    }

    @Nonnull
    private static List<XpBarHud.PartyMemberHudData> resolvePartyMembers(@Nonnull PlayerRef viewerRef) {
        PartyManager partyManager = ExamplePlugin.get().getPartyManager();

        Party party = partyManager.getPartyForMember(viewerRef.getUuid());
        if (party == null) {
            return List.of();
        }

        List<PartyMember> sortedMembers = new ArrayList<>(party.getMemberList());
        UUID viewerWorldId = viewerRef.getWorldUuid();
        sortedMembers.sort(Comparator
                .comparingInt((PartyMember member) -> roleSortOrder(member.getRole()))
                .thenComparingLong(PartyMember::getJoinedAtMillis)
                .thenComparing(member -> member.getEntityId().toString()));

        List<XpBarHud.PartyMemberHudData> result = new ArrayList<>(sortedMembers.size());
        for (PartyMember member : sortedMembers) {
            if (member == null || member.getMemberType() != PartyMemberType.PLAYER) {
                continue;
            }

            PlayerRef memberRef = Universe.get().getPlayer(member.getEntityId());
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }

            UUID memberWorldId = memberRef.getWorldUuid();
            if (viewerWorldId != null && memberWorldId != null && !viewerWorldId.equals(memberWorldId)) {
                continue;
            }

            Ref<EntityStore> memberEntity = memberRef.getReference();
            if (memberEntity == null || !memberEntity.isValid()) {
                continue;
            }

            Store<EntityStore> memberStore = memberEntity.getStore();

            EntityStatMap statMap = memberStore.getComponent(memberEntity, EntityStatMap.getComponentType());
            if (statMap == null) {
                continue;
            }

            ClassComponent classComponent = memberStore.getComponent(memberEntity, ClassComponent.getComponentType());
            String activeClassId = getClassManagementSystem().getPrimaryKnownClassId(classComponent);
            ClassDefinition classDefinition = activeClassId == null || activeClassId.isBlank()
                    ? null
                    : getClassDefinition(activeClassId);

            String classLabel = "";
            if (classDefinition != null && classDefinition.getDisplayName() != null && !classDefinition.getDisplayName().isBlank()) {
                classLabel = classDefinition.getDisplayName();
            } else if (activeClassId != null && !activeClassId.isBlank()) {
                classLabel = humanizeIdentifier(activeClassId);
            }

            List<XpBarHud.PartyStatBarData> bars = new ArrayList<>(3);
            appendPartyStatBar(bars, statMap, "Health", "HP", "#4FD36F");

            if (classDefinition != null && classDefinition.getResourceStats() != null) {
                for (String statId : classDefinition.getResourceStats()) {
                    if (statId == null || statId.isBlank() || "Health".equalsIgnoreCase(statId)) {
                        continue;
                    }
                    appendPartyStatBar(bars, statMap, statId, shortStatLabel(statId), resolveStatColor(statId));
                }
            }

            if (bars.isEmpty()) {
                continue;
            }

            result.add(new XpBarHud.PartyMemberHudData(
                    member.getEntityId().toString(),
                    memberRef.getUsername(),
                    classLabel,
                    resolveRoleAccent(member.getRole()),
                    bars));
        }

        return result;
    }

    @Nullable
    private static String resolveActiveClassId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(ref, ClassComponent.getComponentType());
        return getClassManagementSystem().getPrimaryKnownClassId(classComp);
    }

    @Nonnull
    private static String resolveResourceLabel(
            @Nonnull ResourceDisplayDefinition resourceDisplay,
            @Nonnull String statId) {
        String label = resourceDisplay.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        return humanizeIdentifier(statId);
    }

    @Nonnull
    private static String resolveResourceFillColor(
            @Nonnull ResourceDisplayDefinition resourceDisplay,
            @Nonnull String statId) {
        String fillColor = resourceDisplay.getFillColor();
        if (fillColor != null && !fillColor.isBlank()) {
            return fillColor;
        }
        return resolveStatColor(statId);
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

    @Nonnull
    private static String resolveActiveClassSystemId(@Nonnull String activeClassId) {
        if (activeClassId.isEmpty()) {
            return BASE_SYSTEM_ID;
        }

        ClassDefinition classDef = getClassDefinition(activeClassId);
        if (classDef == null) {
            return BASE_SYSTEM_ID;
        }

        if (classDef.usesCharacterLevel()) {
            return BASE_SYSTEM_ID;
        }

        String systemId = classDef.getLevelSystemId();
        if (systemId == null || systemId.isEmpty()) {
            return BASE_SYSTEM_ID;
        }

        return systemId;
    }

    private static void appendPartyStatBar(
            @Nonnull List<XpBarHud.PartyStatBarData> bars,
            @Nonnull EntityStatMap statMap,
            @Nonnull String statId,
            @Nonnull String label,
            @Nonnull String fillColor) {
        int statIndex = EntityStatType.getAssetMap().getIndex(statId);
        EntityStatValue value = statMap.get(statIndex);
        if (value == null) {
            return;
        }

        String valueText = formatCompactValue(value.get()) + " / " + formatCompactValue(value.getMax());
        bars.add(new XpBarHud.PartyStatBarData(statId, label, valueText, fillColor, value.asPercentage()));
    }

    private static int roleSortOrder(@Nullable PartyRole role) {
        if (role == null) {
            return 2;
        }

        return switch (role) {
            case LEADER -> 0;
            case ASSISTANT -> 1;
            default -> 2;
        };
    }

    @Nonnull
    private static String resolveRoleAccent(@Nullable PartyRole role) {
        if (role == null) {
            return "#8E98A6";
        }

        return switch (role) {
            case LEADER -> "#D8B15B";
            case ASSISTANT -> "#6FA6FF";
            default -> "#8E98A6";
        };
    }

    @Nonnull
    private static String resolveStatColor(@Nonnull String statId) {
        String normalized = statId.replace("_", "").replace(" ", "").toLowerCase();
        return switch (normalized) {
            case "health" -> "#4FD36F";
            case "mana" -> "#4F9DFF";
            case "stamina" -> "#E1C74C";
            case "rage" -> "#D65A4F";
            case "heat" -> "#F18A3C";
            case "oxygen" -> "#5BD3D8";
            case "ammo" -> "#E3B75D";
            case "signatureenergy" -> "#B86BFF";
            default -> "#C8CCD4";
        };
    }

    @Nonnull
    private static String shortStatLabel(@Nonnull String statId) {
        String normalized = statId.replace("_", "").replace(" ", "").toLowerCase();
        return switch (normalized) {
            case "health" -> "HP";
            case "mana" -> "MP";
            case "stamina" -> "STA";
            case "rage" -> "RAGE";
            case "heat" -> "HEAT";
            case "oxygen" -> "O2";
            case "ammo" -> "AMMO";
            case "signatureenergy" -> "SIG";
            default -> humanizeIdentifier(statId).toUpperCase();
        };
    }

    @Nonnull
    private static String humanizeIdentifier(@Nonnull String value) {
        String normalized = value.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder(normalized.length() + 8);
        char previous = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (i > 0 && Character.isUpperCase(current) && Character.isLowerCase(previous) && previous != ' ') {
                result.append(' ');
            }
            result.append(current);
            previous = current;
        }
        return result.toString();
    }

    @Nonnull
    private static String formatCompactValue(float value) {
        float abs = Math.abs(value);
        if (abs >= 1_000_000f) {
            return formatCompactUnit(value / 1_000_000f, "M");
        }
        if (abs >= 1_000f) {
            return formatCompactUnit(value / 1_000f, "K");
        }
        return Integer.toString(Math.max(0, Math.round(value)));
    }

    @Nonnull
    private static String formatCompactUnit(float value, @Nonnull String suffix) {
        float rounded = Math.round(value * 10f) / 10f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.05f) {
            return Integer.toString(Math.round(rounded)) + suffix;
        }
        return rounded + suffix;
    }

    @Nonnull
    private String resolveActiveClassLabel(
            @Nonnull String activeClassId,
            @Nonnull String resolvedSystemId) {
        if (!activeClassId.isEmpty()) {
            ClassDefinition classDef = getClassDefinition(activeClassId);
            if (classDef != null) {
                String display = classDef.getDisplayName();
                if (display != null && !display.isBlank()) {
                    return display;
                }
            }

            return activeClassId;
        }

        LevelSystemConfig config = levelSystem.getConfig(resolvedSystemId);
        if (config != null) {
            String display = config.getDisplayName();
            if (display != null && !display.isBlank()) {
                return display;
            }
        }

        return resolvedSystemId;
    }

    @Nullable
    private static ClassDefinition getClassDefinition(@Nonnull String classId) {
        var map = ClassDefinition.getAssetMap();
        if (map == null) {
            return null;
        }

        ClassDefinition def = map.getAsset(classId);
        if (def != null) {
            return def;
        }

        // Be forgiving about case on IDs.
        return map.getAsset(classId.toLowerCase());
    }

    @Nonnull
    private static ClassManagementSystem getClassManagementSystem() {
        return ExamplePlugin.get().getClassManagementSystem();
    }
}
