package org.pixelbays.rpg.ability.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

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
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Test command to manually unlock abilities for debugging.
 * Usage: /unlockability <abilityId>
 * Usage: /unlockability list
 */
public class UnlockAbilityCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> abilityIdArg;

    public UnlockAbilityCommand() {
        super("unlockability", "Unlock an ability for testing");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.abilityIdArg = null;
        this.addUsageVariant(new UnlockAbilityCommand("Unlock an ability for testing"));
    }

    private UnlockAbilityCommand(String description) {
        super(description);
        this.abilityIdArg = this.withRequiredArg("abilityId", "Ability ID to unlock or 'list'", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Get or create ability component
        ClassAbilityComponent abilityComp = store.getComponent(ref,
                Realmweavers.get().getClassAbilityComponentType());
        if (abilityComp == null) {
            abilityComp = store.addComponent(ref, Realmweavers.get().getClassAbilityComponentType());
        }

        // If no argument, show list
        if (abilityIdArg == null) {
            listUnlockedAbilities(player, abilityComp);
            return;
        }

        String abilityId = abilityIdArg.get(ctx);

        // Handle "list" command
        if ("list".equalsIgnoreCase(abilityId)) {
            listUnlockedAbilities(player, abilityComp);
            return;
        }

        // Validate ability exists
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.unknownAbility").param("abilityId", abilityId));
            return;
        }

        // Check if already unlocked
        if (abilityComp.hasAbility(abilityId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.alreadyUnlocked")
                    .param("ability", abilityDef.getDisplayName()));
            return;
        }

        // Unlock the ability
        abilityComp.unlockAbility(abilityId, "TestClass");

        String displayName = abilityDef.getDisplayName();
        Message typeName = getAbilityTypeMessage(abilityDef);
        
        player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.success")
            .param("ability", displayName)
            .param("type", typeName));
        
        // If it's a passive or toggle, mention it will tick
        switch (abilityDef.getAbilityType()) {
            case Passive -> player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.typePassive"));
            case Toggle -> player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.typeToggle"));
            case Active -> player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.typeActive"));
        }
    }

    private void listUnlockedAbilities(@Nonnull Player player, @Nonnull ClassAbilityComponent abilityComp) {
        if (abilityComp.getUnlockedAbilityIds().isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.none"));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.header"));

        for (String abilityId : abilityComp.getUnlockedAbilityIds()) {
            ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
            String displayName = abilityDef != null ? abilityDef.getDisplayName() : abilityId;
            Message typeName = getAbilityTypeMessage(abilityDef);

            player.sendMessage(Message.translation("pixelbays.rpg.ability.unlock.entry")
                    .param("ability", displayName)
                    .param("type", typeName));
        }
    }

    @Nonnull
    private Message getAbilityTypeMessage(ClassAbilityDefinition abilityDef) {
        if (abilityDef == null || abilityDef.getAbilityType() == null) {
            return Message.translation("pixelbays.rpg.ability.type.unknown");
        }

        return switch (abilityDef.getAbilityType()) {
            case Passive -> Message.translation("pixelbays.rpg.ability.type.passive");
            case Toggle -> Message.translation("pixelbays.rpg.ability.type.toggle");
            case Active -> Message.translation("pixelbays.rpg.ability.type.active");
        };
    }
}
