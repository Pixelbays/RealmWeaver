package org.pixelbays.rpg.party.ui;

import java.util.List;
import java.util.UUID;

import org.pixelbays.rpg.party.PartyMemberType;
import org.pixelbays.rpg.party.PartyRole;
import org.pixelbays.rpg.party.PartyType;

public class PartyUiSnapshot {

    private final UUID partyId;
    private final PartyType partyType;
    private final UUID viewerId;
    private final PartyRole viewerRole;
    private final int memberCount;
    private final int maxSize;
    private final List<MemberView> members;

    public PartyUiSnapshot(UUID partyId,
            PartyType partyType,
            UUID viewerId,
            PartyRole viewerRole,
            int memberCount,
            int maxSize,
            List<MemberView> members) {
        this.partyId = partyId;
        this.partyType = partyType;
        this.viewerId = viewerId;
        this.viewerRole = viewerRole;
        this.memberCount = memberCount;
        this.maxSize = maxSize;
        this.members = List.copyOf(members);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public PartyRole getViewerRole() {
        return viewerRole;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<MemberView> getMembers() {
        return members;
    }

    public boolean isViewerLeader() {
        return viewerRole == PartyRole.LEADER;
    }

    public boolean isViewerAssistant() {
        return viewerRole == PartyRole.ASSISTANT;
    }

    public static class MemberView {
        private final UUID memberId;
        private final String displayName;
        private final PartyMemberType memberType;
        private final PartyRole role;
        private final boolean online;
        private final long joinedAtMillis;

        public MemberView(UUID memberId,
                String displayName,
                PartyMemberType memberType,
                PartyRole role,
                boolean online,
                long joinedAtMillis) {
            this.memberId = memberId;
            this.displayName = displayName;
            this.memberType = memberType;
            this.role = role;
            this.online = online;
            this.joinedAtMillis = joinedAtMillis;
        }

        public UUID getMemberId() {
            return memberId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public PartyMemberType getMemberType() {
            return memberType;
        }

        public PartyRole getRole() {
            return role;
        }

        public boolean isOnline() {
            return online;
        }

        public long getJoinedAtMillis() {
            return joinedAtMillis;
        }
    }
}
