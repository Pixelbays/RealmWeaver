package org.pixelbays.rpg.lockpicking.input;

import javax.annotation.Nonnull;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.lockpicking.system.LockpickingSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEventType;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Intercepts CustomPageEvent.Data packets for the lockpicking page and routes
 * them directly to LockpickingSystem, bypassing the
 * PageManager.customPageRequiredAcknowledgments gate.
 *
 * Without this filter, every UI update packet sent by the server increments
 * an acknowledgment counter, and all button-click (Data) events are silently
 * dropped until the client sends an Acknowledge back. This makes the Set Pin
 * button unreliable since the acknowledgment round-trip overlaps with clicks.
 *
 * By intercepting and consuming the Data event here (returning true = block),
 * we handle it immediately regardless of the counter state.
 */
public class LockpickingInputFilter implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!Realmweavers.get().isLockpickingModuleEnabled()) {
            return false;
        }

        if (!(packet instanceof CustomPageEvent event)) {
            return false;
        }
        if (event.type != CustomPageEventType.Data || event.data == null) {
            return false;
        }

        // Must be a lockpicking action — string-only check, no Store access allowed here
        // because the filter runs on the Netty IO thread, not the WorldThread.
        // Store.getComponent() asserts WorldThread and will throw if called here.
        boolean isSetPin = event.data.contains("\"SetPin\"");
        boolean isCancel = event.data.contains("\"Cancel\"");
        if (!isSetPin && !isCancel) {
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        // Dispatch to the world thread. Session validation (isActive check) happens
        // inside handlePinAttempt / cancelSession — both are safe to call from there.
        Store<EntityStore> store = ref.getStore();
        final boolean doSetPin = isSetPin;
        store.getExternalData().getWorld().execute(() -> {
            if (doSetPin) {
                LockpickingSystem.get().handlePinAttempt(ref, store);
            } else {
                LockpickingSystem.get().cancelSession(ref, store);
            }
        });

        // Block the packet so PageManager never sees it. PageManager would drop it
        // anyway while customPageRequiredAcknowledgments != 0.
        return true;
    }
}
