package org.pixelbays.rpg.party.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.server.core.Message;

class PartyCommandUtilTest {

    @Test
    void managerResultMessage_nullOrEmpty_returnsUnknownError() {
        assertEquals("pixelbays.rpg.common.unknownError", PartyCommandUtil.managerResultMessage(null).getMessageId());
        assertEquals("pixelbays.rpg.common.unknownError", PartyCommandUtil.managerResultMessage("").getMessageId());
    }

    @Test
    void managerResultMessage_knownMessage_mapsToExpectedKey() {
        Message msg = PartyCommandUtil.managerResultMessage("Invite sent.");
        assertEquals("pixelbays.rpg.party.success.inviteSent", msg.getMessageId());
    }

    @Test
    void managerResultMessage_createdParty_setsTypeParam() {
        Message msg = PartyCommandUtil.managerResultMessage("Created party.");
        assertEquals("pixelbays.rpg.party.success.created", msg.getMessageId());

        Map<String, ParamValue> params = msg.getFormattedMessage().params;
        assertNotNull(params);
        assertTrue(params.get("type") instanceof StringParamValue);
        assertEquals("party", ((StringParamValue) params.get("type")).value);
    }

    @Test
    void managerResultMessage_createdRaid_setsTypeParam() {
        Message msg = PartyCommandUtil.managerResultMessage("Created raid.");
        assertEquals("pixelbays.rpg.party.success.created", msg.getMessageId());

        Map<String, ParamValue> params = msg.getFormattedMessage().params;
        assertNotNull(params);
        assertTrue(params.get("type") instanceof StringParamValue);
        assertEquals("raid", ((StringParamValue) params.get("type")).value);
    }

    @Test
    void managerResultMessage_unmapped_mapsToUnmappedMessageWithTextParam() {
        String original = "Some new manager message";
        Message msg = PartyCommandUtil.managerResultMessage(original);
        assertEquals("pixelbays.rpg.common.unmappedMessage", msg.getMessageId());

        Map<String, ParamValue> params = msg.getFormattedMessage().params;
        assertNotNull(params);
        assertTrue(params.get("text") instanceof StringParamValue);
        assertEquals(original, ((StringParamValue) params.get("text")).value);
    }
}
