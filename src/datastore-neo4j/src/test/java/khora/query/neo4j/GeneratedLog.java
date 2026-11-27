package khora.query.neo4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import khora.events.Emitted;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.By;
import khora.events.Event;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.EventLog;
import khora.events.log.InMemoryEventLog;

/**
 * A log of random events from a seed: things brought into being, names, sizes and owners stated,
 * statements retracted, and branches. Every literal value is asserted once, since the graph
 * datastore keeps a thing's values of an attribute as a set.
 */
final class GeneratedLog {
    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity NAME = Identity.named("name");
    static final Identity SIZE = Identity.named("size");
    static final Identity OWNER = Identity.named("owner");

    final EventLog log = new InMemoryEventLog();
    final List<Event> events = new ArrayList<>();
    final List<Identity> things = new ArrayList<>();
    private final List<Identity> statements = new ArrayList<>();
    private final Random random;

    GeneratedLog(long seed, int count) {
        random = new Random(seed);
        for (int i = 0; i < count; i++) {
            append(i);
        }
    }

    private void append(int n) {
        var tuples = new ArrayList<Assertion>();
        int size = 1 + random.nextInt(4);
        for (int i = 0; i < size; i++) {
            int kind = random.nextInt(things.isEmpty() ? 1 : 6);
            switch (kind) {
                case 0 -> {
                    tuples.add(Assertion.entity());
                    tuples.add(Assertion.of(Identity.local(tuples.size() - 1), NAME,
                            new Literal(new Json.Str("thing " + n + "." + i))));
                }
                case 1 -> tuples.add(Assertion.of(thing(), SIZE, new Literal(new Json.Int(10L * n + i - 500))));
                case 2 -> tuples.add(Assertion.of(thing(), OWNER, new Ref(thing())));
                case 3 -> tuples.add(Assertion.of(thing(), NAME, new Literal(new Json.Str("renamed " + n + "." + i))));
                default -> {
                    if (!statements.isEmpty()) {
                        tuples.add(Assertion.retraction(statements.get(random.nextInt(statements.size()))));
                    } else {
                        tuples.add(Assertion.entity());
                    }
                }
            }
        }
        var event = Event.create(Optional.empty(), 1000 + n, Optional.empty(), new Emitted(BY, Assertion.action(tuples)));
        log.append(List.of(event));
        events.add(event);
        for (int i = 0; i < tuples.size(); i++) {
            var id = Identity.of(event.id(), i);
            statements.add(id);
            if (tuples.get(i).equals(Assertion.entity())) {
                things.add(id);
            }
        }
    }

    private Identity thing() {
        return things.get(random.nextInt(things.size()));
    }

}
