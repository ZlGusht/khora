package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventGraphTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");

    static Event event(long recorded, EventId... causes) {
        return Event.create(List.of(causes), recorded, Optional.empty(), new AcceptedAction(BY, Assertion.action(Assertion.entity())));
    }

    @Test
    void appendsEventWithKnownCauses() {
        var graph = new EventGraph();
        var e1 = event(1000);
        var e2 = event(1001, e1.id());
        assertEquals(new AppendResult.Appended(e1.id()), graph.append(e1));
        assertEquals(new AppendResult.Appended(e2.id()), graph.append(e2));
        assertEquals(Optional.of(e2), graph.get(e2.id()));
    }

    @Test
    void refusesMissingCause() {
        var graph = new EventGraph();
        var e1 = event(1000);
        var e2 = event(1001, e1.id());
        assertEquals(new AppendResult.Refused(e2.id(), AppendResult.Reason.MISSING_CAUSE), graph.append(e2));
        assertFalse(graph.get(e2.id()).isPresent());
    }

    @Test
    void refusesTimeNotAfterCauses() {
        var graph = new EventGraph();
        var e1 = event(1000);
        graph.append(e1);
        var e2 = event(1000, e1.id());
        assertEquals(new AppendResult.Refused(e2.id(), AppendResult.Reason.NOT_AFTER_CAUSES), graph.append(e2));
        assertFalse(graph.get(e2.id()).isPresent());
    }

    @Test
    void refusesDuplicate() {
        var graph = new EventGraph();
        var e1 = event(1000);
        assertTrue(graph.append(e1) instanceof AppendResult.Appended);
        assertEquals(new AppendResult.Refused(e1.id(), AppendResult.Reason.DUPLICATE), graph.append(e1));
    }

    @Test
    void refusesALinkOutsideTheCausalPast() {
        var graph = new EventGraph();
        var e1 = event(1000);
        var e2 = event(1001, e1.id());
        graph.append(e1);
        graph.append(e2);
        var inPast = Event.create(List.of(e2.id()), 1002, Optional.empty(),
                new AcceptedAction(BY, Assertion.action(Assertion.entity()), Map.of("based_on", Set.of(e1.id()))));
        assertEquals(new AppendResult.Appended(inPast.id()), graph.append(inPast));
        var e3 = event(1003, e1.id());
        graph.append(e3);
        var outside = Event.create(List.of(e3.id()), 1004, Optional.empty(),
                new AcceptedAction(BY, Assertion.action(Assertion.entity()), Map.of("based_on", Set.of(e2.id()))));
        assertEquals(new AppendResult.Refused(outside.id(), AppendResult.Reason.LINK_OUTSIDE_PAST), graph.append(outside));
    }

    @Test
    void givesEventsInAppendOrderAndItsHeads() {
        var graph = new EventGraph();
        var e1 = event(1000);
        var e2 = event(1001, e1.id());
        var e3 = event(1002, e1.id());
        for (var e : List.of(e1, e2, e3)) {
            graph.append(e);
        }
        assertEquals(List.of(e1, e2, e3), graph.events());
        assertEquals(new java.util.TreeSet<>(Set.of(e2.id(), e3.id())), graph.heads().heads());
    }
}
