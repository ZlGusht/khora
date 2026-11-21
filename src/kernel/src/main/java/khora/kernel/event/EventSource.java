package khora.kernel.event;

import java.util.Optional;

/**
 * Where events are looked up by id: the log, or the events a surface has taken in.
 *
 * <p>Provisional: a lookup only, until K2 chooses the log store.
 */
public interface EventSource {
    Optional<Event> get(EventId id);
}
