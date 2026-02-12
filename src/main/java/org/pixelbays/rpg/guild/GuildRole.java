package org.pixelbays.rpg.guild;

import java.util.EnumSet;
import java.util.Set;

public class GuildRole {

    public static final String LEADER_ID = "leader";
    public static final String OFFICER_ID = "officer";
    public static final String MEMBER_ID = "member";

    private final String id;
    private String name;
    private final EnumSet<GuildPermission> permissions;

    public GuildRole(String id, String name, Set<GuildPermission> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions == null
                ? EnumSet.noneOf(GuildPermission.class)
                : EnumSet.copyOf(permissions);
    }

    public static GuildRole leaderRole() {
        return new GuildRole(LEADER_ID, "Leader", EnumSet.allOf(GuildPermission.class));
    }

    public static GuildRole officerRole() {
        return new GuildRole(OFFICER_ID, "Officer",
                EnumSet.of(GuildPermission.INVITE, GuildPermission.KICK, GuildPermission.PROMOTE,
                        GuildPermission.ACCEPT_APPLICATIONS));
    }

    public static GuildRole memberRole() {
        return new GuildRole(MEMBER_ID, "Member", EnumSet.noneOf(GuildPermission.class));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasPermission(GuildPermission permission) {
        return permissions.contains(permission);
    }

    public void setPermission(GuildPermission permission, boolean enabled) {
        if (enabled) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }

    public Set<GuildPermission> getPermissions() {
        return EnumSet.copyOf(permissions);
    }
}
