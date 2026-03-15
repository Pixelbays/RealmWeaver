package org.pixelbays.rpg.party.finder;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.pixelbays.rpg.party.PartyType;

public class GroupFinderSnapshot {

    private final List<ListingView> listings;
    @Nullable
    private final LeaderListingView ownedListing;
    @Nullable
    private final ApplicationView currentApplication;

    public GroupFinderSnapshot(List<ListingView> listings,
            @Nullable LeaderListingView ownedListing,
            @Nullable ApplicationView currentApplication) {
        this.listings = List.copyOf(listings);
        this.ownedListing = ownedListing;
        this.currentApplication = currentApplication;
    }

    public List<ListingView> getListings() {
        return listings;
    }

    @Nullable
    public LeaderListingView getOwnedListing() {
        return ownedListing;
    }

    @Nullable
    public ApplicationView getCurrentApplication() {
        return currentApplication;
    }

    public static class ListingView {
        private final UUID partyId;
        private final PartyType partyType;
        private final String leaderName;
        private final String activity;
        private final String description;
        private final String requirements;
        private final int currentSize;
        private final int maxSize;
        private final int applicantCount;
        private final long updatedAtMillis;
        private final boolean recruiting;

        public ListingView(UUID partyId,
                PartyType partyType,
                String leaderName,
                String activity,
                String description,
                String requirements,
                int currentSize,
                int maxSize,
                int applicantCount,
                long updatedAtMillis,
                boolean recruiting) {
            this.partyId = partyId;
            this.partyType = partyType;
            this.leaderName = leaderName;
            this.activity = activity;
            this.description = description;
            this.requirements = requirements;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.applicantCount = applicantCount;
            this.updatedAtMillis = updatedAtMillis;
            this.recruiting = recruiting;
        }

        public UUID getPartyId() {
            return partyId;
        }

        public PartyType getPartyType() {
            return partyType;
        }

        public String getLeaderName() {
            return leaderName;
        }

        public String getActivity() {
            return activity;
        }

        public String getDescription() {
            return description;
        }

        public String getRequirements() {
            return requirements;
        }

        public int getCurrentSize() {
            return currentSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public int getApplicantCount() {
            return applicantCount;
        }

        public long getUpdatedAtMillis() {
            return updatedAtMillis;
        }

        public boolean isRecruiting() {
            return recruiting;
        }
    }

    public static class LeaderListingView extends ListingView {
        private final List<ApplicationView> applications;

        public LeaderListingView(UUID partyId,
                PartyType partyType,
                String leaderName,
                String activity,
                String description,
                String requirements,
                int currentSize,
                int maxSize,
                int applicantCount,
                long updatedAtMillis,
                boolean recruiting,
                List<ApplicationView> applications) {
            super(partyId, partyType, leaderName, activity, description, requirements, currentSize, maxSize,
                    applicantCount, updatedAtMillis, recruiting);
            this.applications = List.copyOf(applications);
        }

        public List<ApplicationView> getApplications() {
            return applications;
        }
    }

    public static class ApplicationView {
        private final UUID applicantId;
        private final UUID partyId;
        private final String applicantName;
        private final String activity;
        private final String desiredRole;
        private final String note;
        private final long submittedAtMillis;

        public ApplicationView(UUID applicantId,
                UUID partyId,
                String applicantName,
                String activity,
                String desiredRole,
                String note,
                long submittedAtMillis) {
            this.applicantId = applicantId;
            this.partyId = partyId;
            this.applicantName = applicantName;
            this.activity = activity;
            this.desiredRole = desiredRole;
            this.note = note;
            this.submittedAtMillis = submittedAtMillis;
        }

        public UUID getApplicantId() {
            return applicantId;
        }

        public UUID getPartyId() {
            return partyId;
        }

        public String getApplicantName() {
            return applicantName;
        }

        public String getActivity() {
            return activity;
        }

        public String getDesiredRole() {
            return desiredRole;
        }

        public String getNote() {
            return note;
        }

        public long getSubmittedAtMillis() {
            return submittedAtMillis;
        }
    }
}
