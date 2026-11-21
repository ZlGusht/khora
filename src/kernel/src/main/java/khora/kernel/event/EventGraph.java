package khora.kernel.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Events in memory, linked by cause. Every event in it has all its causes in it, recorded earlier.
 *
 * <p>Provisional: an in-memory stand-in for the log until K2 chooses the log store.
 */
public final class EventGraph implements EventSource {
    private final Map<EventId, Event> events = new HashMap<>();

    /** Appends {@code e} if its causes are here and it was recorded after all of them. */
    public synchronized AppendResult append(Event e) {
        Objects.requireNonNull(e, "e");
        if (events.containsKey(e.id())) {
            return new AppendResult.Refused(e.id(), AppendResult.Reason.DUPLICATE);
        }
        for (var causeId : e.causes()) {
            var cause = events.get(causeId);
            if (cause == null) {
                return new AppendResult.Refused(e.id(), AppendResult.Reason.MISSING_CAUSE);
            }
            if (e.recorded() <= cause.recorded()) {
                return new AppendResult.Refused(e.id(), AppendResult.Reason.NOT_AFTER_CAUSES);
            }
        }
        events.put(e.id(), e);
        return new AppendResult.Appended(e.id());
    }

    @Override
    public synchronized Optional<Event> get(EventId id) {
        return Optional.ofNullable(events.get(id));
    }
}
