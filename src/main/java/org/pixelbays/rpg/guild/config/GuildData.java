package org.pixelbays.rpg.guild.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.pixelbays.rpg.guild.Guild;
import org.pixelbays.rpg.guild.GuildJoinPolicy;
import org.pixelbays.rpg.guild.GuildMember;
import org.pixelbays.rpg.guild.GuildPermission;
import org.pixelbays.rpg.guild.GuildRole;

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
public class GuildData implements JsonAssetWithMap<String, DefaultAssetMap<String, GuildData>> {

    private static final FunctionCodec<GuildMemberData[], List<GuildMemberData>> MEMBER_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(GuildMemberData.CODEC, GuildMemberData[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(GuildMemberData[]::new));

    private static final FunctionCodec<GuildRoleData[], List<GuildRoleData>> ROLE_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(GuildRoleData.CODEC, GuildRoleData[]::new),
                    arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                    list -> list == null ? null : list.toArray(GuildRoleData[]::new));

        private static final FunctionCodec<GuildApplicationData[], List<GuildApplicationData>> APPLICATION_LIST_CODEC =
            new FunctionCodec<>(new ArrayCodec<>(GuildApplicationData.CODEC, GuildApplicationData[]::new),
                arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                list -> list == null ? null : list.toArray(GuildApplicationData[]::new));

