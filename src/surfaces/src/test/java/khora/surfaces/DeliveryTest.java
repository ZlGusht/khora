package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import khora.events.Emitted;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.EventLog;
import khora.events.log.EventLogException;
import khora.events.log.Following;
import khora.events.log.InMemoryEventLog;
import khora.events.log.Logged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How the kernel delivers events to surfaces: once each, in log order, never their own. */
class DeliveryTest {

    static final Identity NOTE = Identity.named("note");
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    static class Probe implements Surface {
        final String name;
        final List<Event> received = new ArrayList<>();
        final Function<Event, List<Action>> respond;

        Probe(String name, Function<Event, List<Action>> respond) {
            this.name = name;
            this.respond = respond;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Set<Interest> interests() {
            return Set.of(Interest.any());
        }

        @Override
        public List<Action> receive(Event event) {
            received.add(event);
            return respond.apply(event);
        }
    }

    static Action note(String text) {
        return Assertion.action(Assertion.of(Identity.named("p"), NOTE, new Assertion.Literal(new Json.Str(text))));
    }

    static String by(Event e) {
        return ((Emitted) e.body()).by().principal();
    }

    InMemoryEventLog log;
    Kernel kernel;

    @BeforeEach
    void kernel() {
        log = new InMemoryEventLog();
        kernel = new Kernel(log, new Hlc(() -> 1000));
    }

    Event submit(Action action) {
        return assertInstanceOf(Answer.Logged.class, kernel.submit(action, ANNA)).event();
    }

    /** Every event a surface received came after its trigger, if the surface received that too. */
    static void assertAfterTriggers(Probe probe) {
        for (int i = 0; i < probe.received.size(); i++) {
            var trigger = probe.received.get(i).trigger();
            for (int j = i; j < probe.received.size(); j++) {
                assertFalse(trigger.equals(Optional.of(probe.received.get(j).id())),
                        probe.name + " got an event before its trigger");
            }
        }
    }

    @Test
    void anEmissionReachesOthersAfterItsCause() {
        var echo = new Probe("echo", e -> by(e).equals("anna") ? List.of(note("seen")) : List.of());
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(echo, 0);
        kernel.attach(watcher, 0);
        var e = submit(note("hello"));
        assertEquals(2, watcher.received.size());
        assertEquals(e, watcher.received.getFirst());
        assertEquals("echo", by(watcher.received.get(1)));
        assertAfterTriggers(watcher);
    }

    @Test
    void catchingUpKeepsLogOrderAndSkipsOwnEmissions() {
        var v0 = submit(note("v0"));
        submit(note("v1"));
        submit(note("v2"));
        var replier = new Probe("replier", e -> by(e).equals("late") ? List.of(note("reply")) : List.of());
        kernel.attach(replier, kernel.position());
        var late = new Probe("late", e -> e.equals(v0) ? List.of(note("from late")) : List.of());
        kernel.attach(late, 0);
        assertEquals(List.of("anna", "anna", "anna", "replier"), late.received.stream().map(DeliveryTest::by).toList());
        assertAfterTriggers(late);
        assertEquals(List.of("late"), replier.received.stream().map(DeliveryTest::by).toList());
    }

    @Test
    void catchingUpAcrossPagesDeliversEachEventOnce() {
        for (int i = 0; i < 1200; i++) {
            submit(note("n" + i));
        }
        var replier = new Probe("replier", e -> by(e).equals("late") ? List.of(note("reply")) : List.of());
        kernel.attach(replier, kernel.position());
        var late = new Probe("late", e -> by(e).equals("anna") && late(e) ? List.of(note("from late")) : List.of());
        kernel.attach(late, 0);
        var ids = late.received.stream().map(Event::id).toList();
        assertEquals(ids.size(), Set.copyOf(ids).size());
        assertTrue(late.received.stream().noneMatch(e -> by(e).equals("late")));
        assertEquals(1, replier.received.size());
    }

    static boolean late(Event e) {
        return ((Emitted) e.body()).action().args().canonical().contains("\"n0\"");
    }

    @Test
    void aSurfaceThatThrowsDoesNotFailTheSubmit() {
        var failing = new Probe("failing", e -> {
            throw new IllegalStateException("broken surface");
        });
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(failing, 0);
        kernel.attach(watcher, 0);
        var e = submit(note("hello"));
        assertEquals(List.of(e), watcher.received);
    }

    @Test
    void aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit() {
        var broken = new Probe("broken", e -> List.of()) {
            @Override
            public Set<Interest> interests() {
                throw new IllegalStateException("broken interests");
            }
        };
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(broken, 0);
        kernel.attach(watcher, 0);
        var e = submit(note("hello"));
        assertEquals(List.of(e), watcher.received);
        assertEquals(1, log.read(1, 100).size());
    }

    @Test
    void anEmissionThatIsRefusedDoesNotFailTheSubmit() {
        var orders = Identity.named("orders/place");
        kernel = new Kernel(log, new Hlc(() -> 1000), new Gate() {
            @Override
            public String name() {
                return "orders";
            }

            @Override
            public boolean guards(Action action) {
                return action.definition().equals(orders);
            }

            @Override
            public Verdict decide(Action action, khora.events.By by) {
                return new Refuse("no orders");
            }

            @Override
            public void take(Event event) {}
        });
        kernel.attach(new Probe("bad", e -> List.of(new Action(orders, Json.Obj.of(java.util.Map.of())))), 0);
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(watcher, 0);
        var e = submit(note("hello"));
        assertEquals(List.of(e), watcher.received);
    }

    @Test
    void aSurfacesEmissionsAreTriggeredByWhatItReceivedInOrder() {
        var twice = new Probe("twice", e -> by(e).equals("anna") ? List.of(note("first"), note("second")) : List.of());
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(twice, 0);
        kernel.attach(watcher, 0);
        submit(note("hello"));
        var first = watcher.received.get(1);
        var second = watcher.received.get(2);
        assertEquals("twice", by(first));
        assertEquals("twice", by(second));
        assertEquals(first.trigger(), second.trigger());
        assertTrue(first.recorded() < second.recorded());
    }

    /** A log whose next appends fail, after storing the events or before. */
    static final class FlakyLog implements EventLog {
        final InMemoryEventLog inner = new InMemoryEventLog();
        int failures;
        boolean storeBeforeFailing;
        int appends;

        @Override
        public List<Long> append(List<Event> events) {
            appends++;
            if (failures > 0) {
                failures--;
                if (storeBeforeFailing) {
                    inner.append(events);
                }
                throw new EventLogException("timed out");
            }
            return inner.append(events);
        }

        @Override
        public List<Logged> read(long from, int max) {
            return inner.read(from, max);
        }

        @Override
        public Optional<Event> get(EventId id) {
            return inner.get(id);
        }

        @Override
        public List<Event> about(Identity thing) {
            return inner.about(thing);
        }

        @Override
        public Following follow(long from, Consumer<Logged> handler) {
            return inner.follow(from, handler);
        }

        @Override
        public void close() {
            inner.close();
        }
    }

    static List<Event> stored(EventLog log) {
        return log.read(1, 100).stream().map(Logged::event).toList();
    }

    @Test
    void anAppendWithAnUnknownOutcomeIsRetriedWithTheSameEvent() {
        for (boolean storedBeforeFailing : new boolean[] {true, false}) {
            var flaky = new FlakyLog();
            var k = new Kernel(flaky, new Hlc(() -> 1000));
            flaky.failures = 1;
            flaky.storeBeforeFailing = storedBeforeFailing;
            var e = assertInstanceOf(Answer.Logged.class, k.submit(note("once"), ANNA)).event();
            assertEquals(List.of(e), stored(flaky));
            assertEquals(2, flaky.appends);
        }
    }

    @Test
    void anAppendThatKeepsFailingCarriesTheEvent() {
        var flaky = new FlakyLog();
        var k = new Kernel(flaky, new Hlc(() -> 1000));
        flaky.failures = Kernel.APPEND_ATTEMPTS;
        flaky.storeBeforeFailing = true;
        var failed = assertThrows(Kernel.Unconfirmed.class, () -> k.submit(note("once"), ANNA));
        assertEquals(List.of(failed.event()), stored(flaky));
        flaky.append(List.of(failed.event()));
        assertEquals(List.of(failed.event()), stored(flaky));
    }

    @Test
    void surfacesEchoingEachOtherStop() {
        kernel.attach(new Probe("ping", e -> List.of(note("ping"))), 0);
        kernel.attach(new Probe("pong", e -> List.of(note("pong"))), 0);
        submit(note("start"));
        var stored = log.read(1, 100_000).size();
        assertTrue(stored <= 2 * Kernel.MAX_GENERATIONS + 1, "stored " + stored);
    }

    @Test
    void aKernelOnAFilledLogKnowsItsPosition() {
        submit(note("one"));
        submit(note("two"));
        assertEquals(2, new Kernel(log, new Hlc(() -> 1000)).position());
    }
}
