package khora.kernel.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Events in memory, linked by cause. Every event in it has all its causes in it, recorded earlier,
 * and every event it links to in its causal past.
 *
 * <p>Provisional: an in-memory stand-in for the log until K2 chooses the log store.
 */
public final class EventGraph implements EventSource {
    private final Map<EventId, Event> events = new HashMap<>();
    private final List<Event> order = new ArrayList<>();

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
        if (e.body() instanceof AcceptedAction a && !a.links().isEmpty()) {
            var past = FrontierCheck.closure(e.causes(), this::lookup);
            for (var ids : a.links().values()) {
                if (!past.containsAll(ids)) {
                    return new AppendResult.Refused(e.id(), AppendResult.Reason.LINK_OUTSIDE_PAST);
                }
            }
        }
        events.put(e.id(), e);
        order.add(e);
        return new AppendResult.Appended(e.id());
    }

    @Override
    public synchronized Optional<Event> get(EventId id) {
        return lookup(id);
    }

    /** Every event, in the order it was appended, which is after its causes. */
    public synchronized List<Event> events() {
        return List.copyOf(order);
    }

    /** The frontier holding every event. */
    public synchronized Frontier heads() {
        return Frontier.of(events.keySet(), this::lookup);
    }

    private Optional<Event> lookup(EventId id) {
        return Optional.ofNullable(events.get(id));
    }
}
