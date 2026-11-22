package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventGraphTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");

    static Event event(long recorded, EventId... causes) {
        return Event.create(List.of(causes), recorded, Optional.empty(), BY, Assertion.entity().toAction());
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
}
