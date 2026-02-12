package org.pixelbays.rpg.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Party {

    private final UUID id;
    private final PartyType type;
    private final Map<UUID, PartyMember> members;
    private final Set<UUID> assistants;
    private UUID leaderId;
    private final PartySettings settings;

    public Party(UUID id, PartyType type, UUID leaderId, PartySettings settings) {
        this.id = id;
        this.type = type;
        this.leaderId = leaderId;
        this.settings = settings;
        this.members = new HashMap<>();
        this.assistants = new HashSet<>();
    }

    public UUID getId() {
        return id;
    }

    public PartyType getType() {
        return type;
    }

    public PartySettings getSettings() {
        return settings;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public Map<UUID, PartyMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public List<PartyMember> getMemberList() {
        return new ArrayList<>(members.values());
    }

    public Set<UUID> getAssistants() {
        return Collections.unmodifiableSet(assistants);
    }

    public boolean hasMember(UUID memberId) {
        return members.containsKey(memberId);
    }

    public PartyMember getMember(UUID memberId) {
        return members.get(memberId);
    }

    public int size() {
        return members.size();
    }

    public void addAssistant(UUID memberId) {
        assistants.add(memberId);
    }

    public void removeAssistant(UUID memberId) {
        assistants.remove(memberId);
    }

    public boolean isAssistant(UUID memberId) {
        return assistants.contains(memberId);
    }

    public void addMember(PartyMember member) {
        members.put(member.getEntityId(), member);
    }

    public void removeMember(UUID memberId) {
        members.remove(memberId);
        assistants.remove(memberId);
    }

    public boolean isLeader(UUID memberId) {
        return leaderId != null && leaderId.equals(memberId);
    }
}
