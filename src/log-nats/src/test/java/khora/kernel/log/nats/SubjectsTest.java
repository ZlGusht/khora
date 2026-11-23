package khora.kernel.log.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import khora.kernel.log.LogRecord;
import org.junit.jupiter.api.Test;

class SubjectsTest {

    private static final String TOKEN = "[A-Za-z0-9_-]+";

    private static LogRecord record(String eventType, String... entities) {
        return LogRecord.unappended("e1", eventType, List.of(entities), new byte[0]);
    }

    private static String base64url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void effectOnAnEntityIsKeyedByTheEncodedEntity() {
        assertEquals("khora.log.effect." + base64url("p:kitchen.ivanovi"),
                Subjects.forRecord(record("effect", "p:kitchen.ivanovi", "other")));
    }

    @Test
    void recordWithoutEntityUsesUnderscore() {
        assertEquals("khora.log.action._", Subjects.forRecord(record("action")));
    }

    @Test
    void specialCharactersGiveOneValidToken() {
        for (String id : List.of("a.b", "a*", "a>", "a b", "кухня")) {
            String subject = Subjects.forRecord(record("action", id));
            String[] tokens = subject.split("\\.", -1);
            assertEquals(4, tokens.length, subject);
            assertTrue(tokens[3].matches(TOKEN), subject);
            assertEquals("khora.log.*." + tokens[3], Subjects.historyFilter(id));
        }
    }
}
