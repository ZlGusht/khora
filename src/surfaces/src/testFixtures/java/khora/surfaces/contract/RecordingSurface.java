package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import khora.events.Action;
import khora.events.Event;
import khora.events.EventId;
import khora.events.log.EventLog;
import khora.surfaces.Interest;
import khora.surfaces.Surface;

/**
 * Wraps a surface and records a {@link Run.Delivery} for each event it receives: the event, the
 * actions it returned, and the ids the log held at that moment. A surface that throws is recorded as
 * returning nothing, and the exception passes on to the kernel.
 */
public final class RecordingSurface implements Surface {
    private final Surface inner;
    private final EventLog log;
    private final List<Run.Delivery> deliveries = new ArrayList<>();

    public RecordingSurface(Surface inner, EventLog log) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.log = Objects.requireNonNull(log, "log");
    }

    /** Every delivery so far, in order. */
    public synchronized List<Run.Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    @Override
    public String name() {
        return inner.name();
    }

    @Override
    public Set<Interest> interests() {
        return inner.interests();
    }

    @Override
    public synchronized List<Action> receive(Event event) {
        var before = ids();
        List<Action> returned = List.of();
        try {
            returned = inner.receive(event);
            return returned;
        } finally {
            deliveries.add(new Run.Delivery(event, returned == null ? List.of() : returned, before));
        }
    }

    private Set<EventId> ids() {
        var ids = new HashSet<EventId>();
        for (long position = 1; ; ) {
            var page = log.read(position, EventLog.MAX_APPEND);
            if (page.isEmpty()) {
                return ids;
            }
            page.forEach(logged -> ids.add(logged.event().id()));
            position = page.getLast().position() + 1;
        }
    }
}
