package org.pixelbays.rpg.party.finder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.classes.config.settings.ClassModSettings;
import org.pixelbays.rpg.global.config.RpgModConfig;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartySettings;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

@SuppressWarnings("null")
public class GroupFinderManager {

    private final Map<UUID, Listing> listingsByPartyId = new ConcurrentHashMap<>();
    private final Map<UUID, Application> applicationsByApplicantId = new ConcurrentHashMap<>();
    private final PartyManager partyManager;
    private final LongSupplier currentTimeMillisSupplier;
    private final RuntimeAccess runtimeAccess;
    private final BooleanSupplier enabledSupplier;

    public GroupFinderManager(@Nonnull PartyManager partyManager) {
        this(partyManager, System::currentTimeMillis, new UniverseRuntimeAccess(), GroupFinderManager::isEnabledFromConfig);
    }

    GroupFinderManager(@Nonnull PartyManager partyManager,
            @Nonnull LongSupplier currentTimeMillisSupplier,
            @Nonnull RuntimeAccess runtimeAccess) {
        this(partyManager, currentTimeMillisSupplier, runtimeAccess, GroupFinderManager::isEnabledFromConfig);
    }

    GroupFinderManager(@Nonnull PartyManager partyManager,
            @Nonnull LongSupplier currentTimeMillisSupplier,
            @Nonnull RuntimeAccess runtimeAccess,
            @Nonnull BooleanSupplier enabledSupplier) {
        this.partyManager = Objects.requireNonNull(partyManager, "partyManager");
        this.currentTimeMillisSupplier = Objects.requireNonNull(currentTimeMillisSupplier, "currentTimeMillisSupplier");
        this.runtimeAccess = Objects.requireNonNull(runtimeAccess, "runtimeAccess");
        this.enabledSupplier = Objects.requireNonNull(enabledSupplier, "enabledSupplier");
    }

