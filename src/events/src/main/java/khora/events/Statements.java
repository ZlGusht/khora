package khora.events;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * What holds about a thing at a frontier: the thing's own assertion and the assertions about it, in
 * the events the frontier holds, without retracted ones. Values are worked out from events, never
 * stored ([[Reads return statements and writes follow a read]]).
 *
 * <p>Provisional: walks the whole frontier on each call; no attribute definitions, so every value
 * stands until it is retracted.
 */
public final class Statements {
    private Statements() {}

    /**
     * The statements about {@code thing} at {@code at}, in recorded order.
     *
     * @throws IllegalArgumentException if {@code source} lacks an event of the frontier
     */
    public static List<Statement> about(Identity thing, Frontier at, EventSource source) {
        return at(at, source).stream()
                .filter(s -> s.id().equals(thing) || s.assertion().identity().filter(thing::equals).isPresent())
                .toList();
    }

    /**
     * Every statement that holds at {@code at}, in recorded order.
     *
     * @throws IllegalArgumentException if {@code source} lacks an event of the frontier
     */
    public static List<Statement> at(Frontier at, EventSource source) {
        var events = new ArrayList<Event>();
        for (var id : FrontierCheck.closure(at.heads(), source)) {
            events.add(source.get(id).orElseThrow());
        }
        events.sort(Comparator.comparingLong(Event::recorded).thenComparing(Event::id));
        var all = new ArrayList<Statement>();
        var retracted = new HashSet<Identity>();
        for (var event : events) {
            if (!(event.body() instanceof AcceptedAction accepted)) {
                continue;
            }
            var tuples = Assertion.in(accepted.action()).orElse(List.of());
            for (int i = 0; i < tuples.size(); i++) {
                var tuple = tuples.get(i).in(event.id());
                if (tuple.attribute().filter(Basic.RETRACTED::equals).isPresent()) {
                    tuple.identity().ifPresent(retracted::add);
                }
                all.add(new Statement(Identity.of(event.id(), i), tuple));
            }
        }
        return all.stream().filter(s -> !retracted.contains(s.id())).toList();
    }
}
