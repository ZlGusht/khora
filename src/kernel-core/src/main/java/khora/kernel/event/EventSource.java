package khora.kernel.event;

import java.util.Optional;

/**
 * Where events are looked up by id: the log, or the events a surface has taken in.
 *
 * <p>An {@code EventLog} is one; so is any set of events a caller holds.
 */
public interface EventSource {
    Optional<Event> get(EventId id);
}
