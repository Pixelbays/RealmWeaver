package org.pixelbays.rpg.leveling.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.classes.config.settings.ClassModSettings.XpRoutingMode;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.leveling.event.GiveXPEvent;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.party.PartyMemberType;
import org.pixelbays.rpg.party.PartySettings;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;

@SuppressWarnings("null")
public class GiveXPHandler implements Consumer<GiveXPEvent> {

    private static final String EXP_RATE_GAIN_STAT = "Exp_Rate_Gain";
    private static final String BASE_SYSTEM_ID = "Base_Character_Level";
    private static final String TAG_KEY = "type";
    private static final String BASE_TAG = "base";
    private static final String RESTED_XP_STAT = "Rested_XP";

    @Override
    public void accept(GiveXPEvent event) {
        handle(event, true);
    }

    private void handle(GiveXPEvent event, boolean allowPartyShare) {
        if (!event.playerRef().isValid()) return;

        RpgModConfig config = resolveConfig();
        if (config == null || !config.isLevelingModuleEnabled()) {
            return;
        }

        if (event.amount() <= 0L) {
            return;
        }

        var store = event.playerRef().getStore();
        var levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        var classSystem = ExamplePlugin.get().getClassManagementSystem();

        if (allowPartyShare && tryDistributePartyXp(event, store)) {
            return;
        }

        String rawType = event.type();
        String type = rawType == null ? "" : rawType.trim();
        String typeLower = type.isEmpty() ? "" : type.toLowerCase(Locale.ROOT);

        List<String> xpTags = config != null ? config.getXpTags() : null;
        Object2FloatMap<String> xpTagSplits = config != null ? config.getXpTagSplits() : null;
        XpRoutingMode routing = config != null ? config.getXpRouting()
            : XpRoutingMode.ActiveClassOnly;

        var classComp = store.getComponent(event.playerRef(), ClassComponent.getComponentType());
        if (classComp != null) {
            classSystem.pruneUnknownClasses(event.playerRef(), store);
            classComp = store.getComponent(event.playerRef(), ClassComponent.getComponentType());
        }
        ClassDefinition primaryClass = resolvePrimaryClass(classComp);
        RaceDefinition activeRace = resolveRace(event.playerRef(), store);
        float expToGrant = applyExperienceModifiers(event.amount(), event.playerRef(), store, config, primaryClass, activeRace);
        expToGrant = applyRestedXpBonus(expToGrant, event.playerRef(), store, config, classComp, primaryClass, typeLower);
        if (expToGrant <= 0f) {
            return;
        }

        if (isBaseType(typeLower)) {
            grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
            return;
        }

        if (!type.isEmpty() && levelSystem.getConfig(type) != null) {
            grantExperience(levelSystem, event.playerRef(), type, expToGrant, store);
            return;
        }

        if (classComp == null || classComp.getLearnedClassIds().isEmpty()) {
            grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
            return;
        }

        if (routing == XpRoutingMode.SplitByTag && typeLower.isEmpty()) {
            if (!applySplitByTag(levelSystem, event.playerRef(), classComp, xpTags, xpTagSplits, expToGrant, store)) {
                grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
            }
            return;
        }

        if (!typeLower.isEmpty() && isKnownTag(typeLower, xpTags)) {
            switch (routing) {
                case ActiveClassOnly -> {
                    if (!grantToActiveClass(levelSystem, event.playerRef(), classComp, typeLower, expToGrant, store)) {
                        if (!grantToMatchingClasses(levelSystem, event.playerRef(), classComp, typeLower, expToGrant, store)) {
                            grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
                        }
                    }
                    return;
                }
                case SplitByTag -> {
                    float split = getSplitForTag(typeLower, xpTagSplits);
                    float splitAmount = expToGrant * split;
                    if (splitAmount > 0f) {
                        if (!grantToMatchingClasses(levelSystem, event.playerRef(), classComp, typeLower, splitAmount, store)) {
                            grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, splitAmount, store);
                        }
                    }
                    return;
                }
                case AllMatchingTags -> {
                    if (!grantToMatchingClasses(levelSystem, event.playerRef(), classComp, typeLower, expToGrant, store)) {
                        grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
                    }
                    return;
                }
                default -> {
                    break;
                }
            }
        }

