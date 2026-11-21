package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

class FrontierTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");

    EventGraph graph;
    Event e1;
    Event e2;
    Event e3;
    Event e4;

    static Event event(long recorded, Event... causes) {
        return Event.create(java.util.Arrays.stream(causes).map(Event::id).toList(), recorded, Optional.empty(), BY,
                Basic.entity());
    }

    static Frontier heads(Event... events) {
        var ids = new TreeSet<EventId>();
        for (var e : events) {
            ids.add(e.id());
        }
        return new Frontier(ids);
    }

    @BeforeEach
    void graph() {
        graph = new EventGraph();
        e1 = event(1000);
        e2 = event(1001, e1);
        e3 = event(1002, e2);
        e4 = event(1003, e1);
        for (var e : List.of(e1, e2, e3, e4)) {
            graph.append(e);
        }
    }

    @Test
    void frontierLackingACauseIsRefused() {
        Map<EventId, Event> taken = new HashMap<>(Map.of(e2.id(), e2));
        EventSource source = id -> Optional.ofNullable(taken.get(id));
        assertEquals(new FrontierCheck.Invalid(e1.id()), FrontierCheck.validate(heads(e2), source));
    }

    @Test
    void unknownHeadIsInvalid() {
        var stranger = event(5000);
        assertEquals(new FrontierCheck.Invalid(stranger.id()), FrontierCheck.validate(heads(stranger), graph));
    }

    @Test
    void validFrontierIsValid() {
        assertEquals(new FrontierCheck.Valid(), FrontierCheck.validate(heads(e3, e4), graph));
        assertEquals(new FrontierCheck.Valid(), FrontierCheck.validate(Frontier.empty(), graph));
    }

    @Test
    void ofReducesToHeads() {
        assertEquals(heads(e3), Frontier.of(List.of(e1.id(), e2.id(), e3.id()), graph));
        assertEquals(heads(e3, e4), Frontier.of(List.of(e4.id(), e1.id(), e3.id()), graph));
    }

    @Test
    void ofRefusesUnknownIds() {
        assertThrows(IllegalArgumentException.class, () -> Frontier.of(List.of(event(5000).id()), graph));
    }

    @Test
    void comparesByCausalOrder() {
        assertEquals(Order.BEFORE, FrontierCheck.compare(heads(e2), heads(e3), graph));
        assertEquals(Order.AFTER, FrontierCheck.compare(heads(e3), heads(e2), graph));
        assertEquals(Order.EQUAL, FrontierCheck.compare(heads(e3), heads(e3), graph));
        assertEquals(Order.CONCURRENT, FrontierCheck.compare(heads(e3), heads(e4), graph));
        assertEquals(Order.BEFORE, FrontierCheck.compare(Frontier.empty(), heads(e1), graph));
        assertEquals(Order.EQUAL, FrontierCheck.compare(Frontier.empty(), Frontier.empty(), graph));
    }

    @Test
    void combineKeepsHeads() {
        assertEquals(heads(e3, e4), FrontierCheck.combine(heads(e3), heads(e4), graph));
        assertEquals(heads(e3), FrontierCheck.combine(heads(e2), heads(e3), graph));
        assertEquals(heads(e1), FrontierCheck.combine(Frontier.empty(), heads(e1), graph));
    }

    @Test
    void longChainDoesNotOverflow() {
        var chain = new EventGraph();
        var last = event(1);
        chain.append(last);
        for (int i = 2; i <= 10_000; i++) {
            last = event(i, last);
            chain.append(last);
        }
        assertEquals(new FrontierCheck.Valid(), FrontierCheck.validate(heads(last), chain));
        assertEquals(Order.BEFORE, FrontierCheck.compare(Frontier.empty(), heads(last), chain));
    }

    @Test
    void headsAreCopied() {
        var ids = new TreeSet<>(Set.of(e1.id()));
        var frontier = new Frontier(ids);
        ids.add(e2.id());
        assertEquals(Set.of(e1.id()), frontier.heads());
    }

    @Test
    void headsIgnoreTheCallersComparator() {
        var reversed = new TreeSet<EventId>(Comparator.reverseOrder());
        reversed.addAll(List.of(e3.id(), e4.id()));
        assertEquals(null, new Frontier(reversed).heads().comparator());
        assertEquals(heads(e3, e4), new Frontier(reversed));
    }
}
