package org.pixelbays.rpg.ability.command;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.ability.binding.AbilityBindingService;
import org.pixelbays.rpg.ability.component.AbilityBindingComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.global.config.RpgModConfig;

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
 * Command to bind abilities to hotbar slots.
 * Usage: /bindability <slot> <abilityId>
 * Usage: /bindability <slot> clear
 * Usage: /bindability list
 */
@SuppressWarnings("null")
public class BindAbilityCommand extends AbstractPlayerCommand {

    private final AbilityBindingService bindingService;
     private final RequiredArg<String> slotArg;
     private final RequiredArg<String> abilityIdArg;

    public BindAbilityCommand() {
        super("bindability", "Bind an ability to a hotbar slot");
        requirePermission(HytalePermissions.fromCommand("player"));
       this.bindingService = new AbilityBindingService();
        this.slotArg = this.withRequiredArg("slot", "Hotbar slot (1-9) or 'list'", ArgTypes.STRING);
        this.abilityIdArg = null;
        this.addUsageVariant(new BindAbilityCommand("Bind an ability to a hotbar slot"));
    }

    private BindAbilityCommand(String description) {
        super(description);
        this.bindingService = new AbilityBindingService();
        this.slotArg = this.withRequiredArg("slot", "Hotbar slot (1-9)", ArgTypes.STRING);
        this.abilityIdArg = this.withRequiredArg("abilityId", "Ability ID or 'clear'", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        String slotToken = slotArg.get(ctx);
        String abilityId = abilityIdArg != null ? abilityIdArg.get(ctx) : null;

        if (slotToken == null || slotToken.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.usage"));
            return;
        }

        if (abilityIdArg == null && "list".equalsIgnoreCase(slotToken)) {
            listBindings(player, store, ref);
            return;
        }

        if (abilityIdArg == null || abilityId == null || abilityId.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.usage"));
            return;
        }

        int slot;
        try {
            slot = Integer.parseInt(slotToken);
        } catch (NumberFormatException e) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.invalidSlot"));
            return;
        }

        // Convert from 1-indexed (user input) to 0-indexed (internal hotbar)
        int internalSlot = slot - 1;

        // Validate slot range (1-9 for user, 0-8 internally)
        if (internalSlot < 0 || internalSlot > 8) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.invalidSlot"));
            return;
        }

        // Validate slot is an ability slot
        RpgModConfig config = RpgModConfig.getAssetMap().getAsset("default");
        if (config == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.configNotFound"));
            return;
        }

        int[] abilitySlots = config.getHotbarAbilitySlots();
        boolean isValidSlot = false;
        for (int abilitySlot : abilitySlots) {
            if (internalSlot == (abilitySlot)) {
                isValidSlot = true;
                break;
            }
        }

        if (!isValidSlot) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.slotNotConfigured")
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
            
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.cleared").param("slot", slot));
            return;
        }

        // Validate ability exists
        ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
        if (abilityDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.unknownAbility").param("abilityId", abilityId));
            return;
        }

        // Check if player has learned this ability
        String abilityName = AbilityBindingService.resolveDisplayText(abilityDef.getDisplayName(), abilityId);
        if (!bindingService.isAbilityUnlocked(ref, store, abilityId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.notLearned")
                .param("ability", abilityName));
            return;
        }

        if (abilityDef.getAbilityType() == ClassAbilityDefinition.AbilityType.Passive) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.passive")
                    .param("ability", abilityName));
            return;
        }

        // Bind the ability
        bindingComp.setHotbarBinding(internalSlot, abilityId);
        
        // Update hotbar icon
        ExamplePlugin.get().getHotbarIconManager().updateHotbarSlot(ref, store, internalSlot, abilityId);
        
        player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.bound")
            .param("ability", abilityName)
            .param("slot", slot));
    }

    private void listBindings(@Nonnull Player player, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref) {
        AbilityBindingComponent bindingComp = store.getComponent(ref,
                ExamplePlugin.get().getAbilityBindingComponentType());
        if (bindingComp == null || bindingComp.getHotbarBindings().isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.none"));
            return;
        }

        player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.header"));

        bindingComp.getHotbarBindings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getKey(), b.getKey()))
                .forEach(entry -> {
                    int internalSlot = entry.getKey();
                    String abilityId = entry.getValue();

                    ClassAbilityDefinition abilityDef = ClassAbilityDefinition.getAssetMap().getAsset(abilityId);
                        String abilityName = abilityDef != null
                            ? AbilityBindingService.resolveDisplayText(abilityDef.getDisplayName(), abilityId)
                            : abilityId;

                    // Display as 1-indexed for user (internal is 0-indexed)
                    int displaySlot = internalSlot + 1;
                    player.sendMessage(Message.translation("pixelbays.rpg.ability.bind.entry")
                            .param("slot", displaySlot)
                            .param("ability", abilityName));
                });
    }
}
