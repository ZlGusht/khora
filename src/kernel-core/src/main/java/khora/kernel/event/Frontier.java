package khora.kernel.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A set of events closed under causes, written as its heads: the heads and everything that caused
 * them ([[Events are the basic unit of khora]]).
 *
 * <p>Provisional: covers the khora log only. External origins, layered surfaces, branches and other
 * deployments are left to later issues ([[Frontiers need more than one position per origin]]).
 *
 * @param heads the events no other event in the frontier was caused by
 */
public record Frontier(SortedSet<EventId> heads) {
    public Frontier {
        Objects.requireNonNull(heads, "heads");
        var sorted = new TreeSet<EventId>();
        sorted.addAll(heads);
        heads = Collections.unmodifiableSortedSet(sorted);
    }

    /** The frontier before anything happened. */
    public static Frontier empty() {
        return new Frontier(new TreeSet<>());
    }

    /**
     * The frontier holding {@code ids} and their causes, reduced to its heads.
     *
     * @throws IllegalArgumentException if an id or one of its causes is not in {@code source}
     */
    public static Frontier of(Collection<EventId> ids, EventSource source) {
        var ancestors = FrontierCheck.closure(ids.stream().flatMap(id -> causesOf(id, source).stream()).toList(),
                source);
        var heads = new TreeSet<EventId>();
        for (var id : ids) {
            if (!ancestors.contains(id)) {
                heads.add(id);
            }
        }
        return new Frontier(heads);
    }

    private static SortedSet<EventId> causesOf(EventId id, EventSource source) {
        return source.get(id).orElseThrow(() -> new IllegalArgumentException("unknown event " + id.value()))
                .causes();
    }
}
