package org.pixelbays.rpg.guild;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.global.util.RpgLogging;
import org.pixelbays.rpg.guild.config.GuildData;
import org.pixelbays.rpg.guild.event.GuildApplicationAcceptedEvent;
import org.pixelbays.rpg.guild.event.GuildApplicationDeniedEvent;
import org.pixelbays.rpg.guild.event.GuildApplicationSubmittedEvent;
import org.pixelbays.rpg.guild.event.GuildCreatedEvent;
import org.pixelbays.rpg.guild.event.GuildDisbandedEvent;
import org.pixelbays.rpg.guild.event.GuildInviteSentEvent;
import org.pixelbays.rpg.guild.event.GuildJoinMethod;
import org.pixelbays.rpg.guild.event.GuildJoinPolicyChangedEvent;
import org.pixelbays.rpg.guild.event.GuildJoinedEvent;
import org.pixelbays.rpg.guild.event.GuildLeaderChangedEvent;
import org.pixelbays.rpg.guild.event.GuildLeftEvent;
import org.pixelbays.rpg.guild.event.GuildMemberKickedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleAssignedEvent;
import org.pixelbays.rpg.guild.event.GuildRoleCreatedEvent;
import org.pixelbays.rpg.guild.event.GuildRolePermissionChangedEvent;

public class GuildManager {

    private final Map<UUID, Guild> guildsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberToGuild = new ConcurrentHashMap<>();
    private final Map<UUID, GuildInvite> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, GuildApplication> pendingApplications = new ConcurrentHashMap<>();
    private final GuildPersistence persistence = new GuildPersistence();

    public GuildActionResult createGuild(UUID leaderId, String rawName, String rawTag) {
        RpgModConfig config = resolveConfig();
        if (config != null && !config.isGuildEnabled()) {
            return GuildActionResult.failure("Guilds are disabled.");
        }

        if (memberToGuild.containsKey(leaderId)) {
            return GuildActionResult.failure("You are already in a guild.");
        }

        String name = rawName == null ? "" : rawName.trim();
        String tag = rawTag == null ? "" : rawTag.trim();

        if (!isNameValid(name, config)) {
            return GuildActionResult.failure("Invalid guild name.");
        }

        if (!isTagValid(tag, config)) {
            return GuildActionResult.failure("Invalid guild tag.");
        }

        if (!isNameAvailable(name)) {
            return GuildActionResult.failure("That guild name is already taken.");
        }

        if (!isTagAvailable(tag)) {
            return GuildActionResult.failure("That guild tag is already taken.");
        }

        GuildJoinPolicy joinPolicy = config == null ? GuildJoinPolicy.INVITE_ONLY : config.getGuildDefaultJoinPolicy();
        Guild guild = new Guild(UUID.randomUUID(), name, tag, leaderId, joinPolicy, "", "");
        GuildMember leader = new GuildMember(leaderId, GuildRole.LEADER_ID, System.currentTimeMillis());
        guild.addMember(leader);

        guildsById.put(guild.getId(), guild);
        memberToGuild.put(leaderId, guild.getId());
        saveGuildIfEnabled(guild, config);
        GuildCreatedEvent.dispatch(guild.getId(), leaderId, name, tag, joinPolicy);

        return GuildActionResult.success("Created guild " + name + ".", guild);
    }

    public GuildActionResult invitePlayer(UUID inviterId, UUID targetId) {
        Guild guild = getGuildForMember(inviterId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild. Use /guild create first.");
        }

        if (!guild.hasPermission(inviterId, GuildPermission.INVITE)) {
            return GuildActionResult.failure("You do not have permission to invite players.");
        }

        if (memberToGuild.containsKey(targetId)) {
            return GuildActionResult.failure("That player is already in a guild.");
        }

        if (guild.size() >= getMaxMembers(configOrDefault())) {
            return GuildActionResult.failure("The guild is full.");
        }

        long expiresAt = System.currentTimeMillis() + getInviteExpiryMillis(configOrDefault());
        pendingInvites.put(targetId, new GuildInvite(guild.getId(), inviterId, expiresAt));
        GuildInviteSentEvent.dispatch(guild.getId(), inviterId, targetId, expiresAt);
        return GuildActionResult.success("Invite sent.", guild);
    }

