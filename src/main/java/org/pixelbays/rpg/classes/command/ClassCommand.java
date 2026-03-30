package org.pixelbays.rpg.classes.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.pixelbays.rpg.classes.talent.command.ClassTalentCommand;
import org.pixelbays.rpg.global.config.RpgModConfig;

/**
 * Main class command collection
 * Usage:
 *   /class list - List all available classes
 *   /class info <className> - Show class details
 *   /class learn <className> - Learn a new class
 *   /class unlearn <className> - Unlearn a class
 *   /class switch <className> - Switch active class
 *   /class abilities [className] - View class abilities
 *   /class progress [className] - View class progress
 *   /class setlevel <className> <level> - Set a class level directly
 *   /class levelup <className> - Grant enough XP to reach next level
 *   /class debug <className> - Dump class data to chat and console
 *   /class useability <abilityId> - Trigger an ability for testing
 *   /class talent info [className] - View talent tree allocations
 *   /class talent allocate <className> <treeId> <nodeId> - Spend a talent point
 *   /class talent refund <className> <treeId> <nodeId> - Refund one rank (Free/Partial mode)
 *   /class talent reset <className> - Reset all talents for a class
 */
public class ClassCommand extends AbstractCommandCollection {

    public ClassCommand() {
        super("class", "Manage your classes and jobs");
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        boolean abilityModuleEnabled = config == null || config.getAbilitySettings().isEnabled();
        boolean levelingModuleEnabled = config == null || config.getLevelingSettings().isEnabled();
        boolean talentModuleEnabled = config == null || config.getTalentSettings().isEnabled();

        this.addSubCommand(new ClassListCommand());
        this.addSubCommand(new ClassInfoCommand());
        this.addSubCommand(new ClassLearnCommand());
        this.addSubCommand(new ClassUnlearnCommand());
        this.addSubCommand(new ClassSwitchCommand());
        this.addSubCommand(new ClassDebugCommand());
        if (abilityModuleEnabled) {
            this.addSubCommand(new ClassAbilitiesCommand());
            this.addSubCommand(new ClassUseAbilityCommand());
        }
        if (levelingModuleEnabled) {
            this.addSubCommand(new ClassProgressCommand());
            this.addSubCommand(new ClassSetLevelCommand());
            this.addSubCommand(new ClassLevelUpCommand());
        }
        if (talentModuleEnabled) {
            this.addSubCommand(new ClassTalentCommand());
        }
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
