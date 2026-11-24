package khora.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import khora.events.Assertion;
import khora.events.Assertion.Value;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.Statements;
import khora.events.log.EventLog;

/**
 * A datastore that keeps nothing of its own: it works every answer out from the events of a log,
 * as {@link Statements} does. It is the reference other datastores are checked against.
 *
 * <p>Provisional: reads every event of the frontier on each answer, and the whole log to find its
 * heads when a query names no frontier.
 */
public final class EventsDatastore implements Datastore {
    private static final int PAGE = 1000;

    private final EventLog log;

    public EventsDatastore(EventLog log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public Answer answer(Query query) {
        var at = query.at().orElseGet(this::heads);
        return Evaluator.answer(query, at, graph(at));
    }

    private Frontier heads() {
        var ids = new ArrayList<EventId>();
        for (long from = 1;; from += PAGE) {
            var page = log.read(from, PAGE);
            page.forEach(l -> ids.add(l.event().id()));
            if (page.size() < PAGE) {
                return Frontier.of(ids, log);
            }
        }
    }

    private Graph graph(Frontier at) {
        var things = new ArrayList<Identity>();
        var byAttribute = new HashMap<Identity, List<Graph.Fact>>();
        var byThing = new HashMap<Identity, Map<Identity, List<Value>>>();
        for (var s : Statements.at(at, log)) {
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
