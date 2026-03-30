package org.pixelbays.rpg.npc.corecomponents.builders;

import javax.annotation.Nonnull;

import org.pixelbays.rpg.npc.corecomponents.ActionRpgCastAbility;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNullOrNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionRpgCastAbility extends BuilderActionBase {

    protected final StringHolder abilityId = new StringHolder();
    protected final StringHolder interactionType = new StringHolder();
    protected final BooleanHolder requireUnlocked = new BooleanHolder();
    protected final BooleanHolder logFailures = new BooleanHolder();

    public BuilderActionRpgCastAbility() {
    }

    @Nonnull
    @Override
    public String getShortDescription() {
        return "Trigger an RPG ability for this NPC";
    }

    @Nonnull
    @Override
    public String getLongDescription() {
        return this.getShortDescription();
    }

    @Nonnull
    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.WorkInProgress;
    }

    @Nonnull
    public Action build(@Nonnull BuilderSupport builderSupport) {
        return new ActionRpgCastAbility(this, builderSupport);
    }

    @Nonnull
    public BuilderActionRpgCastAbility readConfig(@Nonnull JsonElement data) {
        this.requireString(data, "AbilityId", this.abilityId, StringNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "Ability id to trigger", null);
        this.getString(data, "InteractionType", this.interactionType, "", StringNullOrNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "Override interaction type (Primary, Ability1, Ability2, Ability3)",
                null);
        this.getBoolean(data, "RequireUnlocked", this.requireUnlocked, true, BuilderDescriptorState.Stable,
                "Require the ability to be unlocked", null);
        this.getBoolean(data, "LogFailures", this.logFailures, true, BuilderDescriptorState.Stable,
                "Log warnings when ability fails", null);
        return this;
    }

    public String getAbilityId(@Nonnull BuilderSupport support) {
        return this.abilityId.get(support.getExecutionContext());
    }

    public String getInteractionType(@Nonnull BuilderSupport support) {
        return this.interactionType.get(support.getExecutionContext());
    }

    public boolean isRequireUnlocked(@Nonnull BuilderSupport support) {
        return this.requireUnlocked.get(support.getExecutionContext());
    }

    public boolean isLogFailures(@Nonnull BuilderSupport support) {
        return this.logFailures.get(support.getExecutionContext());
    }
}
