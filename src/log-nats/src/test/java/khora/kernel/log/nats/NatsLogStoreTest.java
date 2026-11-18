package khora.kernel.log.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import khora.kernel.log.LogRecord;
import khora.kernel.log.LogStore;
import khora.kernel.log.LogStoreContract;
import khora.kernel.log.LogStoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NatsLogStoreTest extends LogStoreContract {

    private final List<NatsTestServer> servers = new ArrayList<>();

    @Override
    protected LogStore newStore() {
        return NatsLogStore.open(newServer().url());
    }

    private NatsTestServer newServer() {
        NatsTestServer server = new NatsTestServer().start();
        servers.add(server);
        return server;
    }

    @AfterEach
    void stopServers() {
        servers.forEach(NatsTestServer::close);
    }

    private static LogRecord record(String eventId, String entity, byte[] body) {
        return LogRecord.unappended(eventId, "effect", List.of(entity), body);
    }

    @Test
    void historyFindsEntitiesWithSpecialCharacters() {
        List<String> entities = List.of("a.b", "a*", "a>", "a b", "кухня");
        List<LogRecord> batch = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            batch.add(record("e" + i, entities.get(i), ("body" + i).getBytes(StandardCharsets.UTF_8)));
        }
        store.append(batch);
        for (int i = 0; i < entities.size(); i++) {
            List<LogRecord> history = store.history(entities.get(i));
            assertEquals(List.of(batch.get(i).withPosition(i + 1)), history, entities.get(i));
        }
    }

    @Test
    void oversizedBodyStoresNothing() {
        List<LogRecord> batch = List.of(
                record("e1", "a", new byte[] {1}),
                record("e2", "a", new byte[2 * 1024 * 1024]));
        LogStoreException failure = assertThrows(LogStoreException.class, () -> store.append(batch));
        assertTrue(failure.getMessage().contains("e2"), failure.getMessage());
        assertTrue(store.read(1, 10).isEmpty());
    }

    @Test
    void duplicateAcrossAppendsInsideALargerBatchKeepsItsPosition() {
        store.append(List.of(record("e1", "a", new byte[] {1}), record("e2", "b", new byte[] {2})));
        assertEquals(List.of(3L, 2L, 4L, 3L), store.append(List.of(
                record("e3", "a", new byte[] {3}),
                record("e2", "b", new byte[] {2}),
                record("e4", "c", new byte[] {4}),
                record("e3", "a", new byte[] {3}))));
        assertEquals(List.of("e1", "e2", "e3", "e4"), store.read(1, 10).stream().map(LogRecord::eventId).toList());
    }

    @Test
    void readReturnsWholeRecords() {
        LogRecord r = LogRecord.unappended("e1", "observation", List.of("x", "кухня", "a.b"), new byte[] {7, 8});
        store.append(List.of(r, LogRecord.unappended("e2", "action", List.of(), new byte[0])));
        assertEquals(List.of(r.withPosition(1), LogRecord.unappended("e2", "action", List.of(), new byte[0]).withPosition(2)),
                store.read(0, 10));
    }

    @Test
    void openRefusesAnExistingStreamWithOtherStorage() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsLogStore.streamConfiguration())
                .storageType(StorageType.Memory).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherRetention() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsLogStore.streamConfiguration())
                .retentionPolicy(RetentionPolicy.Interest).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherDuplicateWindow() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsLogStore.streamConfiguration())
                .duplicateWindow(Duration.ofSeconds(10)).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherDiscardPolicy() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsLogStore.streamConfiguration())
                .discardPolicy(DiscardPolicy.New).build());
    }

    private void assertOpenRefuses(StreamConfiguration existing) throws Exception {
        NatsTestServer server = newServer();
        try (Connection connection = Nats.connect(server.url())) {
            connection.jetStreamManagement().addStream(existing);
        }
        assertThrows(LogStoreException.class, () -> NatsLogStore.open(server.url()));
    }

    @Test
    void readAndHistoryStreamManyRecords() {
        int perEntity = 1500;
        List<LogRecord> batch = new ArrayList<>();
        for (int i = 0; i < 2 * perEntity; i++) {
            batch.add(record("e" + i, i % 2 == 0 ? "even" : "odd", new byte[] {(byte) i}));
            if (batch.size() == LogStore.MAX_APPEND) {
                store.append(batch);
                batch.clear();
            }
        }
        store.append(batch);
        List<LogRecord> even = store.history("even");
        assertEquals(perEntity, even.size());
        for (int i = 0; i < perEntity; i++) {
            assertEquals(2L * i + 1, even.get(i).position());
            assertEquals("e" + 2 * i, even.get(i).eventId());
        }
        List<LogRecord> page = store.read(2001, 1500);
        assertEquals(1000, page.size());
        assertEquals(2001L, page.getFirst().position());
        assertEquals(3000L, page.getLast().position());
        assertTrue(store.read(3001, 10).isEmpty());
    }

    @Test
    void openAcceptsAnExistingStreamWithTheLogSettings() {
        NatsTestServer server = newServer();
        try (LogStore first = NatsLogStore.open(server.url())) {
            first.append(List.of(record("e1", "a", new byte[] {1})));
        }
        try (LogStore second = NatsLogStore.open(server.url())) {
            assertEquals(List.of("e1"), second.read(1, 10).stream().map(LogRecord::eventId).toList());
        }
    }

    @Test
    void recordsSurviveARestart() {
        NatsTestServer server = newServer();
        try (LogStore first = NatsLogStore.open(server.url())) {
            first.append(List.of(record("e1", "a", new byte[] {1}), record("e2", "b", new byte[] {2})));
            first.append(List.of(record("e3", "a", new byte[] {3})));
        }
        server.kill();
        server.start();
        try (LogStore second = NatsLogStore.open(server.url())) {
            List<LogRecord> records = second.read(1, 10);
            assertEquals(List.of("e1", "e2", "e3"), records.stream().map(LogRecord::eventId).toList());
            assertEquals(List.of(1L, 2L, 3L), records.stream().map(LogRecord::position).toList());
        }
    }

    @Test
    void unreachableServerFailsFast() {
        NatsTestServer server = newServer();
        try (LogStore unreachable = NatsLogStore.open(server.url())) {
            server.stop();
            long started = System.nanoTime();
            assertThrows(LogStoreException.class,
                    () -> unreachable.append(List.of(record("e1", "a", new byte[] {1}))));
            Duration took = Duration.ofNanos(System.nanoTime() - started);
            assertTrue(took.compareTo(Duration.ofSeconds(5)) < 0, "append failed after " + took);
        }
    }
}
