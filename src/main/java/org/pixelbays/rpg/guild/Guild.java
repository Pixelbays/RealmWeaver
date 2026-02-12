package org.pixelbays.rpg.guild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Guild {

    private final UUID id;
    private final String name;
    private final String tag;
    private UUID leaderId;
    private GuildJoinPolicy joinPolicy;
    private final Map<UUID, GuildMember> members;
    private final Map<String, GuildRole> roles;

    public Guild(UUID id, String name, String tag, UUID leaderId, GuildJoinPolicy joinPolicy) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leaderId = leaderId;
        this.joinPolicy = joinPolicy;
        this.members = new HashMap<>();
        this.roles = new HashMap<>();
        ensureDefaultRoles();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public GuildJoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public void setJoinPolicy(GuildJoinPolicy joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

    public Map<UUID, GuildMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public List<GuildMember> getMemberList() {
        return new ArrayList<>(members.values());
    }

    public Map<String, GuildRole> getRoles() {
        return Collections.unmodifiableMap(roles);
    }

    public GuildRole getRole(String roleId) {
        if (roleId == null) {
            return null;
        }
        return roles.get(roleId.toLowerCase());
    }

    public void putRole(GuildRole role) {
        if (role == null || role.getId() == null) {
            return;
        }
        roles.put(role.getId().toLowerCase(), role);
    }

    public void addMember(GuildMember member) {
        members.put(member.getMemberId(), member);
    }

    public void removeMember(UUID memberId) {
        members.remove(memberId);
    }

    public GuildMember getMember(UUID memberId) {
        return members.get(memberId);
    }

    public boolean hasMember(UUID memberId) {
        return members.containsKey(memberId);
    }

    public int size() {
        return members.size();
    }

    public boolean isLeader(UUID memberId) {
        return leaderId != null && leaderId.equals(memberId);
    }

    public boolean hasPermission(UUID memberId, GuildPermission permission) {
        if (isLeader(memberId)) {
            return true;
        }
        GuildMember member = members.get(memberId);
        if (member == null) {
            return false;
        }
        GuildRole role = getRole(member.getRoleId());
        return role != null && role.hasPermission(permission);
    }

    public void ensureDefaultRoles() {
        putRole(GuildRole.leaderRole());
        putRole(GuildRole.officerRole());
        putRole(GuildRole.memberRole());
    }
}
