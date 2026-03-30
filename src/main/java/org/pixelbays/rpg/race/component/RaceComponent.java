package org.pixelbays.rpg.race.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component that stores race selection data for an entity.
 * Supports hybrid race tracking via parent race ids.
 */
@SuppressWarnings({ "PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone",
    "deprecation", "all", "null" })
public class RaceComponent implements Component<EntityStore> {

    private static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.STRING, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    public static final BuilderCodec<RaceComponent> CODEC = BuilderCodec
            .builder(RaceComponent.class, RaceComponent::new)
            .append(new KeyedCodec<>("RaceId", Codec.STRING),
                    (component, value) -> component.raceId = value,
                    component -> component.raceId)
            .add()
            .append(new KeyedCodec<>("ParentRaces", STRING_LIST_CODEC, false, true),
                    (component, value) -> component.parentRaces = value,
                    component -> component.parentRaces)
            .add()
            .append(new KeyedCodec<>("UnlockedRaceAbilities", STRING_LIST_CODEC, false, true),
                    (component, value) -> component.unlockedRaceAbilities = value,
                    component -> component.unlockedRaceAbilities)
            .add()
            .build();

    private String raceId;
    private List<String> parentRaces;
    private List<String> unlockedRaceAbilities;

    public RaceComponent() {
        this.raceId = "";
        this.parentRaces = new ArrayList<>();
        this.unlockedRaceAbilities = new ArrayList<>();
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(@Nullable String raceId) {
        this.raceId = raceId == null ? "" : raceId;
    }

    public List<String> getParentRaces() {
        return parentRaces;
    }

    public void setParentRaces(@Nullable List<String> parentRaces) {
        this.parentRaces = parentRaces == null ? new ArrayList<>() : parentRaces;
    }

    public List<String> getUnlockedRaceAbilities() {
        return unlockedRaceAbilities;
    }

    public void setUnlockedRaceAbilities(@Nullable List<String> unlockedRaceAbilities) {
        this.unlockedRaceAbilities = unlockedRaceAbilities == null ? new ArrayList<>() : unlockedRaceAbilities;
    }

    public boolean hasUnlockedRaceAbility(@Nonnull String abilityId) {
        return unlockedRaceAbilities.contains(abilityId);
    }

    public void unlockRaceAbility(@Nonnull String abilityId) {
        if (!unlockedRaceAbilities.contains(abilityId)) {
            unlockedRaceAbilities.add(abilityId);
        }
    }

    public static ComponentType<EntityStore, RaceComponent> getComponentType() {
        return Realmweavers.get().getRaceComponentType();
    }

    @Nullable
    @Override
    @SuppressWarnings({ "PMD", "CloneDoesntCallSuperClone", "all", "clone", "CloneDoesntDeclareCloneNotSupportedException" })
    public Component<EntityStore> clone() {
        RaceComponent cloned = new RaceComponent();
        cloned.raceId = this.raceId;
        cloned.parentRaces = new ArrayList<>(this.parentRaces);
        cloned.unlockedRaceAbilities = new ArrayList<>(this.unlockedRaceAbilities);
        return cloned;
    }

}
