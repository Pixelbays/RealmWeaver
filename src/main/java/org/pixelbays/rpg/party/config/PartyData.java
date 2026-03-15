package org.pixelbays.rpg.party.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.party.PartyMemberType;
import org.pixelbays.rpg.party.PartyRole;
import org.pixelbays.rpg.party.PartySettings;
import org.pixelbays.rpg.party.PartyType;
import org.pixelbays.rpg.party.config.settings.PartyModSettings.PartyXpGrantingMode;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings({ "deprecation", "ToArrayCallWithZeroLengthArrayArgument", "RedundantArrayCreation" })
public class PartyData implements JsonAssetWithMap<String, DefaultAssetMap<String, PartyData>> {

    private static final FunctionCodec<UUID[], List<UUID>> UUID_LIST_CODEC = new FunctionCodec<>(
            new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? null : list.toArray(UUID[]::new));

    private static final FunctionCodec<PartyMemberData[], List<PartyMemberData>> MEMBER_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(PartyMemberData.CODEC, PartyMemberData[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(PartyMemberData[]::new));

    public static final AssetBuilderCodec<String, PartyData> CODEC = AssetBuilderCodec.builder(
            PartyData.class,
            PartyData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("PartyType", new EnumCodec<>(PartyType.class), false, true),
                    (i, s) -> i.partyType = s, i -> i.partyType)
            .add()
            .append(new KeyedCodec<>("LeaderId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.leaderId = s, i -> i.leaderId)
            .add()
            .append(new KeyedCodec<>("Assistants", UUID_LIST_CODEC, false, true),
                    (i, s) -> i.assistants = s, i -> i.assistants)
            .add()
            .append(new KeyedCodec<>("Members", MEMBER_LIST_CODEC, false, true),
                    (i, s) -> i.members = s, i -> i.members)
            .add()
            .append(new KeyedCodec<>("Settings", PartySettingsData.CODEC, false, true),
                    (i, s) -> i.settings = s, i -> i.settings)
            .add()
            .build();

    private static DefaultAssetMap<String, PartyData> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private PartyType partyType;
    private UUID leaderId;
    private List<UUID> assistants;
    private List<PartyMemberData> members;
    private PartySettingsData settings;

    public PartyData() {
        this.id = "";
        this.partyType = PartyType.PARTY;
        this.leaderId = new UUID(0L, 0L);
        this.assistants = new ArrayList<>();
        this.members = new ArrayList<>();
        this.settings = new PartySettingsData();
    }

    public static DefaultAssetMap<String, PartyData> getAssetMap() {
        if (ASSET_MAP == null) {
            var assetStore = AssetRegistry.getAssetStore(PartyData.class);
            if (assetStore != null) {
                ASSET_MAP = (DefaultAssetMap<String, PartyData>) assetStore.getAssetMap();
            }
        }

        return ASSET_MAP;
    }

    @Override
    public String getId() {
        return id;
    }

    public Party toParty() {
        PartySettings partySettings = settings.toSettings();
        Party party = new Party(UUID.fromString(id), partyType, leaderId, partySettings);
        if (members != null) {
            for (PartyMemberData memberData : members) {
                if (memberData == null) {
                    continue;
                }
                PartyMember member = memberData.toMember();
                party.addMember(member);
            }
        }

        if (!party.hasMember(leaderId)) {
            PartyMember leader = new PartyMember(leaderId, PartyMemberType.PLAYER, PartyRole.LEADER,
                    System.currentTimeMillis());
            party.addMember(leader);
        } else {
            PartyMember leader = party.getMember(leaderId);
            leader.setRole(PartyRole.LEADER);
            party.setLeaderId(leaderId);
            party.removeAssistant(leaderId);
        }

        if (assistants != null) {
            for (UUID assistantId : assistants) {
                if (assistantId == null || assistantId.equals(leaderId) || !party.hasMember(assistantId)) {
                    continue;
                }

                PartyMember assistant = party.getMember(assistantId);
                assistant.setRole(PartyRole.ASSISTANT);
                party.addAssistant(assistantId);
            }
        }

        return party;
    }

    public static PartyData fromParty(Party party) {
        PartyData data = new PartyData();
        data.id = party.getId().toString();
        data.partyType = party.getType();
        data.leaderId = party.getLeaderId();
        data.assistants = new ArrayList<>(party.getAssistants());
        data.members = new ArrayList<>();
        data.settings = PartySettingsData.fromSettings(party.getSettings());

        for (PartyMember member : party.getMemberList()) {
            data.members.add(PartyMemberData.fromMember(member));
        }

        return data;
    }

    public static class PartySettingsData {
        public static final BuilderCodec<PartySettingsData> CODEC = BuilderCodec
                .builder(PartySettingsData.class, PartySettingsData::new)
                .append(new KeyedCodec<>("XpEnabled", Codec.BOOLEAN, false, true),
                        (i, s) -> i.xpEnabled = s, i -> i.xpEnabled)
                .add()
                .append(new KeyedCodec<>("XpGrantingMode", new EnumCodec<>(PartyXpGrantingMode.class), false, true),
                        (i, s) -> i.xpGrantingMode = s, i -> i.xpGrantingMode)
                .add()
                .append(new KeyedCodec<>("XpRangeBlocks", Codec.INTEGER, false, true),
                        (i, s) -> i.xpRangeBlocks = s, i -> i.xpRangeBlocks)
                .add()
                .append(new KeyedCodec<>("XpMinMembersInRange", Codec.INTEGER, false, true),
                        (i, s) -> i.xpMinMembersInRange = s, i -> i.xpMinMembersInRange)
                .add()
                .append(new KeyedCodec<>("NpcAllowed", Codec.BOOLEAN, false, true),
                        (i, s) -> i.npcAllowed = s, i -> i.npcAllowed)
                .add()
                .append(new KeyedCodec<>("MaxSize", Codec.INTEGER, false, true),
                        (i, s) -> i.maxSize = s, i -> i.maxSize)
                .add()
                .append(new KeyedCodec<>("MaxAssistants", Codec.INTEGER, false, true),
                    (i, s) -> i.maxAssistants = s, i -> i.maxAssistants)
                .add()
                .build();

        private boolean xpEnabled;
        private PartyXpGrantingMode xpGrantingMode;
        private int xpRangeBlocks;
        private int xpMinMembersInRange;
        private boolean npcAllowed;
        private int maxSize;
        private int maxAssistants;

        public PartySettingsData() {
            this.xpEnabled = true;
            this.xpGrantingMode = PartyXpGrantingMode.SplitEqualInRange;
            this.xpRangeBlocks = 48;
            this.xpMinMembersInRange = 1;
            this.npcAllowed = true;
            this.maxSize = 5;
            this.maxAssistants = 1;
        }

        public PartySettings toSettings() {
            return new PartySettings(
                    xpEnabled,
                    xpGrantingMode,
                    xpRangeBlocks,
                    xpMinMembersInRange,
                    npcAllowed,
                    maxSize,
                    maxAssistants);
        }

        public static PartySettingsData fromSettings(PartySettings settings) {
            PartySettingsData data = new PartySettingsData();
            data.xpEnabled = settings.isXpEnabled();
            data.xpGrantingMode = settings.getXpGrantingMode();
            data.xpRangeBlocks = settings.getXpRangeBlocks();
            data.xpMinMembersInRange = settings.getXpMinMembersInRange();
            data.npcAllowed = settings.isNpcAllowed();
            data.maxSize = settings.getMaxSize();
            data.maxAssistants = settings.getMaxAssistants();
            return data;
        }
    }

    public static class PartyMemberData {
        public static final BuilderCodec<PartyMemberData> CODEC = BuilderCodec
                .builder(PartyMemberData.class, PartyMemberData::new)
                .append(new KeyedCodec<>("MemberId", Codec.UUID_STRING, false, true),
                        (i, s) -> i.memberId = s, i -> i.memberId)
                .add()
                .append(new KeyedCodec<>("MemberType", new EnumCodec<>(PartyMemberType.class), false, true),
                        (i, s) -> i.memberType = s, i -> i.memberType)
                .add()
                .append(new KeyedCodec<>("Role", new EnumCodec<>(PartyRole.class), false, true),
                        (i, s) -> i.role = s, i -> i.role)
                .add()
                .append(new KeyedCodec<>("JoinedAtMillis", Codec.LONG, false, true),
                    (i, s) -> i.joinedAtMillis = s, i -> i.joinedAtMillis)
                .add()
                .build();

        private UUID memberId;
        private PartyMemberType memberType;
        private PartyRole role;
            private long joinedAtMillis;

        public PartyMemberData() {
            this.memberId = new UUID(0L, 0L);
            this.memberType = PartyMemberType.PLAYER;
            this.role = PartyRole.MEMBER;
            this.joinedAtMillis = 0L;
        }

        public PartyMember toMember() {
            return new PartyMember(memberId, memberType, role,
                    joinedAtMillis > 0 ? joinedAtMillis : System.currentTimeMillis());
        }

        public static PartyMemberData fromMember(PartyMember member) {
            PartyMemberData data = new PartyMemberData();
            data.memberId = member.getEntityId();
            data.memberType = member.getMemberType();
            data.role = member.getRole();
            data.joinedAtMillis = member.getJoinedAtMillis();
            return data;
        }
    }
}
