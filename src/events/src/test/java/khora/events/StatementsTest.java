package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import khora.events.json.Json;
import org.junit.jupiter.api.Test;

class StatementsTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity NAME = Identity.named("name");

    static Event event(long recorded, Assertion... tuples) {
        return Event.create(Optional.empty(), recorded, Optional.empty(), new Emitted(BY, Assertion.action(tuples)));
    }

    static Assertion named(Identity thing, String name) {
        return Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name)));
    }

    @Test
    void aRenameShowsAfterTheEventsThatHoldIt() {
        var e = event(1000, Assertion.entity(), named(Identity.local(0), "Kitchen"));
        var project = Identity.of(e.id(), 0);
        var kitchen = Identity.of(e.id(), 1);
        var f = event(1001, Assertion.retraction(kitchen), named(project, "Studio"));

        assertEquals(List.of(new Statement(project, Assertion.entity()), new Statement(kitchen, named(project, "Kitchen"))),
                Statements.about(project, List.of(e)));
        assertEquals(List.of(new Statement(project, Assertion.entity()),
                new Statement(Identity.of(f.id(), 1), named(project, "Studio"))),
                Statements.about(project, List.of(e, f)));
    }

    @Test
    void ofGivesEveryStatementThatStands() {
        var e = event(1000, Assertion.entity(), named(Identity.local(0), "Kitchen"));
        var project = Identity.of(e.id(), 0);
        var kitchen = Identity.of(e.id(), 1);
        var f = event(1001, Assertion.retraction(kitchen), named(project, "Studio"));
        assertEquals(List.of(new Statement(project, Assertion.entity()),
                new Statement(Identity.of(f.id(), 0), Assertion.retraction(kitchen)),
                new Statement(Identity.of(f.id(), 1), named(project, "Studio"))),
                Statements.of(List.of(e, f)));
        assertEquals(List.of(), Statements.of(List.of()));
    }

    @Test
    void twoReplacementsOfOneStatementBothStand() {
        var e = event(1000, Assertion.entity(), named(Identity.local(0), "Kitchen"));
        var project = Identity.of(e.id(), 0);
        var kitchen = Identity.of(e.id(), 1);
        var ivan = event(1001, Assertion.retraction(kitchen), named(project, "Studio"));
        var mia = event(1002, Assertion.retraction(kitchen), named(project, "Atelier"));
        assertEquals(List.of("Studio", "Atelier"), names(Statements.about(project, List.of(e, ivan, mia))));
    }

    @Test
    void unrelatedEventsDoNotShow() {
        var e = event(1000, Assertion.entity());
        var g = event(1001, named(Identity.named("invoice"), "INV-1"));
        assertEquals(List.of(new Statement(Identity.of(e.id(), 0), Assertion.entity())),
                Statements.about(Identity.of(e.id(), 0), List.of(e, g)));
    }

    @Test
    void statementsAreInTheOrderOfTheEvents() {
        var a = event(1001, named(NAME, "first"));
        var b = event(1000, named(NAME, "second"));
        assertEquals(List.of("first", "second"), names(Statements.about(NAME, List.of(a, b))));
    }

    static List<String> names(List<Statement> statements) {
        return statements.stream()
                .filter(s -> s.assertion().attribute().isPresent() && !s.assertion().attribute().get().equals(Basic.RETRACTED))
                .map(s -> ((Json.Str) ((Assertion.Literal) s.assertion().value().orElseThrow()).value()).value())
                .toList();
    }
}
