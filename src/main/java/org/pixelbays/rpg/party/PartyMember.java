package org.pixelbays.rpg.party;

import java.util.UUID;

public class PartyMember {

    private final UUID entityId;
    private final PartyMemberType memberType;
    private PartyRole role;
    private final long joinedAtMillis;

    public PartyMember(UUID entityId, PartyMemberType memberType, PartyRole role, long joinedAtMillis) {
        this.entityId = entityId;
        this.memberType = memberType;
        this.role = role;
        this.joinedAtMillis = joinedAtMillis;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public PartyMemberType getMemberType() {
        return memberType;
    }

    public PartyRole getRole() {
        return role;
    }

    public void setRole(PartyRole role) {
        this.role = role;
    }

    public long getJoinedAtMillis() {
        return joinedAtMillis;
    }
}
