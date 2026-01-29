package org.pixelbays.rpg.classes.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.classes.system.ClassManagementSystem;
import org.pixelbays.rpg.global.system.RpgLogging;
import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;
import org.pixelbays.rpg.leveling.config.LevelRewardConfig;
import org.pixelbays.rpg.leveling.config.LevelSystemConfig;
import org.pixelbays.rpg.leveling.config.LevelUpEffects;
import org.pixelbays.rpg.leveling.config.StatGrowthConfig;
import org.pixelbays.rpg.leveling.system.LevelProgressionSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class debug <className> - Dump class data to chat and console
 */
@SuppressWarnings("null")
public class ClassDebugCommand extends AbstractPlayerCommand {

    private final ClassManagementSystem classSystem;
    private final ClassAbilitySystem abilitySystem;
    private final LevelProgressionSystem levelSystem;
    private final RequiredArg<String> classNameArg;

    public ClassDebugCommand() {
        super("debug", "Dump class data to chat and console");
        this.classSystem = ExamplePlugin.get().getClassManagementSystem();
        this.abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        this.levelSystem = ExamplePlugin.get().getLevelProgressionSystem();
        this.classNameArg = this.withRequiredArg("className", "The class to debug", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classNameArg.get(ctx);

        ClassDefinition classDef = classSystem.getClassDefinition(classId);
        if (classDef == null) {
            player.sendMessage(Message.raw("Class not found: " + classId));
            return;
        }

        ClassComponent classComp = store.getComponent(ref, ExamplePlugin.get().getClassComponentType());
        boolean learned = classComp != null && classComp.hasLearnedClass(classId);
        boolean active = classComp != null && classId.equals(classComp.getActiveClassId());

        String systemId = classDef.usesCharacterLevel() ? "character_level" : classDef.getLevelSystemId();
        int currentLevel = systemId != null && !systemId.isEmpty() ? levelSystem.getLevel(ref, systemId) : 0;
        float expToNext = systemId != null && !systemId.isEmpty() ? levelSystem.getExpToNextLevel(ref, systemId) : 0f;
        float currentExp = systemId != null && !systemId.isEmpty() ? levelSystem.getExperience(ref, systemId) : 0f;
        float expProgress = systemId != null && !systemId.isEmpty() ? levelSystem.getExpProgress(ref, systemId) : 0f;
        int statPoints = systemId != null && !systemId.isEmpty() ? levelSystem.getStatPoints(ref, systemId) : 0;
        int skillPoints = systemId != null && !systemId.isEmpty() ? levelSystem.getSkillPoints(ref, systemId) : 0;
        LevelSystemConfig levelConfig = systemId != null && !systemId.isEmpty() ? levelSystem.getConfig(systemId)
                : null;

        List<String> lines = new ArrayList<>();
        lines.add("=== Class Debug: " + classDef.getDisplayName() + " ===");
        lines.add("ClassId: " + classDef.getId());
        lines.add("ParentClassId: " + classDef.getParent());
        lines.add("Enabled: " + classDef.isEnabled() + ", Visible: " + classDef.isVisible());
        lines.add("StartingClass: " + classDef.isStartingClass() + ", UsesCharacterLevel: "
                + classDef.usesCharacterLevel());
        lines.add("LevelSystemId: " + classDef.getLevelSystemId());
        lines.add("Learned: " + learned + ", Active: " + active + ", CurrentLevel: " + currentLevel + ", ExpToNext: "
                + expToNext
                + ", CurrentExp: " + currentExp + ", ExpProgress: " + (int) (expProgress * 100) + "%"
                + ", StatPoints: " + statPoints + ", SkillPoints: " + skillPoints);
        lines.add("IconId: " + classDef.getIconId());
        lines.add("Description: " + classDef.getDescription());

        if (!classDef.getRequiredClasses().isEmpty()) {
            lines.add("RequiredClasses: " + String.join(", ", classDef.getRequiredClasses()));
        }

        if (!classDef.getExclusiveWith().isEmpty()) {
            lines.add("ExclusiveWith: " + String.join(", ", classDef.getExclusiveWith()));
        }

        if (!classDef.getResourceStats().isEmpty()) {
            // lines.add("ResourceStats: " + String.join(", ",
            // classDef.getResourceStats().keySet()));
        }

        ClassDefinition.StatModifiers baseStats = classDef.getBaseStatModifiers();
        if (baseStats != null && !baseStats.isEmpty()) {
            lines.add("BaseStatModifiers:");
            baseStats.getAdditiveModifiers().forEach((stat, value) -> lines.add("  +" + value + " " + stat));
            baseStats.getMultiplicativeModifiers()
                    .forEach((stat, value) -> lines.add("  +" + (value * 100) + "% " + stat));
        }

        ClassDefinition.StatModifiers perLevelStats = classDef.getPerLevelModifiers();
        if (perLevelStats != null && !perLevelStats.isEmpty()) {
            lines.add("PerLevelModifiers:");
            perLevelStats.getAdditiveModifiers().forEach((stat, value) -> lines.add("  +" + value + " " + stat));
            perLevelStats.getMultiplicativeModifiers()
                    .forEach((stat, value) -> lines.add("  +" + (value * 100) + "% " + stat));
        }

        ClassDefinition.EquipmentRestrictions equip = classDef.getEquipmentRestrictions();
        if (equip != null) {
            lines.add("EquipmentRestrictions: " + equip.getRestrictionMode());
            if (!equip.getAllowedWeaponTypes().isEmpty()) {
                lines.add("  Weapons: " + String.join(", ", equip.getAllowedWeaponTypes()));
            }
            if (!equip.getAllowedArmorTypes().isEmpty()) {
                lines.add("  Armor: " + String.join(", ", equip.getAllowedArmorTypes()));
            }
            if (!equip.getRequiredItems().isEmpty()) {
                lines.add("  RequiredItems: " + String.join(", ", equip.getRequiredItems()));
            }
        }

        java.util.List<String> abilityIds = new ArrayList<>(classDef.getAbilityIds());
        abilityIds.sort(String::compareToIgnoreCase);
        if (!abilityIds.isEmpty()) {
            lines.add("Abilities: " + abilityIds.size());
            for (String abilityId : abilityIds) {
                ClassAbilityDefinition abilityDef = abilitySystem.getAbilityDefinition(abilityId);
                String display = abilityDef != null && abilityDef.getDisplayName() != null
                        && !abilityDef.getDisplayName().isEmpty()
                                ? abilityDef.getDisplayName() + " (" + abilityId + ")"
                                : abilityId;
                lines.add("  - " + display);
            }
        }

        if (!classDef.getLevelMilestones().isEmpty()) {
            lines.add("LevelMilestones: " + classDef.getLevelMilestones().size());
            for (ClassDefinition.LevelMilestone milestone : classDef.getLevelMilestones()) {
                lines.add("  - level " + milestone.getLevel() + ", skillPoints " + milestone.getSkillPoints()
                        + ", items " + milestone.getItemRewards().size()
                        + (milestone.getInteractionChain() != null ? ", chain " + milestone.getInteractionChain()
                                : ""));
            }
        }

        if (!classDef.getTalentTrees().isEmpty()) {
            lines.add("TalentTrees: " + classDef.getTalentTrees().size());
        }

        ClassDefinition.ClassSwitchingRules switching = classDef.getSwitchingRules();
        if (switching != null) {
            lines.add("SwitchingRules: canSwitch=" + switching.canSwitch()
                    + ", canSwitchInCombat=" + switching.canSwitchInCombat()
                    + ", cooldown=" + switching.getSwitchCooldown());
        }

        lines.add("RelearnExpPenalty: " + classDef.getRelearnExpPenalty());

        lines.add("--- Level System Debug ---");
        lines.add("LevelSystemId: " + systemId);

        if (levelConfig == null) {
            lines.add("LevelSystemConfig: NOT FOUND");
        } else {
            lines.add("LevelSystemName: " + levelConfig.getDisplayName());
            lines.add("LevelSystemDescription: " + levelConfig.getDescription());
            lines.add("LevelSystemEnabled: " + levelConfig.isEnabled() + ", Visible: " + levelConfig.isVisible());
            lines.add("LevelSystemStartingLevel: " + levelConfig.getStartingLevel() + ", MaxLevel: "
                    + levelConfig.getMaxLevel());
            lines.add("LevelSystemIconId: " + levelConfig.getIconId());
            lines.add("LevelSystemInheritsFrom: " + levelConfig.getInheritsFrom());
            lines.add("LevelSystemExpCurveType: " + levelConfig.getExpCurveType() + ", ExpCurveRef: "
                    + levelConfig.getExpCurveRef());

            ExpCurveDefinition expCurve = levelConfig.getExpCurve();
            if (expCurve != null) {
                lines.add("ExpCurve: type=" + expCurve.getType()
                        + ", baseExp=" + expCurve.getBaseExp()
                        + ", growthRate=" + expCurve.getGrowthRate()
                        + ", exponent=" + expCurve.getExponent()
                        + ", customFormula=" + expCurve.getCustomFormula());
            }

            if (levelConfig.getExpTable() != null && !levelConfig.getExpTable().isEmpty()) {
                lines.add("ExpTableSize: " + levelConfig.getExpTable().size());
            }

            if (levelConfig.getPrerequisites() != null && !levelConfig.getPrerequisites().isEmpty()) {
                lines.add("LevelSystemPrerequisites:");
                for (Map.Entry<String, Integer> prereq : levelConfig.getPrerequisites().entrySet()) {
                    lines.add("  - " + prereq.getKey() + " >= " + prereq.getValue());
                }
            }

            LevelRewardConfig defaultRewards = levelConfig.getDefaultRewards();
            if (defaultRewards != null && hasAnyRewards(defaultRewards)) {
                lines.add("DefaultRewards: statPoints=" + defaultRewards.getStatPoints()
                        + ", skillPoints=" + defaultRewards.getSkillPoints()
                        + ", statIncreases=" + defaultRewards.getStatIncreases().size()
                        + ", abilities=" + defaultRewards.getUnlockedAbilities().size()
                        + ", quests=" + defaultRewards.getUnlockedQuests().size()
                        + ", currency=" + defaultRewards.getCurrencyRewards().size()
                        + ", items=" + defaultRewards.getItemRewards().size()
                        + (defaultRewards.getInteractionChain() != null
                                ? ", interactionChain=" + defaultRewards.getInteractionChain()
                                : ""));

                LevelUpEffects effects = defaultRewards.getLevelUpEffects();
                if (effects != null && !effects.isEmpty()) {
                    lines.add("DefaultRewardsEffects: sound=" + effects.getSoundId()
                            + ", particle=" + effects.getParticleEffect()
                            + ", notification=" + (effects.getNotification() != null)
                            + ", eventTitle=" + (effects.getEventTitle() != null));
                }
            }

            if (levelConfig.getLevelRewards() != null && !levelConfig.getLevelRewards().isEmpty()) {
                lines.add("LevelRewardsCount: " + levelConfig.getLevelRewards().size());
            }

            StatGrowthConfig statGrowth = levelConfig.getStatGrowth();
            if (statGrowth != null) {
                int flatCount = statGrowth.getFlatGrowth() == null ? 0 : statGrowth.getFlatGrowth().size();
                int percentCount = statGrowth.getPercentageGrowth() == null ? 0
                        : statGrowth.getPercentageGrowth().size();
                int milestoneCount = statGrowth.getMilestoneGrowth() == null ? 0
                        : statGrowth.getMilestoneGrowth().size();

                if (flatCount > 0 || percentCount > 0 || milestoneCount > 0) {
                    lines.add("StatGrowth: flat=" + flatCount + ", percent=" + percentCount + ", milestones="
                            + milestoneCount);
                }
            }
        }

        for (String line : lines) {
            player.sendMessage(Message.raw(line));
            RpgLogging.debugDeveloper("[ClassDebug] %s", line);
        }
    }

    private static boolean hasAnyRewards(@Nonnull LevelRewardConfig rewards) {
        if (rewards.getStatPoints() > 0 || rewards.getSkillPoints() > 0) {
            return true;
        }

        if (rewards.getStatIncreases() != null && !rewards.getStatIncreases().isEmpty()) {
            return true;
        }

        if (rewards.getUnlockedAbilities() != null && !rewards.getUnlockedAbilities().isEmpty()) {
            return true;
        }

        if (rewards.getUnlockedQuests() != null && !rewards.getUnlockedQuests().isEmpty()) {
            return true;
        }

        if (rewards.getCurrencyRewards() != null && !rewards.getCurrencyRewards().isEmpty()) {
            return true;
        }

        if (rewards.getItemRewards() != null && !rewards.getItemRewards().isEmpty()) {
            return true;
        }

        if (rewards.getInteractionChain() != null && !rewards.getInteractionChain().isEmpty()) {
            return true;
        }

        LevelUpEffects effects = rewards.getLevelUpEffects();
        return effects != null && !effects.isEmpty();
    }
}
