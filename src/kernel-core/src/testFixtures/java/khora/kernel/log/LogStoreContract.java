package khora.kernel.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Behaviour every {@link LogStore} implementation must have. */
public abstract class LogStoreContract {

    protected abstract LogStore newStore();

    protected LogStore store;

    @BeforeEach
    void openStore() {
        store = newStore();
    }

    @AfterEach
    void closeStore() {
        store.close();
    }

    private static LogRecord record(String eventId, String... entities) {
        return LogRecord.unappended(
                eventId, "action", List.of(entities), eventId.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> ids(List<LogRecord> records) {
        return records.stream().map(LogRecord::eventId).toList();
    }

    private static List<Long> positions(List<LogRecord> records) {
        return records.stream().map(LogRecord::position).toList();
    }

    @Test
    void appendAssignsIncreasingPositions() {
        assertEquals(List.of(1L, 2L, 3L), store.append(List.of(record("e1"), record("e2"), record("e3"))));
        assertEquals(List.of(4L), store.append(List.of(record("e4"))));
    }

    @Test
    void readReturnsRecordsFromAPosition() {
        store.append(List.of(record("e1"), record("e2"), record("e3"), record("e4"), record("e5")));
        List<LogRecord> from3 = store.read(3, 10);
        assertEquals(List.of("e3", "e4", "e5"), ids(from3));
        assertEquals(List.of(3L, 4L, 5L), positions(from3));
        assertEquals(List.of("e3", "e4"), ids(store.read(3, 2)));
    }

    @Test
    void historyReturnsRecordsByPrimaryEntity() {
        store.append(List.of(record("e1", "a"), record("e2", "b"), record("e3", "a", "b"), record("e4")));
        assertEquals(List.of("e1", "e3"), ids(store.history("a")));
        assertEquals(List.of("e2"), ids(store.history("b")));
        assertTrue(store.history("x").isEmpty());
    }

    @Test
    void appendOfMoreThan1000RecordsIsRefused() {
        List<LogRecord> batch = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            batch.add(record("e" + i));
        }
        assertThrows(LogStoreException.class, () -> store.append(batch));
        assertTrue(store.read(1, 2000).isEmpty());
    }

    @Test
    void emptyAppendStoresNothing() {
        assertEquals(List.of(), store.append(List.of()));
        assertTrue(store.read(1, 10).isEmpty());
    }

    @Test
    void duplicateEventIdIsStoredOnce() {
        assertEquals(List.of(1L), store.append(List.of(record("e1"))));
        assertEquals(List.of(1L), store.append(List.of(record("e1"))));
        assertEquals(List.of("e1"), ids(store.read(1, 10)));
    }

    @Test
    void duplicateEventIdInsideOneBatchIsStoredOnce() {
        assertEquals(List.of(1L, 1L), store.append(List.of(record("e1"), record("e1"))));
        assertEquals(List.of("e1"), ids(store.read(1, 10)));
    }

    @Test
    void subscribeDeliversExistingThenNewRecords() throws Exception {
        store.append(List.of(record("e1"), record("e2")));
        List<LogRecord> received = new CopyOnWriteArrayList<>();
        Subscription subscription = store.subscribe(2, received::add);
        awaitCount(received, 1);
        assertEquals(List.of("e2"), ids(received));

        store.append(List.of(record("e3")));
        awaitCount(received, 2);
        assertEquals(List.of("e2", "e3"), ids(received));

        subscription.close();
        store.append(List.of(record("e4")));
        Thread.sleep(300);
        assertEquals(List.of("e2", "e3"), ids(received));
    }

    private static void awaitCount(List<LogRecord> received, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (received.size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(received.size() >= count, "expected " + count + " records within 5 seconds, got " + received.size());
    }
}
