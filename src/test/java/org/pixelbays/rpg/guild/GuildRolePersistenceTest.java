package org.pixelbays.rpg.guild;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import java.util.UUID;

import com.hypixel.hytale.codec.ExtraInfo;
import org.junit.jupiter.api.Test;
import org.pixelbays.rpg.guild.config.GuildData;

class GuildRolePersistenceTest {

    @Test
    void guildRoundTrip_preservesEmptyPermissionRoles() {
        UUID leaderId = UUID.randomUUID();
        Guild original = new Guild(UUID.randomUUID(), "Knights of Pixelbays", "KOP", leaderId, GuildJoinPolicy.INVITE_ONLY);
        original.addMember(new GuildMember(leaderId, GuildRole.LEADER_ID, System.currentTimeMillis()));

        Guild restored = GuildData.fromGuild(original).toGuild();
        GuildRole memberRole = restored.getRole(GuildRole.MEMBER_ID);

        assertNotNull(memberRole);
        assertFalse(memberRole.getPermissions().iterator().hasNext());
    }

    @Test
    void guildRoundTrip_preservesPendingApplications() {
        UUID guildId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        Guild original = new Guild(guildId, "Knights of Pixelbays", "KOP", leaderId, GuildJoinPolicy.APPLICATION);
        original.addMember(new GuildMember(leaderId, GuildRole.LEADER_ID, System.currentTimeMillis()));

        GuildData originalData = GuildData.fromGuild(
                original,
                java.util.List.of(new GuildData.GuildApplicationData(applicantId, "Ready to help the guild.", 1234L, 5678L)));

        BsonValue encoded = GuildData.CODEC.encode(originalData, new ExtraInfo());
        GuildData restoredData = GuildData.CODEC.decode((BsonDocument) encoded, new ExtraInfo());

        assertNotNull(restoredData);
        assertEquals(1, restoredData.getApplications().size());
        assertEquals(applicantId, restoredData.getApplications().get(0).getApplicantId());
        assertEquals("Ready to help the guild.", restoredData.getApplications().get(0).getApplicationMessage());
        assertEquals(1234L, restoredData.getApplications().get(0).getCreatedAtMillis());
        assertEquals(5678L, restoredData.getApplications().get(0).getExpiresAtMillis());
    }
}