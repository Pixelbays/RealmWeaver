package org.pixelbays.rpg.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.pixelbays.rpg.chat.config.ChatFilterData;

class ChatFilterManagerTest {

    @Test
    void defaultFilter_censorsStandaloneWordsWithoutTouchingEmbeddedWords() {
        ChatFilterManager manager = new ChatFilterManager(new InMemoryPersistence(), List.of("ass"));
        manager.configure(true, false);

        ChatFilterManager.FilterResult result = manager.filterMessage("This class is ass.");

        assertEquals("This class is ***.", result.filteredContent());
        assertEquals(List.of("ass"), result.matchedWords());
        assertEquals("classroom", manager.filterMessage("classroom").filteredContent());
    }

    @Test
    void customFilter_addsAndAppliesNewWords() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        ChatFilterManager manager = new ChatFilterManager(persistence, List.of());
        manager.configure(false, true);

        manager.addCustomWord("spoiler");

        ChatFilterManager.FilterResult result = manager.filterMessage("That is a spoiler.");

        assertEquals("That is a *******.", result.filteredContent());
        assertEquals(List.of("spoiler"), result.matchedWords());
        assertEquals(List.of("spoiler"), persistence.lastSaved.getCustomWords());
    }

    private static final class InMemoryPersistence extends ChatFilterPersistence {

        private ChatFilterData lastSaved;

        @Override
        public ChatFilterData load() {
            return lastSaved;
        }

        @Override
        public void save(ChatFilterData data) {
            this.lastSaved = data;
        }
    }
}