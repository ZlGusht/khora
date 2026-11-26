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

    static Event naming(long recorded, Identity named, EventId... causes) {
        return Event.create(List.of(causes), recorded, Optional.empty(), new AcceptedAction(BY,
                Assertion.action(Assertion.retraction(named))));
    }

    static Event referring(long recorded, Identity named, EventId... causes) {
        return Event.create(List.of(causes), recorded, Optional.empty(), new AcceptedAction(BY,
                Assertion.action(Assertion.of(Identity.named("p"), Identity.named("see"), new Assertion.Ref(named)))));
    }

    @Test
    void aStatementOutsideTheCausalPastIsRefused() {
        var log = new InMemoryEventLog();
        var e1 = event(1000);
        var e2 = event(1001);
        log.append(List.of(e1, e2));
        var statement = Identity.of(e1.id(), 0);
        assertEquals(Optional.empty(), Causality.check(naming(1002, statement, e1.id()), log));
        assertEquals(Optional.of(Causality.Flaw.STATEMENT_OUTSIDE_PAST), Causality.check(naming(1002, statement), log));
        assertEquals(Optional.of(Causality.Flaw.STATEMENT_OUTSIDE_PAST),
                Causality.check(naming(1002, statement, e2.id()), log));
        assertEquals(Optional.of(Causality.Flaw.STATEMENT_OUTSIDE_PAST),
                Causality.check(referring(1002, statement, e2.id()), log));
        assertEquals(Optional.empty(), Causality.check(referring(1002, statement, e1.id()), log));
    }

    @Test
    void aStatementTheEventDoesNotHoldIsRefused() {
        var log = new InMemoryEventLog();
        var e1 = event(1000);
        log.append(List.of(e1));
        assertEquals(Optional.of(Causality.Flaw.UNKNOWN_STATEMENT),
                Causality.check(naming(1001, Identity.of(e1.id(), 1), e1.id()), log));
        assertEquals(Optional.of(Causality.Flaw.UNKNOWN_STATEMENT),
                Causality.check(referring(1001, Identity.of(e1.id(), 7), e1.id()), log));
    }

    @Test
    void namedIdentitiesNeedNoPast() {
        assertEquals(Optional.empty(), Causality.check(naming(1000, Identity.named("p")), new InMemoryEventLog()));
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
