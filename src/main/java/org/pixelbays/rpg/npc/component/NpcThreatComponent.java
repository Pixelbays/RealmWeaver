package org.pixelbays.rpg.npc.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings({ "PMD", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone", "all", "clone", "null" })
public class NpcThreatComponent implements Component<EntityStore>, Cloneable {

    public static final BuilderCodec<NpcThreatComponent> CODEC = BuilderCodec
            .builder(NpcThreatComponent.class, NpcThreatComponent::new)
            .build();

    private final Map<Ref<EntityStore>, ThreatTracker> threatBySource;
    @Nullable
    private Ref<EntityStore> currentTarget;

    public NpcThreatComponent() {
        this.threatBySource = new LinkedHashMap<>();
        this.currentTarget = null;
    }

    public static ComponentType<EntityStore, NpcThreatComponent> getComponentType() {
        return Realmweavers.get().getNpcThreatComponentType();
    }

    public void addThreat(@Nullable Ref<EntityStore> sourceRef, float threatAmount, long timestampMillis) {
        if (sourceRef == null || !sourceRef.isValid() || threatAmount <= 0.0f) {
            return;
        }

        threatBySource.computeIfAbsent(sourceRef, key -> new ThreatTracker())
                .addThreat(threatAmount, timestampMillis);
    }

    public boolean isEmpty() {
        return threatBySource.isEmpty();
    }

    public void clear() {
        threatBySource.clear();
        currentTarget = null;
    }

    @Nullable
    public Ref<EntityStore> getCurrentTarget() {
        return currentTarget != null && currentTarget.isValid() ? currentTarget : null;
    }

    public void setCurrentTarget(@Nullable Ref<EntityStore> currentTarget) {
        this.currentTarget = currentTarget != null && currentTarget.isValid() ? currentTarget : null;
    }

    @Nullable
    public Ref<EntityStore> pruneAndResolveCurrentTarget(long nowMillis, float lookbackSeconds) {
        long cutoffTime = nowMillis - Math.max(1L, Math.round(Math.max(0.1f, lookbackSeconds) * 1000.0f));
        Ref<EntityStore> resolvedTarget = null;
        float highestThreat = 0.0f;
        Ref<EntityStore> previousTarget = getCurrentTarget();

        Iterator<Map.Entry<Ref<EntityStore>, ThreatTracker>> iterator = threatBySource.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Ref<EntityStore>, ThreatTracker> entry = iterator.next();
            Ref<EntityStore> sourceRef = entry.getKey();
            if (sourceRef == null || !sourceRef.isValid()) {
                iterator.remove();
                continue;
            }

            float totalThreat = entry.getValue().pruneAndGetTotal(cutoffTime);
            if (totalThreat <= 0.0f) {
                iterator.remove();
                continue;
            }

            if (resolvedTarget == null || totalThreat > highestThreat + 0.001f) {
                resolvedTarget = sourceRef;
                highestThreat = totalThreat;
                continue;
            }

            if (Math.abs(totalThreat - highestThreat) <= 0.001f && previousTarget != null
                    && previousTarget.equals(sourceRef)) {
                resolvedTarget = sourceRef;
            }
        }

        setCurrentTarget(resolvedTarget);
        return getCurrentTarget();
    }

    public float getThreatFor(@Nullable Ref<EntityStore> sourceRef, long nowMillis, float lookbackSeconds) {
        if (sourceRef == null) {
            return 0.0f;
        }

        ThreatTracker tracker = threatBySource.get(sourceRef);
        if (tracker == null) {
            return 0.0f;
        }

        long cutoffTime = nowMillis - Math.max(1L, Math.round(Math.max(0.1f, lookbackSeconds) * 1000.0f));
        return tracker.pruneAndGetTotal(cutoffTime);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        NpcThreatComponent cloned = new NpcThreatComponent();
        for (Map.Entry<Ref<EntityStore>, ThreatTracker> entry : this.threatBySource.entrySet()) {
            cloned.threatBySource.put(entry.getKey(), entry.getValue().copy());
        }
        cloned.currentTarget = this.currentTarget;
        return cloned;
    }

    private static final class ThreatTracker {
        private final List<ThreatSample> samples = new ArrayList<>();

        void addThreat(float threatAmount, long timestampMillis) {
            samples.add(new ThreatSample(threatAmount, timestampMillis));
        }

        float pruneAndGetTotal(long cutoffTimeMillis) {
            float totalThreat = 0.0f;
            Iterator<ThreatSample> iterator = samples.iterator();
            while (iterator.hasNext()) {
                ThreatSample sample = iterator.next();
                if (sample.timestampMillis < cutoffTimeMillis) {
                    iterator.remove();
                    continue;
                }
                totalThreat += sample.threatAmount;
            }
            return totalThreat;
        }

        ThreatTracker copy() {
            ThreatTracker copy = new ThreatTracker();
            for (ThreatSample sample : this.samples) {
                copy.samples.add(new ThreatSample(sample.threatAmount, sample.timestampMillis));
            }
            return copy;
        }
    }

    private static final class ThreatSample {
        private final float threatAmount;
        private final long timestampMillis;

        ThreatSample(float threatAmount, long timestampMillis) {
            this.threatAmount = threatAmount;
            this.timestampMillis = timestampMillis;
        }
    }
}