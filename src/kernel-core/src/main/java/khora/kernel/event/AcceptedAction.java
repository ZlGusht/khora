package khora.kernel.event;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An event's body when khora accepted an action: who acted, the action, and its named links.
 *
 * <p>Links name how this event relates to others beyond knowing of them: what a write was based on,
 * what a read read at, what a translation came from. Every linked event is in the event's causal
 * past ([[An event's causes are what it knew]]); {@link EventGraph} refuses one that is not.
 *
 * <p>Provisional: link names are free text until each kind of action defines its own.
 *
 * @param by who acted
 * @param action what was done
 * @param links link name to the events it points to; each set is non-empty
 */
public record AcceptedAction(By by, Action action, Map<String, Set<EventId>> links) implements Body {
    public AcceptedAction {
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(links, "links");
        var copy = new TreeMap<String, Set<EventId>>();
        links.forEach((name, ids) -> {
            Objects.requireNonNull(name, "link name");
            var sorted = new TreeSet<EventId>();
            sorted.addAll(ids);
            if (sorted.isEmpty()) {
                throw new IllegalArgumentException("link " + name + " points to no event");
            }
            copy.put(name, Collections.unmodifiableSortedSet(sorted));
        });
        links = Collections.unmodifiableSortedMap(copy);
    }

    @Override
    public String kind() {
        return "accepted_action";
    }

    /** An accepted action without links. */
    public AcceptedAction(By by, Action action) {
        this(by, action, Map.of());
    }
}
