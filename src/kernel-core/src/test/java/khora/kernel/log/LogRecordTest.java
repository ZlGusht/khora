package khora.kernel.log;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogRecordTest {

    private static LogRecord record(String eventId, String eventType, List<String> entityIds, byte[] body) {
        return LogRecord.unappended(eventId, eventType, entityIds, body);
    }

    @Test
    void eventIdMustNotBeNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> record(null, "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("  ", "action", List.of("a"), new byte[0]));
    }

    @Test
    void eventIdMustBePrintableAscii() {
        assertThrows(IllegalArgumentException.class, () -> record("кухня", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("eé1", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e\n1", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e\u00001", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e\u007f1", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record(" e1", "action", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e1 ", "action", List.of("a"), new byte[0]));
        assertEquals("a b~!", record("a b~!", "action", List.of("a"), new byte[0]).eventId());
    }

    @Test
    void eventTypeMustBeKnown() {
        assertThrows(IllegalArgumentException.class, () -> record("e1", null, List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e1", "command", List.of("a"), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e1", "Action", List.of("a"), new byte[0]));
        for (String type : List.of("action", "outcome", "effect", "observation")) {
            assertEquals(type, record("e1", type, List.of("a"), new byte[0]).eventType());
        }
    }

    @Test
    void entityIdsMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> record("e1", "action", List.of(""), new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> record("e1", "action", List.of("a", " "), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> record("e1", "action", Arrays.asList("a", null), new byte[0]));
        assertEquals(List.of(), record("e1", "action", List.of(), new byte[0]).entityIds());
    }

    @Test
    void equalityComparesBodyContent() {
        LogRecord one = record("e1", "action", List.of("a"), new byte[] {1, 2});
        LogRecord same = record("e1", "action", List.of("a"), new byte[] {1, 2});
        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, record("e1", "action", List.of("a"), new byte[] {1, 3}));
        assertNotEquals(one, one.withPosition(1));
    }

    @Test
    void bodyIsCopied() {
        byte[] body = {1, 2, 3};
        LogRecord record = record("e1", "action", List.of("a"), body);
        body[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, record.body());
    }
}
