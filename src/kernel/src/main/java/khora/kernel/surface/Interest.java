package khora.kernel.surface;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Assertion;
import khora.kernel.event.Basic;
import khora.kernel.event.Event;
import khora.kernel.event.Identity;

/**
 * Which events a surface receives: what they are, and what they are about
 * ([[Surfaces receive and emit events by interest]]). An empty set means any.
 *
 * <p>An assert is about the identities its tuples name, the identities they refer to, and the tuples
 * themselves; a read is about the thing it reads.
 *
 * <p>Provisional: no scopes yet, and no access check at delivery.
 *
 * @param definitions the definitions of the actions wanted
 * @param about the things they are to be about
 */
public record Interest(Set<Identity> definitions, Set<Identity> about) {
    public Interest {
        definitions = Set.copyOf(Objects.requireNonNull(definitions, "definitions"));
        about = Set.copyOf(Objects.requireNonNull(about, "about"));
    }

    /** Every accepted event. */
    public static Interest any() {
        return new Interest(Set.of(), Set.of());
    }

    /** Every accepted event about {@code thing}. */
    public static Interest about(Identity thing) {
        return new Interest(Set.of(), Set.of(thing));
    }

    /** Whether {@code event} is one this interest wants. */
    public boolean matches(Event event) {
        if (!(event.body() instanceof AcceptedAction accepted)) {
            return false;
        }
        if (!definitions.isEmpty() && !definitions.contains(accepted.action().definition())) {
            return false;
        }
        if (about.isEmpty()) {
            return true;
        }
        var things = aboutOf(event, accepted);
        things.retainAll(about);
        return !things.isEmpty();
    }

    private static Set<Identity> aboutOf(Event event, AcceptedAction accepted) {
        var things = new HashSet<Identity>();
        Basic.readOf(accepted.action()).ifPresent(things::add);
        Assertion.in(accepted.action()).ifPresent(tuples -> {
            for (int i = 0; i < tuples.size(); i++) {
                var tuple = tuples.get(i);
                things.add(Identity.of(event.id(), i));
                tuple = tuple.in(event.id());
                tuple.identity().ifPresent(things::add);
                tuple.value().ifPresent(v -> {
                    if (v instanceof Assertion.Ref ref) {
                        things.add(ref.id());
                    }
                });
            }
        });
        return things;
    }
}