    public GuildActionResult acceptInvite(UUID inviteeId) {
        if (memberToGuild.containsKey(inviteeId)) {
            return GuildActionResult.failure("You are already in a guild.");
        }

        GuildInvite invite = getValidInvite(inviteeId);
        if (invite == null) {
            return GuildActionResult.failure("You do not have a pending guild invite.");
        }

        Guild guild = guildsById.get(invite.guildId);
        if (guild == null) {
            pendingInvites.remove(inviteeId);
            return GuildActionResult.failure("That guild was not found.");
        }

        if (guild.size() >= getMaxMembers(configOrDefault())) {
            return GuildActionResult.failure("That guild is full.");
        }

        pendingInvites.remove(inviteeId);
        GuildActionResult result = addMemberToGuild(guild, inviteeId, GuildRole.MEMBER_ID, "Joined the guild.");
        GuildJoinedEvent.dispatch(guild.getId(), inviteeId, GuildRole.MEMBER_ID, GuildJoinMethod.INVITE);
        return result;
    }

    public GuildActionResult declineInvite(UUID inviteeId) {
        if (memberToGuild.containsKey(inviteeId)) {
            return GuildActionResult.failure("You are already in a guild.");
        }

        if (getValidInvite(inviteeId) == null) {
            return GuildActionResult.failure("You do not have a pending guild invite.");
        }

        pendingInvites.remove(inviteeId);
        return GuildActionResult.success("Guild invite declined.");
    }

