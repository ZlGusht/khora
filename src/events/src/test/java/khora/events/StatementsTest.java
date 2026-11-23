package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import org.junit.jupiter.api.Test;

class StatementsTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity NAME = Identity.named("name");

    static Event event(long recorded, List<EventId> causes, Assertion... tuples) {
        return Event.create(causes, recorded, Optional.empty(), new AcceptedAction(BY, Assertion.action(tuples)));
    }

    static Assertion named(Identity thing, String name) {
        return Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name)));
    }

    static Frontier at(Event... events) {
        var ids = new TreeSet<EventId>();
        for (var e : events) {
            ids.add(e.id());
        }
        return new Frontier(ids);
    }

    @Test
    void aRenameShowsByFrontier() {
        var log = new InMemoryEventLog();
        var e = event(1000, List.of(), Assertion.entity(), named(Identity.local(0), "Kitchen"));
        var project = Identity.of(e.id(), 0);
        var kitchen = Identity.of(e.id(), 1);
        var f = event(1001, List.of(e.id()), Assertion.retraction(kitchen), named(project, "Studio"));
        log.append(List.of(e));
        log.append(List.of(f));

        assertEquals(List.of(new Statement(project, Assertion.entity()), new Statement(kitchen, named(project, "Kitchen"))),
                Statements.about(project, at(e), log));
        assertEquals(List.of(new Statement(project, Assertion.entity()),
                new Statement(Identity.of(f.id(), 1), named(project, "Studio"))),
                Statements.about(project, at(f), log));
    }

    @Test
    void unrelatedEventsDoNotShow() {
        var log = new InMemoryEventLog();
        var e = event(1000, List.of(), Assertion.entity());
        var g = event(1001, List.of(), named(Identity.named("invoice"), "INV-1"));
        log.append(List.of(e));
        log.append(List.of(g));
        assertEquals(List.of(new Statement(Identity.of(e.id(), 0), Assertion.entity())),
                Statements.about(Identity.of(e.id(), 0), at(e, g), log));
    }

    @Test
    void anUnknownFrontierIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Statements.about(NAME, at(event(1, List.of())), new InMemoryEventLog()));
    }

    @Test
    void theEmptyFrontierHoldsNothing() {
        assertEquals(List.of(), Statements.about(NAME, Frontier.empty(), new InMemoryEventLog()));
    }

    @Test
    void statementsAreInRecordedOrder() {
        var log = new InMemoryEventLog();
        var a = event(1000, List.of(), named(NAME, "first"));
        var b = event(1001, List.of(a.id()), named(NAME, "second"));
        log.append(List.of(a));
        log.append(List.of(b));
        assertEquals(List.of("first", "second"), Statements.about(NAME, at(b), log).stream()
                .map(s -> ((Json.Str) ((Assertion.Literal) s.assertion().value().orElseThrow()).value()).value())
                .toList());
    }
}
