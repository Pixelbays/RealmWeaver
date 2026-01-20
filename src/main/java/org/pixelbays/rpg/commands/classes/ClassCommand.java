package org.pixelbays.rpg.commands.classes;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

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
 */
public class ClassCommand extends AbstractCommandCollection {

    public ClassCommand() {
        super("class", "Manage your classes and jobs");
        this.addSubCommand(new ClassListCommand());
        this.addSubCommand(new ClassInfoCommand());
        this.addSubCommand(new ClassLearnCommand());
        this.addSubCommand(new ClassUnlearnCommand());
        this.addSubCommand(new ClassSwitchCommand());
        this.addSubCommand(new ClassAbilitiesCommand());
        this.addSubCommand(new ClassProgressCommand());
        this.addSubCommand(new ClassSetLevelCommand());
        this.addSubCommand(new ClassLevelUpCommand());
        this.addSubCommand(new ClassDebugCommand());
        this.addSubCommand(new ClassUseAbilityCommand());
    }
}
