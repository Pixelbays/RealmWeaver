package org.pixelbays.rpg.guild.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.server.core.Message;

class GuildCommandUtilTest {

    @Test
    void managerResultMessage_nullOrEmpty_returnsUnknownError() {
        assertEquals("pixelbays.rpg.common.unknownError", GuildCommandUtil.managerResultMessage(null).getMessageId());
        assertEquals("pixelbays.rpg.common.unknownError", GuildCommandUtil.managerResultMessage("").getMessageId());
    }

    @Test
    void managerResultMessage_knownMessage_mapsToExpectedKey() {
        Message msg = GuildCommandUtil.managerResultMessage("Guilds are disabled.");
        assertEquals("pixelbays.rpg.guild.error.disabled", msg.getMessageId());
    }

    @Test
    void managerResultMessage_newInviteMessages_mapToExpectedKeys() {
        assertEquals(
                "pixelbays.rpg.guild.error.noPendingInvite",
                GuildCommandUtil.managerResultMessage("You do not have a pending guild invite.").getMessageId());
        assertEquals(
                "pixelbays.rpg.guild.success.inviteDeclined",
                GuildCommandUtil.managerResultMessage("Guild invite declined.").getMessageId());
    }

    @Test
    void managerResultMessage_createdGuild_extractsNameParam() {
        Message msg = GuildCommandUtil.managerResultMessage("Created guild Knights of Pixelbays.");
        assertEquals("pixelbays.rpg.guild.success.created", msg.getMessageId());

        Map<String, ParamValue> params = msg.getFormattedMessage().params;
        assertNotNull(params);
        assertTrue(params.get("name") instanceof StringParamValue);
        assertEquals("Knights of Pixelbays", ((StringParamValue) params.get("name")).value);
    }

    @Test
    void managerResultMessage_unmapped_mapsToUnmappedMessageWithTextParam() {
        String original = "Some new guild manager message";
        Message msg = GuildCommandUtil.managerResultMessage(original);
        assertEquals("pixelbays.rpg.common.unmappedMessage", msg.getMessageId());

        Map<String, ParamValue> params = msg.getFormattedMessage().params;
        assertNotNull(params);
        assertTrue(params.get("text") instanceof StringParamValue);
        assertEquals(original, ((StringParamValue) params.get("text")).value);
    }
}
