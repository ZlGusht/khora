package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import khora.kernel.json.Json;
import khora.kernel.log.InMemoryEventLog;
import org.junit.jupiter.api.Test;

class AboutTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity NAME = Identity.named("name");

    static Event event(List<EventId> causes, long recorded, Assertion... tuples) {
        return Event.create(causes, recorded, Optional.empty(), new AcceptedAction(BY, Assertion.action(tuples)));
    }

    @Test
    void aRetractionIsAboutTheThingItChanges() {
        var log = new InMemoryEventLog();
        var e = event(List.of(), 1000, Assertion.entity(),
                Assertion.of(Identity.local(0), NAME, new Assertion.Literal(new Json.Str("Kitchen"))));
        var project = Identity.of(e.id(), 0);
        var f = event(List.of(e.id()), 1001, Assertion.retraction(Identity.of(e.id(), 1)));
        log.append(List.of(e, f));
        assertTrue(About.of(f, log).contains(project));
        assertTrue(log.about(project).contains(f));
    }

    @Test
    void aReferenceIsNotWhatAStatementIsAbout() {
        var log = new InMemoryEventLog();
        var anna = Identity.named("anna");
        var p = Identity.named("p");
        var owns = event(List.of(), 1000, Assertion.of(anna, Identity.named("owns"), new Assertion.Ref(p)));
        log.append(List.of(owns));
        assertTrue(About.of(owns, log).contains(anna));
        assertFalse(About.of(owns, log).contains(p));
    }

    @Test
    void aNewThingIsAboutItself() {
        var e = event(List.of(), 1000, Assertion.entity());
        assertTrue(About.of(e, new InMemoryEventLog()).contains(Identity.of(e.id(), 0)));
    }
}
