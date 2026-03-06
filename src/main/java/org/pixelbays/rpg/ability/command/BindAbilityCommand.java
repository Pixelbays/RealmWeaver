package org.pixelbays.rpg.ability.command;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;
import org.pixelbays.rpg.global.config.RpgModConfig;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Command to bind abilities to hotbar slots.
 * Usage: /bindability <slot> <abilityId>
 * Usage: /bindability <slot> clear
 * Usage: /bindability list
 */
@SuppressWarnings("null")
public class BindAbilityCommand extends AbstractPlayerCommand {

     private final RequiredArg<String> slotArg;
     private final OptionalArg<String> abilityIdArg;

    public BindAbilityCommand() {
        super("bindability", "Bind an ability to a hotbar slot");
        this.slotArg = this.withRequiredArg("slot", "Hotbar slot (1-9) or 'list'", ArgTypes.STRING);
        this.abilityIdArg = this.withOptionalArg("abilityId", "Ability ID or 'clear'", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        String slotToken = slotArg.get(ctx);
        String abilityId = abilityIdArg.provided(ctx) ? abilityIdArg.get(ctx) : null;

        if (slotToken == null || slotToken.isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.usage"));
            return;
        }

        if ("list".equalsIgnoreCase(slotToken)) {
            listBindings(player, store, ref);
            return;
        }

        if (abilityId == null || abilityId.isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.usage"));
            return;
        }

        int slot;
        try {
            slot = Integer.parseInt(slotToken);
        } catch (NumberFormatException e) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.invalidSlot"));
            return;
        }

        // Convert from 1-indexed (user input) to 0-indexed (internal hotbar)
        int internalSlot = slot - 1;

        // Validate slot range (1-9 for user, 0-8 internally)
        if (internalSlot < 0 || internalSlot > 8) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.invalidSlot"));
            return;
        }

        // Check for "list" keyword (case where someone does "/bindability 7 list" or similar)
        if ("list".equalsIgnoreCase(abilityId)) {
            listBindings(player, store, ref);
            return;
        }

        // Validate slot is an ability slot
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config == null) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.configNotFound"));
            return;
        }

        int[] abilitySlots = config.getHotbarAbilitySlots();
        boolean isValidSlot = false;
        for (int abilitySlot : abilitySlots) {
            if (internalSlot == (abilitySlot - 1)) {
                isValidSlot = true;
                break;
            }
        }

        if (!isValidSlot) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.slotNotConfigured")
                .param("slot", slot)
                .param("validSlots", Arrays.toString(abilitySlots)));
            return;
        }

        // Get or create binding component
        AbilityBindingComponent bindingComp = store.getComponent(ref,
                ExamplePlugin.get().getAbilityBindingComponentType());
        if (bindingComp == null) {
            bindingComp = store.addComponent(ref, ExamplePlugin.get().getAbilityBindingComponentType());
        }

        // Handle "clear"
        if ("clear".equalsIgnoreCase(abilityId) || "unbind".equalsIgnoreCase(abilityId)
                || "remove".equalsIgnoreCase(abilityId)) {
            bindingComp.setHotbarBinding(internalSlot, null);
            
            // Update hotbar icon
            ExamplePlugin.get().getHotbarIconManager().updateHotbarSlot(ref, store, internalSlot, null);
            
            player.sendMessage(Message.translation("server.rpg.ability.bind.cleared").param("slot", slot));
            return;
        }

        // Validate ability exists
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.unknownAbility").param("abilityId", abilityId));
            return;
        }

        // Check if player has learned this ability
        ClassAbilitySystem abilitySystem = ExamplePlugin.get().getClassAbilitySystem();
        if (!abilitySystem.isAbilityUnlocked(ref, store, abilityId)) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.notLearned")
                    .param("ability", abilityDef.getDisplayName()));
            return;
        }

        // Bind the ability
        bindingComp.setHotbarBinding(internalSlot, abilityId);
        
        // Update hotbar icon
        ExamplePlugin.get().getHotbarIconManager().updateHotbarSlot(ref, store, internalSlot, abilityId);
        
        player.sendMessage(Message.translation("server.rpg.ability.bind.bound")
            .param("ability", abilityDef.getDisplayName())
            .param("slot", slot));
    }

    private void listBindings(@Nonnull Player player, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref) {
        AbilityBindingComponent bindingComp = store.getComponent(ref,
                ExamplePlugin.get().getAbilityBindingComponentType());
        if (bindingComp == null || bindingComp.getHotbarBindings().isEmpty()) {
            player.sendMessage(Message.translation("server.rpg.ability.bind.none"));
            return;
        }

        player.sendMessage(Message.translation("server.rpg.ability.bind.header"));

        bindingComp.getHotbarBindings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getKey(), b.getKey()))
                .forEach(entry -> {
                    int internalSlot = entry.getKey();
                    String abilityId = entry.getValue();

                    ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
                    String abilityName = abilityDef != null ? abilityDef.getDisplayName() : abilityId;

                    // Display as 1-indexed for user (internal is 0-indexed)
                    int displaySlot = internalSlot + 1;
                    player.sendMessage(Message.translation("server.rpg.ability.bind.entry")
                            .param("slot", displaySlot)
                            .param("ability", abilityName));
                });
    }
}
