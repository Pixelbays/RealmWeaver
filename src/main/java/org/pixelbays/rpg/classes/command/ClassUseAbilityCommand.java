package org.pixelbays.rpg.classes.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.ability.component.ClassAbilityComponent;
import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;
import org.pixelbays.rpg.ability.system.ClassAbilitySystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /class useability <abilityId> - Trigger an ability interaction for testing
 */
public class ClassUseAbilityCommand extends AbstractPlayerCommand {

    private final ClassAbilitySystem abilitySystem;
    private final RequiredArg<String> abilityIdArg;

    public ClassUseAbilityCommand() {
        super("useability", "Trigger an ability interaction for testing");
        requirePermission(HytalePermissions.fromCommand("admin"));
        this.abilitySystem = Realmweavers.get().getClassAbilitySystem();
        this.abilityIdArg = this.withRequiredArg("abilityId", "Ability id to trigger", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String abilityId = this.abilityIdArg.get(ctx);

        ClassAbilityDefinition abilityDef = abilitySystem.getAbilityDefinition(abilityId);
        if (abilityDef == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.ability.error.notFound").param("abilityId", abilityId));
            return;
        }

        ClassAbilityComponent abilityComp = store.getComponent(ref, Realmweavers.get().getClassAbilityComponentType());
        if (abilityComp == null || !abilityComp.hasAbility(abilityId)) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.ability.error.notUnlocked").param("abilityId", abilityId));
            return;
        }

        String chainId = abilityDef.getInteractionChainId();
        if (chainId == null || chainId.isEmpty()) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.ability.error.noChain").param("abilityId", abilityId));
            return;
        }

        RootInteraction root = RootInteraction.getAssetMap().getAsset(chainId);
        if (root == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.ability.error.rootNotFound").param("chainId", chainId));
            return;
        }

        InteractionManager manager = store.getComponent(ref, InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.ability.error.interactionManagerMissing"));
            return;
        }

        ClassAbilityDefinition.AbilityInputBinding binding = abilityDef.getInputBinding();
        InteractionType type = switch (binding == null ? ClassAbilityDefinition.AbilityInputBinding.Ability1
                : binding) {
            case Ability2 -> InteractionType.Ability2;
            case Ability3 -> InteractionType.Ability3;
            case Ability1 -> InteractionType.Ability1;
        };

        InteractionContext context = InteractionContext.forInteraction(manager, ref, type, store);
        InteractionChain chain = manager.initChain(type, context, root, false);
        manager.queueExecuteChain(chain);

        player.sendMessage(Message.translation("pixelbays.rpg.class.ability.success.triggered")
            .param("ability", abilityDef.getDisplayName())
            .param("chainId", chainId));
    }
}
