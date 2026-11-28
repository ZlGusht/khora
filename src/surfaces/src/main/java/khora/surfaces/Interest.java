package khora.surfaces;

import java.util.Objects;
import java.util.Set;
import khora.events.About;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.EventSource;
import khora.events.Identity;

/**
 * Which events a surface receives: what they are, and what they are about
 * ([[Surfaces receive and emit events by interest]]). An empty set means any.
 *
 * <p>What an event is about is {@link About#of}.
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

    /** Whether {@code event} is one this interest wants, given the events of {@code source}. */
    public boolean matches(Event event, EventSource source) {
        if (!(event.body() instanceof Emitted accepted)) {
            return false;
        }
        if (!definitions.isEmpty() && !definitions.contains(accepted.action().definition())) {
            return false;
        }
        if (about.isEmpty()) {
            return true;
        }
        return About.of(event, source).stream().anyMatch(about::contains);
    }
}
