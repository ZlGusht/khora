package khora.kernel.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Assertion;
import khora.kernel.event.By;
import khora.kernel.event.Event;
import khora.kernel.event.EventId;
import khora.kernel.event.Identity;
import khora.kernel.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What every event log must do. A test class for a log extends this and opens one. */
public abstract class EventLogContract {

    protected EventLog log;

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity NAME = Identity.named("name");

    /** A fresh, empty log. */
    protected abstract EventLog open();

    @BeforeEach
    void openLog() {
        log = open();
    }

    @AfterEach
    void closeLog() {
        log.close();
    }

    /** An event that names {@code thing}; each call makes a new event. */
    protected static Event naming(Identity thing, String name) {
        return Event.create(List.of(), 1000, Optional.empty(), new AcceptedAction(BY,
                Assertion.action(Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name))))));
    }

    protected static Event event() {
        return naming(Identity.named("p"), "Kitchen");
    }

    static List<EventId> ids(List<Logged> logged) {
        return logged.stream().map(l -> l.event().id()).toList();
    }

    @Test
    void appendAssignsIncreasingPositions() {
        assertEquals(List.of(1L, 2L, 3L), log.append(List.of(event(), event(), event())));
        assertEquals(List.of(4L), log.append(List.of(event())));
    }

    @Test
    void readReturnsEventsFromAPosition() {
        var events = List.of(event(), event(), event(), event(), event());
        log.append(events);
        var from3 = log.read(3, 10);
        assertEquals(List.of(events.get(2).id(), events.get(3).id(), events.get(4).id()), ids(from3));
        assertEquals(List.of(3L, 4L, 5L), from3.stream().map(Logged::position).toList());
        assertEquals(2, log.read(3, 2).size());
    }

    @Test
    void anEventReadsBackEqual() {
        var e = event();
        log.append(List.of(e));
        assertEquals(e, log.read(1, 1).getFirst().event());
        assertEquals(Optional.of(e), log.get(e.id()));
    }

    @Test
    void getOfAnUnknownIdIsEmpty() {
        assertEquals(Optional.empty(), log.get(event().id()));
    }

    @Test
    void aboutReturnsTheEventsAboutAThing() {
        var a = Identity.named("a");
        var b = Identity.named("b");
        var e1 = naming(a, "one");
        var e2 = naming(b, "two");
        var e3 = naming(a, "three");
        log.append(List.of(e1, e2, e3));
        assertEquals(List.of(e1, e3), log.about(a));
        assertEquals(List.of(e2), log.about(b));
        assertTrue(log.about(Identity.named("x")).isEmpty());
    }

    @Test
    void appendOfTooManyEventsIsRefused() {
        var batch = new ArrayList<Event>();
        for (int i = 0; i <= EventLog.MAX_APPEND; i++) {
            batch.add(event());
        }
        assertThrows(EventLogException.class, () -> log.append(batch));
        assertTrue(log.read(1, 2000).isEmpty());
    }

    @Test
    void emptyAppendStoresNothing() {
        assertEquals(List.of(), log.append(List.of()));
        assertTrue(log.read(1, 10).isEmpty());
    }

    @Test
    void anEventIsStoredOnce() {
        var e = event();
        assertEquals(List.of(1L), log.append(List.of(e)));
        assertEquals(List.of(1L), log.append(List.of(e)));
        assertEquals(List.of(1L, 1L), log.append(List.of(e, e)));
        assertEquals(List.of(e.id()), ids(log.read(1, 10)));
    }

    @Test
    void appendAfterCloseIsRefused() {
        log.close();
        assertThrows(EventLogException.class, () -> log.append(List.of(event())));
    }

    @Test
    void followDeliversExistingThenNewEvents() throws Exception {
        var e1 = event();
        var e2 = event();
        var e3 = event();
        log.append(List.of(e1, e2));
        var received = Collections.synchronizedList(new ArrayList<Logged>());
        var following = log.follow(2, received::add);
        awaitCount(received, 1);
        log.append(List.of(e3));
        awaitCount(received, 2);
        following.close();
        log.append(List.of(event()));
        Thread.sleep(200);
        assertEquals(List.of(e2.id(), e3.id()), ids(new ArrayList<>(received)));
    }

    @Test
    void aHandlerThatThrowsDoesNotStopFollowing() throws Exception {
        var received = Collections.synchronizedList(new ArrayList<Logged>());
        try (var following = log.follow(1, logged -> {
            received.add(logged);
            throw new IllegalStateException("handler fails");
        })) {
            log.append(List.of(event()));
            log.append(List.of(event()));
            awaitCount(received, 2);
            assertEquals(2, received.size(), following.toString());
        }
    }

    private static void awaitCount(List<Logged> received, int count) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (received.size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(received.size() >= count, "expected " + count + " events within 5 seconds, got " + received.size());
    }
}
