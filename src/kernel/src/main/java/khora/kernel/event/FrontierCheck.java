package khora.kernel.event;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checking, comparing and combining frontiers against the events of a source.
 *
 * <p>Walks are iterative, so long cause chains do not overflow the stack.
 *
 * <p>Provisional: each call walks every event in the frontiers; no index yet.
 */
public final class FrontierCheck {
    private FrontierCheck() {}

    /** Whether a frontier holds every cause of every event in it. */
    public sealed interface Validity permits Valid, Invalid {}

    /** Every event in the frontier, and every cause, is in the source. */
    public record Valid() implements Validity {}

    /** {@code missing} is in the frontier, or is a cause of an event in it, but is not in the source. */
    public record Invalid(EventId missing) implements Validity {
        public Invalid {
            Objects.requireNonNull(missing, "missing");
        }
    }

    /** Whether {@code source} holds every event of {@code frontier}, causes included. */
    public static Validity validate(Frontier frontier, EventSource source) {
        var seen = new HashSet<EventId>();
        var work = new ArrayDeque<EventId>(frontier.heads());
        while (!work.isEmpty()) {
            var id = work.pop();
            if (!seen.add(id)) {
                continue;
            }
            var event = source.get(id);
            if (event.isEmpty()) {
                return new Invalid(id);
            }
            work.addAll(event.get().causes());
        }
        return new Valid();
    }

    /**
     * How {@code a} relates to {@code b}.
     *
     * @throws IllegalArgumentException if either frontier is not valid in {@code source}
     */
    public static Order compare(Frontier a, Frontier b, EventSource source) {
        var left = closure(a.heads(), source);
        var right = closure(b.heads(), source);
        if (left.equals(right)) {
            return Order.EQUAL;
        }
        if (right.containsAll(left)) {
            return Order.BEFORE;
        }
        if (left.containsAll(right)) {
            return Order.AFTER;
        }
        return Order.CONCURRENT;
    }

    /**
     * The frontier holding the events of both.
     *
     * @throws IllegalArgumentException if either frontier is not valid in {@code source}
     */
    public static Frontier combine(Frontier a, Frontier b, EventSource source) {
        var ids = new TreeSet<EventId>(a.heads());
        ids.addAll(b.heads());
        return Frontier.of(ids, source);
    }

    /** {@code ids} and all their causes. */
    static Set<EventId> closure(Collection<EventId> ids, EventSource source) {
        var seen = new HashSet<EventId>();
        var work = new ArrayDeque<EventId>(ids);
        while (!work.isEmpty()) {
            var id = work.pop();
            if (!seen.add(id)) {
                continue;
            }
            var event = source.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("unknown event " + id.value()));
            work.addAll(event.causes());
        }
        return seen;
    }
}
