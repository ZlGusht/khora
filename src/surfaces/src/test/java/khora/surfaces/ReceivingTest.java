package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.Frontier;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceivingTest {

    static final Identity NAME = Identity.named("name");
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    /** Records what it receives, and emits what {@code respond} makes of it. */
    static final class Recorder implements Surface {
        final List<Event> received = new ArrayList<>();
        final Set<Interest> interests;
        final Function<Event, List<Action>> respond;

        Recorder(Set<Interest> interests, Function<Event, List<Action>> respond) {
            this.interests = interests;
            this.respond = respond;
        }

        @Override
        public String name() {
            return "recorder";
        }

        @Override
        public Set<Interest> interests() {
            return interests;
        }

        @Override
        public List<Action> receive(Event event) {
            received.add(event);
            return respond.apply(event);
        }
    }

    Kernel kernel;
    Event created;
    Identity project;

    static Action name(Identity thing, String name) {
        return Assertion.action(Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name))));
    }

    Event submit(Action action) {
        return assertInstanceOf(Answer.Accepted.class, kernel.submit(action, ANNA, kernel.frontier(), Map.of())).event();
    }

    @BeforeEach
    void createAProject() {
        kernel = new Kernel(new Projects(), new InMemoryEventLog(), new Hlc(() -> 1000));
        created = submit(Assertion.action(Assertion.entity()));
        project = Identity.of(created.id(), 0);
    }

    @Test
    void anAttachedSurfaceCatchesUpThenReceivesWhatMatches() {
        var recorder = new Recorder(Set.of(Interest.about(project)), e -> List.of());
        kernel.attach(recorder, Frontier.empty());
        var renamed = submit(name(project, "Kitchen"));
        submit(name(Identity.named("invoice"), "INV-1"));
        assertEquals(List.of(created, renamed), recorder.received);
    }

    @Test
    void attachingFromAFrontierSkipsWhatIsInIt() {
        var recorder = new Recorder(Set.of(Interest.any()), e -> List.of());
        kernel.attach(recorder, new Frontier(new TreeSet<>(Set.of(created.id()))));
        var renamed = submit(name(project, "Kitchen"));
        assertEquals(List.of(renamed), recorder.received);
    }

    @Test
    void readsAreReceivedToo() {
        var recorder = new Recorder(Set.of(new Interest(Set.of(Basic.READ), Set.of())), e -> List.of());
        kernel.attach(recorder, kernel.frontier());
        var read = submit(Basic.read(project));
        assertEquals(List.of(read), recorder.received);
    }

    @Test
    void whatASurfaceEmitsIsAnEventCausedByWhatItKnew() {
        var labeller = new Recorder(Set.of(new Interest(Set.of(Basic.ASSERT), Set.of(project))), e ->
                Assertion.in(((AcceptedAction) e.body()).action()).orElseThrow().stream()
                        .filter(t -> t.attribute().equals(Optional.of(NAME)))
                        .map(t -> Assertion.action(Assertion.of(project, Basic.LABEL, t.value().orElseThrow())))
                        .toList());
        kernel.attach(labeller, kernel.frontier());
        var renamed = submit(name(project, "Kitchen"));

        var label = kernel.statements(project, kernel.frontier()).stream()
                .filter(s -> s.assertion().attribute().equals(Optional.of(Basic.LABEL))).findFirst().orElseThrow();
        var emitted = kernel.event(label.id()).orElseThrow();
        var body = (AcceptedAction) emitted.body();
        assertTrue(emitted.causes().contains(renamed.id()));
        assertEquals(Map.of("source", Set.of(renamed.id())), body.links());
        assertEquals("recorder", body.by().principal());
        assertEquals(List.of(renamed), labeller.received);
    }
}
