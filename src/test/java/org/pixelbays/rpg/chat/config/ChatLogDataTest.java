package org.pixelbays.rpg.chat.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ChatLogDataTest {

    @Test
    void chatLogEntryCodec_initializesWithoutStaticOrderFailure() {
        assertDoesNotThrow(() -> ChatLogData.ChatLogEntryData.CODEC);
    }

    @Test
    void chatLogEntry_create_preservesMatchedWords() {
        ChatLogData.ChatLogEntryData entry = ChatLogData.ChatLogEntryData.create(
                System.currentTimeMillis(),
                "global",
                "Tester",
                "Hero",
                "bad word",
                "*** ****",
                List.of("bad", "word", "bad"),
                3);

        assertEquals(List.of("bad", "word"), entry.getMatchedWords());
    }

    @Test
    void chatLogData_create_setsAccountId() {
        UUID accountId = UUID.randomUUID();

        ChatLogData data = ChatLogData.create(accountId);

        assertEquals(accountId, data.getAccountId());
    }
}