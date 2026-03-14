package org.pixelbays.rpg.party;

import org.pixelbays.rpg.party.config.settings.PartyModSettings.PartyXpGrantingMode;

public class PartySettings {

    private final boolean xpEnabled;
    private final PartyXpGrantingMode xpGrantingMode;
    private final int xpRangeBlocks;
    private final int xpMinMembersInRange;
    private final boolean npcAllowed;
    private final int maxSize;

    public PartySettings(boolean xpEnabled,
            PartyXpGrantingMode xpGrantingMode,
            int xpRangeBlocks,
            int xpMinMembersInRange,
            boolean npcAllowed,
            int maxSize) {
        this.xpEnabled = xpEnabled;
        this.xpGrantingMode = xpGrantingMode;
        this.xpRangeBlocks = xpRangeBlocks;
        this.xpMinMembersInRange = xpMinMembersInRange;
        this.npcAllowed = npcAllowed;
        this.maxSize = maxSize;
    }

    public boolean isXpEnabled() {
        return xpEnabled;
    }

    public PartyXpGrantingMode getXpGrantingMode() {
        return xpGrantingMode;
    }

    public int getXpRangeBlocks() {
        return xpRangeBlocks;
    }

    public int getXpMinMembersInRange() {
        return xpMinMembersInRange;
    }

    public boolean isNpcAllowed() {
        return npcAllowed;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
