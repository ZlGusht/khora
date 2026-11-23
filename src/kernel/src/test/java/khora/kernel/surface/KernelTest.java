package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Action;
import khora.kernel.event.Assertion;
import khora.kernel.event.Basic;
import khora.kernel.event.Event;
import khora.kernel.event.EventId;
import khora.kernel.event.Frontier;
import khora.kernel.event.Hlc;
import khora.kernel.event.Identity;
import khora.kernel.event.Statement;
import khora.kernel.json.Json;
import khora.kernel.stub.InMemoryProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KernelTest {

    static final Identity NAME = Identity.named("name");
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);
    static final Context IVAN = new Context("ivan", Optional.empty(), "g3", 10);
    static final Context MIA = new Context("mia", Optional.empty(), "g3", 10);

    Kernel kernel;
    Event created;
    Identity project;

    static Assertion named(Identity thing, String name) {
        return Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name)));
    }

    static Frontier at(Event e) {
        return new Frontier(new TreeSet<>(Set.of(e.id())));
    }

    static Event accepted(Answer answer) {
        return assertInstanceOf(Answer.Accepted.class, answer).event();
    }

    Event read(Context who, Frontier knew) {
        return accepted(kernel.submit(Basic.read(project), who, knew, Map.of()));
    }

    Answer rename(Context who, Event basedOn, String name) {
        var current = kernel.statements(project, at(basedOn)).stream()
                .filter(s -> s.assertion().attribute().equals(Optional.of(NAME))).findFirst().orElseThrow();
        return kernel.submit(Assertion.action(Assertion.retraction(current.id()), named(project, name)),
                who, at(basedOn), Map.of("based_on", Set.of(basedOn.id())));
    }

    @BeforeEach
    void createAProject() {
        kernel = new Kernel(new InMemoryProjects(), new Hlc(() -> 1000));
        created = accepted(kernel.submit(Assertion.action(Assertion.entity(), named(Identity.local(0), "Kitchen")),
                ANNA, Frontier.empty(), Map.of()));
        project = Identity.of(created.id(), 0);
    }

    @Test
    void anAcceptedActionIsAnEventByItsCaller() {
        var body = (AcceptedAction) created.body();
        assertEquals(ANNA.by("projects"), body.by());
        assertEquals(created.id(), kernel.frontier().heads().first());
    }

    @Test
    void aReadIsAnEventAndAnswersWithStatements() {
        var answer = kernel.submit(Basic.read(project), IVAN, at(created), Map.of());
        var accepted = assertInstanceOf(Answer.Accepted.class, answer);
        var body = (AcceptedAction) accepted.event().body();
        assertEquals(Basic.READ, body.action().definition());
        assertEquals(Map.of("at", Set.of(created.id())), body.links());
        assertEquals(List.of(new Statement(project, Assertion.entity()),
                new Statement(Identity.of(created.id(), 1), named(project, "Kitchen"))), accepted.statements());
    }

    @Test
    void aWriteFollowsTheReadItWasBasedOn() {
        var r = read(IVAN, at(created));
        var f = accepted(rename(IVAN, r, "Studio"));
        assertTrue(f.causes().contains(r.id()));
        assertEquals("Studio", nameAt(at(f)));
    }

    @Test
    void aConcurrentRenameOfTheSameNameIsRejected() {
        var ivanRead = read(IVAN, at(created));
        var miaRead = read(MIA, at(created));
        accepted(rename(IVAN, ivanRead, "Studio"));
        assertInstanceOf(Answer.Rejected.class, rename(MIA, miaRead, "Atelier"));
    }

    @Test
    void anUnrelatedWriteDoesNotConflict() {
        var r = read(IVAN, at(created));
        accepted(kernel.submit(Assertion.action(named(Identity.named("invoice"), "INV-1")),
                new Context("bob", Optional.empty(), "g3", 10), Frontier.empty(), Map.of()));
        accepted(rename(IVAN, r, "Studio"));
    }

    @Test
    void aBlindWriteIsAccepted() {
        var r = read(IVAN, at(created));
        accepted(rename(IVAN, r, "Studio"));
        accepted(kernel.submit(Assertion.action(named(project, "Atelier")), MIA, Frontier.empty(), Map.of()));
    }

    @Test
    void anUnknownFrontierIsRejected() {
        var stranger = new Frontier(new TreeSet<>(Set.of(EventId.hashOf("nowhere"))));
        assertInstanceOf(Answer.Rejected.class, kernel.submit(Basic.read(project), IVAN, stranger, Map.of()));
    }

    @Test
    void actionsTheAuthorityDoesNotTakeAreRejected() {
        var other = new Action(Identity.named("orders/place"), Json.Obj.of(Map.of()));
        assertInstanceOf(Answer.Rejected.class, kernel.submit(other, ANNA, kernel.frontier(), Map.of()));
    }

    String nameAt(Frontier f) {
        return kernel.statements(project, f).stream()
                .filter(s -> s.assertion().attribute().equals(Optional.of(NAME)))
                .map(s -> ((Json.Str) ((Assertion.Literal) s.assertion().value().orElseThrow()).value()).value())
                .findFirst().orElseThrow();
    }
}
