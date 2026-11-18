package khora.kernel.log.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import khora.kernel.log.LogRecord;
import khora.kernel.log.LogStore;
import khora.kernel.log.LogStoreContract;
import khora.kernel.log.LogStoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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

    @Override
    @Test
    @Disabled("Task 4")
    protected void subscribeDeliversExistingThenNewRecords() {
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
    void openRefusesAnExistingStreamWithOtherSettings() throws Exception {
        NatsTestServer server = newServer();
        try (Connection connection = Nats.connect(server.url())) {
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name("KHORA_LOG")
                    .subjects("khora.log.>")
                    .storageType(StorageType.Memory)
                    .build());
        }
        assertThrows(LogStoreException.class, () -> NatsLogStore.open(server.url()));
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
}
