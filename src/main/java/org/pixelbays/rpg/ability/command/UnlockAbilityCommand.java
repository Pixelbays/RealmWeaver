package org.pixelbays.rpg.ability.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Test command to manually unlock abilities for debugging.
 * Usage: /unlockability <abilityId>
 * Usage: /unlockability list
 */
public class UnlockAbilityCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> abilityIdArg;

    public UnlockAbilityCommand() {
        super("unlockability", "Unlock an ability for testing");
        this.abilityIdArg = this.withOptionalArg("abilityId", "Ability ID to unlock or 'list'", ArgTypes.STRING);
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
                ExamplePlugin.get().getClassAbilityComponentType());
        if (abilityComp == null) {
            abilityComp = store.addComponent(ref, ExamplePlugin.get().getClassAbilityComponentType());
        }

        // If no argument, show list
        if (!abilityIdArg.provided(ctx)) {
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
            player.sendMessage(Message.translation("server.rpg.ability.unlock.unknownAbility").param("abilityId", abilityId));
            return;
        }

        // Check if already unlocked
        if (abilityComp.hasAbility(abilityId)) {
            player.sendMessage(Message.translation("server.rpg.ability.unlock.alreadyUnlocked")
                    .param("ability", abilityDef.getDisplayName()));
            return;
        }

        // Unlock the ability
        abilityComp.unlockAbility(abilityId, "TestClass");

        String displayName = abilityDef.getDisplayName();
        String typeName = abilityDef.getAbilityType() != null ? abilityDef.getAbilityType().toString() : "Unknown";
        
        player.sendMessage(Message.translation("server.rpg.ability.unlock.success")
            .param("ability", displayName)
            .param("type", typeName));
        
        // If it's a passive or toggle, mention it will tick
        if (abilityDef.getAbilityType() == ClassAbilityDefinition.AbilityType.Passive) {
            player.sendMessage(Message.translation("server.rpg.ability.unlock.typePassive"));
        } else if (abilityDef.getAbilityType() == ClassAbilityDefinition.AbilityType.Toggle) {
            player.sendMessage(Message.translation("server.rpg.ability.unlock.typeToggle"));
        } else {
            player.sendMessage(Message.translation("server.rpg.ability.unlock.typeActive"));
        }
    }

    private void listUnlockedAbilities(@Nonnull Player player, @Nonnull ClassAbilityComponent abilityComp) {
        if (abilityComp.getUnlockedAbilityIds().isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.ability.unlock.none"));
            return;
        }

        player.sendMessage(Message.translation("server.rpg.ability.unlock.header"));

        for (String abilityId : abilityComp.getUnlockedAbilityIds()) {
            ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
            String displayName = abilityDef != null ? abilityDef.getDisplayName() : abilityId;
            String typeName = abilityDef != null && abilityDef.getAbilityType() != null 
                    ? abilityDef.getAbilityType().toString() : "Unknown";

            player.sendMessage(Message.translation("server.rpg.ability.unlock.entry")
                    .param("ability", displayName)
                    .param("type", typeName));
        }
    }
}
