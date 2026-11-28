package khora.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import khora.events.Event;
import khora.events.Assertion;
import khora.events.Assertion.Value;
import khora.events.Identity;
import khora.events.Statements;
import khora.events.log.EventLog;

/**
 * A datastore that keeps nothing of its own: it works every answer out from the events of a log,
 * as {@link Statements} does. It is the reference other datastores are checked against.
 *
 * <p>Provisional: reads the log up to the query's position on each answer.
 */
public final class EventsDatastore implements Datastore {
    private static final int PAGE = 1000;

    private final EventLog log;

    public EventsDatastore(EventLog log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public Answer answer(Query query) {
        var events = new ArrayList<Event>();
        long end = query.at().orElse(Long.MAX_VALUE);
        long last = 0;
        for (long from = 1; from <= end; ) {
            var page = log.read(from, (int) Math.min(PAGE, end - from + 1));
            if (page.isEmpty()) {
                break;
            }
            page.forEach(l -> events.add(l.event()));
            last = page.getLast().position();
            from = last + 1;
        }
        if (query.at().isPresent() && last < end) {
            throw new IllegalArgumentException("position " + end + " is past the log's end, " + last);
        }
        return Evaluator.answer(query, last, graph(events));
    }

    private Graph graph(List<Event> events) {
        var things = new ArrayList<Identity>();
        var byAttribute = new HashMap<Identity, List<Graph.Fact>>();
        var byThing = new HashMap<Identity, Map<Identity, List<Value>>>();
        for (var s : Statements.of(events)) {
            var tuple = s.assertion();
            if (tuple.equals(Assertion.entity())) {
                things.add(s.id());
            } else if (tuple.identity().isPresent() && tuple.attribute().isPresent() && tuple.value().isPresent()) {
                var thing = tuple.identity().get();
                var attribute = tuple.attribute().get();
                var value = tuple.value().get();
                byAttribute.computeIfAbsent(attribute, k -> new ArrayList<>()).add(new Graph.Fact(thing, value));
                byThing.computeIfAbsent(thing, k -> new HashMap<>()).computeIfAbsent(attribute, k -> new ArrayList<>())
                        .add(value);
            }
        }
        return new Graph() {
            @Override
            public List<Identity> things() {
                return things;
            }

            @Override
            public List<Fact> with(Identity attribute) {
                return byAttribute.getOrDefault(attribute, List.of());
            }

            @Override
            public List<Value> values(Identity thing, Identity attribute) {
                return byThing.getOrDefault(thing, Map.of()).getOrDefault(attribute, List.of());
            }
        };
    }
}
