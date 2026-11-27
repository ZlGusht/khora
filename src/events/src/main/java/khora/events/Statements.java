package khora.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * One reading of events: the statements that stand, in the order the events came, without the ones
 * retracted ([[Reads return statements and writes follow a read]]).
 *
 * <p>Exploration: this is an interpretation, one shape among the ones surfaces may make, and the
 * reference for the datastores. Another surface may read the same events otherwise: keep both of two
 * replacements of one statement, or honour only its own retractions.
 */
public final class Statements {
    private Statements() {}

    /** The statements about {@code thing} that stand after {@code events}, in their order. */
    public static List<Statement> about(Identity thing, Iterable<Event> events) {
        return of(events).stream()
                .filter(s -> s.id().equals(thing) || s.assertion().identity().filter(thing::equals).isPresent())
                .toList();
    }

    /** Every statement that stands after {@code events}, in their order. */
    public static List<Statement> of(Iterable<Event> events) {
        var all = new ArrayList<Statement>();
        var retracted = new HashSet<Identity>();
        for (var event : events) {
            if (!(event.body() instanceof Emitted emitted)) {
                continue;
            }
            var tuples = Assertion.in(emitted.action()).orElse(List.of());
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
