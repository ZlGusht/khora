package khora.kernel.log;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import khora.kernel.event.Event;
import khora.kernel.event.EventId;
import khora.kernel.event.EventSource;
import khora.kernel.event.Identity;

/**
 * Where events are kept: append-only, each at a position. Positions start at 1 and increase by 1.
 * It is the one place events live ([[Surfaces receive and emit events by interest]]).
 *
 * <p>An event is stored once: appending one whose id is already stored, or that appears earlier in
 * the same append, stores nothing and gives the stored position. Its id is a salted hash of its
 * content, so two different events never share one.
 *
 * <p>The log stores; it does not judge. Whether an event's causes are present and its links in its
 * causal past is checked before it is appended ({@code Causality}).
 */
public interface EventLog extends EventSource, AutoCloseable {

    /** The most events one {@link #append} accepts. */
    int MAX_APPEND = 1000;

    /**
     * Appends {@code events} in order, all or none, and returns one position per event.
     *
     * <p>More than {@value #MAX_APPEND} events, an event too large for the log, or a closed log are
     * refused before anything is stored, with {@link EventLogException}. Any other
     * {@link EventLogException} means the outcome is unknown: the events are stored all or none, and
     * appending them again is safe.
     */
    List<Long> append(List<Event> events);

    /** Up to {@code max} events, in position order, from {@code from}. */
    List<Logged> read(long from, int max);

    /** The event with {@code id}, if stored. */
    @Override
    Optional<Event> get(EventId id);

    /** The events about {@code thing}, in position order. */
    List<Event> about(Identity thing);

    /**
     * Sends {@code handler} the events from position {@code from} on, then each new one, one at a
     * time on another thread, until the returned {@link Following} is closed. A handler that throws
     * does not stop it.
     */
    Following follow(long from, Consumer<Logged> handler);

    @Override
    void close();
}
