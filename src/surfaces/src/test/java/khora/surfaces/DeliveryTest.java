package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Event;
import khora.events.Frontier;
import khora.events.FrontierCheck;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How the kernel delivers events to surfaces: once each, after their causes, never their own. */
class DeliveryTest {

    static final Identity NOTE = Identity.named("note");
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    static final class Probe implements Surface {
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
        return ((AcceptedAction) e.body()).by().principal();
    }

    InMemoryEventLog log;
    Kernel kernel;

    @BeforeEach
    void kernel() {
        log = new InMemoryEventLog();
        kernel = new Kernel(new Projects(), log, new Hlc(() -> 1000));
    }

    Event submit(Action action) {
        return assertInstanceOf(Answer.Accepted.class, kernel.submit(action, ANNA, kernel.frontier(), Map.of())).event();
    }

    /** Every event a surface received came after all of its causes that the surface also received. */
    static void assertAfterCauses(Probe probe) {
        var seen = new java.util.HashSet<khora.events.EventId>();
        for (var e : probe.received) {
            for (var earlier : probe.received) {
                if (earlier == e) {
                    break;
                }
                seen.add(earlier.id());
            }
            for (var other : probe.received) {
                if (other != e && e.causes().contains(other.id())) {
                    assertTrue(seen.contains(other.id()), probe.name + " got " + e.id() + " before its cause " + other.id());
                }
            }
            seen.clear();
        }
    }

    @Test
    void anEmissionReachesOthersAfterItsCause() {
        var echo = new Probe("echo", e -> by(e).equals("anna") ? List.of(note("seen")) : List.of());
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(echo, Frontier.empty());
        kernel.attach(watcher, Frontier.empty());
        var e = submit(note("hello"));
        assertEquals(2, watcher.received.size());
        assertEquals(e, watcher.received.getFirst());
        assertEquals("echo", by(watcher.received.get(1)));
        assertAfterCauses(watcher);
    }

    @Test
    void catchingUpKeepsLogOrderAndSkipsOwnEmissions() {
        var v0 = submit(note("v0"));
        submit(note("v1"));
        submit(note("v2"));
        var replier = new Probe("replier", e -> by(e).equals("late") ? List.of(note("reply")) : List.of());
        kernel.attach(replier, kernel.frontier());
        var late = new Probe("late", e -> e.equals(v0) ? List.of(note("from late")) : List.of());
        kernel.attach(late, Frontier.empty());
        assertEquals(List.of("anna", "anna", "anna", "replier"), late.received.stream().map(DeliveryTest::by).toList());
        assertAfterCauses(late);
        assertEquals(List.of("late"), replier.received.stream().map(DeliveryTest::by).toList());
    }

    @Test
    void catchingUpAcrossPagesDeliversEachEventOnce() {
        for (int i = 0; i < 1200; i++) {
            submit(note("n" + i));
        }
        var replier = new Probe("replier", e -> by(e).equals("late") ? List.of(note("reply")) : List.of());
        kernel.attach(replier, kernel.frontier());
        var late = new Probe("late", e -> by(e).equals("anna") && late(e) ? List.of(note("from late")) : List.of());
        kernel.attach(late, Frontier.empty());
        var ids = late.received.stream().map(Event::id).toList();
        assertEquals(ids.size(), Set.copyOf(ids).size());
        assertTrue(late.received.stream().noneMatch(e -> by(e).equals("late")));
        assertEquals(1, replier.received.size());
    }

    static boolean late(Event e) {
        return ((AcceptedAction) e.body()).action().args().canonical().contains("\"n0\"");
    }

    @Test
    void aSurfaceThatThrowsDoesNotFailTheSubmit() {
        var failing = new Probe("failing", e -> {
            throw new IllegalStateException("broken surface");
        });
        var watcher = new Probe("watcher", e -> List.of());
        kernel.attach(failing, Frontier.empty());
        kernel.attach(watcher, Frontier.empty());
        var e = submit(note("hello"));
        assertEquals(List.of(e), watcher.received);
    }

    @Test
    void surfacesEchoingEachOtherStop() {
        kernel.attach(new Probe("ping", e -> List.of(note("ping"))), Frontier.empty());
        kernel.attach(new Probe("pong", e -> List.of(note("pong"))), Frontier.empty());
        submit(note("start"));
        var stored = log.read(1, 100_000).size();
        assertTrue(stored <= 2 * Kernel.MAX_GENERATIONS + 1, "stored " + stored);
    }

    @Test
    void aKernelOnAFilledLogKnowsItsHeads() {
        var e = submit(note("one"));
        var f = submit(note("two"));
        var again = new Kernel(new Projects(), log, new Hlc(() -> 1000));
        assertEquals(kernel.frontier(), again.frontier());
        assertFalse(again.frontier().heads().isEmpty());
        assertTrue(FrontierCheck.closure(again.frontier().heads(), log).containsAll(Set.of(e.id(), f.id())));
    }
}
