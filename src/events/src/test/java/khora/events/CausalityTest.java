package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.log.InMemoryEventLog;
import org.junit.jupiter.api.Test;

class CausalityTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");

    static Event event(long recorded, EventId... causes) {
        return Event.create(List.of(causes), recorded, Optional.empty(),
                new AcceptedAction(BY, Assertion.action(Assertion.entity())));
    }

    static Event linked(long recorded, EventId cause, EventId basedOn) {
        return Event.create(List.of(cause), recorded, Optional.empty(), new AcceptedAction(BY,
                Assertion.action(Assertion.entity()), Map.of("based_on", Set.of(basedOn))));
    }

    @Test
    void anEventWithKnownEarlierCausesIsSound() {
        var log = new InMemoryEventLog();
        var e1 = event(1000);
        log.append(List.of(e1));
        assertEquals(Optional.empty(), Causality.check(event(1001, e1.id()), log));
        assertEquals(Optional.empty(), Causality.check(event(1000), log));
    }

    @Test
    void aMissingCauseIsRefused() {
        var e1 = event(1000);
        assertEquals(Optional.of(Causality.Flaw.MISSING_CAUSE), Causality.check(event(1001, e1.id()), new InMemoryEventLog()));
    }

    @Test
    void aTimeNotAfterTheCausesIsRefused() {
        var log = new InMemoryEventLog();
        var e1 = event(1000);
        log.append(List.of(e1));
        assertEquals(Optional.of(Causality.Flaw.NOT_AFTER_CAUSES), Causality.check(event(1000, e1.id()), log));
    }

    @Test
    void aLinkOutsideTheCausalPastIsRefused() {
        var log = new InMemoryEventLog();
        var e1 = event(1000);
        var e2 = event(1001, e1.id());
        var e3 = event(1002, e1.id());
        log.append(List.of(e1, e2, e3));
        assertEquals(Optional.empty(), Causality.check(linked(1003, e2.id(), e1.id()), log));
        assertEquals(Optional.of(Causality.Flaw.LINK_OUTSIDE_PAST), Causality.check(linked(1003, e3.id(), e2.id()), log));
    }
}
