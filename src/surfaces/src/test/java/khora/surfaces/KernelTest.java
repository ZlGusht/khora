package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.stub.Entities;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.Test;

/** Ivan and Mia rename a project: with a gate on names, and without one. */
class KernelTest {

    static final Identity NAME = Projects.NAME;
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);
    static final Context IVAN = new Context("ivan", Optional.empty(), "g3", 10);
    static final Context MIA = new Context("mia", Optional.empty(), "g3", 10);

    Kernel kernel;
    Event created;
    Identity project;

    static Assertion named(Identity thing, String name) {
        return Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name)));
    }

    static Answer.Logged logged(Answer answer) {
        return assertInstanceOf(Answer.Logged.class, answer);
    }

    void start(Gate... gates) {
        kernel = new Kernel(new InMemoryEventLog(), new Hlc(() -> 1000), gates);
        kernel.attach(new Entities(), 0);
        created = logged(kernel.submit(Assertion.action(Assertion.entity(), named(Identity.local(0), "Kitchen")), ANNA)).event();
        project = Identity.of(created.id(), 0);
    }

    /** What {@code who} reads of the project: the statements the entities surface answers with. */
    List<Statement> read(Context who) {
        var answer = logged(kernel.submit(Basic.read(project), who));
        var response = answer.responses().getFirst();
        assertEquals(Optional.of(answer.event().id()), response.trigger());
        return Basic.answerOf(((Emitted) response.body()).action()).orElseThrow().stream()
                .map(id -> kernel.statement(id).orElseThrow()).toList();
    }

    static Statement name(List<Statement> read) {
        return read.stream().filter(s -> s.assertion().attribute().equals(Optional.of(NAME))).findFirst().orElseThrow();
    }

    static List<String> names(List<Statement> read) {
        return read.stream().filter(s -> s.assertion().attribute().equals(Optional.of(NAME)))
                .map(s -> ((Json.Str) ((Assertion.Literal) s.assertion().value().orElseThrow()).value()).value()).toList();
    }

    Answer rename(Context who, Statement current, String name) {
        return kernel.submit(Assertion.action(Assertion.retraction(current.id()), named(project, name)), who);
    }

    @Test
    void anEventIsAFactByItsEmitter() {
        start();
        var body = (Emitted) created.body();
        assertEquals(ANNA.by(Kernel.VIA), body.by());
        assertEquals(Optional.empty(), created.trigger());
        assertEquals(1, kernel.position());
    }

    @Test
    void aReadIsAnEventAndASurfaceAnswersIt() {
        start();
        assertEquals(List.of("Kitchen"), names(read(IVAN)));
    }

    @Test
    void withoutAGateBothRenamesStandAndTheSurfaceShowsBoth() {
        start();
        var ivanSaw = name(read(IVAN));
        var miaSaw = name(read(MIA));
        logged(rename(IVAN, ivanSaw, "Studio"));
        logged(rename(MIA, miaSaw, "Atelier"));
        assertEquals(List.of("Studio", "Atelier"), names(read(ANNA)));
    }

    @Test
    void withAGateTheSecondRenameOfOneNameIsRefused() {
        start(new Projects());
        var ivanSaw = name(read(IVAN));
        var miaSaw = name(read(MIA));
        var ivan = logged(rename(IVAN, ivanSaw, "Studio")).event();
        assertEquals("projects", ((Emitted) ivan.body()).by().via());
        var refused = assertInstanceOf(Answer.Refused.class, rename(MIA, miaSaw, "Atelier"));
        assertTrue(refused.reason().contains(ivan.id().value()), refused.reason());
        assertEquals(List.of("Studio"), names(read(ANNA)));
    }

    @Test
    void aRenameAfterSeeingTheOtherPasses() {
        start(new Projects());
        logged(rename(IVAN, name(read(IVAN)), "Studio"));
        logged(rename(MIA, name(read(MIA)), "Atelier"));
        assertEquals(List.of("Atelier"), names(read(ANNA)));
    }

    @Test
    void aBlindWritePassesTheGate() {
        start(new Projects());
        logged(rename(IVAN, name(read(IVAN)), "Studio"));
        logged(kernel.submit(Assertion.action(named(project, "Atelier")), MIA));
        assertEquals(List.of("Studio", "Atelier"), names(read(ANNA)));
    }

    @Test
    void theGateRefusesReplacingAStatementItDoesNotKnow() {
        start(new Projects());
        var stray = new Statement(Identity.of(created.id(), 5), named(project, "?"));
        assertInstanceOf(Answer.Refused.class, rename(MIA, stray, "Atelier"));
    }

    @Test
    void anActionNoGateGuardsIsLogged() {
        start(new Projects());
        var other = new Action(Identity.named("orders/place"), Json.Obj.of(java.util.Map.of()));
        assertEquals(Kernel.VIA, ((Emitted) logged(kernel.submit(other, ANNA)).event().body()).by().via());
    }

    @Test
    void aGateCatchesUpWithTheLogItStartsOn() {
        start();
        var log = new InMemoryEventLog();
        var k = new Kernel(log, new Hlc(() -> 1000));
        var e = logged(k.submit(Assertion.action(Assertion.entity(), named(Identity.local(0), "Kitchen")), ANNA)).event();
        var kitchen = new Statement(Identity.of(e.id(), 1), named(Identity.of(e.id(), 0), "Kitchen"));
        logged(k.submit(Assertion.action(Assertion.retraction(kitchen.id()), named(Identity.of(e.id(), 0), "Studio")), IVAN));
        var gated = new Kernel(log, new Hlc(() -> 1000), new Projects());
        assertInstanceOf(Answer.Refused.class, gated.submit(
                Assertion.action(Assertion.retraction(kitchen.id()), named(Identity.of(e.id(), 0), "Atelier")), MIA));
    }

    @Test
    void anUnknownTriggerIsRefused() {
        start();
        assertInstanceOf(Answer.Refused.class,
                kernel.submit(Basic.read(project), IVAN, Optional.of(EventId.hashOf("nowhere"))));
    }

    @Test
    void aTriggerFarAheadOfWallTimeIsRefusedAndTheKernelGoesOn() {
        var log = new InMemoryEventLog();
        var future = Event.create(Optional.empty(), Long.MAX_VALUE - 1, Optional.empty(),
                new Emitted(ANNA.by(Kernel.VIA), Assertion.action(Assertion.entity())));
        log.append(List.of(future));
        var k = new Kernel(log, new Hlc(() -> 1000));
        assertInstanceOf(Answer.Refused.class,
                k.submit(Assertion.action(Assertion.entity()), ANNA, Optional.of(future.id())));
        logged(k.submit(Assertion.action(Assertion.entity()), ANNA));
    }
}