    public static final AssetBuilderCodec<String, GuildData> CODEC = AssetBuilderCodec.builder(
            GuildData.class,
            GuildData::new,
            Codec.STRING,
            (t, k) -> t.id = k,
            t -> t.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
            .append(new KeyedCodec<>("Name", Codec.STRING, false, true),
                    (i, s) -> i.name = s, i -> i.name)
            .add()
            .append(new KeyedCodec<>("Tag", Codec.STRING, false, true),
                    (i, s) -> i.tag = s, i -> i.tag)
            .add()
                .append(new KeyedCodec<>("Description", Codec.STRING, false, true),
                    (i, s) -> i.description = s, i -> i.description)
                .add()
                .append(new KeyedCodec<>("Motd", Codec.STRING, false, true),
                    (i, s) -> i.motd = s, i -> i.motd)
                .add()
            .append(new KeyedCodec<>("LeaderId", Codec.UUID_STRING, false, true),
                    (i, s) -> i.leaderId = s, i -> i.leaderId)
            .add()
            .append(new KeyedCodec<>("JoinPolicy", new EnumCodec<>(GuildJoinPolicy.class), false, true),
                    (i, s) -> i.joinPolicy = s, i -> i.joinPolicy)
            .add()
            .append(new KeyedCodec<>("Roles", ROLE_LIST_CODEC, false, true),
                    (i, s) -> i.roles = s, i -> i.roles)
            .add()
            .append(new KeyedCodec<>("Members", MEMBER_LIST_CODEC, false, true),
                    (i, s) -> i.members = s, i -> i.members)
            .add()
                .append(new KeyedCodec<>("Applications", APPLICATION_LIST_CODEC, false, true),
                    (i, s) -> i.applications = s, i -> i.applications)
                .add()
            .build();

    private static DefaultAssetMap<String, GuildData> ASSET_MAP;
    private AssetExtraInfo.Data data;

    private String id;
    private String name;
    private String tag;
    private String description;
    private String motd;
    private UUID leaderId;
    private GuildJoinPolicy joinPolicy;
    private List<GuildRoleData> roles;
    private List<GuildMemberData> members;
    private List<GuildApplicationData> applications;

    public GuildData() {
        this.id = "";
        this.name = "";
        this.tag = "";
        this.description = "";
        this.motd = "";
        this.leaderId = new UUID(0L, 0L);
        this.joinPolicy = GuildJoinPolicy.INVITE_ONLY;
        this.roles = new ArrayList<>();
        this.members = new ArrayList<>();
        this.applications = new ArrayList<>();
    }

    public static DefaultAssetMap<String, GuildData> getAssetMap() {
        if (ASSET_MAP == null) {
            var assetStore = AssetRegistry.getAssetStore(GuildData.class);
            if (assetStore != null) {
                ASSET_MAP = (DefaultAssetMap<String, GuildData>) assetStore.getAssetMap();
            }
        }

        return ASSET_MAP;
    }

    @Override
    public String getId() {
        return id;
    }

    public Guild toGuild() {
        Guild guild = new Guild(UUID.fromString(id), name, tag, leaderId, joinPolicy, description, motd);

        if (roles != null) {
            for (GuildRoleData roleData : roles) {
                if (roleData == null) {
                    continue;
                }
                guild.putRole(roleData.toRole());
            }
        }

        if (members != null) {
            for (GuildMemberData memberData : members) {
                if (memberData == null) {
                    continue;
                }
                GuildMember member = memberData.toMember();
                guild.addMember(member);
            }
        }

        if (!guild.hasMember(leaderId)) {
            GuildMember leader = new GuildMember(leaderId, GuildRole.LEADER_ID, System.currentTimeMillis());
            guild.addMember(leader);
        }

        return guild;
    }

    public List<GuildApplicationData> getApplications() {
        if (applications == null || applications.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(applications);
    }

    public static GuildData fromGuild(Guild guild) {
        return fromGuild(guild, null);
    }

    public static GuildData fromGuild(Guild guild, List<GuildApplicationData> applications) {
        GuildData data = new GuildData();
        data.id = guild.getId().toString();
        data.name = guild.getName();
        data.tag = guild.getTag();
        data.description = guild.getDescription();
        data.motd = guild.getMotd();
        data.leaderId = guild.getLeaderId();
        data.joinPolicy = guild.getJoinPolicy();
        data.roles = new ArrayList<>();
        data.members = new ArrayList<>();
        data.applications = applications == null ? new ArrayList<>() : new ArrayList<>(applications);

        for (GuildRole role : guild.getRoles().values()) {
            data.roles.add(GuildRoleData.fromRole(role));
        }

        for (GuildMember member : guild.getMemberList()) {
            data.members.add(GuildMemberData.fromMember(member));
        }

        return data;
    }

    public static class GuildApplicationData {
        public static final BuilderCodec<GuildApplicationData> CODEC = BuilderCodec
                .builder(GuildApplicationData.class, GuildApplicationData::new)
                .append(new KeyedCodec<>("ApplicantId", Codec.UUID_STRING, false, true),
                        (i, s) -> i.applicantId = s, i -> i.applicantId)
                .add()
                .append(new KeyedCodec<>("ApplicationMessage", Codec.STRING, false, true),
                        (i, s) -> i.applicationMessage = s, i -> i.applicationMessage)
                .add()
                .append(new KeyedCodec<>("CreatedAtMillis", Codec.LONG, false, true),
                        (i, s) -> i.createdAtMillis = s, i -> i.createdAtMillis)
                .add()
                .append(new KeyedCodec<>("ExpiresAtMillis", Codec.LONG, false, true),
                        (i, s) -> i.expiresAtMillis = s, i -> i.expiresAtMillis)
                .add()
                .build();

        private UUID applicantId;
        private String applicationMessage;
        private long createdAtMillis;
        private long expiresAtMillis;

        public GuildApplicationData() {
            this.applicantId = new UUID(0L, 0L);
            this.applicationMessage = "";
            this.createdAtMillis = 0L;
            this.expiresAtMillis = 0L;
        }

        public GuildApplicationData(UUID applicantId, String applicationMessage, long createdAtMillis, long expiresAtMillis) {
            this.applicantId = applicantId;
            this.applicationMessage = applicationMessage == null ? "" : applicationMessage;
            this.createdAtMillis = createdAtMillis;
            this.expiresAtMillis = expiresAtMillis;
        }

        public UUID getApplicantId() {
            return applicantId;
        }

        public String getApplicationMessage() {
            return applicationMessage == null ? "" : applicationMessage;
        }

        public long getCreatedAtMillis() {
            return createdAtMillis;
        }

        public long getExpiresAtMillis() {
            return expiresAtMillis;
        }
    }

    public static class GuildRoleData {
        private static final FunctionCodec<GuildPermission[], List<GuildPermission>> PERMISSION_LIST_CODEC =
                new FunctionCodec<>(new ArrayCodec<>(new EnumCodec<>(GuildPermission.class), GuildPermission[]::new),
                        arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
                        list -> list == null ? null : list.toArray(GuildPermission[]::new));

        public static final BuilderCodec<GuildRoleData> CODEC = BuilderCodec
                .builder(GuildRoleData.class, GuildRoleData::new)
                .append(new KeyedCodec<>("Id", Codec.STRING, false, true),
                        (i, s) -> i.id = s, i -> i.id)
                .add()
                .append(new KeyedCodec<>("Name", Codec.STRING, false, true),
                        (i, s) -> i.name = s, i -> i.name)
                .add()
                .append(new KeyedCodec<>("Permissions", PERMISSION_LIST_CODEC, false, true),
                        (i, s) -> i.permissions = s, i -> i.permissions)
                .add()
                .build();

        private String id;
        private String name;
        private List<GuildPermission> permissions;

        public GuildRoleData() {
            this.id = "";
            this.name = "";
            this.permissions = new ArrayList<>();
        }

        public GuildRole toRole() {
            EnumSet<GuildPermission> permissionSet = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(GuildPermission.class)
                : EnumSet.copyOf(permissions);
            return new GuildRole(id, name, permissionSet);
        }

        public static GuildRoleData fromRole(GuildRole role) {
            GuildRoleData data = new GuildRoleData();
            data.id = role.getId();
            data.name = role.getName();
            data.permissions = new ArrayList<>(role.getPermissions());
            return data;
        }
    }

    public static class GuildMemberData {
        public static final BuilderCodec<GuildMemberData> CODEC = BuilderCodec
                .builder(GuildMemberData.class, GuildMemberData::new)
                .append(new KeyedCodec<>("MemberId", Codec.UUID_STRING, false, true),
                        (i, s) -> i.memberId = s, i -> i.memberId)
                .add()
                .append(new KeyedCodec<>("RoleId", Codec.STRING, false, true),
                        (i, s) -> i.roleId = s, i -> i.roleId)
                .add()
                .build();

        private UUID memberId;
        private String roleId;

        public GuildMemberData() {
            this.memberId = new UUID(0L, 0L);
            this.roleId = GuildRole.MEMBER_ID;
        }

        public GuildMember toMember() {
            return new GuildMember(memberId, roleId, System.currentTimeMillis());
        }

        public static GuildMemberData fromMember(GuildMember member) {
            GuildMemberData data = new GuildMemberData();
            data.memberId = member.getMemberId();
            data.roleId = member.getRoleId();
            return data;
        }
    }
}