    public GuildActionResult cancelInvite(UUID actorId, UUID targetId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.INVITE)) {
            return GuildActionResult.failure("You do not have permission to invite players.");
        }

        GuildInvite invite = getValidInvite(targetId);
        if (invite == null) {
            return GuildActionResult.failure("No outgoing invite found for that player.");
        }

        if (!invite.guildId.equals(guild.getId())) {
            return GuildActionResult.failure("That invite does not belong to your guild.");
        }

        pendingInvites.remove(targetId);
        return GuildActionResult.success("Invite canceled.", guild);
    }

    public GuildActionResult joinGuild(UUID joinerId, String nameOrTag) {
        if (memberToGuild.containsKey(joinerId)) {
            return GuildActionResult.failure("You are already in a guild.");
        }

        Guild guild = findGuildByNameOrTag(nameOrTag);
        if (guild == null) {
            return GuildActionResult.failure("That guild was not found.");
        }

        if (guild.size() >= getMaxMembers(configOrDefault())) {
            return GuildActionResult.failure("That guild is full.");
        }

        if (guild.getJoinPolicy() == GuildJoinPolicy.OPEN) {
            GuildActionResult result = addMemberToGuild(guild, joinerId, GuildRole.MEMBER_ID, "Joined the guild.");
            GuildJoinedEvent.dispatch(guild.getId(), joinerId, GuildRole.MEMBER_ID, GuildJoinMethod.OPEN);
            return result;
        }

        if (guild.getJoinPolicy() == GuildJoinPolicy.APPLICATION) {
            return GuildActionResult.failure("That guild requires applications. Use /guild apply.");
        }

        GuildInvite invite = pendingInvites.get(joinerId);
        if (invite == null || !invite.guildId.equals(guild.getId())) {
            return GuildActionResult.failure("You do not have an invite to that guild.");
        }

        if (invite.expiresAtMillis > 0 && invite.expiresAtMillis < System.currentTimeMillis()) {
            pendingInvites.remove(joinerId);
            return GuildActionResult.failure("Your guild invite has expired.");
        }

        pendingInvites.remove(joinerId);
        GuildActionResult result = addMemberToGuild(guild, joinerId, GuildRole.MEMBER_ID, "Joined the guild.");
        GuildJoinedEvent.dispatch(guild.getId(), joinerId, GuildRole.MEMBER_ID, GuildJoinMethod.INVITE);
        return result;
    }

    public GuildActionResult applyToGuild(UUID applicantId, String nameOrTag) {
        return applyToGuild(applicantId, nameOrTag, null);
    }

    public GuildActionResult applyToGuild(UUID applicantId, String nameOrTag, String rawApplicationMessage) {
        if (memberToGuild.containsKey(applicantId)) {
            return GuildActionResult.failure("You are already in a guild.");
        }

        Guild guild = findGuildByNameOrTag(nameOrTag);
        if (guild == null) {
            return GuildActionResult.failure("That guild was not found.");
        }

        if (guild.getJoinPolicy() != GuildJoinPolicy.APPLICATION) {
            return GuildActionResult.failure("That guild is not accepting applications.");
        }

        if (guild.size() >= getMaxMembers(configOrDefault())) {
            return GuildActionResult.failure("That guild is full.");
        }

        RpgModConfig config = configOrDefault();
        String applicationMessage = normalizeGuildText(
                rawApplicationMessage,
                config == null ? 512 : config.getGuildApplicationMessageMaxLength());
        if (applicationMessage == null) {
            return GuildActionResult.failure("Invalid guild application message.");
        }

        GuildApplication existing = getValidApplication(applicantId);
        if (existing != null && existing.guildId.equals(guild.getId())) {
            return GuildActionResult.failure("You already applied to that guild.");
        }

        long createdAtMillis = System.currentTimeMillis();
        UUID previousGuildId = existing == null ? null : existing.guildId;
        pendingApplications.put(
                applicantId,
                new GuildApplication(
                        guild.getId(),
                        applicationMessage,
                        createdAtMillis,
                        resolveApplicationExpiryMillis(config, createdAtMillis)));
        saveGuildIfEnabled(guild, config);
        saveGuildByIdIfEnabled(previousGuildId, guild.getId(), config);
        GuildApplicationSubmittedEvent.dispatch(guild.getId(), applicantId);
        return GuildActionResult.success("Application submitted.", guild);
    }

    public GuildActionResult acceptApplication(UUID actorId, UUID applicantId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.ACCEPT_APPLICATIONS)) {
            return GuildActionResult.failure("You do not have permission to accept applications.");
        }

        GuildApplication application = getValidApplication(applicantId);
        if (application == null || !application.guildId.equals(guild.getId())) {
            return GuildActionResult.failure("No application found for that player.");
        }

        if (guild.size() >= getMaxMembers(configOrDefault())) {
            return GuildActionResult.failure("The guild is full.");
        }

        pendingApplications.remove(applicantId);
        GuildActionResult result = addMemberToGuild(guild, applicantId, GuildRole.MEMBER_ID, "Application accepted.");
        GuildApplicationAcceptedEvent.dispatch(guild.getId(), actorId, applicantId);
        GuildJoinedEvent.dispatch(guild.getId(), applicantId, GuildRole.MEMBER_ID, GuildJoinMethod.APPLICATION);
        return result;
    }

    public GuildActionResult denyApplication(UUID actorId, UUID applicantId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.REJECT_APPLICATIONS)) {
            return GuildActionResult.failure("You do not have permission to deny applications.");
        }

        GuildApplication application = getValidApplication(applicantId);
        if (application == null || !application.guildId.equals(guild.getId())) {
            return GuildActionResult.failure("No application found for that player.");
        }

        pendingApplications.remove(applicantId);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildApplicationDeniedEvent.dispatch(guild.getId(), actorId, applicantId);
        return GuildActionResult.success("Application denied.", guild);
    }

    public GuildActionResult leaveGuild(UUID memberId) {
        Guild guild = getGuildForMember(memberId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        boolean wasLeader = guild.isLeader(memberId);
        if (wasLeader) {
            UUID newLeaderId = handleLeaderLeave(guild, memberId);
            if (newLeaderId != null) {
                GuildLeaderChangedEvent.dispatch(guild.getId(), memberId, newLeaderId);
            } else {
                GuildDisbandedEvent.dispatch(guild.getId(), memberId);
            }
        } else {
            guild.removeMember(memberId);
        }

        memberToGuild.remove(memberId);
        if (guild.size() == 0) {
            guildsById.remove(guild.getId());
            deleteGuildIfEnabled(guild.getId(), configOrDefault());
        } else {
            saveGuildIfEnabled(guild, configOrDefault());
        }

        GuildLeftEvent.dispatch(guild.getId(), memberId, wasLeader);
        return GuildActionResult.success("You left the guild.");
    }

    public GuildActionResult disbandGuild(UUID leaderId) {
        Guild guild = getGuildForMember(leaderId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.isLeader(leaderId)) {
            return GuildActionResult.failure("Only the leader can disband the guild.");
        }

        removeGuild(guild);
        deleteGuildIfEnabled(guild.getId(), configOrDefault());
        GuildDisbandedEvent.dispatch(guild.getId(), leaderId);
        return GuildActionResult.success("Guild disbanded.");
    }

    public GuildActionResult kickMember(UUID actorId, UUID targetId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null || !guild.hasMember(targetId)) {
            return GuildActionResult.failure("That player is not in your guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.KICK)) {
            return GuildActionResult.failure("You do not have permission to kick members.");
        }

        if (guild.isLeader(targetId)) {
            return GuildActionResult.failure("You cannot kick the leader.");
        }

        guild.removeMember(targetId);
        memberToGuild.remove(targetId);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildMemberKickedEvent.dispatch(guild.getId(), actorId, targetId);

        return GuildActionResult.success("Member removed.");
    }

    public GuildActionResult transferLeadership(UUID actorId, UUID targetId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null || !guild.hasMember(targetId)) {
            return GuildActionResult.failure("That player is not in your guild.");
        }

        if (!guild.isLeader(actorId)) {
            return GuildActionResult.failure("Only the leader can transfer leadership.");
        }

        if (guild.isLeader(targetId)) {
            return GuildActionResult.failure("That player is already the leader.");
        }

        GuildMember oldLeader = guild.getMember(actorId);
        oldLeader.setRoleId(GuildRole.MEMBER_ID);

        GuildMember newLeader = guild.getMember(targetId);
        newLeader.setRoleId(GuildRole.LEADER_ID);
        guild.setLeaderId(targetId);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildLeaderChangedEvent.dispatch(guild.getId(), actorId, targetId);

        return GuildActionResult.success("Leadership transferred.");
    }

    public GuildActionResult setJoinPolicy(UUID actorId, GuildJoinPolicy policy) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.SET_JOIN_POLICY)) {
            return GuildActionResult.failure("You do not have permission to change join policy.");
        }

        GuildJoinPolicy oldPolicy = guild.getJoinPolicy();
        guild.setJoinPolicy(policy);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildJoinPolicyChangedEvent.dispatch(guild.getId(), actorId, oldPolicy, policy);
        return GuildActionResult.success("Join policy updated.");
    }

    public GuildActionResult createRole(UUID actorId, String roleName) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.MANAGE_ROLES)) {
            return GuildActionResult.failure("You do not have permission to manage roles.");
        }

        String roleId = normalizeRoleId(roleName);
        if (roleId == null) {
            return GuildActionResult.failure("Invalid role name.");
        }

        if (GuildRole.LEADER_ID.equals(roleId) || GuildRole.MEMBER_ID.equals(roleId)) {
            return GuildActionResult.failure("That role name is reserved.");
        }

        if (guild.getRole(roleId) != null) {
            return GuildActionResult.failure("That role already exists.");
        }

        GuildRole role = new GuildRole(roleId, roleName.trim(), EnumSet.noneOf(GuildPermission.class));
        guild.putRole(role);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildRoleCreatedEvent.dispatch(guild.getId(), actorId, roleId, role.getName());

        return GuildActionResult.success("Role created.");
    }

    public GuildActionResult renameRole(UUID actorId, String roleId, String roleName) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.MANAGE_ROLES)) {
            return GuildActionResult.failure("You do not have permission to manage roles.");
        }

        String normalizedId = roleId == null ? null : roleId.trim().toLowerCase();
        GuildRole role = guild.getRole(normalizedId);
        if (role == null) {
            return GuildActionResult.failure("That role does not exist.");
        }

        String normalizedRenamedId = normalizeRoleId(roleName);
        if (normalizedRenamedId == null) {
            return GuildActionResult.failure("Invalid role name.");
        }

        String currentRoleId = role.getId();
        if (!currentRoleId.equals(normalizedRenamedId)) {
            if ((GuildRole.LEADER_ID.equals(normalizedRenamedId) || GuildRole.MEMBER_ID.equals(normalizedRenamedId))
                    && !currentRoleId.equals(normalizedRenamedId)) {
                return GuildActionResult.failure("That role name is reserved.");
            }

            GuildRole conflictingRole = guild.getRole(normalizedRenamedId);
            if (conflictingRole != null && !conflictingRole.getId().equalsIgnoreCase(role.getId())) {
                return GuildActionResult.failure("That role already exists.");
            }
        }

        String trimmedName = roleName.trim();
        if (!role.getName().equals(trimmedName)) {
            role.setName(trimmedName);
            saveGuildIfEnabled(guild, configOrDefault());
        }

        return GuildActionResult.success("Role renamed.", guild);
    }

    public GuildActionResult setRolePermission(UUID actorId, String roleId, GuildPermission permission, boolean enabled) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.MANAGE_ROLES)) {
            return GuildActionResult.failure("You do not have permission to manage roles.");
        }

        String normalizedId = roleId == null ? null : roleId.toLowerCase();
        if (GuildRole.LEADER_ID.equals(normalizedId)) {
            return GuildActionResult.failure("Leader permissions cannot be changed.");
        }

        GuildRole role = guild.getRole(normalizedId);
        if (role == null) {
            return GuildActionResult.failure("That role does not exist.");
        }

        role.setPermission(permission, enabled);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildRolePermissionChangedEvent.dispatch(guild.getId(), actorId, role.getId(), permission, enabled);

        return GuildActionResult.success("Role permission updated.");
    }

    public GuildActionResult setRolePermissions(UUID actorId, String roleId, Set<GuildPermission> permissions) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.MANAGE_ROLES)) {
            return GuildActionResult.failure("You do not have permission to manage roles.");
        }

        String normalizedId = roleId == null ? null : roleId.trim().toLowerCase();
        if (GuildRole.LEADER_ID.equals(normalizedId)) {
            return GuildActionResult.failure("Leader permissions cannot be changed.");
        }

        GuildRole role = guild.getRole(normalizedId);
        if (role == null) {
            return GuildActionResult.failure("That role does not exist.");
        }

        EnumSet<GuildPermission> newPermissions = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(GuildPermission.class)
                : EnumSet.copyOf(permissions);

        boolean changed = false;
        for (GuildPermission permission : GuildPermission.values()) {
            boolean enabled = newPermissions.contains(permission);
            if (role.hasPermission(permission) == enabled) {
                continue;
            }

            role.setPermission(permission, enabled);
            GuildRolePermissionChangedEvent.dispatch(guild.getId(), actorId, role.getId(), permission, enabled);
            changed = true;
        }

        if (changed) {
            saveGuildIfEnabled(guild, configOrDefault());
        }

        return GuildActionResult.success("Role permissions updated.", guild);
    }

    public GuildActionResult assignRole(UUID actorId, UUID targetId, String roleId) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null || !guild.hasMember(targetId)) {
            return GuildActionResult.failure("That player is not in your guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.PROMOTE)) {
            return GuildActionResult.failure("You do not have permission to assign roles.");
        }

        String normalizedId = roleId == null ? null : roleId.toLowerCase();
        if (GuildRole.LEADER_ID.equals(normalizedId)) {
            if (!guild.isLeader(actorId)) {
                return GuildActionResult.failure("Only the leader can assign the leader role.");
            }
            return transferLeadership(actorId, targetId);
        }

        GuildRole role = guild.getRole(normalizedId);
        if (role == null) {
            return GuildActionResult.failure("That role does not exist.");
        }

        if (guild.isLeader(targetId)) {
            return GuildActionResult.failure("You cannot change the leader's role.");
        }

        GuildMember member = guild.getMember(targetId);
        member.setRoleId(normalizedId);
        saveGuildIfEnabled(guild, configOrDefault());
        GuildRoleAssignedEvent.dispatch(guild.getId(), actorId, targetId, normalizedId);

        return GuildActionResult.success("Role assigned.");
    }

    public GuildActionResult updateGuildInfo(UUID actorId,
            String rawName,
            String rawTag,
            String rawDescription,
            String rawMotd) {
        Guild guild = getGuildForMember(actorId);
        if (guild == null) {
            return GuildActionResult.failure("You are not in a guild.");
        }

        if (!guild.hasPermission(actorId, GuildPermission.MANAGE_GUILD_INFO)) {
            return GuildActionResult.failure("You do not have permission to manage guild info.");
        }

        RpgModConfig config = configOrDefault();
        boolean allowIdentityUpdates = config == null || config.isGuildAllowNameTagUpdates();

        String description = normalizeGuildText(rawDescription, config == null ? 256 : config.getGuildDescriptionMaxLength());
        if (description == null) {
            return GuildActionResult.failure("Invalid guild description.");
        }

        String motd = normalizeGuildText(rawMotd, config == null ? 128 : config.getGuildMotdMaxLength());
        if (motd == null) {
            return GuildActionResult.failure("Invalid guild message of the day.");
        }

        boolean changed = false;
        if (!guild.getDescription().equals(description)) {
            guild.setDescription(description);
            changed = true;
        }
        if (!guild.getMotd().equals(motd)) {
            guild.setMotd(motd);
            changed = true;
        }

        if (allowIdentityUpdates) {
            String newName = rawName == null ? guild.getName() : rawName.trim();
            String newTag = rawTag == null ? guild.getTag() : rawTag.trim();

            if (!guild.getName().equalsIgnoreCase(newName)) {
                if (!isNameValid(newName, config)) {
                    return GuildActionResult.failure("Invalid guild name.");
                }
                if (!isNameAvailable(newName, guild.getId())) {
                    return GuildActionResult.failure("That guild name is already taken.");
                }
                guild.setName(newName);
                changed = true;
            }

            if (!guild.getTag().equalsIgnoreCase(newTag)) {
                if (!isTagValid(newTag, config)) {
                    return GuildActionResult.failure("Invalid guild tag.");
                }
                if (!isTagAvailable(newTag, guild.getId())) {
                    return GuildActionResult.failure("That guild tag is already taken.");
                }
                guild.setTag(newTag);
                changed = true;
            }
        }

        if (changed) {
            saveGuildIfEnabled(guild, config);
        }

        return GuildActionResult.success("Guild info updated.", guild);
    }

    public Guild getGuildForMember(UUID memberId) {
        UUID guildId = memberToGuild.get(memberId);
        return guildId != null ? guildsById.get(guildId) : null;
    }

    public PendingGuildInvite getPendingInvite(UUID targetId) {
        if (getValidInvite(targetId) == null) {
            return null;
        }

        return toPendingInvite(targetId);
    }

    public List<PendingGuildInvite> getPendingInvitesForGuild(UUID guildId) {
        List<PendingGuildInvite> invites = new ArrayList<>();
        for (Map.Entry<UUID, GuildInvite> entry : pendingInvites.entrySet()) {
            PendingGuildInvite invite = toPendingInvite(entry.getKey());
            if (invite != null && invite.guildId().equals(guildId)) {
                invites.add(invite);
            }
        }
        invites.sort(Comparator.comparingLong(PendingGuildInvite::expiresAtMillis));
        return invites;
    }

    public PendingGuildApplication getPendingApplication(UUID applicantId) {
        if (getValidApplication(applicantId) == null) {
            return null;
        }

        return toPendingApplication(applicantId);
    }

    public List<PendingGuildApplication> getPendingApplicationsForGuild(UUID guildId) {
        List<PendingGuildApplication> applications = new ArrayList<>();
        for (Map.Entry<UUID, GuildApplication> entry : pendingApplications.entrySet()) {
            PendingGuildApplication application = toPendingApplication(entry.getKey());
            if (application != null && application.guildId().equals(guildId)) {
                applications.add(application);
            }
        }
        applications.sort(Comparator.comparingLong(PendingGuildApplication::createdAtMillis));
        return applications;
    }

    public List<Guild> getGuilds() {
        return new ArrayList<>(guildsById.values());
    }

    public void loadFromAssets() {
        RpgModConfig config = resolveConfig();
        if (config != null && !config.isGuildPersistenceEnabled()) {
            return;
        }

        clear();

        for (GuildData data : persistence.loadAll()) {
            if (data == null) {
                continue;
            }

            Guild guild = data.toGuild();
            guildsById.put(guild.getId(), guild);
            for (GuildMember member : guild.getMemberList()) {
                memberToGuild.put(member.getMemberId(), guild.getId());
            }

            boolean modified = false;
            for (GuildData.GuildApplicationData applicationData : data.getApplications()) {
                GuildApplication application = toGuildApplication(applicationData, guild);
                if (application == null) {
                    modified = true;
                    continue;
                }

                UUID applicantId = applicationData.getApplicantId();
                if (guild.hasMember(applicantId) || memberToGuild.containsKey(applicantId)) {
                    modified = true;
                    continue;
                }

                pendingApplications.put(applicantId, application);
            }

            if (modified) {
                saveGuildIfEnabled(guild, config);
            }
        }
    }

    public void clear() {
        guildsById.clear();
        memberToGuild.clear();
        pendingInvites.clear();
        pendingApplications.clear();
    }

    private UUID handleLeaderLeave(Guild guild, UUID leaderId) {
        guild.removeMember(leaderId);

        UUID replacement = guild.getMembers().keySet().stream()
                .filter(id -> !id.equals(leaderId))
                .findFirst()
                .orElse(null);

        if (replacement != null) {
            GuildMember newLeader = guild.getMember(replacement);
            newLeader.setRoleId(GuildRole.LEADER_ID);
            guild.setLeaderId(replacement);
            return replacement;
        } else {
            removeGuild(guild);
            return null;
        }
    }

    private void removeGuild(Guild guild) {
        for (GuildMember member : guild.getMemberList()) {
            memberToGuild.remove(member.getMemberId());
        }
        guildsById.remove(guild.getId());
    }

    private GuildActionResult addMemberToGuild(Guild guild, UUID memberId, String roleId, String message) {
        RpgModConfig config = configOrDefault();
        clearPendingApplication(memberId, guild.getId(), config);
        GuildRole role = guild.getRole(roleId);
        String resolvedRole = role != null ? role.getId() : GuildRole.MEMBER_ID;
        GuildMember member = new GuildMember(memberId, resolvedRole, System.currentTimeMillis());
        guild.addMember(member);
        memberToGuild.put(memberId, guild.getId());
        saveGuildIfEnabled(guild, config);
        return GuildActionResult.success(message, guild);
    }

    private GuildInvite getValidInvite(UUID targetId) {
        GuildInvite invite = pendingInvites.get(targetId);
        if (invite == null) {
            return null;
        }

        if (invite.expiresAtMillis > 0 && invite.expiresAtMillis < System.currentTimeMillis()) {
            pendingInvites.remove(targetId);
            return null;
        }

        return invite;
    }

    private PendingGuildInvite toPendingInvite(UUID targetId) {
        GuildInvite validInvite = getValidInvite(targetId);
        if (validInvite == null) {
            return null;
        }

        Guild guild = guildsById.get(validInvite.guildId);
        if (guild == null) {
            pendingInvites.remove(targetId);
            return null;
        }

        return new PendingGuildInvite(
                targetId,
                guild.getId(),
                guild.getName(),
                guild.getTag(),
                validInvite.inviterId,
                validInvite.expiresAtMillis);
    }

    private PendingGuildApplication toPendingApplication(UUID applicantId) {
        GuildApplication validApplication = getValidApplication(applicantId);
        if (validApplication == null) {
            return null;
        }

        Guild guild = guildsById.get(validApplication.guildId);
        if (guild == null) {
            pendingApplications.remove(applicantId);
            return null;
        }

        return new PendingGuildApplication(
                applicantId,
                guild.getId(),
                guild.getName(),
                guild.getTag(),
                validApplication.applicationMessage,
                validApplication.createdAtMillis,
                validApplication.expiresAtMillis);
    }

    private GuildApplication toGuildApplication(GuildData.GuildApplicationData applicationData, Guild guild) {
        if (applicationData == null || applicationData.getApplicantId() == null) {
            return null;
        }

        long expiresAtMillis = applicationData.getExpiresAtMillis();
        if (expiresAtMillis > 0L && expiresAtMillis < System.currentTimeMillis()) {
            return null;
        }

        return new GuildApplication(
                guild.getId(),
                applicationData.getApplicationMessage(),
                applicationData.getCreatedAtMillis(),
                expiresAtMillis);
    }

    private GuildApplication clearPendingApplication(UUID applicantId, UUID skipGuildId, RpgModConfig config) {
        GuildApplication removedApplication = pendingApplications.remove(applicantId);
        if (removedApplication == null) {
            return null;
        }

        saveGuildByIdIfEnabled(removedApplication.guildId, skipGuildId, config);
        return removedApplication;
    }

    private Guild findGuildByNameOrTag(String nameOrTag) {
        if (nameOrTag == null) {
            return null;
        }

        String needle = nameOrTag.trim();
        if (needle.isEmpty()) {
            return null;
        }

        for (Guild guild : guildsById.values()) {
            if (guild.getName().equalsIgnoreCase(needle)) {
                return guild;
            }
            if (guild.getTag() != null && !guild.getTag().isEmpty()
                    && guild.getTag().equalsIgnoreCase(needle)) {
                return guild;
            }
        }

        return null;
    }

    private boolean isNameAvailable(String name) {
        return isNameAvailable(name, null);
    }

    private boolean isNameAvailable(String name, UUID excludedGuildId) {
        for (Guild guild : guildsById.values()) {
            if (excludedGuildId != null && guild.getId().equals(excludedGuildId)) {
                continue;
            }
            if (guild.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTagAvailable(String tag) {
        return isTagAvailable(tag, null);
    }

    private boolean isTagAvailable(String tag, UUID excludedGuildId) {
        for (Guild guild : guildsById.values()) {
            if (excludedGuildId != null && guild.getId().equals(excludedGuildId)) {
                continue;
            }
            if (guild.getTag() != null && guild.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
        }
        return true;
    }

    private boolean isNameValid(String name, RpgModConfig config) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        int min = config == null ? 3 : config.getGuildNameMinLength();
        int max = config == null ? 24 : config.getGuildNameMaxLength();
        if (trimmed.length() < min || trimmed.length() > max) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_')) {
                return false;
            }
        }
        return true;
    }

    private boolean isTagValid(String tag, RpgModConfig config) {
        if (tag == null) {
            return false;
        }
        String trimmed = tag.trim();
        int min = config == null ? 2 : config.getGuildTagMinLength();
        int max = config == null ? 5 : config.getGuildTagMaxLength();
        if (trimmed.length() < min || trimmed.length() > max) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isLetterOrDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeRoleId(String roleName) {
        if (roleName == null) {
            return null;
        }
        String trimmed = roleName.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            } else if (ch == ' ' || ch == '-' || ch == '_') {
                builder.append('_');
            }
        }
        String roleId = builder.toString().replaceAll("_+", "_");
        return roleId.isEmpty() ? null : roleId;
    }

    private String normalizeGuildText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String trimmed = text.trim();
        if (trimmed.length() > Math.max(0, maxLength)) {
            return null;
        }

        return trimmed;
    }

    private int getMaxMembers(RpgModConfig config) {
        return config == null ? 50 : config.getGuildMaxMembers();
    }

    private long getInviteExpiryMillis(RpgModConfig config) {
        int seconds = config == null ? 3600 : config.getGuildInviteExpirySeconds();
        if (seconds <= 0) {
            return 0L;
        }
        return seconds * 1000L;
    }

    private long resolveApplicationExpiryMillis(RpgModConfig config, long createdAtMillis) {
        int seconds = config == null ? 604800 : config.getGuildApplicationExpirySeconds();
        if (seconds <= 0) {
            return 0L;
        }
        return createdAtMillis + (seconds * 1000L);
    }

    private GuildApplication getValidApplication(UUID applicantId) {
        GuildApplication application = pendingApplications.get(applicantId);
        if (application == null) {
            return null;
        }

        if (application.expiresAtMillis > 0L && application.expiresAtMillis < System.currentTimeMillis()) {
            pendingApplications.remove(applicantId);
            saveGuildByIdIfEnabled(application.guildId, null, configOrDefault());
            return null;
        }

        return application;
    }

    private void saveGuildIfEnabled(Guild guild, RpgModConfig config) {
        if (config != null && !config.isGuildPersistenceEnabled()) {
            return;
        }
        persistence.saveGuild(guild, buildApplicationData(guild.getId()));
    }

    private List<GuildData.GuildApplicationData> buildApplicationData(UUID guildId) {
        List<GuildData.GuildApplicationData> applications = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, GuildApplication> entry : pendingApplications.entrySet()) {
            GuildApplication application = entry.getValue();
            if (!application.guildId.equals(guildId)) {
                continue;
            }
            if (application.expiresAtMillis > 0L && application.expiresAtMillis < now) {
                continue;
            }

            applications.add(new GuildData.GuildApplicationData(
                    entry.getKey(),
                    application.applicationMessage,
                    application.createdAtMillis,
                    application.expiresAtMillis));
        }
        applications.sort(Comparator.comparingLong(GuildData.GuildApplicationData::getCreatedAtMillis));
        return applications;
    }

    private void saveGuildByIdIfEnabled(UUID guildId, UUID skipGuildId, RpgModConfig config) {
        if (guildId == null || guildId.equals(skipGuildId)) {
            return;
        }

        Guild guild = guildsById.get(guildId);
        if (guild != null) {
            saveGuildIfEnabled(guild, config);
        }
    }

    private void deleteGuildIfEnabled(UUID guildId, RpgModConfig config) {
        if (config != null && !config.isGuildPersistenceEnabled()) {
            return;
        }
        persistence.deleteGuild(guildId);
    }

    private RpgModConfig resolveConfig() {
        try {
            var assetMap = RpgModConfig.getAssetMap();
            if (assetMap == null) {
                return null;
            }

            RpgModConfig config = assetMap.getAsset("Default");
            if (config == null) {
                config = assetMap.getAsset("default");
            }

            if (config == null) {
                RpgLogging.debugDeveloper("[Guild] RpgModConfig not found; using defaults.");
            }

            return config;
        } catch (Throwable ex) {
            return null;
        }
    }

    private RpgModConfig configOrDefault() {
        return resolveConfig();
    }

    private static class GuildInvite {
        private final UUID guildId;
        private final UUID inviterId;
        private final long expiresAtMillis;

        private GuildInvite(UUID guildId, UUID inviterId, long expiresAtMillis) {
            this.guildId = guildId;
            this.inviterId = inviterId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static class GuildApplication {
        private final UUID guildId;
        private final String applicationMessage;
        private final long createdAtMillis;
        private final long expiresAtMillis;

        private GuildApplication(UUID guildId, String applicationMessage, long createdAtMillis, long expiresAtMillis) {
            this.guildId = guildId;
            this.applicationMessage = applicationMessage;
            this.createdAtMillis = createdAtMillis;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    public record PendingGuildInvite(
            UUID targetId,
            UUID guildId,
            String guildName,
            String guildTag,
            UUID inviterId,
            long expiresAtMillis) {
    }

    public record PendingGuildApplication(
            UUID applicantId,
            UUID guildId,
            String guildName,
            String guildTag,
            String applicationMessage,
            long createdAtMillis,
            long expiresAtMillis) {
    }
}
