package org.pixelbays.rpg.global.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.drop.ExpItemDropContainer;
import org.pixelbays.rpg.leveling.component.LevelProgressionComponent;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;
import org.pixelbays.rpg.race.component.RaceComponent;
import org.pixelbays.rpg.race.config.RaceDefinition;
import org.pixelbays.rpg.race.system.RaceManagementSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;

/**
 * Centralized XP grant system.
 *
 * Applies global config, class/race config modifiers, and entity stat modifiers
 * (Exp_Rate_Gain) before granting XP via LevelProgressionSystem.
 */
@SuppressWarnings({"null", "deprecation"})
public class XpGrantSystem {

    private static final String EXP_RATE_GAIN_STAT = "Exp_Rate_Gain";

    private final LevelProgressionSystem levelProgressionSystem;
    private final ClassManagementSystem classManagementSystem;
    private final RaceManagementSystem raceManagementSystem;

    public XpGrantSystem(@Nonnull LevelProgressionSystem levelProgressionSystem,
            @Nonnull ClassManagementSystem classManagementSystem,
            @Nonnull RaceManagementSystem raceManagementSystem) {
        this.levelProgressionSystem = levelProgressionSystem;
        this.classManagementSystem = classManagementSystem;
        this.raceManagementSystem = raceManagementSystem;
    }

    /**
     * Grant XP using the active class (or character level) system.
     */
    public void grantExperience(@Nonnull Ref<EntityStore> entityRef,
            float baseExp,
            @Nonnull String source,
            @Nonnull Store<EntityStore> store,
            @Nullable World world) {
        grantExperience(entityRef, baseExp, source, store, world, null);
    }

    /**
     * Grant XP using an optional system override.
     */
    public void grantExperience(@Nonnull Ref<EntityStore> entityRef,
            float baseExp,
            @Nonnull String source,
            @Nonnull Store<EntityStore> store,
            @Nullable World world,
            @Nullable String systemOverride) {
        if (baseExp <= 0f) {
            RpgLogging.debugDeveloper("XP grant skipped (baseExp<=0): base=%s source=%s entity=%s", baseExp, source, entityRef);
            return;
        }

        RpgModConfig config = resolveConfig();
        float expToGrant = baseExp;

        if (config != null) {
            float baseMultiplier = config.getBaseXpMultiplier();
            if (baseMultiplier < 0f) {
                baseMultiplier = 0f;
            }
            expToGrant *= baseMultiplier;
            RpgLogging.debugDeveloper("XP grant base multiplier applied: base=%s multiplier=%s result=%s source=%s entity=%s", baseExp, baseMultiplier, expToGrant, source, entityRef);
        }

        ClassDefinition activeClass = resolveActiveClass(entityRef, store);
        RaceDefinition activeRace = resolveRace(entityRef, store);

        int classLevel = resolveClassLevel(entityRef, store, activeClass);
        float classBonusPercent = getClassExpBonusPercent(activeClass, classLevel);
        float raceBonusPercent = getRaceExpBonusPercent(activeRace);

        expToGrant = applyPercentBonus(expToGrant, classBonusPercent + raceBonusPercent);
        RpgLogging.debugDeveloper("XP grant bonuses applied: classBonus=%s raceBonus=%s result=%s source=%s entity=%s", classBonusPercent, raceBonusPercent, expToGrant, source, entityRef);

        if (expToGrant <= 0f) {
            RpgLogging.debugDeveloper("XP grant skipped (expToGrant<=0): result=%s source=%s entity=%s", expToGrant, source, entityRef);
            return;
        }

        String systemId = systemOverride;
        if (systemId == null || systemId.isEmpty()) {
            systemId = resolveLevelSystemId(activeClass);
        }

        if (systemId == null || systemId.isEmpty()) {
            systemId = "Base_Character_Level";
        }

        RpgLogging.debugDeveloper("XP grant final: systemId=%s amount=%s source=%s entity=%s", systemId, expToGrant, source, entityRef);
        levelProgressionSystem.grantExperience(entityRef, systemId, expToGrant, source, store, world);

        // Track total exp earned on the active class (used for relearn penalty)
        if (activeClass != null) {
            ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
            if (classComp != null) {
                ClassComponent.ClassData classData = classComp.getClassData(activeClass.getId());
                if (classData != null) {
                    classData.addTotalExp(expToGrant);
                    RpgLogging.debugDeveloper("XP grant tracked total exp: classId=%s totalAdded=%s entity=%s", activeClass.getId(), expToGrant, entityRef);
                }
            }
        }
    }

    /**
     * Attempt to grant EXP from an exp-drop ItemStack. Returns true if EXP was granted.
     */
    public boolean tryGrantExperienceFromDrop(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull ItemStack itemStack,
            @Nonnull String source,
            @Nonnull Store<EntityStore> store,
            @Nullable World world) {
        if (itemStack.isEmpty()) {
            RpgLogging.debugDeveloper("XP drop grant skipped (empty stack): source=%s entity=%s", source, entityRef);
            return false;
        }

        var metadata = itemStack.getMetadata();
        if (!ExpItemDropContainer.isExpMetadata(metadata)) {
            RpgLogging.debugDeveloper("XP drop grant skipped (not exp metadata): source=%s entity=%s", source, entityRef);
            return false;
        }

        int amount = itemStack.getQuantity();
        if (amount <= 0) {
            RpgLogging.debugDeveloper("XP drop grant skipped (amount<=0): amount=%s source=%s entity=%s", amount, source, entityRef);
            return false;
        }

        String systemOverride = ExpItemDropContainer.getSystemIdFromMetadata(metadata);
        RpgLogging.debugDeveloper("XP drop grant: amount=%s systemOverride=%s source=%s entity=%s", amount, systemOverride, source, entityRef);
        grantExperience(entityRef, amount, source, store, world, systemOverride);
        return true;
    }

    @Nullable
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

    @Nullable
    private ClassDefinition resolveActiveClass(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        ClassComponent classComp = store.getComponent(entityRef, ClassComponent.getComponentType());
        if (classComp == null) {
            return null;
        }

        String activeClassId = classComp.getActiveClassId();
        if (activeClassId == null || activeClassId.isEmpty()) {
            return null;
        }

        return classManagementSystem.getClassDefinition(activeClassId);
    }

    @Nullable
    private RaceDefinition resolveRace(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        RaceComponent raceComp = store.getComponent(entityRef, RaceComponent.getComponentType());
        if (raceComp == null) {
            return null;
        }

        String raceId = raceComp.getRaceId();
        if (raceId == null || raceId.isEmpty()) {
            return null;
        }

        return raceManagementSystem.getRaceDefinition(raceId);
    }

    private int resolveClassLevel(@Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            @Nullable ClassDefinition classDef) {
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

    @Nullable
    private String resolveLevelSystemId(@Nullable ClassDefinition classDef) {
        if (classDef == null) {
            return null;
        }

        if (classDef.usesCharacterLevel()) {
            return "Base_Character_Level";
        }

        return classDef.getLevelSystemId();
    }

    private float getClassExpBonusPercent(@Nullable ClassDefinition classDef, int classLevel) {
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

    private float getRaceExpBonusPercent(@Nullable RaceDefinition raceDef) {
        if (raceDef == null || raceDef.getStatModifiers() == null) {
            return 0f;
        }

        var stats = raceDef.getStatModifiers();
        return getModifierPercent(stats.getAdditiveModifiers())
                + getModifierPercent(stats.getMultiplicativeModifiers());
    }

    private float getModifierPercent(@Nullable Object2FloatMap<String> modifiers) {
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
}
