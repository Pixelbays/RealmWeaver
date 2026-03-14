package org.pixelbays.rpg.party.config.settings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class PartyModSettings {

    public enum PartyXpGrantingMode {
        SplitEqualInRange,
        FullInRange
    }

    public static final BuilderCodec<PartyModSettings> CODEC = BuilderCodec
            .builder(PartyModSettings.class, PartyModSettings::new)
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN, false, true),
                (i, s) -> i.enabled = s, i -> i.enabled)
            .add()
            .append(new KeyedCodec<>("PartyEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyEnabled = s, i -> i.partyEnabled)
            .add()
            .append(new KeyedCodec<>("PartyMaxSize", Codec.INTEGER, false, true),
                    (i, s) -> i.partyMaxSize = s, i -> i.partyMaxSize)
            .add()
            .append(new KeyedCodec<>("RaidEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.raidEnabled = s, i -> i.raidEnabled)
            .add()
            .append(new KeyedCodec<>("RaidMaxSize", Codec.INTEGER, false, true),
                    (i, s) -> i.raidMaxSize = s, i -> i.raidMaxSize)
            .add()
            .append(new KeyedCodec<>("PartyXpEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyXpEnabled = s, i -> i.partyXpEnabled)
            .add()
            .append(new KeyedCodec<>("PartyXpGrantingMode", new EnumCodec<>(PartyXpGrantingMode.class), false, true),
                    (i, s) -> i.partyXpGrantingMode = s, i -> i.partyXpGrantingMode)
            .add()
            .append(new KeyedCodec<>("PartyXpRangeBlocks", Codec.INTEGER, false, true),
                    (i, s) -> i.partyXpRangeBlocks = s, i -> i.partyXpRangeBlocks)
            .add()
            .append(new KeyedCodec<>("PartyXpMinMembersInRange", Codec.INTEGER, false, true),
                    (i, s) -> i.partyXpMinMembersInRange = s, i -> i.partyXpMinMembersInRange)
            .add()
            .append(new KeyedCodec<>("PartyNpcAllowed", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyNpcAllowed = s, i -> i.partyNpcAllowed)
            .add()
            .append(new KeyedCodec<>("PartyPersistenceEnabled", Codec.BOOLEAN, false, true),
                    (i, s) -> i.partyPersistenceEnabled = s, i -> i.partyPersistenceEnabled)
            .add()
            .append(new KeyedCodec<>("PartyPersistenceIntervalSeconds", Codec.INTEGER, false, true),
                    (i, s) -> i.partyPersistenceIntervalSeconds = s, i -> i.partyPersistenceIntervalSeconds)
            .add()
            .build();

    private boolean enabled;
    private boolean partyEnabled;
    private int partyMaxSize;
    private boolean raidEnabled;
    private int raidMaxSize;
    private boolean partyXpEnabled;
    private PartyXpGrantingMode partyXpGrantingMode;
    private int partyXpRangeBlocks;
    private int partyXpMinMembersInRange;
    private boolean partyNpcAllowed;
    private boolean partyPersistenceEnabled;
    private int partyPersistenceIntervalSeconds;

    public PartyModSettings() {
        this.enabled = true;
        this.partyEnabled = true;
        this.partyMaxSize = 5;
        this.raidEnabled = true;
        this.raidMaxSize = 20;
        this.partyXpEnabled = true;
        this.partyXpGrantingMode = PartyXpGrantingMode.SplitEqualInRange;
        this.partyXpRangeBlocks = 48;
        this.partyXpMinMembersInRange = 1;
        this.partyNpcAllowed = true;
        this.partyPersistenceEnabled = false;
        this.partyPersistenceIntervalSeconds = 60;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPartyEnabled() {
        return partyEnabled;
    }

    public int getPartyMaxSize() {
        return partyMaxSize;
    }

    public boolean isRaidEnabled() {
        return raidEnabled;
    }

    public int getRaidMaxSize() {
        return raidMaxSize;
    }

    public boolean isPartyXpEnabled() {
        return partyXpEnabled;
    }

    public PartyXpGrantingMode getPartyXpGrantingMode() {
        return partyXpGrantingMode;
    }

    public int getPartyXpRangeBlocks() {
        return partyXpRangeBlocks;
    }

    public int getPartyXpMinMembersInRange() {
        return partyXpMinMembersInRange;
    }

    public boolean isPartyNpcAllowed() {
        return partyNpcAllowed;
    }

    public boolean isPartyPersistenceEnabled() {
        return partyPersistenceEnabled;
    }

    public int getPartyPersistenceIntervalSeconds() {
        return partyPersistenceIntervalSeconds;
    }
}
