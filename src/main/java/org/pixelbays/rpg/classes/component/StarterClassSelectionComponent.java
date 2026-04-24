package org.pixelbays.rpg.classes.component;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({"PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone"})
public class StarterClassSelectionComponent implements Component<EntityStore>, Cloneable {

    public static final BuilderCodec<StarterClassSelectionComponent> CODEC = BuilderCodec
            .builder(StarterClassSelectionComponent.class, StarterClassSelectionComponent::new)
            .append(new KeyedCodec<>("SelectionCompleted", Codec.BOOLEAN),
                    (component, value) -> component.selectionCompleted = value,
                    component -> component.selectionCompleted)
            .add()
            .append(new KeyedCodec<>("SelectedClassId", Codec.STRING),
                    (component, value) -> component.selectedClassId = value,
                    component -> component.selectedClassId)
            .add()
            .build();

    private boolean selectionCompleted;
    private String selectedClassId;

    public StarterClassSelectionComponent() {
        this.selectionCompleted = false;
        this.selectedClassId = "";
    }

    public boolean isSelectionCompleted() {
        return selectionCompleted;
    }

    public void setSelectionCompleted(boolean selectionCompleted) {
        this.selectionCompleted = selectionCompleted;
    }

    @Nonnull
    public String getSelectedClassId() {
        return selectedClassId == null ? "" : selectedClassId;
    }

    public void setSelectedClassId(@Nonnull String selectedClassId) {
        this.selectedClassId = selectedClassId;
    }

    public static ComponentType<EntityStore, StarterClassSelectionComponent> getComponentType() {
        return Realmweavers.get().getStarterClassSelectionComponentType();
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        StarterClassSelectionComponent cloned = new StarterClassSelectionComponent();
        cloned.selectionCompleted = this.selectionCompleted;
        cloned.selectedClassId = this.selectedClassId;
        return cloned;
    }
}