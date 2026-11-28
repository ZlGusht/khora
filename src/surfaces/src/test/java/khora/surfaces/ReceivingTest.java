package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import khora.events.Emitted;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
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

    InMemoryEventLog log;
    Kernel kernel;
    Event created;
    Identity project;

    static Action name(Identity thing, String name) {
        return Assertion.action(Assertion.of(thing, NAME, new Assertion.Literal(new Json.Str(name))));
    }

    Event submit(Action action) {
        return assertInstanceOf(Answer.Logged.class, kernel.submit(action, ANNA)).event();
    }

    @BeforeEach
    void createAProject() {
        log = new InMemoryEventLog();
        kernel = new Kernel(log, new Hlc(() -> 1000));
        created = submit(Assertion.action(Assertion.entity()));
        project = Identity.of(created.id(), 0);
    }

    @Test
    void anAttachedSurfaceCatchesUpThenReceivesWhatMatches() {
        var recorder = new Recorder(Set.of(Interest.about(project)), e -> List.of());
        kernel.attach(recorder, 0);
        var renamed = submit(name(project, "Kitchen"));
        submit(name(Identity.named("invoice"), "INV-1"));
        assertEquals(List.of(created, renamed), recorder.received);
    }

    @Test
    void attachingFromAPositionSkipsWhatIsBeforeIt() {
        var recorder = new Recorder(Set.of(Interest.any()), e -> List.of());
        kernel.attach(recorder, 1);
        var renamed = submit(name(project, "Kitchen"));
        assertEquals(List.of(renamed), recorder.received);
    }

    @Test
    void readsAreReceivedToo() {
        var recorder = new Recorder(Set.of(new Interest(Set.of(Basic.READ), Set.of())), e -> List.of());
        kernel.attach(recorder, kernel.position());
        var read = submit(Basic.read(project));
        assertEquals(List.of(read), recorder.received);
    }

    @Test
    void whatASurfaceEmitsIsAnEventTriggeredByWhatItReceived() {
        var labeller = new Recorder(Set.of(new Interest(Set.of(Basic.ASSERT), Set.of(project))), e ->
                Assertion.in(((Emitted) e.body()).action()).orElseThrow().stream()
                        .filter(t -> t.attribute().equals(Optional.of(NAME)))
                        .map(t -> Assertion.action(Assertion.of(project, Basic.LABEL, t.value().orElseThrow())))
                        .toList());
        kernel.attach(labeller, kernel.position());
        var renamed = submit(name(project, "Kitchen"));

        var emitted = log.read(3, 1).getFirst().event();
        var body = (Emitted) emitted.body();
        assertEquals(Optional.of(renamed.id()), emitted.trigger());
        assertEquals("recorder", body.by().principal());
        assertEquals(List.of(renamed), labeller.received);
    }
}
