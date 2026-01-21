Scan complete. Findings:

Critical bug: calculateTotalModifiers() returns null, so recalculateStatBonuses() will throw NPE and never apply stats if called. See ClassStatBonusSystem.java:127.
Unused/unfinished system: ClassStatBonusSystem is only placeholder logging with TODOs and no EntityStatMap integration. See ClassStatBonusSystem.java:26-219.
Not wired up: no calls to recalculateStatBonuses() when classes are learned/switched/unlearned. There are TODOs where this should happen. See ClassManagementSystem.java:202-240.
Potential NPE: ClassUseAbilityCommand uses classComp without null check. See ClassUseAbilityCommand.java:58.
Debug noise: System.out.println left in ClassProgressCommand. See ClassProgressCommand.java:99.
Requirements output incomplete: ClassInfoCommand shows “Requirements” but doesn’t include level prerequisites from Prerequisites. See ClassInfoCommand.java:80.
Switching rules incomplete: combat and cooldown checks are TODOs. See ClassManagementSystem.java:234-235.
