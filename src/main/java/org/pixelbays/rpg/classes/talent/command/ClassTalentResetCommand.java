package org.pixelbays.rpg.classes.talent.command;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.classes.talent.TalentSystem;

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
 * /class talent reset &lt;classId&gt;
 * Resets all talent allocations for a class, refunding points per TalentSettings.ResetMode.
 * In Paid mode, this will consume the configured reset currency cost.
 */
public class ClassTalentResetCommand extends AbstractPlayerCommand {

    private static final String CONFIG_ID = "default";

    private final TalentSystem talentSystem;
    private final RequiredArg<String> classIdArg;

    public ClassTalentResetCommand() {
        super("reset", "Reset all talent allocations for a class");
        this.talentSystem = ExamplePlugin.get().getTalentSystem();
        this.classIdArg = this.withRequiredArg("classId", "The class to reset talents for", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        String classId = this.classIdArg.get(ctx);

        // checkItemCost=true: enforce Paid mode currency consumption
        String result = talentSystem.resetTalents(ref, classId, CONFIG_ID, true, store);
        if (result == null) {
            player.sendMessage(Message.translation("pixelbays.rpg.class.talent.reset.success")
                    .param("classId", classId));
            return;
        }

        player.sendMessage(TalentCommandUtil.translateResult(result));
    }
}
