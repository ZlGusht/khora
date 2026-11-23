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
import io.nats.client.impl.Headers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import khora.kernel.event.Event;
import khora.kernel.event.EventId;
import khora.kernel.event.Identity;
import khora.kernel.log.EventLog;
import khora.kernel.log.EventLogContract;
import khora.kernel.log.EventLogException;
import khora.kernel.log.Following;
import khora.kernel.log.Logged;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NatsEventLogTest extends EventLogContract {

    private final List<NatsTestServer> servers = new ArrayList<>();

    @Override
    protected EventLog open() {
        return NatsEventLog.open(newServer().url());
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

    private static List<EventId> ids(List<Logged> logged) {
        return logged.stream().map(l -> l.event().id()).toList();
    }

    @Test
    void oversizedEventStoresNothing() {
        var big = naming(Identity.named("a"), "x".repeat(2 * 1024 * 1024));
        var failure = assertThrows(EventLogException.class, () -> log.append(List.of(event(), big)));
        assertTrue(failure.getMessage().contains(big.id().value()), failure.getMessage());
        assertTrue(log.read(1, 10).isEmpty());
    }

    @Test
    void duplicateAcrossAppendsInsideALargerBatchKeepsItsPosition() {
        var e1 = event();
        var e2 = event();
        var e3 = event();
        var e4 = event();
        log.append(List.of(e1, e2));
        assertEquals(List.of(3L, 2L, 4L, 3L), log.append(List.of(e3, e2, e4, e3)));
        assertEquals(List.of(e1.id(), e2.id(), e3.id(), e4.id()), ids(log.read(1, 10)));
    }

    @Test
    void openRefusesAnExistingStreamWithOtherStorage() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsEventLog.streamConfiguration())
                .storageType(StorageType.Memory).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherRetention() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsEventLog.streamConfiguration())
                .retentionPolicy(RetentionPolicy.Interest).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherDuplicateWindow() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsEventLog.streamConfiguration())
                .duplicateWindow(Duration.ofSeconds(10)).build());
    }

    @Test
    void openRefusesAnExistingStreamWithOtherDiscardPolicy() throws Exception {
        assertOpenRefuses(new StreamConfiguration.Builder(NatsEventLog.streamConfiguration())
                .discardPolicy(DiscardPolicy.New).build());
    }

    private void assertOpenRefuses(StreamConfiguration existing) throws Exception {
        NatsTestServer server = newServer();
        try (Connection connection = Nats.connect(server.url())) {
            connection.jetStreamManagement().addStream(existing);
        }
        assertThrows(EventLogException.class, () -> NatsEventLog.open(server.url()));
    }

    @Test
    void readAndAboutManyEvents() {
        int perThing = 1500;
        var even = Identity.named("even");
        var odd = Identity.named("odd");
        var all = new ArrayList<Event>();
        var batch = new ArrayList<Event>();
        for (int i = 0; i < 2 * perThing; i++) {
            var e = naming(i % 2 == 0 ? even : odd, "n" + i);
            all.add(e);
            batch.add(e);
            if (batch.size() == EventLog.MAX_APPEND) {
                log.append(batch);
                batch.clear();
            }
        }
        log.append(batch);
        var aboutEven = log.about(even);
        assertEquals(perThing, aboutEven.size());
        for (int i = 0; i < perThing; i++) {
            assertEquals(all.get(2 * i), aboutEven.get(i));
        }
        var page = log.read(2001, 1500);
        assertEquals(1000, page.size());
        assertEquals(2001L, page.getFirst().position());
        assertEquals(3000L, page.getLast().position());
        assertTrue(log.read(3001, 10).isEmpty());
    }

    @Test
    void openAcceptsAnExistingStreamWithTheLogSettings() {
        NatsTestServer server = newServer();
        var e = event();
        try (EventLog first = NatsEventLog.open(server.url())) {
            first.append(List.of(e));
        }
        try (EventLog second = NatsEventLog.open(server.url())) {
            assertEquals(List.of(e.id()), ids(second.read(1, 10)));
        }
    }

    @Test
    void eventsSurviveARestart() {
        NatsTestServer server = newServer();
        var e1 = event();
        var e2 = event();
        var e3 = event();
        try (EventLog first = NatsEventLog.open(server.url())) {
            first.append(List.of(e1, e2));
            first.append(List.of(e3));
        }
        // SIGKILL stops the server process but leaves the host's page cache intact, so this proves
        // that positions survive an ungraceful stop, not that writes were fsynced. Durability rests
        // on the sync_always check NatsTestServer makes against /varz on every start.
        server.kill();
        server.start();
        try (EventLog second = NatsEventLog.open(server.url())) {
            var events = second.read(1, 10);
            assertEquals(List.of(e1.id(), e2.id(), e3.id()), ids(events));
            assertEquals(List.of(1L, 2L, 3L), events.stream().map(Logged::position).toList());
            assertEquals(java.util.Optional.of(e2), second.get(e2.id()));
        }
    }

    @Test
    void unreachableServerFailsFast() {
        NatsTestServer server = newServer();
        try (EventLog unreachable = NatsEventLog.open(server.url())) {
            server.stop();
            long started = System.nanoTime();
            assertThrows(EventLogException.class, () -> unreachable.append(List.of(event())));
            Duration took = Duration.ofNanos(System.nanoTime() - started);
            assertTrue(took.compareTo(Duration.ofSeconds(5)) < 0, "append failed after " + took);
        }
    }

    @Test
    void appendToAPausedServerFailsWithinFiveSeconds() {
        NatsTestServer server = newServer();
        try (EventLog paused = NatsEventLog.open(server.url())) {
            server.pause();
            try {
                long started = System.nanoTime();
                assertThrows(EventLogException.class, () -> paused.append(List.of(event())));
                Duration took = Duration.ofNanos(System.nanoTime() - started);
                assertTrue(took.compareTo(Duration.ofSeconds(5)) < 0, "append failed after " + took);
            } finally {
                server.unpause();
            }
        }
    }

    @Test
    void anUndecodableEventStopsFollowingVisibly() throws Exception {
        NatsTestServer server = newServer();
        List<java.util.logging.LogRecord> logged = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(NatsEventLog.class.getName());
        logger.addHandler(capture);
        var e1 = event();
        try (EventLog log = NatsEventLog.open(server.url());
                Connection raw = Nats.connect(server.url())) {
            log.append(List.of(e1));
            raw.jetStream().publish(Subjects.PREFIX + "bad", new Headers().put("Nats-Msg-Id", "bad"), new byte[] {2});
            log.append(List.of(event()));

            List<EventId> received = new CopyOnWriteArrayList<>();
            try (Following following = log.follow(1, l -> received.add(l.event().id()))) {
                long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
                while (logged.stream().noneMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue())
                        && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
                Thread.sleep(300);
                assertEquals(List.of(e1.id()), received);
            }
            java.util.logging.LogRecord warning = logged.stream()
                    .filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue())
                    .findFirst().orElseThrow(() -> new AssertionError("no warning logged"));
            assertTrue(warning.getMessage().contains("position 2"), warning.getMessage());

            var readFailure = assertThrows(RuntimeException.class, () -> log.read(1, 10));
            assertTrue(readFailure.getMessage() != null, String.valueOf(readFailure));
        } finally {
            logger.removeHandler(capture);
        }
    }
}