        if (!grantToActiveClass(levelSystem, event.playerRef(), classComp, null, expToGrant, store)) {
            grantExperience(levelSystem, event.playerRef(), BASE_SYSTEM_ID, expToGrant, store);
        }
    }

    private boolean tryDistributePartyXp(@Nonnull GiveXPEvent event, @Nonnull Store<EntityStore> store) {
        PartyManager partyManager = ExamplePlugin.get().getPartyManager();
        if (partyManager == null) {
            return false;
        }

        UUIDComponent uuidComponent = store.getComponent(event.playerRef(), UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return false;
        }

        UUID playerId = uuidComponent.getUuid();
        Party party = partyManager.getPartyForMember(playerId);
        if (party == null) {
            return false;
        }

        PartySettings settings = party.getSettings();
        if (settings == null || !settings.isXpEnabled()) {
            return false;
        }

        List<Ref<EntityStore>> eligibleRefs = resolvePartyEligiblePlayersInRange(event.playerRef(), store, party, settings);
        if (eligibleRefs.size() <= 1) {
            return false;
        }

        int minMembers = Math.max(1, settings.getXpMinMembersInRange());
        if (eligibleRefs.size() < minMembers) {
            return false;
        }

        long total = event.amount();
        if (total <= 0L) {
            return true;
        }

        String type = event.type();

        switch (settings.getXpGrantingMode()) {
            case FullInRange -> {
                for (Ref<EntityStore> ref : eligibleRefs) {
                    handle(new GiveXPEvent(ref, total, type), false);
                }
            }
            case SplitEqualInRange -> {
                int count = eligibleRefs.size();
                long per = total / count;
                long remainder = total % count;
                for (int i = 0; i < count; i++) {
                    long share = per + (i < remainder ? 1L : 0L);
                    if (share <= 0L) {
                        continue;
                    }
                    handle(new GiveXPEvent(eligibleRefs.get(i), share, type), false);
                }
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    private List<Ref<EntityStore>> resolvePartyEligiblePlayersInRange(
            @Nonnull Ref<EntityStore> sourceRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull Party party,
            @Nonnull PartySettings settings) {
        int rangeBlocks = settings.getXpRangeBlocks();
        double rangeSq = rangeBlocks > 0 ? (double) rangeBlocks * (double) rangeBlocks : -1.0;

        Vector3d sourcePos = null;
        if (rangeBlocks > 0) {
            TransformComponent sourceTransform = store.getComponent(sourceRef, TransformComponent.getComponentType());
            if (sourceTransform == null) {
                return new ArrayList<>();
            }
            sourcePos = sourceTransform.getPosition();
        }

        List<Ref<EntityStore>> eligible = new ArrayList<>();
        for (PartyMember member : party.getMemberList()) {
            if (member == null || member.getMemberType() != PartyMemberType.PLAYER) {
                continue;
            }

            UUID memberId = member.getEntityId();
            if (memberId == null) {
                continue;
            }

            PlayerRef playerRef = Universe.get().getPlayer(memberId);
            if (playerRef == null) {
                continue;
            }

            Ref<EntityStore> memberRef = playerRef.getReference();
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }

            if (memberRef.getStore() != store) {
                continue;
            }

            if (rangeSq >= 0.0) {
                TransformComponent memberTransform = store.getComponent(memberRef, TransformComponent.getComponentType());
                if (memberTransform == null) {
                    continue;
                }

                Vector3d pos = memberTransform.getPosition();
                double dx = pos.getX() - sourcePos.getX();
                double dy = pos.getY() - sourcePos.getY();
                double dz = pos.getZ() - sourcePos.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > rangeSq) {
                    continue;
                }
            }

            eligible.add(memberRef);
        }

        // Ensure the source is always eligible (even if missing from party member list for any reason)
        if (!eligible.contains(sourceRef)) {
            eligible.add(sourceRef);
        }

        return eligible;
    }

    private boolean applySplitByTag(LevelProgressionSystem levelSystem,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull ClassComponent classComp,
            List<String> xpTags,
            Object2FloatMap<String> xpTagSplits,
            float amount,
            Store<EntityStore> store) {
        if (amount <= 0f || xpTags == null || xpTags.isEmpty()) {
            return false;
        }

        boolean granted = false;
        for (String tag : xpTags) {
            if (tag == null || tag.isEmpty()) {
                continue;
            }

            String tagLower = tag.toLowerCase(Locale.ROOT);
            float split = getSplitForTag(tagLower, xpTagSplits);
            if (split <= 0f) {
                continue;
            }

            float splitAmount = amount * split;
            if (splitAmount <= 0f) {
                continue;
            }

            if (BASE_TAG.equalsIgnoreCase(tagLower)) {
                grantExperience(levelSystem, playerRef, BASE_SYSTEM_ID, splitAmount, store);
                granted = true;
                continue;
            }

            boolean tagGranted = grantToMatchingClasses(levelSystem, playerRef, classComp, tagLower, splitAmount, store);
            granted = granted || tagGranted;
        }

        return granted;
    }

    private boolean grantToActiveClass(LevelProgressionSystem levelSystem,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull ClassComponent classComp,
            String requiredTag,
            float amount,
            Store<EntityStore> store) {
        if (amount <= 0f) {
            return false;
        }

        String primaryClassId = classComp.getPrimaryClassId();
        if (primaryClassId == null || primaryClassId.isEmpty()) {
            return false;
        }

        ClassDefinition activeDef = ClassDefinition.getAssetMap().getAsset(primaryClassId);
        if (activeDef == null || !activeDef.isEnabled()) {
            return false;
        }

        if (requiredTag != null && !requiredTag.isEmpty() && !matchesTag(activeDef, requiredTag)) {
            return false;
        }

        String systemId = activeDef.usesCharacterLevel()
                ? BASE_SYSTEM_ID
                : activeDef.getLevelSystemId();

        if (systemId == null || systemId.isEmpty()) {
            return false;
        }

        grantExperience(levelSystem, playerRef, systemId, amount, store);
        return true;
    }

    private boolean grantToMatchingClasses(LevelProgressionSystem levelSystem,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull ClassComponent classComp,
            String tag,
            float amount,
            Store<EntityStore> store) {
        if (amount <= 0f || tag == null || tag.isEmpty()) {
            return false;
        }

        boolean hasCharacterLevel = false;
        List<ClassDefinition> matchingClasses = new ArrayList<>();

        for (String classId : classComp.getLearnedClassIds()) {
            ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
            if (classDef == null || !classDef.isEnabled()) {
                continue;
            }

            if (!matchesTag(classDef, tag)) {
                continue;
            }

            if (classDef.usesCharacterLevel()) {
                hasCharacterLevel = true;
                continue;
            }

            String systemId = classDef.getLevelSystemId();
            if (systemId == null || systemId.isEmpty()) {
                continue;
            }

            matchingClasses.add(classDef);
        }

        if (hasCharacterLevel) {
            grantExperience(levelSystem, playerRef, BASE_SYSTEM_ID, amount, store);
            return true;
        }

        if (matchingClasses.isEmpty()) {
            return false;
        }

        int classCount = matchingClasses.size();
        float perClass = amount / classCount;

        for (int i = 0; i < classCount; i++) {
            ClassDefinition classDef = matchingClasses.get(i);
            String systemId = classDef.getLevelSystemId();
            if (systemId == null || systemId.isEmpty()) {
                continue;
            }

            float grantAmount = perClass;
            if (grantAmount <= 0f) {
                continue;
            }

            grantExperience(levelSystem, playerRef, systemId, grantAmount, store);
        }

        return true;
    }

    private boolean isKnownTag(String tag, List<String> xpTags) {
        if (tag == null || tag.isEmpty() || xpTags == null || xpTags.isEmpty()) {
            return false;
        }

        for (String entry : xpTags) {
            if (entry != null && entry.equalsIgnoreCase(tag)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTag(@Nonnull ClassDefinition classDef, String tag) {
        return tag != null && !tag.isEmpty() && classDef.hasTag(TAG_KEY, tag);
    }

    private float getSplitForTag(String tag, Object2FloatMap<String> xpTagSplits) {
        if (tag == null || tag.isEmpty() || xpTagSplits == null || xpTagSplits.isEmpty()) {
            return 1.0f;
        }

        for (Object2FloatMap.Entry<String> entry : xpTagSplits.object2FloatEntrySet()) {
            String key = entry.getKey();
            if (key != null && key.equalsIgnoreCase(tag)) {
                return entry.getFloatValue();
            }
        }

        return 1.0f;
    }

    private boolean isBaseType(String type) {
        return type != null && (type.equalsIgnoreCase(BASE_TAG)
                || type.equalsIgnoreCase("character")
                || type.equalsIgnoreCase("Base_Character_Level")
                || type.equalsIgnoreCase(BASE_SYSTEM_ID));
    }

    private RpgModConfig resolveConfig() {
        var assetMap = RpgModConfig.getAssetMap();
        if (assetMap == null) {
            return null;
        }

        RpgModConfig config = assetMap.getAsset("Default");
        if (config != null) {
            return config;
        }

        if (assetMap.getAssetMap().isEmpty()) {
            return null;
        }

        return assetMap.getAssetMap().values().iterator().next();
    }

    private void grantExperience(LevelProgressionSystem levelSystem,
            @Nonnull Ref<EntityStore> playerRef,
            String systemId,
            float amount,
            Store<EntityStore> store) {
        if (amount <= 0f) {
            return;
        }

        if (!levelSystem.hasLevelSystem(playerRef, systemId)) {
            levelSystem.initializeLevelSystem(playerRef, systemId);
        }

        World world = store.getExternalData().getWorld();
        levelSystem.grantExperience(playerRef, systemId, amount, "event:give_xp", store, world);
    }

        private float applyExperienceModifiers(long baseExp,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            RpgModConfig config,
            ClassDefinition activeClass,
            RaceDefinition activeRace) {
        if (baseExp <= 0L) {
            return 0f;
        }

        float expToGrant = baseExp;

        if (config != null) {
            float baseMultiplier = config.getBaseXpMultiplier();
            if (baseMultiplier < 0f) {
                baseMultiplier = 0f;
            }
            expToGrant *= baseMultiplier;
        }

        int classLevel = resolveClassLevel(playerRef, store, activeClass);
        float classBonusPercent = getClassExpBonusPercent(activeClass, classLevel);
        float raceBonusPercent = getRaceExpBonusPercent(activeRace);

        expToGrant = applyPercentBonus(expToGrant, classBonusPercent + raceBonusPercent);
        return expToGrant;
    }

    private ClassDefinition resolvePrimaryClass(ClassComponent classComp) {
        if (classComp == null) {
            return null;
        }

        ClassManagementSystem classSystem = ExamplePlugin.get().getClassManagementSystem();
        String primaryClassId = classSystem.getPrimaryKnownClassId(classComp);
        if (primaryClassId == null || primaryClassId.isEmpty()) {
            return null;
        }

        return ClassDefinition.getAssetMap().getAsset(primaryClassId);
    }

    private RaceDefinition resolveRace(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        RaceComponent raceComp = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (raceComp == null) {
            return null;
        }

        String raceId = raceComp.getRaceId();
        if (raceId == null || raceId.isEmpty()) {
            return null;
        }

        return RaceDefinition.getAssetMap().getAsset(raceId);
    }

        private int resolveClassLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            ClassDefinition classDef) {
        if (classDef == null) {
            return 1;
        }

        String systemId = resolveLevelSystemId(classDef);
        if (systemId == null || systemId.isEmpty()) {
            return 1;
        }

        LevelProgressionComponent levelComp = store.getComponent(entityRef, LevelProgressionComponent.getComponentType());
        if (levelComp == null) {
            return 1;
        }

        LevelProgressionComponent.LevelSystemData data = levelComp.getSystem(systemId);
        if (data == null) {
            return 1;
        }

        return Math.max(1, data.getCurrentLevel());
    }

    private String resolveLevelSystemId(ClassDefinition classDef) {
        if (classDef.usesCharacterLevel()) {
            return BASE_SYSTEM_ID;
        }

        return classDef.getLevelSystemId();
    }

    private float getClassExpBonusPercent(ClassDefinition classDef, int classLevel) {
        if (classDef == null) {
            return 0f;
        }

        float bonus = 0f;

        ClassDefinition.StatModifiers base = classDef.getBaseStatModifiers();
        bonus += getModifierPercent(base != null ? base.getAdditiveModifiers() : null);
        bonus += getModifierPercent(base != null ? base.getMultiplicativeModifiers() : null);

        ClassDefinition.StatModifiers perLevel = classDef.getPerLevelModifiers();
        if (perLevel != null) {
            bonus += getModifierPercent(perLevel.getAdditiveModifiers()) * classLevel;
            bonus += getModifierPercent(perLevel.getMultiplicativeModifiers()) * classLevel;
        }

        return bonus;
    }

    private float getRaceExpBonusPercent(RaceDefinition raceDef) {
        if (raceDef == null || raceDef.getStatModifiers() == null) {
            return 0f;
        }

        var stats = raceDef.getStatModifiers();
        return getModifierPercent(stats.getAdditiveModifiers())
                + getModifierPercent(stats.getMultiplicativeModifiers());
    }

    private float getModifierPercent(Object2FloatMap<String> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return 0f;
        }

        return modifiers.getOrDefault(EXP_RATE_GAIN_STAT, 0f);
    }

    private float applyPercentBonus(float base, float percent) {
        if (percent == 0f) {
            return base;
        }

        float multiplier = 1.0f + (percent / 100.0f);
        if (multiplier < 0f) {
            multiplier = 0f;
        }

        return base * multiplier;
    }

    private float applyRestedXpBonus(float baseExp,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            RpgModConfig config,
            ClassComponent classComp,
            ClassDefinition primaryClass,
            String typeLower) {
        if (baseExp <= 0f || config == null || !config.isRestedXpEnabled()) {
            return baseExp;
        }

        List<String> restedTags = config.getRestedXpGainTags();
        if (restedTags == null || restedTags.isEmpty()) {
            return baseExp;
        }

        if (!shouldApplyRestedXp(classComp, primaryClass, typeLower, restedTags)) {
            return baseExp;
        }

        EntityStatMap statMap = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return baseExp;
        }

        int restedIndex = EntityStatType.getAssetMap().getIndex(RESTED_XP_STAT);
        if (restedIndex == Integer.MIN_VALUE) {
            return baseExp;
        }

        EntityStatValue restedValue = statMap.get(restedIndex);
        if (restedValue == null) {
            return baseExp;
        }

        float restedAvailable = restedValue.get();
        if (restedAvailable <= 0f) {
            return baseExp;
        }

        int bonusPercent = config.getRestedXpBonusPercent();
        if (bonusPercent <= 0) {
            return baseExp;
        }

        int ratio = config.getRestedXpConsumeRatio();
        if (ratio <= 0) {
            ratio = 1;
        }

        float desiredBonus = baseExp * (bonusPercent / 100f);
        if (desiredBonus <= 0f) {
            return baseExp;
        }

        float requiredRested = desiredBonus / ratio;
        float bonus;
        float consume;

        if (restedAvailable >= requiredRested) {
            bonus = desiredBonus;
            consume = requiredRested;
        } else {
            bonus = restedAvailable * ratio;
            consume = restedAvailable;
        }

        if (consume > 0f) {
            statMap.subtractStatValue(restedIndex, consume);
            RpgLogging.debugDeveloper("Rested XP consumed: %s (bonus=%s ratio=%s)", consume, bonus, ratio);
        }

        return baseExp + bonus;
    }

        private boolean shouldApplyRestedXp(ClassComponent classComp,
            ClassDefinition primaryClass,
            String typeLower,
            List<String> restedTags) {
        if (typeLower != null && !typeLower.isEmpty()) {
            if (isKnownTag(typeLower, restedTags)) {
                return true;
            }

            if (classComp != null) {
                for (String classId : classComp.getLearnedClassIds()) {
                    ClassDefinition classDef = ClassDefinition.getAssetMap().getAsset(classId);
                    if (classDef == null || !classDef.isEnabled()) {
                        continue;
                    }

                    String systemId = classDef.getLevelSystemId();
                    if (systemId != null && systemId.equalsIgnoreCase(typeLower)
                            && hasAnyTag(classDef, restedTags)) {
                        return true;
                    }
                }
            }
        }

        return primaryClass != null && hasAnyTag(primaryClass, restedTags);
    }

    private boolean hasAnyTag(@Nonnull ClassDefinition classDef, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }

        for (String tag : tags) {
            if (tag != null && !tag.isEmpty() && classDef.hasTag(TAG_KEY, tag)) {
                return true;
            }
        }

        return false;
    }
}