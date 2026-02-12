package org.pixelbays.rpg.guild;

import java.util.UUID;

public class GuildMember {

    private final UUID memberId;
    private String roleId;
    private final long joinedAtMillis;

    public GuildMember(UUID memberId, String roleId, long joinedAtMillis) {
        this.memberId = memberId;
        this.roleId = roleId;
        this.joinedAtMillis = joinedAtMillis;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public long getJoinedAtMillis() {
        return joinedAtMillis;
    }
}
