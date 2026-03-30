package org.pixelbays.rpg.npc.corecomponents.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.AbilityUnlock;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.ClassLevel;
import org.pixelbays.rpg.npc.component.NpcRpgSetupComponent.LevelSystemLevel;
import org.pixelbays.rpg.npc.corecomponents.ActionRpgSetup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNoEmptyStringsValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNullOrNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionRpgSetup extends BuilderActionBase {

    protected final StringHolder raceId = new StringHolder();
    protected final StringArrayHolder parentRaces = new StringArrayHolder();
    protected final StringArrayHolder raceAbilities = new StringArrayHolder();
    protected final StringArrayHolder classes = new StringArrayHolder();
    protected final StringHolder primaryClass = new StringHolder();

    protected final List<ClassLevel> classLevels = new ArrayList<>();
    protected final List<LevelSystemLevel> levelSystems = new ArrayList<>();
    protected final List<AbilityUnlock> abilities = new ArrayList<>();

    @Nonnull
    @Override
    public String getShortDescription() {
        return "Configure RPG class, race, and ability data for an NPC";
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
        return new ActionRpgSetup(this, builderSupport);
    }

    @Nonnull
    public BuilderActionRpgSetup readConfig(@Nonnull JsonElement data) {
        this.getString(data, "RaceId", this.raceId, "", StringNullOrNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "Race id to assign", null);
        readStringArray(data, "ParentRaces", this.parentRaces, StringArrayNoEmptyStringsValidator.get());
        readStringArray(data, "RaceAbilities", this.raceAbilities, StringArrayNoEmptyStringsValidator.get());
        readStringArray(data, "Classes", this.classes, StringArrayNoEmptyStringsValidator.get());
        this.getString(data, "PrimaryClass", this.primaryClass, "", StringNullOrNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "Primary class to prioritize", null);

        readClassLevels(data);
        readLevelSystems(data);
        readAbilities(data);
        return this;
    }

    public String getRaceId(@Nonnull BuilderSupport support) {
        return this.raceId.get(support.getExecutionContext());
    }

    @Nonnull
    public List<String> getParentRaces(@Nonnull BuilderSupport support) {
        return toList(this.parentRaces.get(support.getExecutionContext()));
    }

    @Nonnull
    public List<String> getRaceAbilities(@Nonnull BuilderSupport support) {
        return toList(this.raceAbilities.get(support.getExecutionContext()));
    }

    @Nonnull
    public List<String> getClasses(@Nonnull BuilderSupport support) {
        return toList(this.classes.get(support.getExecutionContext()));
    }

    public String getPrimaryClass(@Nonnull BuilderSupport support) {
        return this.primaryClass.get(support.getExecutionContext());
    }

    @Nonnull
    public List<ClassLevel> getClassLevels() {
        return new ArrayList<>(this.classLevels);
    }

    @Nonnull
    public List<LevelSystemLevel> getLevelSystems() {
        return new ArrayList<>(this.levelSystems);
    }

    @Nonnull
    public List<AbilityUnlock> getAbilities() {
        return new ArrayList<>(this.abilities);
    }

    @Nonnull
    private List<String> toList(@Nullable String[] values) {
        if (values == null || values.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(values));
    }

    private void readClassLevels(@Nonnull JsonElement data) {
        this.ignoreAttribute("ClassLevels");
        JsonElement element = this.getOptionalJsonElement(data, "ClassLevels");
        if (element == null) {
            return;
        }
        if (!element.isJsonArray()) {
            this.addError(new IllegalStateException("ClassLevels must be an array"));
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                this.addError(new IllegalStateException("ClassLevels entries must be objects"));
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String classId = readStringValue(obj, "ClassId");
            int level = readInt(obj, "Level", 1);
            if (classId != null && !classId.isEmpty()) {
                this.classLevels.add(new ClassLevel(classId, level));
            }
        }
    }

    private void readLevelSystems(@Nonnull JsonElement data) {
        this.ignoreAttribute("LevelSystems");
        JsonElement element = this.getOptionalJsonElement(data, "LevelSystems");
        if (element == null) {
            return;
        }
        if (!element.isJsonArray()) {
            this.addError(new IllegalStateException("LevelSystems must be an array"));
            return;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                this.addError(new IllegalStateException("LevelSystems entries must be objects"));
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String systemId = readStringValue(obj, "SystemId");
            int level = readInt(obj, "Level", 1);
            if (systemId != null && !systemId.isEmpty()) {
                this.levelSystems.add(new LevelSystemLevel(systemId, level));
            }
        }
    }

    private void readAbilities(@Nonnull JsonElement data) {
        this.ignoreAttribute("Abilities");
        JsonElement element = this.getOptionalJsonElement(data, "Abilities");
        if (element == null) {
            return;
        }
        if (!element.isJsonArray()) {
            this.addError(new IllegalStateException("Abilities must be an array"));
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                this.addError(new IllegalStateException("Abilities entries must be objects"));
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String abilityId = readStringValue(obj, "AbilityId");
            String classId = readStringValue(obj, "ClassId");
            int rank = readInt(obj, "Rank", 1);
            if (abilityId != null && !abilityId.isEmpty()) {
                this.abilities.add(new AbilityUnlock(abilityId, classId, rank));
            }
        }
    }

    @Nullable
    private String readStringValue(@Nonnull JsonObject obj, @Nonnull String key) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            this.addError(new IllegalStateException(key + " must be a string"));
            return "";
        }
        return element.getAsString();
    }

    private void readStringArray(@Nonnull JsonElement data,
            @Nonnull String key,
            @Nonnull StringArrayHolder holder,
            @Nonnull StringArrayNoEmptyStringsValidator validator) {
        this.ignoreAttribute(key);
        JsonElement element = this.getOptionalJsonElement(data, key);
        if (element == null) {
            holder.readJSON(null, 0, Integer.MAX_VALUE, new String[0], validator, key, this.builderParameters);
            return;
        }

        holder.readJSON(element, 0, Integer.MAX_VALUE, validator, key, this.builderParameters);
    }

    private int readInt(@Nonnull JsonObject obj, @Nonnull String key, int defaultValue) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            this.addError(new IllegalStateException(key + " must be a number"));
            return defaultValue;
        }
        return element.getAsInt();
    }
}