    @Nonnull
    public GroupFinderActionResult upsertListing(@Nonnull UUID actorId,
            @Nullable String activity,
            @Nullable String description,
            @Nullable String requirements,
            @Nullable String openRoles) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }

        ManagedParty managedParty = resolveManagedParty(actorId);
        if (managedParty == null) {
            return GroupFinderActionResult.failure("You must be the party leader to manage a listing.");
        }

        String normalizedActivity = normalize(activity);
        if (normalizedActivity.isEmpty()) {
            return GroupFinderActionResult.failure("Activity is required.");
        }

        RoleParseResult parsedRoles = parseRoles(openRoles);
        if (!parsedRoles.invalidRoles().isEmpty()) {
            return GroupFinderActionResult.failure("Unknown role type: " + parsedRoles.invalidRoles().getFirst());
        }

        Listing listing = new Listing(
                managedParty.party().getId(),
                normalizedActivity,
                normalize(description),
                normalize(requirements),
            parsedRoles.roles(),
                now());
        listingsByPartyId.put(managedParty.party().getId(), listing);
        return GroupFinderActionResult.success("Listing updated.", managedParty.party().getId());
    }

    @Nonnull
    public GroupFinderActionResult removeListing(@Nonnull UUID actorId) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }

        ManagedParty managedParty = resolveManagedParty(actorId);
        if (managedParty == null) {
            return GroupFinderActionResult.failure("You must be the party leader to manage a listing.");
        }

        Listing removed = listingsByPartyId.remove(managedParty.party().getId());
        if (removed == null) {
            return GroupFinderActionResult.failure("You do not have an active listing.");
        }

        removeApplicationsForParty(managedParty.party().getId());
        return GroupFinderActionResult.success("Listing removed.", managedParty.party().getId());
    }

    @Nonnull
    public GroupFinderActionResult applyToListing(@Nonnull UUID applicantId,
            @Nullable UUID listingPartyId,
            @Nullable String desiredRole,
            @Nullable String note) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }
        if (partyManager.getPartyForMember(applicantId) != null) {
            return GroupFinderActionResult.failure("You are already in a party.");
        }
        if (applicationsByApplicantId.containsKey(applicantId)) {
            return GroupFinderActionResult.failure("You already have an active application.");
        }
        if (listingPartyId == null) {
            return GroupFinderActionResult.failure("That listing does not exist.");
        }

        Listing listing = listingsByPartyId.get(listingPartyId);
        Party party = partyManager.getParty(listingPartyId);
        if (listing == null || party == null) {
            return GroupFinderActionResult.failure("That listing does not exist.");
        }
        if (party.getLeaderId() != null && party.getLeaderId().equals(applicantId)) {
            return GroupFinderActionResult.failure("You cannot apply to your own party listing.");
        }
        if (!isRecruiting(party)) {
            return GroupFinderActionResult.failure("That group is no longer recruiting.");
        }

        String normalizedDesiredRole = normalize(desiredRole);
        if (!normalizedDesiredRole.isEmpty()) {
            String canonicalDesiredRole = canonicalRole(normalizedDesiredRole);
            if (canonicalDesiredRole == null) {
                return GroupFinderActionResult.failure("Unknown role type: " + normalizedDesiredRole);
            }
            normalizedDesiredRole = canonicalDesiredRole;
        }

        if (!listing.openRoles().isEmpty()) {
            if (normalizedDesiredRole.isEmpty()) {
                return GroupFinderActionResult.failure("Choose a role for this listing.");
            }
            if (!containsIgnoreCase(listing.openRoles(), normalizedDesiredRole)) {
                return GroupFinderActionResult.failure("That group is not recruiting for that role.");
            }
        }

        Application application = new Application(
                applicantId,
                listingPartyId,
                normalizedDesiredRole,
                normalize(note),
                now());
        applicationsByApplicantId.put(applicantId, application);

        UUID leaderId = party.getLeaderId();
        if (leaderId != null) {
            runtimeAccess.sendPlayerMessage(leaderId,
                    Message.translation("pixelbays.rpg.groupFinder.notify.newApplicant")
                            .param("player", runtimeAccess.resolveDisplayName(applicantId))
                            .param("activity", listing.activity()));
        }

        return GroupFinderActionResult.success("Application submitted.", listingPartyId);
    }

    @Nonnull
    public GroupFinderActionResult withdrawApplication(@Nonnull UUID applicantId) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }

        Application removed = applicationsByApplicantId.remove(applicantId);
        if (removed == null) {
            return GroupFinderActionResult.failure("You do not have an active application.");
        }

        return GroupFinderActionResult.success("Application withdrawn.", removed.partyId());
    }

    @Nonnull
    public GroupFinderActionResult rejectApplicant(@Nonnull UUID actorId, @Nonnull UUID applicantId) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }

        ManagedParty managedParty = resolveManagedParty(actorId);
        if (managedParty == null) {
            return GroupFinderActionResult.failure("You must be the party leader to manage a listing.");
        }

        Application application = applicationsByApplicantId.get(applicantId);
        if (application == null || !application.partyId().equals(managedParty.party().getId())) {
            return GroupFinderActionResult.failure("No application found for that player.");
        }

        applicationsByApplicantId.remove(applicantId);
        runtimeAccess.sendPlayerMessage(applicantId,
                Message.translation("pixelbays.rpg.groupFinder.notify.applicationRejected")
                        .param("activity", listingActivity(managedParty.party().getId())));
        return GroupFinderActionResult.success("Applicant rejected.", managedParty.party().getId());
    }

    @Nonnull
    public GroupFinderActionResult inviteApplicant(@Nonnull UUID actorId, @Nonnull UUID applicantId) {
        pruneInvalidState();
        if (!isEnabled()) {
            return GroupFinderActionResult.failure("Group finder is disabled.");
        }

        ManagedParty managedParty = resolveManagedParty(actorId);
        if (managedParty == null) {
            return GroupFinderActionResult.failure("You must be the party leader to manage a listing.");
        }

        Application application = applicationsByApplicantId.get(applicantId);
        if (application == null || !application.partyId().equals(managedParty.party().getId())) {
            return GroupFinderActionResult.failure("No application found for that player.");
        }

        PartyActionResult inviteResult = partyManager.invitePlayer(actorId, applicantId);
        if (!inviteResult.isSuccess()) {
            return GroupFinderActionResult.failure(inviteResult.getMessage());
        }

        applicationsByApplicantId.remove(applicantId);
        runtimeAccess.sendPlayerMessage(applicantId,
                Message.translation("pixelbays.rpg.groupFinder.notify.applicationInvited")
                        .param("leader", runtimeAccess.resolveDisplayName(actorId))
                        .param("activity", listingActivity(managedParty.party().getId())));
        return GroupFinderActionResult.success("Party invite sent.", managedParty.party().getId());
    }

    @Nonnull
    public GroupFinderSnapshot getSnapshot(@Nonnull UUID viewerId) {
        pruneInvalidState();

        List<GroupFinderSnapshot.ListingView> listingViews = listingsByPartyId.values().stream()
                .map(this::toListingView)
                .filter(Objects::nonNull)
                .filter(GroupFinderSnapshot.ListingView::isRecruiting)
                .sorted(Comparator.comparingLong(GroupFinderSnapshot.ListingView::getUpdatedAtMillis).reversed()
                        .thenComparing(GroupFinderSnapshot.ListingView::getActivity, String.CASE_INSENSITIVE_ORDER))
                .toList();

        GroupFinderSnapshot.LeaderListingView ownedListing = null;
        Party viewerParty = partyManager.getPartyForMember(viewerId);
        if (viewerParty != null && viewerParty.isLeader(viewerId)) {
            Listing owned = listingsByPartyId.get(viewerParty.getId());
            if (owned != null) {
                ownedListing = toLeaderListingView(owned, viewerParty);
            }
        }

        GroupFinderSnapshot.ApplicationView currentApplication = null;
        Application application = applicationsByApplicantId.get(viewerId);
        if (application != null) {
            Listing listing = listingsByPartyId.get(application.partyId());
            Party party = partyManager.getParty(application.partyId());
            if (listing != null && party != null) {
                currentApplication = toApplicationView(application, listing);
            }
        }

        return new GroupFinderSnapshot(listingViews, ownedListing, currentApplication);
    }

    private void pruneInvalidState() {
        List<UUID> invalidPartyIds = new ArrayList<>();
        for (UUID partyId : listingsByPartyId.keySet()) {
            if (partyManager.getParty(partyId) == null) {
                invalidPartyIds.add(partyId);
            }
        }
        for (UUID partyId : invalidPartyIds) {
            listingsByPartyId.remove(partyId);
            removeApplicationsForParty(partyId);
        }

        List<UUID> invalidApplicantIds = new ArrayList<>();
        for (var entry : applicationsByApplicantId.entrySet()) {
            UUID applicantId = entry.getKey();
            Application application = entry.getValue();
            if (partyManager.getPartyForMember(applicantId) != null
                    || !listingsByPartyId.containsKey(application.partyId())
                    || partyManager.getParty(application.partyId()) == null) {
                invalidApplicantIds.add(applicantId);
            }
        }
        for (UUID applicantId : invalidApplicantIds) {
            applicationsByApplicantId.remove(applicantId);
        }
    }

    private void removeApplicationsForParty(@Nonnull UUID partyId) {
        applicationsByApplicantId.entrySet().removeIf(entry -> entry.getValue().partyId().equals(partyId));
    }

    @Nullable
    private ManagedParty resolveManagedParty(@Nonnull UUID actorId) {
        Party party = partyManager.getPartyForMember(actorId);
        if (party == null || !party.isLeader(actorId)) {
            return null;
        }
        return new ManagedParty(actorId, party);
    }

    @Nullable
    private GroupFinderSnapshot.ListingView toListingView(@Nonnull Listing listing) {
        Party party = partyManager.getParty(listing.partyId());
        if (party == null) {
            return null;
        }

        return new GroupFinderSnapshot.ListingView(
                listing.partyId(),
                party.getType(),
                runtimeAccess.resolveDisplayName(party.getLeaderId()),
                listing.activity(),
                listing.description(),
                listing.requirements(),
                listing.openRoles(),
                party.size(),
                maxSize(party),
                applicantCount(listing.partyId()),
                listing.updatedAtMillis(),
                isRecruiting(party));
    }

    @Nonnull
    private GroupFinderSnapshot.LeaderListingView toLeaderListingView(@Nonnull Listing listing, @Nonnull Party party) {
        List<GroupFinderSnapshot.ApplicationView> applications = applicationsByApplicantId.values().stream()
                .filter(application -> application.partyId().equals(listing.partyId()))
                .sorted(Comparator.comparingLong(Application::submittedAtMillis))
                .map(application -> toApplicationView(application, listing))
                .toList();

        return new GroupFinderSnapshot.LeaderListingView(
                listing.partyId(),
                party.getType(),
                runtimeAccess.resolveDisplayName(party.getLeaderId()),
                listing.activity(),
                listing.description(),
                listing.requirements(),
                listing.openRoles(),
                party.size(),
                maxSize(party),
                applications.size(),
                listing.updatedAtMillis(),
                isRecruiting(party),
                applications);
    }

    @Nonnull
    private GroupFinderSnapshot.ApplicationView toApplicationView(@Nonnull Application application, @Nonnull Listing listing) {
        return new GroupFinderSnapshot.ApplicationView(
                application.applicantId(),
                application.partyId(),
                runtimeAccess.resolveDisplayName(application.applicantId()),
                listing.activity(),
                application.desiredRole(),
                application.note(),
                application.submittedAtMillis());
    }

    private int applicantCount(@Nonnull UUID partyId) {
        int count = 0;
        for (Application application : applicationsByApplicantId.values()) {
            if (application.partyId().equals(partyId)) {
                count++;
            }
        }
        return count;
    }

    private boolean isRecruiting(@Nonnull Party party) {
        return party.size() < maxSize(party);
    }

    private int maxSize(@Nonnull Party party) {
        PartySettings settings = party.getSettings();
        if (settings == null) {
            return party.getType() == org.pixelbays.rpg.party.PartyType.RAID ? 20 : 5;
        }
        return settings.getMaxSize();
    }

    @Nonnull
    private String listingActivity(@Nonnull UUID partyId) {
        Listing listing = listingsByPartyId.get(partyId);
        return listing == null ? "" : listing.activity();
    }

    private boolean isEnabled() {
        return enabledSupplier.getAsBoolean();
    }

    @Nonnull
    private RoleParseResult parseRoles(@Nullable String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            return new RoleParseResult(List.of(), List.of());
        }

        Set<String> parsedRoles = new LinkedHashSet<>();
        List<String> invalidRoles = new ArrayList<>();
        for (String token : rawRoles.split(",")) {
            String normalizedToken = normalize(token);
            if (normalizedToken.isEmpty()) {
                continue;
            }

            String canonicalRole = canonicalRole(normalizedToken);
            if (canonicalRole == null) {
                invalidRoles.add(normalizedToken);
                continue;
            }
            parsedRoles.add(canonicalRole);
        }

        return new RoleParseResult(new ArrayList<>(parsedRoles), invalidRoles);
    }

    @Nullable
    private String canonicalRole(@Nullable String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        for (String configuredRole : getConfiguredRoleTypes()) {
            if (configuredRole != null && configuredRole.equalsIgnoreCase(role)) {
                return configuredRole;
            }
        }

        return null;
    }

    @Nonnull
    private List<String> getConfiguredRoleTypes() {
        RpgModConfig config = resolveConfig();
        if (config != null && !config.getClassSettings().getRoleTypes().isEmpty()) {
            return config.getClassSettings().getRoleTypes();
        }
        return new ClassModSettings().getRoleTypes();
    }

    private boolean containsIgnoreCase(@Nonnull List<String> values, @Nonnull String target) {
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnabledFromConfig() {
        RpgModConfig config = resolveConfig();
        return config == null || config.isGroupFinderEnabled();
    }

    @Nullable
    private static RpgModConfig resolveConfig() {
        try {
            var assetMap = RpgModConfig.getAssetMap();
            if (assetMap == null) {
                return null;
            }

            RpgModConfig config = assetMap.getAsset("Default");
            if (config == null) {
                config = assetMap.getAsset("default");
            }
            return config;
        } catch (Throwable throwable) {
            return null;
        }
    }

    @Nonnull
    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private long now() {
        return currentTimeMillisSupplier.getAsLong();
    }

        record Listing(UUID partyId,
            String activity,
            String description,
            String requirements,
            List<String> openRoles,
            long updatedAtMillis) {
    }

    record Application(UUID applicantId, UUID partyId, String desiredRole, String note, long submittedAtMillis) {
    }

    record ManagedParty(UUID actorId, Party party) {
    }

    record RoleParseResult(List<String> roles, List<String> invalidRoles) {
    }

    interface RuntimeAccess {
        void sendPlayerMessage(@Nonnull UUID playerId, @Nonnull Message message);

        @Nonnull
        String resolveDisplayName(@Nonnull UUID entityId);
    }

    private static class UniverseRuntimeAccess implements RuntimeAccess {
        @Override
        public void sendPlayerMessage(@Nonnull UUID playerId, @Nonnull Message message) {
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef != null) {
                playerRef.sendMessage(message);
            }
        }

        @Override
        @Nonnull
        public String resolveDisplayName(@Nonnull UUID entityId) {
            PlayerRef playerRef = Universe.get().getPlayer(entityId);
            if (playerRef != null) {
                return playerRef.getUsername();
            }
            return entityId.toString();
        }
    }
}
